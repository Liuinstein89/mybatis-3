package shfq.one_to_many3;

import org.apache.ibatis.session.SqlSession;
import shfq.Utils.SessionUtil;

/**
 * author:      shfq
 * description:
 * create date: 2016/9/13.
 */
public class OneToManyTest {
    public static void main(String[] args) throws Exception {
        SqlSession session = SessionUtil.getSessionByConfigXmlPath("shfq/one_to_many3/mybatis-config.xml");
        Object o = session.selectList("shfq.one_many_annotation.People.selectPeople", 2);
        System.out.println("");
    }

}
