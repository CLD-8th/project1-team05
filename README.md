# 📚 BookExchange

> 중고 도서를 등록하고 다른 사용자가 교환을 요청할 수 있는 간단한 도서 교환 서비스

Spring Boot 기반으로 최소 서비스를 구현한 뒤  
Docker와 AWS 환경으로 확장하면서 **클라우드 아키텍처를 설계하는 프로젝트**입니다.

---

## 📌 프로젝트 소개

집에 더 이상 읽지 않는 책을 등록하고  
다른 사용자가 원하는 책에 교환 요청을 보낼 수 있는 서비스입니다.

핵심 흐름은 다음과 같습니다.

```text
도서 등록
   ↓
도서 목록
   ↓
도서 상세 조회
   ↓
교환 요청
   ↓
교환 상태 변경
```

복잡한 서비스 구현보다는 작은 애플리케이션을 먼저 완성하고  
이를 Docker와 AWS 환경으로 확장하는 것을 목표로 합니다.

---

## 🛠️ 기술 스택

| 구분 | 기술 |
|---|---|
| Language | Java 21 |
| Backend | Spring Boot |
| View | Thymeleaf |
| Frontend | HTML / CSS / JavaScript |
| Database | MySQL |
| ORM | Spring Data JPA |
| Cache | Redis |
| Build Tool | Gradle |
| Container | Docker / Docker Compose |
| Server | Ubuntu Server |
| Cloud | AWS |

> Redis, Docker, AWS 환경은 이후 단계에서 구성합니다.

---

## ✨ 주요 기능

### 📖 도서

- 도서 등록
- 도서 목록 조회
- 도서 상세 조회
- 제목 / 저자 검색
- 교환 상태 필터

### 🔄 교환 요청

- 도서 교환 요청
- 교환 요청 목록 조회
- 교환 요청 시 도서 상태 변경

```text
AVAILABLE
    ↓
교환 요청
    ↓
REQUESTED
```

### 👤 회원

회원 기능은 추가 구현 예정입니다.

회원 기능이 추가되면 다음 정보를 구분합니다.

- 내가 등록한 도서
- 내가 보낸 교환 요청
- 내가 받은 교환 요청

---

## 🗄️ 현재 데이터 구조

현재 구현된 주요 테이블은 다음과 같습니다.

```text
bookexchange
│
├── book
│   ├── id
│   ├── title
│   ├── author
│   ├── description
│   └── status
│
└── exchange_request
    ├── id
    ├── book_id
    └── status
```

회원 기능 추가 후 다음과 같이 확장할 예정입니다.

```text
member
  │
  ├──── book.owner_id
  │
  └──── exchange_request.requester_id
```

자세한 내용은 `docs/erd.md`를 참고합니다.

---

## ⚙️ 로컬 실행 환경

### 필요 환경

프로젝트 실행 전 다음 환경이 필요합니다.

```text
JDK 21
MySQL
```

MySQL에는 다음 데이터베이스를 생성합니다.

```sql
CREATE DATABASE bookexchange
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

---

## 🔐 환경 변수

DB 접속 정보는 코드에 직접 저장하지 않고 환경 변수로 관리합니다.

```text
DB_URL=jdbc:mysql://localhost:3306/bookexchange
DB_USERNAME=root
DB_PASSWORD=본인의_MySQL_비밀번호
```

IntelliJ에서는 다음 위치에서 설정합니다.

```text
실행 구성 편집
    ↓
BookexchangeApplication
    ↓
환경 변수
```

> 실제 DB 비밀번호는 GitHub에 커밋하지 않습니다.

---

## ▶️ 실행 방법

### IntelliJ

`BookexchangeApplication.java`를 실행합니다.

정상적으로 실행되면 다음 주소로 접속합니다.

```text
http://localhost:8080
```

---

### PowerShell

환경 변수를 설정합니다.

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/bookexchange"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="본인의_MySQL_비밀번호"
```

Spring Boot를 실행합니다.

```powershell
.\gradlew.bat bootRun
```

---

## 🌐 주요 페이지

| 기능 | URL |
|---|---|
| 메인 화면 | `/` |
| 도서 목록 | `/books` |
| 도서 등록 | `/books/new` |
| 도서 상세 | `/books/{id}` |
| 교환 요청 목록 | `/exchanges` |

교환 요청은 다음 API를 사용합니다.

```text
POST /books/{id}/exchange
```

---

## ⚡ Redis 적용 계획

도서 상세 정보는 Redis에 캐싱할 예정입니다.

```text
사용자
   ↓
Spring Boot
   ↓
Redis
 ┌─┴────────────┐
 │ Cache Hit    │ Cache Miss
 ▼              ▼
Redis           MySQL
반환             조회
                  ↓
              Redis 저장
```

Redis Key:

```text
book:{bookId}
```

예:

```text
book:3
```

실제 Redis 연결은 Ubuntu Server의 Docker 환경에서 진행합니다.

---

## 🐳 Docker 구성 계획

최종적으로 Docker Compose를 이용하여 다음 서비스를 함께 실행합니다.

```text
Ubuntu Server
      │
      ▼
Docker Compose
      │
 ┌────┼─────┐
 ▼    ▼     ▼
App   DB   Cache
 │    │     │
 ▼    ▼     ▼
Spring MySQL Redis
Boot
```

---

## ☁️ AWS 확장 계획

로컬 및 Docker 환경에서 애플리케이션 동작을 확인한 뒤  
AWS 환경으로 단계적으로 확장합니다.

```text
1단계
단일 서버 구성
      ↓
2단계
DB / Cache 등 관리형 서비스 분리
      ↓
3단계
다중 가용 영역 구성
      ↓
Load Balancer
      ↓
Auto Scaling
```

최종적으로 장애 대응과 확장이 가능한 클라우드 아키텍처를 설계하는 것을 목표로 합니다.

---

## 📂 프로젝트 구조

```text
bookexchange/
│
├── docs/
│   ├── service.md
│   ├── erd.md
│   ├── api.md
│   ├── database.md
│   └── redis.md
│
├── src/
│   └── main/
│       ├── java/
│       │   └── com/team5/bookexchange/
│       │       ├── controller/
│       │       ├── service/
│       │       ├── repository/
│       │       └── entity/
│       │
│       └── resources/
│           ├── templates/
│           ├── static/
│           └── application.yml
│
├── build.gradle
└── README.md
```

---

## 📑 상세 문서

프로젝트의 상세 설계 및 실행 방법은 `docs` 디렉터리에서 확인할 수 있습니다.

| 문서 | 설명 |
|---|---|
| `docs/service.md` | 서비스 개요 및 전체 기능 |
| `docs/erd.md` | 데이터베이스 구조 및 관계 |
| `docs/api.md` | URL 및 API 설계 |
| `docs/database.md` | MySQL 설정 및 실행 환경 |
| `docs/redis.md` | Redis Key 및 캐시 설계 |

---

## 🚧 구현 현황

| 기능 | 상태 |
|---|---|
| Spring Boot 기본 구성 | ✅ |
| MySQL 연결 | ✅ |
| 메인 화면 | ✅ |
| 도서 등록 | ✅ |
| 도서 목록 | ✅ |
| 도서 검색 / 필터 | ✅ |
| 도서 상세 조회 | ✅ |
| 교환 요청 생성 | ✅ |
| 교환 요청 목록 | ✅ |
| 도서 상태 변경 | ✅ |
| 회원 기능 | 🔨 진행 예정 |
| Redis | 🔨 진행 예정 |
| Docker Compose | 🔨 진행 예정 |
| Ubuntu Server 실행 | 🔨 진행 예정 |
| AWS 아키텍처 | 🔨 설계 예정 |

---

## 👥 Team

**Team 05**

클라우드 아키텍처 설계 및 기획 프로젝트