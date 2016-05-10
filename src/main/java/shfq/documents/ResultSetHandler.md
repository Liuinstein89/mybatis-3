ResultSetHandler 主要负责处理 ResultSet 

  private static final Object DEFERED = new Object();

  private final Executor executor;
  private final Configuration configuration;
  private final MappedStatement mappedStatement;
  private final RowBounds rowBounds;
  private final ParameterHandler parameterHandler;
  private final ResultHandler<?> resultHandler;
  private final BoundSql boundSql;
  private final TypeHandlerRegistry typeHandlerRegistry;
  private final ObjectFactory objectFactory;
  private final ReflectorFactory reflectorFactory;

  // nested resultmaps
  private final Map<CacheKey, Object> nestedResultObjects = new HashMap<CacheKey, Object>();
  private final Map<CacheKey, Object> ancestorObjects = new HashMap<CacheKey, Object>();
  private final Map<String, String> ancestorColumnPrefix = new HashMap<String, String>();

  // multiple resultsets
  private final Map<String, ResultMapping> nextResultMaps = new HashMap<String, ResultMapping>();
  private final Map<CacheKey, List<PendingRelation>> pendingRelations = new HashMap<CacheKey, List<PendingRelation>>();

DEFERED 不知道作用
Executor sql 语句的真正执行者
configuration 配置文件
mappedStatement 对 <insert> 的映射
rowBounds 查询结果集中返回的记录的条数的范围。
ParameterHandler 主要负责处理 Statement 中的参数的。
resultHandler 不清楚作用。
boundSql 真正的 sql 。
typeHandlerRegistry typeHandler 的注册类。
objectFactory 创建对象。
reflectorFactory 未知。



