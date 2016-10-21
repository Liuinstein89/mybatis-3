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
package shfq.nested_result_map.xml;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import shfq.nested_result_map.vo.Address;

import java.io.IOException;
import java.io.Reader;

/**
 * Created by shfq on 2016/8/21.
 */
public class AddressTest {
    public static void main(String[] args) {
        test();
    }
    private static void test() {
        SqlSession session = null;
        try {
            Reader reader = Resources.getResourceAsReader("shfq/nested_result_map/xml/mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            session = sqlSessionFactory.openSession();
            Address address = session.selectOne("shfq.nested_result_map.xml.address.selectAddressUseInclude", 1);

            System.out.println("");

            session.commit();
        } catch (IOException e) {
            session.rollback();
            e.printStackTrace();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
}
