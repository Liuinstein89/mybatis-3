
读 DefaultResultSetHandler.java 阅读到 prepareCompositeKeyParameter L159



javaType resultMap ofType type resultType 这几者有些相似

ofType 只能用于 <collection/> 表示的是集合中存储的对象的类型
javaType 用于 <collection/> 表示的是 集合 的类型比如 ArrayList LinkedList，如果不指定的话默认为 ArrayList
javaType 用于其他情形下指的是映射的目标类型
type 用于 resultMap 映射的目标类型

在 <select/> 中 resultType 和 resultMap 有些类似。在简单对象映射下，可以用 resultType 在复杂的查询下使用 resultMap

有 <constructor/> 、<discriminator/> 的情况

有 <constructor/> 或 <association/> 的情况下同时有 select 属性。XmlMapperBuilder.processNestedResultMappings()

组合列：MapperBuilderAssistant.parseCompositeColumnName()
resultMap extend

在 resultMap 中 有 <constructor/> 同时又有组合列 并且有一个列的值既是 构造方法的参数 同时也是组合列的一列会不会出问题？？

Configuration.checkLocallyForDiscriminatedNestedResultMaps();