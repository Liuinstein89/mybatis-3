package shfq.one_many_annotation;

/**
 * author:      shfq
 * description:
 * create date: 2016/8/2.
 */
public class Pet {
    private int id;
    private String name;
    private People owner;

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

    public People getOwner() {
        return owner;
    }

    public void setOwner(People owner) {
        this.owner = owner;
    }
}
