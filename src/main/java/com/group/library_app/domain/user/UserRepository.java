package com.group.library_app.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

// JpaRepository 상속한 것 만으로 빈이 됨
public interface UserRepository extends JpaRepository<User,Long> {

}
