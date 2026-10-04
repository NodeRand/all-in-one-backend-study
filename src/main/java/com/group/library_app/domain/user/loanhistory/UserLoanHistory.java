package com.group.library_app.domain.user.loanhistory;

import com.group.library_app.domain.user.User;
import jakarta.persistence.*;

@Entity
public class UserLoanHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id = null;

    // 한 명의 User한테 대출기록은 여러 개
    @ManyToOne
    @JoinColumn(nullable = false)
    private User user;


    private String bookName;

    // boolean으로 처리하면, tinyint에 잘 매핑됨
    private boolean isReturn;

    // JPA 기본 생성자 → docs/jpa-protected-no-arg-constructor.md
    protected UserLoanHistory(){}

    public UserLoanHistory(User user, String bookName) {
        this.user = user;
        this.bookName = bookName;
        this.isReturn = false; // 어차피 초기값은 false
    }

    // 이런 단순 작업(엔티티 필드값 변경) util은 엔티티 내부에서 제작
    public void doReturn(){
        this.isReturn=true;
    }

    public String getBookName() {
        return this.bookName;
    }

    public boolean isReturn() {
        return this.isReturn;
    }
}
