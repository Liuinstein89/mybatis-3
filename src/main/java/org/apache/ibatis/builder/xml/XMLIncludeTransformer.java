/**
 *    Copyright 2009-2015 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.builder.xml;

import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.parsing.PropertyParser;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.session.Configuration;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Properties;

/**
 * @author Frank D. Martinez [mnesarco]
 */
public class XMLIncludeTransformer {

  private final Configuration configuration;
  private final MapperBuilderAssistant builderAssistant;

  public XMLIncludeTransformer(Configuration configuration, MapperBuilderAssistant builderAssistant) {
    this.configuration = configuration;
    this.builderAssistant = builderAssistant;
  }

  public void applyIncludes(Node source) {
    Properties variablesContext = new Properties();
    Properties configurationVariables = configuration.getVariables();
    if (configurationVariables != null) {
      variablesContext.putAll(configurationVariables);
    }
    applyIncludes(source, variablesContext);
  }

  /**
   * Recursively apply includes through all SQL fragments.
   * @param source Include node in DOM tree
   * @param variablesContext Current context for static variables with values
   *
   * 递归地应用 <include/> 标签。
   * 如果一个 Node 是 <include/> 的话，由于 <include/> 子元素里含有属性，则把这些属性变量收集起来存储到 Properties 中
   * 然后根据 <include refid="id"/> refid 找到相应的 <sql/> Node，然后对 <sql/> Node 再递归应用 <include/> 。<sql/> 中也可能会
   * 引用到 <include/> 。
   * 节点 Node 可以分为三类
   * 1. <include/>
   * 2. 非 <include/> 的元素节点
   * 3. 文本节点
   * 其中第三类最简单，只是把文本中出现的变量用属性变量中对应的值替换。
   * 第二类节点也比较简单，第二类节点会获取到其子元素，然后对这些子元素循环调用递归方法。
   * 第一类节点最复杂，如果对递归的概念不清楚的话很难理解。我是彻底地理解了，但要用合适的语言表达出来还是很困难的。
   * 第一类节点是 <include/> 节点。节点中可能含有属性元素，首先会收集节点中的所有属性名和值
   * 然后根据 <include/> 中的 refid 属性找到相应的 <sql> 元素，再对 <sql/> 元素递归应用 include 。
   * 应用完 include 之后会用 <sql/> 元素 把 <include/> 替换掉。
   * 然后把 <sql/> 元素中的子元素按照先后的顺序挪到其父元素内
   * 最后把 <sql/> 节点删除
   * applyIncludes 其实就是一个递归替换的过程，把属性变量替换，把 <include/> 元素替换为 <sql/> 元素。
   *
   *
   *
   */
  private void applyIncludes(Node source, final Properties variablesContext) {
    if (source.getNodeName().equals("include")) {
      // new full context for included SQL - contains inherited context and new variables from current include node
      Properties fullContext;

      String refid = getStringAttribute(source, "refid");
      // replace variables in include refid value
      refid = PropertyParser.parse(refid, variablesContext);
      Node toInclude = findSqlFragment(refid); // 是复制出来的一个节点，如果不是复制的就会有问题，因为一个 <sql> 节点可能被多个语句所引用。
      // <include> 中可能有属性变量，如果有的话把属性变量解析出来存储到 properties 中。
      Properties newVariablesContext = getVariablesContext(source, variablesContext);
      if (!newVariablesContext.isEmpty()) {
        // merge contexts
        fullContext = new Properties();
        fullContext.putAll(variablesContext);
        fullContext.putAll(newVariablesContext);
      } else {
        // no new context - use inherited fully
        fullContext = variablesContext;
      }
      applyIncludes(toInclude, fullContext);
      if (toInclude.getOwnerDocument() != source.getOwnerDocument()) {
        toInclude = source.getOwnerDocument().importNode(toInclude, true); // todo 为什么要导入节点？
      }
      // 根据 curd 中的 <include> 所引用 sql id，把 <sql> 标签替换
      source.getParentNode().replaceChild(toInclude, source);
      while (toInclude.hasChildNodes()) {
        // todo insertBefore 方法的作用是把 toInclude.getFirstChild() 节点插入到 toInclude 节点的前面，
        // 但是待插入的这个节点如果已经在 toInclude.getParentNode() 这个节点中存在的话，则先把旧的节点删除，再插入新的节点
        // 在下面的代码中 toInclude.getFirstChild() 节点肯定在 toInclude.getParentNode() 节点中，
        // 所以其实是把 toInclude 元素的子元素按照子元素出现的顺序都挪到了和 toInclude 元素的前面，最终 toInclude 没有任何子元素了。
        toInclude.getParentNode().insertBefore(toInclude.getFirstChild(), toInclude);
      }
      // <sql/> 元素的子元素都已经被挪到其前面，所以 <sql/> 元素已经没有用了，删除它
      toInclude.getParentNode().removeChild(toInclude);
    } else if (source.getNodeType() == Node.ELEMENT_NODE) {
      // 如果节点是元素节点的话则遍历其子元素递归应用 include
      NodeList children = source.getChildNodes();
      for (int i=0; i<children.getLength(); i++) {
        applyIncludes(children.item(i), variablesContext);
      }
    } else if (source.getNodeType() == Node.ATTRIBUTE_NODE && !variablesContext.isEmpty()) {
      // 如果节点是属性节点同时属性变量结合不为空的话则把属性中的所有变量全部替换
      // replace variables in all attribute values
      source.setNodeValue(PropertyParser.parse(source.getNodeValue(), variablesContext));
    } else if (source.getNodeType() == Node.TEXT_NODE && !variablesContext.isEmpty()) {
      // 如果节点是文本节点同时属性变量结合不为空的话则把属性中的所有变量全部替换

      // replace variables ins all text nodes
      source.setNodeValue(PropertyParser.parse(source.getNodeValue(), variablesContext));
    }
  }

  private Node findSqlFragment(String refid) {
    refid = builderAssistant.applyCurrentNamespace(refid, true);
    try {
      XNode nodeToInclude = configuration.getSqlFragments().get(refid);
      return nodeToInclude.getNode().cloneNode(true);
    } catch (IllegalArgumentException e) {
      throw new IncompleteElementException("Could not find SQL statement to include with refid '" + refid + "'", e);
    }
  }

  private String getStringAttribute(Node node, String name) {
    return node.getAttributes().getNamedItem(name).getNodeValue();
  }

  /**
   * Read placholders and their values from include node definition. 
   * @param node Include node instance
   * @param inheritedVariablesContext Current context used for replace variables in new variables values
   * @return variables context from include instance (no inherited values)
   * 把 <include></include> 标签中的 <property name="name" value="value"></property>
   * 属性中的 name 和 value 读取出来保存到 Properties 中
   *
   */
  private Properties getVariablesContext(Node node, Properties inheritedVariablesContext) {
    Properties variablesContext = new Properties();
    NodeList children = node.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE) {
        String name = getStringAttribute(n, "name");
        String value = getStringAttribute(n, "value");
        // Replace variables inside
        value = PropertyParser.parse(value, inheritedVariablesContext);
        // Push new value
        Object originalValue = variablesContext.put(name, value);
        if (originalValue != null) {
          throw new BuilderException("Variable " + name + " defined twice in the same include definition");
        }
      }
    }
    return variablesContext;
  }
  
}
