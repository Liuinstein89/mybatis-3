transaction 包比较简单，主要有两种 Java class ，一种是 Transaction 类，另一种是 TransactionFactory 类。

- Transaction 是对数据库 Connection 和数据库事物的封装。
- TransactionFactory 是工厂类，所有的工厂类的目的都是创建对象，顾名思义 TransactionFactory 工厂的目的是创建 Transaction 对象。

从另外一个角度来讲又可以分为两种，一种是托管（managed）类，另一种是非托管（jdbc）类。

- JdbcTransaction 管理 Transaction 的全部生命周期。
- ManagedTransaction 托管事物，顾名思义把事物托管给别人，一般是托管给容器，commit 和 rollback 的操作托管给容器去处理，默认 ManagedTransaction 不会去 close 掉连接，但也可以通过配置让 ManagedTransaction 去 close 掉连接（mybatis 的文档中刚好写反了这点）。

> JdbcTransaction 和 ManagedTransaction 的区别就是前者负责 commit 、rollback、close 后者把 commit 、rollback、close 交由容器托管了（也可以通过配置让 ManagedTransaction 来关闭连接）。