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
package shfq.annotation;


import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import shfq.nested_result_map.vo.Address;

import java.io.IOException;
import java.io.Reader;

/**
 * Created by shfq on 2016/10/23.
 */
public class Test {
    public static void main(String[] args) {
        try {
            Reader reader = Resources.getResourceAsReader("shfq/annotation/mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession session = sqlSessionFactory.openSession();
            AddressMapper mapper = session.getMapper(AddressMapper.class);
            Address address = mapper.queryById(1);
            System.out.println("id:" + address.getId());
            System.out.println("name:" + address.getName());
            System.out.println("post code:" + address.getPostCode());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
