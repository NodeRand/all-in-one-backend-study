# `Optional` 과 메서드 레퍼런스(`::`) 정리

> library-app 실습 중 정리. `findByName(...).orElseThrow(IllegalArgumentException::new)` 한 줄을 뜯어본 결과.

## 1. `Optional`은 배열이 아니다

가장 헷갈렸던 지점. `Optional` 의 속은 이렇게 생겼다.

```java
public final class Optional<T> {
    private static final Optional<?> EMPTY = new Optional<>(null);
    private final T value;                 // ← 필드 딱 하나. null 이거나, 객체 하나.

    private Optional(T value) {            // private! 직접 new 불가
        this.value = value;
    }

    public static <T> Optional<T> of(T value) {
        return new Optional<>(Objects.requireNonNull(value));   // 매번 새로 생성
    }

    public static <T> Optional<T> empty() {
        return (Optional<T>) EMPTY;        // 생성 안 함. 미리 만들어둔 것 재사용
    }
}
```

**배열도 리스트도 아니고, 참조 하나를 감싼 평범한 객체다.** 길이도 인덱스도 없다.

```java
Optional<Book> opt = ...;
opt[0]        // X — 배열이 아님
opt.size()    // X — 그런 메서드 없음
opt.get()     // O — 감싼 값 꺼내기
```

### 그릇 세 가지를 구분

```java
UserLoanHistory           history   // 객체 1개 (또는 null)
Optional<UserLoanHistory> opt       // 0개 또는 1개를 담는 "상자"
List<UserLoanHistory>     list      // 0개 이상을 담는 "목록"
```

`<>` 안이 **내용물의 타입**, 바깥이 **담는 그릇**이다.

| | `Optional<T>` | `List<T>` |
|---|---|---|
| 담는 개수 | 0 또는 1 | 0 이상 |
| 내부 구조 | 참조 1칸 (`T value`) | 진짜 목록 |
| 인덱스 | 없음 | 있음 |

> 참고: `JdbcTemplate.query()` 는 결과가 0건이든 1건이든 **항상 `List`** 를 반환한다(`UserJdbcRepository` 의 `[0]` / `[]` 주석이 그 얘기). Spring Data JPA는 다르게, **반환 타입으로 "몇 건 나올지"를 내가 선언**하면 거기 맞춰준다.

## 2. 존재하는 상태는 둘뿐 — 껍데기와 알맹이

```
찾았을 때  : Optional 객체 { value = Book@1a2b }
못 찾았을 때: Optional 객체 { value = null }
             └─ Optional 자리에 null이 오는 게 아니라, 껍데기는 멀쩡히 있다
```

**`Optional` 객체 자체는 항상 존재한다.** 그래서 `findByName(...)` 결과에 바로 `.orElseThrow(...)` 를 점 찍어 이어붙여도 NPE가 나지 않는다.

생성자 관점에서 보면 층이 둘이다:

| | 껍데기 (`Optional`) | 알맹이 (`Book`) |
|---|---|---|
| 책 찾음 | `new Optional<>(book)` — 생성됨 | Hibernate가 `Book` 생성 |
| 책 없음 | `EMPTY` 재사용 — **생성 안 됨** | **아예 안 만들어짐** |

비어 있는 `Optional` 은 상태가 `value = null` 하나뿐이라 서로 구별할 필요가 없다. 그래서 매번 만들지 않고 싱글턴을 공유한다. 생성자가 `private` 인 것도 이런 최적화를 내부에서 자유롭게 하기 위해서다.

## 3. 왜 쓰나 — null을 껍데기 안에 가둔다

```java
// 포장지 없으면 — null이 밖에 노출됨
Book book = bookRepository.findByName(name);
book.getName();      // book이 null이면 여기서 NPE. 컴파일러는 침묵.

// 포장지 있으면 — null이 안에 갇힘
Optional<Book> opt = bookRepository.findByName(name);
opt.getName();       // 컴파일 에러! Optional엔 그런 메서드 없음
                     // → 꺼내려면 orElseThrow/orElse를 반드시 거쳐야 함
```

null을 없앤 게 아니라 **껍데기 안으로 밀어 넣어 바깥에서 안 보이게** 만든 것이다. 결과적으로 컴파일러가 *"없을 때 어떻게 할지 안 정했잖아"* 를 강제한다.

## 4. 상자 여는 방법

```java
.orElseThrow(...)      // 없으면 예외          → T
.orElse(기본값)         // 없으면 기본값        → T
.orElseGet(() -> ...)  // 없으면 계산해서 반환  → T
.isPresent()           // 있는지만 확인        → boolean
.get()                 // 그냥 꺼냄 (비었으면 NoSuchElementException) → T
```

`.get()` 만 피하면 된다. 비었을 때 터질 거면 `Optional` 을 쓴 의미가 없어진다.

### 좌변 타입이 바뀌는 이유

좌변은 **"메서드의 반환 타입"이 아니라 "표현식 전체의 타입"** 과 맞춰야 한다. 체인의 **마지막 호출**이 타입을 결정한다.

```java
repo.findByUserIdAndBookName(...)                    // → Optional<UserLoanHistory>
repo.findByUserIdAndBookName(...).orElseThrow(...)   // → UserLoanHistory
                                  └─ 여기서 타입이 바뀜
```

`orElseThrow` 의 시그니처가 `T` 를 반환하기 때문이다.

```java
public <X extends Throwable> T orElseThrow(Supplier<? extends X> supplier) throws X
//                            ↑ Optional<T>에서 알맹이 T를 반환
```

**리포지토리 선언은 그대로 `Optional<UserLoanHistory>` 다.** 바뀐 건 서비스 쪽 표현식뿐. 이게 `Optional` 의 설계 의도이기도 하다 — "없을 수도 있다"는 리포지토리가 타입으로 알리고, 어떻게 처리할지(예외/기본값/무시)는 호출하는 쪽이 정한다.

## 5. `IllegalArgumentException::new` — 메서드 레퍼런스

`orElseThrow` 가 받는 건 예외 **객체**가 아니라 예외를 **만들 줄 아는 것**이다.

### ① `Supplier` — 함수형 인터페이스

```java
public interface Supplier<T> {
    T get();       // 받는 것 없음, T 하나 반환
}
```

메서드가 하나뿐인 인터페이스를 **함수형 인터페이스**라 하고, 이런 것만 람다로 쓸 수 있다.

### ② 같은 코드의 세 가지 표기 — 전부 동일한 동작

```java
// (a) 익명 클래스 — 자바 7 시절
.orElseThrow(new Supplier<IllegalArgumentException>() {
    @Override
    public IllegalArgumentException get() {
        return new IllegalArgumentException();
    }
});

// (b) 람다 — 자바 8+
.orElseThrow(() -> new IllegalArgumentException());

// (c) 메서드 레퍼런스 — 가장 짧은 형태
.orElseThrow(IllegalArgumentException::new);
```

(b) → (c) 규칙: **"받은 인자를 그대로 넘기기만 하는 람다"는 `::` 로 줄일 수 있다.** 여기선 인자가 없고 `new` 만 하니 `IllegalArgumentException::new` 로 끝난다.

### ③ `::` 의 네 가지 형태

| 표기 | 의미 | 람다로 풀면 |
|---|---|---|
| `Type::new` | 생성자 | `() -> new Type()` |
| `Type::staticMethod` | 정적 메서드 | `x -> Type.staticMethod(x)` |
| `instance::method` | 특정 객체의 메서드 | `x -> instance.method(x)` |
| `Type::instanceMethod` | 받은 객체의 메서드 | `x -> x.instanceMethod()` |

예: `.map(Book::getName)` = `.map(book -> book.getName())` (네 번째 형태)

### ④ 왜 객체가 아니라 `Supplier`로 받나 — 지연 평가

객체를 직접 받는 구조였다면:

```java
.orElseThrow(new IllegalArgumentException())   // 가상의 설계
```

인자는 먼저 평가되므로 **값이 있어서 예외를 안 던질 때도 예외 객체가 무조건 생성된다.** 예외 생성은 스택트레이스를 통째로 캡처하는 작업이라 비싸다.

`Supplier` 로 받으면 "만드는 방법"만 넘겨두고 **정말 비었을 때만 `get()` 을 호출**한다. 이것이 지연 평가(lazy evaluation).

### ⑤ 메시지를 넣으려면 람다로 풀어야 한다

```java
.orElseThrow(IllegalArgumentException::new)                      // O — 인자 없음
.orElseThrow(() -> new IllegalArgumentException("책이 없어요"))    // O — 람다로
.orElseThrow(IllegalArgumentException::new("책이 없어요"))         // X — 그런 문법 없음
```

`::` 는 "이 메서드를 참조한다"일 뿐, 인자를 미리 끼워 넣는 문법이 없다.

## 6. 이 프로젝트에 적용

```java
// src/main/java/com/group/library_app/service/book/BookService.java
@Transactional
public void loanBook(BookLoanRequest request){
    //1. 책 정보를 가져온다.
    Book book = bookRepository.findByName(request.getBookName()).orElseThrow(IllegalArgumentException::new);
    //         ↑ Optional<Book> 을 열어 Book 으로

    //2~3. 대출 중인지 확인
    if(userLoanHistoryRepository.existsByBookNameAndIsReturn(book.getName(), false)){
        throw new IllegalArgumentException("이미 대출 중인 책입니다.");
    }

    //4. 유저 정보를 가져온다.
    User user = userRepository.findByName(request.getUserName()).orElseThrow(IllegalArgumentException::new);

    //5. UserLoanHistory 저장
    userLoanHistoryRepository.save(new UserLoanHistory(user.getId(), book.getName()));
}
```

### 실제로 겪은 것 — 메시지 없는 예외의 대가

`POST /book/loan` 요청에서 이 스택트레이스가 떴다.

```
java.lang.IllegalArgumentException
    at java.base/java.util.Optional.orElseThrow(Optional.java:403)
    at com.group.library_app.service.book.BookService.loanBook(BookService.java:39)
```

- **원인**: 문법 오류가 아니었다. SQL(`select ... from book where name=?`)은 정상적으로 나갔고 결과가 0건이었을 뿐. `book` 테이블에 `클린 코드` 한 권뿐인데 다른 이름을 보냈다.
- **`user_loan_history` 가 비어 있던 것도 결과지 원인이 아니다.** 39번 줄에서 죽었으니 저장하는 51번 줄까지 간 적이 없고, `@Transactional` 이라 롤백까지 된다.
- **진짜 문제**: `IllegalArgumentException::new` 는 메시지가 비어 있어서 로그가 아무것도 알려주지 않는다. `loanBook` 안에만 같은 패턴이 두 군데(책 39행, 유저 48행)라, 줄 번호가 없었으면 어느 쪽인지도 몰랐을 것.

개선 형태:

```java
Book book = bookRepository.findByName(request.getBookName())
        .orElseThrow(() -> new IllegalArgumentException(
                String.format("존재하지 않는 책입니다. name=%s", request.getBookName())));
```

메서드 레퍼런스에서 람다로 바뀐 이유는 §5-⑤ 그대로 — 인자를 넘겨야 하기 때문.

### 곁가지 — `Optional` 로 선언하면 "최대 1건"을 약속하는 것

```java
// UserLoanHistoryRepository.java
Optional<UserLoanHistory> findByUserIdAndBookName(long userId, String bookName);
```

이 선언은 **"이 조건으로는 최대 1건만 나온다"** 는 약속이다. 2건 이상이면 `IncorrectResultSizeDataAccessException` 이 터진다.

그런데 이 조건은 반납 여부를 안 따지므로, 같은 사람이 같은 책을 빌렸다 반납하고 또 빌리면 행이 2개가 되어 깨진다. 반납 로직이라면 조건을 하나 더 거는 편이 안전하다:

```java
Optional<UserLoanHistory> findByUserIdAndBookNameAndIsReturn(long userId, String bookName, boolean isReturn);
```

## 7. 한 줄 요약

**`Optional` 은 참조 1칸을 감싼 객체이고(배열 아님), `orElseThrow` 는 그 껍데기를 열어 알맹이 타입으로 바꾸는 동작이며, `IllegalArgumentException::new` 는 "예외를 만드는 방법"을 지연 평가용으로 넘기는 `Supplier` 다.**
