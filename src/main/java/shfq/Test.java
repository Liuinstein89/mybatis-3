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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * author:      shfq
 * create date: 2015/12/22.
 */
public class Test {
    public static void main(String[] args) {
//        try {
//            update();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        Integer[] ints =  { 6, 10, 13, 5, 8, 3, 2, 11 };
        List<Integer> list = Arrays.asList(ints);
        quickSort(list, 0, list.size()-1);
        for (int value : list) {
            System.out.println(value);
            System.out.println("");
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
            Student student = session.selectOne("Student.getById", 9);
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

    private static void queryByMapper() {
        SqlSession session = null;
        try {
            Reader reader = Resources.getResourceAsReader("shfq/mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            session = sqlSessionFactory.openSession();
            StudentMapper mapper = session.getMapper(StudentMapper.class);
            Student student = mapper.selectStudent(20);
            System.out.println("");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (session != null) {
                session.close();
            }
        }

    }

    private static void deleteById() {
        SqlSession session = null;
        try {
            Reader reader = Resources.getResourceAsReader("shfq/mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            session = sqlSessionFactory.openSession();
            int count = session.delete("Student.deleteStudentById", 9);
            System.out.println(count + " record was deleted");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (session != null) {
                session.close();
            }
        }

    }
    private static void update() {
        SqlSession session = null;
        try {
            Student student = new Student();
            student.setId(10);
//            student.setName("shfq");
            student.setEmail("shanbeirenshfq@163.com");

            Reader reader = Resources.getResourceAsReader("shfq/mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            session = sqlSessionFactory.openSession();
            int count = session.update("updateStudent.updateStudent", student);
            System.out.println(count + " record was updated");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (session != null) {
                session.close();
            }
        }

    }

    private static List<Student> queryAllStudents() {
        SqlSession session = null;
        try {
            Reader reader = Resources.getResourceAsReader("shfq/mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            session = sqlSessionFactory.openSession();
            List<Student> students = session.selectList("Student.getAll");
            System.out.println(students.size() + " record was got");
            List<Student> students1 = session.selectList("Student.getAll");
            System.out.println(students == students1);
            for (int i = 0; i < students.size(); i++) {
                System.out.println(students.get(i) == students1.get(i));
            }
            return students;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private static void quickSort(List<Integer> integers, int leftBoundary, int rightBoundary) {
        if ((rightBoundary - leftBoundary) == 0 || (rightBoundary - leftBoundary) == -1) {
            return;
        }
        int pivotIndex = partition(integers, leftBoundary, rightBoundary, (leftBoundary + rightBoundary) / 2);
        quickSort(integers, leftBoundary, pivotIndex-1);
        quickSort(integers, pivotIndex + 1, rightBoundary);
    }

    private static int partition(List<Integer> integers, int leftBoundary, int rightBoundary, int pivotIndex) {
        swap(integers, leftBoundary, pivotIndex);
        int pivotValue = integers.get(leftBoundary);
        int boundary = leftBoundary + 1;
        for (int i = 0; i < rightBoundary - leftBoundary; i++) {
            int current = i + 1 + leftBoundary;
            if (integers.get(current) < pivotValue) {
                swap(integers, current, boundary);
                boundary = boundary + 1;
            }
        }
        swap(integers, leftBoundary, boundary - 1);


        return boundary-1;
    }

    private static void swap(List<Integer> integers, int index1, int index2) {
        if (integers == null || integers.size() == 1) {
            return;
        }
        if (index1 < 0 || index2 < 0 || index1 >= integers.size() || index2 >= integers.size()) {
            return;
        }
        int temp = integers.get(index1);
        integers.set(index1, integers.get(index2));
        integers.set(index2, temp);
    }


}
