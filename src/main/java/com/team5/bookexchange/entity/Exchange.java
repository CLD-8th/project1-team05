package com.team5.bookexchange.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "exchange")
@Getter
@Setter
@NoArgsConstructor
public class Exchange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 책을 요청했는지
    private Long bookId;

    // 누가 요청했는지
    private Long requesterId;

    // 누구에게 요청했는지
    private Long receiverId;
}