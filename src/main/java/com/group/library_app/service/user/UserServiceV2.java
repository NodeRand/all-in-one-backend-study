package com.group.library_app.service.user;

import com.group.library_app.domain.user.User;
import com.group.library_app.domain.user.UserRepository;
import com.group.library_app.dto.user.request.UserCreateRequest;
import com.group.library_app.dto.user.request.UserUpdateRequest;
import com.group.library_app.dto.user.response.UserResponse;
import com.group.library_app.repository.user.UserJdbcRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceV2 {
    private final UserRepository userRepository;

    public UserServiceV2(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 아래 있는 함수가 시작될 때 start transaction;을 해준다 (트랜잭션을 시작!)
    // 함수가 예외 없이 잘 끝났다면 commit
    // 혹시라도 문제가 있다면 rollback
    // @Transactional // org.springframework꺼로 사용 (jakarta꺼 x)
    public void saveUser(UserCreateRequest request){
        // save: JpaRepository의 메소드
        userRepository.save(new User(request.getName(),request.getAge()));
        throw new IllegalArgumentException();
    }
    @Transactional(readOnly = true) // readOnly=true -> 약간의 성능적 이점
    public List<UserResponse> getUsers(){
        List<User> users = userRepository.findAll();
        // stream()과 map과 collect의 기능? 자바 개념 이슈
        // return users.stream().map(user -> new UserResponse(user.getId(), user.getName(), user.getAge())).collect(Collectors.toList());
        // UserResponse::new의 문법
        return users.stream().map(UserResponse::new).collect(Collectors.toList());
    }

    @Transactional
    public void updateUser(UserUpdateRequest request){
        // SELECT * FROM user WHERE id = ?;
        // Optional<User> -> 이게 뭐지
        User user = userRepository.findById(request.getId()).orElseThrow(IllegalArgumentException::new);
        user.updateName(request.getName());
        userRepository.save(user);
    }
    @Transactional
    // 질문: 우리 서비스 실무에서는 삭제도 id기준으로 찾는데 여기서는 왜 이름을 기준으로 찾는 건지?
    public void deleteUser(String name){
        // SELECT * FROM user WHERE name = ?;
        User user = userRepository.findByName(name).orElseThrow(IllegalArgumentException::new);

//        if(!userRepository.existsByName(name)){
//            throw new IllegalArgumentException();
//        }
//        // 위에서 검증을 끝냈으니 무조건 해당 user가 있다고 가정하고 삭제
//        User user = userRepository.findByName(name);
        userRepository.delete(user);
    }
}
