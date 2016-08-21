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
package shfq;

import org.apache.ibatis.annotations.*;
import shfq.nested_result_map.vo.Student;

/**
 * author:      shfq
 * description:
 * create date: 2016/7/11.
 */
public interface StudentMapperTest {
//    @Delete("delete from student where id=#{id}")
//    int delete(Student student);

    @Update("update student set name=#{name}, email=#{email} where id=#{id}")
    int update(Student student);
//
//    @Insert("insert into student(name, branch, percentage, phone, email, address) values (#{name}, #{branch}, #{percentage}, #{phone}, #{email}, #{address})")
//    int add(Student student);

//    @Results({@Result(property = "name", column="name"),
//              @Result(property = "branch", column = "branch"),
//              @Result(property = "percentage", column = "percentage"),
//              @Result(property = "phone", column = "phone"),
//              @Result(property = "email", column = "email")
//    })
//    @Select("select name, branch, percentage, phone, email from student where id=#{id}")
//    Student queryById(Student student);
}


