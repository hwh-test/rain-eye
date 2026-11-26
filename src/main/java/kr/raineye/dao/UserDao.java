package kr.raineye.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;

// [기존 코드와 맞출 곳] 0단계의 DBUtil 패키지가 다르면 이 import에 빨간 줄이 생깁니다.
// Eclipse에서 Ctrl+Shift+O 를 누르면 실제 위치로 자동으로 바뀝니다.
import kr.raineye.util.DBUtil;

/** users 테이블 접근. SQL은 모두 PreparedStatement로만 실행합니다. */
public class UserDao {

    /** 로그인에 필요한 users 한 행 */
    public static class UserRow {
        public long userId;
        public String email;
        public String passwordHash;
        public String nickname;
        public String role;
        public String districtCode;
    }

    /** 이메일이 이미 있을 때 insertCitizen이 던지는 예외 */
    public static class DuplicateEmailException extends Exception {
        private static final long serialVersionUID = 1L;
        public DuplicateEmailException(Throwable cause) { super(cause); }
    }

    public UserRow findByEmail(String email) throws SQLException {
        String sql = "SELECT user_id, email, password_hash, nickname, role, district_code "
                   + "FROM users WHERE email = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                UserRow u = new UserRow();
                u.userId = rs.getLong("user_id");
                u.email = rs.getString("email");
                u.passwordHash = rs.getString("password_hash");
                u.nickname = rs.getString("nickname");
                u.role = rs.getString("role");
                u.districtCode = rs.getString("district_code");
                return u;
            }
        }
    }

    public boolean existsByEmail(String email) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * 시민 회원가입. 역할은 항상 CITIZEN (공무원·기동반은 시드로 미리 만든 계정만 사용).
     * @return 새 user_id
     */
    public long insertCitizen(String email, String passwordHash, String nickname, String districtCode)
            throws SQLException, DuplicateEmailException {
        String sql = "INSERT INTO users (email, password_hash, nickname, role, district_code, mileage_balance, created_at) "
                   + "VALUES (?, ?, ?, 'CITIZEN', ?, 0, NOW())";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, email);
            ps.setString(2, passwordHash);
            ps.setString(3, nickname);
            ps.setString(4, districtCode);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1L;
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DuplicateEmailException(e);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) throw new DuplicateEmailException(e); // Duplicate entry
            throw e;
        }
    }
}
