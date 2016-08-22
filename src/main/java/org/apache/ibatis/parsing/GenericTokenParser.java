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
package org.apache.ibatis.parsing;

/**
 * @author Clinton Begin
 */
public class GenericTokenParser {

  private final String openToken;
  private final String closeToken;
  private final TokenHandler handler;

  public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
    this.openToken = openToken;
    this.closeToken = closeToken;
    this.handler = handler;
  }

  public String parse(String text) {
    StringBuilder builder = new StringBuilder();
    if (text != null && text.length() > 0) {
      char[] src = text.toCharArray();
      int offset = 0;
      int start = text.indexOf(openToken, offset);
      // 循环解析多个参数
      // 先确定 offset 再根据 start = text.indexOf(openToken, offset);
      // 计算出 start
      while (start > -1) {
        // 有转义字符 去掉转义字符
        if (start > 0 && src[start - 1] == '\\') {
          // the variable is escaped. remove the backslash.
          builder.append(src, offset, start - offset - 1).append(openToken);
          offset = start + openToken.length();
        } else {
          int end = text.indexOf(closeToken, start);
          // 标签应该是成对儿出现的，结束标签如果不存在的话则说明解析已经完成
          // 把 offset 置为 src.length 在循环的末尾处
          // start = text.indexOf(openToken, offset);
          // 所以 start 为 -1 ，退出循环
          if (end == -1) {
            builder.append(src, offset, src.length - offset);
            offset = src.length;
          } else {
            // 找到了一对儿标签即 开始标签和结束标签
            // 把从偏移量开始处追加 start-offset 个字符，其中不包括 start 对应的字符，也就是说不会追加 openToken 字符
            builder.append(src, offset, start - offset);
            // 跳过 openToken
            offset = start + openToken.length();
            // 解析出开始标签和结束标签之间的内容
            String content = new String(src, offset, end - offset);
            // 处理内容结果 不同的 handler 有不同的处理策略
            builder.append(handler.handleToken(content));
            // 重新计算偏移量 偏移量等于结束标签索引位置+结束标签的长度
            offset = end + closeToken.length();
          }
        }
        // 处理一对儿标签结束，从下一个位置重新开始处理
        start = text.indexOf(openToken, offset);
      }
      // TODO 什么时候会出现这种情况 当结束标签不是在 text 的最末尾的时候会出现这种情况
      if (offset < src.length) {
        builder.append(src, offset, src.length - offset);
      }
    }
    return builder.toString();
  }

}
