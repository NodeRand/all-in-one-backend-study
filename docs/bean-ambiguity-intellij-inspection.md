# 같은 타입 빈이 2개일 때 (@Primary · @Qualifier), 그리고 IntelliJ가 침묵한 이유 (2026-09-04)

> 강의 [27]~ 구간. `BookRepository` 인터페이스에 구현체를 2개 만들고 나서.
> 개념 배경은 [spring-bean-di-ioc.md](spring-bean-di-ioc.md) 참고.

## 증상

강의에서는 *"이 시점에 `bookRepository`에 빨간 밑줄이 뜬다 — 어떤 빈을 넣을지 스프링이 모른다"* 고 하는데,
**내 IntelliJ에서는 밑줄이 안 떴다.** 코드가 맞게 짜여서 안 뜨는 건지, IDE가 못 잡는 건지 알 수 없는 상태.

당시 코드:

```java
// repository/book/BookRepository.java
public interface BookRepository { void saveBook(); }

// repository/book/BookMemoryRepository.java
@Repository
public class BookMemoryRepository implements BookRepository { ... }

// repository/book/BookMySqlRepository.java
@Repository
public class BookMySqlRepository implements BookRepository { ... }

// service/book/BookService.java
@Service
public class BookService {
    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {   // ← 강의에서 빨간 밑줄이 뜬다는 자리
        this.bookRepository = bookRepository;
    }
}
```

## 확인 — 코드는 진짜로 모호했다

밑줄 유무로 판단하지 말고 **컨텍스트를 띄워서** 확인한다. 다만 이 프로젝트는 MySQL이 안 떠 있으면
`entityManagerFactory`가 **먼저** 죽어서 빈 모호성까지 도달하지 못한다. 그래서 book 패키지만 스캔하는
임시 테스트로 격리해서 확인했다:

```java
@Configuration
@ComponentScan(basePackages = {
    "com.group.library_app.repository.book",
    "com.group.library_app.service.book"
})
static class BookOnlyConfig {}
// new AnnotationConfigApplicationContext(BookOnlyConfig.class)
```

결과:

```
org.springframework.beans.factory.NoUniqueBeanDefinitionException
No qualifying bean of type 'com.group.library_app.repository.book.BookRepository'
available: expected single matching bean but found 2:
    bookMemoryRepository, bookMySqlRepository
```

> **강의 말이 맞았다. IDE만 조용했다.**

이름으로 구제되지도 않는다 — 생성자 파라미터 이름이 `bookRepository`인데 빈 이름은
`bookMemoryRepository` / `bookMySqlRepository`라 어느 쪽과도 안 맞는다 (아래 §해결 ③ 참고).

## 원인 — Spring 플러그인이 로드되지 않았다

*"Could not autowire. There is more than one bean of type ..."* 는 **자바 컴파일러가 아니라 IntelliJ의
Spring 플러그인이 제공하는 검사**다. 순수 자바 문법으로 `BookService(BookRepository)`는 아무 문제 없는
코드라, 플러그인이 없으면 아무도 밑줄을 그어주지 않는다.

IDE 로그(`~/Library/Logs/JetBrains/IntelliJIdea2026.1/idea.log`, 2026-09-04 19:43 기동분):

```
Module intellij.spring      is not enabled because dependency com.intellij.modules.ultimate is not available
Module intellij.spring.core is not enabled because dependency intellij.spring is not available
Module intellij.spring.boot is not enabled because dependency intellij.spring is not available
```

그리고 `~/Library/Application Support/JetBrains/IntelliJIdea2026.1/disabled_plugins.txt`:

```
com.intellij.modules.ultimate
```

바이너리는 Ultimate(`IU-261.25134.95`)가 맞는데 **Ultimate 모듈이 비활성**이라 무료 기능셋으로 돌고 있었다.
라이선스 키 파일도 없다. 즉 Spring 플러그인이 디스크에는 있지만 로드가 안 된 상태.

| | 가격 | Spring 지원 |
|---|---|---|
| IntelliJ IDEA Community | 무료 | ❌ 빈 검사 · 초록 콩나물 아이콘 · 설정 자동완성 전부 없음 |
| IntelliJ IDEA Ultimate | 유료 (30일 체험) | ✅ |

**같이 죽는 것들** — Database 툴윈도우도 Ultimate 전용이다. [19]~[21] DB 강의 때 쓰던 게 지금은 안 열린다
(`.idea/dataSources.xml`은 남아 있음). 대안: 터미널 `mysql` CLI, DBeaver, MySQL Workbench (전부 무료).

### 되살리는 방법

1. **학생·교직원 라이선스** — `.ac.kr` 메일이나 ISIC 카드로 Ultimate 전 기능 무료(1년, 재학 중 갱신). 1순위.
2. 유료 구독 — 개인 기준 연 $16x 수준, 해마다 내려간다. 결제 전 jetbrains.com/idea/buy 에서 현재가 확인.
3. ⚠️ JetBrains **비상업용 무료 라이선스는 Rider · CLion · WebStorm 등이 대상이고 IDEA Ultimate은 아니다.**
   헷갈리기 쉬운 지점.

### 안 사도 강의는 진행된다

밑줄은 **편의**지 정답의 출처가 아니다. 빈 등록·주입 실패는 **전부 기동 시점 예외**라 띄우면 드러난다:

```bash
./gradlew bootRun     # 모호하면 NoUniqueBeanDefinitionException 을 뱉고 죽는다
```

오히려 이 구간에서 남는 게 있다 — **스택트레이스 맨 아래 `Caused by`를 읽는 눈**.
([2026-08-04 드라이버 이슈](../notes/2026-08-04-mysql-driver-issue.md)에서 얻은 규칙과 같다)

## 해결 — 3가지

### ① `@Primary` — "기본값은 이놈" (구현체 쪽)

```java
@Primary
@Repository
public class BookMemoryRepository implements BookRepository { ... }
```

### ② `@Qualifier` — "이 자리는 이놈" (쓰는 쪽)

```java
public BookService(@Qualifier("bookMySqlRepository") BookRepository bookRepository) { ... }
```

### ③ 파라미터 이름을 빈 이름과 맞추기

```java
public BookService(BookRepository bookMemoryRepository) { ... }
```

타입으로 후보를 못 좁히면 스프링이 **이름으로 한 번 더 시도**하는 걸 이용한 것. 동작은 하지만
**변수명만 바꿔도 조용히 깨지므로 실무에서는 안 쓴다.**

### 우선순위

| | 성격 | 붙는 위치 | 강도 |
|---|---|---|---|
| `@Primary` | 평소 기본값 | 구현체(빈) | 약 |
| `@Qualifier` | 이번 자리만 예외 | 주입받는 쪽 | **강 (둘이 겹치면 이김)** |
| 파라미터 이름 | 우연한 일치 | 주입받는 쪽 | 최약 (권장 안 함) |

> **기억할 규칙: 같은 타입 빈이 2개 이상이면 스프링은 절대 알아서 고르지 않는다.**
> `@Primary`로 기본을 정하거나 `@Qualifier`로 자리마다 지정해야 한다.

## 곁다리 — 나중에 물릴 것

`repository/book/BookMemoryRepository.java`

```java
import java.awt.print.Book;   // ← 도메인 Book이 아니라 AWT 인쇄 API의 Book
```

지금은 아래 코드가 다 주석이라 조용하지만, `books.add(new Book())` 주석을 푸는 순간 엉뚱한 클래스가 붙는다.
IntelliJ 자동 import 후보에서 잘못 고를 때 흔히 나는 실수. `domain/book/Book`을 만들면 **이 import 줄부터
지우고** 다시 넣을 것.
