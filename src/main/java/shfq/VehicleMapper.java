package shfq;

import org.apache.ibatis.annotations.Case;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.TypeDiscriminator;
import shfq.vo.Bus;
import shfq.vo.Car;
import shfq.vo.Vehicle;

/**
 * author:      shfq
 * description:
 * create date: 2016/8/1.
 */
public interface VehicleMapper {
    @TypeDiscriminator(column = "type", cases = {
                                                  @Case(value = "car", type = Car.class, results = {@Result(id = true, column = "id", property = "id", javaType = int.class)}),
                                                  @Case(value = "bus", type= Bus.class)})

    @Select("SELECT * FROM vehicle WHERE id=#{id}")
    Vehicle selectVehicle(int id);

}
