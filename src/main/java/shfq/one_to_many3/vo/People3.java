package shfq.one_to_many3.vo;

import shfq.one_many_annotation.People;
import shfq.one_to_many2.vo.*;

import java.util.List;

/**
 * author:      shfq
 * description:
 * create date: 2016/8/2.
 */
public class People3 {
    private int id;
    private String name;
    private List<People3> children;
    private People3 father;

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

    public List<People3> getChildren() {
        return children;
    }

    public void setChildren(List<People3> children) {
        this.children = children;
    }

    public People3 getFather() {
        return father;
    }

    public void setFather(People3 father) {
        this.father = father;
    }
}
