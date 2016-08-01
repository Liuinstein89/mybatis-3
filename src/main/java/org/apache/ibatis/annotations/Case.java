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
package org.apache.ibatis.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Clinton Begin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Case {
  String value();

  Class<?> type();

  // 不同的 Case 它们返回的列可能会不同所以需要不同的 Result 映射，一般使用到鉴别器的情况是基类、子类，Case 确定了子类的类型，
  // 每个子类的属性可能不相同所以才会有 Result[] 映射
  Result[] results() default {};

  // 鉴别器会根据不同的 Case 来调用不同的类的构造方法创建对象，所以会有构造方法
  Arg[] constructArgs() default {};
}
