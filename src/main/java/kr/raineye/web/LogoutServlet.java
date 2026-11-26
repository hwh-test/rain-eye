package kr.raineye.web;

import java.io.IOException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.raineye.auth.LoginUser;

/** 로그아웃은 POST로만 (다른 사이트의 링크·이미지로 로그아웃되지 않게) */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        AuthSupport.signOut(req);
        AuthSupport.redirect(req, res, "/login?logout=1");
    }

    /** 주소창에 /logout을 직접 치면 로그아웃하지 않고 원래 화면으로 */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        LoginUser user = AuthSupport.currentUser(req);
        if (user == null) AuthSupport.redirect(req, res, "/login");
        else AuthSupport.redirectHome(req, res, user.getRole());
    }
}
