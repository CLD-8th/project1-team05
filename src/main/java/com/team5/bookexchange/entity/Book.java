package com.team5.bookexchange.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "book")
@Getter
@Setter
@NoArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이 게시글을 등록한 회원의 ID
    private Long ownerId;

    private String title;

    private String author;

    private String description;

    private String status;

    private String imageName;

}