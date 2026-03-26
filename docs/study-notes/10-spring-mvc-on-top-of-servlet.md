# 10. Spring MVC On Top Of Servlet

## 목표

스프링 MVC가 서블릿 위에서 어떤 추상화를 제공하는지 연결한다.

## 확인한 것

- [x] `DispatcherServlet` 역할
- [x] `HelloServlet`과의 공통점/차이
- [x] URL 매핑 주체 변화
- [x] `Controller`, `Service`, `Repository` 관계

## 배운 내용 정리

### 1. 스프링 MVC도 결국 서블릿이다

스프링 MVC의 핵심은 `DispatcherServlet`이다. 이것은 `HttpServlet`을 상속한 일반 서블릿이다. 톰캣 입장에서는 `HelloServlet`과 다를 게 없다.

```
톰캣 → DispatcherServlet.service() → doGet()/doPost()
```

`web.xml` 또는 `WebApplicationInitializer`로 `DispatcherServlet`을 `/` 또는 `/*`에 매핑하면 모든 요청이 이 서블릿 하나로 들어온다.

### 2. HelloServlet과 DispatcherServlet 비교

| | `HelloServlet` | `DispatcherServlet` |
|---|---|---|
| 상속 | `HttpServlet` | `HttpServlet` (간접) |
| URL 매핑 | `@WebServlet` 또는 `web.xml` | `web.xml` 또는 Java Config |
| 요청 처리 | `doGet()` 직접 구현 | 내부에서 `HandlerMapping`으로 컨트롤러 위임 |
| 뷰 렌더링 | `PrintWriter` 또는 `forward()` | `ViewResolver`가 처리 |
| 확장성 | 서블릿 하나당 URL 하나 | 서블릿 하나가 모든 URL 처리 |

`HelloServlet`은 URL 하나에 서블릿 하나를 직접 연결했다. 스프링 MVC는 `DispatcherServlet` 하나가 모든 요청을 받아서 내부적으로 적절한 컨트롤러로 위임하는 **Front Controller 패턴**이다.

### 3. URL 매핑 주체 변화

**서블릿 방식:**
```
톰캣(Mapper) → URL로 서블릿 결정 → 서블릿 호출
```

**스프링 MVC 방식:**
```
톰캣(Mapper) → DispatcherServlet
  → HandlerMapping → URL로 컨트롤러 결정
  → HandlerAdapter → 컨트롤러 호출
  → ViewResolver → 뷰 렌더링
```

서블릿 방식에서는 톰캣이 URL 매핑을 담당했다. 스프링 MVC에서는 톰캣은 `DispatcherServlet`까지만 라우팅하고, 그 이후 URL → 컨트롤러 매핑은 스프링의 `HandlerMapping`이 담당한다. `@RequestMapping`, `@GetMapping` 같은 어노테이션이 `HandlerMapping`에 등록되는 정보다.

### 4. 스프링 컨텍스트 초기화 — ServletContextListener와의 연결

06단계에서 `ServletContextListener.contextInitialized()`가 애플리케이션 시작 시점에 호출된다고 배웠다. 스프링은 이 시점을 활용해서 ApplicationContext(빈 컨테이너)를 초기화한다.

전통적인 Spring MVC (외장 톰캣 + `web.xml`) 방식에서는 컨텍스트가 둘로 분리됐다:

- **Root ApplicationContext** — `ContextLoaderListener`가 생성. `Service`, `Repository` 등 웹과 무관한 공통 빈
- **Servlet WebApplicationContext** — `DispatcherServlet`이 생성. `Controller`, `ViewResolver` 등 웹 레이어 빈

이렇게 분리한 이유는 `DispatcherServlet`을 여러 개 띄울 때 공통 빈을 공유하기 위해서였다. 하지만 실제로 `DispatcherServlet`을 여러 개 쓰는 경우는 드물었고, 요즘은 서비스를 분리하려면 MSA로 아예 별도 애플리케이션으로 나누는 방식을 택한다.

**Spring Boot에서는 이 구분이 사라졌다.** `ContextLoaderListener`를 사용하지 않고 `DispatcherServlet` 하나에 단일 `ApplicationContext`를 붙인다. 모든 빈이 하나의 컨텍스트에 있고, Root/Servlet 구분이 없다.

```
// 전통적인 Spring MVC
contextInitialized()
  → ContextLoaderListener (Root ApplicationContext)
  → DispatcherServlet.init() (Servlet WebApplicationContext)

// Spring Boot
SpringApplication.run()
  → 단일 ApplicationContext
  → DispatcherServlet에 연결
```

### 5. 필터와 인터셉터 — 06단계에서 예고했던 연결

05단계에서 배운 필터는 서블릿 스펙에 속하고 톰캣 레벨에서 동작한다. 스프링의 인터셉터(`HandlerInterceptor`)는 `DispatcherServlet` 내부에서 동작한다.

```
요청
  → Filter (톰캣 레벨, 서블릿 스펙)
  → DispatcherServlet
    → HandlerInterceptor.preHandle() (스프링 레벨)
    → Controller
    → HandlerInterceptor.postHandle()
  → Filter (응답)
```

필터는 스프링 빈에 접근하기 어렵지만, 인터셉터는 스프링 컨텍스트 안에서 동작하므로 빈을 자유롭게 사용할 수 있다.

### 6. ServletRequestListener와의 연결 — 요청 단위 스코프

06단계에서 `ServletRequestListener`는 10단계에서 다룬다고 했다.

스프링의 `RequestContextListener`는 `ServletRequestListener`를 구현한다. 요청이 시작될 때 `RequestContextHolder`에 현재 request를 바인딩해두면, 이후 스프링 빈들이 `@RequestScope`나 `RequestContextHolder.getRequestAttributes()`로 요청 정보에 접근할 수 있다.

```
requestInitialized()
  → RequestContextHolder에 request 바인딩
  → @RequestScope 빈, RequestContextHolder 사용 가능
requestDestroyed()
  → RequestContextHolder 정리 → @RequestScope 빈 소멸
```

단, Spring Boot + `DispatcherServlet` 환경에서는 `RequestContextListener`를 별도로 등록하지 않아도 된다. `DispatcherServlet` 자체가 요청을 받을 때 `RequestContextHolder`에 바인딩하고 끝나면 해제하기 때문이다. `RequestContextListener`는 `DispatcherServlet` 밖(예: 필터)에서도 `RequestContextHolder`를 써야 할 때 필요하다.

## 연결해서 이해한 내용

| 서블릿/톰캣 개념 | 스프링 MVC 대응 |
|---|---|
| `HttpServlet` | `DispatcherServlet` (서블릿 구현체) |
| `web.xml` 서블릿 매핑 | `@RequestMapping`, `@GetMapping` |
| `request.setAttribute()` + `forward()` | `Model` + `ViewResolver` |
| `Filter` | `Filter` (동일) + `HandlerInterceptor` |
| `ServletContextListener` | `ContextLoaderListener` |
| `ServletRequestListener` | `RequestContextListener` |
| `HttpSession` | `HttpSession` (동일) + `@SessionScope` |

## 답해야 하는 질문

1. 스프링 MVC를 도입해도 톰캣이 필요한 이유는 무엇인가?
2. 스프링 MVC는 서블릿 스펙 위에서 어떤 추상화를 제공하는가?

## 질문에 대한 답

### 1. 스프링 MVC를 도입해도 톰캣이 필요한 이유는 무엇인가?

스프링 MVC는 서블릿 스펙 위에서 동작하는 프레임워크이지 서블릿 컨테이너가 아니다. HTTP 요청을 받는 소켓 연결, 스레드 풀 관리, HTTP 파싱, WAR 배포, 세션 관리 등은 모두 톰캣이 담당한다. 스프링 MVC는 톰캣이 처리한 요청을 `DispatcherServlet`을 통해 받아서 컨트롤러로 위임할 뿐이다. 스프링 부트의 내장 톰캣도 톰캣이 없어진 게 아니라 톰캣을 내부에 포함시킨 것이다.

### 2. 스프링 MVC는 서블릿 스펙 위에서 어떤 추상화를 제공하는가?

서블릿 방식에서는 URL 하나마다 서블릿 클래스를 만들고 `web.xml`에 등록해야 했다. 스프링 MVC는 `DispatcherServlet` 하나로 모든 요청을 받아 `HandlerMapping`으로 컨트롤러를 찾고, `ViewResolver`로 뷰를 처리하는 Front Controller 패턴을 제공한다. 덕분에 개발자는 서블릿 생명주기나 URL 매핑 관리 없이 `@Controller`와 `@RequestMapping`만으로 요청 처리 로직을 작성할 수 있다. 필터/리스너 같은 서블릿 스펙은 그대로 활용하면서, 그 위에 DI, AOP, 트랜잭션 관리 등을 추가로 제공한다.

## 내 말로 설명

스프링 MVC는 톰캣과 서블릿 스펙 위에서 동작한다. `DispatcherServlet`이라는 서블릿 하나가 모든 요청을 받아서 내부적으로 컨트롤러로 위임한다. 지금까지 배운 필터, 리스너, 세션은 스프링 MVC에서도 그대로 동작한다. 스프링이 추가하는 것은 URL → 컨트롤러 매핑 자동화, 빈 관리, 뷰 렌더링 추상화, 인터셉터 등이다. 톰캣이 HTTP를 처리하고, 스프링이 그 위에서 애플리케이션 로직을 구조화한다.