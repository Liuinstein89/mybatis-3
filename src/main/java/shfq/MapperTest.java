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

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;

/**
 * author:      shfq
 * description:
 * create date: 2016/7/11.
 */
public class MapperTest {
    public static void main(String[] args) {
        Student student = query();
        System.out.println("");

    }
    private static Student query() {
        SqlSession session = null;
        try {
            Reader reader = Resources.getResourceAsReader("shfq/mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            session = sqlSessionFactory.openSession();
            StudentMapperTest studentMapperTest = session.getMapper(StudentMapperTest.class);
//            Student student = new Student();
//            student.setEmail("shfq@163.com");
//            student.setName("shfq");
//            student.setAddress("shanxi");
//
//            studentMapper.add();
            Student student = new Student();
            student.setName("shfq111");
            student.setEmail("shfq@gmail.com");
            student.setId(10);
            int i = studentMapperTest.update(student);
            session.commit();
            session.close();

            return student;
        } catch (IOException e) {
            e.printStackTrace();
            if (session != null) {
                session.rollback();
                session.close();
            }

            return null;
        } finally {
        }
    }
}
