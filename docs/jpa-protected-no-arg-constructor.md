# JPA 기본 생성자와 `protected` 정리

> library-app 실습 중 정리. `Book` · `UserLoanHistory` 엔티티에 `protected Book() {}` 을 왜 쓰는지에서 출발해 자바 접근제어자까지.

## 1. 왜 기본 생성자(인자 없는 생성자)가 필요한가

Hibernate가 DB의 행을 읽어 객체로 만들 때 **2단계**로 움직인다.

```
① 빈 객체를 하나 만든다        ← 기본 생성자를 리플렉션으로 호출
② 필드에 값을 하나씩 꽂는다     ← 리플렉션으로 필드에 직접 주입 (setter도 안 거침)
```

여기서 Hibernate는 `Book(String name)` 같은 **내가 만든 생성자를 쓸 수 없다.** 인자가 무엇을 뜻하는지, 어떤 순서로 넣어야 하는지 알 방법이 없기 때문이다. 그래서 JPA 명세가 못 박아 뒀다:

> 엔티티는 **`public` 또는 `protected` 인자 없는 생성자**를 가져야 한다.

그리고 자바에서는 **생성자를 하나라도 직접 정의하면 암묵적 기본 생성자가 사라진다.** `Book(String name)` 을 만든 순간 `Book()` 이 없어지므로 직접 다시 써줘야 한다.

### 곁가지 — 검증 로직은 DB 로딩 때 안 돈다

①→② 순서이므로, DB에서 읽어올 때는 `Book(String name)` 안의 `IllegalArgumentException` 검증이 **실행되지 않는다.** DB에 이미 들어 있는 값은 저장 시점에 검증을 통과한 값이니 다시 검증할 이유가 없다. 의도된 동작이다.

## 2. 왜 하필 `protected`인가 — 양쪽에서 조인 걸린 결과

| 접근제어자 | 결과 |
|---|---|
| `private` | ❌ 지연 로딩 프록시가 `Book`을 **상속**해야 하는데 `super()` 호출이 막힌다 |
| `protected` | ✅ 프록시 OK + 다른 패키지에서 `new Book()` 차단 |
| `public` | ❌ 아무 데서나 `new Book()` 가능 → 검증을 우회하는 뒷문이 열린다 |

- **아래쪽 제약**: `FetchType.LAZY` 일 때 Hibernate는 엔티티를 **상속한 프록시 클래스**를 런타임에 생성한다. 자식 클래스 생성자는 반드시 `super()` 를 호출해야 하므로 `private` 이면 프록시를 못 만든다.
- **위쪽 제약**: `public` 이면 이름 없는 빈 `Book` 을 아무나 만들 수 있다. 팀원이 무심코 쓸 수 있는 게 가장 위험하다.

즉 **"프레임워크는 쓰라고 열어두되, 애플리케이션 코드는 쓰지 마라"** 를 접근제어자로 표현한 것이다.

> 관례적으로 쓰는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)` (롬복) 도 정확히 같은 이유다.

## 3. 자바 접근제어자 — `protected`의 실제 범위

`protected` 는 **"자식 클래스만"이 아니라 "같은 패키지 + 자식 클래스"** 다. `default`(package-private)보다 **넓다.**

| 접근제어자 | 같은 클래스 | 같은 패키지 | 다른 패키지의 자식 | 그 외 |
|---|---|---|---|---|
| `private` | O | X | X | X |
| (default) | O | O | X | X |
| `protected` | O | **O** | O | X |
| `public` | O | O | O | O |

### "다른 패키지의 자식 클래스"란

그냥 상속인데 패키지가 다른 경우다.

```java
// package com.group.library_app.domain.book;
public class Book {
    protected Book() {}
}

// package com.group.library_app.domain.novel;   ← 다른 패키지
public class Novel extends Book {                // ← 자식
    public Novel() {
        super();        // O — protected라서 가능. default였다면 컴파일 에러
    }
}
```

반대로 이건 막힌다 — 우리가 노린 것이 바로 이것:

```java
// package com.group.library_app.service.book;
public class BookService {        // 자식도 아니고 패키지도 다름
    void f() {
        Book b = new Book();      // X 컴파일 에러
    }
}
```

### 제약 — 다른 패키지에서는 "자기 자신을 통해서만"

```java
// 다른 패키지, class Novel extends Book
class Novel extends Book {
    void f(Book other) {
        this.doSomething();        // O — 내 것
        super.doSomething();       // O
        other.doSomething();       // X — 남의 Book 인스턴스로는 불가
    }
}
```

상속받았으니 "내 몸의 일부"는 되지만, "`Book` 타입 전체에 대한 열쇠"를 받은 건 아니다.

### 그래서 완벽한 밀봉은 아니다

`Book` 이 `com.group.library_app.domain.book` 에 있으므로 **같은 패키지 안에서는 `new Book()` 이 그냥 된다.** 지금은 그 패키지에 `Book` · `BookRepository` 뿐이라 실질적 문제가 없지만, 원리상 열려 있다.

**완벽한 차단이 아니라 "의도를 드러내는 관례 + 실수 방지턱"** 으로 보는 게 맞다. `service` · `controller` 에서의 오용은 확실히 막아주므로 실무적 효과는 다 나온다.

## 4. 이 프로젝트에 적용

```java
// src/main/java/com/group/library_app/domain/book/Book.java
@Entity
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id = null;

    @Column(nullable = false, length = 255)
    private String name;

    protected Book() {}                       // ← Hibernate 전용. 애플리케이션 코드는 아래 것을 쓴다

    public Book(String name) {
        if (name == null || name.isBlank()) { // ← DB 로딩 시에는 이 검증이 돌지 않는다
            throw new IllegalArgumentException(String.format("잘못된 name(%s)이 들어왔습니다", name));
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
```

```java
// src/main/java/com/group/library_app/domain/user/loanhistory/UserLoanHistory.java
@Entity
public class UserLoanHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id = null;
    private long userId;
    private String bookName;
    private boolean isReturn;

    protected UserLoanHistory(){}             // ← 같은 이유

    public UserLoanHistory(long userId, String bookName) {
        this.userId = userId;
        this.bookName = bookName;
        this.isReturn = false;
    }
}
```

두 엔티티 모두 **"Hibernate용 생성자"와 "내가 쓸 생성자"를 분리**하고, 전자를 `protected` 로 좁혀 둔 형태다.

## 5. 한 줄 요약

**기본 생성자가 필요한 건 Hibernate가 리플렉션으로 빈 객체를 먼저 만들기 때문이고, `protected`인 건 프록시 상속(`private` 불가)과 오용 방지(`public` 비권장) 사이에서 유일하게 남는 선택지이기 때문이다.**
