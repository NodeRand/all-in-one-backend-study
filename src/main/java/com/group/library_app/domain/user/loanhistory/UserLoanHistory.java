package com.group.library_app.domain.user.loanhistory;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class UserLoanHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id = null;
    private long userId;
    private String bookName;

    // boolean으로 처리하면, tinyint에 잘 매핑됨
    private boolean isReturn;

    // JPA 기본 생성자 → docs/jpa-protected-no-arg-constructor.md
    protected UserLoanHistory(){}

    public UserLoanHistory(long userId, String bookName) {
        this.userId = userId;
        this.bookName = bookName;
        this.isReturn = false; // 어차피 초기값은 false
    }

    // 이런 단순 작업(엔티티 필드값 변경) util은 엔티티 내부에서 제작
    public void doReturn(){
        this.isReturn=true;
    }
}
