package shfq.one_many_annotation;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;

/**
 * Created by shfq on 2016/8/4.
 */
public class PeopleTest {
    public static void main(String[] args) {
        testQuery();
    }
    public static void testQuery() {
        try {
            Reader reader = Resources.getResourceAsReader("shfq/mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession session = sqlSessionFactory.openSession();
            // query student data
            Object people = session.selectList("shfq.one_many_annotation.People.selectPeople", 1);
            System.out.println("record queried successfully");
            session.commit();
            session.close();
            System.out.println("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
