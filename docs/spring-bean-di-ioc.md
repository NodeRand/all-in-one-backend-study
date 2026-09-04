# Spring 빈 · DI · IoC 정리

> library-app 실습 중 정리. `UserController` → `UserService` → `UserRepository` 리팩터링 맥락.

## 빈 (Bean)

**빈 = Spring 컨테이너(ApplicationContext)가 대신 `new` 해서 대신 보관해주는 객체.**

특별한 타입도, 상속해야 할 인터페이스도 없다. 그냥 객체다. 컨테이너의 관리 대상이 되면 빈, 내가 직접 `new` 하면 그냥 객체.

컨테이너가 "관리한다"는 것의 실체:

| 하는 일 | 내용 |
|---|---|
| 객체 생성 | `new UserController(...)`를 대신 실행 |
| 의존성 주입 | 생성자가 요구한 타입을 찾아서 넣어줌 |
| 싱글톤 보관 | 인스턴스를 1개만 만들어 계속 재사용 |
| 이름 등록 | `"userController"`로 컨테이너에 저장 |
| 생명주기 | 앱 종료 시 정리 작업 실행 |

## 빈이 되는 방법 2가지

### ① `@Component` 계열 + 컴포넌트 스캔

```java
@RestController   // = @Component + @ResponseBody
@Service          // = @Component
@Repository       // = @Component
@Component        // 원형
```

전부 `@Component`의 변종. 기능은 사실상 같고 **이름으로 역할을 표시**하는 용도.

`@SpringBootApplication` 안에 `@ComponentScan`이 들어 있어서, **그 클래스가 있는 패키지와 하위 전체**를 훑어 발견한다.

### ② `@Configuration` + `@Bean` 메서드의 리턴값

```java
@Configuration
public class MyConfig {
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);   // 이 리턴 객체가 빈이 됨
    }
}
```

`@Component`를 붙일 수 없는 **남의 라이브러리 클래스**를 빈으로 만들 때. `JdbcTemplate`이 이 경우 — Spring Boot의 auto-configuration이 위와 거의 똑같은 코드로 만들어준다.

## DI (의존성 주입)

**생성자 파라미터에 쓴 타입이 곧 주문서.**

```java
public UserController(JdbcTemplate jdbcTemplate){ ... }
//                    ^^^^^^^^^^^^ "나 이 타입 하나 필요해" 라는 선언
```

값이 미리 박혀 있는 게 아니라 **빈 슬롯**이다. Spring이 리플렉션으로 이 타입을 읽고, 컨테이너에서 맞는 객체를 찾아 채운 뒤 생성자를 실제로 호출한다.

- **타입으로 매칭한다.** 변수명은 안 본다 (같은 타입 빈이 2개 이상일 때만 이름이 tiebreaker)
- 생성자가 1개뿐이면 `@Autowired` 생략 가능 (Spring 4.3+)
- `import`는 주입과 무관 — 컴파일러에게 이름 위치를 알려주는 것뿐

### 주입되는 3가지 조건

1. **그 타입의 빈이 등록돼 있어야 함** — 없으면 부팅 실패
   `required a bean of type '...' that could not be found`
   (정확히 그 타입이거나 하위 타입/구현체면 OK. 인터페이스로 선언하고 구현체를 주입받는 게 흔한 형태)
2. **딱 1개여야 함** — 2개면 `NoUniqueBeanDefinitionException` → `@Primary` / `@Qualifier`로 지목
3. **그 클래스 자신도 빈이어야 함** ← 놓치기 쉬움
   Spring은 자기가 만드는 객체의 생성자만 채운다. 빈이 아니면 생성자를 부를 기회조차 없다.

> **클래스가 존재하는 것 ≠ 빈이 등록된 것.** import도 되고 컴파일도 되는데 주입이 안 되면 대부분 이 착각.

## IoC (제어의 역전)

두 가지가 뒤집혀 있다.

### ① 생성 순서 — 안쪽 의존성부터

```
DataSource  →  JdbcTemplate  →  UserController
(제일 먼저)                      (제일 마지막)
```

손으로 짜면 "컨트롤러 만들자 → 아 JdbcTemplate 필요하네" 식으로 거슬러 올라가지만, Spring은 의존성 그래프를 먼저 파악해두고 **아무것도 의존하지 않는 것부터** 만들어 위로 넘긴다. 그래서 `UserController`가 만들어지는 시점엔 `JdbcTemplate`이 이미 완성돼 대기 중.

> 곁가지: A↔B 순환 의존은 어느 쪽도 먼저 못 만들어서 부팅 실패 (`circular reference`). 구조적으로 예방된다.

### ② 호출 방향 — 프레임워크가 내 코드를 부른다

```
일반 라이브러리:  내 코드   →  라이브러리 호출   (내가 주도)
프레임워크:      프레임워크  →  내 코드 호출     (프레임워크가 주도)
```

나는 "이런 클래스가 있고, 이런 게 필요하고, `PUT /user`가 오면 이걸 실행해줘"라고 **선언만** 한다. 실행은 Spring이 한다. (헐리우드 원칙: *"먼저 연락하지 마세요, 저희가 연락드립니다."*)

프론트엔드에서 이미 하고 있는 것과 같다 — `useEffect` 콜백을 내가 호출하지 않고, `<Route>`를 선언해두면 라우터가 렌더한다.

### 생성자는 앱 시작 시 딱 1번

```
앱 시작        → new UserController(jdbcTemplate)  ✅ 평생 1회
                → 인스턴스 1개를 컨테이너에 보관

PUT /user 요청 → 보관된 그 인스턴스의 updateUser()   (생성자 X)
GET /user 요청 → 같은 인스턴스의 getUsers()          (생성자 X)
```

요청이 만 번 와도 생성자는 다시 호출되지 않는다. **모든 요청이 같은 객체를 공유**하므로 요청마다 변하는 상태를 필드에 담으면 안 된다. 필드는 `final`로.

## 빈으로 만드는 것 / 안 만드는 것

| | 예 | 이유 |
|---|---|---|
| ✅ 빈 | Controller / Service / Repository | 기능·역할 담당, 상태 없음 → 1개 공유해도 안전 |
| ❌ 빈 아님 | `User`, `UserCreateRequest` | 데이터 담는 객체. 요청마다 새로 `new` 해야 함 |

DTO/도메인 객체를 빈으로 만들면 A의 데이터가 B에게 보이는 사고가 난다. 생성자에 `User` 타입을 넣었을 때 부팅이 실패하는 건 버그가 아니라 정상.

## 이 프로젝트에 적용

### 지금 (DI 없음)

```java
@RestController
public class UserController {
    private final UserService userService;

    public UserController(JdbcTemplate jdbcTemplate){   // JdbcTemplate은 주입받지만
        this.userService = new UserService(jdbcTemplate); // 서비스는 손으로 new
    }
}

public class UserService {                              // @Service 없음 → 빈 아님
    private final UserRepository userRepository;

    public UserService(JdbcTemplate jdbcTemplate){       // 같은 형태인데 자동 주입 안 됨
        userRepository = new UserRepository(jdbcTemplate); // 리포지토리도 손으로 new
    }
}
```

문제:
- 컨트롤러가 서비스의 생성 방법과 그 의존성까지 알고 있다 → 서비스 생성자가 바뀌면 컨트롤러도 고쳐야 함
- `jdbcTemplate`을 계층 아래로 계속 손으로 전달 → 클래스가 늘면 `new` 사슬이 지옥
- 실제로 이 구조 때문에 `jdbcTemplate`을 메서드 인자로까지 넘기려다 컴파일 에러가 났다

**같은 프로젝트 안에 자동 주입되는 경우(`UserController`)와 안 되는 경우(`UserService`)가 나란히 있고, 차이는 애노테이션 한 줄.**

### 다음 (DI 적용)

```java
@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;
    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}

@Service
public class UserService {
    private final UserRepository userRepository;
    public UserService(UserRepository userRepository) {   // JdbcTemplate 말고 리포지토리를 받음
        this.userRepository = userRepository;
    }
}

@RestController
public class UserController {
    private final UserService userService;
    public UserController(UserService userService) {      // JdbcTemplate이 아예 안 보임
        this.userService = userService;
    }
}
```

`JdbcTemplate → UserRepository → UserService → UserController` 순서로 Spring이 알아서 조립한다. 각 클래스는 "나는 무엇이 필요하다"만 생성자로 선언하면 끝이고, 컨트롤러는 `JdbcTemplate`의 존재를 알 필요조차 없어진다.

## JdbcTemplate이 만들어지는 경로

```
build.gradle: spring-boot-starter-data-jpa      ← spring-jdbc를 끌고 옴
        ↓
application.yml: spring.datasource.url / username / password
        ↓
DataSourceAutoConfiguration   →  DataSource 빈 (HikariCP 커넥션 풀)
        ↓
JdbcTemplateAutoConfiguration →  JdbcTemplate 빈
        ↓
UserController 생성자에 주입 ✅
```

`spring.datasource` 설정이 없으면 `DataSource`가 안 만들어지고, 연쇄적으로 `JdbcTemplate`도 없어서 부팅 실패.

## 등록된 빈 직접 확인하기

```java
public static void main(String[] args) {
    ConfigurableApplicationContext ctx = SpringApplication.run(LibraryAppApplication.class, args);
    Arrays.stream(ctx.getBeanDefinitionNames())
          .filter(name -> name.toLowerCase().contains("user") || name.contains("jdbc"))
          .forEach(System.out::println);
}
```

`userController`, `jdbcTemplate`, `dataSource`는 있고 `userService`는 없는 것을 눈으로 확인할 수 있다.
