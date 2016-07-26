package shfq;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
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
        try {
            System.out.println("hello");
            canAccessPrivateMethods();
            Constructor[] constructors = TestReflector.class.getDeclaredConstructors();
            constructors[0].setAccessible(true);
            TestReflector reflector = (TestReflector) constructors[0].newInstance("test parameter");
            Method method = TestReflector.class.getDeclaredMethod("func1");
            method.invoke(reflector);
            System.out.println("");

        } catch (Exception e) {

            System.out.println(e);
        }
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
