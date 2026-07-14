# LiveKlass

## 프로젝트 개요

LiveKlass는 크리에이터가 강의를 개설하고 클래스메이트가 수강 신청, 결제 확정, 취소를 할 수 있는 수강 신청 시스템입니다.

주요 구현 범위:

- 강의 등록, 목록 조회, 상세 조회
- 강의 상태 전이: `DRAFT -> OPEN -> CLOSED`
- 수강 신청 상태 전이: `PENDING -> CONFIRMED -> CANCELLED`
- 정원 초과 방지
- 동시 신청 상황을 고려한 낙관적 락 재시도
- 결제 PG callback 기반 결제 확정
- 결제 callback 멱등성 처리
- 취소 가능 기간 제한
- 크리에이터 전용 강의별 수강생 목록 조회
- 읽기/쓰기 datasource routing
- DB outbox 기반 이벤트 기록

## 기술 스택

- Java 21
- Spring Boot 3.5.3
- Spring Web
- Spring Data JPA
- QueryDSL
- Bean Validation
- H2 Database
- Gradle
- JUnit 5, Spring Boot Test, MockMvc

## 실행 방법

### 로컬 실행

이 프로젝트는 Java 21 기준으로 작성되었습니다. 처음 clone 받은 환경에서는 JDK 21이 설치되어 있고 `JAVA_HOME`이 JDK 21을 가리키는지 먼저 확인합니다.

```bash
java -version
```

출력에 Java 21이 표시되어야 합니다. Gradle은 wrapper를 사용하므로 별도 Gradle 설치는 필요하지 않습니다.

```bash
./gradlew bootRun
```

Windows PowerShell에서는 다음처럼 실행합니다.

```powershell
.\gradlew bootRun
```

기본 실행 포트는 Spring Boot 기본값인 `8080`입니다.

```text
http://localhost:8080
```

### DB 설정

기본 설정은 H2 in-memory DB입니다.

```properties
app.datasource.master.url=jdbc:h2:mem:liveklass;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
app.datasource.slave.url=jdbc:h2:mem:liveklass;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.jpa.hibernate.ddl-auto=create-drop
```

로컬 H2에서는 master/slave datasource가 같은 DB를 바라보며, `@Transactional(readOnly = true)` 값에 따라 routing 되는 구조를 테스트합니다.

H2 console:

```text
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:liveklass
User Name: sa
Password:
```

## API 목록 및 예시

사용자 식별은 별도 인증 시스템 대신 `X-USER-ID` header를 사용합니다. 별도 사용자 테이블은 없으며, 양수 Long 형태의 header 값을 사용자 id로 간주합니다. 크리에이터 권한은 강의 등록 시 저장된 `creatorId`와 요청자의 `X-USER-ID`가 일치하는지로 판단합니다.

### 강의 등록

```http
POST /api/klasses
X-USER-ID: 1
Content-Type: application/json
```

```json
{
  "title": "Spring Boot 입문",
  "description": "Spring Boot와 JPA를 학습하는 강의입니다.",
  "price": 50000,
  "capacity": 30,
  "startDate": "2026-08-01T00:00:00",
  "endDate": "2026-08-31T23:59:59"
}
```

응답:

```http
201 Created
Location: /api/klasses/1
```

```json
{
  "id": 1
}
```

### 강의 모집 시작

```http
PATCH /api/klasses/1/open
X-USER-ID: 1
```

응답:

```http
204 No Content
```

### 강의 모집 마감

```http
PATCH /api/klasses/1/close
X-USER-ID: 1
```

응답:

```http
204 No Content
```

### 강의 목록 조회

```http
GET /api/klasses?status=OPEN&page=0&size=20
```

응답 예시:

```json
{
  "content": [
    {
      "id": 1,
      "creatorId": 1,
      "title": "Spring Boot 입문",
      "description": "Spring Boot와 JPA를 학습하는 강의입니다.",
      "price": 50000,
      "capacity": 30,
      "enrolledCount": 1,
      "startDate": "2026-08-01T00:00:00",
      "endDate": "2026-08-31T23:59:59",
      "status": "OPEN",
      "statusLabel": "모집 중",
      "createdAt": "2026-07-13T10:00:00",
      "updatedAt": "2026-07-13T10:00:00"
    }
  ],
  "pageable": {},
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "size": 20,
  "number": 0
}
```

### 강의 상세 조회

```http
GET /api/klasses/1
```

### 강의 상태 form 조회

```http
GET /api/klasses/form
```

응답:

```json
{
  "DRAFT": "초안",
  "OPEN": "모집 중",
  "CLOSED": "모집 마감"
}
```

### 수강 신청

```http
POST /api/klasses/1/enrollments
X-USER-ID: 2
```

응답:

```http
201 Created
Location: /api/enrollments/1
```

```json
{
  "id": 1
}
```

### 수강 취소

```http
POST /api/enrollments/1/cancel
X-USER-ID: 2
```

응답:

```http
204 No Content
```

### 내 수강 신청 목록

```http
GET /api/users/2/enrollments?page=0&size=20
```

### 강의별 수강생 목록

크리에이터 전용 API입니다.

```http
GET /api/klasses/1/enrollments?page=0&size=20
X-USER-ID: 1
```

### 수강 신청 상태 form 조회

```http
GET /api/enrollments/form
```

응답:

```json
{
  "PENDING": "결제 대기",
  "CONFIRMED": "수강 확정",
  "CANCELLED": "취소됨"
}
```

### 결제 확정 callback

외부 PG사가 결제 완료 후 호출하는 callback API로 가정했습니다.

```http
POST /api/payments/callback
Idempotency-Key: pay-callback-1
Content-Type: application/json
```

```json
{
  "paymentId": "pay-20260713-0001",
  "enrollmentId": 1,
  "paidAmount": 50000
}
```

응답:

```http
204 No Content
```

`Idempotency-Key` header가 없으면 `"payment:" + paymentId`를 fallback key로 사용합니다.

## 데이터 모델 설명

### Klass

강의를 나타내는 aggregate root입니다.

주요 필드:

- `id`
- `creatorId`
- `title`
- `description`
- `price`
- `capacity`
- `enrolledCount`
- `startDate`
- `endDate`
- `status`: `DRAFT`, `OPEN`, `CLOSED`
- `version`: optimistic locking
- `createdAt`, `updatedAt`

주요 인덱스:

- `idx_klasses_status(status)`
- `idx_klasses_creator_status(creator_id, status)`

### Enrollment

수강 신청을 나타냅니다. 하나의 `Klass`에 반드시 속하므로 FK를 사용합니다.

주요 필드:

- `id`
- `klass_id`
- `userId`
- `status`: `PENDING`, `CONFIRMED`, `CANCELLED`
- `confirmedAt`
- `cancelledAt`
- `createdAt`, `updatedAt`

주요 인덱스:

- `idx_enrollments_klass_user(klass_id, user_id)`
- `idx_enrollments_user_status(user_id, status)`

### PaymentIdempotency

외부 결제 callback의 중복 처리를 위한 테이블입니다.

주요 필드:

- `id`
- `idempotencyKey`
- `requestHash`
- `status`: `PROCESSING`, `COMPLETED`
- `completedAt`
- `createdAt`, `updatedAt`

`idempotencyKey`에는 unique 제약을 둡니다.

### OutboxEvent

도메인 변경과 외부 이벤트 발행 사이의 정합성을 보장하기 위한 DB outbox 테이블입니다.

주요 필드:

- `id`
- `aggregateType`
- `aggregateId`
- `eventType`: `ENROLLMENT_CREATED`, `ENROLLMENT_CONFIRMED`, `ENROLLMENT_CANCELLED`
- `payload`
- `status`: `PENDING`, `PUBLISHED`, `FAILED`
- `retryCount`
- `nextRetryAt`
- `occurredAt`
- `publishedAt`

## 요구사항 해석 및 가정

- 강의 엔티티 이름은 Java 예약어 `Class`와 충돌하지 않도록 `Klass`로 정했습니다.
- 강의 생성 직후 상태는 `DRAFT`입니다.
- `DRAFT` 강의에는 수강 신청할 수 없습니다.
- 강의는 `OPEN` 상태에서만 신청할 수 있습니다.
- 정원은 결제 확정 시점이 아니라 수강 신청 생성 시점에 점유합니다.
- `PENDING` 신청도 활성 신청으로 보고 중복 신청과 정원 계산에 포함합니다.
- 같은 사용자는 같은 강의에 `PENDING` 또는 `CONFIRMED` 신청을 중복 생성할 수 없습니다.
- `PENDING` 신청은 언제든 취소할 수 있습니다.
- `CONFIRMED` 신청은 결제 확정 후 7일 이내이고 강의 시작 전인 경우에만 취소할 수 있습니다.
- 강의 제목 중복은 허용합니다. 같은 제목의 강의를 기수/기간별로 다시 열 수 있다고 판단했습니다.
- 결제 확정은 사용자 직접 호출이 아니라 외부 PG callback으로 처리한다고 가정했습니다.
- 인증/인가는 간략화하여 `X-USER-ID` header로 처리합니다.
- H2에서는 실제 replication이 불가능하므로 datasource routing 구조만 검증합니다.

## 설계 결정과 이유

### CQRS 수준의 패키지 분리

완전한 이벤트 소싱 CQRS는 과제 범위에 비해 과하다고 판단했습니다. 대신 command/read controller, service, repository를 패키지로 분리해 쓰기와 조회 책임을 명확히 했습니다.

### JPA FK 사용

`Klass`와 `Enrollment`는 같은 bounded context 안의 강한 관계입니다. 수강 신청은 반드시 하나의 강의에 속하고, 신청 생성 시 정원 증가와 신청 저장이 같은 트랜잭션에서 처리되므로 FK를 사용했습니다.

### 낙관적 락

정원 초과를 막기 위해 `Klass.version`에 `@Version`을 사용했습니다. 동시에 마지막 자리에 신청하는 경우 하나의 트랜잭션만 먼저 성공하고, 나머지는 optimistic lock 예외를 받습니다. 이 예외는 `EnrollmentFacade`에서 최대 3회 재시도합니다.

### 분산락 대신 DB 낙관적 락

Redis 분산락도 고려했지만 별도 인프라, 락 만료, 락 해제 실패 처리 부담이 있습니다. 이번 과제에서는 단일 `Klass` aggregate의 정원 변경이 핵심이므로 DB 낙관적 락을 기본으로 선택했습니다. 운영에서 특정 인기 강의 경합이 매우 크다면 Redis lock 또는 queue 기반 직렬 처리를 앞단에 추가할 수 있고, 그 경우에도 DB 낙관적 락은 최종 방어선으로 유지합니다.

### 멱등성 처리

결제 callback은 네트워크 문제로 중복 전달될 수 있습니다. `Idempotency-Key`와 request hash를 저장해 같은 요청은 한 번만 처리하고, 같은 key로 다른 payload가 오면 충돌로 처리합니다.

request hash:

```text
SHA-256(paymentId + ":" + enrollmentId + ":" + paidAmount)
```

SHA-256은 보안 목적의 암호화가 아니라 요청 동일성 비교를 위한 fingerprint로 사용합니다. 복호화하지 않고 같은 입력이면 같은 hash가 나온다는 성질만 사용합니다.

### Outbox 패턴

수강 신청, 결제 확정, 취소 후 알림이나 정산 시스템으로 이벤트를 보낼 수 있습니다. DB 변경 직후 외부 broker에 직접 발행하면 DB commit과 메시지 발행이 어긋날 수 있으므로, 같은 DB 트랜잭션 안에서 `OutboxEvent`를 저장합니다.

현재 구현은 외부 broker 없이 `NoopOutboxMessagePublisher`로 발행 성공을 시뮬레이션합니다. 운영 환경에서는 outbox relay가 Kafka 또는 RabbitMQ로 이벤트를 발행하고 broker ack 후 `PUBLISHED`로 변경하는 구조를 고려합니다.

### 읽기/쓰기 datasource routing

`@Transactional(readOnly = true)`이면 slave datasource, 일반 transaction이면 master datasource로 routing합니다. `LazyConnectionDataSourceProxy`를 사용해 transaction의 readOnly 정보가 확정된 뒤 실제 connection을 얻도록 했습니다.

## 테스트 실행 방법

```bash
./gradlew clean test
```

현재 테스트 범위:

- application context 로딩
- datasource routing
- Klass command/read service
- Klass controller
- Enrollment command/read service
- Enrollment controller
- optimistic lock retry facade
- 동시 수강 신청 concurrency
- payment callback/idempotency
- payment callback controller
- outbox publisher

## 선택 요구사항 구현 현황

- 수강 취소 시 취소 가능 기간 제한은 구현했습니다. `CONFIRMED` 신청은 결제 확정 후 7일 이내이고 강의 시작 전인 경우에만 취소할 수 있습니다.
- 강의별 수강생 목록 조회는 구현했습니다. `GET /api/klasses/{klassId}/enrollments`에서 `X-USER-ID` header로 요청자를 확인하고, 강의 생성자인 경우에만 조회할 수 있습니다.
- 신청 내역 페이지네이션은 구현했습니다. 사용자별 신청 내역과 강의별 수강생 목록 모두 Spring `Pageable`을 사용합니다.
- waitlist는 이번 구현 범위에서 제외했고, 제외 사유는 아래 제약사항에 정리했습니다.

## 미구현 / 제약사항

- waitlist는 선택 요구사항으로 이번 구현 범위에서 제외했습니다.
- waitlist를 구현하려면 정원 초과 시 자동 등록 여부, 취소 시 자동 승격 여부, 승격 후 결제 대기 만료 시간, 동시 승격 제어 정책을 추가로 정의해야 합니다.
- H2 환경에서는 실제 MySQL replication을 구성하지 않고 datasource routing 구조만 검증합니다.
- outbox publisher는 로컬에서 외부 broker 발행을 시뮬레이션합니다. 운영에서는 Kafka 또는 RabbitMQ relay 구현이 필요합니다.
- 인증/인가는 실제 토큰 기반 인증이 아니라 `X-USER-ID` header로 단순화했습니다. 별도 사용자 테이블은 없고, 요청 header의 양수 Long 값을 사용자 id로 사용합니다.
- 크리에이터 전용 기능은 별도 role 없이 강의의 `creatorId`와 요청자의 `X-USER-ID`가 일치하는지로 판단합니다.
- 강의 수정 API는 과제 필수 범위에 명시되지 않아 구현하지 않았습니다.

## AI 활용 범위

AI는 요구사항을 기능 단위로 나누고, 구현 순서와 테스트 범위를 정리하는 보조 도구로 활용했습니다. 코드 작성 중에는 예외 케이스 누락 여부, 동시성 제어 방식, 결제 callback 멱등성 처리, outbox 적용 범위를 점검하는 데 사용했습니다.

구체적으로는 다음 작업에 활용했습니다.

- 요구사항을 강의, 수강 신청, 결제 확정, 취소, 조회, outbox, 동시성 제어 항목으로 분해
- 구현 순서와 테스트 케이스 초안 정리
- 낙관적 락, 비관적 락, 분산락 등 동시성 제어 방식 비교
- 결제 callback 멱등성 처리와 request hash 검증 방식 검토
- README 초안 작성과 설명 항목 누락 여부 점검

AI가 제안한 내용을 그대로 사용하지 않고, 실제 과제 범위와 구현 복잡도를 비교해 필요한 부분만 선택했습니다. 예를 들어 waitlist, 실제 PG 연동, Kafka/RabbitMQ relay, MySQL replication 구성은 구현 범위에서 제외하고 제약사항 또는 운영 고려사항으로만 정리했습니다.

최종 코드 구조, 도메인 정책, API 응답 형태, 테스트 통과 여부는 직접 확인하고 조정했습니다.
