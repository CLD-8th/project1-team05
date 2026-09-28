\# 중고 도서 교환 서비스 ERD



\## 1. 회원 (member)



| 컬럼 | 설명 |

|---|---|

| id | 회원 번호 (PK) |

| username | 사용자 이름 |

| password | 비밀번호 |



\## 2. 도서 (book)



| 컬럼 | 설명 |

|---|---|

| id | 도서 번호 (PK) |

| member\_id | 등록한 회원 번호 (FK) |

| title | 책 제목 |

| author | 저자 |

| description | 책 설명 |

| status | 교환 가능 상태 |



\## 3. 교환 요청 (exchange\_request)



| 컬럼 | 설명 |

|---|---|

| id | 요청 번호 (PK) |

| book\_id | 교환을 원하는 도서 번호 (FK) |

| requester\_id | 요청한 회원 번호 (FK) |

| status | 요청 상태 |



\## 관계



member 1 : N book



member 1 : N exchange\_request



book 1 : N exchange\_request

