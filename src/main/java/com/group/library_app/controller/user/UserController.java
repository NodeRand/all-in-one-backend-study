package com.group.library_app.controller.user;

import com.group.library_app.domain.user.User;
import com.group.library_app.dto.user.request.UserCreateRequest;
import com.group.library_app.dto.user.request.UserUpdateRequest;
import com.group.library_app.dto.user.response.UserResponse;
import com.group.library_app.service.fruit.FruitService;
import com.group.library_app.service.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@RestController
public class UserController {
    // db 사용 전 코드 // private final List<User> users = new ArrayList<>();
    private final UserService userService;
    private final FruitService fruitService;

    public UserController(UserService userService, @Qualifier("appleService") FruitService fruitService){
        this.userService = userService;
        this.fruitService = fruitService;
    }

    @PostMapping("/user")
    public void saveUser(@RequestBody UserCreateRequest request){
        // db 사용 전 코드 // users.add(new User(request.getName(), request.getAge()));
        userService.saveUser(request);
    }

    @GetMapping("/user")
    public List<UserResponse> getUsers() {
        return userService.getUsers();
    }

    @PutMapping("/user")
    public void updateUser(@RequestBody UserUpdateRequest request){
        userService.updateUser(request);
    }

    @DeleteMapping("/user")
    public void deleteUser(@RequestParam String name){ // 특정 name을 지우겠다
        userService.deleteUser(name);
    }

    @GetMapping("/user/error-test")
    public void errorTest(){
        throw new IllegalArgumentException();
    }
}
