package com.group.library_app.domain.user;

import jakarta.persistence.*;

@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id = null;

    @Column(nullable = false, length = 20, name="name")
    // name varchar(20), 이 때 필드명과 칼럼명이 동일하다면 name="name" 부분 생략 가능
    private String name;
    // age는 name과 달리 20자 제한이나 nullable 제한이 없기 때문에 @Column 생략 가능
    private Integer age;

    protected User(){}

    public User(String name, Integer age) {
        if(name == null || name.isBlank())
            throw new IllegalArgumentException(String.format("잘못된 name(%s)이 들어왔습니다.",name));
        this.name = name;
        this.age = age;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getAge() {
        return age;
    }

    public void updateName(String name){
        this.name = name;
    }
}
