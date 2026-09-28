# 🗄️ 데이터베이스 및 실행 환경

중고 도서 교환 서비스의 로컬 개발 환경과 MySQL 데이터베이스 설정 방법을 정리한다.

---

## 1. 개발 환경

| 항목 | 사용 환경 |
|---|---|
| Java | JDK 21 |
| Backend | Spring Boot |
| Database | MySQL |
| Database Name | `bookexchange` |
| Character Set | `utf8mb4` |
| ORM | Spring Data JPA |
| Build Tool | Gradle |

---

# 2. 데이터베이스 생성

프로젝트를 처음 실행하는 팀원은 MySQL 또는 DBeaver에서 데이터베이스를 생성한다.

```sql
CREATE DATABASE bookexchange
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

이미 `bookexchange` 데이터베이스가 존재한다면 다시 생성할 필요가 없다.

생성된 데이터베이스를 확인한다.

```sql
SHOW DATABASES;
```

---

# 3. 환경 변수 설정

데이터베이스 계정과 비밀번호는 GitHub에 직접 저장하지 않고 **환경 변수**로 관리한다.

다음 3개의 환경 변수가 필요하다.

```text
DB_URL=jdbc:mysql://localhost:3306/bookexchange
DB_USERNAME=root
DB_PASSWORD=본인의_MySQL_비밀번호
```

## IntelliJ 설정

IntelliJ에서 다음 위치로 이동한다.

```text
실행 구성 편집
    ↓
BookexchangeApplication
    ↓
환경 변수
```

환경 변수에 다음 값을 등록한다.

```text
DB_URL=jdbc:mysql://localhost:3306/bookexchange
DB_USERNAME=root
DB_PASSWORD=본인의_MySQL_비밀번호
```

> 실제 MySQL 비밀번호는 코드나 GitHub에 커밋하지 않는다.

---

# 4. application.yml

Spring Boot는 환경 변수에서 MySQL 접속 정보를 가져온다.

```yaml
spring:
  application:
    name: bookexchange

  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

현재 `ddl-auto: update`를 사용하므로 Entity 변경 사항을 기준으로 필요한 테이블 구조가 자동으로 반영된다.

---

# 5. 프로젝트 실행

## IntelliJ

`BookexchangeApplication.java`를 실행한다.

정상적으로 실행되면 콘솔에서 다음과 비슷한 로그를 확인할 수 있다.

```text
Started BookexchangeApplication
```

실행 후 브라우저에서 다음 주소로 접속한다.

```text
http://localhost:8080
```

BookExchange 메인 화면이 표시되면 애플리케이션이 정상적으로 실행된 것이다.

---

## PowerShell

PowerShell에서 직접 실행할 경우 먼저 환경 변수를 설정한다.

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/bookexchange"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="본인의_MySQL_비밀번호"

.\gradlew.bat bootRun
```

---

# 6. 현재 데이터베이스 구조

현재 프로젝트에서는 다음 테이블을 사용하거나 구현할 예정이다.

```text
bookexchange
│
├── book
│
└── exchange_request

추가 예정
│
└── member
```

---

## 6.1 book

도서 정보를 저장하는 테이블이다.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `id` | BIGINT | 도서 번호 (PK, 자동 증가) |
| `title` | VARCHAR(255) | 도서 제목 |
| `author` | VARCHAR(255) | 저자 |
| `description` | VARCHAR(255) | 도서 설명 |
| `status` | VARCHAR(255) | 도서 교환 상태 |

### 도서 상태

현재 사용하는 상태는 다음과 같다.

| 상태 | 의미 |
|---|---|
| `AVAILABLE` | 교환 가능 |
| `REQUESTED` | 교환 요청이 들어온 상태 |
| `EXCHANGED` | 교환 완료 |

현재 구현된 테이블이다.

---

## 6.2 exchange_request

도서에 대한 교환 요청을 저장하는 테이블이다.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `id` | BIGINT | 교환 요청 번호 (PK, 자동 증가) |
| `book_id` | BIGINT | 교환을 요청한 도서 번호 |
| `status` | VARCHAR(255) | 교환 요청 상태 |

### 교환 요청 상태

| 상태 | 의미 |
|---|---|
| `PENDING` | 교환 요청 대기 중 |
| `ACCEPTED` | 교환 요청 수락 |
| `REJECTED` | 교환 요청 거절 |

현재 기본적인 교환 요청 저장 기능까지 구현되어 있다.

현재 단계에서는 `book_id`를 이용하여 어떤 도서에 대한 요청인지 구분한다.

---

## 6.3 member

회원 정보를 저장하기 위한 테이블이다.

현재는 **구현 예정**이다.

회원 기능이 추가되면 다음과 같은 정보를 관리할 예정이다.

| 컬럼 | 설명 |
|---|---|
| `id` | 회원 번호 |
| `nickname` | 사용자 이름 |

회원 기능 구현 후 `book`과 `exchange_request`에 회원 정보를 연결할 예정이다.

예상 구조는 다음과 같다.

```text
member
  │
  ├──── book.owner_id
  │
  └──── exchange_request.requester_id
```

이를 통해 다음 정보를 구분할 수 있게 된다.

- 누가 등록한 도서인지
- 누가 교환을 요청했는지
- 내가 보낸 교환 요청
- 내가 받은 교환 요청

> 회원 기능은 팀 내 별도 작업으로 구현할 예정이다.

---

# 7. 현재 데이터 관계

현재 구현된 관계를 단순하게 표현하면 다음과 같다.

```text
book
│
│ id
│
└──────────────┐
               │
               ▼
        exchange_request
        book_id
```

예를 들어 3번 도서에 교환 요청이 들어오면:

```text
book

id = 3
title = 클라우드 입문
status = REQUESTED
```

```text
exchange_request

id = 1
book_id = 3
status = PENDING
```

과 같이 저장된다.

회원 기능이 추가되면 도서 소유자와 교환 요청자 관계도 추가할 예정이다.

---

# 8. 데이터베이스 확인

DBeaver에서 다음 SQL을 이용하여 현재 데이터베이스 상태를 확인할 수 있다.

```sql
USE bookexchange;

SHOW TABLES;
```

도서 데이터 확인:

```sql
SELECT * FROM book;
```

교환 요청 데이터 확인:

```sql
SELECT * FROM exchange_request;
```

현재 테이블 구조를 확인하려면 다음 명령을 사용할 수 있다.

```sql
DESC book;

DESC exchange_request;
```

---

# 9. Spring Boot ↔ MySQL 연결 확인

Spring Boot를 실행한 뒤 브라우저에서 다음 주소로 접속한다.

```text
http://localhost:8080/books
```

등록된 도서 목록 화면이 정상적으로 표시되면 Spring Boot가 실행되고 있는지 확인할 수 있다.

실제로 데이터베이스 저장까지 확인하려면 웹 화면에서 도서를 등록한 뒤 DBeaver에서 다음 SQL을 실행한다.

```sql
SELECT * FROM book;
```

등록한 도서가 조회되면 다음 흐름이 정상적으로 연결된 것이다.

```text
웹 브라우저
    ↓
Spring Boot
    ↓
JPA
    ↓
MySQL
    ↓
book 테이블
```

---

# 10. 현재 구현 상태

현재 데이터베이스 관련 구현 상태는 다음과 같다.

| 기능 | 상태 |
|---|---|
| MySQL 연결 | ✅ 완료 |
| 도서 저장 | ✅ 완료 |
| 도서 목록 조회 | ✅ 완료 |
| 도서 상세 조회 | ✅ 완료 |
| 교환 요청 저장 | ✅ 완료 |
| 도서 상태 변경 | ✅ 완료 |
| 회원 정보 | 🔨 구현 예정 |
| 도서 소유자 연결 | 🔨 구현 예정 |
| 교환 요청자 연결 | 🔨 구현 예정 |
| Redis 연결 | 🔨 구현 예정 |