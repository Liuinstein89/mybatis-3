package shfq.one_to_many2.vo;

/**
 * Created by shfq on 2016/10/14.
 */
public class Pet2 {
    private int id;
    private String name;
    // 宠物的窝
    private PetHouse petHouse;
    private int ownerId;

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

    public PetHouse getPetHouse() {
        return petHouse;
    }

    public void setPetHouse(PetHouse petHouse) {
        this.petHouse = petHouse;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }
}
