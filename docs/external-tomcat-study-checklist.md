# Spring + External Tomcat Study Checklist

이 문서는 현재 프로젝트를 기준으로 `Jakarta EE WAR -> external Tomcat -> Spring MVC -> Spring Boot WAR` 흐름을 차근차근 밟기 위한 학습 체크리스트다.

지금 프로젝트 상태:

- `war` 플러그인 사용 중
- `HelloServlet` 존재
- `index.jsp` 존재
- `web.xml`은 비어 있음
- WAR 빌드 확인 완료: `build/libs/tomcat-1.0-SNAPSHOT.war`

핵심 목표:

- 톰캣이 웹 요청을 어떻게 받아 처리하는지 이해한다.
- 서블릿 기반 웹 애플리케이션의 구조를 직접 손으로 익힌다.
- 그 위에 스프링 MVC가 어떻게 올라가는지 이해한다.
- 마지막에 Spring Boot의 내장 톰캣과 외장 톰캣 방식을 비교 정리한다.

---

## 0. Ground Rules

- [ ] 학습 기록용 노트를 하나 유지한다.
- [ ] 매 단계마다 "요청이 어디서 시작해서 어디까지 갔는지"를 직접 글로 적는다.
- [ ] 에러가 나면 바로 고치기 전에 로그를 먼저 읽는다.
- [ ] 자동 설정에 기대기보다 현재 단계에서 필요한 설정을 직접 확인한다.

기록할 때 매번 답할 질문:

1. 요청을 처음 받는 주체는 누구인가?
2. URL 매핑은 어디서 결정되는가?
3. 지금 단계에서 스프링이 하는 일과 톰캣이 하는 일은 어떻게 나뉘는가?
4. 현재 앱의 진입점은 무엇인가?

---

## 1. Project Baseline

목표: 지금 프로젝트가 무엇으로 구성되어 있는지 정확히 파악한다.

- [ ] [`build.gradle`](C:\Users\chose\IdeaProjects\Tomcat\tomcat\build.gradle)를 읽고 `war`, `compileOnly`, `jakarta.servlet-api` 의미를 설명할 수 있다.
- [ ] [`HelloServlet.java`](C:\Users\chose\IdeaProjects\Tomcat\tomcat\src\main\java\com\tomcat\tomcat\HelloServlet.java)를 읽고 `HttpServlet`, `init`, `doGet`, `destroy` 역할을 설명할 수 있다.
- [ ] [`web.xml`](C:\Users\chose\IdeaProjects\Tomcat\tomcat\src\main\webapp\WEB-INF\web.xml)이 비어 있어도 앱이 동작하는 이유를 설명할 수 있다.
- [ ] [`index.jsp`](C:\Users\chose\IdeaProjects\Tomcat\tomcat\src\main\webapp\index.jsp)와 `/hello-servlet`의 관계를 설명할 수 있다.
- [ ] `annotation 기반 서블릿 등록`과 `web.xml 기반 등록` 차이를 말할 수 있다.

실행:

```powershell
.\gradlew.bat clean war
```

확인:

- [ ] `build/libs/tomcat-1.0-SNAPSHOT.war` 파일이 생성된다.
- [ ] WAR 파일이 "배포 단위"라는 의미를 설명할 수 있다.

정리 질문:

1. 이 프로젝트에서 톰캣이 가장 먼저 알게 되는 것은 무엇인가?
2. `HelloServlet`은 누가 생성하고 누가 호출하는가?

---

## 2. External Tomcat Deployment

목표: WAR 파일이 외장 톰캣에 배포되는 흐름을 직접 본다.

사전 체크:

- [ ] 사용 중인 톰캣 버전이 현재 프로젝트의 Jakarta Servlet API와 호환되는지 확인한다.
- [ ] 톰캣 설치 디렉터리에서 `bin`, `conf`, `webapps`, `logs` 위치를 파악한다.

실행 순서:

1. `.\gradlew.bat clean war`로 WAR 생성
2. 생성된 WAR를 톰캣 `webapps`에 복사
3. 톰캣 시작
4. 브라우저에서 앱 접속

체크:

- [ ] 톰캣 기동 로그에서 배포 로그를 찾을 수 있다.
- [ ] `webapps` 아래에 WAR 이름 기준으로 앱이 배포되는 구조를 확인한다.
- [ ] `/hello-servlet` 요청이 정상 동작한다.
- [ ] `/` 또는 기본 페이지가 어떤 파일로 연결되는지 확인한다.

관찰 포인트:

- [ ] WAR를 복사하면 톰캣이 언제 압축을 푸는지 본다.
- [ ] 배포 후 디렉터리 구조에서 `WEB-INF/classes`와 `WEB-INF/lib`를 확인한다.
- [ ] "왜 `WEB-INF` 아래 리소스는 직접 URL로 접근되지 않는가?"를 설명할 수 있다.

정리 질문:

1. 톰캣은 WAR 파일을 어떤 단위의 웹 애플리케이션으로 취급하는가?
2. 현재 애플리케이션의 context path는 무엇인가?

---

## 3. Servlet Request Flow

목표: 요청 하나를 끝까지 따라간다.

- [ ] `/hello-servlet` 요청이 들어왔을 때 실행 순서를 적는다.
- [ ] 브라우저 -> 톰캣 커넥터 -> 서블릿 컨테이너 -> 서블릿 매핑 -> `HelloServlet#doGet` 흐름을 설명할 수 있다.
- [ ] `response.setContentType("text/html")`가 왜 필요한지 설명할 수 있다.
- [ ] `PrintWriter`로 직접 응답을 쓰는 방식의 한계를 설명할 수 있다.

실험:

- [ ] `HelloServlet` 응답 메시지를 한 번 바꿔본다.
- [ ] 쿼리 파라미터를 읽어서 응답 내용에 반영해본다.
- [ ] `doPost()`를 추가하고 GET/POST 차이를 확인한다.

추천 확장 실험:

- [ ] URL 매핑을 `/hello`로 바꿔본다.
- [ ] 여러 URL 패턴을 하나의 서블릿에 매핑해본다.

정리 질문:

1. 톰캣은 URL을 기준으로 어떤 서블릿을 선택하는가?
2. 서블릿은 왜 HTTP 요청/응답 객체를 받는가?

---

## 4. web.xml Explicit Mapping

목표: annotation 없이도 서블릿이 등록되는 구조를 이해한다.

할 일:

- [ ] `HelloServlet`의 `@WebServlet`을 제거한다.
- [ ] [`web.xml`](C:\Users\chose\IdeaProjects\Tomcat\tomcat\src\main\webapp\WEB-INF\web.xml)에 직접 `servlet` / `servlet-mapping`을 추가한다.
- [ ] 같은 요청이 계속 동작하는지 확인한다.

확인:

- [ ] annotation 등록과 `web.xml` 등록의 차이를 설명할 수 있다.
- [ ] 예전 Java EE 스타일과 현재 annotation 기반 방식의 장단점을 말할 수 있다.

정리 질문:

1. 배포 기술자가 코드를 수정하지 않고 URL 매핑을 제어하려면 어떤 방식이 더 직접적인가?
2. 왜 외장 톰캣 환경을 공부할 때 `web.xml`을 한 번은 직접 만져보는 것이 좋은가?

---

## 5. Filter

목표: 요청이 서블릿에 도달하기 전후에 끼어드는 지점을 이해한다.

할 일:

- [ ] 요청 로그를 남기는 `Filter`를 하나 만든다.
- [ ] 요청 URI, 메서드, 처리 시작/종료 시점을 출력한다.
- [ ] annotation 또는 `web.xml`로 필터를 등록한다.

확인:

- [ ] 필터가 서블릿보다 먼저 실행되는 것을 로그로 확인한다.
- [ ] 필터 체인이라는 개념을 설명할 수 있다.
- [ ] 공통 관심사에 필터가 왜 적합한지 설명할 수 있다.

추천 실험:

- [ ] 특정 URL 패턴에만 필터를 적용해본다.
- [ ] 응답 헤더를 하나 추가해본다.

정리 질문:

1. 필터와 서블릿의 책임은 어떻게 다른가?
2. 스프링에 가면 필터와 인터셉터는 어떻게 구분되는가?

---

## 6. Listener And Lifecycle

목표: 웹 애플리케이션의 시작/종료 시점을 이해한다.

할 일:

- [ ] `ServletContextListener`를 추가한다.
- [ ] 앱 시작 시 로그, 종료 시 로그를 남긴다.
- [ ] 서블릿의 `init()`과 listener 시점 차이를 비교한다.

확인:

- [ ] 톰캣 시작과 앱 초기화가 완전히 같은 개념이 아님을 설명할 수 있다.
- [ ] 애플리케이션 단위 초기화와 서블릿 단위 초기화의 차이를 설명할 수 있다.

정리 질문:

1. "서버가 시작된다"와 "웹 애플리케이션이 초기화된다"는 어떻게 다른가?
2. 앱 전체 공용 자원을 준비하는 코드는 어디에 두는 것이 적절한가?

---

## 7. Session

목표: 세션이 톰캣 레벨에서 어떻게 관리되는지 체감한다.

할 일:

- [ ] `HttpSession`을 사용해 방문 횟수를 저장한다.
- [ ] 새 세션 생성 여부를 화면이나 로그에 표시한다.
- [ ] 브라우저 쿠키를 지우고 동작 변화를 확인한다.

확인:

- [ ] `JSESSIONID`가 무엇인지 설명할 수 있다.
- [ ] 세션이 서버 메모리와 어떤 관계가 있는지 설명할 수 있다.
- [ ] 로그인 상태 유지에 세션이 왜 자주 쓰이는지 말할 수 있다.

추천 실험:

- [ ] 세션 무효화 기능을 추가한다.
- [ ] 서로 다른 브라우저에서 세션이 분리되는지 확인한다.

정리 질문:

1. 쿠키와 세션의 역할은 어떻게 다른가?
2. 톰캣 재시작 시 세션은 어떻게 되는가?

---

## 8. JSP And View Rendering

목표: JSP가 서블릿과 어떤 관계인지 이해한다.

- [ ] `index.jsp`를 수정해서 동적 표현을 조금 추가해본다.
- [ ] JSP도 결국 서블릿으로 변환된다는 사실을 이해한다.
- [ ] "서블릿이 직접 HTML을 쓰는 방식"과 "JSP로 뷰를 분리하는 방식"을 비교할 수 있다.

추천 실험:

- [ ] 서블릿에서 request attribute를 넣고 JSP에서 출력해본다.
- [ ] forward와 redirect 차이를 확인한다.

정리 질문:

1. JSP는 누가 실행하는가?
2. MVC에서 JSP는 어느 레이어에 가까운가?

---

## 9. Error Handling And Logs

목표: 에러가 났을 때 어디를 봐야 하는지 익힌다.

할 일:

- [ ] 의도적으로 예외를 하나 발생시켜본다.
- [ ] 톰캣 로그와 브라우저 응답을 비교한다.
- [ ] 404와 500의 차이를 확인한다.

추천 실험:

- [ ] `web.xml`로 에러 페이지 매핑을 추가한다.
- [ ] 없는 URL 요청 시 톰캣이 어떤 응답을 주는지 확인한다.

확인:

- [ ] "컨테이너가 처리한 에러"와 "애플리케이션 코드가 던진 에러"를 구분할 수 있다.
- [ ] 로그를 볼 때 우선순위를 정할 수 있다.

정리 질문:

1. 지금 프로젝트에서 에러가 났을 때 가장 먼저 볼 로그는 어디인가?
2. 외장 톰캣에서는 애플리케이션 로그와 서버 로그가 어떻게 구분될 수 있는가?

---

## 10. Spring MVC On Top Of Servlet

목표: 이제 스프링이 서블릿 위에서 어떻게 동작하는지 연결한다.

이 단계에서 이해할 핵심:

- 톰캣은 여전히 HTTP 요청을 받는다.
- 스프링 MVC는 보통 `DispatcherServlet`을 진입점으로 사용한다.
- 톰캣은 "스프링 컨트롤러"를 직접 아는 것이 아니라 "서블릿"을 실행한다.

체크리스트:

- [ ] `DispatcherServlet`이 무엇인지 설명할 수 있다.
- [ ] `HelloServlet`과 `DispatcherServlet`의 공통점/차이를 설명할 수 있다.
- [ ] 스프링 MVC 도입 후 URL 매핑 주체가 어떻게 바뀌는지 설명할 수 있다.
- [ ] `Controller`, `Service`, `Repository`가 서블릿 컨테이너와 어떤 관계인지 설명할 수 있다.

정리 질문:

1. 스프링 MVC를 도입해도 톰캣이 여전히 필요한 이유는 무엇인가?
2. 스프링 MVC는 서블릿 스펙 위에서 어떤 추상화를 제공하는가?

---

## 11. Spring Boot WAR Deployment

목표: 내장 톰캣 방식과 외장 톰캣 WAR 배포 방식을 연결한다.

체크리스트:

- [ ] Spring Boot JAR 실행과 WAR 배포의 차이를 설명할 수 있다.
- [ ] 외장 톰캣 배포를 위해 왜 WAR 패키징이 필요한지 설명할 수 있다.
- [ ] `SpringBootServletInitializer`가 왜 필요한지 설명할 수 있다.
- [ ] "내장 톰캣"과 "외장 톰캣"에서 설정 책임이 어떻게 달라지는지 정리할 수 있다.

비교 항목:

- [ ] 실행 방식
- [ ] 배포 방식
- [ ] 설정 위치
- [ ] 로그 확인 방식
- [ ] 운영 편의성
- [ ] 컨테이너 이해도 확보 측면

정리 질문:

1. 빠른 개발에는 왜 내장 톰캣이 유리한가?
2. 학습과 운영 이해 측면에서는 왜 외장 톰캣이 도움이 되는가?

---

## 12. Final Compare And Explain

목표: 최종적으로 남에게 설명할 수 있을 정도로 정리한다.

- [ ] "브라우저에서 `/hello-servlet` 요청을 보내면 무슨 일이 일어나는가?"를 5분 안에 설명할 수 있다.
- [ ] "톰캣과 스프링의 경계"를 예시와 함께 설명할 수 있다.
- [ ] "서블릿을 먼저 공부하고 스프링으로 가는 이유"를 설명할 수 있다.
- [ ] "내장 톰캣과 외장 톰캣의 차이"를 실무 관점과 학습 관점으로 나눠 설명할 수 있다.

최종 산출물:

- [ ] 요청 흐름 다이어그램 1장
- [ ] 톰캣 핵심 개념 정리 1장
- [ ] 서블릿 생명주기 정리 1장
- [ ] Spring MVC 연결 정리 1장
- [ ] 내장 톰캣 vs 외장 톰캣 비교표 1장

---

## Suggested Order For This Project

이 프로젝트에서 실제 작업 순서는 아래처럼 가면 된다.

1. 현재 `HelloServlet` 동작 확인
2. 외장 톰캣에 WAR 배포
3. 요청 흐름 추적
4. `web.xml`로 서블릿 등록 전환
5. Filter 추가
6. Listener 추가
7. Session 실험
8. JSP forward/redirect 실험
9. 에러 페이지/로그 확인
10. 그 다음 Spring MVC 도입
11. 마지막에 Spring Boot WAR 배포와 비교

---

## Practical Notes

- 이 프로젝트는 `jakarta.servlet` 계열을 사용한다.
- 톰캣 버전과 Servlet API 버전은 반드시 맞춰서 본다.
- 외장 톰캣 학습의 핵심은 "코드 작성"보다 "배포 후 관찰"이다.
- 매 단계마다 로그, URL 매핑, 배포 결과 디렉터리를 함께 확인해야 이해가 빨라진다.

---

## Next Immediate Tasks

바로 다음으로 할 일:

- [ ] WAR를 외장 톰캣에 실제 배포한다.
- [ ] `/hello-servlet` 접속 성공 화면을 확인한다.
- [ ] 톰캣 로그에서 이 앱의 배포 로그를 찾는다.
- [ ] 배포된 `WEB-INF/classes` 구조를 눈으로 확인한다.
- [ ] 요청 흐름을 10줄 이내로 직접 적어본다.
