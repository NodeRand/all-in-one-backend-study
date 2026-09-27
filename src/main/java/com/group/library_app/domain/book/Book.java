package com.group.library_app.domain.book;

import jakarta.persistence.*;

@Entity
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id = null;

    @Column(nullable = false, length = 255)
    private String name;

    // JPA 기본 생성자. protected인 이유(리플렉션 생성 · 프록시 상속 · 오용 방지)
    // → docs/jpa-protected-no-arg-constructor.md
    protected Book() {}

    public Book(String name) {
        if (name == null || name.isBlank()) {
            // 질문: String.format이 무슨 문법?
            throw new IllegalArgumentException(String.format("잘못된 name(%s)이 들어왔습니다", name));
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
