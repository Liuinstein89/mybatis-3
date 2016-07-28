package shfq;

import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.type.IntegerTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.StringTypeHandler;

/**
 * author:      shfq
 * description:
 * create date: 2016/7/28.
 */
public interface AddressMapper {
    @Select("SELECT * FROM address WHERE id=#{id}")
    @ConstructorArgs(value = {
            @Arg(id = true, column = "id", javaType = int.class, jdbcType = JdbcType.INTEGER, typeHandler = IntegerTypeHandler.class),
            @Arg(column = "name", javaType = String.class, jdbcType = JdbcType.VARCHAR, typeHandler = StringTypeHandler.class),
            @Arg(column = "post_code", javaType = String.class)})
    Address selectAddress(int id);

}
