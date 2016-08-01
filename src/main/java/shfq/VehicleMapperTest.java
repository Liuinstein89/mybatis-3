package shfq;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import shfq.vo.Vehicle;

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
        Vehicle vehicle2 = query(2);
        System.out.println("test");
    }
    private static Vehicle query(int id) {
        SqlSession session = null;
        try {
            Reader reader = Resources.getResourceAsReader("shfq/mybatis-config.xml");
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
