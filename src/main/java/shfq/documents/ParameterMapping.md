代表了参数值的映射，例如在 INSERT INTO STUDENT (NAME, BRANCH, PERCENTAGE, PHONE, EMAIL ) VALUES (#{name}, #{branch}, #{percentage}, #{phone}, #{email}); 中的参数 #{name}
  private Configuration configuration;

  private String property;
  private ParameterMode mode;
  private Class<?> javaType = Object.class;
  private JdbcType jdbcType;
  private Integer numericScale;
  private TypeHandler<?> typeHandler;
  private String resultMapId;
  private String jdbcTypeName;
  private String expression;
configuration 总的配置文件
property 代表了参数的名称，在本例中是 name
mode 代表了参数的模式，IN 、OUT 、INOUT 在存储过程中才会有 OUT 和 INOUT 本例中 mode 的值为 IN 。
javaType 是指参数值的类型，在
 <insert id = "insert" parameterType = "shfq.Student">
        INSERT INTO STUDENT (NAME, BRANCH, PERCENTAGE, PHONE, EMAIL ) VALUES (#{name}, #{branch}, #{percentage}, #{phone}, #{email});
        <selectKey keyProperty = "id" resultType = "int" order = "AFTER">
            select last_insert_id() as id
        </selectKey>
    </insert>
中，参数值 name 的类型应该就是 parameterType= "shfq.Student" 中的 Student 类中的 name 字段的类型。
jdbcType 应该是指该字段在数据库中的列的类型，暂时不知道类型为 null 
numericScale 暂时不知道是什么
typeHandler 主要用于对 PreparedStatement 语句设置参数值以及在查询的结果集中获取某一列的值。
resultMapId 是什么暂时未知
jdbcTypeName 暂时未知
expression 暂时未知



