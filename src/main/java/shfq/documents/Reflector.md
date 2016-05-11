Reflector 用于反射。

  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  private Class<?> type;
  private String[] readablePropertyNames = EMPTY_STRING_ARRAY;
  private String[] writeablePropertyNames = EMPTY_STRING_ARRAY;
  private Map<String, Invoker> setMethods = new HashMap<String, Invoker>();
  private Map<String, Invoker> getMethods = new HashMap<String, Invoker>();
  private Map<String, Class<?>> setTypes = new HashMap<String, Class<?>>();
  private Map<String, Class<?>> getTypes = new HashMap<String, Class<?>>();
  private Constructor<?> defaultConstructor;

  private Map<String, String> caseInsensitivePropertyMap = new HashMap<String, String>();
  
type 是反射类的 Class 对象。
readablePropertyNames 可读属性名称集合，get 方法的名称结合。
writeablePropertyNames 可写属性名称集合，set 方法的名称的集合。
setMethods set 方法集合。
getMethods 方法集合。
setTypes 可写属性名称与属性类型的映射的集合
getTypes 可读属性名称与属性类型的映射的集合
defaultConstructor 默认构造方法
caseInsensitivePropertyMap 大小写不敏感的属性集合。
 