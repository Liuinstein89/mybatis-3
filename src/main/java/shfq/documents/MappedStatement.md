MappedStatement 是对 <insert> xml 的封装。


  private String resource;
  private Configuration configuration;
  private String id;
  private Integer fetchSize;
  private Integer timeout;
  private StatementType statementType;
  private ResultSetType resultSetType;
  private SqlSource sqlSource;
  private Cache cache;
  private ParameterMap parameterMap;
  private List<ResultMap> resultMaps;
  private boolean flushCacheRequired;
  private boolean useCache;
  private boolean resultOrdered;
  private SqlCommandType sqlCommandType;
  private KeyGenerator keyGenerator;
  private String[] keyProperties;
  private String[] keyColumns;
  private boolean hasNestedResultMaps;
  private String databaseId;
  private Log statementLog;
  private LanguageDriver lang;
  private String[] resultSets;

resource 表示的是 <insert> 所在 xml 的全名称。
configuration 代表的是 mybatis-config.xml 总的配置文件。
id 代表的是 <insert> 标签中的 id 属性 。
fetchSize 代表的是该语句需要返回的结果集中记录的最大值。
timeout 设置的是该语句的超时时间。
statementType 有三种取值分别为：STATEMENT 、PREPARED 、CALLABLE ，应该是在解析 xml 文件的时候确定的 statementType 。如果在 <insert> 标签中有 # 符号的时候就表示的是 PREPARED 。
resultSetType 在 insert 的时候 resultSetType 为 null ，resultSetType 会影响 query 的结果集。
sqlSource 代表了最原始的 sql 语句，应该是在解析 xml 的时候生成的，在本例子中 sqlSource 的值为 INSERT INTO STUDENT (NAME, BRANCH, PERCENTAGE, PHONE, EMAIL ) VALUES (?, ?, ?, ?, ?);
cache 缓存，具体的工作原理不清楚

resultMaps 用于查询



    
<insert id = "insert" parameterType = "shfq.Student">
        INSERT INTO STUDENT (NAME, BRANCH, PERCENTAGE, PHONE, EMAIL ) VALUES (#{name}, #{branch}, #{percentage}, #{phone}, #{email});
        <selectKey keyProperty = "id" resultType = "int" order = "AFTER">
            select last_insert_id() as id
        </selectKey>
    </insert>