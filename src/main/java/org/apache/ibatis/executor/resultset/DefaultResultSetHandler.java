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
  private final Map<CacheKey, Object> nestedResultObjects = new HashMap<CacheKey, Object>();
  private final Map<CacheKey, Object> ancestorObjects = new HashMap<CacheKey, Object>();
  private final Map<String, String> ancestorColumnPrefix = new HashMap<String, String>();

  // multiple resultsets
  private final Map<String, ResultMapping> nextResultMaps = new HashMap<String, ResultMapping>();
  private final Map<CacheKey, List<PendingRelation>> pendingRelations = new HashMap<CacheKey, List<PendingRelation>>(); // todo 一个 cacheKey 其实代表了一个 result 对象，同一个 result 对象可能需要多次链接到父对象上，比如 A 有属性 B1 和 B2，其中属性 B1 和 B2 是同一个对象

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

    // todo 返回值
    final List<Object> multipleResults = new ArrayList<Object>();

    int resultSetCount = 0;
    ResultSetWrapper rsw = getFirstResultSet(stmt);

    // todo 什么时候会出现多个 resultMap 呢？？？ 存储过程？？？
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
          handleResultSet(rsw, resultMap, null, parentMapping);
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
        handleRowValues(rsw, resultMap, null, RowBounds.DEFAULT, parentMapping);
      } else {
        // todo 什么时候 resultHandler 为 null？为什么 resultHandler 为 null 的时候 需要 multipleResults.add
        // 而resultHandler 不为 null 的时候 却不需要？？？
        if (resultHandler == null) {
          DefaultResultHandler defaultResultHandler = new DefaultResultHandler(objectFactory);
          // todo 处理返回的一个结果集，一个结果集中可能有多条记录
          handleRowValues(rsw, resultMap, defaultResultHandler, rowBounds, null);
          multipleResults.add(defaultResultHandler.getResultList());
        } else {
          handleRowValues(rsw, resultMap, resultHandler, rowBounds, null);
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
  // todo 处理一个结果集中的所有记录
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

  // todo 处理简单 resultMap 只含有一个简单对象
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

  // todo 处理一个结果集中的一行数据 把一行数据映射为一个对象
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
      // todo 属性映射和自动映射的顺序能不能交换???
      foundValues = applyPropertyMappings(rsw, resultMap, metaObject, lazyLoader, null) || foundValues;
      foundValues = lazyLoader.size() > 0 || foundValues;
      resultObject = foundValues ? resultObject : null;
      return resultObject;
    }
    return resultObject;
  }

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
  // PROPERTY MAPPINGS
  //

  private boolean applyPropertyMappings(ResultSetWrapper rsw, ResultMap resultMap, MetaObject metaObject, ResultLoaderMap lazyLoader, String columnPrefix)
      throws SQLException {
    final List<String> mappedColumnNames = rsw.getMappedColumnNames(resultMap, columnPrefix); // todo 什么时候 mappedColumnNames 不为空？？？
    boolean foundValues = false;
    final List<ResultMapping> propertyMappings = resultMap.getPropertyResultMappings();
    for (ResultMapping propertyMapping : propertyMappings) {
      String column = prependPrefix(propertyMapping.getColumn(), columnPrefix);
      if (propertyMapping.getNestedResultMapId() != null) {
        // todo 为什么要忽略？？？？ 什么时候可以忽略
        // the user added a column attribute to a nested result map, ignore it
        column = null;
      }
      if (propertyMapping.isCompositeResult()
          || (column != null && mappedColumnNames.contains(column.toUpperCase(Locale.ENGLISH)))
          || propertyMapping.getResultSet() != null) { // todo mappedColumnNames 是干啥的？？？ 为啥 mappedColumnNames.contains(column.toUpperCase(Locale.ENGLISH))
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
   * 根据属性名称获取到相应的值
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
      // todo propertyMapping.getResultSet() != null 这种情况是哪种情况???? 有存储过程的时候
      addPendingChildRelation(rs, metaResultObject, propertyMapping);   // TODO is that OK?
      return DEFERED;
    } else {
      // 属性类型是原生类型，属性值直接在 resultSet 中获取
      final TypeHandler<?> typeHandler = propertyMapping.getTypeHandler();
      final String column = prependPrefix(propertyMapping.getColumn(), columnPrefix);
      return typeHandler.getResult(rs, column);
    }
  }

  // todo 给对象设置所有的属性值 没有映射过的列名设置属性值
  private boolean applyAutomaticMappings(ResultSetWrapper rsw, ResultMap resultMap, MetaObject metaObject, String columnPrefix) throws SQLException {
    final List<String> unmappedColumnNames = rsw.getUnmappedColumnNames(resultMap, columnPrefix);
    boolean foundValues = false;
    // todo 什么时候 unmappedColumnNames 不为空？？？？
    for (String columnName : unmappedColumnNames) {
      String propertyName = columnName;
      if (columnPrefix != null && !columnPrefix.isEmpty()) {
        // When columnPrefix is specified,
        // ignore columns without the prefix.
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
          // issue #377, call setter on nulls
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
   * 其实是相当于给 result 对象创建了 cacheKey 。如果 names 是 id 的话，如果不是则表示的应该是所有的属性，所有的属性都一样，才表示对象相同
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
    if (resultObject != null && !typeHandlerRegistry.hasTypeHandler(resultMap.getType())) {
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
        if (constructorMapping.getNestedQueryId() != null) {
          value = getNestedQueryConstructorValue(rsw.getResultSet(), constructorMapping, columnPrefix);
        } else if (constructorMapping.getNestedResultMapId() != null) {
          final ResultMap resultMap = configuration.getResultMap(constructorMapping.getNestedResultMapId());
          value = getRowValue(rsw, resultMap);
        } else {
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
      if (mappedStatement.isResultOrdered()) {
        if (partialObject == null && rowValue != null) {
          nestedResultObjects.clear();
          storeObject(resultHandler, resultContext, rowValue, parentMapping, rsw.getResultSet());
        }
        rowValue = getRowValue(rsw, discriminatedResultMap, rowKey, rowKey, null, partialObject);
      } else {
        rowValue = getRowValue(rsw, discriminatedResultMap, rowKey, rowKey, null, partialObject);
        if (partialObject == null) {
          storeObject(resultHandler, resultContext, rowValue, parentMapping, rsw.getResultSet());
        }
      }
    }
    if (rowValue != null && mappedStatement.isResultOrdered() && shouldProcessMoreRows(resultContext, rowBounds)) {
      storeObject(resultHandler, resultContext, rowValue, parentMapping, rsw.getResultSet());
    }
  }

  //
  // GET VALUE FROM ROW FOR NESTED RESULT MAP
  //

  private Object getRowValue(ResultSetWrapper rsw, ResultMap resultMap, CacheKey combinedKey, CacheKey absoluteKey, String columnPrefix, Object partialObject) throws SQLException {
    final String resultMapId = resultMap.getId();
    Object resultObject = partialObject;
    if (resultObject != null) {
      final MetaObject metaObject = configuration.newMetaObject(resultObject);
      putAncestor(absoluteKey, resultObject, resultMapId, columnPrefix);
      applyNestedResultMappings(rsw, resultMap, metaObject, columnPrefix, combinedKey, false);
      ancestorObjects.remove(absoluteKey);
    } else {
      final ResultLoaderMap lazyLoader = new ResultLoaderMap();
      resultObject = createResultObject(rsw, resultMap, lazyLoader, columnPrefix);
      if (resultObject != null && !typeHandlerRegistry.hasTypeHandler(resultMap.getType())) {
        final MetaObject metaObject = configuration.newMetaObject(resultObject);
        boolean foundValues = !resultMap.getConstructorResultMappings().isEmpty();
        if (shouldApplyAutomaticMappings(resultMap, true)) {
          foundValues = applyAutomaticMappings(rsw, resultMap, metaObject, columnPrefix) || foundValues;
        }
        foundValues = applyPropertyMappings(rsw, resultMap, metaObject, lazyLoader, columnPrefix) || foundValues;
        putAncestor(absoluteKey, resultObject, resultMapId, columnPrefix);
        foundValues = applyNestedResultMappings(rsw, resultMap, metaObject, columnPrefix, combinedKey, true) || foundValues;
        ancestorObjects.remove(absoluteKey);
        foundValues = lazyLoader.size() > 0 || foundValues;
        resultObject = foundValues ? resultObject : null;
      }
      if (combinedKey != CacheKey.NULL_CACHE_KEY) {
        nestedResultObjects.put(combinedKey, resultObject);
      }
    }
    return resultObject;
  }

  private void putAncestor(CacheKey rowKey, Object resultObject, String resultMapId, String columnPrefix) {
    if (!ancestorColumnPrefix.containsKey(resultMapId)) {
      ancestorColumnPrefix.put(resultMapId, columnPrefix);
    }
    ancestorObjects.put(rowKey, resultObject);
  }

  //
  // NESTED RESULT MAP (JOIN MAPPING)
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
            rowKey = createRowKey(nestedResultMap, rsw, ancestorColumnPrefix.get(nestedResultMapId));
            ancestorObject = ancestorObjects.get(rowKey);
          }
          if (ancestorObject != null) {
            if (newObject) {
              linkObjects(metaObject, resultMapping, ancestorObject); // issue #385
            }
          } else {
            rowKey = createRowKey(nestedResultMap, rsw, columnPrefix);
            final CacheKey combinedKey = combineKeys(rowKey, parentRowKey);
            Object rowValue = nestedResultObjects.get(combinedKey);
            boolean knownValue = (rowValue != null);
            instantiateCollectionPropertyIfAppropriate(resultMapping, metaObject); // mandatory            
            if (anyNotNullColumnHasValue(resultMapping, columnPrefix, rsw.getResultSet())) {
              rowValue = getRowValue(rsw, nestedResultMap, combinedKey, rowKey, columnPrefix, rowValue);
              if (rowValue != null && !knownValue) {
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
          anyNotNullColumnHasValue = true;
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
   * 为了构建 rowKey 而获取 resultMapping
   * @param resultMap
   * @return
   */
  private List<ResultMapping> getResultMappingsForRowKey(ResultMap resultMap) {
    // id 当然是作为缓存 key 的理想对象
    List<ResultMapping> resultMappings = resultMap.getIdResultMappings();
    if (resultMappings.size() == 0) {
      // 如果没有 id 的话则需要每个 PropertyResultMapping
      resultMappings = resultMap.getPropertyResultMappings();
    }
    return resultMappings;
  }

  private void createRowKeyForMappedProperties(ResultMap resultMap, ResultSetWrapper rsw, CacheKey cacheKey, List<ResultMapping> resultMappings, String columnPrefix) throws SQLException {
    for (ResultMapping resultMapping : resultMappings) {
      if (resultMapping.getNestedResultMapId() != null && resultMapping.getResultSet() == null) {
        // Issue #392
        final ResultMap nestedResultMap = configuration.getResultMap(resultMapping.getNestedResultMapId());
        createRowKeyForMappedProperties(nestedResultMap, rsw, cacheKey, nestedResultMap.getConstructorResultMappings(),
            prependPrefix(resultMapping.getColumnPrefix(), columnPrefix));
      } else if (resultMapping.getNestedQueryId() == null) {
        final String column = prependPrefix(resultMapping.getColumn(), columnPrefix);
        final TypeHandler<?> th = resultMapping.getTypeHandler();
        List<String> mappedColumnNames = rsw.getMappedColumnNames(resultMap, columnPrefix);
        // Issue #114
        if (column != null && mappedColumnNames.contains(column.toUpperCase(Locale.ENGLISH))) {
          final Object value = th.getResult(rsw.getResultSet(), column);
          if (value != null) {
            cacheKey.update(column);
            cacheKey.update(value);
          }
        }
      }
    }
  }

  private void createRowKeyForUnmappedProperties(ResultMap resultMap, ResultSetWrapper rsw, CacheKey cacheKey, String columnPrefix) throws SQLException {
    final MetaClass metaType = MetaClass.forClass(resultMap.getType(), reflectorFactory);
    List<String> unmappedColumnNames = rsw.getUnmappedColumnNames(resultMap, columnPrefix);
    for (String column : unmappedColumnNames) {
      String property = column;
      if (columnPrefix != null && !columnPrefix.isEmpty()) {
        // When columnPrefix is specified, ignore columns without the prefix.
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
