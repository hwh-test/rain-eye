package kr.raineye.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import kr.raineye.util.DBUtil; // [기존 코드와 맞출 곳] UserDao와 같음

/** districts 참조표(성동구 행정동 17개) 읽기 */
public class DistrictDao {

    public static class District {
        public final String code;
        public final String name;
        public District(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    public List<District> findAll() throws SQLException {
        String sql = "SELECT district_code, name FROM districts ORDER BY name";
        List<District> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new District(rs.getString("district_code"), rs.getString("name")));
            }
        }
        return list;
    }

    public boolean exists(String code) throws SQLException {
        String sql = "SELECT 1 FROM districts WHERE district_code = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
