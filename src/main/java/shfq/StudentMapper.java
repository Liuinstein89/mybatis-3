package shfq;

import org.apache.ibatis.annotations.Select;

/**
 * author:      shfq
 * description:
 * create date: 2016/5/11.
 */
public interface StudentMapper {
    @Select("SELECT * FROM student WHERE id=#{id}")
    Student selectStudent(int id);
}
