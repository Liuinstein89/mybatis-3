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
package shfq;

/**
 * author:      shfq
 * description:
 * create date: 2016/7/26.
 */
public class TestReflector {
    private String s;

    private TestReflector(String s) {
        this.s = s;
    }

    private void func() {
        System.out.println("this is a private function");
    }

    public void func1() {
        System.out.println(s);
    }

}
