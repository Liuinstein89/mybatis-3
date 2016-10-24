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
