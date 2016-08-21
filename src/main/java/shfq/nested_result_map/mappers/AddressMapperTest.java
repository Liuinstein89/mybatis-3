package shfq.nested_result_map.mappers;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import shfq.nested_result_map.mappers.AddressMapper;
import shfq.nested_result_map.vo.Address;

import java.io.IOException;
import java.io.Reader;

/**
 * author:      shfq
 * description:
 * create date: 2016/7/28.
 */
public class AddressMapperTest {
    public static void main(String[] args) {
        queryByMapper();

    }

    private static void queryByMapper() {
        SqlSession session = null;
        try {
            Reader reader = Resources.getResourceAsReader("shfq/mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            session = sqlSessionFactory.openSession();
            AddressMapper mapper = session.getMapper(AddressMapper.class);
            Address address = mapper.selectAddress(1);
            System.out.println(address.getName());

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (session != null) {
                session.close();
            }
        }

    }
}
