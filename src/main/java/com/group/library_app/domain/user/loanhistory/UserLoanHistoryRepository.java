package com.group.library_app.domain.user.loanhistory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserLoanHistoryRepository extends JpaRepository<UserLoanHistory,Long>
{
    // select * from user_loan_history where book_name =? and is_return = ?
    boolean existsByBookNameAndIsReturn(String name, boolean isReturn);
    // 반환 타입 Optional<T>의 의미(= 최대 1건 약속)와 여는 법
    // → docs/optional-and-method-reference.md
    Optional<UserLoanHistory> findByUserIdAndBookName(long userId, String bookName);
}
