# 13. Tomcat Internals

## 목표

톰캣 내부 컴포넌트 구조와 스레드 모델을 이해한다.

## 확인한 것

- [x] `Server`, `Service`, `Connector`, `Engine`, `Host`, `Context` 계층 구조
- [x] `server.xml`에서 컴포넌트 구조 확인
- [x] 스레드 풀과 요청 하나당 스레드 할당 방식
- [x] 스레드 모델이 서블릿 설계에 미치는 영향

## 배운 내용 정리

### 1. 컴포넌트 계층 구조

톰캣은 중첩된 컴포넌트 계층으로 구성된다.

```
Server
└── Service (Catalina)
    ├── Connector (HTTP/1.1 :8080)
    └── Engine (Catalina)
        └── Host (localhost)
            ├── Context (/tomcat-study)
            │   └── Wrapper → Servlet
            └── Context (/ROOT)
                └── Wrapper → Servlet
```

각 컴포넌트의 역할:

| 컴포넌트 | 역할 |
|---|---|
| `Server` | 톰캣 프로세스 전체. JVM 하나에 하나. `shutdown` 포트를 열어 `SHUTDOWN` 명령을 수신한다. |
| `Service` | `Connector`와 `Engine`을 묶는 단위. 하나의 `Engine`에 여러 `Connector`를 연결할 수 있다. |
| `Connector` | HTTP 요청을 수신하는 엔드포인트. 포트 번호, 프로토콜, 타임아웃 등을 설정한다. |
| `Engine` | `Service` 내에서 실제 요청을 처리하는 진입점. `Host`를 찾아 요청을 넘긴다. |
| `Host` | 가상 호스트. `localhost`가 기본이다. `appBase`(기본 `webapps/`)에서 WAR를 배포한다. |
| `Context` | 웹 애플리케이션 하나. `Context` 하나 = WAR 하나 = 애플리케이션 하나. |
| `Wrapper` | 서블릿 하나를 감싸는 컨테이너. 서블릿 인스턴스 생명주기를 관리한다. |

"Context 하나가 웹 애플리케이션 하나"라는 감각이 핵심이다. `/tomcat-study`로 들어오는 요청은 전부 그 `Context`가 처리하고, `/ROOT`로 들어오는 요청은 다른 `Context`가 처리한다.

### 2. server.xml 분석

실제 설치된 톰캣의 `server.xml`이다.

```xml
<Server port="-1" shutdown="SHUTDOWN">

  <Service name="Catalina">

    <Connector port="8080" protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8443" />

    <Engine name="Catalina" defaultHost="localhost">

      <Host name="localhost" appBase="webapps"
            unpackWARs="true" autoDeploy="true">

        <Valve className="org.apache.catalina.valves.AccessLogValve" ... />

      </Host>
    </Engine>
  </Service>
</Server>
```

주목할 설정:

- `Server port="-1"` — shutdown 포트 비활성화 (보안상 이유로 비활성화한 것)
- `Connector port="8080"` — HTTP 요청을 8080에서 수신
- `Host appBase="webapps"` — `webapps/` 디렉터리가 WAR 배포 위치
- `unpackWARs="true"` — WAR를 자동으로 압축 해제
- `autoDeploy="true"` — `webapps/`에 파일이 추가되면 자동 배포
- `AccessLogValve` — `localhost_access_log`에 HTTP 접근 로그 기록

`server.xml`에는 `Context`가 명시적으로 없다. `autoDeploy="true"`이면 톰캣이 `webapps/`를 감시해서 WAR를 발견하면 자동으로 `Context`를 생성한다.

### 3. Coyote와 Catalina

톰캣은 내부적으로 두 개의 서브시스템으로 나뉜다.

| 서브시스템 | 담당 |
|---|---|
| **Coyote** | 네트워크 I/O, HTTP 파싱, 소켓 관리, 스레드 풀 |
| **Catalina** | 서블릿 컨테이너 (`Engine` ~ `Wrapper`) |

"Catalina"라는 이름은 두 가지 의미로 쓰인다. 하나는 서블릿 컨테이너 서브시스템 전체를 가리키는 코드명이고, 다른 하나는 `server.xml`에서 `Service name="Catalina"`, `Engine name="Catalina"`처럼 인스턴스 식별자로 쓴 것이다. 후자는 그냥 기본값으로 붙인 이름이고 다른 이름으로 바꿔도 동작한다.

`Connector`가 Coyote 컴포넌트다. 스레드 풀을 들고 있고, 요청이 들어오면 풀에서 스레드 하나를 꺼내 Catalina 쪽으로 넘긴다. 그 스레드가 응답을 반환할 때까지 처리를 담당하고, 완료되면 풀로 반환된다. "요청당 스레드"는 엄밀히 말하면 "Coyote 스레드 풀에서 꺼낸 스레드 하나"다.

`server.xml`에서 프로토콜을 지정할 때 Coyote 클래스를 직접 명시하기도 한다:

```xml
<!-- HTTP/1.1은 내부적으로 org.apache.coyote.http11.Http11NioProtocol -->
<Connector port="8080" protocol="HTTP/1.1" ... />

<!-- HTTPS는 명시적으로 NIO 클래스 지정 -->
<Connector port="8443" protocol="org.apache.coyote.http11.Http11NioProtocol" ... />
```

Spring Boot 내장 톰캣도 동일한 구조다. `application.properties`의 `server.tomcat.threads.max`가 이 Coyote 스레드 풀 크기를 설정하는 것이다.

### 4. 요청이 서블릿까지 가는 경로

HTTP 요청 하나가 서블릿에 도달하기까지의 흐름:

```
브라우저
    │  HTTP 요청
    ▼
Coyote: Connector (포트 8080에서 수신)
    │  소켓에서 HTTP 파싱 → Request/Response 객체 생성
    │  스레드 풀에서 스레드 할당
    ▼
Catalina: Engine → Host → Context 탐색
    │  URL에서 context path 추출 → Host에서 Context 찾기
    │  context path 이후 경로로 서블릿 찾기
    ▼
Filter Chain 실행
    │
    ▼
Wrapper → Servlet.service()
    │
    ▼
Coyote: 응답 직렬화 → 소켓으로 전송 → 브라우저
```

### 4. 스레드 풀과 요청 하나당 스레드

`Connector`는 내부에 스레드 풀을 가진다. 요청이 들어오면 풀에서 스레드 하나를 꺼내 그 요청을 처음부터 끝까지 처리한다.

기본값:

| 설정 | 기본값 | 의미 |
|---|---|---|
| `maxThreads` | 200 | 동시에 처리할 수 있는 최대 요청 수 |
| `minSpareThreads` | 10 | 항상 대기 중인 최소 스레드 수 |
| `acceptCount` | 100 | 스레드가 모두 사용 중일 때 대기할 수 있는 요청 수 |

`server.xml`에서 직접 지정하거나, 별도 `Executor`를 정의해서 여러 `Connector`가 공유하게 할 수도 있다.

```xml
<!-- Executor를 별도 정의하면 여러 Connector가 공유 가능 -->
<Executor name="tomcatThreadPool" namePrefix="catalina-exec-"
    maxThreads="150" minSpareThreads="4"/>

<Connector executor="tomcatThreadPool" port="8080" ... />
```

현재 설치된 서버는 `Executor`를 따로 정의하지 않고 `Connector` 기본 스레드 풀을 사용한다.

### 5. 스레드 모델이 서블릿 설계에 미치는 영향

서블릿 인스턴스는 톰캣이 하나만 만든다. 여러 요청이 동시에 들어오면 같은 인스턴스의 `service()` 메서드를 여러 스레드가 동시에 호출한다.

```
요청 A (스레드 1) ─┐
요청 B (스레드 2) ─┼─→ HelloServlet 인스턴스 하나 → service()
요청 C (스레드 3) ─┘
```

**인스턴스 변수는 공유된다.** 여러 스레드가 같은 필드를 동시에 읽고 쓰면 race condition이 발생한다.

```java
// 위험: 인스턴스 변수
public class CountServlet extends HttpServlet {
    private int count = 0;  // 모든 스레드가 공유

    protected void doGet(HttpServletRequest req, HttpServletResponse res) {
        count++;  // 스레드 안전하지 않음
    }
}
```

```java
// 안전: 지역 변수
public class SafeServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse res) {
        int count = 0;  // 스레드마다 스택에 따로 생성
        count++;
    }
}
```

서블릿에서 상태를 유지해야 한다면 `synchronized`, `AtomicInteger`, 또는 DB/세션에 저장하는 방식을 써야 한다.

Spring MVC의 `@Controller`도 싱글톤 빈이므로 동일하다. 필드에 상태를 두면 위험하다.

## server.xml 분석

```
Server
├── port="-1"  shutdown 포트 비활성화
└── Service name="Catalina"
    ├── Connector port="8080"  HTTP 수신
    │   connectionTimeout="20000"  20초 연결 타임아웃
    └── Engine name="Catalina" defaultHost="localhost"
        └── Host name="localhost" appBase="webapps"
            unpackWARs="true"   WAR 자동 압축 해제
            autoDeploy="true"   파일 추가 시 자동 배포
            └── Valve: AccessLogValve → localhost_access_log
```

`Context`는 `server.xml`에 없다. `autoDeploy`가 `webapps/`를 감시하다가 WAR를 발견하면 동적으로 `Context`를 생성한다. `Context`를 명시적으로 제어하려면 `conf/Catalina/localhost/` 디렉터리에 Context XML 파일을 두거나 `server.xml` 안에 직접 정의한다.

## 답해야 하는 질문

### 1. 톰캣이 요청을 받아서 서블릿에 전달하기까지 어떤 컴포넌트를 거치는가?

`Connector`가 소켓에서 HTTP를 파싱하고 스레드를 할당한다. 이후 `Engine` → `Host` → `Context` 순으로 URL을 분석해서 어느 웹 애플리케이션의 어느 서블릿인지 결정한다. `Context`에서 필터 체인을 실행하고 최종적으로 `Wrapper`가 서블릿의 `service()`를 호출한다.

### 2. 서블릿이 스레드 안전하지 않으면 어떤 문제가 생기는가?

서블릿 인스턴스는 하나이고, 동시 요청은 여러 스레드가 같은 인스턴스를 공유한다. 인스턴스 변수에 상태를 저장하면 race condition이 생긴다. 예를 들어 카운터를 인스턴스 변수로 두면 두 스레드가 동시에 읽고 쓸 때 값이 덮어써진다. 지역 변수는 스레드마다 스택이 분리되어 있으므로 안전하다.

## 내 말로 설명

톰캣의 구조는 `Server → Service → Connector + Engine → Host → Context → Wrapper → Servlet`으로 중첩된다. 요청이 들어오면 `Connector`가 HTTP를 파싱하고 스레드 풀에서 스레드 하나를 꺼내서 그 요청을 처리한다. `Engine`은 URL의 context path로 `Host`에서 `Context`를 찾고, 나머지 경로로 서블릿을 찾아 `service()`를 호출한다. 서블릿 인스턴스는 하나뿐이므로 인스턴스 변수를 쓰면 동시 요청에서 race condition이 생긴다. `server.xml`에서 이 구조를 직접 확인할 수 있고, `Context`는 `autoDeploy`가 자동으로 생성한다.