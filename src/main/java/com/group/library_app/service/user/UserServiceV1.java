package com.group.library_app.service.user;

import com.group.library_app.dto.user.request.UserCreateRequest;
import com.group.library_app.dto.user.request.UserUpdateRequest;
import com.group.library_app.dto.user.response.UserResponse;
import com.group.library_app.repository.user.UserJdbcRepository;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class UserServiceV1 {
    private final UserJdbcRepository userJdbcRepository;

    public UserServiceV1(UserJdbcRepository userJdbcRepository){
        this.userJdbcRepository = userJdbcRepository;
    }

    // 얘는 @RequestBody가 필요 없음 -> 컨트롤러가 객체로 변환해준 걸 받기만 함 (UserController에 있던 함수의 역할 분리)
    public void updateUser(UserUpdateRequest request){
        if(userJdbcRepository.isUserNotExist(request.getId())){
            throw new IllegalArgumentException();
        }
       userJdbcRepository.updateUserName(request.getName(), request.getId());
    }

    public void deleteUser(String name){
        if(userJdbcRepository.isUserNotExist(name)){
            throw new IllegalArgumentException();
        }
        userJdbcRepository.deleteUsername(name);
    }

    public void saveUser(UserCreateRequest request){
        userJdbcRepository.saveUser(request.getName(),request.getAge());
    }

    public List<UserResponse> getUsers(){
        return userJdbcRepository.getUsers();
    }
}
