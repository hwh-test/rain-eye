package kr.raineye.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * MariaDB 연결을 만든다. 모든 DAO는 여기서 Connection을 얻고 try-with-resources로 닫는다.
 * (나중에 사용자가 늘면 Tomcat DataSource 커넥션 풀로 바꿀 수 있도록 이 클래스 한 곳에만 둔다.)
 */
public final class DBUtil {

    static {
        try {
            // WEB-INF/lib에 둔 드라이버를 Tomcat 웹앱 클래스로더에서 확실히 등록하기 위해 명시적으로 로드
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[DBUtil] MariaDB 드라이버가 없습니다. WEB-INF/lib에 mariadb-java-client jar를 넣으세요.");
        }
    }

    private DBUtil() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                AppConfig.get("db.url"),
                AppConfig.get("db.user"),
                AppConfig.get("db.password"));
    }
}
