package shfq.vo;

import org.apache.ibatis.type.Alias;

/**
 * author:      shfq
 * description:
 * create date: 2016/7/11.
 */
@Alias("my_person")
public class Person {
    private int id;
    private String name;

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
}
