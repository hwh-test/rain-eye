<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="kr.raineye.auth.LoginUser" %>
<%
    LoginUser me = (LoginUser) session.getAttribute(LoginUser.SESSION_KEY);
%>
<!DOCTYPE html>
<html lang="ko">
<head>
<%@ include file="/WEB-INF/views/common/head.jspf" %>
<title>지도 · 레인아이</title>
</head>
<body>
<%@ include file="/WEB-INF/views/common/topbar.jspf" %>
<main class="container py-4" style="max-width: 560px;">
  <div class="alert alert-primary">
    <strong>시민 화면 (M2 메인 지도 자리)</strong><br>
    <span class="small">2단계 확인용 임시 화면입니다. 3단계에서 지도와 제보 기능이 들어옵니다.</span>
  </div>
  <ul class="list-group small">
    <li class="list-group-item d-flex justify-content-between"><span>회원 번호</span><span><%= me.getUserId() %></span></li>
    <li class="list-group-item d-flex justify-content-between"><span>이메일</span><span><%= kr.raineye.util.Html.esc(me.getEmail()) %></span></li>
    <li class="list-group-item d-flex justify-content-between"><span>행정동 코드</span><span><%= kr.raineye.util.Html.esc(me.getDistrictCode()) %></span></li>
  </ul>
  <p class="re-demo-note text-center mt-4 mb-0">시연용 가상 데이터</p>
</main>
</body>
</html>
