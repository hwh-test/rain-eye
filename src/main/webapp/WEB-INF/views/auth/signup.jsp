<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, kr.raineye.util.Html, kr.raineye.dao.DistrictDao" %>
<%
    String ctx = request.getContextPath();
    @SuppressWarnings("unchecked")
    List<String> errors = (List<String>) request.getAttribute("errors");
    @SuppressWarnings("unchecked")
    List<DistrictDao.District> districts = (List<DistrictDao.District>) request.getAttribute("districts");
    String selected = (String) request.getAttribute("districtCode");
    if (selected == null || selected.isEmpty()) selected = "";
%>
<!DOCTYPE html>
<html lang="ko">
<head>
<%@ include file="/WEB-INF/views/common/head.jspf" %>
<title>회원가입 · 레인아이</title>
</head>
<body>
<main class="container py-4">
  <div class="re-auth-card">
    <div class="text-center mb-3">
      <a href="<%= ctx %>/login" class="text-decoration-none"><h1 class="re-brand h3 mb-0">레인아이</h1></a>
    </div>

    <div class="card shadow-sm border-0">
      <div class="card-body p-4">
        <h2 class="h5 mb-3">시민 회원가입</h2>

        <% if (errors != null && !errors.isEmpty()) { %>
          <div class="alert alert-danger py-2 small" role="alert">
            <ul class="mb-0 ps-3">
              <% for (String e : errors) { %><li><%= Html.esc(e) %></li><% } %>
            </ul>
          </div>
        <% } %>

        <form method="post" action="<%= ctx %>/signup" novalidate>
          <div class="mb-3">
            <label for="email" class="form-label">이메일</label>
            <input type="email" class="form-control" id="email" name="email" maxlength="100"
                   value="<%= Html.esc(request.getAttribute("email")) %>"
                   autocomplete="email" autocapitalize="none" spellcheck="false" required>
            <div class="form-text">처리 완료 알림을 받을 주소예요.</div>
          </div>
          <div class="mb-3">
            <label for="password" class="form-label">비밀번호</label>
            <input type="password" class="form-control" id="password" name="password"
                   minlength="8" maxlength="64" autocomplete="new-password" required>
            <div class="form-text">영문과 숫자를 섞어 8자 이상</div>
          </div>
          <div class="mb-3">
            <label for="password2" class="form-label">비밀번호 확인</label>
            <input type="password" class="form-control" id="password2" name="password2"
                   minlength="8" maxlength="64" autocomplete="new-password" required>
          </div>
          <div class="mb-3">
            <label for="nickname" class="form-label">닉네임</label>
            <input type="text" class="form-control" id="nickname" name="nickname" maxlength="20"
                   value="<%= Html.esc(request.getAttribute("nickname")) %>" required>
          </div>
          <div class="mb-4">
            <label for="districtCode" class="form-label">사는 동네 (성동구 행정동)</label>
            <select class="form-select" id="districtCode" name="districtCode" required>
              <option value="" <%= selected.isEmpty() ? "selected" : "" %>>선택해 주세요</option>
              <% if (districts != null) for (DistrictDao.District d : districts) { %>
                <option value="<%= Html.esc(d.code) %>" <%= d.code.equals(selected) ? "selected" : "" %>><%= Html.esc(d.name) %></option>
              <% } %>
            </select>
          </div>
          <button type="submit" class="btn btn-re w-100 py-2">가입하기</button>
        </form>

        <hr>
        <p class="text-center small mb-0">이미 계정이 있나요? <a href="<%= ctx %>/login">로그인</a></p>
      </div>
    </div>
  </div>
</main>
</body>
</html>
