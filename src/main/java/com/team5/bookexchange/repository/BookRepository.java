package com.team5.bookexchange.repository;

import com.team5.bookexchange.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
}