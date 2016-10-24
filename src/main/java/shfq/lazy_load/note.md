mybatis 的懒加载原理

断断续续的阅读 mybatis 的源码有好几个月了，想把自己了解到的一些东西与大家分享。今天给大家分享一下 mybatis 的懒加载原理。

mybatis 的懒加载过程挺复杂的，涉及到的东西有很多，包括：配置文件的解析、mapper 文件的解析、sql 语句的映射、结果集的映射、懒加载等。

### 简单的说一下 mybatis 框架是干什么的

我用我自己的话来总结一下 mybatis 是干什么的，mybatis 不会直接和数据库进行打交道，mybatis 其实是对 jdbc api 的进一步封装，最终和数据库打交道的仍然是 jdbc 。

常用的 jdbc 操作有：增、删、改、查、事务等。在这里不准备讲事务，以后应该会专门写一篇博客讲 mybatis 事务的。

jdbc 的操作主要是通过 sql 语句来实现的。所以，mybatis 很大的一部分实现是怎样把 mybatis 的 xml（或注解）和参数一步一步最终映射为一条 sql 语句（或者是 sql 语句和执行这条 sql 语句所需的参数），然后把这条 sql 语句（或者是 这条sql 语句和执行这条 sql 语句所需的参数），然后再进一步的映射为 jdbc 的 Statement（或 PreparedStatement）。最终 jdbc 去执行 Statement（或 PreparedStatement）语句。

对于 增、删、改这三类操作，mybatis 的主要作用就是把 xml（或注解）和参数最终映射为 Statement（或 PreparedStatement），然后由 jdbc 去执行，最终映射为数据库中的一条记录或者是删除了数据库中的一条记录或者是修改了数据库中的一条记录。

查询操作要比前面的三类操作更复杂，因为查询操作首先是把 xml（或注解）和参数最终映射为 Statement（或 PreparedStatement），然后由 jdbc 去执行，最终返回一个结果集。然后 mybatis 再根据 xml 配置文件或者是接口方法中的返回值来判断返回给用户的数据类型是什么，根据返回类型再 new 出对象，然后将结果集中的值取出来，通过 Java 反射调用把值设置到对象中的一个过程。

把结果集映射为返回对象的过程是一个很复杂的过程，虽然原理看起来很简单，把结果集中的列字段映射为对象中的属性。因为 mybatis 的功能很强大，这些强大的功能的背后需要复杂的代码来实现。今天，在这里也不会讲将结果集映射为对象的过程，以后有时间可能会讲。

总结一下，mybatis 的最主要的两个功能：

- 将 xml（或注解）和参数映射为 sql 交给 jdbc 去执行（增、删、改、查）
- 对于查询，将 jdbc 查询出的结果集映射为对象，将映射好的对象返回给用户。

### 懒加载的例子

Blog.java:

    public class Blog {
        private int id;
        private String content;
        private Author author;
    
        public int getId() {
            return id;
        }
    
        public void setId(int id) {
            this.id = id;
        }
    
        public String getContent() {
            return content;
        }
    
        public void setContent(String content) {
            this.content = content;
        }
    
        public Author getAuthor() {
            return author;
        }
    
        public void setAuthor(Author author) {
            this.author = author;
        }
    }

Author.java:

    public class Author {
        private int id;
        private String name;
    
        public int getId() {
            return id;
        }
    
        public void setId(int id) {
            this.id = id;
        }
    
        public String getName() {
            return name;
        }
    
        public void setName(String name) {
            this.name = name;
        }
    }
    
BlogMapper.xml:
    
    <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
    <mapper namespace = "shfq.lazy_load.vo.Blog">
        <select id="selectBlog" parameterType="int" resultMap="blogResultMap">
            SELECT * from blog where id=#{id}
        </select>
        
        <resultMap id="blogResultMap" type="shfq.lazy_load.vo.Blog">
            <id column="id" property="id"></id>
            <result column="content" property="content"></result>
            <association property="author" column="author_id" select="selectAuthor" fetchType="lazy"/>
        </resultMap>
        
        <select id="selectAuthor" parameterType="int" resultType="shfq.lazy_load.vo.Author">
            SELECT * from author where id=#{id}
        </select>
    </mapper>
    
mybatis-config.xml:
    
    <!DOCTYPE configuration
            PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
            "http://mybatis.org/dtd/mybatis-3-config.dtd">
    <configuration>
        <environments default="development">
            <environment id="development">
                <transactionManager type="JDBC"/>
                <dataSource type="POOLED">
                    <property name="driver" value="com.mysql.jdbc.Driver"/>
                    <property name="url" value="jdbc:mysql://localhost:3306/mybatis-demo"/>
                    <property name="username" value="root"/>
                    <property name="password" value="123456"/>
                </dataSource>
            </environment>
        </environments>
        <mappers>
            <mapper resource="shfq/lazy_load/BlogMapper.xml"></mapper>
        </mappers>
    </configuration>
    
Test.java

    public class Test {
        public static void main(String[] args) throws Exception {
            selectAuthor();
        }
        private static void selectAuthor() throws Exception {
            Reader reader = Resources.getResourceAsReader("shfq/lazy_load/mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession session = sqlSessionFactory.openSession();
            Object o = session.selectOne("shfq.lazy_load.vo.Blog.selectBlog", 1);
            System.out.println("");
        }
    
    }
    
上面就是懒加载例子的相关代码，例子很简单。根据 id 查询 Blog，Blog 里又有一个 Author 对象，Author 对象是懒加载的。什么是懒加载？就是在需要它的时候才加载，不需要的话就不加载。

### 懒加载的原理

mybatis 会循环处理结果集中返回的每行数据的，在处理之前首先会通过反射调用构造方法来创建 result 对象，结果集中的一行数据最终会映射为一个 result 对象（严格的来说是不对的，结果集中的一行数据在多表连接的情况下可能会映射为多个 result 对象，结果集中的一行数据在多表连接一对多的情况下结果集中的多行数据可能映射一个 result 对象，简单的单表查询结果集中的一行数据映射为一个 result 对象）。

其实在调用构造方法创建 result 对象的时候，构造方法还可能会有参数，需要先把结果集中的参数都提取出来，传给相应的构造方法通过反射创建对象。

mybatis 其实第一步是解析配置文件，把配置文件映射为 mybatis 的 Configuration 类，把配置文件的 xml 属性都映射为 Java 对象中的属性值。xml mapper 文件的处理比较复杂，< resultMap/> 标签映射为 ResultMap 对象，<resultMap/> 标签中的< id/> 、< result/>、< association/> 等映射为 ResultMapping 对象。

其实要讲 mybatis 的实现过程，涉及到的点太多了，没法在一篇博客中讲。在这里，我只简单的讲一下原理。

接着再讲创建好 result 对象之后，mybatis 会循环 resultMappings（<resultMap/>标签中的每个子标签都映射为一个 resultMapping，这些 resultMapping 组成了一个集合就是 resultMappings）集合，看有没有需要懒加载的属性，如果有的话，则会为这个 result 对象创建一个代理对象。

什么情况下才会出现需要懒加载的属性呢？只有 < association property="author" column="author_id" select="selectAuthor" fetchType="lazy"/>
 和 < collection property="xxx" column="xxx" select="xxxx" fetchType="lazy"/> 作为一个子标签出现在 < resultMap/> 标签（也不一定只是 < resultMap/> 标签，< collection/> 等标签事实上也算是一个 < resultMap/> 标签）之内才会出现需要懒加载的属性。select 属性和 fetchType="lazy" 必须同时出现在 < collection/> 或 < association/> 属性中才需要懒加载，select 表示的是嵌套查询语句的 id ，fetchType="lazy" 表示的是懒加载。

再接着讲 result 对象的代理对象，代理类是由 javassist 框架在运行时创建和加载的，这个代理类继承自 result 对象所属的类，以上面的例子为例，这个代理类继承自 Blog 类。

对于这个代理类的详细讲解在我的博客[深入理解 Java 动态代理](http://blog.csdn.net/shfqbluestone/article/details/52853460)中有很详细的介绍，在这里我再简单的介绍下。

这个代理类继承了 result 对象所属的类（被代理类）并重写了被代理类的所有的方法，所以在代理对象上调用懒加载属性的 get 方法（getAuthor()）时会触发懒加载动作，mybatis 这时就能发现需要去懒加载一个属性，然后去加载这个属性。

其实，有几个方法都可以触发懒加载的操作，比如调用懒加载的 get/set 方法（确实调用 set 方法时也会触发懒加载操作）还有调用 clone()、equals()、hashCode()、toString()方法也会触发懒加载操作，如果代理对象有多个懒加载属性，则调用后面的这四个方法时会同时触发加载所有的懒加载属性。

懒加载操作只会触发一次，下次再调用这些方法时不会再次触发懒加载操作的。

懒加载其实又是一次查询操作，懒加载查询需要传递一些参数，还有一些其他条件。这些待传递的属性、参数、查询所需的 sql 语句等相关的条件都已经封装到了代理对象内部，这些条件封装在一个 Map 中，键是懒加载查询的属性名称，值是查询该属性所需的条件，包括参数、sql 语句等。懒加载完一个属性之后会把这个属性从 Map 中移除，所以再次出发懒加载操作时 mybatis 就知道该属性已经被被加载过了，不会重复加载。

懒加载也是一个挺复杂的过程，我在上面的讲解中省略了很多，要全部讲出来涉及的东西太多。

我再总结一下，懒加载功能使用了代理对象，所以在调用懒加载属性的 get/set 方法（或者是其他触发懒加载操作的方法）时 mybatis 才能知道这时候应该去加载懒加载属性。

我对 mybatis 的懒加载原理已经很清楚了（我没有读过 hibernate 的源码，但我坚信 hibernate 的懒加载原理也是类似的，肯定使用了代理，所以才能知道需要在什么时候去加载懒加载属性），我在写出来的时候省略了许多过程，不知道大家能不能读懂。读懂我的 [深入理解 Java 动态代理](http://blog.csdn.net/shfqbluestone/article/details/52853460) 这篇博客对理解 mybatis 的懒加载原理有很大的帮助，我建议大家认真读一下 [深入理解 Java 动态代理](http://blog.csdn.net/shfqbluestone/article/details/52853460) 这篇博客。
读懂了我的 [深入理解 Java 动态代理](http://blog.csdn.net/shfqbluestone/article/details/52853460) 这篇博客之后对于大家理解 AOP 原理、spring 事务管理（其他框架的事务管理也是类似的）等都会有很大的帮助。AOP、sprig 事务管理背后都用到了 Java 动态代理。
 
[https://github.com/fengsmith/mybatis-3](https://github.com/fengsmith/mybatis-3) 这是我在阅读 mybatis 源码时 fork 的 mybatis 源码仓库，里面有我对源码的注释和阅读源码时构造的一些例子。阅读源码很不容易，需要坚持也需要方法，如果大家想要阅读 mybatis 源码的话可以与我交流。




