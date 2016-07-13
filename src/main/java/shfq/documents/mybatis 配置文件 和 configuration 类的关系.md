配置文件是一个 xml 文件，通过解析这个 xml 文件最终会根据这个文件构造出 Configuration 对象。
配置文件中的 <properties> 属性对应着 Configuration 类的 variables 属性，variables 的类型为 Properties。通过 variables 的 set/get 方法可以访问到配置属性。
<property> 属性中的 name 或 value 不出现的话 则 name 或 value 为 null 则忽略掉该属性，如果 name 或 value 为 "" 空的字符串，把空的字符串和其他字符串同样对待。

<typeAliases> 对应着 TypeAliasRegistry 中的类型为 Map<String, Class> 的 TYPE_ALIASES 。
如果 alias 中的 alias 为 null （不出现这个属性）的话，如果该类有 Alias annotation 的话，别名则为 Alias 中定义的别名，然后把该别名转化为小写字母。其实别名最终都会被转化为小写字母如果没有定义别名的话则 alias 为该类的简单名称，然后全部字母都小写。
 
<mappers> 对应着 MapperRegistry 类中的 knownMappers 属性其类型为 Map<Class<?>, MapperProxyFactory<?>> 
会加载 namespace 为 mapper.xml 的 xml 文件，这个文件有可能不存在有可能存在，如果不存在的话一般是把 sql 语句写在了 annotation 上。如果没有 annotation 的话应该会有相应的 xml 文件。




