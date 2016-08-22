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
