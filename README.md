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
- MySQL Driver
- Gradle
- JUnit 5, Spring Boot Test, MockMvc

## 실행 방법

### 로컬 실행

```bash
./gradlew bootRun
```

기본 실행 포트는 Spring Boot 기본값인 `8080`입니다.

```text
http://localhost:8080
```

### DB 설정

기본 설정은 H2 in-memory DB입니다.

```properties
app.datasource.master.url=jdbc:h2:mem:liveklass;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
app.datasource.slave.url=jdbc:h2:mem:liveklass;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
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

사용자 식별은 별도 인증 시스템 대신 `X-USER-ID` header를 사용합니다.

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

운영 MySQL replication 적용 시 권장 순서:

1. primary/replica 인스턴스 준비
2. primary `server-id`, `log-bin`, `binlog_format` 등 binlog 설정
3. replication 전용 계정 생성 및 권한 부여
4. primary 기준 데이터 snapshot 또는 dump 생성
5. replica에 초기 데이터 복원
6. replica에서 primary host, log file, position 또는 GTID 설정
7. replication start 후 lag와 상태 확인
8. application datasource를 primary/master, replica/slave로 연결
9. schema migration은 primary에 먼저 적용하고 replica 반영 상태를 확인한 뒤 application 배포

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

마지막 확인 결과:

```text
BUILD SUCCESSFUL
테스트 메서드 45개
```

## 미구현 / 제약사항

- waitlist는 선택 요구사항으로 이번 구현 범위에서 제외했습니다.
- waitlist를 구현하려면 정원 초과 시 자동 등록 여부, 취소 시 자동 승격 여부, 승격 후 결제 대기 만료 시간, 동시 승격 제어 정책을 추가로 정의해야 합니다.
- H2 환경에서는 실제 MySQL replication을 구성하지 않고 datasource routing 구조만 검증합니다.
- outbox publisher는 로컬에서 외부 broker 발행을 시뮬레이션합니다. 운영에서는 Kafka 또는 RabbitMQ relay 구현이 필요합니다.
- 인증/인가는 실제 토큰 기반 인증이 아니라 `X-USER-ID` header로 단순화했습니다.
- 강의 수정 API는 과제 필수 범위에 명시되지 않아 구현하지 않았습니다.

## AI 활용 범위

AI는 요구사항 정리, 설계 대안 비교, 구현 순서 정리, 코드 초안 작성, 테스트 케이스 도출에 활용했습니다.

AI가 제안한 내용을 그대로 사용하지 않고, 구현 중 아래 항목들을 직접 비교하고 조정했습니다.

설계 판단:

- 강의 엔티티명을 `Class`, `Lecture`, `CourseClass`, `Klass` 중 비교했고, Java 예약어 충돌을 피하면서 프로젝트명과도 맞는 `Klass`를 선택했습니다.
- `DRAFT` 상태는 임시 저장/초안 상태로 해석했고, 신청 불가 상태를 명확히 표현하기 위해 유지했습니다.
- `Klass`와 `Enrollment`는 같은 bounded context 안의 강한 관계라고 판단해 FK를 사용했습니다. 단, cascade는 사용하지 않아 불필요한 전파를 막았습니다.
- 강의 제목 중복은 제한하지 않기로 했습니다. 같은 강의가 기수나 기간별로 반복 개설될 수 있다고 봤기 때문입니다.
- 정원은 결제 확정 시점이 아니라 `PENDING` 신청 생성 시점에 점유하도록 정했습니다. 신청 성공 후 결제 확정에서 정원 초과가 발생하는 흐름을 피하기 위한 선택입니다.
- 대기열은 선택 요구사항이지만 자동 등록, 자동 승격, 결제 대기 만료, 동시 승격 제어 등 추가 정책이 필요하다고 판단해 이번 범위에서는 제외했습니다.

동시성 및 운영 고려:

- 정원 제어 방식은 비관적 락, 낙관적 락, Redis 분산락을 비교했고, 단일 `Klass` aggregate의 짧은 정원 변경에는 `@Version` 기반 낙관적 락이 적절하다고 판단했습니다.
- 낙관적 락 재시도는 같은 트랜잭션 안에서 반복하면 의미가 없으므로, transaction 바깥의 `EnrollmentFacade`에서 재시도하도록 조정했습니다.
- Redis 분산락은 인기 강의처럼 경합이 큰 운영 환경에서 고려할 수 있지만, Redis 장애나 lease 만료 상황에 대비해 DB 낙관적 락은 최종 방어선으로 유지하는 방향으로 정리했습니다.
- read/write datasource routing은 `@Transactional(readOnly = true)` 기준으로 구현했고, connection 획득 시점을 늦추기 위해 `LazyConnectionDataSourceProxy`를 사용했습니다.
- H2에서는 실제 replica 구성이 불가능하므로 master/slave datasource가 같은 DB를 보게 하고 routing 동작만 테스트했습니다.
- 운영 MySQL replication 적용 시 replication 초기화, 계정 생성, binlog 설정, replica 연결, schema migration 순서를 README에 남겼습니다.

코드 구조 판단:

- command/read controller, service, repository를 분리했지만, 완전한 이벤트 소싱 CQRS는 과하다고 판단해 적용하지 않았습니다.
- repository는 도메인 내부에 두고, command repository는 Spring Data JPA, read repository는 QueryDSL DTO projection을 사용하도록 나눴습니다.
- controller DTO는 request/response로 분리하고, service DTO는 내부 입력/조회 모델로 사용했습니다. 조회용 `KlassDto`를 생성/update 입력으로 재사용하지 않도록 했습니다.
- 목록/상세 DTO를 무조건 API별로 나누지 않고, service 단에서는 `KlassDto` 같은 도메인별 조회 DTO를 기본으로 사용했습니다.
- entity에는 setter와 public builder를 열지 않고 `Klass.create(...)`, `Enrollment.pending(...)` 같은 정적 팩토리로 생성 경로를 제한했습니다.
- `validateEnrollable`, `validateOwner`처럼 entity 자신의 상태를 판단하는 로직은 entity 안에 두고, DB 조회가 필요한 중복 신청 검증은 service에서 처리했습니다.
- 한 번만 쓰이고 흐름을 더 복잡하게 만드는 private method는 인라인으로 정리했습니다.
- enum의 화면 표시값과 `FORM_OPTIONS`, active 상태 그룹은 enum 내부에 두어 controller의 `/form` API와 상태 그룹 판단에서 재사용했습니다.
- 취소 가능 기간 `7일` 정책은 entity 내부 상수로 숨기기보다 `EnrollmentCancellationPolicy`로 분리했습니다.

결제, 멱등성, outbox 판단:

- 결제 확정은 사용자가 직접 호출하는 API가 아니라 외부 PG callback으로 해석했습니다.
- `Idempotency-Key` header가 있으면 우선 사용하고, PG사가 제공하지 않는 경우를 고려해 `"payment:" + paymentId`를 fallback key로 사용했습니다.
- request hash는 `SHA-256(paymentId + ":" + enrollmentId + ":" + paidAmount)`로 만들었습니다. 복호화 목적이 아니라 같은 요청인지 비교하기 위한 fingerprint로 사용했습니다.
- 같은 idempotency key에 다른 payload가 들어오면 충돌로 처리하고, 같은 payload가 다시 들어오면 멱등하게 성공 처리하도록 구현했습니다.
- outbox 패턴은 DB 상태 변경과 외부 이벤트 발행의 불일치를 막기 위해 적용했습니다. 현재는 외부 broker 없이 DB outbox와 publisher 상태 전이까지만 구현하고, 운영에서는 Kafka/RabbitMQ relay를 고려한다고 정리했습니다.

검증 과정:

- datasource routing 테스트로 readOnly transaction이 slave datasource를 선택하는지 확인했습니다.
- service 테스트로 상태 전이, 정원 증가/복구, 중복 신청 방지, 취소 기간 제한을 검증했습니다.
- controller 테스트로 request validation, header 기반 사용자 식별, 응답 DTO 변환을 확인했습니다.
- 결제 callback 테스트로 정상 확정, 멱등 재호출, key 충돌, 처리 중 요청, 금액 불일치, fallback key를 검증했습니다.
- concurrency 테스트로 동시에 여러 사용자가 마지막 자리에 신청해도 capacity를 초과하지 않는지 확인했습니다.
- outbox publisher 테스트로 `PENDING -> PUBLISHED` 상태 전이와 retry 대상 조회 조건을 검증했습니다.

구현 후 `./gradlew clean test`로 검증했고, 테스트 결과와 남은 제약사항을 README에 함께 기록했습니다.
