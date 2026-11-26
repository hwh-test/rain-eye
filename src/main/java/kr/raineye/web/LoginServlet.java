package kr.raineye.web;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.raineye.auth.LoginUser;
import kr.raineye.auth.PasswordUtil;
import kr.raineye.auth.Role;
import kr.raineye.dao.UserDao;

/** M1 로그인 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String VIEW = "/WEB-INF/views/auth/login.jsp";
    private static final String FAIL_MSG = "이메일 또는 비밀번호가 올바르지 않습니다.";

    private final transient UserDao userDao = new UserDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        LoginUser user = AuthSupport.currentUser(req);
        if (user != null) {
            AuthSupport.redirectHome(req, res, user.getRole());
            return;
        }
        req.setAttribute("email", req.getParameter("email"));
        req.setAttribute("next", req.getParameter("next"));
        if (req.getParameter("joined") != null) req.setAttribute("info", "가입이 완료되었습니다. 로그인해 주세요.");
        if (req.getParameter("logout") != null) req.setAttribute("info", "로그아웃되었습니다.");
        if (req.getParameter("expired") != null) req.setAttribute("info", "로그인이 필요합니다.");
        req.getRequestDispatcher(VIEW).forward(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String email = normalizeEmail(req.getParameter("email"));
        String password = req.getParameter("password");
        String next = req.getParameter("next");

        req.setAttribute("email", email);
        req.setAttribute("next", next);

        if (email.isEmpty() || password == null || password.isEmpty()) {
            fail(req, res, "이메일과 비밀번호를 입력해 주세요.");
            return;
        }

        UserDao.UserRow row;
        try {
            row = userDao.findByEmail(email);
        } catch (SQLException e) {
            log("로그인 조회 실패", e);
            fail(req, res, "서버 오류로 로그인하지 못했습니다. 잠시 후 다시 시도해 주세요.");
            return;
        }

        // 없는 이메일이어도 BCrypt 비교를 한 번 해서 응답 시간 차이로 가입 여부가 드러나지 않게 함
        boolean ok = PasswordUtil.check(password, row == null ? null : row.passwordHash);
        Role role = (row == null) ? null : Role.from(row.role);
        if (!ok || role == null) {
            fail(req, res, FAIL_MSG);
            return;
        }

        LoginUser user = new LoginUser(row.userId, row.email, row.nickname, role, row.districtCode);
        AuthSupport.signIn(req, user);

        String target = AuthSupport.safeNext(next, role);
        AuthSupport.redirect(req, res, target != null ? target : role.getHomePath());
    }

    private void fail(HttpServletRequest req, HttpServletResponse res, String msg) throws ServletException, IOException {
        req.setAttribute("error", msg);
        req.getRequestDispatcher(VIEW).forward(req, res);
    }

    static String normalizeEmail(String s) {
        return (s == null) ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
