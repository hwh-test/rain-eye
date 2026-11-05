<%@ page contentType="text/html; charset=UTF-8" import="kr.raineye.util.AppConfig" %>
<%
  // 지도 키는 브라우저에 노출되는 키입니다(리퍼러 제한으로 보호). 기상청 키는 절대 이렇게 내보내지 않습니다.
  String mapsKey = AppConfig.get("gmaps.apiKey");
  String mapId = AppConfig.get("gmaps.mapId").isEmpty() ? "DEMO_MAP_ID" : AppConfig.get("gmaps.mapId");
%>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>레인아이 · 지도 테스트</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  <style>
    html, body { height: 100%; margin: 0; }
    #map { height: calc(100% - 56px); }
  </style>
</head>
<body>
<div class="d-flex align-items-center gap-2 px-3 bg-white border-bottom" style="height:56px">
  <a href="index.jsp" class="btn btn-sm btn-outline-secondary">←</a>
  <button id="btnLocate" class="btn btn-sm btn-primary">내 위치</button>
  <span id="msg" class="small text-secondary text-truncate"></span>
</div>
<div id="map"></div>

<% if (mapsKey.isEmpty()) { %>
<script>document.getElementById('msg').textContent = 'config.properties에 gmaps.apiKey를 넣으세요.';</script>
<% } else { %>
<script>
  // Google 공식 동적 로더(부트스트랩) — importLibrary()로 필요한 라이브러리만 불러옵니다.
  (g=>{var h,a,k,p="The Google Maps JavaScript API",c="google",l="importLibrary",q="__ib__",m=document,b=window;b=b[c]||(b[c]={});var d=b.maps||(b.maps={}),r=new Set,e=new URLSearchParams,u=()=>h||(h=new Promise(async(f,n)=>{await (a=m.createElement("script"));e.set("libraries",[...r]+"");for(k in g)e.set(k.replace(/[A-Z]/g,t=>"_"+t[0].toLowerCase()),g[k]);e.set("callback",c+".maps."+q);a.src=`https://maps.${c}apis.com/maps/api/js?`+e;d[q]=f;a.onerror=()=>h=n(Error(p+" could not load."));a.nonce=m.querySelector("script[nonce]")?.nonce||"";m.head.append(a)}));d[l]?console.warn(p+" only loads once. Ignoring:",g):d[l]=(f,...n)=>r.add(f)&&u().then(()=>d[l](f,...n))})({
    key: "<%= mapsKey %>",
    v: "weekly",
    language: "ko",
    region: "KR"
  });

  const msg = t => document.getElementById('msg').textContent = t;

  async function init() {
    const { Map } = await google.maps.importLibrary("maps");
    const { AdvancedMarkerElement, PinElement } = await google.maps.importLibrary("marker");

    const seoulCityHall = { lat: 37.5665, lng: 126.9780 };
    const map = new Map(document.getElementById("map"), {
      center: seoulCityHall,
      zoom: 15,
      mapId: "<%= mapId %>",   // Advanced Marker는 Map ID가 없으면 뜨지 않습니다.
      gestureHandling: "greedy",
      streetViewControl: false
    });

    // 상태별 색 확인용 샘플 마커 3개 (양호/주의/위험)
    const samples = [
      { lat: 37.5671, lng: 126.9769, status: "GOOD",    color: "#2e9e5b" },
      { lat: 37.5659, lng: 126.9795, status: "CAUTION", color: "#e0a100" },
      { lat: 37.5652, lng: 126.9768, status: "DANGER",  color: "#d93025" }
    ];
    for (const s of samples) {
      const pin = new PinElement({ background: s.color, borderColor: "#ffffff", glyphColor: "#ffffff" });
      new AdvancedMarkerElement({ map, position: s, title: s.status, content: pin.element });
    }

    // 위험 구역 원(히트맵 대신)
    new google.maps.Circle({
      map, center: { lat: 37.5656, lng: 126.9775 }, radius: 100,
      strokeColor: "#d93025", strokeOpacity: 0.8, strokeWeight: 1,
      fillColor: "#d93025", fillOpacity: 0.15
    });

    let me = null;
    document.getElementById("btnLocate").onclick = () => {
      if (!window.isSecureContext) {
        msg("HTTPS가 아니라서 위치를 쓸 수 없습니다(localhost는 예외). 터널 주소로 접속하세요.");
        return;
      }
      msg("위치 확인 중…");
      navigator.geolocation.getCurrentPosition(pos => {
        const p = { lat: pos.coords.latitude, lng: pos.coords.longitude };
        if (me) me.map = null;
        me = new AdvancedMarkerElement({ map, position: p, title: "내 위치",
              content: new PinElement({ background: "#1a73e8", borderColor: "#fff", glyph: "" }).element });
        map.panTo(p);
        msg("정확도 약 " + Math.round(pos.coords.accuracy) + "m");
      }, err => msg("위치 실패: " + err.message), { enableHighAccuracy: true, timeout: 10000 });
    };

    msg(window.isSecureContext ? "지도 로드 완료 (보안 컨텍스트)" : "지도 로드 완료 (HTTP — 위치 기능 제한)");
  }

  init().catch(e => msg("지도 로드 실패: " + e.message + " — 키, 결제 계정, 리퍼러 제한을 확인하세요."));
</script>
<% } %>
</body>
</html>
