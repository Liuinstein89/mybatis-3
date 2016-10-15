package shfq.lazy_load;

import org.apache.ibatis.session.SqlSession;
import shfq.Utils.SessionUtil;
import shfq.lazy_load.vo.Author;
import shfq.lazy_load.vo.Blog;

import java.util.List;

/**
 * author:      shfq
 * description:
 * create date: 2016/9/13.
 */
public class Test {
    public static void main(String[] args) throws Exception {
        SqlSession session = SessionUtil.getSessionByConfigXmlPath("shfq/lazy_load/mybatis-config.xml");
        Object o = session.selectList("shfq.lazy_load.vo.Blog.selectBlog", 1);
        System.out.println("");
        List<Blog> blogs = (List<Blog>) o;
        Blog blog = blogs.get(0);
        String content = blog.getContent();
        System.out.println(content);

        Author author = blog.getAuthor();
        System.out.println(author.getName());
    }

}
