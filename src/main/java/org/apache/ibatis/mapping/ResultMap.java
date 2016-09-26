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
package org.apache.ibatis.mapping;

import org.apache.ibatis.session.Configuration;

import java.util.*;

/**
 * @author Clinton Begin
 * ResultMap 是对查询出来的结果集到返回对象整体映射所需的条件的一个封装 resultMap 可能是 <resultMap> <case> <association> <collection>
 *
 */
public class ResultMap {
  // 如果是 mapper 的话 id 是 包名.类名.方法名-参数类型
  // 关联着一条 sql 语句
  private String id;
  // 返回的对象的类型 如果返回的是集合的话，type 则是集合元素的类型
  private Class<?> type;
  private List<ResultMapping> resultMappings;
  // 在 mapper 中 @ConstructorArgs 中如果有 @Arg id=true 的话，则添加在 list 中，如果 list 为空的话，则把所有的 ResultMapping 都添加到 list 中 idResultMappings 主要用于本地缓存，如果没有 idResultMapping 的话，则把所有的列都作为 idResultMappings，因为如果有 id 的话 id 可以唯一的区分查出来的这一行数据，如果没有的话则把所有的列都需要加入
  private List<ResultMapping> idResultMappings;
  // 在 mapper 中 @ConstructorArgs 中出现的都属于 constructorResultMappings
  // 同一个 ResultMapping 不会同时出现在 constructorResultMappings 和 propertyResultMappings 两个 list 中
  // 为什么不会同时出现呢？constructorResultMappings 会在创建构造方法的时候设置属性值，如果在构造方法中出现过的当然不需要再次重新设置属性值 constructorResultMappings + propertyResultMappings = resultMapping
  private List<ResultMapping> constructorResultMappings;
  // 属性 resultMapping 可能会空，如果所有的映射都是通过构造方法进行的话
  private List<ResultMapping> propertyResultMappings;
  // todo 需要映射的列集合，比如一个 select * 可能查询出好多列，但我只想映射其中的两列，多余的可以不映射。
  private Set<String> mappedColumns;
  private Discriminator discriminator;
  private boolean hasNestedResultMaps; // todo resultMap 里是否有嵌套 resultMap ，如果一个 resultMap 里的任一个 resultMapping 里有 resultMap 并且 resultSet 为空的话 好像还有其他的特殊情况，会调用 forceNestedResultMaps() 方法 就算有嵌套映射 为什么还需要有 resultSet 为空的条件，可能是 resultMap 和它的嵌套 resultMap 映射的是同一个结果集中的数据。有了 resultSet 后 resultMap 和它的 resultMap 是从不同的结果集中抽取数据。
  // todo 有没有嵌套查询 例如在 mapper 中有 annotation @One @Many 就是嵌套查询???? resultMap 里 只要有一个 resultMapping 有嵌套查询则该 resultMap 是有嵌套查询的
  private boolean hasNestedQueries;
  private Boolean autoMapping;

  private ResultMap() {
  }

  public static class Builder {
    private ResultMap resultMap = new ResultMap();

    public Builder(Configuration configuration, String id, Class<?> type, List<ResultMapping> resultMappings) {
      this(configuration, id, type, resultMappings, null);
    }

    public Builder(Configuration configuration, String id, Class<?> type, List<ResultMapping> resultMappings, Boolean autoMapping) {
      resultMap.id = id;
      resultMap.type = type;
      resultMap.resultMappings = resultMappings;
      resultMap.autoMapping = autoMapping;
    }

    public Builder discriminator(Discriminator discriminator) {
      resultMap.discriminator = discriminator;
      return this;
    }

    public Class<?> type() {
      return resultMap.type;
    }

    public ResultMap build() {
      if (resultMap.id == null) {
        throw new IllegalArgumentException("ResultMaps must have an id");
      }
      dd
      resultMap.mappedColumns = new HashSet<String>();
      resultMap.idResultMappings = new ArrayList<ResultMapping>();
      resultMap.constructorResultMappings = new ArrayList<ResultMapping>();
      resultMap.propertyResultMappings = new ArrayList<ResultMapping>();
      for (ResultMapping resultMapping : resultMap.resultMappings) {
        resultMap.hasNestedQueries = resultMap.hasNestedQueries || resultMapping.getNestedQueryId() != null;
        resultMap.hasNestedResultMaps = resultMap.hasNestedResultMaps || (resultMapping.getNestedResultMapId() != null && resultMapping.getResultSet() == null);
        final String column = resultMapping.getColumn();
        if (column != null) {
          resultMap.mappedColumns.add(column.toUpperCase(Locale.ENGLISH));
        } else if (resultMapping.isCompositeResult()) {
          for (ResultMapping compositeResultMapping : resultMapping.getComposites()) {
            final String compositeColumn = compositeResultMapping.getColumn();
            if (compositeColumn != null) {
              resultMap.mappedColumns.add(compositeColumn.toUpperCase(Locale.ENGLISH));
            }
          }
        }
        if (resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR)) {
          resultMap.constructorResultMappings.add(resultMapping);
        } else {
          resultMap.propertyResultMappings.add(resultMapping);
        }
        if (resultMapping.getFlags().contains(ResultFlag.ID)) {
          resultMap.idResultMappings.add(resultMapping);
        }
      }
      if (resultMap.idResultMappings.isEmpty()) {
        // 为什么要全部添加？ 因为 idResultMappings 主要用于本地缓存，如果没有 idResultMapping 的话，则把所有的列都作为 idResultMappings，因为如果有 id 的话 id 可以唯一的区分查出来的这一行数据，如果没有的话则把所有的列都需要加入
        resultMap.idResultMappings.addAll(resultMap.resultMappings);
      }
      // lock down collections
      resultMap.resultMappings = Collections.unmodifiableList(resultMap.resultMappings);
      resultMap.idResultMappings = Collections.unmodifiableList(resultMap.idResultMappings);
      resultMap.constructorResultMappings = Collections.unmodifiableList(resultMap.constructorResultMappings);
      resultMap.propertyResultMappings = Collections.unmodifiableList(resultMap.propertyResultMappings);
      resultMap.mappedColumns = Collections.unmodifiableSet(resultMap.mappedColumns);
      return resultMap;
    }
  }

  public String getId() {
    return id;
  }

  public boolean hasNestedResultMaps() {
    return hasNestedResultMaps;
  }

  public boolean hasNestedQueries() {
    return hasNestedQueries;
  }

  public Class<?> getType() {
    return type;
  }

  public List<ResultMapping> getResultMappings() {
    return resultMappings;
  }

  public List<ResultMapping> getConstructorResultMappings() {
    return constructorResultMappings;
  }

  public List<ResultMapping> getPropertyResultMappings() {
    return propertyResultMappings;
  }

  public List<ResultMapping> getIdResultMappings() {
    return idResultMappings;
  }

  public Set<String> getMappedColumns() {
    return mappedColumns;
  }

  public Discriminator getDiscriminator() {
    return discriminator;
  }

  public void forceNestedResultMaps() {
    hasNestedResultMaps = true;
  }
  
  public Boolean getAutoMapping() {
    return autoMapping;
  }

}
