이 폴더에 아래 jar 파일을 직접 넣으세요. (Eclipse가 자동으로 클래스패스에 잡습니다)

1) mariadb-java-client-3.5.10.jar   — 0단계부터 필요 (DB 접속)
   https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.5.10/mariadb-java-client-3.5.10.jar

2) jbcrypt-0.4.jar                  — 2단계(로그인)에서 필요, 미리 넣어 두어도 됨
   https://repo1.maven.org/maven2/org/mindrot/jbcrypt/0.4/jbcrypt-0.4.jar

servlet-api는 넣지 마세요. Tomcat이 이미 가지고 있습니다(중복되면 오류).
