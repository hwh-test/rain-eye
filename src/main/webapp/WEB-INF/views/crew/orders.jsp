<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="kr.raineye.auth.LoginUser" %>
<%
    LoginUser me = (LoginUser) session.getAttribute(LoginUser.SESSION_KEY);
%>
<!DOCTYPE html>
<html lang="ko">
<head>
<%@ include file="/WEB-INF/views/common/head.jspf" %>
<title>작업 목록 · 레인아이</title>
</head>
<body>
<%@ include file="/WEB-INF/views/common/topbar.jspf" %>
<main class="container py-4" style="max-width: 560px;">
  <div class="alert alert-secondary">
    <strong>현장 기동반 화면 (C1 작업 목록 자리)</strong><br>
    <span class="small">2단계 확인용 임시 화면입니다. 기동반 화면은 선택 기능이라 필요할 때 추가합니다.</span>
  </div>
</main>
</body>
</html>
