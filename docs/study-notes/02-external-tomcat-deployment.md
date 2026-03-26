# 02. External Tomcat Deployment

## 목표

WAR 파일을 외장 톰캣에 배포하고 배포 구조를 이해한다.

## 확인한 것

- [x] 톰캣 버전과 Jakarta Servlet 호환성
- [x] `webapps`, `conf`, `logs` 위치
- [x] WAR 배포 후 context path
- [x] `WEB-INF/classes`, `WEB-INF/lib` 구조
- [x] 톰캣 배포 로그

## 실행 명령 / 절차

1. `gradlew clean war`로 WAR 빌드 — `build/libs/tomcat-1.0-SNAPSHOT.war` 생성
2. WAR 파일을 `C:\Program Files\Apache Software Foundation\Tomcat 11.0\webapps\tomcat-study.war`로 복사
3. 톰캣이 실행 중이면 자동으로 배포됨 (hot deploy)
4. `http://localhost:8080/tomcat-study/` 로 접근 확인

## 배운 내용 정리

### 1. 톰캣 버전과 Jakarta Servlet 호환성

이 프로젝트는 `jakarta.servlet-api:6.1.0`을 사용한다. Servlet 6.1은 Tomcat 11.x에서만 지원한다. Tomcat 10.x는 Servlet 6.0, Tomcat 9.x 이하는 `javax.*` 네임스페이스를 사용하는 구버전 스펙이다. 따라서 이 프로젝트는 반드시 Tomcat 11.x에 배포해야 한다.

### 2. 외장 톰캣의 디렉터리 구조

톰캣 설치 경로(`C:\Program Files\Apache Software Foundation\Tomcat 11.0\`) 아래의 주요 디렉터리:

| 디렉터리 | 역할 |
|---------|------|
| `webapps/` | WAR 파일을 놓는 배포 위치. 각 WAR가 하나의 웹 애플리케이션이 된다. |
| `conf/` | `server.xml`, `web.xml` 등 톰캣 전체 설정 파일 |
| `logs/` | `catalina.log`, 접근 로그 등 런타임 로그 |
| `lib/` | 톰캣 자체 라이브러리 (서블릿 API 등이 여기 있음) |
| `work/` | JSP 컴파일 결과물 등 톰캣이 런타임에 생성하는 파일 |

### 3. WAR 배포 방식과 context path

WAR 파일을 `webapps/`에 복사하면 톰캣이 자동으로 감지하고 압축을 해제한다. WAR는 사실 ZIP 포맷이고, 톰캣이 같은 이름의 디렉터리로 풀어서 서빙한다.

- `tomcat-study.war` → `webapps/tomcat-study/` 디렉터리로 자동 압축 해제
- context path는 WAR 파일명에서 결정된다: `tomcat-study.war` → `/tomcat-study`
- 따라서 `http://localhost:8080/tomcat-study/`로 접근

공식 문서는 **Context = 웹 애플리케이션**이라고 한 줄로 정의한다. 톰캣은 여러 웹 애플리케이션을 동시에 실행할 수 있고, context path가 URL에서 어느 애플리케이션인지 구분하는 기준이 된다.

```
http://localhost:8080/tomcat-study/hello-servlet
                     ^^^^^^^^^^^^^ ^^^^^^^^^^^^^^
                     context path  서블릿 경로
```

`webapps/` 아래에 여러 WAR를 배포하면 각각 독립된 context path를 갖는다:

```
webapps/tomcat-study.war  →  /tomcat-study
webapps/shop.war          →  /shop
webapps/ROOT.war          →  /  (루트, context path 없음)
```

### 4. 배포 후 `WEB-INF` 구조 확인

압축 해제된 `webapps/tomcat-study/` 안의 구조:

```text
webapps/tomcat-study/
├── index.jsp
├── META-INF/
│   └── MANIFEST.MF
└── WEB-INF/
    ├── web.xml
    └── classes/
        └── com/tomcat/tomcat/HelloServlet.class
```

01단계에서 확인한 WAR 내부 구조와 완전히 동일하다. WAR를 풀면 그게 곧 배포 디렉터리가 된다.

### 5. CATALINA_HOME과 CATALINA_BASE

공식 문서(Introduction)에서는 두 변수를 다음과 같이 정의한다.

- **`CATALINA_HOME`** — 톰캣 설치 루트. `bin/`, `lib/` 같은 **정적 파일(바이너리, JAR)**이 위치한다.
- **`CATALINA_BASE`** — 특정 톰캣 인스턴스의 런타임 루트. `webapps/`, `conf/`, `logs/`, `work/`, `temp/` 같은 **동적 파일(설정, 로그, 배포)**이 여기서 resolve된다.

별도로 설정하지 않으면 `CATALINA_BASE = CATALINA_HOME`이다. 현재 설치 환경에서도 환경변수로 따로 지정되어 있지 않아 둘 다 `C:\Program Files\Apache Software Foundation\Tomcat 11.0`을 가리킨다.

두 값을 분리하는 이유는 **하나의 톰캣 바이너리로 여러 인스턴스를 띄울 때**다. `CATALINA_HOME`의 바이너리를 공유하면서 인스턴스마다 별도의 `CATALINA_BASE`를 지정해 `webapps/`, `conf/`, `logs/`를 독립적으로 운영한다. 이렇게 하면 톰캣 버전 업그레이드 시 바이너리만 교체하면 되고, 각 인스턴스의 설정과 애플리케이션은 그대로 유지된다.

주의할 점은 `conf/server.xml`, `conf/web.xml`은 반드시 `CATALINA_BASE` 아래에 있어야 하며, `CATALINA_HOME`으로 fallback되지 않는다.

## 로그/배포 구조 관찰

`logs/catalina.2026-03-26.log`에서 확인한 배포 로그:

```text
26-Mar-2026 11:26:08.580 INFO [Catalina-utility-2] org.apache.catalina.startup.HostConfig.deployWAR
웹 애플리케이션 아카이브 [C:\...\webapps\tomcat-study.war]을(를) 배치합니다.

26-Mar-2026 11:26:08.969 INFO [Catalina-utility-2] org.apache.catalina.startup.HostConfig.deployWAR
웹 애플리케이션 아카이브 [C:\...\webapps\tomcat-study.war]의 배치가 [388] 밀리초에 완료되었습니다.
```

- `Catalina-utility-2` — 백그라운드에서 `webapps`를 주기적으로 감시하는 스레드
- `HostConfig.deployWAR` — WAR 파일을 감지해서 배포하는 컴포넌트
- 388ms만에 배포 완료

## 답해야 하는 질문

1. 톰캣은 WAR 파일을 어떤 단위의 웹 애플리케이션으로 취급하는가?
2. 현재 애플리케이션의 context path는 무엇인가?

## 질문에 대한 답

### 1. 톰캣은 WAR 파일을 어떤 단위의 웹 애플리케이션으로 취급하는가?

WAR 파일 하나가 웹 애플리케이션(Context) 하나다. 톰캣은 `webapps/` 아래의 각 WAR를 독립된 웹 애플리케이션으로 등록하고, 별도의 클래스로더와 context path를 부여한다. 여러 WAR를 동시에 배포하면 각각 독립적으로 동작한다.

### 2. 현재 애플리케이션의 context path는 무엇인가?

`/tomcat-study`다. WAR 파일명인 `tomcat-study.war`에서 결정된다. 브라우저에서 `http://localhost:8080/tomcat-study/`로 접근하고, `/hello-servlet`은 `http://localhost:8080/tomcat-study/hello-servlet`이 된다.

## 참고 문헌

- Apache Tomcat 11.0 Introduction: https://tomcat.apache.org/tomcat-11.0-doc/introduction.html
- Apache Tomcat 11.0 Application Developer's Guide - Deployment: https://tomcat.apache.org/tomcat-11.0-doc/appdev/deployment.html

## 내 말로 설명

WAR 파일을 톰캣의 `webapps/`에 복사하면 배포는 끝난다. 톰캣이 파일을 감지하고 자동으로 압축을 해제한 뒤 웹 애플리케이션으로 등록한다. WAR 파일명이 곧 context path가 되어 URL의 첫 번째 경로 세그먼트가 된다. 배포된 애플리케이션은 톰캣 안에서 독립된 단위로 동작하며, 서블릿 API는 톰캣의 `lib/`에서 제공한다.