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
package shfq.discriminator.mappers;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import shfq.discriminator.vo.Vehicle;

import java.io.IOException;
import java.io.Reader;

/**
 * author:      shfq
 * description:
 * create date: 2016/8/1.
 */
public class VehicleMapperTest {
    public static void main(String[] args) {
        Vehicle vehicle1 = query(1);
        System.out.println("test");
    }
    private static Vehicle query(int id) {
        SqlSession session = null;
        try {
            Reader reader = Resources.getResourceAsReader("shfq/discriminator/mappers/xml/mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            session = sqlSessionFactory.openSession();
            VehicleMapper vehicleMapper = session.getMapper(VehicleMapper.class);
            Vehicle vehicle = vehicleMapper.selectVehicle(id);
            session.commit();
            session.close();

            return vehicle;
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
