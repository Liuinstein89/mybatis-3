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
package shfq.discriminator.mappers;

import org.apache.ibatis.annotations.Select;
import shfq.discriminator.vo.Vehicle;

/**
 * author:      shfq
 * description:
 * create date: 2016/8/1.
 */
public interface VehicleMapper {
//    @TypeDiscriminator(column = "type", cases = {
//                                                  @Case(value = "car", type = Car.class, results = {@Result(id = true, column = "id", property = "id", javaType = int.class)}),
//                                                  @Case(value = "bus", type= Bus.class)})
//
//    @Select("SELECT * FROM vehicle WHERE id=#{id}")
    @Select("select * from vehicle where id=#{id}")
    Vehicle selectVehicle(int id);

}
