# 09. Error Handling And Logs

## 목표

에러가 났을 때 톰캣과 애플리케이션 로그를 어떻게 읽어야 하는지 익힌다.

## 확인한 것

- [x] 404와 500 차이
- [x] 톰캣 로그 위치
- [x] 예외 발생 시 응답과 로그 비교
- [x] 에러 페이지 매핑

## 배운 내용 정리

### 1. 404 vs 500

| 상태코드 | 의미 | 발생 원인 |
|---|---|---|
| 404 | Not Found | URL에 매핑된 서블릿/리소스가 없음 |
| 500 | Internal Server Error | 서블릿 실행 중 처리되지 않은 예외 발생 |

404는 톰캣이 URL 매핑을 찾지 못했을 때 반환한다. 500은 서블릿 코드에서 예외가 던져졌을 때 톰캣이 잡아서 반환한다.

### 2. 커스텀 에러 페이지 — web.xml 매핑

`web.xml`에 `<error-page>`를 등록하면 특정 상태코드에 대한 커스텀 페이지를 지정할 수 있다.

```xml
<error-page>
    <error-code>404</error-code>
    <location>/error/404.jsp</location>
</error-page>

<error-page>
    <error-code>500</error-code>
    <location>/error/500.jsp</location>
</error-page>
```

에러 페이지 JSP에서는 톰캣이 `request`에 담아주는 에러 관련 속성을 사용할 수 있다:

| 속성 | 내용 |
|---|---|
| `jakarta.servlet.error.status_code` | HTTP 상태코드 |
| `jakarta.servlet.error.request_uri` | 에러가 발생한 URL |
| `jakarta.servlet.error.exception` | 발생한 예외 객체 |
| `jakarta.servlet.error.message` | 에러 메시지 |

### 3. 예외 발생 시 필터 체인 동작

500 에러를 유발하는 서블릿에 요청했을 때 필터 로그:

```
[LoggingFilter] BEFORE - GET /tomcat-study/error-test
(AFTER 없음)
```

`chain.doFilter()` 이후 코드는 실행되지 않는다. 실제 stack trace에서 그 이유를 확인할 수 있다:

```
java.lang.RuntimeException: Intentional error for testing
    at ErrorServlet.doGet()            ← 예외 발생
    at HttpServlet.service()
    at ApplicationFilterChain.doFilter()
    at LoggingFilter.doFilter()        ← chain.doFilter()에서 예외가 위로 전파
    at StandardWrapperValve.invoke()   ← 톰캣이 예외 잡음
    at ErrorReportValve.invoke()       ← 에러 페이지로 전달
```

`LoggingFilter.doFilter()` 안의 `chain.doFilter()`에서 예외가 위로 던져지면서 그 이후 라인(`AFTER`)에 도달하지 못한다. 톰캣의 `StandardWrapperValve`가 예외를 잡아서 `<error-page>` 매핑에 따라 커스텀 500 페이지로 전달한다.

예외를 필터에서 잡으려면 `chain.doFilter()`를 try-catch로 감싸야 한다:

```java
try {
    chain.doFilter(request, response);
} catch (Exception e) {
    // 예외 처리
}
```

### 4. `<error-page>` 적용 범위

`web.xml`의 `<error-page>`는 해당 웹 애플리케이션(Context) 안에서만 적용된다. `web.xml`이 각 Context에 속하기 때문이다. 다른 애플리케이션(`ROOT` 등)의 에러에는 적용되지 않는다.

### 5. 톰캣 로그 파일

외장 톰캣의 로그는 `CATALINA_BASE/logs/` 아래에 있다.

| 파일 | 내용 |
|---|---|
| `catalina.YYYY-MM-DD.log` | JULI가 기록하는 서버 레벨 로그. 톰캣 시작/종료, 배포, 커넥터 초기화 등 |
| `localhost.YYYY-MM-DD.log` | JULI가 기록하는 웹 애플리케이션 레벨 로그. 서블릿 예외 stack trace가 `SEVERE`로 여기 찍힘 |
| `localhost_access_log.YYYY-MM-DD.txt` | HTTP 요청별 접근 로그. URL, 상태코드, 응답 바이트 크기 기록 |
| `commons-daemon.YYYY-MM-DD.log` | Windows 서비스 데몬(procrun)의 시작/종료 이벤트. 서비스로 실행 시에만 생성됨 |
| `tomcat11-stdout.YYYY-MM-DD.log` | `System.out.println` 출력. 서비스로 실행 시 stdout이 여기로 리다이렉트됨 |
| `tomcat11-stderr.YYYY-MM-DD.log` | `System.err` 출력 + JULI 로그 중 일부. 서비스 실행 시 stderr이 여기로 리다이렉트됨. `catalina.log`와 내용이 겹침 |

`localhost_access_log` 예시:
```
0:0:0:0:0:0:0:1 - - [26/Mar/2026:11:26:48 +0900] "GET /tomcat-study/ HTTP/1.1" 200 169
0:0:0:0:0:0:0:1 - - [26/Mar/2026:11:26:58 +0900] "GET /tomcat-study/hello-servlet HTTP/1.1" 200 53
```

요청 URL, HTTP 상태코드, 응답 바이트 크기가 기록된다.

### 6. 애플리케이션 로그 vs 서버 로그

현재 프로젝트는 `System.out.println`으로 로그를 찍는다. 이 출력은 톰캣의 `stdout` 로그로 들어간다. 서버 레벨 로그(`catalina.log`)와 섞이지 않는다.

실제 운영에서는 `System.out.println` 대신 Logback, Log4j 같은 로깅 프레임워크를 사용해서 애플리케이션 로그를 별도 파일로 분리한다.

## 로그 메모

`catalina.log`에서 확인한 WAR 배포 로그:
```
INFO [Catalina-utility-2] HostConfig.deployWAR
웹 애플리케이션 아카이브 [...\webapps\tomcat-study.war]을(를) 배치합니다.
웹 애플리케이션 아카이브 [...\webapps\tomcat-study.war]의 배치가 [388] 밀리초에 완료되었습니다.
```

`localhost_access_log`에서 확인한 요청 로그:
```
"GET /tomcat-study/ HTTP/1.1" 200 169
"GET /tomcat-study/hello-servlet HTTP/1.1" 200 53
```

## 답해야 하는 질문

1. 지금 프로젝트에서 에러가 났을 때 가장 먼저 볼 로그는 어디인가?
2. 외장 톰캣에서는 애플리케이션 로그와 서버 로그가 어떻게 구분될 수 있는가?

## 질문에 대한 답

### 1. 지금 프로젝트에서 에러가 났을 때 가장 먼저 볼 로그는 어디인가?

서비스로 실행 중이라면 `localhost.YYYY-MM-DD.log`가 가장 먼저 볼 곳이다. 서블릿에서 발생한 예외의 stack trace가 여기 `SEVERE` 레벨로 기록된다. `tomcat11-stdout.log`에서는 `System.out.println`으로 찍은 필터 로그를 확인해서 어느 요청이 들어왔는지, `AFTER`가 없는지 파악할 수 있다. `localhost_access_log`에서는 HTTP 상태코드를 확인한다.

### 2. 외장 톰캣에서는 애플리케이션 로그와 서버 로그가 어떻게 구분될 수 있는가?

톰캣 자체 로그(`catalina.log`)는 서버 시작/종료, 배포, 커넥터 초기화 같은 서버 레벨 이벤트를 기록한다. 애플리케이션 로그는 `System.out`/`System.err`로 출력하면 `tomcat11-stdout.log`/`tomcat11-stderr.log`로 들어간다. Logback 같은 로깅 프레임워크를 쓰면 별도 파일(`logs/app.log`)로 분리할 수 있어 서버 로그와 애플리케이션 로그를 명확하게 구분할 수 있다.

## 내 말로 설명

404는 URL 매핑이 없을 때, 500은 서블릿에서 예외가 터졌을 때 발생한다. `web.xml`의 `<error-page>`로 각 상태코드에 커스텀 페이지를 매핑할 수 있다. 예외가 발생하면 필터의 `chain.doFilter()` 이후 코드는 실행되지 않는다. 외장 톰캣의 로그는 `logs/` 디렉터리에 파일별로 분리되어 있고, 접근 로그와 서버 로그, 애플리케이션 출력이 각각 다른 파일에 기록된다.