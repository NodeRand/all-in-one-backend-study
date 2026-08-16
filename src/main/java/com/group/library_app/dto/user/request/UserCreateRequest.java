package com.group.library_app.dto.user.request;

public class UserCreateRequest {
    private String name;
    private Integer age; // 그냥 int가 아닌 이유 : null 표현 여부

    public String getName() {
        return name;
    }

    public Integer getAge() {
        return age;
    }
}
