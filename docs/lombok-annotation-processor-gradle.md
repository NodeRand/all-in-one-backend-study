# 롬복을 넣었는데 `cannot find symbol method getId()` — Gradle은 `annotationProcessor` 가 있어야 롬복을 돌린다 (2026-10-28)

> 강의 [60] application.yml과 application.properties, lombok.
> 개념 배경은 [58] build.gradle 이해하기(`implementation` · `runtimeOnly` 구분) 필기.

## 증상

강의대로 `build.gradle` 에 롬복을 넣고 `User` · `UserCreateRequest` 의 getter 를 지운 뒤 `@Getter` 를 붙였다.
IntelliJ 실행 버튼을 누르자 빌드가 실패했다.

```groovy
dependencies {
	implementation 'org.projectlombok:lombok'   // ← 강의 화면 그대로
	...
}
```

```
UserResponse.java
  cannot find symbol method getId()    :17
  cannot find symbol method getName()  :18
  cannot find symbol method getAge()   :19
  symbol:   method getId()
  location: variable user of type User
```

`@Getter` 가 붙어 있는데도 `getId()` 가 **실제로 없는** 상태로 컴파일됐다.
IntelliJ 설정 「어노테이션 처리 활성화」 는 이미 켜져 있었다(`.idea/compiler.xml` · `enabled="true"`).

## 원인 — 롬복은 라이브러리가 아니라 「컴파일 중에 끼어드는 도구」 다

- 롬복은 실행 중에 쓰이는 코드가 아니다. **컴파일하는 순간** `@Getter` 를 보고 `getId()` 같은 메서드를
  만들어 넣는 **어노테이션 프로세서**다.
- **Gradle 은 5.0(2018) 부터** `implementation`(컴파일 클래스패스)에 있는 프로세서를 자동으로 돌리지 않는다.
  `annotationProcessor` 로 **따로 지정한 것만** 돌린다. 그래서 `@Getter` 가 무시됐다.
- 이 프로젝트는 IntelliJ 가 빌드를 **Gradle 에 맡긴다**(Settings › Build Tools › Gradle › *Build and run using* = Gradle,
  `.idea/gradle.xml` 에 따로 지정이 없으면 이 값). 실행 버튼도 결국 Gradle 빌드라, IntelliJ 의 「어노테이션 처리 활성화」
  체크는 이 빌드에 영향을 주지 않는다.

### 그럼 강의는 왜 `implementation` 만으로 됐나

*Build and run using* 을 **IntelliJ IDEA** 로 두면 IntelliJ 자체 컴파일러가 빌드하고, 그 컴파일러는
어노테이션 프로세서 화면의 **「프로젝트 클래스 경로에서 프로세서 가져오기」** 설정대로 클래스패스의 롬복을 찾아 돌린다.
강의 화면의 3단계(의존성 → IntelliJ 롬복 플러그인 → IntelliJ 어노테이션 프로세서 설정)가 이 경로다.
**강의 화면의 빌드 담당 설정은 확인하지 못했다** — 동작으로 보아 IntelliJ 빌드였을 것으로 본다.

| 빌드 담당 | `implementation` 만 | `compileOnly` + `annotationProcessor` |
| --- | --- | --- |
| IntelliJ IDEA (강의로 추정) | 된다 (IDE 의 「어노테이션 처리 활성화」 가 클래스패스에서 찾는다) | 된다 |
| **Gradle (이 프로젝트)** | **실패** | 된다 |
| 서버 `./gradlew build` ([54]) | **실패** | 된다 |

## 처방

```diff
 dependencies {
-	implementation 'org.projectlombok:lombok'
+	compileOnly 'org.projectlombok:lombok'
+	annotationProcessor 'org.projectlombok:lombok'
 	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
```

- `annotationProcessor` — 컴파일할 때 롬복을 프로세서로 돌린다. **이게 빠져서 난 에러다.**
- `compileOnly` — 코드가 `lombok.Getter` 를 import 할 수 있게 컴파일할 때만 둔다. 메서드를 만들어 넣고 나면
  실행 중엔 필요 없어서 jar 에 안 들어간다. `implementation`(컴파일 + 실행) · `runtimeOnly`(실행만)에 이은 셋째 칸.
- 버전은 안 적는다 — 스프링 부트의 dependency-management 가 롬복 버전도 맞춰 준다([58]).
- 확인: `./gradlew compileJava` → `BUILD SUCCESSFUL`. IntelliJ 는 Gradle 새로고침(코끼리 아이콘) 뒤 실행.
- 테스트 코드에서도 롬복을 쓰면 `testCompileOnly` · `testAnnotationProcessor` 두 줄이 더 필요하다.
- 요즘 Spring Initializr 에서 Lombok 을 고르면 처음부터 위 두 줄로 만들어 준다.

## 배운 것

- **IDE 에서 되는 것 ≠ 빌드에서 되는 것.** IDE 설정에 기대는 방식은 그 IDE 안에서만 된다.
  `build.gradle` 에 맞게 적어야 터미널 · 서버([54]의 `./gradlew build`) · 다른 사람 컴퓨터에서도 똑같이 된다.
- 에러가 「메서드가 없다」 인데 어노테이션은 붙어 있다면, **그 어노테이션을 처리할 도구가 컴파일에 참여했는지**부터 본다.
