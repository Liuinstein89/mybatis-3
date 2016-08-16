package shfq.discriminator.mappers;

import org.apache.ibatis.annotations.Select;
import shfq.discriminator.vo.Vehicle;

/**
 * author:      shfq
 * description:
 * create date: 2016/8/1.
 */
public interface VehicleMapper {
//    @TypeDiscriminator(column = "type", cases = {
//                                                  @Case(value = "car", type = Car.class, results = {@Result(id = true, column = "id", property = "id", javaType = int.class)}),
//                                                  @Case(value = "bus", type= Bus.class)})
//
//    @Select("SELECT * FROM vehicle WHERE id=#{id}")
    @Select("select * from vehicle where id=#{id}")
    Vehicle selectVehicle(int id);

}
