本来是为了构造 DefaultResultSetHandler.java 中 applyNestedResultMappings() 方法中
if (ancestor != null) { if (newObject) { }} 什么时候 ancestor != null 但是 newObject 的值是 false 的这种情形。

但是发现这个例子还是不能出现上面的那种情况，但发现了：
petHouse 是同一个，但创建了两次，不过两个对象的属性值是相同的，好像也没有什么问题。
因为子 rowKey 相同的情况下，mybatis 无法判断它们是不是同一个对象。
只有最外层的对象可以从 nestedResultObjects 集合中取到，
因为最外层的对象是从 handleRowValuesForNestedResultMap() 方法中创建的缓存 rowKey ，
其传递给 getRowValue() 方法中的 combinedKey 参数就是 rowKey 两者是相同的，所以能取到，<resultMap/> 里面嵌套的 <resultMap/> 
所确定的嵌套对象在 nestedResultObjects 集合中几乎是用不到的，为什么？因为获取到内层对象时需要 combinedKey ，一般来说父 key 是不相同的，
除非父父 key 也相同，则所有的父对象都相同，一般是不会出现这种情况的。
