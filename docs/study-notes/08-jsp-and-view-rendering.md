# 08. JSP And View Rendering

## 목표

JSP와 서블릿의 관계, forward/redirect 차이를 이해한다.

## 확인한 것

- [x] JSP가 서블릿으로 변환된다는 점
- [x] request attribute 전달
- [x] forward / redirect 차이
- [x] 뷰 분리 방식의 장단점

## 배운 내용 정리

### 1. JSP는 서블릿이다

JSP 파일은 톰캣이 처음 요청을 받을 때 Java 코드로 변환하고 컴파일해서 서블릿으로 실행한다. 변환된 파일은 `work/` 디렉터리에 저장된다.

```
index.jsp
  → 톰캣(Jasper)이 Java 코드로 변환
  → 컴파일 → .class
  → 서블릿으로 실행
```

JSP 안의 `<%= ... %>`는 `out.print(...)`로, `<% ... %>`는 그대로 Java 코드로 변환된다. `${message}` 같은 EL(Expression Language)은 `request.getAttribute("message")`로 변환된다.

두 번째 요청부터는 이미 컴파일된 서블릿을 바로 실행하므로 빠르다.

### 2. 서블릿 → JSP 데이터 전달 (forward)

서블릿에서 데이터를 `request`에 담고 JSP로 forward한다. 이것이 서블릿+JSP 기반 MVC의 기본 패턴이다.

**ViewServlet.java:**
```java
request.setAttribute("message", "Hello from Servlet!");
request.setAttribute("items", new String[]{"Apple", "Banana", "Strawberry"});
request.getRequestDispatcher("/WEB-INF/view.jsp").forward(request, response);
```

**view.jsp:**
```jsp
<h2>Message: ${message}</h2>
<% for (String item : (String[]) request.getAttribute("items")) { %>
    <li><%= item %></li>
<% } %>
```

`request.setAttribute()`로 저장한 데이터는 같은 요청 안에서만 살아있다. forward는 같은 요청을 JSP에 넘기므로 데이터가 그대로 전달된다.

### 3. forward vs redirect

| | forward | redirect |
|---|---|---|
| 동작 | 서버 내부에서 다음 컴포넌트로 요청 전달 | 브라우저에게 새 URL로 요청하라고 응답 |
| URL 변화 | 브라우저 URL 유지 | 브라우저 URL 변경 |
| request 공유 | 같은 request 객체 공유 | 새 request 생성 |
| request attribute | JSP에서 접근 가능 | 전달 불가 (새 요청이므로) |
| HTTP 상태코드 | 200 | 302 |
| 사용 예 | 서블릿 → JSP 뷰 렌더링 | 로그인 후 메인 페이지 이동 |

`/view`로 접근하면 URL이 `/view`로 유지된다. forward이므로 브라우저는 JSP가 아닌 서블릿이 응답했다고 알고 있다.

### 4. WEB-INF 아래에 JSP를 두는 이유

`view.jsp`를 `WEB-INF/` 안에 배치했다. `WEB-INF/` 아래 파일은 브라우저에서 직접 URL로 접근할 수 없다. 톰캣이 차단하기 때문이다.

`http://localhost:8080/tomcat-study/WEB-INF/view.jsp`로 직접 접근하면 403이 반환된다.

JSP를 반드시 서블릿을 통해서만 접근하도록 강제할 수 있어, 데이터 없이 JSP가 직접 호출되는 상황을 막을 수 있다.

### 5. 뷰 분리 방식의 장단점

**서블릿에서 PrintWriter로 직접 HTML 작성 (03단계 방식):**
- HTML이 Java 코드 안에 문자열로 섞임
- 유지보수 어려움

**서블릿 + JSP forward 방식:**
- 서블릿: 데이터 준비 (로직)
- JSP: 화면 렌더링 (뷰)
- 역할이 분리되어 유지보수가 쉬움
- MVC 패턴의 기초

## 예제/실험 결과

`http://localhost:8080/tomcat-study/view` 접근 결과:
- `Message: Hello from Servlet!` 출력
- Apple, Banana, Strawberry 목록 출력
- URL은 `/view`로 유지됨 (forward)

`http://localhost:8080/tomcat-study/WEB-INF/view.jsp` 직접 접근:
- 403 반환 (WEB-INF 직접 접근 차단)

## 답해야 하는 질문

1. JSP는 누가 실행하는가?
2. MVC에서 JSP는 어느 레이어에 가까운가?

## 질문에 대한 답

### 1. JSP는 누가 실행하는가?

톰캣이다. 정확히는 톰캣 내부의 Jasper라는 JSP 엔진이 JSP 파일을 서블릿으로 변환하고 컴파일해서 실행한다. 개발자가 작성한 JSP는 결국 `HttpServlet`을 상속한 클래스로 변환되어 톰캣이 관리한다.

### 2. MVC에서 JSP는 어느 레이어에 가까운가?

View 레이어다. 서블릿이 Model(데이터)을 준비하고 Controller 역할을 하며, JSP는 그 데이터를 받아 HTML로 렌더링하는 View 역할을 한다. `request.setAttribute()`로 전달된 데이터를 EL(`${}`)이나 스크립틀릿(`<%= %>`)으로 출력하는 것이 JSP의 주된 역할이다.

## 내 말로 설명

JSP는 결국 서블릿이다. 톰캣이 처음 요청 시 Java 코드로 변환해서 컴파일하고 이후에는 컴파일된 서블릿을 바로 실행한다. 서블릿에서 `request.setAttribute()`로 데이터를 담고 `forward()`로 JSP에 넘기면 JSP가 그 데이터를 화면에 렌더링한다. 이것이 서블릿+JSP MVC의 기본 구조다. JSP를 `WEB-INF/` 아래에 두면 직접 URL 접근을 막을 수 있어 반드시 서블릿을 통해서만 접근하게 강제할 수 있다.