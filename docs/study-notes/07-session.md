# 07. Session

## 목표

세션이 톰캣 레벨에서 어떻게 유지되는지 이해한다.

## 확인한 것

- [x] `HttpSession` 사용
- [x] `JSESSIONID` 확인
- [x] 브라우저별 세션 차이
- [x] 세션 무효화 결과

## 배운 내용 정리

### 1. HTTP는 stateless — 세션이 필요한 이유

HTTP는 기본적으로 요청과 응답이 끝나면 연결 상태를 기억하지 않는다. 같은 브라우저가 두 번 요청해도 서버 입장에서는 동일 사용자인지 알 방법이 없다. 이 문제를 해결하기 위해 세션을 사용한다.

### 2. 톰캣의 세션 동작 방식

1. 브라우저가 처음 요청을 보낸다
2. 서블릿에서 `request.getSession()`을 호출하면 톰캣이 새 세션을 생성하고 고유한 세션 ID를 발급한다
3. 톰캣이 응답 헤더에 `Set-Cookie: JSESSIONID=<세션ID>; Path=/tomcat-study`를 추가한다
4. 브라우저가 이후 요청부터 쿠키에 `JSESSIONID`를 담아 보낸다
5. 톰캣이 `JSESSIONID`를 보고 기존 세션 객체를 찾아 서블릿에 전달한다

세션 데이터는 서버(톰캣) 메모리에 저장된다. 브라우저에는 세션 ID만 쿠키로 저장된다.

### 3. `HttpSession` API

```java
// 세션 가져오기 (없으면 새로 생성)
HttpSession session = request.getSession();

// 세션 가져오기 (없으면 null 반환, 새로 생성하지 않음)
HttpSession session = request.getSession(false);

// 세션에 데이터 저장
session.setAttribute("count", 1);

// 세션에서 데이터 읽기
Integer count = (Integer) session.getAttribute("count");

// 세션 ID 확인
String id = session.getId();

// 세션 무효화 (저장된 데이터 모두 삭제)
session.invalidate();
```

### 4. JSESSIONID와 Path

브라우저 개발자 도구(F12 → Application → Cookies)에서 확인한 JSESSIONID:

- `Path: /tomcat-study` — 우리 앱(`tomcat-study`)의 세션
- `Path: /` — ROOT 앱의 세션 (ROOT 앱에 접근한 경우에만 생성)

`Path`가 다르면 별개의 쿠키다. 브라우저는 요청 URL의 path에 맞는 쿠키만 전송하므로, `/tomcat-study/session` 요청에는 `Path: /tomcat-study`짜리 JSESSIONID만 포함된다.

시크릿 창에서는 쿠키가 초기화된 상태라 ROOT 앱에 접근하지 않으면 `Path: /` JSESSIONID가 생기지 않는다.

### 5. 브라우저별 세션 차이

일반 창과 시크릿 창은 쿠키를 공유하지 않는다. 따라서 Session ID가 다르고 방문 횟수도 독립적으로 카운트된다. 크롬과 파이어폭스도 마찬가지로 별도의 세션을 갖는다.

이것이 "세션은 브라우저 단위"라는 의미다. 정확히는 쿠키를 공유하는 단위가 세션의 경계가 된다.

### 6. 세션 무효화

"세션 무효화" 링크 클릭 시 `session.invalidate()`가 호출된다. 그 결과:

- 서버 메모리에서 세션 객체와 저장된 데이터가 삭제됨
- 이후 `request.getSession()`을 호출하면 새 세션이 생성되고 새 JSESSIONID가 발급됨
- 방문 횟수가 1로 초기화됨

로그아웃 기능이 내부적으로 이 방식을 사용한다.

### 7. HttpSessionListener

06단계에서 언급했듯이 세션 생성/소멸 시점을 감지할 수 있다.

```java
public class MySessionListener implements HttpSessionListener {

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        System.out.println("세션 생성: " + se.getSession().getId());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        System.out.println("세션 소멸: " + se.getSession().getId());
    }
}
```

세션 무효화 시 `sessionDestroyed()`가 호출되고, 새 세션 생성 시 `sessionCreated()`가 호출된다.

## 실험 결과

- `http://localhost:8080/tomcat-study/session` 접근 → Session ID와 방문 횟수 1 출력
- 새로고침 → 방문 횟수 증가, Session ID 동일
- 시크릿 창에서 접근 → 다른 Session ID, 방문 횟수 1부터 시작
- "세션 무효화" 클릭 → 리다이렉트 후 방문 횟수 1로 초기화, 새 Session ID 발급

## 답해야 하는 질문

1. 쿠키와 세션의 역할은 어떻게 다른가?
2. 톰캣 재시작 시 세션은 어떻게 되는가?

## 질문에 대한 답

### 1. 쿠키와 세션의 역할은 어떻게 다른가?

쿠키는 데이터를 브라우저(클라이언트)에 저장한다. 세션은 데이터를 서버 메모리에 저장하고, 브라우저에는 세션을 식별하는 키(JSESSIONID)만 쿠키로 저장한다. 쿠키는 브라우저 개발자 도구에서 직접 읽고 수정할 수 있어 민감한 데이터를 저장하기에 적합하지 않다. 세션은 실제 데이터가 서버에 있으므로 상대적으로 안전하지만, 서버 메모리를 소비하고 서버가 재시작되면 사라진다.

### 2. 톰캣 재시작 시 세션은 어떻게 되는가?

기본적으로 톰캣은 재시작 시 세션을 메모리에서 직렬화해서 파일로 저장하고(`work/` 디렉터리), 재시작 후 다시 복원한다. 이를 세션 퍼시스턴스(Session Persistence)라고 한다. 단, 세션에 저장된 객체가 `Serializable`을 구현하지 않으면 직렬화에 실패해서 세션이 사라진다. 이 기능을 끄면 재시작 시 모든 세션이 초기화된다.

## 내 말로 설명

세션은 HTTP의 stateless 특성을 보완하기 위한 서버 측 저장소다. 톰캣이 세션 ID를 발급하고 JSESSIONID 쿠키로 브라우저에 전달한다. 이후 요청마다 브라우저가 JSESSIONID를 보내면 톰캣이 해당 세션 객체를 찾아준다. 세션 데이터는 서버 메모리에 있고 브라우저에는 ID만 있다. 브라우저(쿠키)가 달라지면 세션도 달라진다.