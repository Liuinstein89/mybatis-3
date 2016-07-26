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
