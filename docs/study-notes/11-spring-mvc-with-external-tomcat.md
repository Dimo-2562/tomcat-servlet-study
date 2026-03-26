# 11. Spring MVC With External Tomcat

## 목표

Spring Boot 없이 순수 Spring MVC를 외장 톰캣에 배포하는 구조를 이해한다.

## 확인한 것

- [x] `build.gradle`에 Spring MVC 의존성 추가
- [x] `WebApplicationInitializer`로 `DispatcherServlet` 등록
- [x] `@Controller`로 요청 처리
- [x] `web.xml` 방식과 Java Config 방식 비교

## 배운 내용 정리

### 1. 의존성 추가

`spring-webmvc`를 `implementation`으로 추가한다. `jakarta.servlet-api`는 여전히 `compileOnly`다.

```gradle
implementation('org.springframework:spring-webmvc:6.2.5')
compileOnly('jakarta.servlet:jakarta.servlet-api:6.1.0')
```

`spring-webmvc`는 `spring-context`, `spring-beans`, `spring-core` 등을 포함하므로 별도로 추가할 필요 없다. `implementation`이므로 WAR 내부 `WEB-INF/lib/`에 포함된다.

### 2. WebApplicationInitializer — web.xml 없이 DispatcherServlet 등록

Servlet 3.0부터 `web.xml` 없이 Java 코드로 서블릿을 등록할 수 있다. 톰캣은 시작 시 클래스패스에서 `WebApplicationInitializer` 구현체를 자동으로 찾아서 `onStartup()`을 호출한다.

```java
public class AppInitializer implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext servletContext) {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.register(AppConfig.class);

        DispatcherServlet dispatcherServlet = new DispatcherServlet(context);

        ServletRegistration.Dynamic registration = servletContext.addServlet("dispatcher", dispatcherServlet);
        registration.setLoadOnStartup(1);
        registration.addMapping("/spring/*");
    }
}
```

- `AnnotationConfigWebApplicationContext` — Java Config 기반 스프링 컨텍스트
- `context.register(AppConfig.class)` — `@Configuration` 클래스 등록
- `addMapping("/spring/*")` — 이 경로로 들어오는 요청을 `DispatcherServlet`이 처리

04단계에서 `web.xml`에 `<servlet>` + `<servlet-mapping>`으로 서블릿을 등록한 것과 동일한 동작이다. 방식만 다르다.

### 3. AppConfig — Spring MVC 설정

```java
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "com.tomcat.tomcat.spring")
public class AppConfig {
}
```

- `@EnableWebMvc` — `HandlerMapping`, `HandlerAdapter`, `ViewResolver` 등 Spring MVC 기반 인프라 빈 자동 등록
- `@ComponentScan` — 지정 패키지에서 `@Controller`, `@Service` 등을 스캔해서 빈으로 등록

### 4. Controller

```java
@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello from Spring MVC!";
    }
}
```

`DispatcherServlet`이 `/spring/*`에 매핑되어 있으므로 실제 접근 URL은 `/tomcat-study/spring/hello`다.

- `/tomcat-study` — context path (WAR 파일명)
- `/spring` — `DispatcherServlet` 매핑 경로
- `/hello` — `@GetMapping` 경로

### 5. web.xml 방식 vs Java Config 방식

| | `web.xml` | `WebApplicationInitializer` |
|---|---|---|
| 등록 방식 | XML | Java 코드 |
| 타입 안전성 | 없음 | 있음 |
| IDE 지원 | 제한적 | 풍부 |
| Servlet 스펙 | 2.5 이하 포함 모두 | 3.0 이상 |
| Spring Boot | 사용 안 함 | `SpringBootServletInitializer`로 확장 |

두 방식은 동일한 결과를 낸다. `WebApplicationInitializer`는 `web.xml`을 완전히 대체할 수 있다. Spring Boot는 `SpringBootServletInitializer`가 이 인터페이스를 구현해서 동일한 방식으로 외장 톰캣에 배포한다.

### 6. 기존 서블릿과 공존

`DispatcherServlet`을 `/spring/*`에 매핑했기 때문에 기존 서블릿들(`/hello-servlet`, `/session` 등)은 그대로 동작한다. 톰캣의 URL 매핑 규칙에 따라:

- `/spring/*` → `DispatcherServlet` → Spring MVC 처리
- `/hello-servlet` → `HelloServlet` → 기존 서블릿 처리
- `/session` → `SessionServlet` → 기존 서블릿 처리

같은 WAR 안에서 순수 서블릿과 Spring MVC가 공존한다.

## 실험 결과

`http://localhost:8080/tomcat-study/spring/hello` 접근 결과:
```
Hello from Spring MVC!
```

기존 서블릿(`/hello-servlet`, `/session` 등)도 정상 동작 확인.

## 답해야 하는 질문

1. `WebApplicationInitializer`는 `web.xml`을 어떻게 대체하는가?
2. Spring MVC를 붙였을 때 톰캣과의 경계는 어디인가?

## 질문에 대한 답

### 1. `WebApplicationInitializer`는 `web.xml`을 어떻게 대체하는가?

Servlet 3.0부터 `ServletContainerInitializer` 스펙이 추가됐다. 톰캣은 시작 시 클래스패스에서 이 인터페이스 구현체를 찾아 실행한다. Spring은 `SpringServletContainerInitializer`가 `WebApplicationInitializer` 구현체들을 찾아서 `onStartup()`을 호출하도록 구현했다. 덕분에 `web.xml` 없이 Java 코드로 서블릿, 필터, 리스너를 등록할 수 있다.

### 2. Spring MVC를 붙였을 때 톰캣과의 경계는 어디인가?

경계는 `DispatcherServlet`이다. 톰캣은 HTTP 요청을 받아 URL 매핑에 따라 `DispatcherServlet.service()`를 호출하는 것까지만 담당한다. 그 이후 `HandlerMapping`으로 컨트롤러를 찾고, `@GetMapping`을 처리하고, 응답을 직렬화하는 것은 모두 Spring MVC가 담당한다. 톰캣 입장에서 `DispatcherServlet`은 그냥 서블릿 하나다.

## 내 말로 설명

Spring Boot 없이 Spring MVC를 외장 톰캣에 붙이려면 `WebApplicationInitializer`로 `DispatcherServlet`을 직접 등록해야 한다. 이것이 `web.xml`에 `<servlet>` + `<servlet-mapping>`을 쓰는 것과 동일한 역할이다. `DispatcherServlet`이 특정 경로를 담당하면 그 경로의 요청은 Spring MVC가 처리하고, 나머지는 기존 서블릿이 처리한다. 톰캣은 어느 쪽이든 그냥 서블릿을 호출할 뿐이다.
