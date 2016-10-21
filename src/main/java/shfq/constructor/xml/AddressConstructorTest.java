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
package shfq.constructor.xml;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import shfq.nested_result_map.vo.Address;

import java.io.IOException;
import java.io.Reader;

/**
 * author:      shfq
 * description:
 * create date: 2016/8/16.
 */
public class AddressConstructorTest {
    public static void main(String[] args) {
        Address address = queryAddressById(1);

    }

    private static Address queryAddressById(int id) {
        SqlSession session = null;
        try {
            Reader reader = Resources.getResourceAsReader("shfq/constructor/xml/mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            session = sqlSessionFactory.openSession();
            Address address = session.selectOne("shfq.constructor.xml.Address.selectAddress", id);
            session.commit();
            System.out.println("record queried successfully");
            return address;
        } catch (IOException e) {
            e.printStackTrace();
            session.rollback();
            return null;

        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

}
