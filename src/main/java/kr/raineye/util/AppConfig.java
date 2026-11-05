package kr.raineye.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * config.properties(클래스패스)에서 설정값을 읽는다.
 * 실제 키와 비밀번호가 담긴 config.properties는 Git에 올리지 않는다(.gitignore).
 * 저장소에는 config.example.properties만 올린다.
 */
public final class AppConfig {

    private static final Properties PROPS = new Properties();
    private static String loadError;

    static {
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null) {
                loadError = "config.properties를 찾을 수 없습니다. config.example.properties를 복사해 만드세요.";
            } else {
                PROPS.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            loadError = "config.properties 읽기 실패: " + e.getMessage();
        }
    }

    private AppConfig() {}

    public static String get(String key) {
        String v = PROPS.getProperty(key);
        return v == null ? "" : v.trim();
    }

    /** 설정 파일을 못 읽었으면 그 이유, 정상이면 null */
    public static String getLoadError() {
        return loadError;
    }
}
