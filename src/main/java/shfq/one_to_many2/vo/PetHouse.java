package shfq.one_to_many2.vo;

import shfq.one_many_annotation.People;

import java.util.List;

/**
 * Created by shfq on 2016/10/14.
 */
public class PetHouse {
    private int id;
    private String location;
    private People2 owner;
    private List<Pet2> pets;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public People2 getOwner() {
        return owner;
    }

    public void setOwner(People2 owner) {
        this.owner = owner;
    }

    public List<Pet2> getPets() {
        return pets;
    }

    public void setPets(List<Pet2> pets) {
        this.pets = pets;
    }
}
