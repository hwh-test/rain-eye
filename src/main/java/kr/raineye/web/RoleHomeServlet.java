package kr.raineye.web;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 2단계용 임시 첫 화면 3개. 역할 분리가 되는지만 보여 줍니다.
 * 3단계에서 /m/map, 6단계에서 /admin/dashboard 실제 서블릿을 만들면
 * 그 경로를 아래 urlPatterns에서 지우세요(같은 경로를 두 서블릿이 가지면 Tomcat이 시작되지 않음).
 */
@WebServlet(urlPatterns = { "/m/map", "/admin/dashboard", "/crew/orders" })
public class RoleHomeServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String view;
        switch (req.getServletPath()) {
            case "/m/map": view = "/WEB-INF/views/citizen/map.jsp"; break;
            case "/admin/dashboard": view = "/WEB-INF/views/official/dashboard.jsp"; break;
            default: view = "/WEB-INF/views/crew/orders.jsp";
        }
        req.getRequestDispatcher(view).forward(req, res);
    }
}
