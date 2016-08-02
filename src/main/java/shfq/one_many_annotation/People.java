package shfq.one_many_annotation;

import java.util.List;

/**
 * author:      shfq
 * description:
 * create date: 2016/8/2.
 */
public class People {
    private int id;
    private String name;
    private List<Pet> pets;

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

    public List<Pet> getPets() {
        return pets;
    }

    public void setPets(List<Pet> pets) {
        this.pets = pets;
    }
}
