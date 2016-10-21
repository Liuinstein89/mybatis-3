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
package shfq.nested_result_map.mappers;

import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.type.IntegerTypeHandler;
import org.apache.ibatis.type.JdbcType;
import shfq.nested_result_map.vo.Address;

/**
 * author:      shfq
 * description:
 * create date: 2016/7/28.
 */
public interface AddressMapper {
    @Select("SELECT * FROM address WHERE id=#{id}")
    @ConstructorArgs(value = {
            @Arg(id = true, column = "id", javaType = int.class, jdbcType = JdbcType.INTEGER, typeHandler = IntegerTypeHandler.class)})
    Address selectAddress(int id);

}
