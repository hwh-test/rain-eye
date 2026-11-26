package kr.raineye.util;

/**
 * JSP 출력용 HTML 이스케이프.
 * EL(${...})은 이스케이프를 하지 않으므로, 사용자가 입력한 값(닉네임·이메일 등)은
 * JSP에서 반드시 <%= Html.esc(값) %>로 출력합니다.
 */
public final class Html {

    private Html() {}

    public static String esc(Object value) {
        if (value == null) return "";
        String s = value.toString();
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
