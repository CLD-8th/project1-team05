# 데이터베이스 및 실행 환경

## 1. 데이터베이스 정보

- DBMS: MySQL
- Database: `bookexchange`
- Character Set: `utf8mb4`
- Java: JDK 21
- Backend: Spring Boot

---

## 2. 데이터베이스 생성

프로젝트를 처음 실행하는 팀원은 MySQL 또는 DBeaver에서 아래 SQL을 실행한다.

```sql
CREATE DATABASE bookexchange
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

이미 `bookexchange` 데이터베이스가 있다면 다시 생성할 필요가 없다.

---

## 3. 환경 변수 설정

DB 접속 정보는 GitHub에 직접 저장하지 않고 환경 변수로 관리한다.

다음 3개의 환경 변수를 설정한다.

```text
DB_URL=jdbc:mysql://localhost:3306/bookexchange
DB_USERNAME=root
DB_PASSWORD=본인의_MySQL_비밀번호
```

IntelliJ에서는

`실행 구성 편집 → 환경 변수`

에서 설정한다.

실제 비밀번호는 GitHub에 커밋하지 않는다.

---

## 4. application.yml

Spring Boot는 환경 변수에서 DB 접속 정보를 가져온다.

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

---

## 5. 프로젝트 실행

### IntelliJ

`BookexchangeApplication.java`를 실행한다.

정상적으로 실행되면 콘솔에서 다음과 같은 로그를 확인할 수 있다.

```text
Started BookexchangeApplication
```

### PowerShell에서 실행

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/bookexchange"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="본인의_MySQL_비밀번호"

.\gradlew.bat bootRun
```

---

## 6. 현재 테이블 구조

### book

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT | 도서 번호 (PK, 자동 증가) |
| title | VARCHAR(255) | 도서 제목 |
| author | VARCHAR(255) | 저자 |
| description | VARCHAR(255) | 도서 설명 |
| status | VARCHAR(255) | 교환 상태 |

현재 구현 완료된 테이블이다.

### member

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT | 회원 번호 (PK, 자동 증가) |
| username | VARCHAR(255) | 사용자 이름 |
| password | VARCHAR(255) | 비밀번호 |

구현 예정이다.

### exchange_request

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT | 요청 번호 (PK, 자동 증가) |
| book_id | BIGINT | 요청 대상 도서 (FK) |
| requester_id | BIGINT | 요청한 회원 (FK) |
| status | VARCHAR(255) | 요청 상태 |

구현 예정이다.

---

## 7. DB 확인

DBeaver에서 다음 SQL로 확인할 수 있다.

```sql
USE bookexchange;

SHOW TABLES;

SELECT * FROM book;
```

현재 프로젝트는 JPA의 `ddl-auto: update` 설정을 사용하므로 Entity를 기준으로 테이블이 생성된다.

---

## 8. 연결 확인

Spring Boot 실행 후 브라우저에서 다음 주소로 접속한다.

```text
http://localhost:8080/books
```

등록된 도서 목록이 JSON으로 출력되면 Spring Boot와 MySQL 연결이 정상적으로 완료된 것이다.