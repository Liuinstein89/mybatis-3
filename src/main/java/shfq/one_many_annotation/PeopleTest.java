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

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;

/**
 * Created by shfq on 2016/8/4.
 */
public class PeopleTest {
    public static void main(String[] args) {
        testQuery();
//        findNum();
    }
    public static void testQuery() {
        try {
            Reader reader = Resources.getResourceAsReader("shfq/mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession session = sqlSessionFactory.openSession();
            // query student data
            Object people = session.selectList("shfq.one_many_annotation.People.selectPeople", 1);
            System.out.println("record queried successfully");
            session.commit();
            session.close();
            System.out.println("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void findNum() {
        int i = 0;
        while (true) {
            int sum = sum(i);
            if (sum1(sum) == 50) {
                System.out.println(i);
                return;
            } else {
                i++;
            }

        }
    }

    private static int sum(int beginNum) {
        return  (beginNum + beginNum + 999)*1000 / 2;
    }

    private static int sum1(int num) {
        String s = String.valueOf(num);
        int sum = 0;
        for (int i = 0; i < s.length(); i++) {
            sum += Integer.parseInt(s.substring(i, i + 1));
        }
        return sum;
    }
}
