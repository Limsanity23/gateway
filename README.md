# 시작하기


## 사전 준비
Java 11  
Gradle 5.x  
로컬 서버 (접속 ip, port 정보)
전광판 연동서버 (접속 ip, port 정보)
차단기 연동서버 (접속 ip, port 정보)
모바일푸시 연동서버 (접속 ip, port 정보)
홈넷 연동서버 (접속 ip, port 정보)


## 어플리케이션 설정
application-[profile명].yml에서 다음 설정값 확인.
* 어플리케이션 포트 확인(운영 12010).  
* MariaDB 연결설정 확인.

## 빌드
$ gadle clean build --debug 


## 실행
$ java -jar -Dspring.profiles.active=[profile명] ./api-signage/build/libs/gateway-server-1.0-RELEASE.jar


## 로깅
> 로그 파일은 어플리케이션 실행 명령을 수행한 디렉토리 상위 logs 디렉토리 내에 저장.  
30일간 보관.  
상세 설정은 log4j2.xml 