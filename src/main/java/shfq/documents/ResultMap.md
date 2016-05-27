ResultMap 代表了 <resultMap> 标签。

  private String id;
  private Class<?> type;
  private List<ResultMapping> resultMappings;
  private List<ResultMapping> idResultMappings;
  private List<ResultMapping> constructorResultMappings;
  private List<ResultMapping> propertyResultMappings;
  private Set<String> mappedColumns;
  private Discriminator discriminator;
  private boolean hasNestedResultMaps;
  private boolean hasNestedQueries;
  private Boolean autoMapping;

id 是由 namespace 和 resultMap 的 id 加上 . 号拼接而成的，本例中的 id 是 Student.result
type 是 <resultMap> 的 type 属性，是一个 Class 类型的对象。
resultMappings 是 ResultMapping 的集合，一个 ResultMapping 代表了一个 <result> 标签，例如 <result property = "id" column = "ID"/> 详见 ResultMapping 。
idResultMappings 在 <resultMap> 标签中如果有 <id> 子标签的话 idResultMappings 就是子标签的集合，如果没有 <id> 子标签的话则该集合和 resultMappings 集合是一样的。
constructorResultMappings 如果
propertyResultMappings 
mappedColumns 是 <resultMap> 中的 <result> 中 column 属性值的集合。
discriminator 暂时不知道
hasNestedResultMaps 有没有嵌套 ResultMap
hasNestedQueries 有没有嵌套查询
autoMapping 其实在 <resultMap> 中还有个属性 autoMapping ，不过这个属性可以省略。
 


<mapper namespace = "Student">
    <resultMap id = "result" type = "shfq.Student">
        <result property = "id" column = "ID"/>
        <result property = "name" column = "NAME"/>
        <result property = "branch" column = "BRANCH"/>
        <result property = "percentage" column = "PERCENTAGE"/>
        <result property = "phone" column = "PHONE"/>
        <result property = "email" column = "EMAIL"/>
    </resultMap>
</mapper>