\# Redis Key 설계



\## 도서 상세 정보 캐시



\### Key



book:{bookId}



예시:



book:1

book:2



\### Value



도서 상세 정보를 저장한다.



\- 도서 번호

\- 제목

\- 저자

\- 설명

\- 교환 상태



\### 동작



도서 상세 조회 요청

↓

Redis에서 book:{bookId} 조회

↓

캐시가 있으면 Redis 데이터 반환

↓

캐시가 없으면 MySQL 조회

↓

조회 결과를 Redis에 저장

↓

사용자에게 반환



\### 캐시 삭제



도서 정보가 수정되거나 삭제되면

해당 book:{bookId} 캐시도 삭제한다.

