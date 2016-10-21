/**
 *    Copyright 2009-2015 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
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
        selectAuthor();
    }

    private static void selectBlog() throws Exception {
        SqlSession session = SessionUtil.getSessionByConfigXmlPath("shfq/lazy_load/mybatis-config.xml");
        Object o = session.selectList("shfq.lazy_load.vo.Blog.selectBlog", 1);
        System.out.println("1 " +System.currentTimeMillis());
        System.out.println("");
        List<Blog> blogs = (List<Blog>) o;
        Blog blog = blogs.get(0);
        String content = blog.getContent();
        System.out.println(content);
        System.out.println("2 " +System.currentTimeMillis());

        Author author = blog.getAuthor();
        System.out.println(author.getName());
    }

    private static void selectAuthor() throws Exception {
        SqlSession session = SessionUtil.getSessionByConfigXmlPath("shfq/lazy_load/mybatis-config.xml");
        Object o = session.selectOne("shfq.lazy_load.vo.Author.selectAuthor", 1);
        System.out.println("");
    }

}
