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
package shfq.composite_column;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;

/**
 * author:      shfq
 * description:
 * create date: 2016/8/10.
 */
public class ClerkTest {
    public static void main(String[] args) {
        queryClerk();
        System.out.println("");

    }
    private static void queryClerk() {
        try {
            Reader reader = Resources.getResourceAsReader("shfq/composite_column/scripts/mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession session = sqlSessionFactory.openSession();
//            Object clerk = session.selectList("shfq.composite_column.Clerk.selectClerk", 1);
            Object clerk = session.selectList("shfq.composite_column.composite.Clerk.selectClerk", 1);
            System.out.println("record queried successfully");
            session.commit();
            session.close();
            System.out.println("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void queryClerkAddress() {
        try {
            Reader reader = Resources.getResourceAsReader("shfq/mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession session = sqlSessionFactory.openSession();
            // query student data
            ClerkAddress queryParameter = new ClerkAddress();
            queryParameter.setHouseNo(18);
            queryParameter.setStreetNo(20);
            Object clerkAddress = session.selectList("shfq.composite_column.ClerkAddress.selectClerkAddress", queryParameter);
            System.out.println("record queried successfully");
            session.commit();
            session.close();
            System.out.println("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
