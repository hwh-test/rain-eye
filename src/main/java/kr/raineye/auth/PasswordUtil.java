package kr.raineye.auth;

import java.nio.charset.StandardCharsets;

import org.mindrot.jbcrypt.BCrypt;

/** BCrypt 해시 (jBCrypt 0.4, $2a$10$ — 시드 계정과 같은 형식) */
public final class PasswordUtil {

    private static final int COST = 10;

    /** 없는 이메일로 로그인할 때도 같은 시간이 걸리도록 비교용으로 쓰는 해시 */
    private static final String DUMMY_HASH = BCrypt.hashpw("rain-eye-dummy", BCrypt.gensalt(COST));

    private PasswordUtil() {}

    public static String hash(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(COST));
    }

    /** 해시가 null이거나 형식이 깨져 있으면 false (예외를 던지지 않음) */
    public static boolean check(String plain, String hash) {
        if (plain == null) return false;
        String h = (hash == null) ? DUMMY_HASH : hash.trim();
        try {
            boolean ok = BCrypt.checkpw(plain, h);
            return ok && hash != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** BCrypt는 앞 72바이트만 쓰므로 그보다 긴 비밀번호는 받지 않습니다(한글은 1자 3바이트). */
    public static boolean fitsBcrypt(String plain) {
        return plain != null && plain.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
