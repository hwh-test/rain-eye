<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>레인아이 · 개발 환경 점검</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<div class="container py-4" style="max-width: 640px">
  <h1 class="h4 mb-1">레인아이(Rain-I) 개발 환경 점검</h1>
  <p class="text-secondary small mb-4">0단계 완료 기준: 이 페이지가 뜨고, 아래 DB 항목이 OK이면 됩니다.</p>

  <table class="table table-sm bg-white border">
    <tbody id="rows">
      <tr><td colspan="2" class="text-secondary">확인 중…</td></tr>
    </tbody>
  </table>

  <div class="list-group">
    <a class="list-group-item list-group-item-action" href="maptest.jsp">지도·Advanced Marker·현재 위치 테스트 →</a>
    <a class="list-group-item list-group-item-action" href="dev/kma-test">기상청 단기예보 호출 테스트 (원본 JSON) →</a>
    <a class="list-group-item list-group-item-action" href="health">/health 원본 JSON →</a>
  </div>
</div>

<script>
  const labels = { java: 'Java', server: 'Tomcat', config: '설정 파일', db: 'DB 접속',
                   dbVersion: 'MariaDB 버전', dbTime: 'DB 현재 시각', secure: 'HTTPS 접속' };
  fetch('health')
    .then(r => r.json())
    .then(d => {
      const tbody = document.getElementById('rows');
      tbody.innerHTML = '';
      for (const [k, label] of Object.entries(labels)) {
        const v = String(d[k]);
        const ok = v.startsWith('OK') || k === 'java' || k === 'server' || (k === 'secure' && v === 'true');
        const bad = v.startsWith('FAIL') || v.includes('찾을 수 없');
        const tr = document.createElement('tr');
        const th = document.createElement('th'); th.className = 'w-25'; th.textContent = label;
        const td = document.createElement('td'); td.textContent = v;
        if (bad) td.className = 'text-danger';
        else if (ok && (k === 'db' || k === 'config')) td.className = 'text-success';
        tr.append(th, td); tbody.append(tr);
      }
    })
    .catch(e => { document.getElementById('rows').innerHTML =
      '<tr><td class="text-danger">/health 호출 실패: 서블릿이 배포되지 않았을 수 있습니다.</td></tr>'; });
</script>
</body>
</html>
