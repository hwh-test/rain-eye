package kr.raineye.web;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kr.raineye.auth.AccessRule;
import kr.raineye.auth.LoginUser;
import kr.raineye.auth.Role;

/** 서블릿들이 같이 쓰는 로그인 관련 도우미 */
public final class AuthSupport {

    /** 로그인 유지 시간: 2시간 (시연·리허설 중 끊기지 않게) */
    public static final int SESSION_SECONDS = 2 * 60 * 60;

    private AuthSupport() {}

    public static LoginUser currentUser(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) return null;
        Object o = s.getAttribute(LoginUser.SESSION_KEY);
        return (o instanceof LoginUser) ? (LoginUser) o : null;
    }

    /**
     * 로그인 성공 처리. 세션 고정 공격을 막기 위해 기존 세션을 버리고 새 세션을 만듭니다.
     */
    public static void signIn(HttpServletRequest req, LoginUser user) {
        HttpSession old = req.getSession(false);
        if (old != null) old.invalidate();
        HttpSession s = req.getSession(true);
        s.setMaxInactiveInterval(SESSION_SECONDS);
        s.setAttribute(LoginUser.SESSION_KEY, user);
    }

    public static void signOut(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s != null) s.invalidate();
    }

    public static void redirect(HttpServletRequest req, HttpServletResponse res, String path) throws IOException {
        res.sendRedirect(req.getContextPath() + path);
    }

    public static void redirectHome(HttpServletRequest req, HttpServletResponse res, Role role) throws IOException {
        redirect(req, res, role.getHomePath());
    }

    /**
     * 로그인 전에 열려던 주소(next)로 돌려보내도 되는지 검사.
     * 같은 사이트 안의 경로이고, 그 역할로 열 수 있는 곳일 때만 그 경로를 돌려주고 아니면 null.
     * (외부 사이트로 튕기는 오픈 리다이렉트 방지)
     */
    public static String safeNext(String next, Role role) {
        if (next == null || next.isEmpty()) return null;
        if (!next.startsWith("/") || next.startsWith("//")) return null;
        if (next.contains("\\") || next.contains("\r") || next.contains("\n")) return null;

        String pathOnly = next;
        int q = pathOnly.indexOf('?');
        if (q >= 0) pathOnly = pathOnly.substring(0, q);
        if (pathOnly.contains("..") || pathOnly.contains(";")) return null;
        if (pathOnly.equals("/login") || pathOnly.equals("/signup") || AccessRule.isRoot(pathOnly)) return null;

        return AccessRule.decide(pathOnly, role, false) == AccessRule.Decision.ALLOW ? next : null;
    }
}
