# 빌드/실행 실패 이슈 정리 (2026-08-04)

## 증상

`LibraryAppApplication` 실행 시 exit code 1로 종료. 스택트레이스가 길지만 맨 아래 `Caused by`가 진짜 원인이다.

```
Caused by: java.lang.IllegalStateException:
    Cannot load driver class: com.mysql.cj.jdbc.Driver
```

위쪽의 `entityManagerFactory` → `dataSourceScriptDatabaseInitializer` → `dataSource` 실패는 전부 여기서 파생된 연쇄 반응.

> **스프링 에러는 항상 마지막 `Caused by`부터 읽는다.**

## 원인 1 — MySQL 드라이버가 classpath에 없음 (핵심)

`application.yml`은 MySQL을 쓰겠다고 선언했는데, `build.gradle`에는 H2만 있었다.

```gradle
runtimeOnly 'com.h2database:h2'   // MySQL 커넥터 없음
```

설정과 의존성이 어긋난 상태. 실행 로그의 classpath에도 `h2-2.3.232.jar`만 있고 `mysql-connector-j`는 없었다.

**수정:**

```gradle
runtimeOnly 'com.mysql:mysql-connector-j'
```

## 원인 2 — DB 이름 오타

```yaml
url: "jdbc:mysql://localhost/libary_app"   # r 누락
```

실제 스키마는 `library_app`. 원인 1만 고쳤다면 바로 다음 단계에서 "Unknown database"로 죽었을 것. → `library_app`으로 수정.

## 결과

```
Started LibraryAppApplication in 1.454 seconds
Database version: 9.6
```

정상 기동 및 MySQL 연결 확인.

---

## 왜 이런 일이 생겼나

강의에서 Spring Initializr로 프로젝트를 만들 때 `MySQL Driver` 체크박스가 빠졌다. 별도 설치가 필요한 게 아니라 **의존성 한 줄 차이**였다.

| 대상 | 성격 |
|---|---|
| MySQL 서버 | OS에 설치하는 별도 프로그램 |
| JDK, IntelliJ | OS에 설치 |
| **MySQL JDBC 드라이버** | 설치 아님. `build.gradle`에 선언하면 Gradle이 받아오는 **라이브러리(jar)** |

드라이버는 "자바 앱이 MySQL과 대화하는 법을 아는 라이브러리"다. 에러가 "연결 실패"가 아니라 "드라이버 클래스를 못 찾음"이었던 게 그 증거 — 접속을 시도조차 못 한 단계였다.

> **기억할 규칙: DB를 바꾸면 `application.yml`과 `build.gradle`을 항상 한 쌍으로 바꾼다.**

---

## 의존성 관리 개념 (프론트엔드 비교)

| | npm | Gradle |
|---|---|---|
| 선언 파일 | `package.json` | `build.gradle` |
| 받아오기 | `npm install` | `./gradlew build` 또는 IntelliJ 코끼리 아이콘 (`Cmd+Shift+I`) |
| 저장 위치 | `node_modules/` (프로젝트별) | `~/.gradle/caches/` (**전역 공유**) |
| 저장소 | npm registry | Maven Central |
| 선언 파일 자동 수정 | `npm install axios`가 해줌 | **없음. 직접 적어야 함** |

- 이번에 `mysql-connector-j-9.7.0.jar`가 새로 다운로드됨 (캐시에 있던 8.0.31, 9.2.0은 예전 프로젝트가 받아둔 것).
- `build.gradle`에 버전을 안 적었는데도 9.7.0이 붙은 건 Spring Boot dependency-management 플러그인이 부트 버전에 맞는 조합을 정해주기 때문. 그래서 스프링 의존성은 보통 이름만 적는다.
- 직접 적은 건 3줄인데 classpath에 jar가 50개 넘는 건 전이 의존성(transitive dependency) 때문.

---

## 부수적으로 발견한 것들

### `DROP DATABASE library` 문법 에러

```
ERROR 1064 (42000): You have an error in your SQL syntax; ... near 'library' at line 1
```

MySQL 9.2부터 JavaScript 저장 프로그램용 `CREATE/DROP LIBRARY` 구문이 생기면서 `LIBRARY`가 **예약어**가 됐다. 현재 서버가 9.6이라 해당됨.

확인 방법:

```sql
SELECT * FROM INFORMATION_SCHEMA.KEYWORDS WHERE WORD = 'LIBRARY';  -- RESERVED = 1
```

백틱으로 감싸면 되지만(`` DROP DATABASE `library`; ``), 이름을 `library_app`으로 쓰는 지금 방식이 더 편하다.

### 기타

- **설정 파일 2개 공존** — `application.properties`와 `application.yml`이 둘 다 있다. 지금은 키가 안 겹쳐서 무해하지만(`.properties` 우선), 하나로 합치는 걸 권장.
- **`spring.jpa.open-in-view` 경고** — 기본값 안내일 뿐 동작에 문제 없음.
- **H2 제거됨** — 나중에 테스트를 인메모리 DB로 돌리려면 `testRuntimeOnly 'com.h2database:h2'` 추가.

---

## Whitelabel Error Page 404 (별건, 에러 아님)

```
This application has no explicit mapping for /error, so you are seeing this as a fallback.
There was an unexpected error (type=Not Found, status=404).
```

브라우저로 `http://localhost:8080/` 를 열었을 때 나오는 스프링 기본 404 페이지. 루트 경로(`/`)에 매핑된 컨트롤러가 없어서 그렇다. **이 페이지가 보인다는 건 서버가 정상적으로 떠 있다는 뜻**이다 (앱이 죽었으면 "연결할 수 없음"이 뜬다).

현재 정의된 엔드포인트:

| 메서드 | 경로 | 위치 |
|---|---|---|
| GET | `/add` | `CalculatorController.java:12` |
| POST | `/multiply` | `CalculatorController.java:18` |
| POST | `/user` | `UserController.java:28` |
| GET | `/user` | `UserController.java:35` |

브라우저 주소창으로는 GET만 보낼 수 있다. POST는 curl이나 Postman 필요.
