package com.group.library_app.service.user;

import com.group.library_app.dto.user.request.UserCreateRequest;
import com.group.library_app.dto.user.request.UserUpdateRequest;
import com.group.library_app.dto.user.response.UserResponse;
import com.group.library_app.repository.user.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class UserService {
    private final UserRepository userRepository;

    public UserService(JdbcTemplate jdbcTemplate){
        userRepository = new UserRepository(jdbcTemplate);
    }

    // 얘는 @RequestBody가 필요 없음 -> 컨트롤러가 객체로 변환해준 걸 받기만 함 (UserController에 있던 함수의 역할 분리)
    public void updateUser(UserUpdateRequest request){
        if(userRepository.isUserNotExist(request.getId())){
            throw new IllegalArgumentException();
        }
       userRepository.updateUserName(request.getName(), request.getId());
    }

    public void deleteUser(String name){
        if(userRepository.isUserNotExist(name)){
            throw new IllegalArgumentException();
        }
        userRepository.deleteUsername(name);
    }

    public void saveUser(UserCreateRequest request){
        userRepository.saveUser(request.getName(),request.getAge());
    }

    public List<UserResponse> getUsers(){
        return userRepository.getUsers();
    }
}
