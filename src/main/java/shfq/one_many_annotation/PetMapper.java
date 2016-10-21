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
package shfq.one_many_annotation;

import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * author:      shfq
 * description:
 * create date: 2016/8/2.
 */
public interface PetMapper {
    @Results(value = {@Result(column = "owner_id", property = "owner", one = @One(select = "shfq.one_many_annotation.PeopleMapper.selectPeople"))})
    @Select("select * from pet where owner_id=#{ownerId}")
    List<Pet> selectPets(int ownerId);

    @Results(value = {@Result(column = "owner_id", property = "owner", one = @One(select = "shfq.one_many_annotation.PeopleMapper.selectPeople"))})
    @Select("select * from pet where id=#{id}")
    Pet selectPet(int id);


}
