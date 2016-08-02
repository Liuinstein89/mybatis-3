package shfq.one_many_annotation;

import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * author:      shfq
 * description:
 * create date: 2016/8/2.
 */
public interface PetMapper {
    @Select("select * from pet where ownerId=#{ownerId}")
    List<Pet> selectPets(int ownerId);


}
