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
//        testQuery();
        findNum();
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

    private static void findNum() {
        int i = 0;
        while (true) {
            int sum = sum(i);
            if (sum1(sum) == 50) {
                System.out.println(i);
                return;
            } else {
                i++;
            }

        }
    }

    private static int sum(int beginNum) {
        return  (beginNum + beginNum + 999)*1000 / 2;
    }

    private static int sum1(int num) {
        String s = String.valueOf(num);
        int sum = 0;
        for (int i = 0; i < s.length(); i++) {
            sum += Integer.parseInt(s.substring(i, i + 1));
        }
        return sum;
    }
}
