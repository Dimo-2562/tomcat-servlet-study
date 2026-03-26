# 05. Filter

## 목표

필터가 요청 전후에 어떤 역할을 하는지 이해한다.

## 확인한 것

- [x] 필터 등록 방식
- [x] 요청 전/후 로그
- [x] 필터 체인 개념
- [x] URL 패턴별 적용

## 배운 내용 정리

### 1. 필터란

필터는 서블릿에 요청이 도달하기 전, 그리고 서블릿이 응답을 반환한 후에 끼어드는 컴포넌트다. `jakarta.servlet.Filter` 인터페이스를 구현한다.

```java
public class LoggingFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) { ... }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // 요청 전 처리
        chain.doFilter(request, response);
        // 요청 후 처리
    }

    @Override
    public void destroy() { ... }
}
```

`chain.doFilter()`가 핵심이다. 이 호출이 다음 필터 또는 서블릿으로 요청을 넘긴다. 호출 전이 "요청 전", 호출 후가 "요청 후"다. `chain.doFilter()`를 호출하지 않으면 요청이 서블릿까지 도달하지 않는다.

### 2. 필터 등록 방식

`web.xml`에 `<filter>`와 `<filter-mapping>`으로 등록한다. 서블릿 등록과 구조가 동일하다.

```xml
<filter>
    <filter-name>loggingFilter</filter-name>
    <filter-class>com.tomcat.tomcat.LoggingFilter</filter-class>
</filter>

<filter-mapping>
    <filter-name>loggingFilter</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

`<url-pattern>/*</url-pattern>`은 모든 요청에 이 필터를 적용한다는 뜻이다. `/hello-servlet`처럼 특정 경로만 지정할 수도 있다.

`@WebFilter` 어노테이션으로도 등록할 수 있다. `@WebServlet`과 동일한 방식이다.

### 3. 필터 체인

필터는 여러 개를 등록할 수 있고, `web.xml`에 등록된 순서대로 실행된다.

```
요청 → Filter1 → Filter2 → Filter3 → Servlet
응답 ← Filter1 ← Filter2 ← Filter3 ← Servlet
```

각 필터는 `chain.doFilter()`로 다음 필터에게 요청을 넘기고, 서블릿이 응답을 작성한 뒤 역순으로 돌아온다. 이 구조를 필터 체인(FilterChain)이라고 한다.

### 4. 필터 생명주기

서블릿과 마찬가지로 톰캣이 생명주기를 관리한다.

- `init()` — 애플리케이션 시작 시 한 번 호출
- `doFilter()` — 매 요청마다 호출
- `destroy()` — 애플리케이션 종료 시 한 번 호출

### 5. 필터와 서블릿의 책임 차이

| | 필터 | 서블릿 |
|---|---|---|
| 역할 | 요청/응답 전처리, 공통 관심사 | 비즈니스 로직 처리 |
| 예시 | 인증, 로깅, 인코딩, CORS | 데이터 조회, 응답 생성 |
| 적용 범위 | URL 패턴으로 여러 서블릿에 적용 | 특정 URL에만 적용 |

## 로그 예시

`/tomcat-study/hello-servlet` 요청 시 콘솔 출력:

```
[LoggingFilter] init
[LoggingFilter] BEFORE - GET /tomcat-study/hello-servlet
[LoggingFilter] AFTER - /tomcat-study/hello-servlet
```

`chain.doFilter()` 전후로 로그가 찍히는 것을 확인했다.

## 답해야 하는 질문

1. 필터와 서블릿의 책임은 어떻게 다른가?
2. 필터에서 `chain.doFilter()`를 호출하지 않으면 어떻게 되는가?
3. `web.xml`에 필터를 여러 개 등록하면 실행 순서는 어떻게 결정되는가?

## 질문에 대한 답

### 1. 필터와 서블릿의 책임은 어떻게 다른가?

필터는 여러 서블릿에 공통으로 적용해야 하는 횡단 관심사(cross-cutting concerns)를 처리한다. 인증, 로깅, 인코딩 설정, CORS 헤더 추가 같은 것들이다. 서블릿은 특정 요청에 대한 비즈니스 로직을 처리한다. 필터가 먼저 실행되고 통과한 요청만 서블릿에 도달한다.

### 2. 필터에서 `chain.doFilter()`를 호출하지 않으면 어떻게 되는가?

요청이 다음 필터나 서블릿으로 전달되지 않는다. 필터에서 응답을 직접 작성하거나 아무것도 하지 않으면 클라이언트는 서블릿의 응답을 받지 못한다. 인증 필터에서 인증 실패 시 `chain.doFilter()`를 호출하지 않고 401을 반환하는 방식이 이 패턴을 활용한 예다.

### 3. `web.xml`에 필터를 여러 개 등록하면 실행 순서는 어떻게 결정되는가?

`web.xml`에 `<filter-mapping>`이 선언된 순서대로 실행된다. 위에 선언된 필터가 먼저 실행된다. `@WebFilter` 어노테이션 방식은 실행 순서가 보장되지 않으므로, 순서가 중요한 경우 `web.xml`을 사용하는 것이 안전하다.

## 참고 문헌

- Jakarta Servlet 6.1 API - Filter: https://jakarta.ee/specifications/servlet/6.1/apidocs/jakarta.servlet/jakarta/servlet/filter

## 내 말로 설명

필터는 서블릿 앞에 세워두는 문지기다. `chain.doFilter()`를 기준으로 전후 처리를 나눌 수 있고, 이 메서드를 호출하지 않으면 요청을 막을 수 있다. URL 패턴으로 적용 범위를 지정하므로 여러 서블릿에 공통 로직을 한 곳에서 관리할 수 있다. 인증, 로깅, 인코딩처럼 비즈니스 로직과 무관한 공통 처리를 필터에 두는 것이 일반적이다.