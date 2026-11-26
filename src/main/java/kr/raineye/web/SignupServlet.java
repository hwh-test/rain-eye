package kr.raineye.web;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.raineye.auth.LoginUser;
import kr.raineye.auth.PasswordUtil;
import kr.raineye.dao.DistrictDao;
import kr.raineye.dao.UserDao;

/**
 * 시민 회원가입. 공무원·기동반 계정은 화면에서 만들 수 없습니다(시드로 미리 발급).
 * 가입이 끝나면 자동 로그인하지 않고 로그인 화면으로 보냅니다(시연 순서: 회원가입 → 로그인 → 지도).
 */
@WebServlet("/signup")
public class SignupServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String VIEW = "/WEB-INF/views/auth/signup.jsp";
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern HAS_LETTER = Pattern.compile(".*[A-Za-z].*");
    private static final Pattern HAS_DIGIT = Pattern.compile(".*[0-9].*");

    private final transient UserDao userDao = new UserDao();
    private final transient DistrictDao districtDao = new DistrictDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        LoginUser user = AuthSupport.currentUser(req);
        if (user != null) {
            AuthSupport.redirectHome(req, res, user.getRole());
            return;
        }
        show(req, res, new ArrayList<>());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String email = LoginServlet.normalizeEmail(req.getParameter("email"));
        String password = nz(req.getParameter("password"));
        String password2 = nz(req.getParameter("password2"));
        String nickname = nz(req.getParameter("nickname")).trim();
        String districtCode = nz(req.getParameter("districtCode")).trim();

        req.setAttribute("email", email);
        req.setAttribute("nickname", nickname);
        req.setAttribute("districtCode", districtCode);

        List<String> errors = validate(email, password, password2, nickname);

        try {
            if (districtCode.isEmpty() || !districtDao.exists(districtCode)) {
                errors.add("사는 동네(행정동)를 선택해 주세요.");
            }
            if (errors.isEmpty() && userDao.existsByEmail(email)) {
                errors.add("이미 가입된 이메일입니다.");
            }
            if (!errors.isEmpty()) {
                show(req, res, errors);
                return;
            }

            userDao.insertCitizen(email, PasswordUtil.hash(password), nickname, districtCode);

        } catch (UserDao.DuplicateEmailException e) {
            errors.add("이미 가입된 이메일입니다.");
            show(req, res, errors);
            return;
        } catch (SQLException e) {
            log("회원가입 실패", e);
            errors.add("서버 오류로 가입하지 못했습니다. 잠시 후 다시 시도해 주세요.");
            show(req, res, errors);
            return;
        }

        AuthSupport.redirect(req, res, "/login?joined=1&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8));
    }

    /** 입력값 검사 (DB가 필요 없는 것만). 화면의 HTML 검사와 별개로 서버에서 반드시 다시 검사합니다. */
    static List<String> validate(String email, String password, String password2, String nickname) {
        List<String> errors = new ArrayList<>();
        if (email.isEmpty() || email.length() > 100 || !EMAIL.matcher(email).matches()) {
            errors.add("이메일 형식이 올바르지 않습니다.");
        }
        if (password.length() < 8 || !HAS_LETTER.matcher(password).matches() || !HAS_DIGIT.matcher(password).matches()) {
            errors.add("비밀번호는 영문과 숫자를 섞어 8자 이상으로 입력해 주세요.");
        } else if (password.length() > 64 || !PasswordUtil.fitsBcrypt(password)) {
            errors.add("비밀번호가 너무 깁니다.");
        }
        if (!password.equals(password2)) {
            errors.add("비밀번호 확인이 일치하지 않습니다.");
        }
        int nlen = nickname.codePointCount(0, nickname.length());
        if (nlen < 2 || nlen > 20) {
            errors.add("닉네임은 2~20자로 입력해 주세요.");
        }
        return errors;
    }

    private void show(HttpServletRequest req, HttpServletResponse res, List<String> errors)
            throws ServletException, IOException {
        try {
            req.setAttribute("districts", districtDao.findAll());
        } catch (SQLException e) {
            log("행정동 목록 조회 실패", e);
            req.setAttribute("districts", new ArrayList<DistrictDao.District>());
            errors.add("행정동 목록을 불러오지 못했습니다(DB 연결 확인).");
        }
        req.setAttribute("errors", errors);
        req.getRequestDispatcher(VIEW).forward(req, res);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
