<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="kr.raineye.util.Html" %>
<%
    String ctx = request.getContextPath();
    String error = (String) request.getAttribute("error");
    String info = (String) request.getAttribute("info");
    Object email = request.getAttribute("email");
    Object next = request.getAttribute("next");
%>
<!DOCTYPE html>
<html lang="ko">
<head>
<%@ include file="/WEB-INF/views/common/head.jspf" %>
<title>로그인 · 레인아이</title>
</head>
<body>
<main class="container py-5">
  <div class="re-auth-card">
    <div class="text-center mb-4">
      <h1 class="re-brand h2 mb-1">레인아이</h1>
      <p class="text-secondary small mb-0">막힌 빗물받이를 함께 기록하고 침수를 미리 막아요</p>
    </div>

    <div class="card shadow-sm border-0">
      <div class="card-body p-4">
        <h2 class="h5 mb-3">로그인</h2>

        <% if (info != null) { %>
          <div class="alert alert-info py-2 small"><%= Html.esc(info) %></div>
        <% } %>
        <% if (error != null) { %>
          <div class="alert alert-danger py-2 small" role="alert"><%= Html.esc(error) %></div>
        <% } %>

        <form method="post" action="<%= ctx %>/login" novalidate>
          <input type="hidden" name="next" value="<%= Html.esc(next) %>">
          <div class="mb-3">
            <label for="email" class="form-label">이메일</label>
            <input type="email" class="form-control" id="email" name="email" value="<%= Html.esc(email) %>"
                   autocomplete="username" autocapitalize="none" spellcheck="false" required>
          </div>
          <div class="mb-3">
            <label for="password" class="form-label">비밀번호</label>
            <input type="password" class="form-control" id="password" name="password"
                   autocomplete="current-password" required>
          </div>
          <button type="submit" class="btn btn-re w-100 py-2">로그인</button>
        </form>

        <hr>
        <p class="text-center small mb-0">
          처음이신가요? <a href="<%= ctx %>/signup">시민 회원가입</a>
        </p>
      </div>
    </div>
    <p class="re-demo-note text-center mt-3 mb-0">공무원·기동반 계정은 담당 기관에서 발급합니다.</p>
  </div>
</main>
</body>
</html>
