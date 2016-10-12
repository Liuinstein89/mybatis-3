
读 DefaultResultSetHandler.java 阅读到 prepareCompositeKeyParameter L159



javaType resultMap ofType type resultType 这几者有些相似

ofType 只能用于 <collection/> 表示的是集合中存储的对象的类型
javaType 用于 <collection/> 表示的是 集合 的类型比如 ArrayList LinkedList，如果不指定的话默认为 ArrayList
javaType 用于其他情形下指的是映射的目标类型
type 用于 resultMap 映射的目标类型

在 < select /> 中 resultType 和 resultMap 有些类似。在简单对象映射下，可以用 resultType 在复杂的查询下使用 resultMap

有 <constructor/> 、<discriminator/> 的情况

有 <constructor/> 或 <association/> 的情况下同时有 select 属性。XmlMapperBuilder.processNestedResultMappings()

组合列：MapperBuilderAssistant.parseCompositeColumnName()
resultMap extend

在 resultMap 中 有 <constructor/> 同时又有组合列 并且有一个列的值既是 构造方法的参数 同时也是组合列的一列会不会出问题？？

Configuration.checkLocallyForDiscriminatedNestedResultMaps();
返回类型为 Map 的

几个疑问：

-- 嵌套 resultMap 中，在属性映射的时候，如果这个属性如果是复合属性是怎么映射的？
-- 构造方法中可不可以传一个符合对象类型？？？？
-- partialObject 什么时候不为空？
-- resultMap 中的 extends
-- 在 <select/> 中的 resultType 是不是也相当于是一个 <resultMap/>
-- MapperBuilderAssistant 中 parseCompositeColumnName() 方法的 new StringTokenizer(columnName, "{}=, ", false); 什么时候会有 {}= ？？






几个概念：

-- resultMap <resultMap/> <collection/> <association/> <case/> 本质上都是 resultMap 
-- 嵌套 resultMap ：就是在同一个结果集中，一个 resultMap 中的一个 <result> 中又确定了一个结果集。在 resultMapping 元素中有 resultMap 属性或者是 <collection/> <association/> 元素。但要注意 <collection/> 和 <association/> 是有前提条件的，条件就是这两个元素不能有 select 属性，因为
-- 简单 resultMap 中的 resultMapping 中也有可能有 nestedResultMapId 对吗？？？
-- 懒加载和嵌套查询时是同时出现的。、
-- 属性映射有三种：1.构造方法 2.自动映射 3.set 方法 。不知道在构造方法中能不能映射复合类型。
-- 自动映射都是简单类型的映射，为什么呢？因为自动映射是在 <resultMap> 中没有加的映射而是直接从结果集中筛选出非映射的列，然后通过反射查看返回类型有没有与列名相对应的属性，从数据库结果集中返回的列肯定都是简单类型。

几个结论：

-- 在有嵌套 resultMap 中的 resultMapping 中的 column 是没有作用的，所以 <collection/> 等中的 column 属性是没有用的。