package kr.raineye.dev;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.raineye.util.AppConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 0단계 확인용: 기상청 단기예보(getVilageFcst)를 한 번 호출해 원본 응답을 그대로 보여 준다.
 * GET /dev/kma-test            → 서울 중구 근처 격자(nx=60, ny=127)
 * GET /dev/kma-test?nx=..&ny=..
 *
 * 4단계(스케줄러)에서 이 호출 코드를 서비스 클래스로 옮기고, 이 서블릿은 지운다.
 */
@WebServlet("/dev/kma-test")
public class KmaTestServlet extends HttpServlet {

    private static final String ENDPOINT =
            "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int[] BASE_HOURS = {2, 5, 8, 11, 14, 17, 20, 23};

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");

        String serviceKey = AppConfig.get("kma.serviceKey");
        if (serviceKey.isEmpty()) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"config.properties에 kma.serviceKey(Decoding 키)를 넣으세요.\"}");
            return;
        }

        String nx = param(req, "nx", "60");
        String ny = param(req, "ny", "127");
        LocalDateTime base = latestBaseTime(LocalDateTime.now(KST));

        String url = ENDPOINT
                + "?serviceKey=" + URLEncoder.encode(serviceKey, StandardCharsets.UTF_8)
                + "&pageNo=1&numOfRows=60&dataType=JSON"
                + "&base_date=" + base.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "&base_time=" + base.format(DateTimeFormatter.ofPattern("HHmm"))
                + "&nx=" + nx + "&ny=" + ny;

        try {
            HttpResponse<String> r = http.send(
                    HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            resp.setStatus(r.statusCode());
            // 정상이면 JSON, 키 오류 등은 XML로 올 수 있어 원문 그대로 전달
            resp.getWriter().write(r.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resp.setStatus(504);
            resp.getWriter().write("{\"error\":\"요청이 중단되었습니다.\"}");
        } catch (IOException e) {
            resp.setStatus(502);
            resp.getWriter().write("{\"error\":\"기상청 호출 실패: " + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    /**
     * 단기예보는 02·05·08·11·14·17·20·23시에 발표되고 약 10분 뒤부터 조회된다.
     * 지금 시각 기준으로 조회 가능한 가장 최근 발표 시각을 돌려준다.
     */
    static LocalDateTime latestBaseTime(LocalDateTime now) {
        LocalDateTime t = now.minusMinutes(10);
        for (int i = BASE_HOURS.length - 1; i >= 0; i--) {
            if (t.getHour() >= BASE_HOURS[i]) {
                return t.toLocalDate().atTime(BASE_HOURS[i], 0);
            }
        }
        return t.toLocalDate().minusDays(1).atTime(23, 0); // 00:00~02:09 → 전날 23시
    }

    private static String param(HttpServletRequest req, String name, String def) {
        String v = req.getParameter(name);
        return (v != null && v.matches("\\d{1,3}")) ? v : def;
    }
}
