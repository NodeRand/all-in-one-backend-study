package com.group.library_app.service.book;

import com.group.library_app.domain.book.Book;
import com.group.library_app.domain.book.BookRepository;
import com.group.library_app.dto.book.request.BookCreateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookService {
    // private final BookMemoryRepository bookRepository = new BookMemoryRepository();
    // private final BookRepository bookRepository = new BookMemoryRepository();
    // 인터페이스를 쓰면 그냥 우변만 바꾸면 됨
    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Transactional
    public void saveBook(BookCreateRequest request){
        bookRepository.save(new Book(request.getName()));
    }
}

