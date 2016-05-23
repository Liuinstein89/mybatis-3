TypeHandler 主要负责处理 PreparedStatement 中的参数设置和 query 结果集中某一列字段的获取。
无论是 MyBatis 在预处理语句（PreparedStatement）中设置一个参数时，还是从结果集中取出一个值时， 都会用类型处理器将获取的值以合适的方式转换成 Java 类型。下表描述了一些默认的类型处理器。