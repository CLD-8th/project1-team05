# BookExchange AWS 환경 문제 분석

## 1. 현재 구조

Git 이력상 담당한 Docker 실행 환경과 이미지 저장 기능을 중심으로 확인하되, AWS에서 이 기능들과 직접 연결되는 회원 세션, Redis, MySQL, 교환 요청 흐름까지 함께 분석했다.

현재 `compose.yaml`은 한 서버에서 다음 컨테이너를 모두 실행한다.

```text
사용자
  │
  ▼
EC2 한 대
  └─ Docker Compose
      ├─ Spring Boot :8080
      │   ├─ JVM 메모리의 로그인 세션
      │   └─ /app/uploads의 도서 이미지
      ├─ MySQL 8.4
      │   └─ mysql-data Docker volume
      └─ Redis 8
          ├─ 도서 상세 캐시
          └─ 도서 조회수
```

Docker Compose 설정 자체는 `docker compose config` 검사를 통과한다. 그러나 컨테이너가 분리되어 있을 뿐 MySQL, Redis, 이미지, 세션이 사실상 같은 EC2에 종속되어 있다. 따라서 현재 구조는 개발·시연 환경에는 간단하지만, 실제 서비스의 장애 대응과 수평 확장에는 적합하지 않다.


## 2. 기능별 문제와 대응

### 2.1 단일 EC2와 Docker Compose

**현재 구현 방식**
Spring Boot, MySQL, Redis를 같은 EC2의 Docker Compose로 실행한다. MySQL과 이미지는 Docker volume을 사용하지만 Redis에는 별도 volume이 없다.

**AWS에서 예상되는 문제**
EC2 또는 가용 영역에 장애가 나면 웹 서비스, DB, 캐시, 이미지가 동시에 중단된다. Docker volume은 컨테이너 재생성에는 도움이 되지만 다른 EC2가 공유할 수 있는 저장소가 아니며, EC2 종료 시 루트 EBS가 함께 삭제되도록 설정되어 있다면 데이터도 사라진다. 같은 Compose 구성을 여러 EC2에서 각각 실행하면 각 서버가 서로 다른 MySQL·Redis·이미지를 가지는 분리된 서비스가 된다.

**문제가 발생하는 이유**
상태가 필요한 구성요소와 수평 확장 가능한 앱이 한 호스트의 생명주기를 공유하기 때문이다. 컨테이너 분리는 프로세스 격리일 뿐 고가용성이나 데이터 복제를 제공하지 않는다.

**대응 방법**
Spring Boot만 EC2에 남기고 최소 2개 가용 영역의 Auto Scaling Group으로 실행한다. 앞에는 ALB를 두고 `/actuator/health`를 상태 확인에 사용한다. MySQL, Redis, 이미지는 각각 외부 관리형 저장소로 분리한다. 애플리케이션 이미지는 한 번 빌드해 ECR에 올리고 각 EC2가 같은 이미지를 실행하도록 한다.

**필요한 AWS 서비스**
EC2 Auto Scaling, Application Load Balancer, ECR, RDS for MySQL, ElastiCache for Redis, S3, CloudWatch.

**해당 방법을 선택한 근거**
현재 Docker 이미지를 그대로 활용하면서 앱만 무상태로 만들 수 있다. ECS나 EKS까지 도입하지 않아도 EC2 장애 교체와 수평 확장이 가능하므로 현재 규모에서 변경 범위가 가장 작다.

**Trade-off**
EC2 두 대, ALB와 관리형 저장소의 고정 비용이 생긴다. 배포 시 여러 인스턴스의 버전 관리와 종료 중 요청 처리를 고려해야 한다. 트래픽과 가용성 요구가 낮은 과제·시연 단계라면 단일 EC2와 정기 백업으로 시작할 수 있지만, 이는 장애 중단을 받아들이는 선택이다.

### 2.2 도서 이미지 업로드와 조회

**현재 구현 방식**
`ImageService`가 업로드 파일을 `uploads/books`에 저장하고 DB에는 파일명만 기록한다. `WebConfig`가 같은 서버의 `/uploads/**`를 정적 파일로 제공하며 Compose의 `upload-data` volume이 `/app/uploads`에 연결된다. MIME 타입과 5MB 크기는 검사하지만 실제 파일 내용 검증은 하지 않는다.

**AWS에서 예상되는 문제**
서버가 여러 대이면 업로드를 처리한 EC2에만 이미지가 존재한다. 다음 이미지 요청이 다른 EC2로 전달되면 깨진 이미지가 보인다. EC2 종료나 volume 손상 시 DB에는 파일명이 남지만 실제 파일은 사라질 수 있다. 반대로 파일 저장 후 DB 저장이 실패하면 참조되지 않는 파일이 남는다. 큰 이미지 요청이 늘면 앱의 네트워크와 디스크 I/O도 함께 사용한다.

**문제가 발생하는 이유**
이미지 저장 위치와 제공 위치가 특정 앱 인스턴스의 로컬 파일 시스템이기 때문이다. 파일과 DB 저장도 하나의 원자적 작업이 아니며, Content-Type은 클라이언트가 보낸 값이라 위장될 수 있다.

**대응 방법**
이미지는 private S3 bucket에 UUID 기반 object key로 저장하고 DB에는 object key만 기록한다. 조회는 CloudFront 또는 제한된 S3 URL을 사용한다. 업로드 실패 시 DB를 저장하지 않고, DB 저장 실패 시 업로드한 객체를 삭제하는 보상 처리를 둔다. 서버에서 파일 signature도 확인한다. 초기에는 앱이 S3로 업로드하고, 업로드 트래픽이 실제 병목이 될 때만 presigned URL 직접 업로드로 전환한다.

**필요한 AWS 서비스**
Amazon S3. 이미지 조회량이 커지거나 전 세계 전송이 필요할 때 CloudFront를 추가한다.

**해당 방법을 선택한 근거**
S3는 여러 EC2가 같은 객체를 조회할 수 있고 EC2 생명주기와 파일을 분리한다. 현재 파일명 저장 구조도 object key 저장 방식으로 비교적 작게 변경할 수 있다.

**Trade-off**
S3 저장·요청·전송 비용과 IAM·bucket policy 관리가 추가된다. CloudFront를 사용하면 캐시 무효화와 URL 설계가 필요하다. presigned URL은 앱 부하를 줄이지만 업로드 완료 확인과 미사용 객체 정리 로직이 더 필요하므로 처음부터 도입하지 않는다.


## 3. AWS 구조

```text
Route 53
   │
ACM 인증서가 적용된 ALB
   │
   ├─ EC2(Spring Boot) - AZ A
   └─ EC2(Spring Boot) - AZ B
          │
          ├─ RDS MySQL Multi-AZ : 회원·도서·교환 요청 원본 데이터
          ├─ ElastiCache Redis  : 상세 캐시·공유 세션·단기 조회수
          └─ S3                 : 도서 이미지

ECR        : 동일한 애플리케이션 이미지 배포
CloudWatch : 로그·지표·경보
```

CloudFront, WAF, RDS Proxy, read replica는 기본 구성에 넣지 않는다. 이미지 전송량, 공격 트래픽, DB 연결 수, 읽기 부하가 실제 기준을 넘을 때 추가한다.


