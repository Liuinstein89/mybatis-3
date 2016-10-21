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
package shfq.discriminator.xml;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import shfq.discriminator.vo.Bus;
import shfq.discriminator.vo.Car;

import java.io.IOException;
import java.io.Reader;

/**
 * author:      shfq
 * description:
 * create date: 2016/8/16.
 */
public class VehicleTest {
    public static void main(String[] args) {
        Car car = queryCarById(1);
        Bus bus = queryBusById(2);
        System.out.println("");

    }

    private static Car queryCarById(int id) {
        SqlSession session = null;
        try {
            Reader reader = Resources.getResourceAsReader("shfq/discriminator/xml/mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            session = sqlSessionFactory.openSession();
            Car vehicle = session.selectOne("shfq.discriminator.vo.Vehicle.selectVehicle", id);
            session.commit();
            System.out.println("record queried successfully");
            return vehicle;
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
    private static Bus queryBusById(int id) {
        SqlSession session = null;
        try {
            Reader reader = Resources.getResourceAsReader("shfq/discriminator/xml/mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            session = sqlSessionFactory.openSession();
            Bus vehicle = session.selectOne("shfq.discriminator.vo.Vehicle.selectVehicle", id);
            session.commit();
            System.out.println("record queried successfully");
            return vehicle;
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
