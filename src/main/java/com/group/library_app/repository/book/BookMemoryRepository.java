package com.group.library_app.repository.book;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.awt.print.Book;
import java.util.ArrayList;
import java.util.List;
@Primary
@Repository
public class BookMemoryRepository implements BookRepository{
    // private final List<Book> books = new ArrayList<Book>();

    @Override
    public void saveBook(){
        System.out.println("MemoryRepository");
    }
}
