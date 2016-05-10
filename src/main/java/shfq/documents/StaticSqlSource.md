  StaticSqlSource 代表了 <insert> 标签中 的 INSERT INTO STUDENT (NAME, BRANCH, PERCENTAGE, PHONE, EMAIL ) VALUES (#{name}, #{branch}, #{percentage}, #{phone}, #{email});

  private String sql;
  private List<ParameterMapping> parameterMappings;
  private Configuration configuration;

sql 代表了 对 INSERT INTO STUDENT (NAME, BRANCH, PERCENTAGE, PHONE, EMAIL ) VALUES (#{name}, #{branch}, #{percentage}, #{phone}, #{email}); 处理后的最原始的 sql 语句，处理后的结果为： INSERT INTO STUDENT (NAME, BRANCH, PERCENTAGE, PHONE, EMAIL ) VALUES (?, ?, ?, ?, ?); 

parameterMappings 代表了参数集合 (#{name}, #{branch}, #{percentage}, #{phone}, #{email}) ParameterMapping 详见 ParameterMapping
configuration 代表了配置文件

