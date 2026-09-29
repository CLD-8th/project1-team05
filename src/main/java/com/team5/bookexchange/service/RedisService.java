package com.team5.bookexchange.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.team5.bookexchange.dto.BookCache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.team5.bookexchange.dto.LatestBookIdsCache;
import java.time.Duration;

@Service
public class RedisService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {

        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // 조회수 +1
    public Long increaseViewCount(Long bookId) {

        String key = "book:views:" + bookId;

        return redisTemplate.opsForValue().increment(key);
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

    // 도서 상세 정보를 Redis에 저장
    public void saveBookCache(BookCache bookCache) {
        saveBookCache(bookCache, false);
    }

    public void saveBookCache(BookCache bookCache, boolean latest) {

        String key = "book:" + bookCache.getId();

        try {
            String json = objectMapper.writeValueAsString(bookCache);

            if (latest) {
                redisTemplate.opsForValue().set(key, json);
            } else {
                redisTemplate.opsForValue().set(key, json, Duration.ofMinutes(30));
            }

        } catch (JacksonException e) {
            throw new RuntimeException("Redis 캐시 저장 실패", e);
        }
    }

    // Redis에서 도서 상세 정보 조회
    public BookCache getBookCache(Long bookId) {

        String key = "book:" + bookId;

        String json = redisTemplate.opsForValue().get(key);

        // Redis에 데이터가 없음 = MISS
        if (json == null) {
            return null;
        }

        try {
            return objectMapper.readValue(json, BookCache.class);

        } catch (JacksonException e) {
            throw new RuntimeException("Redis 캐시 조회 실패", e);
        }
    }

    // 도서 캐시 삭제
    public void deleteBookCache(Long bookId) {

        String key = "book:" + bookId;

        redisTemplate.delete(key);
    }

    // 도서 상세 캐시 존재 여부 확인
    public boolean hasBookCache(Long bookId) {
        String key = "book:" + bookId;

        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // 최신 상세 캐시 대상 ID 저장
    public void saveLatestBookIdsCache(LatestBookIdsCache cache) {
        String key = "books:latest:ids";

        String json = objectMapper.writeValueAsString(cache);

        redisTemplate.opsForValue().set(key, json);
    }

    // 최신 상세 캐시 대상 ID 조회
    public LatestBookIdsCache getLatestBookIdsCache() {
        String key = "books:latest:ids";

        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            return null;
        }

        return objectMapper.readValue(
                json,
                LatestBookIdsCache.class
        );
    }

    public void deleteLatestBookIdsCache() {
        String key = "books:latest:ids";

        redisTemplate.delete(key);
    }

    // 최신 대상에서 벗어나더라도 상세 캐시를 바로 버리지 않는다.
    public void expireBookCache(Long id) {
        redisTemplate.expire("book:" + id, Duration.ofMinutes(30));
    }

}
