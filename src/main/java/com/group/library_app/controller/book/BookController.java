package com.group.library_app.controller.book;

import com.group.library_app.dto.book.request.BookCreateRequest;
import com.group.library_app.dto.book.request.BookLoanRequest;
import com.group.library_app.dto.book.request.BookReturnRequest;
import com.group.library_app.service.book.BookService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookController {
    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping("/book")
    public void saveBook(@RequestBody BookCreateRequest request){
        bookService.saveBook(request);
    }

    @PostMapping("/book/loan")
    public void loanBook(@RequestBody BookLoanRequest request){
        bookService.loanBook(request);
    }

    @PutMapping("/book/return")   // 프론트(static/v1)가 PUT으로 보낸다
    public void returnBook(@RequestBody BookReturnRequest request){
        bookService.returnBook(request);
    }
}
