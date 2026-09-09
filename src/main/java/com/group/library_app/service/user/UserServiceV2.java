package com.group.library_app.service.user;

import com.group.library_app.domain.user.User;
import com.group.library_app.domain.user.UserRepository;
import com.group.library_app.dto.user.request.UserCreateRequest;
import com.group.library_app.dto.user.response.UserResponse;
import com.group.library_app.repository.user.UserJdbcRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceV2 {
    private final UserRepository userRepository;

    public UserServiceV2(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void saveUser(UserCreateRequest request){
        // save: JpaRepository의 메소드
        userRepository.save(new User(request.getName(),request.getAge()));
    }

    public List<UserResponse> getUsers(){
        List<User> users = userRepository.findAll();
        // stream()과 map과 collect의 기능? 자바 개념 이슈
        return users.stream().map(user -> new UserResponse(user.getId(), user.getName(), user.getAge())).collect(Collectors.toList());
    }
}
