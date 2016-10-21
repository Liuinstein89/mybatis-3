/**
 *    Copyright 2009-2015 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
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
