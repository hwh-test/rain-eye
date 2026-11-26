package kr.raineye.filter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.raineye.auth.AccessRule;
import kr.raineye.auth.LoginUser;
import kr.raineye.auth.Role;
import kr.raineye.web.AuthSupport;

/**
 * 권한 검사를 한 곳에서 처리하는 필터. 규칙은 AccessRule에 있습니다.
 * 요청 파라미터를 읽지 않으므로 EncodingFilter와 실행 순서가 바뀌어도 한글이 깨지지 않습니다.
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = "/*")
public class AuthFilter extends HttpFilter {
    private static final long serialVersionUID = 1L;

    private static final String FORBIDDEN_VIEW = "/WEB-INF/views/error/403.jsp";

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        String path = pathOf(req);
        LoginUser user = AuthSupport.currentUser(req);
        Role role = (user == null) ? null : user.getRole();

        switch (AccessRule.decide(path, role, isDirectLocal(req))) {
            case ALLOW:
                if (user != null && !AccessRule.isPublic(path)) {
                    // 로그아웃 후 "뒤로 가기"로 이전 화면이 보이지 않게 캐시 금지
                    res.setHeader("Cache-Control", "no-store");
                }
                chain.doFilter(req, res);
                return;

            case LOGIN:
                if (AccessRule.isApi(path)) {
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED); // 4단계 이후 fetch 요청용
                    return;
                }
                String next = "";
                if ("GET".equals(req.getMethod())) {
                    String q = req.getQueryString();
                    next = "&next=" + URLEncoder.encode(q == null ? path : path + "?" + q, StandardCharsets.UTF_8);
                }
                AuthSupport.redirect(req, res, "/login?expired=1" + next);
                return;

            case FORBIDDEN:
                if (AccessRule.isApi(path)) {
                    res.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                res.setHeader("Cache-Control", "no-store");
                req.getRequestDispatcher(FORBIDDEN_VIEW).forward(req, res);
                return;

            case ROOT_REDIRECT:
            default:
                if (user == null) AuthSupport.redirect(req, res, "/login");
                else AuthSupport.redirectHome(req, res, role);
        }
    }

    /** 컨텍스트 경로를 뺀 경로. Tomcat이 이미 디코딩·정규화한 값을 씀 (/../ 같은 우회 방지) */
    private static String pathOf(HttpServletRequest req) {
        String sp = req.getServletPath();
        String pi = req.getPathInfo();
        String p = (sp == null ? "" : sp) + (pi == null ? "" : pi);
        return p.isEmpty() ? "/" : p;
    }

    /**
     * PC 브라우저가 localhost로 직접 연 요청인지.
     * Cloudflare 터널을 거친 요청도 Tomcat에는 127.0.0.1에서 온 것처럼 보이므로,
     * 터널이 붙이는 Cf-Connecting-IP / X-Forwarded-For 헤더가 있으면 직접 접속이 아닌 것으로 봅니다.
     */
    private static boolean isDirectLocal(HttpServletRequest req) {
        String addr = req.getRemoteAddr();
        boolean loopback = "127.0.0.1".equals(addr) || "0:0:0:0:0:0:0:1".equals(addr) || "::1".equals(addr);
        return loopback
                && req.getHeader("Cf-Connecting-IP") == null
                && req.getHeader("X-Forwarded-For") == null;
    }
}
