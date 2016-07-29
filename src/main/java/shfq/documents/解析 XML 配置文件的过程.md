调用 XMLConfigBuilder 的 parseConfiguration() 方法，最复杂的是解析 mapper 元素。

- 如果是 mapper 接口的话，会把接口 class 添加到 Configuration 的 mapperRegistry 中。

MapperAnnotationBuilder 主要负责解析 mapper 中的各种 annotation 元素，mapper 接口中最主要的就是各种 annotation 。



inline-parameter = (propertyName | expression) oldJdbcType attributes
propertyName = /expression language's property navigation path/
expression = '(' /expression language's expression/ ')'
oldJdbcType = ':' /any valid jdbc type/
attributes = (',' attribute)*
attribute = name '=' value