package kr.raineye.dev;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.raineye.util.AppConfig;
import kr.raineye.util.DBUtil;
import kr.raineye.util.Json;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 0단계 완료 확인용: Java·Tomcat·설정 파일·DB 접속 상태를 JSON으로 보여 준다.
 * GET /health
 */
@WebServlet("/health")
public class HealthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");

        String configStatus = AppConfig.getLoadError() == null ? "OK" : AppConfig.getLoadError();
        String dbStatus;
        String dbVersion = null;
        String dbTime = null;

        try (Connection con = DBUtil.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT VERSION(), NOW(), DATABASE()")) {
            rs.next();
            dbVersion = rs.getString(1);
            dbTime = rs.getString(2);
            dbStatus = "OK (database=" + rs.getString(3) + ")";
        } catch (Exception e) {
            dbStatus = "FAIL: " + e.getMessage();
        }

        String json = "{"
                + "\"java\":" + Json.str(System.getProperty("java.version")) + ","
                + "\"server\":" + Json.str(getServletContext().getServerInfo()) + ","
                + "\"config\":" + Json.str(configStatus) + ","
                + "\"db\":" + Json.str(dbStatus) + ","
                + "\"dbVersion\":" + Json.str(dbVersion) + ","
                + "\"dbTime\":" + Json.str(dbTime) + ","
                + "\"secure\":" + req.isSecure()
                + "}";
        resp.getWriter().write(json);
    }
}
