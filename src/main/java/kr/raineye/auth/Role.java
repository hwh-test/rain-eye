package kr.raineye.auth;

/**
 * users.role 값과 역할별 첫 화면.
 * 3·6단계에서 실제 화면이 생기면 homePath만 바꾸면 됩니다.
 */
public enum Role {
    CITIZEN("/m/map", "시민"),
    OFFICIAL("/admin/dashboard", "공무원"),
    CREW("/crew/orders", "현장 기동반");

    private final String homePath;
    private final String label;

    Role(String homePath, String label) {
        this.homePath = homePath;
        this.label = label;
    }

    /** 컨텍스트 경로(/rain-eye)를 뺀 첫 화면 경로 */
    public String getHomePath() {
        return homePath;
    }

    public String getLabel() {
        return label;
    }

    /** DB 문자열을 Role로. 알 수 없는 값이면 null (로그인 실패로 처리) */
    public static Role from(String value) {
        if (value == null) return null;
        for (Role r : values()) {
            if (r.name().equals(value.trim())) return r;
        }
        return null;
    }
}
