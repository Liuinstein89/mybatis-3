BoundSql 是从 SqlSource 中获取到的，经过处理过动态内容之后的真正的 sql 语句。 

  private String sql;
  private List<ParameterMapping> parameterMappings;
  private Object parameterObject;
  private Map<String, Object> additionalParameters;
  private MetaObject metaParameters;

sql 代表了最原始的 sql 语句，可能含有 ? 占位符。
parameterMappings 代表了参数集合
parameterObject 代表了参数对象
additionalParameters 代表了 for 循环或者是 bind 中的参数
metaParameters