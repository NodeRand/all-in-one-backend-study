package com.group.library_app.repository.book;

import org.springframework.stereotype.Repository;

@Repository
public class BookMySqlRepository implements BookRepository{
    @Override
    public void saveBook() {
        System.out.println("MySqlRepository");
    }
}
