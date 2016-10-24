/**
 *    Copyright 2009-2015 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.executor.resultset;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.executor.loader.ResultLoader;
import org.apache.ibatis.executor.loader.ResultLoaderMap;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.result.DefaultResultContext;
import org.apache.ibatis.executor.result.DefaultResultHandler;
import org.apache.ibatis.executor.result.ResultMapException;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.*;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.lang.reflect.Constructor;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public class DefaultResultSetHandler implements ResultSetHandler {

  private static final Object DEFERED = new Object();

  private final Executor executor;
  private final Configuration configuration;
  private final MappedStatement mappedStatement;
  private final RowBounds rowBounds;
  private final ParameterHandler parameterHandler;
  private final ResultHandler<?> resultHandler;
  private final BoundSql boundSql;
  private final TypeHandlerRegistry typeHandlerRegistry;
  private final ObjectFactory objectFactory;
  private final ReflectorFactory reflectorFactory;

  // nested resultmaps
  private final Map<CacheKey, Object> nestedResultObjects = new HashMap<CacheKey, Object>(); // todo 只有一个对象的所有子属性完全获取到之后才会把这个对象缓存到 nestedResultObjects 集合中。嵌套结果对象集合一般用在多表关联查询类似 people-》pets 中，迭代结果集的第二行数据时 people 对象会从嵌套结果对象结果集中取出，而不用再像第一次那样来获取了，提高了效率。
  private final Map<CacheKey, Object> ancestorObjects = new HashMap<CacheKey, Object>(); // todo 祖先对象缓存集合 主要用于 a 中有个 b 同时 b 中又有个 a 在获取 b 对象的时候 a 对象会保存在祖先缓存对象集合中，并把取出的对象链接到 b 中。祖先对象在使用完后会移除，例如 a 中有 b 而 b 中 又有 c ，则 a b c 都是祖先对象，当把 c 对象获取到之后会移除缓存中的 c ，当把 b 对象获取完之后会移除 b ，当把 a 对象获取完之后也会移除 a。祖先对象和嵌套对象的层次不一样，祖先对象的层次是在同一层即同一行数据中(数据库结果集的同一行数据)，嵌套对象的层次不一定是在同一行数据中，有可能位于同一行，有可能位于多行（数据库结果集的多行数据）中。祖先对象集合避免了死循环。
  private final Map<String, String> ancestorColumnPrefix = new HashMap<String, String>(); // todo 祖先列前缀集合，键为 resultMapId 值为列前缀 作用主要是判断父对象是不是在缓存中。在获取完一个 resultMap 中的一个值后会把缓存中的这个值移除。在获取完一个结果集中的一行数据后会清空整个祖先集合，在调用 applyNestedResultMappings(); 方法之前会先把当前对象加入到祖先集合中，在获取该对象的属性的过程中可能会用到祖先对象。

  // multiple resultsets
  private final Map<String, ResultMapping> nextResultMaps = new HashMap<String, ResultMapping>();
  private final Map<CacheKey, List<PendingRelation>> pendingRelations = new HashMap<CacheKey, List<PendingRelation>>(); // todo 有问题  见 addPendingChildRelation() 方法的 throw new ExecutorException("Two different properties are mapped to the same resultSet"); 一个 cacheKey 其实代表了一个 result 对象，同一个 result 对象可能需要多次链接到父对象上，比如 A 有属性 B1 和 B2，其中属性 B1 和 B2 是同一个对象

  private static class PendingRelation {
    public MetaObject metaObject;
    public ResultMapping propertyMapping;
  }

  public DefaultResultSetHandler(Executor executor, MappedStatement mappedStatement, ParameterHandler parameterHandler, ResultHandler<?> resultHandler, BoundSql boundSql,
      RowBounds rowBounds) {
    this.executor = executor;
    this.configuration = mappedStatement.getConfiguration();
    this.mappedStatement = mappedStatement;
    this.rowBounds = rowBounds;
    this.parameterHandler = parameterHandler;
    this.boundSql = boundSql;
    this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
    this.objectFactory = configuration.getObjectFactory();
    this.reflectorFactory = configuration.getReflectorFactory();
    this.resultHandler = resultHandler;
  }

  //
  // HANDLE OUTPUT PARAMETER
  //

  @Override
  public void handleOutputParameters(CallableStatement cs) throws SQLException {
    final Object parameterObject = parameterHandler.getParameterObject();
    final MetaObject metaParam = configuration.newMetaObject(parameterObject);
    final List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
    for (int i = 0; i < parameterMappings.size(); i++) {
      final ParameterMapping parameterMapping = parameterMappings.get(i);
      if (parameterMapping.getMode() == ParameterMode.OUT || parameterMapping.getMode() == ParameterMode.INOUT) {
        if (ResultSet.class.equals(parameterMapping.getJavaType())) {
          handleRefCursorOutputParameter((ResultSet) cs.getObject(i + 1), parameterMapping, metaParam);
        } else {
          final TypeHandler<?> typeHandler = parameterMapping.getTypeHandler();
          metaParam.setValue(parameterMapping.getProperty(), typeHandler.getResult(cs, i + 1));
        }
      }
    }
  }

  private void handleRefCursorOutputParameter(ResultSet rs, ParameterMapping parameterMapping, MetaObject metaParam) throws SQLException {
    try {
      final String resultMapId = parameterMapping.getResultMapId();
      final ResultMap resultMap = configuration.getResultMap(resultMapId);
      final DefaultResultHandler resultHandler = new DefaultResultHandler(objectFactory);
      final ResultSetWrapper rsw = new ResultSetWrapper(rs, configuration);
      handleRowValues(rsw, resultMap, resultHandler, new RowBounds(), null);
      metaParam.setValue(parameterMapping.getProperty(), resultHandler.getResultList());
    } finally {
      // issue #228 (close resultsets)
      closeResultSet(rs);
    }
  }

  //
  // HANDLE RESULT SETS
  //
  @Override
  public List<Object> handleResultSets(Statement stmt) throws SQLException {
    ErrorContext.instance().activity("handling results").object(mappedStatement.getId());

    // 返回值 会保存 resultMap 中的返回值但并不是保存每个 resultMap 中的返回值，内层 resultMap 中的返回值其实是最外层 resultMap 中的返回值的属性，不需要保存，但需要把这些属性设置到父对象上。
    final List<Object> multipleResults = new ArrayList<Object>();

    int resultSetCount = 0;
    ResultSetWrapper rsw = getFirstResultSet(stmt);

    // todo 什么时候会出现多个 resultMap 呢？？？ 存储过程？？？ 应该是存储过程，如果是非存储过程的话，只会有一个结果集，handleResultSet(rsw, resultMap, multipleResults, null);处理完一个 resultMap 之后结果集就关闭了
    List<ResultMap> resultMaps = mappedStatement.getResultMaps();
    int resultMapCount = resultMaps.size();
    validateResultMapsCount(rsw, resultMapCount);
    // todo 处理结果集，mybatis 有可能一次查询返回多个结果集。好像是存储过程可以返回多个结果集，其他情况下不知道会不会返回多个结果集。
    while (rsw != null && resultMapCount > resultSetCount) {
      ResultMap resultMap = resultMaps.get(resultSetCount);
      handleResultSet(rsw, resultMap, multipleResults, null);
      rsw = getNextResultSet(stmt);
      cleanUpAfterHandlingResultSet();
      resultSetCount++;
    }

    // todo 什么时候会出现多个 resultSet ??? 有存储过程的时候会出现
    String[] resultSets = mappedStatement.getResulSets();
    if (resultSets != null) {
      while (rsw != null && resultSetCount < resultSets.length) {
        ResultMapping parentMapping = nextResultMaps.get(resultSets[resultSetCount]);
        if (parentMapping != null) {
          String nestedResultMapId = parentMapping.getNestedResultMapId();
          ResultMap resultMap = configuration.getResultMap(nestedResultMapId);
          handleResultSet(rsw, resultMap, null, parentMapping); // todo 为什么 multipleResults 在上面的 while 循环中出现了，而在此处却传了一个 null
        }
        rsw = getNextResultSet(stmt);
        cleanUpAfterHandlingResultSet();
        resultSetCount++;
      }
    }

    return collapseSingleResultList(multipleResults);
  }

  private ResultSetWrapper getFirstResultSet(Statement stmt) throws SQLException {
    ResultSet rs = stmt.getResultSet();
    while (rs == null) {
      // move forward to get the first resultset in case the driver
      // doesn't return the resultset as the first result (HSQLDB 2.1)
      if (stmt.getMoreResults()) {
        rs = stmt.getResultSet();
      } else {
        if (stmt.getUpdateCount() == -1) {
          // no more results. Must be no resultset
          break;
        }
      }
    }
    return rs != null ? new ResultSetWrapper(rs, configuration) : null;
  }

  private ResultSetWrapper getNextResultSet(Statement stmt) throws SQLException {
    // Making this method tolerant of bad JDBC drivers
    try {
      if (stmt.getConnection().getMetaData().supportsMultipleResultSets()) {
        // Crazy Standard JDBC way of determining if there are more results
        if (!((!stmt.getMoreResults()) && (stmt.getUpdateCount() == -1))) {
          ResultSet rs = stmt.getResultSet();
          return rs != null ? new ResultSetWrapper(rs, configuration) : null;
        }
      }
    } catch (Exception e) {
      // Intentionally ignored.
    }
    return null;
  }

  private void closeResultSet(ResultSet rs) {
    try {
      if (rs != null) {
        rs.close();
      }
    } catch (SQLException e) {
      // ignore
    }
  }

  private void cleanUpAfterHandlingResultSet() {
    nestedResultObjects.clear(); // todo 什么时候不为空
    ancestorColumnPrefix.clear();
  }

  private void validateResultMapsCount(ResultSetWrapper rsw, int resultMapCount) {
    if (rsw != null && resultMapCount < 1) {
      throw new ExecutorException("A query was run and no Result Maps were found for the Mapped Statement '" + mappedStatement.getId()
          + "'.  It's likely that neither a Result Type nor a Result Map was specified.");
    }
  }

  // todo 处理返回的一个结果集
  private void handleResultSet(ResultSetWrapper rsw, ResultMap resultMap, List<Object> multipleResults, ResultMapping parentMapping) throws SQLException {
    try {
      // todo 什么时候会有 parentMapping ???? 存储过程有多个结果集的时候会有
      if (parentMapping != null) {
        handleRowValues(rsw, resultMap, null, RowBounds.DEFAULT, parentMapping); // todo 这儿并没有 multipleResults.add()； 这一行代码，这是为什么呢
      } else {
        // todo 什么时候 resultHandler 不为 null？应该是在 xml sql 中配置了 resultHandler 为什么 resultHandler 为 null 的时候 需要 multipleResults.add
        // 而resultHandler 不为 null 的时候 却不需要？？？
        if (resultHandler == null) {
          DefaultResultHandler defaultResultHandler = new DefaultResultHandler(objectFactory);
          // todo 处理返回的一个结果集，一个结果集中可能有多条记录
          handleRowValues(rsw, resultMap, defaultResultHandler, rowBounds, null); // resultHandler 和 parentMapping 不会同时出现
          multipleResults.add(defaultResultHandler.getResultList());
        } else {
          handleRowValues(rsw, resultMap, resultHandler, rowBounds, null); // todo 为什么有了 resultHandler 之后就不需要 multipleResults.add(defaultResultHandler.getResultList()); bug?还是故意的？？
        }
      }
    } finally {
      // issue #228 (close resultsets)
      closeResultSet(rsw.getResultSet());
    }
  }

  @SuppressWarnings("unchecked")
  private List<Object> collapseSingleResultList(List<Object> multipleResults) {
    return multipleResults.size() == 1 ? (List<Object>) multipleResults.get(0) : multipleResults;
  }

  //
  // HANDLE ROWS FOR SIMPLE RESULTMAP
  //
  // todo 处理一个结果集中的某个 resultMap 中的所有记录
  private void handleRowValues(ResultSetWrapper rsw, ResultMap resultMap, ResultHandler<?> resultHandler, RowBounds rowBounds, ResultMapping parentMapping) throws SQLException {
    if (resultMap.hasNestedResultMaps()) {
      ensureNoRowBounds();
      checkResultHandler();
      handleRowValuesForNestedResultMap(rsw, resultMap, resultHandler, rowBounds, parentMapping);
    } else {
      handleRowValuesForSimpleResultMap(rsw, resultMap, resultHandler, rowBounds, parentMapping);
    }
  }

  private void ensureNoRowBounds() {
    if (configuration.isSafeRowBoundsEnabled() && rowBounds != null && (rowBounds.getLimit() < RowBounds.NO_ROW_LIMIT || rowBounds.getOffset() > RowBounds.NO_ROW_OFFSET)) {
      throw new ExecutorException("Mapped Statements with nested result mappings cannot be safely constrained by RowBounds. "
          + "Use safeRowBoundsEnabled=false setting to bypass this check.");
    }
  }

  protected void checkResultHandler() {
    if (resultHandler != null && configuration.isSafeResultHandlerEnabled() && !mappedStatement.isResultOrdered()) {
      throw new ExecutorException("Mapped Statements with nested result mappings cannot be safely used with a custom ResultHandler. "
          + "Use safeResultHandlerEnabled=false setting to bypass this check "
          + "or ensure your statement returns ordered data and set resultOrdered=true on it.");
    }
  }

  // todo 处理简单 resultMap 只含有一个简单对象 也不一定，有可能是存储过程中一个结果集里的对象还有一个子对象在另外一个结果集中
  private void handleRowValuesForSimpleResultMap(ResultSetWrapper rsw, ResultMap resultMap, ResultHandler<?> resultHandler, RowBounds rowBounds, ResultMapping parentMapping)
      throws SQLException {
    DefaultResultContext<Object> resultContext = new DefaultResultContext<Object>();
    skipRows(rsw.getResultSet(), rowBounds);
    // todo 每次只处理一条记录
    while (shouldProcessMoreRows(resultContext, rowBounds) && rsw.getResultSet().next()) {
      // todo 识别器 返回值的类型是不确定的，结果集出来之后会根据结果集中某一行的某一列的值来确定具体的返回一行所对应的 java 类型。
      ResultMap discriminatedResultMap = resolveDiscriminatedResultMap(rsw.getResultSet(), resultMap, null);
      Object rowValue = getRowValue(rsw, discriminatedResultMap);
      storeObject(resultHandler, resultContext, rowValue, parentMapping, rsw.getResultSet());
    }
  }

  /**
   * todo ????
   * @param resultHandler
   * @param resultContext
   * @param rowValue
   * @param parentMapping
   * @param rs
     * @throws SQLException
     */
  private void storeObject(ResultHandler<?> resultHandler, DefaultResultContext<Object> resultContext, Object rowValue, ResultMapping parentMapping, ResultSet rs) throws SQLException {
    if (parentMapping != null) {
      linkToParents(rs, parentMapping, rowValue);
    } else {
      callResultHandler(resultHandler, resultContext, rowValue);
    }
  }

  @SuppressWarnings("unchecked" /* because ResultHandler<?> is always ResultHandler<Object>*/)
  private void callResultHandler(ResultHandler<?> resultHandler, DefaultResultContext<Object> resultContext, Object rowValue) {
    resultContext.nextResultObject(rowValue);
    ((ResultHandler<Object>)resultHandler).handleResult(resultContext);
  }

  private boolean shouldProcessMoreRows(ResultContext<?> context, RowBounds rowBounds) throws SQLException {
    return !context.isStopped() && context.getResultCount() < rowBounds.getLimit();
  }

  private void skipRows(ResultSet rs, RowBounds rowBounds) throws SQLException {
    if (rs.getType() != ResultSet.TYPE_FORWARD_ONLY) {
      if (rowBounds.getOffset() != RowBounds.NO_ROW_OFFSET) {
        rs.absolute(rowBounds.getOffset());
      }
    } else {
      for (int i = 0; i < rowBounds.getOffset(); i++) {
        rs.next();
      }
    }
  }

  //
  // GET VALUE FROM ROW FOR SIMPLE RESULT MAP
  //

  // todo 处理一个结果集中的一行数据 把一行数据映射为一个对象 简单 resultMap
  private Object getRowValue(ResultSetWrapper rsw, ResultMap resultMap) throws SQLException {
    final ResultLoaderMap lazyLoader = new ResultLoaderMap();
    // todo 创建好的返回的对象
    Object resultObject = createResultObject(rsw, resultMap, lazyLoader, null);
    if (resultObject != null && !typeHandlerRegistry.hasTypeHandler(resultMap.getType())) { // todo 为什么注册器里有这个 typeHandler 就直接返回 因为注册器里注册的都是简单的类型，例如 String，int,float 而不是复杂的自定义对象，所以直接返回即可
      final MetaObject metaObject = configuration.newMetaObject(resultObject);
      boolean foundValues = !resultMap.getConstructorResultMappings().isEmpty();
      // 自动映射通过反射调用 set 方法设置属性值（给没有映射过的列设置属性值）
      if (shouldApplyAutomaticMappings(resultMap, false)) {
        foundValues = applyAutomaticMappings(rsw, resultMap, metaObject, null) || foundValues;
      }
      // 给属性映射设置值 其中 属性映射 构造方法映射 自动映射这三者没有交集
      // todo 属性映射和自动映射的顺序能不能交换??? 可以的，这两者是没有交集的
      foundValues = applyPropertyMappings(rsw, resultMap, metaObject, lazyLoader, null) || foundValues;
      foundValues = lazyLoader.size() > 0 || foundValues;
      resultObject = foundValues ? resultObject : null;
      return resultObject;
    }
    return resultObject;
  }

  /**
   * 什么是自动映射？就是从结果集中查询出来的列值，在没有做 <result></result> 映射下也会把相应的列值设置到对象的相应属性中。
   * 如果在 resultMap 中配置了自动映射，则按配置的值判断
   * 如果没配置的话，如果是简单 resultMap 只要全局配置不是 NONE （不进行自动映射） 就会自动映射
   * 如果是嵌套 resultMap 的话只有全局配置是 FULL(全部进行自动映射) 才会自动映射
   * 自动映射行为有三种值：1、NONE（不进行自动映射） 2、PARTIAL（部分自动映射，即简单的 resultMap 会进行自动映射，嵌套的 resultMap 不会进行自动映射）
   * 比如在嵌套的 resultMap 中很可能会有重名的属性（列名）比如最容易重名的就是 id
   * @param resultMap
   * @param isNested
   * @return
     */
  private boolean shouldApplyAutomaticMappings(ResultMap resultMap, boolean isNested) {
    if (resultMap.getAutoMapping() != null) {
      return resultMap.getAutoMapping();
    } else {
      if (isNested) {
        return AutoMappingBehavior.FULL == configuration.getAutoMappingBehavior();
      } else {
        return AutoMappingBehavior.NONE != configuration.getAutoMappingBehavior();
      }
    }
  }

  //
  // PROPERTY MAPPINGS 也包括组合列 组合列和 select 成对儿出现 最终查询出一个对象设置给某个属性
  //

  private boolean applyPropertyMappings(ResultSetWrapper rsw, ResultMap resultMap, MetaObject metaObject, ResultLoaderMap lazyLoader, String columnPrefix)
      throws SQLException {
    final List<String> mappedColumnNames = rsw.getMappedColumnNames(resultMap, columnPrefix); // todo 什么时候 mappedColumnNames 不为空？？？ 在?<resultMap/> 中有<result/>映射的时候不为空
    boolean foundValues = false;
    final List<ResultMapping> propertyMappings = resultMap.getPropertyResultMappings();
    for (ResultMapping propertyMapping : propertyMappings) {
      String column = prependPrefix(propertyMapping.getColumn(), columnPrefix);
      if (propertyMapping.getNestedResultMapId() != null) {
        // 在有嵌套 resultMap 的时候不会用列名，为什么不用呢？嵌套 resultMap 其实确定了一个对象，这个对象的属性映射与 parent resultMapping 的这个列名没关系
        // the user added a column attribute to a nested result map, ignore it
        column = null;

      }
      if (propertyMapping.isCompositeResult()
          || (column != null && mappedColumnNames.contains(column.toUpperCase(Locale.ENGLISH))) // todo mappedColumnNames.contains(column.toUpperCase(Locale.ENGLISH)) 有没有 有用，因为在 嵌套查询中的列名虽然不为空，但也不会在 mappedColumnNames.contains 中包括
          || propertyMapping.getResultSet() != null) { // mappedColumnNames 是干啥的？为啥需要 mappedColumnNames.contains(column.toUpperCase(Locale.ENGLISH)) ？？？ 感觉是不需要的，因为 mappedColumnNames+unmappedColumnNames=fullNames(从结果集中查询出来的所有的列名集合) mappedColumnNames 是从 fullNames 集合中遍历并且该列名在 resultMappings（也包括组合列名） 中出现过才会加入 也就是说 mappedColumnNames 是 resultMapping 中的列名和结果集中的列名的交集。
        Object value = getPropertyMappingValue(rsw.getResultSet(), metaObject, propertyMapping, lazyLoader, columnPrefix);
        // issue #541 make property optional
        final String property = propertyMapping.getProperty();
        // issue #377, call setter on nulls
        if (value != DEFERED
            && property != null
            && (value != null || (configuration.isCallSettersOnNulls() && !metaObject.getSetterType(property).isPrimitive()))) {
          metaObject.setValue(property, value);
        }
        if (value != null || value == DEFERED) {
          foundValues = true;
        }
      }
    }
    return foundValues;
  }
  /**
   * 根据属性名称获取到相应的值 有三种情况：1、该属性映射关联了一个嵌套查询语句，通过嵌套查询语句查询出该属性的值 2、该属性映射关联了一个结果集 3、属性类型是原生类型，属性值直接通过 resultSet 获取     嵌套 resultMap 该怎么处理呢？？？？？？？ 嵌套 resultMap 不经过这里是由 applyNestedResultMappings 处理
   * @param rs
   * @param metaResultObject
   * @param propertyMapping
   * @param lazyLoader
   * @param columnPrefix
   * @return
   * @throws SQLException
   */
  private Object getPropertyMappingValue(ResultSet rs, MetaObject metaResultObject, ResultMapping propertyMapping, ResultLoaderMap lazyLoader, String columnPrefix)
      throws SQLException {
    // 通过嵌套查询查出属性
    if (propertyMapping.getNestedQueryId() != null) {
      return getNestedQueryMappingValue(rs, metaResultObject, propertyMapping, lazyLoader, columnPrefix);
    } else if (propertyMapping.getResultSet() != null) {
      // 在有存储过程的时候可能会出现多个结果集 propertyMapping.getResultSet() != null
      addPendingChildRelation(rs, metaResultObject, propertyMapping);   // TODO is that OK?
      return DEFERED;
    } else {
      // 属性类型是原生类型，属性值直接在 resultSet 中获取
      final TypeHandler<?> typeHandler = propertyMapping.getTypeHandler();
      final String column = prependPrefix(propertyMapping.getColumn(), columnPrefix);
      return typeHandler.getResult(rs, column);
    }
  }

  // todo 从未映射列集合中取出所有的列，看该列是不是 resultMap 对应的 Java 类型中的属性，如果是的话，设置相应的值
  private boolean applyAutomaticMappings(ResultSetWrapper rsw, ResultMap resultMap, MetaObject metaObject, String columnPrefix) throws SQLException {
    final List<String> unmappedColumnNames = rsw.getUnmappedColumnNames(resultMap, columnPrefix);
    boolean foundValues = false;
    // todo 什么时候 unmappedColumnNames 不为空？？？？
    for (String columnName : unmappedColumnNames) {
      String propertyName = columnName;
      if (columnPrefix != null && !columnPrefix.isEmpty()) {
        // When columnPrefix is specified,
        // ignore columns without the prefix. // 因为未映射的列有些并不是这个 resultMap 对应的 Java 类型的属性，这些列名当然不会以 columnPrefix 开头
        if (columnName.toUpperCase(Locale.ENGLISH).startsWith(columnPrefix)) {
          propertyName = columnName.substring(columnPrefix.length());
        } else {
          continue;
        }
      }
      final String property = metaObject.findProperty(propertyName, configuration.isMapUnderscoreToCamelCase());
      if (property != null && metaObject.hasSetter(property)) {
        final Class<?> propertyType = metaObject.getSetterType(property);
        if (typeHandlerRegistry.hasTypeHandler(propertyType)) {
          final TypeHandler<?> typeHandler = rsw.getTypeHandler(propertyType, columnName);
          final Object value = typeHandler.getResult(rsw.getResultSet(), columnName);
          // issue #377, call setter on nulls 结果集中的值（也就是 value ）为 null 的时候是否调用映射对象的 setter 方法见 http://www.mybatis.org/mybatis-3/zh/configuration.html callSettersOnNulls 属性
          if (value != null || configuration.isCallSettersOnNulls()) {
            if (value != null || !propertyType.isPrimitive()) {
              // todo 设置对象的一个属性值
              metaObject.setValue(property, value);
            }
            foundValues = true;
          }
        }
      }
    }
    return foundValues;
  }

  // MULTIPLE RESULT SETS

  private void linkToParents(ResultSet rs, ResultMapping parentMapping, Object rowValue) throws SQLException {
    CacheKey parentKey = createKeyForMultipleResults(rs, parentMapping, parentMapping.getColumn(), parentMapping.getForeignColumn());
    List<PendingRelation> parents = pendingRelations.get(parentKey);
    if (parents != null) {
      for (PendingRelation parent : parents) {
        if (parent != null && rowValue != null) {
            linkObjects(parent.metaObject, parent.propertyMapping, rowValue);
        }
      }
    }
  }

  /**
   * 添加待定的子关系 只有在多个结果集（存储过程）中才会用到它
   * @param rs
   * @param metaResultObject
   * @param parentMapping
   * @throws SQLException
   */
  private void addPendingChildRelation(ResultSet rs, MetaObject metaResultObject, ResultMapping parentMapping) throws SQLException {
    CacheKey cacheKey = createKeyForMultipleResults(rs, parentMapping, parentMapping.getColumn(), parentMapping.getColumn());
    PendingRelation deferLoad = new PendingRelation();
    deferLoad.metaObject = metaResultObject;
    deferLoad.propertyMapping = parentMapping;
    List<PendingRelation> relations = pendingRelations.get(cacheKey);
    // issue #255
    if (relations == null) {
      relations = new ArrayList<DefaultResultSetHandler.PendingRelation>();
      pendingRelations.put(cacheKey, relations);
    }
    relations.add(deferLoad);
    ResultMapping previous = nextResultMaps.get(parentMapping.getResultSet());
    if (previous == null) {
      nextResultMaps.put(parentMapping.getResultSet(), parentMapping);
    } else {
      if (!previous.equals(parentMapping)) {
        throw new ExecutorException("Two different properties are mapped to the same resultSet");
      }
    }
  }

  /**
   * 为 multiple result（只有存储过程才可能有多个结果集） 创建缓存 key
   * 缓存 key 与 resultMapping 有关系，属性名称以及该属性名称相应的值。
   * todo 其实是相当于给 result 对象创建了 cacheKey 。如果 names 是 id 的话，???????????如果不是则表示的应该是所有的属性，所有的属性都一样，才表示对象相同
   * @param rs
   * @param resultMapping
   * @param names
   * @param columns 作用主要是根据 列名 在 resultSet 中取出对应的值
   * @return
   * @throws SQLException
   */
  private CacheKey createKeyForMultipleResults(ResultSet rs, ResultMapping resultMapping, String names, String columns) throws SQLException {
    CacheKey cacheKey = new CacheKey();
    cacheKey.update(resultMapping);
    if (columns != null && names != null) {
      String[] columnsArray = columns.split(",");
      String[] namesArray = names.split(",");
      for (int i = 0 ; i < columnsArray.length ; i++) {
        Object value = rs.getString(columnsArray[i]);
        if (value != null) {
          cacheKey.update(namesArray[i]);
          cacheKey.update(value);
        }
      }
    }
    return cacheKey;
  }

  //
  // INSTANTIATION & CONSTRUCTOR MAPPING
  //

  private Object createResultObject(ResultSetWrapper rsw, ResultMap resultMap, ResultLoaderMap lazyLoader, String columnPrefix) throws SQLException {
    final List<Class<?>> constructorArgTypes = new ArrayList<Class<?>>();
    final List<Object> constructorArgs = new ArrayList<Object>();
    // todo 创建的返回的对象
    final Object resultObject = createResultObject(rsw, resultMap, constructorArgTypes, constructorArgs, columnPrefix);
    if (resultObject != null && !typeHandlerRegistry.hasTypeHandler(resultMap.getType())) { // 说明这个 resultObject 是个复杂对象，简单对象比如 int String 直接返回即可，简单对象类型会在 typeHandlerRegistry 里注册
      final List<ResultMapping> propertyMappings = resultMap.getPropertyResultMappings();
      // todo ？只要有一个属性配置了懒加载就需要创建代理对象？？？
      for (ResultMapping propertyMapping : propertyMappings) {
        // issue gcode #109 && issue #149
        if (propertyMapping.getNestedQueryId() != null && propertyMapping.isLazy()) {
          return configuration.getProxyFactory().createProxy(resultObject, lazyLoader, configuration, objectFactory, constructorArgTypes, constructorArgs);
        }
      }
    }

    return resultObject;
  }

  // 创建需要返回的对象
  private Object createResultObject(ResultSetWrapper rsw, ResultMap resultMap, List<Class<?>> constructorArgTypes, List<Object> constructorArgs, String columnPrefix)
      throws SQLException {
    final Class<?> resultType = resultMap.getType();
    final MetaClass metaType = MetaClass.forClass(resultType, reflectorFactory);
    final List<ResultMapping> constructorMappings = resultMap.getConstructorResultMappings();
    // 原生类型的 type 都会保存在 typeHandlerRegistry 比如 String boolean float double 等
    if (typeHandlerRegistry.hasTypeHandler(resultType)) {
      return createPrimitiveResultObject(rsw, resultMap, columnPrefix);
    } else if (!constructorMappings.isEmpty()) {
      return createParameterizedResultObject(rsw, resultType, constructorMappings, constructorArgTypes, constructorArgs, columnPrefix);
    } else if (resultType.isInterface() || metaType.hasDefaultConstructor()) {
      return objectFactory.create(resultType);
    } else if (shouldApplyAutomaticMappings(resultMap, false)) {
      return createByConstructorSignature(rsw, resultType, constructorArgTypes, constructorArgs, columnPrefix);
    }
    throw new ExecutorException("Do not know how to create an instance of " + resultType);
  }

  /**
   * 利用非默认构造方法创建 result 对象
   * @param rsw
   * @param resultType
   * @param constructorMappings
   * @param constructorArgTypes
   * @param constructorArgs
   * @param columnPrefix
   * @return
   */
  Object createParameterizedResultObject(ResultSetWrapper rsw, Class<?> resultType, List<ResultMapping> constructorMappings,
      List<Class<?>> constructorArgTypes, List<Object> constructorArgs, String columnPrefix) {
    boolean foundValues = false;
    for (ResultMapping constructorMapping : constructorMappings) {
      final Class<?> parameterType = constructorMapping.getJavaType();
      final String column = constructorMapping.getColumn();
      final Object value;
      try {
        if (constructorMapping.getNestedQueryId() != null) {// 构造方法里如果有需要嵌套查询出来的属性
          value = getNestedQueryConstructorValue(rsw.getResultSet(), constructorMapping, columnPrefix); // todo 构造方法里如果有需要懒加载的属性怎么处理？？好像是 mybatis 的 bug 啊
        } else if (constructorMapping.getNestedResultMapId() != null) {// 构造方法里如果有嵌套结果属性
          final ResultMap resultMap = configuration.getResultMap(constructorMapping.getNestedResultMapId());
          value = getRowValue(rsw, resultMap);
        } else {// 简单类型的属性
          final TypeHandler<?> typeHandler = constructorMapping.getTypeHandler();
          value = typeHandler.getResult(rsw.getResultSet(), prependPrefix(column, columnPrefix));
        }
      } catch (ResultMapException e) {
        throw new ExecutorException("Could not process result for mapping: " + constructorMapping, e);
      } catch (SQLException e) {
        throw new ExecutorException("Could not process result for mapping: " + constructorMapping, e);
      }
      // 构造方法中参数的 java 类型 在获取构造方法时候会根据参数类型来获取
      constructorArgTypes.add(parameterType);
      // 从结果集中获取到的某个参数的参数值 在调用构造方法的时候会传参实例化对象
      constructorArgs.add(value);
      foundValues = value != null || foundValues;
    }
    // 调用有参数的构造方法来创建对象
    return foundValues ? objectFactory.create(resultType, constructorArgTypes, constructorArgs) : null;
  }

  private Object createByConstructorSignature(ResultSetWrapper rsw, Class<?> resultType, List<Class<?>> constructorArgTypes, List<Object> constructorArgs,
      String columnPrefix) throws SQLException {
    for (Constructor<?> constructor : resultType.getDeclaredConstructors()) {
      if (typeNames(constructor.getParameterTypes()).equals(rsw.getClassNames())) {
        boolean foundValues = false;
        for (int i = 0; i < constructor.getParameterTypes().length; i++) {
          Class<?> parameterType = constructor.getParameterTypes()[i];
          String columnName = rsw.getColumnNames().get(i);
          TypeHandler<?> typeHandler = rsw.getTypeHandler(parameterType, columnName);
          Object value = typeHandler.getResult(rsw.getResultSet(), prependPrefix(columnName, columnPrefix));
          constructorArgTypes.add(parameterType);
          constructorArgs.add(value);
          foundValues = value != null || foundValues;
        }
        return foundValues ? objectFactory.create(resultType, constructorArgTypes, constructorArgs) : null;
      }
    }
    throw new ExecutorException("No constructor found in " + resultType.getName() + " matching " + rsw.getClassNames());
  }

  private List<String> typeNames(Class<?>[] parameterTypes) {
    List<String> names = new ArrayList<String>();
    for (Class<?> type : parameterTypes) {
      names.add(type.getName());
    }
    return names;
  }

  /**
   * 创建原生类型的 result 对象
   * 比如 select id from student
   * id 是 Integer 类型的
   * @param rsw
   * @param resultMap
   * @param columnPrefix
   * @return
   * @throws SQLException
   */
  private Object createPrimitiveResultObject(ResultSetWrapper rsw, ResultMap resultMap, String columnPrefix) throws SQLException {
    final Class<?> resultType = resultMap.getType();
    final String columnName;
    if (!resultMap.getResultMappings().isEmpty()) {
      final List<ResultMapping> resultMappingList = resultMap.getResultMappings();
      // todo 为什么只取 resultMappingList.get(0);
      // 因为返回的对象是原生类型，只返回了数据库中的一列，正常情况下映射列表 resultMappingList 的长度应该是 1
      final ResultMapping mapping = resultMappingList.get(0);
      columnName = prependPrefix(mapping.getColumn(), columnPrefix);
    } else {
      columnName = rsw.getColumnNames().get(0);
    }
    final TypeHandler<?> typeHandler = rsw.getTypeHandler(resultType, columnName);
    return typeHandler.getResult(rsw.getResultSet(), columnName);
  }

  //
  // NESTED QUERY
  //

  /**
   * 嵌套查询出某一条记录，然后把该条记录的值作为构造方法的参数
   *  <constructor>
   *      <arg select=""></arg>
   *  </constructor>
   *  构造方法的某一个参数可能是一个 嵌套查询
   * @param rs
   * @param constructorMapping
   * @param columnPrefix
   * @return
   * @throws SQLException
   */
  private Object getNestedQueryConstructorValue(ResultSet rs, ResultMapping constructorMapping, String columnPrefix) throws SQLException {
    final String nestedQueryId = constructorMapping.getNestedQueryId();
    final MappedStatement nestedQuery = configuration.getMappedStatement(nestedQueryId);
    final Class<?> nestedQueryParameterType = nestedQuery.getParameterMap().getType();
    final Object nestedQueryParameterObject = prepareParameterForNestedQuery(rs, constructorMapping, nestedQueryParameterType, columnPrefix);
    Object value = null;
    if (nestedQueryParameterObject != null) {
      final BoundSql nestedBoundSql = nestedQuery.getBoundSql(nestedQueryParameterObject);
      final CacheKey key = executor.createCacheKey(nestedQuery, nestedQueryParameterObject, RowBounds.DEFAULT, nestedBoundSql);
      final Class<?> targetType = constructorMapping.getJavaType();
      final ResultLoader resultLoader = new ResultLoader(configuration, executor, nestedQuery, nestedQueryParameterObject, targetType, key, nestedBoundSql);
      value = resultLoader.loadResult();
    }
    return value;
  }

  private Object getNestedQueryMappingValue(ResultSet rs, MetaObject metaResultObject, ResultMapping propertyMapping, ResultLoaderMap lazyLoader, String columnPrefix)
      throws SQLException {
    final String nestedQueryId = propertyMapping.getNestedQueryId();
    final String property = propertyMapping.getProperty();
    final MappedStatement nestedQuery = configuration.getMappedStatement(nestedQueryId);
    final Class<?> nestedQueryParameterType = nestedQuery.getParameterMap().getType();
    // 嵌套查询时所需的参数
    final Object nestedQueryParameterObject = prepareParameterForNestedQuery(rs, propertyMapping, nestedQueryParameterType, columnPrefix);
    Object value = null;
    if (nestedQueryParameterObject != null) {
      final BoundSql nestedBoundSql = nestedQuery.getBoundSql(nestedQueryParameterObject);
      final CacheKey key = executor.createCacheKey(nestedQuery, nestedQueryParameterObject, RowBounds.DEFAULT, nestedBoundSql);
      final Class<?> targetType = propertyMapping.getJavaType();
      // 好像虽然有 nestedQuery 这个参数，但在查询是否命中缓存的时候只会使用 key 值来判断，暂时还没有用到 nestedQuery 这个参数。
      if (executor.isCached(nestedQuery, key)) {
        executor.deferLoad(nestedQuery, metaResultObject, property, key, targetType);
        value = DEFERED;
      } else {
        final ResultLoader resultLoader = new ResultLoader(configuration, executor, nestedQuery, nestedQueryParameterObject, targetType, key, nestedBoundSql);
        if (propertyMapping.isLazy()) {
          lazyLoader.addLoader(property, metaResultObject, resultLoader);
          value = DEFERED;
        } else {
          value = resultLoader.loadResult();
        }
      }
    }
    return value;
  }

  private Object prepareParameterForNestedQuery(ResultSet rs, ResultMapping resultMapping, Class<?> parameterType, String columnPrefix) throws SQLException {
    if (resultMapping.isCompositeResult()) {
      return prepareCompositeKeyParameter(rs, resultMapping, parameterType, columnPrefix);
    } else {
      return prepareSimpleKeyParameter(rs, resultMapping, parameterType, columnPrefix);
    }
  }

  private Object prepareSimpleKeyParameter(ResultSet rs, ResultMapping resultMapping, Class<?> parameterType, String columnPrefix) throws SQLException {
    final TypeHandler<?> typeHandler;
    if (typeHandlerRegistry.hasTypeHandler(parameterType)) {
      typeHandler = typeHandlerRegistry.getTypeHandler(parameterType);
    } else {
      typeHandler = typeHandlerRegistry.getUnknownTypeHandler();
    }
    return typeHandler.getResult(rs, prependPrefix(resultMapping.getColumn(), columnPrefix));
  }

  /**
   * 如果 parameterType 为 null 的话，实例化出来的参数 parameterObject 为 HashMap，在 组合列的情况下 如果 parameterObject 为 HashMap
   * 的话会把组合列以键值对儿的形式保存
   * @param rs
   * @param resultMapping
   * @param parameterType
   * @param columnPrefix
   * @return
   * @throws SQLException
   */
  private Object prepareCompositeKeyParameter(ResultSet rs, ResultMapping resultMapping, Class<?> parameterType, String columnPrefix) throws SQLException {
    final Object parameterObject = instantiateParameterObject(parameterType);
    final MetaObject metaObject = configuration.newMetaObject(parameterObject);
    boolean foundValues = false;
    for (ResultMapping innerResultMapping : resultMapping.getComposites()) {
      final Class<?> propType = metaObject.getSetterType(innerResultMapping.getProperty());
      final TypeHandler<?> typeHandler = typeHandlerRegistry.getTypeHandler(propType);
      final Object propValue = typeHandler.getResult(rs, prependPrefix(innerResultMapping.getColumn(), columnPrefix));
      // issue #353 & #560 do not execute nested query if key is null
      if (propValue != null) {
        metaObject.setValue(innerResultMapping.getProperty(), propValue);
        foundValues = true;
      }
    }
    return foundValues ? parameterObject : null;
  }

  private Object instantiateParameterObject(Class<?> parameterType) {
    if (parameterType == null) {
      return new HashMap<Object, Object>();
    } else {
      return objectFactory.create(parameterType);
    }
  }

  //
  // DISCRIMINATOR
  //

  public ResultMap resolveDiscriminatedResultMap(ResultSet rs, ResultMap resultMap, String columnPrefix) throws SQLException {
    Set<String> pastDiscriminators = new HashSet<String>();
    Discriminator discriminator = resultMap.getDiscriminator();
    // todo 为什么要循环呢？一个 resultMap 中可能会有多个 discriminator
    // resultMap 中含有 discriminator discriminator 又确定了一个 discriminator
    while (discriminator != null) {
      final Object value = getDiscriminatorValue(rs, discriminator, columnPrefix);
      // 鉴别器 中的 column 的值又是 Map 中的 key ，根据 key 取出相应的值，根据值可以确定返回的类型
      final String discriminatedMapId = discriminator.getMapIdFor(String.valueOf(value));
      // discriminatedMapId 可以是一个 class 的全名，也可以是一个 <resultMap> 的 id
      // discriminatedMapId 是 <resultMap> 的 id 的情况
      if (configuration.hasResultMap(discriminatedMapId)) {
        resultMap = configuration.getResultMap(discriminatedMapId);
        Discriminator lastDiscriminator = discriminator;
        discriminator = resultMap.getDiscriminator();
        // !pastDiscriminators.add(discriminatedMapId) 防止死循环 如果某个 discriminatedMapId 已经出现过了，则 break
        if (discriminator == lastDiscriminator || !pastDiscriminators.add(discriminatedMapId)) {
          break;
        }
      } else {
        // discriminatedMapId 是一个 class 的全名
        break;
      }
    }
    return resultMap;
  }

  /**
   * 根据 <discriminator javaType="String" column="type"> 标签中的 column 值获取到相应的值
   * @param rs
   * @param discriminator
   * @param columnPrefix
   * @return
   * @throws SQLException
   */
  private Object getDiscriminatorValue(ResultSet rs, Discriminator discriminator, String columnPrefix) throws SQLException {
    final ResultMapping resultMapping = discriminator.getResultMapping();
    final TypeHandler<?> typeHandler = resultMapping.getTypeHandler();
    return typeHandler.getResult(rs, prependPrefix(resultMapping.getColumn(), columnPrefix));
  }

  private String prependPrefix(String columnName, String prefix) {
    if (columnName == null || columnName.length() == 0 || prefix == null || prefix.length() == 0) {
      return columnName;
    }
    return prefix + columnName;
  }

  //
  // HANDLE NESTED RESULT MAPS
  //

  private void handleRowValuesForNestedResultMap(ResultSetWrapper rsw, ResultMap resultMap, ResultHandler<?> resultHandler, RowBounds rowBounds, ResultMapping parentMapping) throws SQLException {
    final DefaultResultContext<Object> resultContext = new DefaultResultContext<Object>();
    skipRows(rsw.getResultSet(), rowBounds);
    Object rowValue = null;
    while (shouldProcessMoreRows(resultContext, rowBounds) && rsw.getResultSet().next()) {
      final ResultMap discriminatedResultMap = resolveDiscriminatedResultMap(rsw.getResultSet(), resultMap, null);
      final CacheKey rowKey = createRowKey(discriminatedResultMap, rsw, null);
      Object partialObject = nestedResultObjects.get(rowKey);
      // issue #577 && #542
      if (mappedStatement.isResultOrdered()) { // todo 什么时候会是有序的？？？有序是怎么实现的？？？？
        if (partialObject == null && rowValue != null) {
          nestedResultObjects.clear();
          storeObject(resultHandler, resultContext, rowValue, parentMapping, rsw.getResultSet());
        }
        rowValue = getRowValue(rsw, discriminatedResultMap, rowKey, rowKey, null, partialObject);
      } else {
        rowValue = getRowValue(rsw, discriminatedResultMap, rowKey, rowKey, null, partialObject);
        if (partialObject == null) {
          // todo 什么时候 partialObject 对象不为 null ??? 例如：people 里有 pets pets 里有多个 pet 对象，sql 语句类似：SELECT people.id as people_id, people.name as people_name, pet.id as pet_id, pet.name as pet_name, pet.owner_id FROM people people, pet pet WHERE people.id=1 AND pet.owner_id=people.id 查询出来的结果是：
          // people_id people_name pet_id pet_name owner_id
          // 1         张三         1      dog      1
          // 1         张三         2      cat      1
          // 最终映射为对象就是一个 people 里有个 pets 集合，pets 集合里有两个 pet 对象。
          storeObject(resultHandler, resultContext, rowValue, parentMapping, rsw.getResultSet());
        }
      }
    }
    if (rowValue != null && mappedStatement.isResultOrdered() && shouldProcessMoreRows(resultContext, rowBounds)) {
      storeObject(resultHandler, resultContext, rowValue, parentMapping, rsw.getResultSet());
    } // todo 这又是干啥的，为什么在循环里边 storeObject 过有需要 storeObject ？？？
  }

  //
  // GET VALUE FROM ROW FOR NESTED RESULT MAP todo 有几个重载的 getRowValue(); 为什么设计成这样？？参数少的 getRowValue(); 是为简单 ResultMap 设计的，参数多的 getRowValue 是为嵌套 ResultMap 设计的
  //

  /**
   * 递归调用 getRowValue()->applyNestedResultMappings()->getRowValue()
   * @param rsw
   * @param resultMap
   * @param combinedKey
   * @param absoluteKey
   * @param columnPrefix 关联 resultMap 的前缀见：http://www.mybatis.org/mybatis-3/zh/sqlmap-xml.html ，其对应着 <association/> 或者是 <collection/> 标签中的 columnPrefix 属性。 因为是嵌套 resultMap 的列前缀，所以第一个 resultMap 的 getRowValue(); 方法传的参数是 null 只有其 resultMapping 又关联了另外一个 resultMap 在调用子 resultMap 中的 getRowValue(); 方法时会传子 resultMap 的列前缀。
   * @param partialObject
   * @return
     * @throws SQLException
     */
  private Object getRowValue(ResultSetWrapper rsw, ResultMap resultMap, CacheKey combinedKey, CacheKey absoluteKey, String columnPrefix, Object partialObject) throws SQLException {
    final String resultMapId = resultMap.getId();
    Object resultObject = partialObject; // 什么时候 partialObject 不为空？比如 people 里有 pets pets 里有多个 pet 当获取第二个 people 时 partialObject 就不为空。
    if (resultObject != null) {
      final MetaObject metaObject = configuration.newMetaObject(resultObject);
      putAncestor(absoluteKey, resultObject, resultMapId, columnPrefix);
      applyNestedResultMappings(rsw, resultMap, metaObject, columnPrefix, combinedKey, false); // 从缓存中取出的对象时最后一个参数传递 false
      ancestorObjects.remove(absoluteKey);
    } else {
      final ResultLoaderMap lazyLoader = new ResultLoaderMap(); // 用于懒加载 一个对象（调用 getRowValue() 方法后获得的对象）的所有需要懒加载的属性以及加载这个属性所需要的条件都封装到了 ResultLoaderMap 里面
      resultObject = createResultObject(rsw, resultMap, lazyLoader, columnPrefix);
      if (resultObject != null && !typeHandlerRegistry.hasTypeHandler(resultMap.getType())) { // typeHandlerRegistry.hasTypeHandler(resultMap.getType()) 嵌套对象是个简单类型
        final MetaObject metaObject = configuration.newMetaObject(resultObject);
        boolean foundValues = !resultMap.getConstructorResultMappings().isEmpty();
        if (shouldApplyAutomaticMappings(resultMap, true)) { // 如果是嵌套 resultMap 传 true ，否则传 false
          foundValues = applyAutomaticMappings(rsw, resultMap, metaObject, columnPrefix) || foundValues; // 自动映射属性，嵌套 resultMap 默认不自动映射
        }
        foundValues = applyPropertyMappings(rsw, resultMap, metaObject, lazyLoader, columnPrefix) || foundValues;
        putAncestor(absoluteKey, resultObject, resultMapId, columnPrefix); // 在 applyNestedResultMappings(); 之前保存祖先对象，之后再移除，再设置其属性的时候属性里又可能需要到祖先对象，所以属性设置完毕之后移除祖先对象
        foundValues = applyNestedResultMappings(rsw, resultMap, metaObject, columnPrefix, combinedKey, true) || foundValues; // 对一个对象的复杂属性进行设值，缓存中没有对象，则最后一个参数传 true .
        ancestorObjects.remove(absoluteKey); // todo 为什么要移除呢？？？ 祖先对象是为获取其子对象服务的（子对象里可能又有祖先对象，比如 a 中有 b b 中又有 a 此时 b 是子对象，当把 b 中的所有属性都获取到从而 b 的值也就获取到了，所以可以把 b 从祖先对象集合中移除了。在获取 b 的值时用到了祖先对象集合，a 的值是通过从祖先对象集合中获取到的，然后再设置到 b 中，如果不是这样做的话，很有可能会造成死循环）
        foundValues = lazyLoader.size() > 0 || foundValues;
        resultObject = foundValues ? resultObject : null; // 如果所有的 resultMapping 中的列名和结果集中的列名没有交集或者是有交集的列值都是空值则 foundValues 为 false ，在这种情况下会返回 null 而不是创建好的那个 resultValue
      }
      // 到底什么时候 combinedKey == CacheKey.NULL_CACHE_KEY? 见 one_to_many_null_cache_key 包
      if (combinedKey != CacheKey.NULL_CACHE_KEY) {
        nestedResultObjects.put(combinedKey, resultObject);
      }
    }
    return resultObject;
  }

  private void putAncestor(CacheKey rowKey, Object resultObject, String resultMapId, String columnPrefix) { // 存放祖先对象 只会在嵌套 resultMap 中使用
    if (!ancestorColumnPrefix.containsKey(resultMapId)) {
      ancestorColumnPrefix.put(resultMapId, columnPrefix);
    }
    ancestorObjects.put(rowKey, resultObject);
  }

  //
  // NESTED RESULT MAP (JOIN MAPPING) todo parentRowKey 是指父对象的缓存 key 设置某个对象的复杂属性 得分析一下多层嵌套对象为什么不会造成死循环 造成死循环是因为嵌套 a 中有 b b 中又有 a 但在 applyNestedResultMappings(); 方法中有嵌套的情况下 ancestorObject 对象就不为空，儿 ancestorObject 不为空的话不会再调用 getRowValue();方法，所以不会造成死循环。
  //

  private boolean applyNestedResultMappings(ResultSetWrapper rsw, ResultMap resultMap, MetaObject metaObject, String parentPrefix, CacheKey parentRowKey, boolean newObject) {
    boolean foundValues = false;
    for (ResultMapping resultMapping : resultMap.getPropertyResultMappings()) {
      final String nestedResultMapId = resultMapping.getNestedResultMapId();
      if (nestedResultMapId != null && resultMapping.getResultSet() == null) {
        try {
          final String columnPrefix = getColumnPrefix(parentPrefix, resultMapping);
          final ResultMap nestedResultMap = getNestedResultMap(rsw.getResultSet(), nestedResultMapId, columnPrefix);
          CacheKey rowKey = null;
          Object ancestorObject = null;
          if (ancestorColumnPrefix.containsKey(nestedResultMapId)) {
            rowKey = createRowKey(nestedResultMap, rsw, ancestorColumnPrefix.get(nestedResultMapId)); // 此处创建缓存 key 时传的列名前缀和下面 else 块中 createRowKey() 传的列名前缀参数是不一样的。前者主要是看祖先缓存中有没有该对象，所以在创建缓存 key 时传的列前缀应该和之前创建祖先对象的缓存 key 时传的参数是一样的，之前创建祖先对象的缓存 key 时传的参数是保存在 ancestorColumnPrefix map 中，键是 resultMap id 。而后者创建缓存 key 时需要根据父列名前缀和当前 resultMap 的列名前缀组合来获取具体的列值来创建缓存 key 。
            ancestorObject = ancestorObjects.get(rowKey); // 这儿的 ancestorObject 是不是一定不为 null ，不是的，还有可能是为 null 的。比如在迭代结果集的第二行数据的时候 ancestorColumnPrefix 里一定会有所有 resultMapId 但并不是所有的对象都是当前属性的所属对象的祖先对象。举个具体的例子，比如 People->pets 在获取 people 对象 pets 集合中的第二个 pet 的时候，ancestorColumnPrefix 集合中包含着 resultMapId 但 ancestorObjects 集合中是没有 pet 对象的。只有在调用 applyNestedResultMappings(); 方法获取 pet 对象的属性之前（已经创建好 pet 对象）才会把 pet 对象加入到祖先对象集合中，而此时还没有创建 pet 呢。
          }
          if (ancestorObject != null) {
            if (newObject) { // todo 为什么新的对象就需要链接旧的就不需要。 // 构造一个 A 里有 People 的类，试试看是什么效果？？？？ // todo 还是不知道在哪种场景下 ancestorObject 对象不为 null 但 newObject 为 false ？？？？ 可能在这种复杂的情况下是存在的：People->pets->Pet->C->People 模式下有两行数据：1、people1->pets->pet1->c1->people1 2、people1->pets->pet2->c1->people1 好像这种情形还是无法满足条件！！！到底能不能满足好像还是不清楚，需要构造数据来验证一下，或者认真分析一下代码？？？
              linkObjects(metaObject, resultMapping, ancestorObject); // issue #385
            }
          } else {
            rowKey = createRowKey(nestedResultMap, rsw, columnPrefix);
            final CacheKey combinedKey = combineKeys(rowKey, parentRowKey); // 为什么要把两者的 key 结合起来呢？？？ 大概是不加父 key 的话会出现重复的 key ??? 有可能，比如张三的语文成绩是 80 李四的语文成绩也是 80 ，成绩表里本身是有主键的，但学生和成绩表联合查询之后在 select 语句中可能并没有查询出主键，这样一来两个语文成绩都是 80 分，它们的缓存 key 自然也是相同的，但是它们的父对象学生的缓存 key 是不同的，所以需要把父对象的缓存 key 和 子对象的缓存 key 相结合，这样一来缓存 key 就是唯一的。这样做有可能本来是同一个对象但却创建了两个或多个对象，这些对象的属性值是相同的的，但这几个对象不是同一个对象。可以见 one_to_many2 包，petHouse 是同一个，但创建了两次，不过两个对象的属性值是相同的，好像也没有什么问题。因为子 rowKey 相同的情况下，mybatis 无法判断它们是不是同一个对象。只有最外层的对象可以从 nestedResultObjects 集合中取到，因为最外层的对象是从 handleRowValuesForNestedResultMap() 方法中创建的缓存 rowKey ，其传递给 getRowValue() 方法中的 combinedKey 参数就是 rowKey 两者是相同的，所以能取到，<resultMap/> 里面嵌套的 <resultMap/> 所确定的嵌套对象在 nestedResultObjects 集合中几乎是用不到的，为什么？因为获取到内层对象时需要 combinedKey ，一般来说父 key 是不相同的，除非父父 key 也相同，则所有的父对象都相同，一般是不会出现这种情况的。
            Object rowValue = nestedResultObjects.get(combinedKey);
            boolean knownValue = (rowValue != null); // rowValue 的作用应该是为了设置 knowValue 的值，因为在下面又对 rowValue 重新赋值了
            instantiateCollectionPropertyIfAppropriate(resultMapping, metaObject); // mandatory  todo 作用 初始化属性的集合类型 例如 A 有属性 list List 则会给 list = new ArrayList();
            if (anyNotNullColumnHasValue(resultMapping, columnPrefix, rsw.getResultSet())) {
              rowValue = getRowValue(rsw, nestedResultMap, combinedKey, rowKey, columnPrefix, rowValue);// todo rowValue 为什么要重复赋值 如果从缓存中获取到值以后为什么还需要重新调用 getRowValue(); 来获取值？ 这和 handleRowValuesForNestedResultMap(); 方法中 partialObject 不为 null 的时候仍然调用 getRowValue(); 是一样的道理。虽然缓存中已经有了这个对象，但这个对象不是完整的对象。例如，例如 class A 的一个对象 a 和 People 的一个对象 people 是一一对应的，我们知道 people 里又有 pets 集合，在这种情况下虽然 people 对象已经在缓存中存在了但是 people 的 pets 集合里可能只有一个 pet 这个集合里可能有多个 pet 所以 people 虽然在缓存中存在但仍然需要去调用 getRowValue(); 方法来获取 people 中的 pets 中的元素 pet 。
              if (rowValue != null && !knownValue) { // todo 什么时候 knowValue 的值为 true ???见上一句注释，在 A 中有 People 属性，多表连接的时候已经获取到第一行数据后再次获取 people 的值的时候 people 已经在缓存中存在，所以 knowValue 的值为 true 表示的是已知的对象（缓存中存在的对象）。
                linkObjects(metaObject, resultMapping, rowValue);
                foundValues = true;
              }
            }
          }
        } catch (SQLException e) {
          throw new ExecutorException("Error getting nested result map values for '" + resultMapping.getProperty() + "'.  Cause: " + e, e);
        }
      }
    }
    return foundValues;
  }

  private String getColumnPrefix(String parentPrefix, ResultMapping resultMapping) {
    final StringBuilder columnPrefixBuilder = new StringBuilder();
    if (parentPrefix != null) {
      columnPrefixBuilder.append(parentPrefix);
    }
    if (resultMapping.getColumnPrefix() != null) {
      columnPrefixBuilder.append(resultMapping.getColumnPrefix());
    }
    return columnPrefixBuilder.length() == 0 ? null : columnPrefixBuilder.toString().toUpperCase(Locale.ENGLISH);
  }

  private boolean anyNotNullColumnHasValue(ResultMapping resultMapping, String columnPrefix, ResultSet rs) throws SQLException {
    Set<String> notNullColumns = resultMapping.getNotNullColumns();
    boolean anyNotNullColumnHasValue = true;
    if (notNullColumns != null && !notNullColumns.isEmpty()) {
      anyNotNullColumnHasValue = false;
      for (String column: notNullColumns) {
        rs.getObject(prependPrefix(column, columnPrefix));
        if (!rs.wasNull()) {
          anyNotNullColumnHasValue = true; // 非空列集合中的任何一列对应的值不为空则 anyNotNullColumnHasValue 为真。
          break;
        }
      }
    }
    return anyNotNullColumnHasValue;
  }

  private ResultMap getNestedResultMap(ResultSet rs, String nestedResultMapId, String columnPrefix) throws SQLException {
    ResultMap nestedResultMap = configuration.getResultMap(nestedResultMapId);
    return resolveDiscriminatedResultMap(rs, nestedResultMap, columnPrefix);
  }

  //
  // UNIQUE RESULT KEY
  //

  private CacheKey createRowKey(ResultMap resultMap, ResultSetWrapper rsw, String columnPrefix) throws SQLException {
    final CacheKey cacheKey = new CacheKey();
    cacheKey.update(resultMap.getId());
    List<ResultMapping> resultMappings = getResultMappingsForRowKey(resultMap);
    if (resultMappings.size() == 0) {
      if (Map.class.isAssignableFrom(resultMap.getType())) {
        createRowKeyForMap(rsw, cacheKey);
      } else {
        createRowKeyForUnmappedProperties(resultMap, rsw, cacheKey, columnPrefix);
      }
    } else {
      createRowKeyForMappedProperties(resultMap, rsw, cacheKey, resultMappings, columnPrefix);
    }
    return cacheKey;
  }

  private CacheKey combineKeys(CacheKey rowKey, CacheKey parentRowKey) {
    if (rowKey.getUpdateCount() > 1 && parentRowKey.getUpdateCount() > 1) {
      CacheKey combinedKey;
      try {
        combinedKey = rowKey.clone();
      } catch (CloneNotSupportedException e) {
        throw new ExecutorException("Error cloning cache key.  Cause: " + e, e);
      }
      combinedKey.update(parentRowKey);
      return combinedKey;
    }
    return CacheKey.NULL_CACHE_KEY;
  }

  /**
   * 获取 resultMapping 为构建 row 缓存 key 做准备工作
   * @param resultMap
   * @return
   */
  private List<ResultMapping> getResultMappingsForRowKey(ResultMap resultMap) {
    // id 当然是作为缓存 key 的理想对象
    List<ResultMapping> resultMappings = resultMap.getIdResultMappings();
    if (resultMappings.size() == 0) { // todo 应该走不到这儿，在 build resultMap 的时候如果没有 id ，就会把所有的 resultMapping 添加到 idResultMappings
      // 如果没有 id 的话则需要每个 PropertyResultMapping
      // 有可能为空，如果所有的字段映射都是通过构造方法设置的话 todo 如果 idResultMappings 为空，如果在构造方法中也传递了参数，这样一来是不是参与构造缓存 key 的属性就少了一部分，会不会有问题
      resultMappings = resultMap.getPropertyResultMappings();
    }
    return resultMappings;
  }

  /**
   * 递归调用设置一个对象的缓存 key todo 如果 a 中有 b b 中有 a 会不会造成死循环？？？？
   * @param resultMap
   * @param rsw
   * @param cacheKey
   * @param resultMappings
   * @param columnPrefix
   * @throws SQLException
   */
  private void createRowKeyForMappedProperties(ResultMap resultMap, ResultSetWrapper rsw, CacheKey cacheKey, List<ResultMapping> resultMappings, String columnPrefix) throws SQLException {
    for (ResultMapping resultMapping : resultMappings) {
      if (resultMapping.getNestedResultMapId() != null && resultMapping.getResultSet() == null) { // resultMapping.getResultSet() != null 时，其结果集是另外一个结果集而不是当前结果集，所以没法缓存
        // Issue #392
        final ResultMap nestedResultMap = configuration.getResultMap(resultMapping.getNestedResultMapId());
        createRowKeyForMappedProperties(nestedResultMap, rsw, cacheKey, nestedResultMap.getConstructorResultMappings(),
            prependPrefix(resultMapping.getColumnPrefix(), columnPrefix));
      } else if (resultMapping.getNestedQueryId() == null) { // todo 如果有嵌套查询的话，则不需要加上缓存 key ，加上缓存 key 的时候一般是把某一行数据的某几列的列名和列值都更新，那这样会不会出问题，根据缓存不能区分出是不是同一个对象
        final String column = prependPrefix(resultMapping.getColumn(), columnPrefix);
        final TypeHandler<?> th = resultMapping.getTypeHandler();
        List<String> mappedColumnNames = rsw.getMappedColumnNames(resultMap, columnPrefix);
        // Issue #114
        if (column != null && mappedColumnNames.contains(column.toUpperCase(Locale.ENGLISH))) {// todo column 是不是可能会有组合列出现的情况
          final Object value = th.getResult(rsw.getResultSet(), column);
          if (value != null) {
            cacheKey.update(column);
            cacheKey.update(value);
          }
        }
      }
    }
  }

  /**
   * todo 如果一个 resultMap 中的 idResultMappings 和 propertyResultMappings 为空的话会利用 unmappedPropertyMappings 创建缓存 key ，
   * todo idResultMapping 和 propertyResultMappings 都为空的话，那么 constructorResultMappings 应该不会空，那么为什么不用 constructorResultMappings 创建缓存 key 。
   * @param resultMap
   * @param rsw
   * @param cacheKey
   * @param columnPrefix
   * @throws SQLException
     */
  private void createRowKeyForUnmappedProperties(ResultMap resultMap, ResultSetWrapper rsw, CacheKey cacheKey, String columnPrefix) throws SQLException {
    final MetaClass metaType = MetaClass.forClass(resultMap.getType(), reflectorFactory);
    List<String> unmappedColumnNames = rsw.getUnmappedColumnNames(resultMap, columnPrefix);
    for (String column : unmappedColumnNames) {
      String property = column;
      if (columnPrefix != null && !columnPrefix.isEmpty()) {
        // When columnPrefix is specified, ignore columns without the prefix.
        // columnPrefix 与 resultMap 有关，一个结果集中可能有多个 resultMap ，所以 unmappedColumnNames 中会有些列可能没有前缀
        if (column.toUpperCase(Locale.ENGLISH).startsWith(columnPrefix)) {
          property = column.substring(columnPrefix.length());
        } else {
          continue;
        }
      }
      if (metaType.findProperty(property, configuration.isMapUnderscoreToCamelCase()) != null) {
        String value = rsw.getResultSet().getString(column);
        if (value != null) {
          cacheKey.update(column);
          cacheKey.update(value);
        }
      }
    }
  }

  /**
   * 为 resultSet 创建缓存 一个 resultSet 其实就是好多个 键值对儿（列名和对应的值） 所以需要更新每个 列名和值
   * @param rsw
   * @param cacheKey
   * @throws SQLException
   */
  private void createRowKeyForMap(ResultSetWrapper rsw, CacheKey cacheKey) throws SQLException {
    List<String> columnNames = rsw.getColumnNames();
    for (String columnName : columnNames) {
      final String value = rsw.getResultSet().getString(columnName);
      if (value != null) {
        cacheKey.update(columnName);
        cacheKey.update(value);
      }
    }
  }

  /**
   * 把 rowValue 对象链接到相关的对象上
   * @param metaObject
   * @param resultMapping
   * @param rowValue
   */
  private void linkObjects(MetaObject metaObject, ResultMapping resultMapping, Object rowValue) {
    final Object collectionProperty = instantiateCollectionPropertyIfAppropriate(resultMapping, metaObject);
    if (collectionProperty != null) {
      // 如果是集合的话，则把 rowValue 添加到集合中
      final MetaObject targetMetaObject = configuration.newMetaObject(collectionProperty);
      targetMetaObject.add(rowValue);
    } else {
      // 如果是非集合的话，直接反射调用设置值
      metaObject.setValue(resultMapping.getProperty(), rowValue);
    }
  }

  /**
   * 实例化集合属性
   * @param resultMapping
   * @param metaObject
   * @return 如果属性是集合类型则返回集合对象，否则返回 null
   */
  private Object instantiateCollectionPropertyIfAppropriate(ResultMapping resultMapping, MetaObject metaObject) {
    final String propertyName = resultMapping.getProperty();
    Object propertyValue = metaObject.getValue(propertyName);
    if (propertyValue == null) {
      Class<?> type = resultMapping.getJavaType();
      if (type == null) {
        type = metaObject.getSetterType(propertyName);
      }
      try {
        if (objectFactory.isCollection(type)) {
          propertyValue = objectFactory.create(type);
          metaObject.setValue(propertyName, propertyValue);
          return propertyValue;
        }
      } catch (Exception e) {
        throw new ExecutorException("Error instantiating collection property for result '" + resultMapping.getProperty() + "'.  Cause: " + e, e);
      }
    } else if (objectFactory.isCollection(propertyValue.getClass())) {
      // todo 设置 list 中的每个值时需要调用 instantiateCollectionPropertyIfAppropriate ，自然 list 不为空时就会走到这儿
      return propertyValue;
    }
    return null;
  }

}
