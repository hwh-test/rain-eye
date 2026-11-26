package kr.raineye.auth;

import java.io.Serializable;

/**
 * 세션에 저장하는 로그인 사용자 정보. 비밀번호 해시는 넣지 않습니다.
 * 마일리지처럼 자주 바뀌는 값은 세션에 두지 말고 화면마다 DB에서 읽습니다.
 */
public class LoginUser implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 세션 속성 이름 */
    public static final String SESSION_KEY = "loginUser";

    private final long userId;
    private final String email;
    private final String nickname;
    private final Role role;
    private final String districtCode;

    public LoginUser(long userId, String email, String nickname, Role role, String districtCode) {
        this.userId = userId;
        this.email = email;
        this.nickname = nickname;
        this.role = role;
        this.districtCode = districtCode;
    }

    public long getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getNickname() { return nickname; }
    public Role getRole() { return role; }
    public String getDistrictCode() { return districtCode; }
}
