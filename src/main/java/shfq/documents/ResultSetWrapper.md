ResultSetWrapper 是对 ResultSet 的封装

  private final ResultSet resultSet;
  private final TypeHandlerRegistry typeHandlerRegistry;
  private final List<String> columnNames = new ArrayList<String>();
  private final List<String> classNames = new ArrayList<String>();
  private final List<JdbcType> jdbcTypes = new ArrayList<JdbcType>();
  private final Map<String, Map<Class<?>, TypeHandler<?>>> typeHandlerMap = new HashMap<String, Map<Class<?>, TypeHandler<?>>>();
  private Map<String, List<String>> mappedColumnNamesMap = new HashMap<String, List<String>>();
  private Map<String, List<String>> unMappedColumnNamesMap = new HashMap<String, List<String>>();

resultSet 查询结果集
typeHandlerRegistry 未知
columnNames 查询出的记录的所有列名称的集合。
classNames 查询出的记录的素有列的类型名称的集合。
jdbcTypes 结果集中的返回列的 jdbcType 集合。
typeHandlerMap 未知
mappedColumnNamesMap
unMappedColumnNamesMap 