package shfq.composite_column;

/**
 * author:      shfq
 * description:
 * create date: 2016/8/10.
 */
public class Clerk {
    private int id;
    private String name;
    private ClerkAddress clerkAddress;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ClerkAddress getClerkAddress() {
        return clerkAddress;
    }

    public void setClerkAddress(ClerkAddress clerkAddress) {
        this.clerkAddress = clerkAddress;
    }
}
