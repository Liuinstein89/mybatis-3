package shfq.nested_result_map.mappers;

import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.type.IntegerTypeHandler;
import org.apache.ibatis.type.JdbcType;
import shfq.nested_result_map.vo.Address;

/**
 * author:      shfq
 * description:
 * create date: 2016/7/28.
 */
public interface AddressMapper {
    @Select("SELECT * FROM address WHERE id=#{id}")
    @ConstructorArgs(value = {
            @Arg(id = true, column = "id", javaType = int.class, jdbcType = JdbcType.INTEGER, typeHandler = IntegerTypeHandler.class)})
    Address selectAddress(int id);

}
