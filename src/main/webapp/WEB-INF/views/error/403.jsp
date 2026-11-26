<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% String ctx = request.getContextPath(); %>
<!DOCTYPE html>
<html lang="ko">
<head>
<%@ include file="/WEB-INF/views/common/head.jspf" %>
<title>접근 권한 없음 · 레인아이</title>
</head>
<body>
<main class="container py-5 text-center" style="max-width: 480px;">
  <h1 class="display-6 re-brand">403</h1>
  <p class="mb-4">이 화면을 볼 권한이 없습니다.</p>
  <a class="btn btn-re" href="<%= ctx %>/home">내 화면으로 가기</a>
</main>
</body>
</html>
