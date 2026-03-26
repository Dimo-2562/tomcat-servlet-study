# 03. Servlet Request Flow

## 목표

요청 하나가 톰캣과 서블릿을 어떻게 통과하는지 이해한다.

## 확인한 것

- [x] 브라우저 -> 톰캣 -> 서블릿 매핑 -> `doGet()` 흐름
- [x] `contentType` 설정 이유
- [x] `PrintWriter` 응답 방식의 한계
- [x] GET/POST 처리 차이

## 배운 내용 정리

### 1. 브라우저 → 톰캣 → 서블릿 → `doGet()` 흐름

`http://localhost:8080/tomcat-study/hello-servlet`으로 GET 요청이 들어왔을 때 흐름:

1. 톰캣이 `/tomcat-study` → `tomcat-study` Context(웹 애플리케이션) 선택
2. 남은 경로 `/hello-servlet`을 해당 Context의 서블릿 매핑에서 검색
3. `@WebServlet(value = "/hello-servlet")`이 등록된 `HelloServlet` 발견
4. 톰캣이 `HttpServletRequest`, `HttpServletResponse` 객체를 생성해서 `HelloServlet.doGet(request, response)` 호출
5. 서블릿이 응답을 작성하면 톰캣이 이를 HTTP 응답으로 변환해 브라우저에 전송

서블릿은 요청을 직접 받는 게 아니라 톰캣이 만들어준 `request`/`response` 객체를 통해 HTTP를 다룬다. 소켓 연결, HTTP 파싱, 응답 직렬화는 모두 톰캣이 처리한다.

### 2. `response.setContentType("text/html")`이 필요한 이유

브라우저는 응답 본문이 무엇인지 스스로 알지 못한다. HTTP 응답 헤더의 `Content-Type`을 보고 어떻게 렌더링할지 결정한다.

```java
response.setContentType("text/html");
```

이 코드는 HTTP 응답 헤더에 `Content-Type: text/html`을 추가한다. 설정하지 않으면 브라우저가 HTML을 텍스트로 그대로 출력하거나 다운로드하려 할 수 있다.

`getWriter()`를 호출하기 **전에** 설정해야 한다. 헤더는 본문보다 먼저 전송되기 때문이다.

### 3. `PrintWriter`로 응답하는 방식의 한계

```java
PrintWriter out = response.getWriter();
out.println("<html><body>");
out.println("<h1>" + message + "</h1>");
out.println("</body></html>");
```

`PrintWriter`로 HTML을 직접 문자열로 조립하는 방식은 다음과 같은 한계가 있다:

- **가독성** — HTML이 Java 코드 안에 문자열로 섞여 있어 유지보수가 어렵다
- **타입 안전성 없음** — HTML 태그를 잘못 닫거나 빠뜨려도 컴파일 오류가 나지 않는다
- **바이너리 응답 불가** — 이미지, PDF 등 바이너리 데이터는 `getWriter()` 대신 `getOutputStream()`을 써야 한다. 둘을 동시에 쓸 수 없다

이런 이유로 실제로는 JSP나 템플릿 엔진으로 뷰를 분리한다.

### 4. GET/POST 처리 차이

`HttpServlet`은 HTTP 메서드별로 처리 메서드가 분리되어 있다:

| HTTP 메서드 | 서블릿 메서드 |
|------------|-------------|
| GET | `doGet()` |
| POST | `doPost()` |
| PUT | `doPut()` |
| DELETE | `doDelete()` |

현재 `HelloServlet`에는 `doGet()`만 있다. `doPost()`를 오버라이드하지 않으면 부모 클래스인 `HttpServlet`의 기본 구현이 호출되는데, 이 기본 구현은 `405 Method Not Allowed`를 반환한다.

즉, `http://localhost:8080/tomcat-study/hello-servlet`으로 POST 요청을 보내면 405 에러가 발생한다.

## 요청 흐름 써보기

```
브라우저
  │  GET /tomcat-study/hello-servlet
  ▼
톰캣 (Connector — HTTP 파싱, 소켓 처리)
  │  /tomcat-study → Context 선택
  ▼
톰캣 (Mapper — URL 매핑 조회)
  │  /hello-servlet → HelloServlet
  ▼
HelloServlet.doGet(HttpServletRequest, HttpServletResponse)
  │  response.setContentType("text/html")
  │  PrintWriter.println(...)
  ▼
톰캣 (HTTP 응답 직렬화)
  │  200 OK + Content-Type: text/html + body
  ▼
브라우저 (HTML 렌더링)
```

## 답해야 하는 질문

1. 톰캣은 URL을 기준으로 어떤 서블릿을 선택하는가?
2. 서블릿은 왜 HTTP 요청/응답 객체를 받는가?

## 질문에 대한 답

### 1. 톰캣은 URL을 기준으로 어떤 서블릿을 선택하는가?

URL을 두 단계로 분리해서 선택한다. 먼저 context path(`/tomcat-study`)로 어느 웹 애플리케이션인지 결정하고, 나머지 경로(`/hello-servlet`)로 해당 애플리케이션 내 서블릿을 찾는다. 서블릿 매핑은 `@WebServlet` 어노테이션이나 `web.xml`에 등록된 정보를 기준으로 한다.

### 2. 서블릿은 왜 HTTP 요청/응답 객체를 받는가?

서블릿이 HTTP를 직접 다루지 않아도 되도록 하기 위해서다. 소켓 연결 수립, HTTP 메시지 파싱, 헤더/바디 분리, 응답 직렬화 같은 저수준 작업은 모두 톰캣이 처리한다. 서블릿은 이미 파싱된 `HttpServletRequest`와 응답을 쓸 수 있는 `HttpServletResponse`만 받으면 된다. 덕분에 서블릿 코드는 비즈니스 로직에만 집중할 수 있다.

## 내 말로 설명

브라우저가 요청을 보내면 톰캣이 URL을 분석해 적절한 서블릿을 찾고 `doGet()` 같은 메서드를 호출한다. 서블릿은 소켓이나 HTTP 파싱을 직접 다루지 않고, 톰캣이 만들어준 `request`/`response` 객체를 통해 요청을 읽고 응답을 작성한다. `doPost()`를 구현하지 않으면 POST 요청은 자동으로 405가 된다. `PrintWriter`로 HTML을 직접 조립하는 방식은 동작하지만, 뷰와 로직이 섞이는 문제가 있어 실제로는 JSP나 템플릿 엔진으로 분리한다.