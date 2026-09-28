package com.team5.bookexchange.repository;

import com.team5.bookexchange.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    long countByStatus(String status);

    List<Book> findTop3ByOrderByIdDesc();
}

