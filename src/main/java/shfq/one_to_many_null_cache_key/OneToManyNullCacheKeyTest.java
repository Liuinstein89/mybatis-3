package shfq.one_to_many_null_cache_key;

import org.apache.ibatis.session.SqlSession;
import shfq.Utils.SessionUtil;

/**
 * author:      shfq
 * description:
 * create date: 2016/9/13.
 */
public class OneToManyNullCacheKeyTest {
    public static void main(String[] args) throws Exception {
        SqlSession session = SessionUtil.getSessionByConfigXmlPath("shfq/one_to_many_null_cache_key/mybatis-config.xml");
        Object o = session.selectList("shfq.one_to_many_null_cache_key.People.selectPeople", 1);
        System.out.println("");
    }

}
