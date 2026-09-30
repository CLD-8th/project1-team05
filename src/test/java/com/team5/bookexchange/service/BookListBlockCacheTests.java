package com.team5.bookexchange.service;

import com.team5.bookexchange.dto.BookListItem;
import com.team5.bookexchange.dto.BookListBlockCache;
import com.team5.bookexchange.entity.Book;
import com.team5.bookexchange.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import java.util.*;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookListBlockCacheTests {
    private final BookRepository repository = mock(BookRepository.class);
    private final RedisService redis = mock(RedisService.class);
    private final BookService service = new BookService(repository, redis);

    private List<Book> books(int firstId, int count) {
        return IntStream.range(0, count).mapToObj(i -> {
            Book book = new Book();
            book.setId((long) firstId - i);
            book.setOwnerId(1L);
            book.setStatus("REQUESTED");
            return book;
        }).toList();
    }

    @Test
    void pagesFourFiveSixShareOneDatabaseRead() {
        Map<Integer, BookListBlockCache> blocks = new HashMap<>();
        when(redis.getBookListVersion()).thenReturn("v1");
        when(redis.getBookListBlock(eq("v1"), anyInt()))
                .thenAnswer(call -> blocks.get(call.getArgument(1)));
        doAnswer(call -> {
            blocks.put(call.getArgument(1), call.getArgument(2));
            return null;
        }).when(redis).saveBookListBlock(eq("v1"), anyInt(), any());
        Pageable blockRequest = PageRequest.of(1, 30, Sort.by(Sort.Direction.DESC, "id"));
        when(repository.findAll(blockRequest))
                .thenReturn(new PageImpl<>(books(970, 30), blockRequest, 1000));
        for (int page = 3; page <= 5; page++) {
            Page<BookListItem> result = service.findPage(page);
            assertEquals(10, result.getNumberOfElements());
            assertEquals(970L - (page - 3) * 10, result.getContent().getFirst().getId());
            assertEquals(100, result.getTotalPages());
        }
        verify(repository, times(1)).findAll(blockRequest);
    }

    @Test
    void partialBlockAndOutOfRangePageRemainSafe() {
        when(redis.getBookListVersion()).thenReturn("v1");
        when(redis.getBookListBlock("v1", 1)).thenReturn(new BookListBlockCache(
                books(5, 5).stream().map(BookListItem::new).toList(), 35));
        assertEquals(5, service.findPage(3).getNumberOfElements());
        assertTrue(service.findPage(4).isEmpty());
        assertEquals(4, service.findPage(4).getTotalPages());
        verifyNoInteractions(repository);
    }

    @Test
    void emptyDatabaseReturnsEmptyPage() {
        when(redis.getBookListVersion()).thenReturn("v1");
        when(repository.findAll(any(Pageable.class))).thenReturn(Page.empty());
        Page<BookListItem> result = service.findPage(0);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalPages());
    }

    @Test
    void registrationAndEveryStatusChangeInvalidateLists() {
        Book book = books(1, 1).getFirst();
        when(repository.save(book)).thenReturn(book);
        when(repository.findById(1L)).thenReturn(Optional.of(book));
        service.save(book);
        service.updateStatus(1L, "AVAILABLE");
        service.requestExchange(1L);
        service.acceptExchange(1L, 1L);
        book.setStatus("REQUESTED");
        service.rejectExchange(1L, 1L);
        verify(redis, times(5)).invalidateBookListCache();
    }

    @Test
    void detailAlwaysReadsDatabaseWithoutCache() {
        Book book = books(1, 1).getFirst();
        when(repository.findById(1L)).thenReturn(Optional.of(book));
        assertSame(book, service.findById(1L));
        verifyNoInteractions(redis);
    }

    @Test
    void listJsonExcludesDescriptionAndOwnerButKeepsLinkAndCover() {
        Book book = books(1, 1).getFirst();
        book.setTitle("Title");
        book.setAuthor("Author");
        book.setImageName("cover.jpg");
        book.setDescription("Detail only");
        var mapper = new tools.jackson.databind.ObjectMapper();
        String json = mapper.writeValueAsString(new BookListBlockCache(
                List.of(new BookListItem(book)), 1));
        assertFalse(json.contains("description"));
        assertFalse(json.contains("ownerId"));
        BookListItem item = mapper.readValue(json, BookListBlockCache.class).getBooks().getFirst();
        assertEquals(1L, item.getId());
        assertEquals("cover.jpg", item.getImageName());
        assertEquals("Title", item.getTitle());
        assertEquals("Author", item.getAuthor());
        assertEquals("REQUESTED", item.getStatus());
    }

    @Test
    void rejectedWriteDoesNotInvalidateCache() {
        Book book = books(1, 1).getFirst();
        when(repository.findById(1L)).thenReturn(Optional.of(book));
        assertThrows(RuntimeException.class, () -> service.acceptExchange(1L, 99L));
        verifyNoInteractions(redis);
        verify(repository, never()).save(any(Book.class));
    }
}
