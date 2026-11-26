package kr.raineye.web;

import java.io.IOException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.raineye.auth.LoginUser;

/** /home → 역할별 첫 화면으로 이동 (403 화면의 "내 화면으로" 버튼 등에서 사용) */
@WebServlet("/home")
public class HomeServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        LoginUser user = AuthSupport.currentUser(req);
        if (user == null) AuthSupport.redirect(req, res, "/login"); // AuthFilter가 먼저 막지만 안전하게 한 번 더
        else AuthSupport.redirectHome(req, res, user.getRole());
    }
}
