package shfq.annotation;

import org.apache.ibatis.annotations.Select;
import shfq.nested_result_map.vo.Address;

/**
 * Created by shfq on 2016/10/23.
 */
public interface AddressMapper {
    @Select("select * from address where id=#{id}")
    Address queryById(int id);
}
