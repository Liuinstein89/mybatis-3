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
