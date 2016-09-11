package shfq.multiple_resultset;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;

/**
 * Created by shfq on 2016/9/8.
 */
public class MultipleResultSetTest {
    public static void main(String[] args) {
        queryPeople();

    }
    private static void queryPeople() {
        try {
            Reader reader = Resources.getResourceAsReader("shfq/multiple_resultset/xml/mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession session = sqlSessionFactory.openSession();
            Object people = session.selectList("shfq.multiple_resultset.xml.caller.callProcedure", 1);
            System.out.println("record queried successfully");
            session.commit();
            session.close();

            System.out.println("dd");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
