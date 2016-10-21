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

import java.lang.reflect.ReflectPermission;

/**
 * author:      shfq
 * description:
 * create date: 2016/7/26.
 * 虚拟机参数设置：-Djava.security.manager=default
 * 透视JAVA——反编译、修补和逆向工程技术
 * 在开启了 securityManager 之后，Object o = Object.class.getDeclaredConstructors(); 会报错
 */
public class TestSecurityManager {
    public static void main(String[] args) {
//        try {
//            System.out.println("hello");
//            canAccessPrivateMethods();
//            Constructor[] constructors = TestReflector.class.getDeclaredConstructors();
//            constructors[0].setAccessible(true);
//            TestReflector reflector = (TestReflector) constructors[0].newInstance("test parameter");
//            Method method = TestReflector.class.getDeclaredMethod("func1");
//            method.invoke(reflector);
//            System.out.println("");
//
//        } catch (Exception e) {
//
//            System.out.println(e);
//        }
        String s = "\\t";
        System.out.println(s.length());
        System.out.println(s);

    }
    private static boolean canAccessPrivateMethods() {
        try {
            SecurityManager securityManager = System.getSecurityManager();
            if (null != securityManager) {
                securityManager.checkPermission(new ReflectPermission("suppressAccessChecks"));
            }
        } catch (SecurityException e) {
            return false;
        }
        return true;
    }
}
