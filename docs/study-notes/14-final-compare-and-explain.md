# 14. Final Compare and Explain

## 목표

전체 내용을 남에게 설명할 수 있을 정도로 정리한다.

## 최종적으로 설명할 수 있어야 하는 것

- [x] `/hello-servlet` 요청 흐름
- [x] 톰캣과 스프링의 경계
- [x] 서블릿을 먼저 공부하고 스프링으로 가는 이유
- [x] 내장 톰캣과 외장 톰캣 차이

## 최종 정리

### 1. `/hello-servlet` 요청 흐름 — 전체 경로

`http://localhost:8080/tomcat-study/hello-servlet`에 GET 요청을 보냈을 때:

```
브라우저
    │
    ▼
[Coyote] Connector :8080
    소켓 수신 → HTTP 파싱 → HttpServletRequest/Response 생성
    스레드 풀에서 스레드 하나 꺼냄
    │
    ▼
[Catalina] Engine → Host → Context
    URL에서 "/tomcat-study" → Context(/tomcat-study) 탐색
    Context에서 "/hello-servlet" → HelloServlet 결정
    │
    ▼
[Catalina] Filter Chain (톰캣이 실행)
    LoggingFilter.doFilter() → "BEFORE" 로그
    chain.doFilter() 호출
    │
    ▼
[Catalina] HelloServlet.doGet()
    응답 작성
    │
    ▼
[Catalina] Filter Chain (복귀)
    LoggingFilter → "AFTER" 로그
    │
    ▼
[Coyote] Connector
    응답 직렬화 → 소켓으로 전송
    스레드 풀로 반환
    │
    ▼
브라우저
```

### 2. 톰캣과 스프링의 경계

경계는 `DispatcherServlet`이다.

```
톰캣 담당                          스프링 담당
──────────────────────             ──────────────────────
소켓 수신                          HandlerMapping
HTTP 파싱                          Controller 호출
스레드 할당                        @RequestMapping 처리
Context/Host 탐색                  ArgumentResolver
Filter Chain                       ReturnValueHandler
DispatcherServlet.service() 호출   응답 직렬화 (MessageConverter)
세션 관리 (JSESSIONID)
```

톰캣 입장에서 `DispatcherServlet`은 그냥 서블릿 하나다. `service()`를 호출하는 것까지만 톰캣이 담당하고, 그 이후는 전부 스프링이 처리한다.

### 3. 서블릿을 먼저 공부하는 이유

Spring MVC는 서블릿 위에서 동작한다. Spring Boot가 대부분을 자동화해주지만, 문제가 생겼을 때 원인을 찾으려면 그 아래 레이어를 알아야 한다.

서블릿을 먼저 공부하면:

- `DispatcherServlet`이 왜 존재하는지 이해된다 (모든 요청을 하나의 서블릿이 받아서 라우팅하는 Front Controller 패턴)
- `Filter`와 Spring `Interceptor`의 차이가 명확해진다 (Filter는 톰캣 레벨, Interceptor는 스프링 레벨)
- `@WebServlet`, `web.xml`, `WebApplicationInitializer`가 모두 같은 목적임을 이해한다 (서블릿 등록)
- 세션, 쿠키, 에러 페이지가 톰캣이 제공하는 기능임을 안다

스프링만 공부하면 "왜 이렇게 동작하는가"가 블랙박스로 남는다.

### 4. 내장 톰캣 vs 외장 톰캣

| | 내장 톰캣 (JAR) | 외장 톰캣 (WAR) |
|---|---|---|
| 실행 | `java -jar app.jar` | `webapps/`에 WAR 복사 |
| 진입점 | `SpringApplication.run()` → `main()` | `SpringBootServletInitializer.configure()` |
| 톰캣 버전 | `spring-boot-starter-tomcat`이 결정 | 설치된 톰캣 버전 |
| 포트 설정 | `application.properties` | `server.xml` |
| 로그 | 애플리케이션 로그에 통합 | 톰캣 `logs/` 디렉터리 |
| 제어 방향 | 스프링이 톰캣을 제어 | 톰캣이 스프링을 제어 |
| 여러 앱 | 하나의 JAR = 하나의 앱 | 하나의 톰캣에 여러 WAR 배포 가능 |

## 비교표 / 다이어그램

### 단계별 학습 흐름

```
01 WAR 구조 이해
    └── WAR = webapps에 배포, context path = WAR 파일명

02 외장 톰캣 배포
    └── webapps/에 복사 → 자동 압축 해제 → Context 생성

03 서블릿 기초
    └── HttpServlet → doGet/doPost → HttpServletRequest/Response

04 web.xml
    └── <servlet> + <servlet-mapping> = URL과 서블릿 연결

05 Filter
    └── 서블릿 앞뒤에서 실행 → 로깅, 인증, 인코딩

06 Listener
    └── 앱/세션/요청 생명주기 이벤트 감지

07 Session
    └── JSESSIONID 쿠키 → 서버에 HttpSession 저장

08 JSP
    └── JSP = 서블릿으로 컴파일됨 (Jasper) → forward/redirect

09 Error Page
    └── web.xml에 <error-page> → 404/500 커스텀 페이지

10 Tomcat Log
    └── catalina.log / localhost.log / access_log / stdout

11 Spring MVC + 외장 톰캣
    └── WebApplicationInitializer → DispatcherServlet 등록

12 Spring Boot WAR
    └── SpringBootServletInitializer.configure() = WAR 진입점
    └── providedRuntime → 내장 톰캣 WAR 제외

13 Tomcat Internals
    └── Coyote(스레드 풀) + Catalina(서블릿 컨테이너)
    └── Server → Service → Connector + Engine → Host → Context
```

### 요청 처리 레이어

```
HTTP 요청
    │
    ▼  [톰캣 - Coyote]
Connector (소켓, HTTP 파싱, 스레드 할당)
    │
    ▼  [톰캣 - Catalina]
Engine → Host → Context (URL 라우팅)
    │
    ▼  [톰캣]
Filter Chain
    │
    ▼  [경계]
DispatcherServlet.service()
    │
    ▼  [스프링]
HandlerMapping → Controller → Service → Repository
    │
    ▼  [스프링]
MessageConverter → Response
    │
    ▼  [톰캣 - Coyote]
소켓 전송
```

## 5분 설명 스크립트

**Q. 스프링 부트로 개발하면 톰캣이 내장되어 있는데, 그 톰캣이 뭘 하는 건가요?**

톰캣은 HTTP 요청을 받아서 서블릿을 실행하는 서블릿 컨테이너입니다. 브라우저가 요청을 보내면 Coyote(Connector)가 소켓에서 HTTP를 파싱하고 스레드 풀에서 스레드를 꺼냅니다. 그리고 URL을 분석해서 어느 서블릿을 호출할지 결정하고, 필터 체인을 거쳐 서블릿의 `service()`를 호출합니다.

**Q. 그럼 스프링은 어디서부터 개입하나요?**

`DispatcherServlet`부터입니다. 톰캣이 `DispatcherServlet.service()`를 호출하는 순간이 경계입니다. 그 이후 `HandlerMapping`으로 컨트롤러를 찾고, `@GetMapping`을 처리하고, 응답을 직렬화하는 건 전부 스프링의 역할입니다. 톰캣 입장에서 `DispatcherServlet`은 그냥 서블릿 하나입니다.

**Q. 왜 스프링 부트부터 공부하지 않고 서블릿부터 공부하나요?**

스프링 부트는 많은 것을 자동화합니다. 그런데 문제가 생기면 그 아래를 봐야 합니다. `Filter`와 `Interceptor`가 왜 다른지, 세션이 어디서 관리되는지, WAR 배포 시 왜 `SpringBootServletInitializer`가 필요한지 — 이것들은 서블릿과 톰캣을 알아야 이해됩니다. 스프링만 공부하면 이 부분이 블랙박스로 남습니다.

**Q. 내장 톰캣과 외장 톰캣 차이는요?**

내장 톰캣은 스프링이 톰캣을 제어합니다. `java -jar`로 실행하고 `application.properties`로 설정합니다. 외장 톰캣은 반대로 톰캣이 스프링을 제어합니다. WAR를 `webapps/`에 복사하면 톰캣이 압축을 풀고 `SpringBootServletInitializer.configure()`를 진입점으로 스프링 컨텍스트를 초기화합니다. 설정은 `server.xml`이 담당하고, 하나의 톰캣에 여러 WAR를 올릴 수 있습니다.