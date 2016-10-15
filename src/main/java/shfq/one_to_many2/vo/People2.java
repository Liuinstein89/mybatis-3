package shfq.one_to_many2.vo;

import java.util.List;

/**
 * author:      shfq
 * description:
 * create date: 2016/8/2.
 */
public class People2 {
    private int id;
    private String name;
    private List<Pet2> pets;

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

    public List<Pet2> getPets() {
        return pets;
    }

    public void setPets(List<Pet2> pets) {
        this.pets = pets;
    }
}
