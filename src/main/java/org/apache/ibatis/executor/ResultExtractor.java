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
package org.apache.ibatis.executor;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.Array;
import java.util.List;

/**
 * @author Andrew Gustafson
 * 把查询出来的 List 转换为目标形式
 */
public class ResultExtractor {
  private final Configuration configuration;
  private final ObjectFactory objectFactory;

  public ResultExtractor(Configuration configuration, ObjectFactory objectFactory) {
    this.configuration = configuration;
    this.objectFactory = objectFactory;
  }

  /**
   * 把查询出来的 List 转换为目标形式 map 类型是怎么处理的？
   * @param list
   * @param targetType
   * @return
     */
  public Object extractObjectFromList(List<Object> list, Class<?> targetType) {
    Object value = null;
    // targetType 如果是 List 类型或这是 List 类型的父类型
    if (targetType != null && targetType.isAssignableFrom(list.getClass())) {
      value = list;
    } else if (targetType != null && objectFactory.isCollection(targetType)) {
      // 集合类型
      value = objectFactory.create(targetType);
      MetaObject metaObject = configuration.newMetaObject(value);
      metaObject.addAll(list);
    } else if (targetType != null && targetType.isArray()) {
      // 数组类型
      Class<?> arrayComponentType = targetType.getComponentType();
      Object array = Array.newInstance(arrayComponentType, list.size());
      if (arrayComponentType.isPrimitive()) {
        // 原生类型
        for (int i = 0; i < list.size(); i++) {
          Array.set(array, i, list.get(i));
        }
        value = array;
      } else {
        // 对象类型
        value = list.toArray((Object[])array);
      }
    } else {
      if (list != null && list.size() > 1) {
        throw new ExecutorException("Statement returned more than one row, where no more than one was expected.");
      } else if (list != null && list.size() == 1) {
        // 单个对象
        value = list.get(0);
      }
    }
    return value;
  }
}
