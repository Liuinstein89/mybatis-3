/**
 * Copyright 2009-2015 the original author or authors.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package shfq;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * author:      shfq
 * create date: 2015/12/22.
 */
public class Test {
    public static void main(String[] args) {

        try {
        testQuery();
//        testInsert();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void testInsert() {
        try {
            Reader reader = Resources.getResourceAsReader("shfq/mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession session = sqlSessionFactory.openSession();

            //Create a new student object
            Student student = new Student("Mohammad", "It", 80, 984803322, "Mohammad@gmail.com");

            //Insert student data
            session.insert("Student.insert", student);
            System.out.println("record inserted successfully");
            session.commit();
            session.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void testQuery() {
        try {
            Reader reader = Resources.getResourceAsReader("shfq/mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession session = sqlSessionFactory.openSession();
            // query student data
            Student student = session.selectOne("Student.getById", 10);
            System.out.println("record queried successfully");
            session.commit();
            session.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void test() {
        try {
            Connection connection = DriverManager.getConnection("jdbc:mysql://192.168.0.78:3306/shfqtest", "root", "123456");
            PreparedStatement statement = connection.prepareStatement("insert student VALUES (?, ?, ?, ?)");
            System.out.println(statement.toString());
            statement.setInt(1, 2000);
            statement.setString(2, "shfq");
            statement.setInt(3, 28);
            statement.setFloat(4, 175);
            int i = statement.executeUpdate();
            System.out.println(i);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
