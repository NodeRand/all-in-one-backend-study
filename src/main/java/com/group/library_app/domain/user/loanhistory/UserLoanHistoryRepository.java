package com.group.library_app.domain.user.loanhistory;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLoanHistoryRepository extends JpaRepository<UserLoanHistory,Long>
{
    // select * from user_loan_history where book_name =? and is_return = ?
    boolean existsByBookNameAndIsReturn(String name, boolean isReturn);
}
