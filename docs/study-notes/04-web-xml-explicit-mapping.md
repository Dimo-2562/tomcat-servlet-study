# 04. web.xml Explicit Mapping

## 목표

annotation 없이 `web.xml`만으로 서블릿을 등록하는 구조를 이해한다.

## 확인한 것

- [x] `@WebServlet` 제거 여부
- [x] `servlet`, `servlet-mapping` 설정
- [x] annotation 방식과 `web.xml` 방식 차이

## 배운 내용 정리

### 1. `@WebServlet` 제거

`HelloServlet.java`에서 `@WebServlet(name = "helloServlet", value = "/hello-servlet")` 어노테이션을 제거했다. 어노테이션이 없으면 톰캣이 클래스 스캔만으로는 이 클래스를 서블릿으로 인식하지 못한다.

### 2. `web.xml`에 서블릿 등록

`web.xml` 안에 `<servlet>`과 `<servlet-mapping>`을 추가해서 동일한 매핑을 설정했다.

```xml
<servlet>
    <servlet-name>helloServlet</servlet-name>
    <servlet-class>com.tomcat.tomcat.HelloServlet</servlet-class>
</servlet>

<servlet-mapping>
    <servlet-name>helloServlet</servlet-name>
    <url-pattern>/hello-servlet</url-pattern>
</servlet-mapping>
```

- `<servlet>` — 서블릿 이름과 클래스를 톰캣에 등록
- `<servlet-mapping>` — 해당 서블릿을 어느 URL에 연결할지 지정
- `<servlet-name>`이 두 블록을 연결하는 키 역할을 한다

주의할 점은 `<servlet>`과 `<servlet-mapping>`이 반드시 `<web-app>` 태그 **안**에 있어야 한다는 것이다. 밖에 두면 유효하지 않은 XML이 된다.

### 3. `@WebServlet` 방식과 `web.xml` 방식 비교

| | `@WebServlet` | `web.xml` |
|---|---|---|
| 설정 위치 | Java 소스 코드 | XML 파일 |
| URL 변경 시 | 코드 수정 + 재컴파일 필요 | XML만 수정하면 됨 |
| 가독성 | 코드와 매핑이 한 곳에 | 매핑이 분리되어 있음 |
| 서블릿 3.0 이전 | 사용 불가 | 유일한 방법 |

코드를 수정하지 않고 URL 매핑을 바꾸려면 `web.xml` 방식이 더 직접적이다. 어노테이션 방식은 코드 수정과 재빌드가 필요하지만, `web.xml`은 파일만 바꾸면 된다.

### 4. Smart Tomcat 플러그인

이전까지는 WAR를 직접 `webapps/`에 복사해서 배포했다. IntelliJ의 Smart Tomcat 플러그인을 추가하면 빌드와 배포를 IDE에서 바로 실행할 수 있다.

## 설정 예시 / 실험 결과

**변경 전 (`@WebServlet` 방식):**

```java
@WebServlet(name = "helloServlet", value = "/hello-servlet")
public class HelloServlet extends HttpServlet { ... }
```

**변경 후 (`web.xml` 방식):**

`HelloServlet.java` — 어노테이션 없음:
```java
public class HelloServlet extends HttpServlet { ... }
```

`web.xml`:
```xml
<servlet>
    <servlet-name>helloServlet</servlet-name>
    <servlet-class>com.tomcat.tomcat.HelloServlet</servlet-class>
</servlet>
<servlet-mapping>
    <servlet-name>helloServlet</servlet-name>
    <url-pattern>/hello-servlet</url-pattern>
</servlet-mapping>
```

결과: `http://localhost:8080/tomcat-study/hello-servlet` 동일하게 동작 확인.

## 답해야 하는 질문

1. 코드를 수정하지 않고 URL 매핑을 제어하려면 어떤 방식이 더 직접적인가?
2. 외장 톰캣 환경을 공부할 때 왜 `web.xml`을 직접 만져보는 것이 좋은가?

## 질문에 대한 답

### 1. 코드를 수정하지 않고 URL 매핑을 제어하려면 어떤 방식이 더 직접적인가?

`web.xml` 방식이다. `@WebServlet`은 코드에 박혀 있어서 URL을 바꾸려면 Java 파일을 수정하고 재컴파일해야 한다. `web.xml`은 XML 파일만 수정하면 되므로, 코드 변경 없이 URL 매핑을 제어할 수 있다.

### 2. 외장 톰캣 환경을 공부할 때 왜 `web.xml`을 직접 만져보는 것이 좋은가?

`web.xml`은 서블릿 컨테이너가 웹 애플리케이션을 초기화하는 기준이 되는 파일이다. 어노테이션은 편리하지만 내부 동작을 추상화해버린다. `web.xml`로 직접 등록해보면 톰캣이 서블릿, URL 매핑, 필터, 리스너를 어떤 구조로 인식하는지 명확하게 이해할 수 있다. 이후에 필터나 리스너, Spring의 `DispatcherServlet` 등을 공부할 때도 `web.xml` 구조를 알고 있으면 훨씬 이해가 빠르다.

## 내 말로 설명

`@WebServlet` 어노테이션을 제거하고 `web.xml`에 `<servlet>`과 `<servlet-mapping>`을 추가하면 동일하게 동작한다. 두 방식은 결국 같은 정보를 톰캣에 전달하는 방법의 차이다. `web.xml`은 코드 밖에서 매핑을 제어할 수 있다는 점에서 더 유연하고, 톰캣이 웹 애플리케이션을 초기화하는 구조를 직접 눈으로 볼 수 있다는 점에서 학습 가치가 높다.