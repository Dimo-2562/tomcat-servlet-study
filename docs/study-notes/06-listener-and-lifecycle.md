# 06. Listener And Lifecycle

## 목표

웹 애플리케이션 시작/종료와 서블릿 생명주기 차이를 이해한다.

## 확인한 것

- [x] `ServletContextListener` 동작 시점
- [x] 앱 시작/종료 로그
- [x] `init()`과 listener 차이

## 배운 내용 정리

### 1. ServletContextListener란

`ServletContextListener`는 웹 애플리케이션(Context) 전체의 시작과 종료 시점에 호출되는 리스너다. `jakarta.servlet.ServletContextListener` 인터페이스를 구현한다.

```java
public class AppLifecycleListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // 애플리케이션 시작 시 한 번 호출
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // 애플리케이션 종료 시 한 번 호출
    }
}
```

`ServletContextEvent`를 통해 `ServletContext`에 접근할 수 있다. `ServletContext`는 웹 애플리케이션 전체에서 공유되는 컨텍스트 객체로, context path나 초기화 파라미터 같은 정보를 담고 있다.

### 2. 리스너 등록

`web.xml`에 `<listener>`로 등록한다.

```xml
<listener>
    <listener-class>com.tomcat.tomcat.AppLifecycleListener</listener-class>
</listener>
```

`@WebListener` 어노테이션으로도 등록할 수 있다.

### 3. 초기화 순서

실제 콘솔 출력으로 확인한 초기화 순서:

```
[AppLifecycleListener] Application started - context path: /tomcat-study
[LoggingFilter] init
(배포 완료)
```

서블릿 스펙이 보장하는 초기화 순서: **리스너 → 필터 → 서블릿**

"배포 완료" 메시지가 가장 나중에 찍히는 이유는 IntelliJ가 톰캣의 초기화 완료 응답을 받은 뒤 출력하기 때문이다. 즉, 리스너와 필터 init이 모두 끝나야 톰캣이 배포 완료를 알린다.

### 4. `ServletContextListener`와 서블릿 `init()`의 차이

| | `ServletContextListener` | 서블릿 `init()` |
|---|---|---|
| 호출 시점 | 애플리케이션 시작/종료 | 해당 서블릿 초기화/종료 |
| 호출 횟수 | 애플리케이션당 한 번 | 서블릿 인스턴스당 한 번 |
| 용도 | 전역 자원 초기화, DB 커넥션 풀 | 해당 서블릿이 필요한 자원 초기화 |
| 실행 순서 | 필터/서블릿보다 먼저 | 리스너/필터 이후 |

서블릿 `init()`은 해당 서블릿이 처음 요청을 받을 때(또는 `load-on-startup` 설정 시 앱 시작 시) 호출된다. 반면 리스너는 요청과 무관하게 앱이 시작되자마자 호출된다.

### 5. 다양한 리스너 종류

`ServletContextListener` 외에도 여러 리스너가 있다:

| 리스너 | 역할 |
|---|---|
| `ServletContextListener` | 애플리케이션 시작/종료 |
| `HttpSessionListener` | 세션 생성/소멸 → 07단계에서 다룸 |
| `ServletRequestListener` | 요청 시작/종료 → 10단계(Spring MVC)에서 다룸 |
| `ServletContextAttributeListener` | ServletContext 속성 변경 |

## 생명주기 흐름

```
톰캣 시작
  │
  ▼
Context(웹 애플리케이션) 초기화
  │
  ├─ ServletContextListener.contextInitialized()
  ├─ Filter.init()
  └─ Servlet.init() (load-on-startup 설정 시)
  │
  ▼
요청 처리 (반복)
  │  Filter.doFilter() → Servlet.service() → doGet()/doPost()
  │
  ▼
Context 종료 (톰캣 종료 또는 언디플로이)
  │
  ├─ Servlet.destroy()
  ├─ Filter.destroy()
  └─ ServletContextListener.contextDestroyed()
```

## 답해야 하는 질문

1. 서버 시작과 웹 애플리케이션 초기화는 어떻게 다른가?
2. 공용 자원 초기화 코드는 어디에 두는 것이 적절한가?

## 질문에 대한 답

### 1. 서버 시작과 웹 애플리케이션 초기화는 어떻게 다른가?

서버 시작은 톰캣 프로세스가 실행되고 커넥터(포트 8080)가 열리는 시점이다. 웹 애플리케이션 초기화는 그 이후에 `webapps/` 아래의 각 애플리케이션을 로딩하는 시점이다. 하나의 톰캣에 여러 웹 애플리케이션이 있을 수 있고, 각각 독립적으로 초기화된다. `ServletContextListener.contextInitialized()`는 서버 시작이 아닌 웹 애플리케이션 초기화 시점에 호출된다.

### 2. 공용 자원 초기화 코드는 어디에 두는 것이 적절한가?

`ServletContextListener.contextInitialized()`가 적절하다. 이 시점은 요청이 들어오기 전, 필터와 서블릿이 초기화되기 전이다. DB 커넥션 풀, 캐시, 설정 파일 로딩처럼 애플리케이션 전체에서 공유하는 자원은 여기서 초기화하고 `ServletContext`에 속성으로 저장해두면 모든 서블릿에서 접근할 수 있다. 종료 시에는 `contextDestroyed()`에서 자원을 해제한다.

## 내 말로 설명

리스너는 웹 애플리케이션 전체의 생명주기를 감시한다. 서블릿 `init()`이 해당 서블릿의 초기화인 것과 달리, `ServletContextListener`는 애플리케이션 자체의 시작과 종료에 반응한다. 초기화 순서는 리스너 → 필터 → 서블릿 순이며, 이 순서는 서블릿 스펙이 보장한다. DB 커넥션 풀처럼 앱 전체에서 공유해야 하는 자원은 리스너에서 초기화하는 것이 자연스럽다.