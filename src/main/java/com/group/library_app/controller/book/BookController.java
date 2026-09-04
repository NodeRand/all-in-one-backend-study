package com.group.library_app.controller.book;

import com.group.library_app.service.book.BookService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookController {
    private final BookService bookService = new BookService();
    @PostMapping("/book")
    public void saveBook(){
        bookService.saveBook();
    }
}
