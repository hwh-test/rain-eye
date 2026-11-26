package kr.raineye.auth;

/**
 * 경로별 접근 규칙. 서블릿 API에 의존하지 않아 단독으로 테스트할 수 있습니다.
 * AuthFilter가 이 결과대로 통과·로그인 이동·403을 처리합니다.
 *
 * 규칙 (위에서부터 먼저 맞는 것)
 *  1. 공개: /login, /signup, /css/*, /js/*, /img/*, /favicon.ico
 *  2. 루트(/, /index.jsp = 0단계 점검표): PC에서 localhost로 직접 열면 점검표, 그 밖(터널·폰)은 로그인/역할 홈으로 이동
 *  3. 개발 점검용(/dev/*, /health, /maptest.jsp): PC localhost 직접 접속 또는 공무원만
 *  4. 그 밖의 모든 경로는 로그인 필요
 *  5. 역할 전용: /m/* 시민, /admin/* 공무원, /crew/* 기동반
 */
public final class AccessRule {

    public enum Decision {
        /** 그대로 통과 */
        ALLOW,
        /** 로그인 필요 → 로그인 화면(또는 /api/는 401) */
        LOGIN,
        /** 로그인은 했지만 권한 없음 → 403 */
        FORBIDDEN,
        /** 루트를 터널·폰에서 연 경우 → 로그인 화면 또는 역할 홈으로 이동 */
        ROOT_REDIRECT
    }

    private AccessRule() {}

    /**
     * @param path        컨텍스트 경로를 뺀 경로 (servletPath + pathInfo), 예: "/m/map"
     * @param role        로그인한 역할, 비로그인이면 null
     * @param directLocal PC에서 localhost로 직접 연 요청인지 (터널을 거친 요청은 false)
     */
    public static Decision decide(String path, Role role, boolean directLocal) {
        if (path == null || path.isEmpty()) path = "/";

        if (isPublic(path)) return Decision.ALLOW;

        if (isRoot(path)) {
            return directLocal ? Decision.ALLOW : Decision.ROOT_REDIRECT;
        }

        if (isDev(path)) {
            if (directLocal || role == Role.OFFICIAL) return Decision.ALLOW;
            return (role == null) ? Decision.LOGIN : Decision.FORBIDDEN;
        }

        if (role == null) return Decision.LOGIN;

        Role required = requiredRole(path);
        if (required != null && required != role) return Decision.FORBIDDEN;

        return Decision.ALLOW;
    }

    /** 경로를 쓰려면 꼭 필요한 역할. 역할 제한이 없으면 null */
    public static Role requiredRole(String path) {
        if (startsWithDir(path, "/m")) return Role.CITIZEN;
        if (startsWithDir(path, "/admin")) return Role.OFFICIAL;
        if (startsWithDir(path, "/crew")) return Role.CREW;
        return null;
    }

    public static boolean isPublic(String path) {
        return path.equals("/login")
                || path.equals("/signup")
                || path.equals("/favicon.ico")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/img/");
    }

    public static boolean isRoot(String path) {
        return path.equals("/") || path.equals("/index.jsp");
    }

    public static boolean isDev(String path) {
        return startsWithDir(path, "/dev")
                || path.equals("/health")
                || path.equals("/maptest.jsp");
    }

    public static boolean isApi(String path) {
        return startsWithDir(path, "/api");
    }

    /** "/m"이면 "/m"과 "/m/..."는 맞고 "/map"은 아님 */
    private static boolean startsWithDir(String path, String dir) {
        return path.equals(dir) || path.startsWith(dir + "/");
    }
}
