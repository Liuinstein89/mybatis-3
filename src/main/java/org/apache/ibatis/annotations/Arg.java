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

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.UnknownTypeHandler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Clinton Begin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
// column 和 javaType 是必填的，其他可以选填 javaType 如果不填的话，在调用构造方法的时候，mybatis 认为这个参数的类型是 Object ，如果构造方法的参数不是 Object 类型的话就会报错
// typeHandler 会根据 javaType 查出来的，多种 jdbcType 对应着同一种 javaType 比如 VARCHAR 、null 、NCHAR 、CHAR 、LONGVARCHAR 、NCLOB 、CLOB 、NVARCHAR 都对应着 String.class
// 一个 Arg 最终会转化为一个 ResultMapping
public @interface Arg {
  boolean id() default false;

  String column() default "";

  Class<?> javaType() default void.class;

  JdbcType jdbcType() default JdbcType.UNDEFINED;

  Class<? extends TypeHandler<?>> typeHandler() default UnknownTypeHandler.class;

  // todo select 和 resultMap 这两个方法是干什么用的？
  String select() default "";

  String resultMap() default "";
}
