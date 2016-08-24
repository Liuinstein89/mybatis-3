package shfq.select_key;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import shfq.one_many_annotation.People;
import shfq.one_many_annotation.Pet;

import java.io.IOException;
import java.io.Reader;

/**
 * author:      shfq
 * description:
 * create date: 2016/8/24.
 */
public class PetTest {
    public static void main(String[] args) {
        Pet pet = new Pet();
        pet.setName("cat");
        pet.setOwner(new People());
        pet.getOwner().setId(1);

        insertPet(pet);
        System.out.println(pet.getId());

    }
    private static void insertPet(Pet pet) {
        SqlSession session = null;
        try {


            Reader reader = Resources.getResourceAsReader("shfq/select_key/xml/mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            session = sqlSessionFactory.openSession(true);
            int i;
            i = session.insert("shfq.select_key.xml.pet.insertPet", pet);
            System.out.println(i);
//            session.commit();
            session.close();
            System.out.println(i);
        } catch (IOException e) {
            e.printStackTrace();
            if (session != null) {
//                session.rollback();
            }

        }finally {
            if (session != null) {
                session.close();
            }
        }
    }
}
