package shfq.one_many_annotation;

import org.apache.ibatis.annotations.Many;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

/**
 * author:      shfq
 * description:
 * create date: 2016/8/2.
 */
public interface PeopleMapper {
    @Results(value = {@Result(id = true, column = "id", property = "pets", many = @Many(select = "shfq.one_many_annotation.PetMapper.selectPets"))})
    @Select("select * from people where id=#{id}")
    People selectPeople(int id);
}
