package shfq.one_many_annotation;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;

/**
 * author:      shfq
 * description:
 * create date: 2016/8/2.
 */
public class MapperTest {
    public static void main(String[] args) {
        queryPeopleByMapper();
//        queryPetByMapper();

    }

    private static void queryPeopleByMapper() {
        SqlSession session = null;
        try {
            Reader reader = Resources.getResourceAsReader("shfq/mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            session = sqlSessionFactory.openSession();
            PeopleMapper mapper = session.getMapper(PeopleMapper.class);
            People people = mapper.selectPeople(1);
            System.out.println(people.getName());

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (session != null) {
                session.close();
            }
        }

    }

    private static void queryPetByMapper() {
//        SqlSession session = null;
//        try {
//            Reader reader = Resources.getResourceAsReader("shfq/mybatis-config.xml");
//            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
//            session = sqlSessionFactory.openSession();
//            PetMapper mapper = session.getMapper(PetMapper.class);
//            Pet pet = mapper.selectPet(1);
//            System.out.println("");
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            if (session != null) {
//                session.close();
//            }
//        }

    }
}
