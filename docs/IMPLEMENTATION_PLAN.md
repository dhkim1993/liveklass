# LiveKlass 구현 계획

## 목표

과제 A인 수강 신청 시스템을 Spring Boot 기반으로 구현한다.

핵심 구현 범위는 강의 관리, 수강 신청, 결제 확정, 취소, 정원 관리, 동시성 제어다. 여기에 실무 운영 흐름을 고려해 읽기/쓰기 DB 라우팅, 외부 결제 콜백 멱등성, DB outbox 패턴까지 제한적으로 포함한다.

최종 제출물은 `main` 브랜치에서 실행 가능해야 하며, 소스 코드, 테스트 코드, API 예시, 데이터 모델 설명, 실행 방법, AI 활용 범위를 `README.md`에 포함한다.

## 내가 고민한 부분과 판단

과제 안내에 AI 사용이 가능하다고 되어 있지만, 그대로 복사한 결과물이 아니라 본인의 판단과 검증 결과가 반영되어야 한다고 명시되어 있다. 그래서 구현 전 설계 단계에서 아래 항목들을 직접 비교하고 선택했다.

### 1. 엔티티 이름

강의 엔티티를 `Class`로 만들면 Java 예약어와 충돌한다. `Lecture`, `CourseClass`, `Klass` 중 고민했고, 프로젝트명이 `liveklass`인 점과 코드에서 짧게 읽히는 점을 고려해 `Klass`로 정했다.

### 2. Klass와 Enrollment의 FK 처리

운영 환경에서는 약한 결합을 위해 FK 없이 id 값만 저장하는 방식도 고려할 수 있다. 하지만 이번 과제에서는 `Klass`와 `Enrollment`가 같은 수강 신청 bounded context 안에 있고, 수강 신청은 반드시 하나의 강의에 속해야 한다.

또한 신청 생성 시 `Klass.enrolledCount`를 증가시키고 `Enrollment`를 생성하는 작업이 같은 트랜잭션에서 처리된다. 따라서 두 엔티티의 관계를 DB 차원에서도 명확히 보장하는 것이 요구사항과 데이터 모델 설명에 더 적합하다고 판단했다.

그래서 `Enrollment`는 `klass_id` FK를 통해 `Klass`와 연관관계를 갖도록 설계했다. 단, 불필요한 전파를 막기 위해 cascade는 사용하지 않고, 조회 성능을 위해 `FetchType.LAZY`를 사용한다.

### 3. 정원 차감 시점

정원을 결제 확정 시점에 차감하면 결제 대기 중인 사용자가 많을 때 마지막 결제 시점에 정원 초과가 발생할 수 있다. 사용자가 신청에 성공했는데 결제 확정에서 실패하는 흐름은 사용자 경험과 도메인 규칙이 복잡해진다.

따라서 `PENDING` 신청 생성 시점에 정원을 점유하도록 정했다. `PENDING -> CONFIRMED`에서는 정원 변화가 없고, `PENDING/CONFIRMED -> CANCELLED`에서 정원을 복구한다.

### 4. 동시성 제어 방식

정원 초과를 막기 위해 비관적 락과 낙관적 락을 비교했다. 이번 과제는 수강 신청이라는 짧은 트랜잭션에서 같은 강의의 마지막 자리 경합을 처리하는 것이 핵심이므로, `Klass`에 `@Version`을 두는 낙관적 락을 선택했다.

낙관적 락 충돌은 실패로 바로 끝내지 않고 제한 횟수만큼 재시도한다. 단, 재시도는 같은 트랜잭션 안에서 하면 의미가 없으므로 facade에서 반복하고 command service가 매번 새 트랜잭션을 열도록 설계한다.

Redis 기반 분산락도 고려했지만, 별도 인프라와 락 만료/해제 실패 처리 부담이 생긴다. 이번 과제의 정원 관리는 단일 `Klass` aggregate의 `enrolledCount` 변경이므로 JPA `@Version` 기반 낙관적 락과 재시도를 기본 구현으로 선택했다. 다만 특정 인기 강의처럼 경합이 매우 큰 운영 환경에서는 Redis 분산락 또는 queue 기반 직렬 처리를 앞단에 추가해 요청을 직렬화할 수 있다. 이 경우에도 Redis 장애, lease time 만료, 락 해제 실패 같은 상황에 대비해 DB의 낙관적 락과 중복 신청 방지 제약은 최종 정합성 방어선으로 유지한다.

### 5. CQRS 분리 수준

과제 규모에서 완전한 이벤트 소싱 기반 CQRS는 과하다. 대신 controller, service, repository naming을 command/query로 분리하고, 쓰기 로직과 조회 로직의 책임을 나눠 가독성과 테스트 경계를 분명히 한다.

### 6. H2에서 master/slave 라우팅

H2는 실제 MySQL primary/replica 같은 복제 구성을 제공하지 않는다. 그래서 로컬 H2에서는 master datasource와 slave datasource가 같은 DB URL을 바라보게 하고, `@Transactional(readOnly = true)`에 따라 라우팅되는 구조만 검증한다.

운영 환경에서는 master datasource를 MySQL primary에, slave datasource를 MySQL replica에 연결하는 것으로 확장한다.

### 7. 결제 확정 API

과제에서는 외부 결제 연동이 필수는 아니지만, 실제 서비스에서는 사용자가 직접 결제 확정 API를 호출하기보다 결제 시스템이 콜백을 보내는 구조가 일반적이다. 그래서 결제 확정은 사용자 API가 아니라 외부 결제 콜백 API로 분리한다.

콜백은 네트워크 문제로 중복 전달될 수 있으므로 `Idempotency-Key`와 request hash를 저장해 같은 요청은 같은 응답을 반환하고, 같은 key로 다른 payload가 들어오면 충돌로 처리한다.

### 8. 결제 콜백 request hash

`Idempotency-Key`만 저장하면 같은 key로 다른 payload가 들어오는 경우를 구분하기 어렵다. 그래서 `paymentId`, `enrollmentId`, `paidAmount`를 조합한 문자열의 SHA-256 값을 `requestHash`로 저장한다.

SHA-256은 복호화하려는 목적이 아니라 요청 동일성 비교를 위한 fingerprint로 사용한다. 같은 요청이면 항상 같은 hash가 나오고, 금액이나 결제 식별자가 달라지면 다른 hash가 나오므로 같은 key의 잘못된 재사용을 감지할 수 있다.

### 9. PG사가 Idempotency-Key를 제공하지 않는 경우

모든 PG사가 `Idempotency-Key` 헤더를 제공한다고 가정할 수는 없다. 이 경우에는 PG가 보장하는 고유 결제 식별자인 `paymentId`를 fallback key로 사용한다.

구현에서는 `Idempotency-Key` 헤더가 있으면 우선 사용하고, 없으면 `"payment:" + paymentId`를 멱등성 key로 사용한다. 단, 이 정책은 `paymentId`가 PG 시스템 안에서 전역적으로 유일하고 변하지 않는다는 전제를 둔다.

### 10. 시간 처리 방식

취소 가능 기간은 `confirmedAt` 기준 7일 이내, 강의 시작 전이라는 시간 기반 규칙을 가진다. 서비스 코드에서 `LocalDateTime.now()`를 직접 호출하면 테스트가 실제 현재 시간에 의존하게 된다.

그래서 `Clock`을 주입해 운영에서는 실제 시간을 사용하고, 테스트에서는 고정된 시간을 주입해 경계값을 검증할 수 있도록 한다.

### 11. LazyConnectionDataSourceProxy 사용 이유

read/write datasource 라우팅은 현재 트랜잭션의 `readOnly` 값을 기준으로 결정한다. 그런데 커넥션을 너무 일찍 가져오면 트랜잭션의 readOnly 정보가 확정되기 전에 datasource가 선택될 수 있다.

이를 피하기 위해 `LazyConnectionDataSourceProxy`로 실제 커넥션 획득 시점을 늦춘다. 이렇게 하면 `@Transactional(readOnly = true)`가 적용된 query service는 slave datasource로, 일반 command service는 master datasource로 라우팅되는 구조를 안정적으로 검증할 수 있다.

### 12. Outbox 적용 범위

상태 변경 후 알림, 정산, 메시지 발행 같은 후속 처리를 바로 외부 브로커에 보내면 DB 트랜잭션 성공과 메시지 발행 성공이 어긋날 수 있다. 이를 막기 위해 도메인 상태 변경과 outbox 저장을 같은 DB 트랜잭션에서 처리한다.

이번 과제에서는 Kafka를 직접 붙이지 않고 DB outbox와 scheduler 기반 publisher까지만 구현한다. 운영 환경에서는 outbox relay가 Kafka 또는 RabbitMQ로 이벤트를 발행하는 구조를 고려했다고 README에 명시한다.

### 13. AI 활용 범위

AI는 요구사항 정리, 설계 대안 비교, 구현 계획 초안 작성에 활용했다. 최종 설계는 위 항목처럼 직접 판단한 기준에 따라 조정했고, 구현 후에는 테스트 코드와 실행 결과로 검증한다.

README에는 AI 활용 범위를 간단히 적되, 어떤 부분을 직접 판단하고 검증했는지 함께 남긴다.

## 확정 기술 스택

- 언어: Java
- 프레임워크: Spring Boot
- 영속성: Spring Data JPA
- 동적 조회: QueryDSL
- 로컬 DB: H2
- 트랜잭션 라우팅: `@Transactional(readOnly = true/false)` 기반 datasource routing
- 커넥션 프록시: `LazyConnectionDataSourceProxy`
- 시간 처리: `Clock` 주입
- 테스트 DB: H2

## 도메인 모델

### Klass

`Klass`는 강의를 의미한다. Java 예약어인 `Class`와 충돌하지 않도록 `Klass`라는 이름을 사용한다.

필드:

- `id`
- `creatorId`
- `title`
- `description`
- `price`
- `capacity`
- `enrolledCount`
- `startDate`
- `endDate`
- `status`
- `version`
- `createdAt`
- `updatedAt`

상태:

- `DRAFT`: 초안, 신청 불가
- `OPEN`: 모집 중, 신청 가능
- `CLOSED`: 모집 마감, 신청 불가

상태 전이:

```text
DRAFT -> OPEN -> CLOSED
```

### Enrollment

`Enrollment`는 사용자의 수강 신청을 의미한다.

필드:

- `id`
- `klass`
- `userId`
- `status`
- `confirmedAt`
- `cancelledAt`
- `createdAt`
- `updatedAt`

상태:

- `PENDING`: 신청 완료, 결제 대기
- `CONFIRMED`: 결제 완료, 수강 확정
- `CANCELLED`: 취소됨

상태 전이:

```text
없음 -> PENDING
PENDING -> CONFIRMED
PENDING -> CANCELLED
CONFIRMED -> CANCELLED
```

`CANCELLED`는 최종 상태로 본다.

## 정원 관리 정책

수강 신청이 `PENDING`으로 생성되는 시점에 정원을 점유한다.

```text
PENDING 생성:
  Klass.enrolledCount + 1

PENDING -> CONFIRMED:
  정원 변화 없음

PENDING -> CANCELLED:
  Klass.enrolledCount - 1

CONFIRMED -> CANCELLED:
  Klass.enrolledCount - 1

CANCELLED -> CANCELLED:
  멱등 성공, 정원 변화 없음
```

`Klass.enrolledCount`는 활성 신청 수를 의미하며, `PENDING + CONFIRMED`를 포함한다.

## 취소 정책

```text
PENDING:
  언제든 취소 가능

CONFIRMED:
  confirmedAt 기준 7일 이내 취소 가능
  Klass.startDate 전까지만 취소 가능

CANCELLED:
  반복 취소 요청은 상태 변경 없이 성공 응답
```

현재 시간이 필요한 로직은 `LocalDateTime.now()`를 직접 호출하지 않고 `Clock`을 주입해서 사용한다.

## 동시성 제어 정책

정원 관리는 낙관적 락으로 제어한다.

- `Klass`에 `@Version`을 추가한다.
- 신청 트랜잭션 안에서 `Klass.enrolledCount`를 증가시킨다.
- 동시에 같은 강의에 신청하면 하나의 트랜잭션만 먼저 커밋되고, 나머지는 optimistic lock 예외를 받는다.
- optimistic lock 예외는 facade 계층에서 제한 횟수만큼 재시도한다.
- 각 재시도는 반드시 새 트랜잭션에서 실행되어야 한다.

구조:

```text
EnrollmentFacade
  - @Transactional 없음
  - optimistic lock 재시도 루프 담당

EnrollmentCommandService
  - @Transactional
  - 실제 쓰기 로직 담당
```

최대 재시도 횟수를 초과하면 `409 CONCURRENCY_CONFLICT`를 반환한다.

## CQRS 구조

완전한 이벤트 소싱 CQRS가 아니라, 쓰기와 조회 책임을 코드 구조에서 분리한다.

예상 패키지:

```text
config
common
klass
  domain
    enums
  commandcontroller
  commandservice
  commandrepository
  readcontroller
  readservice
  readrepository
enrollment
  domain
    enums
  commandcontroller
  commandservice
  commandrepository
  readcontroller
  readservice
  readrepository
exception
```

Command service:

```java
@Transactional
```

Query service:

```java
@Transactional(readOnly = true)
```

## Datasource 라우팅

트랜잭션의 read-only 여부에 따라 master/slave datasource를 선택한다.

구성 요소:

- `RoutingDataSource extends AbstractRoutingDataSource`
- `TransactionSynchronizationManager.isCurrentTransactionReadOnly()`
- `LazyConnectionDataSourceProxy`

라우팅 규칙:

```text
readOnly = true  -> SLAVE
readOnly = false -> MASTER
```

로컬 H2:

- `masterDataSource`와 `slaveDataSource`가 같은 H2 DB를 바라본다.
- 실제 복제는 없지만, read/write 라우팅 구조를 검증할 수 있다.

운영 MySQL:

- `masterDataSource`는 MySQL primary를 바라본다.
- `slaveDataSource`는 MySQL replica를 바라본다.

## 외부 결제 콜백과 멱등성

결제 확정은 사용자 API가 아니라 외부 결제 시스템의 콜백으로 처리한다.

Endpoint:

```text
POST /api/payment-events/confirm
Idempotency-Key: pay_evt_20260709_001
```

Request:

```json
{
  "paymentId": "pay_20260709_001",
  "enrollmentId": 1,
  "paidAmount": 50000
}
```

처리 흐름:

```text
1. Idempotency-Key 헤더를 검증한다. 헤더가 없으면 paymentId 기반 fallback key를 사용한다.
2. paymentId, enrollmentId, paidAmount로 request hash를 만든다.
3. 같은 key가 이미 있는데 request hash가 다르면 409로 거절한다.
4. 같은 key가 있고 COMPLETED 상태이며 hash도 같으면 저장된 응답을 반환한다.
5. 새로운 key면 PROCESSING 상태로 저장한다.
6. Enrollment를 조회한다.
7. paidAmount가 Klass.price와 같은지 검증한다.
8. Enrollment를 PENDING에서 CONFIRMED로 변경한다.
9. ENROLLMENT_CONFIRMED outbox event를 저장한다.
10. 멱등성 응답을 저장하고 COMPLETED 상태로 변경한다.
```

Request hash:

```text
SHA-256(paymentId + ":" + enrollmentId + ":" + paidAmount)
```

멱등성 key:

```text
Idempotency-Key 헤더가 있으면:
  header value

Idempotency-Key 헤더가 없으면:
  "payment:" + paymentId
```

멱등성 상태:

- `PROCESSING`
- `COMPLETED`
- `FAILED`

충돌 케이스:

```text
같은 key + 다른 hash:
  409 IDEMPOTENCY_KEY_CONFLICT

같은 key + PROCESSING:
  409 IDEMPOTENCY_REQUEST_PROCESSING
```

## Outbox 패턴

도메인 변경과 이벤트 기록을 같은 DB 트랜잭션으로 묶기 위해 DB outbox를 사용한다.

Outbox event:

- `ENROLLMENT_CREATED`
- `ENROLLMENT_CONFIRMED`
- `ENROLLMENT_CANCELLED`

필드:

- `id`
- `aggregateType`
- `aggregateId`
- `eventType`
- `payload`
- `status`
- `retryCount`
- `nextRetryAt`
- `occurredAt`
- `publishedAt`

상태:

- `PENDING`
- `PUBLISHED`
- `FAILED`

동작:

```text
1. 도메인 변경과 같은 트랜잭션에서 outbox event를 저장한다.
2. Scheduler가 발행 가능한 PENDING 이벤트를 조회한다.
3. Publisher가 이벤트 발행을 시뮬레이션한다.
4. 성공하면 PUBLISHED로 변경한다.
5. 실패하면 retryCount를 증가시키고 nextRetryAt을 갱신한다.
```

운영 고려:

- Kafka 또는 RabbitMQ를 downstream broker로 붙일 수 있다.
- 그래도 DB outbox는 안정적인 이벤트 발행의 기준 데이터로 유지한다.
- outbox relay가 outbox row를 Kafka로 발행하고 broker ack를 받은 뒤 `PUBLISHED`로 변경하는 구조를 고려한다.

## API 계획

### Klass Command

```text
POST  /api/klasses
PATCH /api/klasses/{klassId}/open
PATCH /api/klasses/{klassId}/close
```

### Klass Query

```text
GET /api/klasses?status=OPEN&page=0&size=20
GET /api/klasses/{klassId}
```

### Enrollment Command

```text
POST /api/klasses/{klassId}/enrollments
POST /api/enrollments/{enrollmentId}/cancel
```

### Payment Callback

```text
POST /api/payment-events/confirm
```

### Enrollment Query

```text
GET /api/users/{userId}/enrollments?page=0&size=20
GET /api/klasses/{klassId}/enrollments?page=0&size=20
```

## 인증/인가 단순화

전체 인증 시스템은 구현하지 않고 `X-USER-ID` 헤더로 사용자를 식별한다.

규칙:

```text
Klass 생성:
  X-USER-ID를 creatorId로 사용

Klass open/close:
  creatorId와 X-USER-ID가 같아야 함

Enrollment 생성:
  X-USER-ID를 userId로 사용

Enrollment 취소:
  enrollment.userId와 X-USER-ID가 같아야 함

내 신청 목록 조회:
  path userId와 X-USER-ID가 같아야 함

강의별 수강생 목록 조회:
  klass.creatorId와 X-USER-ID가 같아야 함

Payment callback:
  외부 시스템 콜백으로 보고 X-USER-ID를 요구하지 않음
```

## 예외 처리

커스텀 예외와 공통 예외 응답을 사용한다.

```text
LiveKlassException
ErrorCode
GlobalExceptionHandler
```

에러 응답:

```json
{
  "code": "KLASS_NOT_OPEN",
  "message": "모집 중인 강의만 신청할 수 있습니다."
}
```

초기 에러 코드:

- `KLASS_NOT_FOUND`
- `ENROLLMENT_NOT_FOUND`
- `KLASS_NOT_OPEN`
- `KLASS_ALREADY_OPEN`
- `KLASS_ALREADY_CLOSED`
- `CAPACITY_EXCEEDED`
- `DUPLICATE_ACTIVE_ENROLLMENT`
- `INVALID_ENROLLMENT_STATUS`
- `CANCEL_PERIOD_EXPIRED`
- `FORBIDDEN_KLASS_ACCESS`
- `CONCURRENCY_CONFLICT`
- `INVALID_PAYMENT_AMOUNT`
- `IDEMPOTENCY_KEY_CONFLICT`
- `IDEMPOTENCY_REQUEST_PROCESSING`
- `INVALID_REQUEST`

## 테스트 계획

### 도메인 테스트

- Klass는 생성 시 `DRAFT` 상태다.
- Klass는 `DRAFT -> OPEN -> CLOSED`로 전이할 수 있다.
- Enrollment 생성 시 정원을 점유한다.
- 결제 확정 시 정원은 변하지 않는다.
- 취소 시 정원은 한 번만 복구된다.
- 반복 취소는 멱등하게 성공한다.
- 확정된 신청은 강의 시작 전이면서 결제 확정 후 7일 이내 취소 가능하다.
- 취소 가능 기간이 지나면 취소할 수 없다.
- 강의 시작 이후에는 취소할 수 없다.

### 서비스 통합 테스트

- OPEN 강의 신청 시 `PENDING` 신청이 생성된다.
- DRAFT/CLOSED 강의 신청은 실패한다.
- 정원 초과 신청은 실패한다.
- 같은 사용자의 활성 신청 중복은 실패한다.
- 취소 이력만 있는 경우 재신청할 수 있다.
- 결제 콜백으로 신청이 확정된다.
- 결제 금액이 다르면 실패한다.
- 같은 멱등성 key와 같은 요청은 저장된 응답을 반환한다.
- 같은 멱등성 key와 다른 요청은 거절한다.
- `Idempotency-Key` 헤더가 없으면 `paymentId` fallback key로 처리한다.
- 도메인 변경 시 outbox event가 저장된다.

### API 테스트

- 주요 command/query API가 기대한 응답과 HTTP status를 반환한다.
- 에러 응답은 공통 포맷을 사용한다.
- `X-USER-ID` 기반 권한 검증이 적용된다.

### 동시성 테스트

- capacity가 1인 강의에 여러 사용자가 동시에 신청해도 성공한 활성 신청은 1개만 존재한다.
- 최종 `Klass.enrolledCount`가 capacity를 넘지 않는다.
- optimistic lock 재시도 경로가 동작한다.

### Outbox 테스트

- 발행 가능한 outbox event는 발행 성공 후 `PUBLISHED`로 변경된다.
- 발행 실패 시 `retryCount`가 증가하고 `nextRetryAt`이 갱신된다.

## README 필수 포함 항목

최종 `README.md`에는 아래 항목을 포함한다.

- 프로젝트 개요
- 기술 스택
- 실행 방법
- API 목록 및 예시
- 데이터 모델 설명
- 요구사항 해석 및 가정
- 설계 결정과 이유
- 테스트 실행 방법
- 미구현 / 제약사항
- AI 활용 범위
- H2 로컬 datasource routing 동작 방식
- 운영 MySQL replication 고려사항
- 운영 Kafka/RabbitMQ outbox relay 고려사항

## 운영 MySQL Replication 고려사항

README에는 운영 적용 시 아래 순서를 설명한다.

```text
1. Primary MySQL을 초기화한다.
2. primary에 고유한 server-id를 설정한다.
3. binary log를 활성화한다.
4. 애플리케이션 DB를 생성한다.
5. replication 전용 계정을 생성한다.
6. replication 권한을 부여한다.
7. Replica MySQL을 primary와 다른 server-id로 초기화한다.
8. replica에 read_only를 활성화한다.
9. replica가 primary를 source로 바라보도록 연결한다.
10. replication을 시작한다.
11. replication 상태를 확인한다.
12. schema migration은 primary에만 실행한다.
13. replica가 migration을 따라잡은 뒤 read traffic을 라우팅한다.
14. 애플리케이션 master datasource는 primary로 설정한다.
15. 애플리케이션 slave datasource는 replica로 설정한다.
```

Migration 원칙:

- Flyway 또는 Liquibase는 primary에만 적용한다.
- DDL은 primary에서 실행되고 replica로 복제된다.
- replica에 migration이 반영되기 전에는 read traffic을 보내지 않는다.

일관성 원칙:

- 일반 조회는 replica를 사용할 수 있다.
- 쓰기 직후 강한 일관성이 필요한 조회는 master 사용을 검토한다.

## 구현 순서

```text
1. Spring Boot 프로젝트 구조를 생성한다.
2. JPA, QueryDSL, H2, validation 의존성을 추가한다.
3. 도메인 모델을 구현한다.
4. JPA repository와 QueryDSL query repository를 구현한다.
5. datasource routing과 Clock 설정을 구현한다.
6. 공통 예외 처리를 구현한다.
7. Klass command/read service를 구현한다.
8. Enrollment command/read service와 optimistic lock 재시도를 구현한다.
9. 도메인과 service 테스트를 작성한다.
10. outbox event 모델, repository, publisher 기본 흐름을 구현한다.
11. payment idempotency 모델과 결제 콜백 API를 구현한다.
12. command/read controller와 DTO를 구현한다.
13. API, 동시성, outbox 테스트를 작성한다.
14. README를 완성한다.
15. 전체 테스트를 실행한다.
```
