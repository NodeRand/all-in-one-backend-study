package com.group.library_app.domain.book;

import jakarta.persistence.*;

@Entity
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id = null;

    @Column(nullable = false, length = 255)
    private String name;

    // 질문: JPA를 위한 기본 생성자..protected를 왜 걸어주더라
    protected Book() {}

    public Book(String name) {
        if (name == null || name.isBlank()) {
            // 질문: String.format이 무슨 문법?
            throw new IllegalArgumentException(String.format("잘못된 name(%s)이 들어왔습니다", name));
        }
        this.name = name;
    }
}
