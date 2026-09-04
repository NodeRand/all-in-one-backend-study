package com.group.library_app.service.book;

import com.group.library_app.repository.book.BookMemoryRepository;
import com.group.library_app.repository.book.BookMySqlRepository;
import com.group.library_app.repository.book.BookRepository;

public class BookService {
    // private final BookMemoryRepository bookRepository = new BookMemoryRepository();
    // private final BookRepository bookRepository = new BookMemoryRepository();
    // 인터페이스를 쓰면 그냥 우변만 바꾸면 됨
    private final BookRepository bookRepository = new BookMySqlRepository();
    public void saveBook(){
        bookRepository.saveBook();
    }
}

