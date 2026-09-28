# ⚡ Redis Key 설계

중고 도서 교환 서비스에서 자주 조회되는 **도서 상세 정보**를 빠르게 조회하기 위해 Redis 캐시를 사용한다.

현재 Redis 구조는 설계 단계이며, 이후 Ubuntu Server의 Docker 환경에서 Redis를 실행한 뒤 Spring Boot와 연결할 예정이다.

---

# 1. Redis 사용 목적

현재 도서 상세 조회는 다음과 같이 동작한다.

```text
사용자
  ↓
GET /books/{id}
  ↓
Spring Boot
  ↓
MySQL
  ↓
book 조회
```

도서 상세 페이지가 반복해서 조회될 경우 매번 MySQL에 접근하게 된다.

Redis를 적용하면 자주 조회되는 도서 정보를 Redis에 임시로 저장하여 MySQL 조회 횟수를 줄일 수 있다.

```text
사용자
  ↓
Spring Boot
  ↓
Redis 확인
  ↓
데이터가 있으면
Redis에서 바로 조회
```

---

# 2. 도서 상세 정보 캐시

## Key

도서 번호를 이용하여 Key를 생성한다.

```text
book:{bookId}
```

예시:

```text
book:1
book:2
book:3
```

3번 도서의 상세 정보를 저장한다면 다음 Key를 사용한다.

```text
book:3
```

---

# 3. Value

Redis에는 도서 상세 조회에 필요한 정보를 저장한다.

예시:

```json
{
  "id": 3,
  "title": "클라우드 입문",
  "author": "홍길동",
  "description": "AWS 공부용 책입니다.",
  "status": "AVAILABLE"
}
```

저장 대상 정보:

| 데이터 | 설명 |
|---|---|
| `id` | 도서 번호 |
| `title` | 도서 제목 |
| `author` | 저자 |
| `description` | 도서 설명 |
| `status` | 도서 교환 상태 |

---

# 4. 캐시 조회 동작

사용자가 도서 상세 페이지를 조회하면 먼저 Redis를 확인한다.

```text
GET /books/3
      ↓
Redis 확인
      ↓
book:3 존재?
   ┌──────┴──────┐
   │             │
  YES            NO
   │             │
   ▼             ▼
Redis 데이터    MySQL 조회
사용             │
                 ▼
            조회 결과
                 │
                 ▼
           Redis에 저장
           Key = book:3
                 │
                 ▼
              사용자
```

즉, 동작 순서는 다음과 같다.

1. 사용자가 도서 상세 정보를 요청한다.
2. Redis에서 `book:{bookId}`를 조회한다.
3. Redis에 데이터가 있으면 해당 데이터를 사용한다.
4. Redis에 데이터가 없으면 MySQL에서 도서를 조회한다.
5. MySQL 조회 결과를 Redis에 저장한다.
6. 조회된 도서 정보를 사용자에게 보여준다.

---

# 5. Cache Hit / Cache Miss

Redis 캐시 조회 결과는 크게 두 가지로 나뉜다.

### Cache Hit

Redis에 데이터가 존재하는 경우이다.

```text
GET /books/3
      ↓
Redis
      ↓
book:3 있음
      ↓
Cache Hit
      ↓
Redis 데이터 사용
```

MySQL을 다시 조회하지 않아도 된다.

### Cache Miss

Redis에 데이터가 존재하지 않는 경우이다.

```text
GET /books/3
      ↓
Redis
      ↓
book:3 없음
      ↓
Cache Miss
      ↓
MySQL 조회
      ↓
Redis 저장
      ↓
결과 사용
```

처음 조회하는 도서이거나 캐시가 삭제된 경우 발생할 수 있다.

---

# 6. 캐시 삭제

Redis에 저장된 데이터와 MySQL의 실제 데이터가 서로 달라지는 상황을 방지해야 한다.

따라서 도서 정보가 변경되면 해당 도서의 캐시를 삭제한다.

예를 들어 3번 도서가 변경되면:

```text
MySQL

book id = 3
정보 변경
    ↓
Redis

book:3 삭제
```

이후 다시 상세 조회가 발생하면:

```text
GET /books/3
      ↓
Redis

book:3 없음
      ↓
MySQL에서 최신 데이터 조회
      ↓
새로운 book:3 캐시 생성
```

---

# 7. 교환 요청 발생 시 캐시 처리

현재 서비스에서는 교환 요청이 발생하면 도서 상태가 변경된다.

```text
AVAILABLE
    ↓
교환 요청
    ↓
REQUESTED
```

따라서 Redis가 적용된 이후에는 교환 요청이 발생할 때 기존 도서 캐시도 삭제해야 한다.

예:

```text
POST /books/3/exchange
        ↓
exchange_request 생성
        ↓
book 상태 변경

AVAILABLE → REQUESTED
        ↓
Redis

book:3 삭제
```

캐시를 삭제하지 않으면 Redis에 이전 상태인 `AVAILABLE`이 남아 있을 수 있기 때문이다.

---

# 8. TTL

캐시 데이터가 Redis에 영구적으로 남지 않도록 **TTL(Time To Live)**을 설정할 예정이다.

TTL은 캐시 데이터가 유지되는 시간을 의미한다.

예를 들어 TTL을 10분으로 설정하면:

```text
book:3 저장
   ↓
10분 동안 유지
   ↓
TTL 만료
   ↓
자동 삭제
```

정확한 TTL 값은 Redis 구현 및 테스트 단계에서 결정한다.

---

# 9. Redis 장애 시 처리

Redis는 도서 데이터의 원본 저장소가 아니다.

실제 도서 데이터는 MySQL에 저장한다.

```text
MySQL
= 실제 데이터 저장

Redis
= 조회 성능을 위한 임시 캐시
```

따라서 Redis에 캐시가 없더라도 MySQL을 통해 도서 정보를 조회할 수 있는 구조로 구현한다.

---

# 10. 구현 예정 환경

Redis는 이후 Ubuntu Server에서 Docker를 이용하여 실행할 예정이다.

최종적으로 다음과 같은 구조를 구성한다.

```text
사용자
  ↓
Spring Boot
  │
  ├──── Redis
  │     도서 상세 캐시
  │
  └──── MySQL
        실제 데이터
```

Docker Compose 단계에서는 다음 서비스를 함께 실행하는 것을 목표로 한다.

```text
Docker Compose
│
├── app
│   └── Spring Boot
│
├── db
│   └── MySQL
│
└── cache
    └── Redis
```

---

# 11. 구현 상태

| 항목 | 상태 |
|---|---|
| Redis Key 설계 | ✅ 완료 |
| 도서 상세 캐시 설계 | ✅ 완료 |
| Cache Hit / Miss 흐름 | ✅ 설계 완료 |
| 캐시 삭제 방식 | ✅ 설계 완료 |
| TTL | 🔨 구현 시 결정 |
| Spring Boot ↔ Redis 연결 | 🔨 구현 예정 |
| Ubuntu Redis 실행 | 🔨 구현 예정 |
| Docker Compose 연결 | 🔨 구현 예정 |

현재 단계에서는 **Redis 설계까지만 완료**하고, 실제 연결과 동작 검증은 Ubuntu Server 및 Docker Compose 구성 단계에서 진행한다.