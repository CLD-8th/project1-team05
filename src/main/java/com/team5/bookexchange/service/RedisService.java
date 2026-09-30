package com.team5.bookexchange.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    public RedisService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 조회수 +1
    public Long increaseViewCount(Long bookId) {

        String viewKey = "book:views:" + bookId;

        // 개별 도서 조회수 증가
        Long viewCount =
                redisTemplate.opsForValue().increment(viewKey);

        // TOP 10 랭킹 점수도 +1
        redisTemplate.opsForZSet().incrementScore(
                "book:ranking",
                bookId.toString(),
                1
        );

        return viewCount;
    }

    // 현재 조회수 가져오기
    public Long getViewCount(Long bookId) {

        String key = "book:views:" + bookId;

        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return 0L;
        }

        return Long.parseLong(value);
    }

    // 조회수 TOP 10 도서 ID 가져오기
    public List<Long> getTop10BookIds() {

        Set<String> bookIds =
                redisTemplate.opsForZSet()
                        .reverseRange(
                                "book:ranking",
                                0,
                                9
                        );

        if (bookIds == null) {
            return Collections.emptyList();
        }

        return bookIds.stream()
                .map(Long::parseLong)
                .toList();
    }
}