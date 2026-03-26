# 12. Spring Boot WAR Deployment

## 목표

Spring Boot WAR 배포와 내장 톰캣 방식을 비교한다.

## 확인한 것

- [x] JAR 실행과 WAR 배포 차이
- [x] `SpringBootServletInitializer` 역할
- [x] 내장 톰캣과 외장 톰캣의 설정 책임 차이
- [x] 로그와 운영 방식 차이

## 배운 내용 정리

### 1. build.gradle 변경

Spring Boot 플러그인을 추가하고 의존성을 Spring Boot 방식으로 변경했다.

```gradle
plugins {
    id 'java'
    id 'war'
    id 'org.springframework.boot' version '3.4.4'
    id 'io.spring.dependency-management' version '1.1.7'
}

dependencies {
    compileOnly('jakarta.servlet:jakarta.servlet-api')
    implementation('org.springframework.boot:spring-boot-starter-web')
    providedRuntime('org.springframework.boot:spring-boot-starter-tomcat')
}
```

핵심은 `providedRuntime('spring-boot-starter-tomcat')`이다. 내장 톰캣을 WAR에서 제외한다는 선언이다. JAR로 실행할 때는 내장 톰캣이 필요하지만, 외장 톰캣에 배포할 때는 컨테이너가 이미 있으므로 포함하면 충돌한다.

### 2. SpringBootServletInitializer와 configure()

```java
@SpringBootApplication
public class SpringApp extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(SpringApp.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringApp.class, args);
    }
}
```

두 가지 진입점이 공존한다:

| 실행 방식 | 진입점 | 역할 |
|---|---|---|
| JAR 직접 실행 | `main()` | JVM이 직접 호출 |
| WAR 외장 톰캣 배포 | `configure()` | 톰캣이 `SpringBootServletInitializer.onStartup()` 호출 시 사용 |

`configure()`가 없으면 외장 톰캣은 어떤 클래스로 스프링을 시작해야 할지 알 수 없다. `SpringBootServletInitializer`의 기본 `configure()`는 아무것도 하지 않으므로 반드시 오버라이드해야 한다.

`SpringBootServletInitializer`는 `WebApplicationInitializer`를 구현한다. 11단계에서 만든 `AppInitializer`와 동일한 메커니즘이다. 톰캣이 시작 시 클래스패스에서 `WebApplicationInitializer` 구현체를 찾아 `onStartup()`을 호출하고, `SpringBootServletInitializer`가 `configure()`에서 지정한 클래스로 스프링 컨텍스트를 초기화한다.

### 3. bootWar vs war

Spring Boot 플러그인을 추가하면 두 가지 WAR 빌드 태스크가 생긴다:

| 태스크 | 결과물 | 내용 |
|---|---|---|
| `./gradlew war` | `*-plain.war` | 내장 톰캣 제외한 순수 WAR |
| `./gradlew bootWar` | `*.war` | 실행 가능한 WAR (내장 톰캣 포함 + 외장 톰캣 배포 가능) |

외장 톰캣 배포에는 `bootWar`로 빌드한 WAR를 사용해야 한다.

### 4. DispatcherServlet 매핑 차이

11단계(순수 Spring MVC)에서는 `AppInitializer`에서 `DispatcherServlet`을 `/spring/*`에 직접 매핑했다. Spring Boot는 `DispatcherServlet`을 기본적으로 `/*`에 자동 매핑한다.

```
11단계: http://localhost:8080/tomcat-study/spring/hello
12단계: http://localhost:8080/tomcat-study/hello
```

Spring Boot가 `DispatcherServlet` 등록과 매핑을 자동으로 처리하기 때문이다.

### 5. 내장 톰캣 vs 외장 톰캣 설정 책임 차이

| | 내장 톰캣 (JAR) | 외장 톰캣 (WAR) |
|---|---|---|
| 톰캣 버전 | `spring-boot-starter-tomcat`이 결정 | 설치된 톰캣 버전 사용 |
| 포트 설정 | `application.properties`의 `server.port` | `server.xml`의 `Connector` |
| 톰캣 설정 | `application.properties` 또는 Java Config | `server.xml`, `context.xml` |
| 로그 위치 | 애플리케이션 로그에 통합 | 톰캣 `logs/` 디렉터리 |
| 배포 방식 | `java -jar` | `webapps/`에 WAR 복사 |

내장 톰캣은 스프링이 톰캣을 제어한다. 외장 톰캣은 톰캣이 스프링을 제어한다.

## 비교표 초안

```
JAR 실행 (내장 톰캣)                    WAR 배포 (외장 톰캣)
─────────────────────                    ────────────────────
java -jar app.jar                        webapps/app.war 복사
     │                                        │
     ▼                                        ▼
SpringApplication.run()              톰캣이 WAR 압축 해제
     │                                        │
     ▼                                        ▼
내장 톰캣 시작                    SpringBootServletInitializer.onStartup()
     │                                        │
     ▼                                        ▼
DispatcherServlet 자동 등록        configure()로 스프링 컨텍스트 초기화
     │                                        │
     ▼                                        ▼
요청 처리                                요청 처리
```

## 답해야 하는 질문

1. 빠른 개발에는 왜 내장 톰캣이 유리한가?
2. 학습과 운영 이해 측면에서는 왜 외장 톰캣이 도움이 되는가?

## 질문에 대한 답

### 1. 빠른 개발에는 왜 내장 톰캣이 유리한가?

`java -jar`로 바로 실행되므로 별도의 톰캣 설치나 WAR 배포 과정이 없다. 코드 변경 후 재시작이 단순하고, `application.properties` 하나로 모든 설정을 관리할 수 있다. CI/CD 파이프라인도 단순해진다.

### 2. 학습과 운영 이해 측면에서는 왜 외장 톰캣이 도움이 되는가?

내장 톰캣은 톰캣의 동작을 스프링이 추상화해버린다. 외장 톰캣을 쓰면 WAR 배포 구조, `webapps/` 디렉터리, `server.xml` 설정, 톰캣 로그 파일, Context 개념 등을 직접 마주하게 된다. 또한 여러 애플리케이션을 하나의 톰캣에 올리는 구조, 톰캣 단위의 스레드 풀 설정 등 운영 환경에서 실제로 다루는 개념들을 이해할 수 있다.

## 내 말로 설명

Spring Boot WAR 배포의 핵심은 두 가지다. 첫째, `providedRuntime`으로 내장 톰캣을 WAR에서 제외한다. 둘째, `SpringBootServletInitializer`의 `configure()`가 외장 톰캣에서의 진입점이 된다. `main()`은 JAR 실행 시, `configure()`는 WAR 배포 시 각각 스프링을 시작하는 역할을 한다. Spring Boot는 `DispatcherServlet`을 `/*`에 자동 매핑하므로, 11단계처럼 직접 경로를 지정할 필요가 없다.