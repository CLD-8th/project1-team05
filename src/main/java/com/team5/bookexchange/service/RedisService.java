package com.team5.bookexchange.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.team5.bookexchange.dto.BookListBlockCache;
import tools.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.UUID;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class RedisService {

    private static final String LIST_VERSION_KEY = "books:list:v2:version";
    private static final Duration LIST_TTL = Duration.ofMinutes(5);
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    public RedisService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
    // 여러 서버에서 공유하는 목록 캐시 버전. 이전 묶음은 TTL로 정리한다.
    public String getBookListVersion() {
        String version = redisTemplate.opsForValue().get(LIST_VERSION_KEY);
        if (version != null) {
            return version;
        }
        String candidate = UUID.randomUUID().toString();
        if (Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(LIST_VERSION_KEY, candidate))) {
            return candidate;
        }
        version = redisTemplate.opsForValue().get(LIST_VERSION_KEY);
        if (version == null) {
            throw new IllegalStateException("목록 캐시 버전을 읽을 수 없습니다.");
        }
        return version;
    }

    public BookListBlockCache getBookListBlock(String version, int block) {
        String json = redisTemplate.opsForValue().get(blockKey(version, block));
        return json == null ? null : objectMapper.readValue(json, BookListBlockCache.class);
    }

    public void saveBookListBlock(String version, int block, BookListBlockCache cache) {
        redisTemplate.opsForValue().set(blockKey(version, block),
                objectMapper.writeValueAsString(cache), LIST_TTL);
    }

    // 외부 트랜잭션이 있다면 성공적으로 커밋된 뒤 버전을 변경한다.
    public void invalidateBookListCache() {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    rotateBookListVersion();
                }
            });
        } else {
            rotateBookListVersion();
        }
    }

    private void rotateBookListVersion() {
        redisTemplate.opsForValue().set(LIST_VERSION_KEY, UUID.randomUUID().toString());
    }

    private String blockKey(String version, int block) {
        return "books:list:v2:block:" + version + ":" + block;
    }
}
