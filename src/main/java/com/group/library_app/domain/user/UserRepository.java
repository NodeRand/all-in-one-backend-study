package com.group.library_app.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// JpaRepository 상속한 것 만으로 빈이 됨
public interface UserRepository extends JpaRepository<User,Long> {
    // 질문: public같은 접근제어자 없이 그냥 함수 뚝딱? 이게 뭐지
    Optional<User> findByName(String name);
//    User findByName(String name);
//    boolean existsByName(String name);
//    long countByAge(Integer age);
}
