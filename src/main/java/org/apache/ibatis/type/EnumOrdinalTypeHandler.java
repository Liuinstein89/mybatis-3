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
package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Clinton Begin
 * 枚举值出现的顺序（整型，从 0 开始）与数据库中的整型值建立映射
 * 比如枚举：
 * public enum Season {
 *     SPRING,
 *     SUMMER,
 *     AUTUMN,
 *     WINTER
 * }
 * SPRING 枚举的 ordinal 值是 0
 * SUMMER 枚举的 ordinal 值是 1
 * AUTUMN 枚举的 ordinal 值是 2
 * WINTER 枚举的 ordinal 值是 3
 * 数据库中保存的值是整型值，查询出来映射为对象的时候会转换为枚举值。同理枚举值作为参数值时到了最后转化为 sql 语句中的参数时会转化会相应的整型值。
 *
 * 但这样有个缺点，比如有人想用 1 2 3 4 来对应 SPRING SUMMER AUTUMN WINTER 该怎么办呢？可以自己写 TypeHandler 来处理，和这个是类似的。
 *
 * public enum Season {
 *     SPRING(1),
 *     SUMMER(2),
 *     AUTUMN(3),
 *     WINTER(4);
 *     private int value;
 *
 *     Season(int value) {
 *         this.value = value;
 *     }
 *
 *     public int getValue() {
 *         return value;
 *     }
 * }
 *
 *
 */
public class EnumOrdinalTypeHandler<E extends Enum<E>> extends BaseTypeHandler<E> {
  // 枚举类型 class
  private Class<E> type;
  // 枚举值数组 相当于 E.values();
  private final E[] enums;

  public EnumOrdinalTypeHandler(Class<E> type) {
    if (type == null) {
      throw new IllegalArgumentException("Type argument cannot be null");
    }
    this.type = type;
    this.enums = type.getEnumConstants();
    if (this.enums == null) {
      throw new IllegalArgumentException(type.getSimpleName() + " does not represent an enum type.");
    }
  }

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
    ps.setInt(i, parameter.ordinal());
  }

  /**
   * 根据列名获取到数据库中存储的整型值，最终返回相应的枚举类型值
   * @param rs
   * @param columnName
   * @return
   * @throws SQLException
     */
  @Override
  public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
    int i = rs.getInt(columnName);
    if (rs.wasNull()) {
      return null;
    } else {
      try {
        return enums[i];
      } catch (Exception ex) {
        throw new IllegalArgumentException("Cannot convert " + i + " to " + type.getSimpleName() + " by ordinal value.", ex);
      }
    }
  }

  /**
   * 根据列的索引获取到数据库中存储的整型值，最终返回相应的枚举类型值
   * @param rs
   * @param columnIndex
   * @return
   * @throws SQLException
     */
  @Override
  public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    int i = rs.getInt(columnIndex);
    if (rs.wasNull()) {
      return null;
    } else {
      try {
        return enums[i];
      } catch (Exception ex) {
        throw new IllegalArgumentException("Cannot convert " + i + " to " + type.getSimpleName() + " by ordinal value.", ex);
      }
    }
  }

  /**
   * 根据列的索引获取到数据库中存储的整型值，最终返回相应的枚举类型值
   * @param cs
   * @param columnIndex
   * @return
   * @throws SQLException
     */
  @Override
  public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    int i = cs.getInt(columnIndex);
    if (cs.wasNull()) {
      return null;
    } else {
      try {
        return enums[i];
      } catch (Exception ex) {
        throw new IllegalArgumentException("Cannot convert " + i + " to " + type.getSimpleName() + " by ordinal value.", ex);
      }
    }
  }
  
}
