<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="kr.raineye.auth.LoginUser" %>
<%
    LoginUser me = (LoginUser) session.getAttribute(LoginUser.SESSION_KEY);
    String ctx = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="ko">
<head>
<%@ include file="/WEB-INF/views/common/head.jspf" %>
<title>대시보드 · 레인아이</title>
</head>
<body>
<%@ include file="/WEB-INF/views/common/topbar.jspf" %>
<main class="container-fluid py-4 px-4">
  <div class="row g-4">
    <div class="col-lg-8">
      <div class="card border-0 shadow-sm">
        <div class="card-body">
          <h1 class="h5">공무원 대시보드 (P1 자리)</h1>
          <p class="text-secondary small mb-0">2단계 확인용 임시 화면입니다. 6단계에서 핫스팟 지도·우선순위·작업 지시가 들어옵니다.</p>
        </div>
      </div>
    </div>
    <div class="col-lg-4">
      <div class="card border-0 shadow-sm">
        <div class="card-body small">
          <h2 class="h6">개발 점검 (공무원만)</h2>
          <ul class="mb-0">
            <li><a href="<%= ctx %>/health">/health</a></li>
            <li><a href="<%= ctx %>/dev/kma-test">/dev/kma-test</a></li>
            <li><a href="<%= ctx %>/maptest.jsp">maptest.jsp</a></li>
          </ul>
        </div>
      </div>
    </div>
  </div>
  <p class="re-demo-note mt-4 mb-0">시연용 가상 데이터</p>
</main>
</body>
</html>
