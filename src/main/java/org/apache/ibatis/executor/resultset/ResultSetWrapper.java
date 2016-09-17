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

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.*;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * @author Iwao AVE!
 * 对 ResultSet 的封装
 */
class ResultSetWrapper {

  private final ResultSet resultSet;
  private final TypeHandlerRegistry typeHandlerRegistry;
  // 结果集中返回的所有的列的别名或列名
  private final List<String> columnNames = new ArrayList<String>();
  // 列的 java 类型
  private final List<String> classNames = new ArrayList<String>();
  // 列的 jdbc 类型
  private final List<JdbcType> jdbcTypes = new ArrayList<JdbcType>();
  // todo 为什么要设计成这样？ 键是一个列的名称，值是 map
  // todo 难道是因为 discriminator 的缘故，同一个列，值不同，返回的类型也不同?应该不是这样的原因
  private final Map<String, Map<Class<?>, TypeHandler<?>>> typeHandlerMap = new HashMap<String, Map<Class<?>, TypeHandler<?>>>();

  // 假设 Map<String, List<String>> 中的 值一个 list 是 mappedColumnNames，则 mappedColumnNames + unMappedColumnNames = columnNames
  //  其中 mappedColumnNames 是根据 resultMap 中的 mappedColumns 来决定的 mappedColumnNames 的每一个值一定是某一个 resultMap 所映射的列名，但
  // unMappedColumnNames 却不一定是，它里面可能包含了其他 resultMap 映射的所有的列。
  // mappedColumnNames
  // 为什么要设计成这样的结构呢，即 Map 里的值是 List 因为一个结果集里可能需要映射多个 ResultMap 一个 ResultMap 需要对应一个 List
  // 键是 resultMap + : + columnPrefix 组成 某个 resultMap 中需要映射的所有列
  private Map<String, List<String>> mappedColumnNamesMap = new HashMap<String, List<String>>();
  // 为什么要设计成这样的结构呢，即 Map 里的值是 List 因为一个结果集里可能需要映射多个 ResultMap 一个 ResultMap 需要对应一个 List
  // 某个 resultMap 中不需要映射的列，即 columnNames - mappedColumnNames(某个 resultMap 中 mappedCoulumnNames)
  private Map<String, List<String>> unMappedColumnNamesMap = new HashMap<String, List<String>>();

  public ResultSetWrapper(ResultSet rs, Configuration configuration) throws SQLException {
    super();
    this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
    this.resultSet = rs;
    final ResultSetMetaData metaData = rs.getMetaData();
    final int columnCount = metaData.getColumnCount();
    for (int i = 1; i <= columnCount; i++) {
      columnNames.add(configuration.isUseColumnLabel() ? metaData.getColumnLabel(i) : metaData.getColumnName(i));
      jdbcTypes.add(JdbcType.forCode(metaData.getColumnType(i)));
      classNames.add(metaData.getColumnClassName(i));
    }
  }

  public ResultSet getResultSet() {
    return resultSet;
  }

  public List<String> getColumnNames() {
    return this.columnNames;
  }

  public List<String> getClassNames() {
    return Collections.unmodifiableList(classNames);
  }

  /**
   * Gets the type handler to use when reading the result set.
   * Tries to get from the TypeHandlerRegistry by searching for the property type.
   * If not found it gets the column JDBC type and tries to get a handler for it.
   * 读取结果集中的值时会获取 handler
   * 先会根据属性名称试着从 TypeHandlerRegistry 中查找相应的 typeHandler
   * // todo ????
   *
   * @param propertyType
   * @param columnName
   * @return
   */
  public TypeHandler<?> getTypeHandler(Class<?> propertyType, String columnName) {
    TypeHandler<?> handler = null;
    Map<Class<?>, TypeHandler<?>> columnHandlers = typeHandlerMap.get(columnName);
    if (columnHandlers == null) {
      columnHandlers = new HashMap<Class<?>, TypeHandler<?>>();
      typeHandlerMap.put(columnName, columnHandlers);
    } else {
      handler = columnHandlers.get(propertyType);
    }
    if (handler == null) {
      handler = typeHandlerRegistry.getTypeHandler(propertyType);
      // Replicate logic of UnknownTypeHandler#resolveTypeHandler
      // See issue #59 comment 10
      if (handler == null || handler instanceof UnknownTypeHandler) {
        final int index = columnNames.indexOf(columnName);
        final JdbcType jdbcType = jdbcTypes.get(index);
        final Class<?> javaType = resolveClass(classNames.get(index));
        if (javaType != null && jdbcType != null) {
          handler = typeHandlerRegistry.getTypeHandler(javaType, jdbcType);
        } else if (javaType != null) {
          handler = typeHandlerRegistry.getTypeHandler(javaType);
        } else if (jdbcType != null) {
          handler = typeHandlerRegistry.getTypeHandler(jdbcType);
        }
      }
      if (handler == null || handler instanceof UnknownTypeHandler) {
        handler = new ObjectTypeHandler();
      }
      columnHandlers.put(propertyType, handler);
    }
    return handler;
  }

  private Class<?> resolveClass(String className) {
    try {
      return Resources.classForName(className);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }


  /**
   * 加载某个 resultMap 的 mappedColumnNames 和 unmappedColumnNames
   * @param resultMap
   * @param columnPrefix
   * @throws SQLException
     */
  private void loadMappedAndUnmappedColumnNames(ResultMap resultMap, String columnPrefix) throws SQLException {
    List<String> mappedColumnNames = new ArrayList<String>();
    List<String> unmappedColumnNames = new ArrayList<String>();
    final String upperColumnPrefix = columnPrefix == null ? null : columnPrefix.toUpperCase(Locale.ENGLISH);
    final Set<String> mappedColumns = prependPrefixes(resultMap.getMappedColumns(), upperColumnPrefix);
    for (String columnName : columnNames) {
      final String upperColumnName = columnName.toUpperCase(Locale.ENGLISH);
      if (mappedColumns.contains(upperColumnName)) {
        mappedColumnNames.add(upperColumnName);
      } else {
        unmappedColumnNames.add(columnName);
      }
    }
    mappedColumnNamesMap.put(getMapKey(resultMap, columnPrefix), mappedColumnNames);
    unMappedColumnNamesMap.put(getMapKey(resultMap, columnPrefix), unmappedColumnNames);
  }

  public List<String> getMappedColumnNames(ResultMap resultMap, String columnPrefix) throws SQLException {
    List<String> mappedColumnNames = mappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
    if (mappedColumnNames == null) {
      loadMappedAndUnmappedColumnNames(resultMap, columnPrefix);
      mappedColumnNames = mappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
    }
    return mappedColumnNames;
  }

  public List<String> getUnmappedColumnNames(ResultMap resultMap, String columnPrefix) throws SQLException {
    List<String> unMappedColumnNames = unMappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
    if (unMappedColumnNames == null) {
      loadMappedAndUnmappedColumnNames(resultMap, columnPrefix);
      unMappedColumnNames = unMappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
    }
    return unMappedColumnNames;
  }

  private String getMapKey(ResultMap resultMap, String columnPrefix) {
    return resultMap.getId() + ":" + columnPrefix;
  }

  /**
   * 迭代 columnNames 并添加带有前缀的列名
   * @param columnNames
   * @param prefix
   * @return
     */
  private Set<String> prependPrefixes(Set<String> columnNames, String prefix) {
    if (columnNames == null || columnNames.isEmpty() || prefix == null || prefix.length() == 0) {
      return columnNames;
    }
    final Set<String> prefixed = new HashSet<String>();
    for (String columnName : columnNames) {
      prefixed.add(prefix + columnName);
    }
    return prefixed;
  }
  
}
