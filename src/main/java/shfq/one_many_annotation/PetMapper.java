package shfq.one_many_annotation;

import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * author:      shfq
 * description:
 * create date: 2016/8/2.
 */
public interface PetMapper {
    @Results(value = {@Result(column = "owner_id", property = "owner", one = @One(select = "shfq.one_many_annotation.PeopleMapper.selectPeople"))})
    @Select("select * from pet where owner_id=#{ownerId}")
    List<Pet> selectPets(int ownerId);

//    @Results(value = {@Result(column = "owner_id", property = "owner", one = @One(select = "shfq.one_many_annotation.PeopleMapper.selectPeople"))})
//    @Select("select * from pet where id=#{id}")
//    Pet selectPet(int id);


}
