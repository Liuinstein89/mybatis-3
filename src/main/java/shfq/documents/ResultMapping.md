ResultMapping 对应着 <resultMap> 标签中的子标签 <result> 例如，<result property = "id" column = "ID"/> ，下文中的 ResultMapping 都以 <result property = "id" column = "ID"/> 为例子。
ResultMapping 其实就是一个 VO 对象中的一个字段与这个 VO 对象相对应的数据库表中的一个列的映射关系。

    <resultMap id = "result" type = "shfq.Student">
        <result property = "id" column = "ID"/>
        <result property = "name" column = "NAME"/>
        <result property = "branch" column = "BRANCH"/>
        <result property = "percentage" column = "PERCENTAGE"/>
        <result property = "phone" column = "PHONE"/>
        <result property = "email" column = "EMAIL"/>
    </resultMap>
    
  private Configuration configuration;
  private String property;
  private String column;
  private Class<?> javaType;
  private JdbcType jdbcType;
  private TypeHandler<?> typeHandler;
  private String nestedResultMapId;
  private String nestedQueryId;
  private Set<String> notNullColumns;
  private String columnPrefix;
  private List<ResultFlag> flags;
  private List<ResultMapping> composites;
  private String resultSet;
  private String foreignColumn;
  private boolean lazy;
configuration 总的配置文件
property 对应着 <result> 标签中的 property 属性，在本例中 property 的值是 "id" ，这个 property 其实是 Student 中的字段的名称，在本例中是 id 字段。
column 对应着 <result> 标签中的 column 属性，在本例中 column 的值是 "ID" ，这个 column 其实是数据库表 STUDENT 表中与 id 字段相对应的 ID 列。
javaType 其实 <result> 标签中还有几个省略的属性，这几个属性是 javaType,jdbcType,typeHandler  <result property = "id" column = "ID" javaType="" jdbcType="" typeHandler=""/>，如果在 <result> 中指定了 javaType ，那么 javaType 就是指定的值，如果没指定的话就是 VO 对象的字段的类型，在本例中是 Student 对象中的 id 字段的类型，id 字段的类型是 int 。
jdbcType 对应着 <result> 标签中的 jdbcType 属性，不过 jdbcType 可以省略，如果没有省略的话指定什么就是什么，如果省略了的话就为 null 
typeHandler 暂时未知
nestedResultMapId
nestedQueryId
notNullColumns
columnPrefix
flags
composites
resultSet
foreignColumn 


jdbc
  