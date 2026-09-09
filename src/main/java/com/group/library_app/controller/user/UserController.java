package com.group.library_app.controller.user;

import com.group.library_app.dto.user.request.UserCreateRequest;
import com.group.library_app.dto.user.request.UserUpdateRequest;
import com.group.library_app.dto.user.response.UserResponse;
import com.group.library_app.service.fruit.FruitService;
import com.group.library_app.service.user.UserServiceV1;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {
    // db 사용 전 코드 // private final List<User> users = new ArrayList<>();
    private final UserServiceV1 userServiceV1;
    private final FruitService fruitService;

    public UserController(UserServiceV1 userServiceV1, @Qualifier("main") FruitService fruitService){
        this.userServiceV1 = userServiceV1;
        this.fruitService = fruitService;
    }

    @PostMapping("/user")
    public void saveUser(@RequestBody UserCreateRequest request){
        // db 사용 전 코드 // users.add(new User(request.getName(), request.getAge()));
        userServiceV1.saveUser(request);
    }

    @GetMapping("/user")
    public List<UserResponse> getUsers() {
        return userServiceV1.getUsers();
    }

    @PutMapping("/user")
    public void updateUser(@RequestBody UserUpdateRequest request){
        userServiceV1.updateUser(request);
    }

    @DeleteMapping("/user")
    public void deleteUser(@RequestParam String name){ // 특정 name을 지우겠다
        userServiceV1.deleteUser(name);
    }

    @GetMapping("/user/error-test")
    public void errorTest(){
        throw new IllegalArgumentException();
    }
}
