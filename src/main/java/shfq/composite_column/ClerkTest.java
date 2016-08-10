package shfq.composite_column;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;

/**
 * author:      shfq
 * description:
 * create date: 2016/8/10.
 */
public class ClerkTest {
    public static void main(String[] args) {
        queryClerk();
        System.out.println("");

    }
    private static void queryClerk() {
        try {
            Reader reader = Resources.getResourceAsReader("shfq/mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession session = sqlSessionFactory.openSession();
            Object clerk = session.selectList("shfq.composite_column.Clerk.selectClerk", 1);
            System.out.println("record queried successfully");
            session.commit();
            session.close();
            System.out.println("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void queryClerkAddress() {
        try {
            Reader reader = Resources.getResourceAsReader("shfq/mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession session = sqlSessionFactory.openSession();
            // query student data
            ClerkAddress queryParameter = new ClerkAddress();
            queryParameter.setHouseNo(18);
            queryParameter.setStreetNo(20);
            Object clerkAddress = session.selectList("shfq.composite_column.ClerkAddress.selectClerkAddress", queryParameter);
            System.out.println("record queried successfully");
            session.commit();
            session.close();
            System.out.println("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
