package kr.raineye.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;

import java.io.IOException;

/**
 * 모든 요청·응답을 UTF-8로 처리한다(한글 깨짐 방지).
 * 2단계에서 만들 권한 필터보다 먼저 실행되어야 하므로 이름순으로 앞서게 둔다.
 */
@WebFilter("/*")
public class EncodingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        if (req.getCharacterEncoding() == null) {
            req.setCharacterEncoding("UTF-8");
        }
        resp.setCharacterEncoding("UTF-8");
        chain.doFilter(req, resp);
    }
}
