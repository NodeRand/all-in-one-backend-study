package com.group.library_app.domain.user;

import com.group.library_app.domain.user.loanhistory.UserLoanHistory;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id = null;

    @Column(nullable = false, length = 20, name="name")
    // name varchar(20), 이 때 필드명과 칼럼명이 동일하다면 name="name" 부분 생략 가능
    private String name;
    // age는 name과 달리 20자 제한이나 nullable 제한이 없기 때문에 @Column 생략 가능
    private Integer age;

    // user와의 1:N
    // user와의 관계에서 userLoanHistory가 연관관계의 주인
    // 질문: 테이블간 인덱스를 누가 갖고있느냐에 따라 주인이 되는 것인가? 근데 양방향 인덱스는 또 어떻게하는 지,
    //  주로 삭제 이슈에서 터지는 걸로 알고 있음. user가 지워지면 userLoanHistory들은?
    // 다음 단원에서 확인된 답변: N:1의 연관관계에서는 user쪽에서
    //  아래의 어노테이션+userLoanHistory코드 자체를 안 씀으로서 단방향으로만 설정이 가능
    //  cascade: 하나 지울 때 관련 기록 지우기에 대한 답변
    //  orpanRemoval: 엔티티에서 지운걸 실제 DB에 반영하기 위함
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserLoanHistory> userLoanHistories = new ArrayList<>();

    protected User(){}

    public User(String name, Integer age) {
        if(name == null || name.isBlank())
            throw new IllegalArgumentException(String.format("잘못된 name(%s)이 들어왔습니다.",name));
        this.name = name;
        this.age = age;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getAge() {
        return age;
    }

    public void updateName(String name){
        this.name = name;
    }
    public void loanBook(String bookName){
        // cmd+p : 함수에 필요한 인자 설명 단축키
        this.userLoanHistories.add(new UserLoanHistory(this, bookName));
    }

    public void returnBook(String bookName){
        // 질문: stream이 뭐였더라 자바 문법 기본기 이슈
        UserLoanHistory targetHistory = this.userLoanHistories.stream()
                .filter(history->history.getBookName().equals(bookName))
                // 같은 책을 빌림→반납→재대출하면 같은 bookName 기록이 여러 개 쌓임 (예: 반납된 id 2, 대출 중 id 23)
                // 이름만으로 findFirst()하면 앞쪽의 이미 반납된 기록이 잡혀 doReturn()이 true→true로 아무 변화 없이 끝나고,
                // 예외도 없어서 "반납 완료"로 응답하지만 실제 대출 중인 기록은 영원히 0으로 남음
                // loanBook에서 미반납 중복 대출을 막으므로 "이 책 + 미반납" 기록은 최대 1개 → 이 필터로 정확히 그 하나만 남김
                // (순서에 기대서 마지막 기록을 고르는 방식은 JPA가 List 순서를 보장하지 않아 위험)
                .filter(history->!history.isReturn())
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
        targetHistory.doReturn();
    }
}
