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
