ParameterHandler 主要负责处理 Statement 中的参数的。

  private final TypeHandlerRegistry typeHandlerRegistry;

  private final MappedStatement mappedStatement;
  private final Object parameterObject;
  private BoundSql boundSql;
  private Configuration configuration;

typeHandlerRegister 是 TypeHandler 的集合。
mappedStatement 对 <insert> 标签的映射。
parameterObject 参数对象。
boundSql 真正的 sql 。
configuration 配置文件。
