为了构造 NullCacheKey 出现的情况。

只有在嵌套 resultMap 的情况下才有可能出现 NullCacheKey 。
当嵌套 <resultMap/> 或者其 <result/> 确定的 <resultMap/> 中的 resultMapping 
集合中的列名与结果集中的列名没有交集的情况下，就会出现 NullCacheKey 。

在本例子中构造出了这样的一个例子，详情见 PeopleMapper.xml ，在父 <resultMap/> 中需要 people_id 但在 select 语句中没有这一列。在子 <resultMap/> 中需要 pet_name 这一列，但在 select 语句中也没有这一列。
这样一来就会构造出 NullCacheKey 出现的例子。

在我构造的这个例子中，结果集中查询出来两行数据，返回的对象是一个 List ，list 里有两个元素，但每个元素都是 null 。不知道算不算是 mybatis 的一个 bug 。