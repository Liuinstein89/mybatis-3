package shfq;

import java.sql.*;

/**
 * Created by shfq on 2016/7/24.
 */
public class JdbcTest {
    public static void main(String[] args) {
        try {
            Connection connection = getConection();
            Statement statement = connection.createStatement();
            statement.execute("SELECT * FROM address a , student s where a.id=s.address_id");
            ResultSet resultSet = statement.getResultSet();
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static Connection getConection() throws Exception {
        String url = "jdbc:mysql://localhost:3306/mybatis-demo";
        String user = "root";
        String password = "root";
        return DriverManager.getConnection(url, user, password);
    }
}
