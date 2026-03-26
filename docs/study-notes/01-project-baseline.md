# 01. Project Baseline

## 목표

현재 프로젝트의 WAR 구조와 기본 동작을 이해한다.

## 확인한 것

- [x] `build.gradle`의 `war` 플러그인 의미
- [x] `compileOnly jakarta.servlet-api` 의미
- [x] `HelloServlet`의 역할
- [x] `web.xml`이 비어 있어도 동작하는 이유
- [x] `index.jsp`와 `/hello-servlet` 연결 관계

## 직접 확인한 파일

- [`build.gradle`](C:\Users\chose\IdeaProjects\Tomcat\tomcat\build.gradle)
- [`HelloServlet.java`](C:\Users\chose\IdeaProjects\Tomcat\tomcat\src\main\java\com\tomcat\tomcat\HelloServlet.java)
- [`web.xml`](C:\Users\chose\IdeaProjects\Tomcat\tomcat\src\main\webapp\WEB-INF\web.xml)
- [`index.jsp`](C:\Users\chose\IdeaProjects\Tomcat\tomcat\src\main\webapp\index.jsp)

## 배운 내용 정리

### 1. `build.gradle`에서 확인한 구조

이 프로젝트는 `java` + `war` 플러그인을 사용한다. JAR로 단독 실행하는 구조가 아니라, 톰캣 같은 서블릿 컨테이너에 배포하는 WAR 중심 프로젝트다.

`compileOnly('jakarta.servlet:jakarta.servlet-api:6.1.0')`는 서블릿 API가 컴파일 시점에만 필요하다는 뜻이다. 실행 시에는 외장 톰캣이 이 API를 제공하므로 WAR에 포함시킬 필요가 없다.

### 2. `HelloServlet`의 역할

`HelloServlet`은 `HttpServlet`을 상속한 가장 기본적인 서블릿이다. `@WebServlet(name = "helloServlet", value = "/hello-servlet")` 어노테이션 덕분에 `/hello-servlet` 요청이 이 클래스로 매핑된다.

각 메서드는 서블릿 생명주기의 특정 시점에 대응한다. `init()`은 초기화, `doGet()`은 GET 요청 처리, `destroy()`는 종료 시점이다. 중요한 점은 이 객체를 애플리케이션 코드가 직접 생성하는 게 아니라 톰캣이 생성하고 관리한다는 것이다.

### 3. `web.xml`이 비어 있어도 동작하는 이유

`web.xml`은 단순한 XML 파일이 아니라 웹 애플리케이션의 배포 설정을 담는 deployment descriptor다. 서블릿, 필터, 리스너, 초기화 파라미터, 보안 제약 등 컨테이너가 알아야 하는 설정을 기술하는 곳이다.

예전에는 이 파일에 서블릿과 URL 매핑을 직접 등록했지만, 현재 프로젝트에서는 `@WebServlet` 어노테이션을 대신 사용한다. 톰캣이 클래스패스를 스캔하면서 어노테이션을 발견하므로 `web.xml`이 비어 있어도 서블릿 등록과 URL 매핑이 정상적으로 이루어진다.

### 4. `index.jsp`와 `/hello-servlet`의 관계

`index.jsp`는 시작 페이지 역할을 한다. 페이지 안의 `hello-servlet` 링크를 클릭하면 브라우저가 해당 URL로 요청을 보내고, 톰캣이 URL 매핑을 확인한 뒤 `HelloServlet#doGet()`을 호출한다. JSP가 진입 화면, 서블릿이 실제 요청 처리를 담당하는 구조다.

### 5. WAR 내부 구조 — `WEB-INF/classes`와 `WEB-INF/lib`

실제 생성된 WAR를 열어보면 `WEB-INF/classes/com/tomcat/tomcat/HelloServlet.class`가 들어 있다.

처음에는 `WEB-INF/classes`와 `WEB-INF/lib`도 소스코드의 [`src/main/webapp/WEB-INF`](C:\Users\chose\IdeaProjects\Tomcat\tomcat\src\main\webapp\WEB-INF) 아래에 직접 있어야 한다고 착각했다. 하지만 이 디렉터리들은 특정 빌드 도구의 관례가 아니라 Java 웹 애플리케이션의 표준 배포 구조다. 서블릿 컨테이너가 클래스를 찾는 기본 경로가 `WEB-INF/classes`이고, 라이브러리를 찾는 경로가 `WEB-INF/lib/*.jar`이다. ANT든 Gradle이든 Maven이든 WAR를 만들면 이 구조를 따른다.

소스 프로젝트에서 직접 관리하는 것은 `web.xml`, JSP, 정적 리소스 같은 웹 리소스다. 반면 `WEB-INF/classes`에는 [`src/main/java`](C:\Users\chose\IdeaProjects\Tomcat\tomcat\src\main\java)의 컴파일 결과물이 들어가고, `WEB-INF/lib`에는 함께 배포할 JAR 의존성이 들어간다.

현재 WAR에서 `WEB-INF/lib`가 비어 있는 이유는 포함할 런타임 라이브러리가 없기 때문이다. `jakarta.servlet-api`는 `compileOnly`이므로 WAR에 포함되지 않고, 실행 시 톰캣이 제공한다.

정리하면, `webapp/WEB-INF`는 소스 기준 위치이고 WAR 내부의 `WEB-INF/classes`, `WEB-INF/lib`는 빌드 후 배포 기준 위치다. 이 둘을 구분해서 봐야 한다.

## 실행/관찰 결과

`.\gradlew.bat clean war` 실행으로 WAR 빌드에 성공했다. 생성된 파일은 `build/libs/tomcat-1.0-SNAPSHOT.war`이다.

WAR 내부 파일 목록 확인 명령:

```powershell
jar tf build\libs\tomcat-1.0-SNAPSHOT.war
```

확인 결과:

```text
META-INF/
META-INF/MANIFEST.MF
WEB-INF/
WEB-INF/classes/
WEB-INF/classes/com/tomcat/tomcat/HelloServlet.class
index.jsp
WEB-INF/web.xml
```

`WEB-INF/lib` 아래에 JAR가 없는 이유는 런타임 의존성이 없고, 서블릿 API도 `compileOnly`이기 때문이다.

## 참고 문헌

- Apache Tomcat Application Developer's Guide - Deployment: https://tomcat.apache.org/tomcat-11.0-doc/appdev/deployment.html

## 답해야 하는 질문

1. 이 프로젝트에서 톰캣이 가장 먼저 알게 되는 것은 무엇인가?
2. `HelloServlet`은 누가 생성하고 누가 호출하는가?

## 질문에 대한 답

### 1. 이 프로젝트에서 톰캣이 가장 먼저 알게 되는 것은 무엇인가?

톰캣은 먼저 이 WAR가 하나의 웹 애플리케이션이라는 사실을 인식한다. 이후 배포 과정에서 `web.xml`, 어노테이션, 클래스 구조를 바탕으로 서블릿과 URL 매핑 정보를 파악한다. 현재 프로젝트에서는 `@WebServlet("/hello-servlet")`을 통해 `/hello-servlet` 요청을 `HelloServlet`에 연결한다.

### 2. `HelloServlet`은 누가 생성하고 누가 호출하는가?

`HelloServlet`은 애플리케이션 코드가 직접 생성하지 않는다. 서블릿 컨테이너인 톰캣이 인스턴스를 생성하고 생명주기를 관리한다. 브라우저 요청이 `/hello-servlet`으로 들어오면 톰캣이 매핑된 서블릿을 찾아 `doGet()` 같은 메서드를 호출한다.

## 내 말로 설명

이 프로젝트는 외장 톰캣에 배포할 WAR를 만드는 구조다. 브라우저가 `/hello-servlet`으로 요청을 보내면 톰캣이 URL 매핑을 확인하고 `HelloServlet`을 호출한다. 이 서블릿은 우리가 직접 생성하는 객체가 아니라 톰캣이 생명주기를 관리하는 컴포넌트다. `web.xml`이 비어 있어도 `@WebServlet` 어노테이션 덕분에 서블릿이 등록된다. WAR 내부의 `WEB-INF/classes`, `WEB-INF/lib`는 특정 빌드 도구의 관례가 아니라 Java 웹 애플리케이션의 표준 배포 구조다. 현재 `WEB-INF/lib`가 비어 있는 이유는 포함할 런타임 라이브러리가 없기 때문이다.