# LiveKlass 코드 컨벤션

## Entity

JPA entity는 아래 규칙을 기본으로 한다.

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExampleEntity {
}
```

- setter는 열지 않는다.
- 상태 변경은 의미 있는 도메인 메서드로 표현한다.
- JPA 기본 생성자는 `protected`로 제한한다.
- 생성이 필요한 경우 정적 팩토리 메서드를 우선 사용한다.
- entity에는 `@Builder`를 기본적으로 사용하지 않는다.
- 생성 규칙이 있는 entity는 `create(...)`, `pending(...)` 같은 이름 있는 정적 팩토리 메서드로 생성 경로를 제한한다.
- builder는 도메인 기본값이나 상태 전이를 우회할 수 있으므로, 정말 필요한 경우에만 접근 범위를 제한해서 사용한다.
- 생성 요청값의 null, blank, 범위 검증은 controller request DTO의 Bean Validation으로 처리한다.
- entity에는 상태 전이, 정원 관리, 취소 가능 기간 같은 도메인 규칙만 둔다.
- entity 자신의 필드 값을 비교하거나 검사하는 로직은 entity 안에 둔다.
- 예: `Klass.status`, `Klass.capacity`, `Klass.enrolledCount`, `Enrollment.status`, `Enrollment.confirmedAt`, `Enrollment.userId` 기반 검증.
- 취소 가능 기간처럼 값 자체가 정책으로 보이는 규칙은 entity 내부 상수로 숨기지 않고 도메인 정책 클래스로 분리할 수 있다.
- service는 entity의 getter를 조합해 상태를 판단하지 않고, `klass.open(...)`, `klass.incrementCapacity()`, `enrollment.cancel(...)`, `enrollment.validateOwner(...)` 같은 도메인 메서드를 호출한다.
- DB 조회가 필요한 검증, 외부 시스템 조회가 필요한 검증, 여러 aggregate를 조합해야 하는 검증은 service에서 처리한다.

## Package Structure

최상위 패키지는 공통 설정과 공통 모듈, 도메인별 모듈로 나눈다.

```text
com.liveklass
  config
  common
  klass
  enrollment
```

각 도메인 패키지 안에서는 command와 read 책임을 패키지명으로 분리한다.

```text
com.liveklass.klass
  domain
    enums
  facade
  commandcontroller
  commandservice
  commandrepository
  readcontroller
  readservice
  readrepository

com.liveklass.enrollment
  domain
    enums
  facade
  commandcontroller
  commandservice
  commandrepository
  readcontroller
  readservice
  readrepository
```

- `config`에는 QueryDSL, datasource routing, Clock 같은 애플리케이션 설정을 둔다.
- `common`에는 여러 도메인에서 공유하는 기반 클래스, 공통 예외, 공통 유틸리티, outbox 같은 공통 인프라를 둔다.
- 도메인 entity는 각 도메인의 `domain` 패키지에 둔다.
- 도메인 enum은 각 도메인의 `domain.enums` 패키지에 둔다.
- facade가 필요한 경우 service 패키지에 섞지 않고 도메인별 `facade` 패키지로 분리한다.
- facade는 재시도, 여러 service 조합, 외부 시스템 호출 흐름처럼 service 트랜잭션 경계 바깥에서 조율해야 하는 로직을 담당한다.
- 빈 디렉터리는 만들지 않고, 해당 계층 구현 시점에 패키지를 생성한다.

공통 outbox는 아래 구조를 사용한다.

```text
com.liveklass.common.outbox
  domain
    enums
  repository
  service
```

- 도메인 변경과 outbox event 저장은 같은 command transaction 안에서 처리한다.
- 로컬 구현의 publisher는 외부 브로커 대신 발행 성공/실패 상태 전이만 담당한다.
- 운영 환경에서는 outbox relay가 Kafka 또는 RabbitMQ로 이벤트를 발행하는 구조를 고려한다.

## Index

조회 조건과 권한 확인에 자주 쓰이는 컬럼은 entity의 `@Table(indexes = ...)`에 명시한다.

예:

```java
@Table(
    name = "klasses",
    indexes = {
        @Index(name = "idx_klasses_status", columnList = "status"),
        @Index(name = "idx_klasses_creator_status", columnList = "creator_id,status")
    }
)
```

- 상태 필터 조회가 있으면 status index를 검토한다.
- 사용자별/생성자별 조회가 있으면 user id 또는 creator id index를 검토한다.
- 복합 인덱스는 실제 query 조건 순서를 기준으로 둔다.

## Enum

도메인 enum은 도메인별 `enums` 패키지에 둔다.

예:

```text
com.liveklass.klass.domain.enums.KlassStatus
com.liveklass.enrollment.domain.enums.EnrollmentStatus
```

enum에는 프론트엔드 폼이나 API 응답에서 사용할 수 있는 한국어 표시값을 함께 둔다.

```java
@Getter
@RequiredArgsConstructor
public enum ExampleStatus {
    READY("준비"),
    DONE("완료");

    public static final Map<String, String> FORM_OPTIONS = ...
    public static final List<ExampleStatus> ACTIVE_STATUSES = List.of(READY);

    private final String label;

    public boolean isActive() {
        return ACTIVE_STATUSES.contains(this);
    }
}
```

- enum name은 영문 code로 사용한다.
- `label`은 화면 표시용 한국어 값으로 사용한다.
- `FORM_OPTIONS`는 프론트엔드가 선택 가능한 enum 값을 구성할 수 있도록 제공한다.
- `FORM_OPTIONS`는 enum 선언 순서가 유지되는 `LinkedHashMap` 기반 불변 map으로 만든다.
- `PENDING`, `CONFIRMED`처럼 여러 상태를 하나의 의미로 묶어 반복 사용하는 경우 service에 흩어두지 않고 enum 내부 상수로 둔다.
- 같은 상태 그룹 판정이 반복되면 `isActive()` 같은 enum 메서드를 추가해 호출부가 상태 조합을 직접 알지 않도록 한다.

## Repository

repository는 최상위 공통 `repository` 패키지로 분리하지 않고, 각 도메인 패키지 내부에 둔다.

예:

```text
com.liveklass.klass.commandrepository
com.liveklass.klass.readrepository
com.liveklass.enrollment.commandrepository
com.liveklass.enrollment.readrepository
```

CUD(create, update, delete)는 Spring Data JPA repository가 담당한다.

```java
public interface KlassRepository extends JpaRepository<Klass, Long> {
}
```

조회는 read 전용 repository interface와 QueryDSL 기반 `Impl` 클래스로 분리한다.

```java
public interface KlassReadRepository {
    Page<KlassDto> findPage(KlassStatus status, Pageable pageable);
}

@Repository
@RequiredArgsConstructor
public class KlassReadRepositoryImpl implements KlassReadRepository {
}
```

- CUD repository에는 단순 저장, 삭제, 단건 조회, exists 계열만 둔다.
- 복잡한 목록 조회와 검색 조건은 read repository에서 처리한다.
- read repository 구현체는 조회만 담당하고 entity 상태를 변경하지 않는다.
- 목록/검색처럼 화면 응답에 필요한 필드가 정해진 조회는 QueryDSL DTO projection으로 바로 조회한다.
- read service는 read repository에서 조회한 DTO를 그대로 반환하거나, 필요한 권한 검증만 추가로 수행한다.
- QueryDSL에서 entity를 반환하고 연관 엔티티가 응답에 필요하면 `fetchJoin`을 적용해 N+1을 방지한다.
- DTO projection 조회는 필요한 컬럼을 직접 select하므로 `fetchJoin`을 사용하지 않고 일반 join을 사용한다.
- 페이징 쿼리에서는 `OneToMany`, `ManyToMany` 같은 컬렉션 fetch join을 사용하지 않는다.
- count query에는 불필요한 `fetchJoin`을 사용하지 않는다.

## Service DTO

command service는 entity를 조회하고 도메인 메서드를 호출한 뒤 JPA 변경 감지로 CUD를 처리한다.

- 생성 command는 생성된 entity id만 반환한다.
- 수정/상태 변경 command는 기본적으로 `void`를 반환한다.
- command service에서는 화면 응답용 DTO를 만들지 않는다.
- command 실행 후 상세 데이터가 필요하면 read API를 다시 호출하는 흐름을 기본으로 한다.
- service 입력 모델은 `CreateKlassDto`처럼 service 전용 DTO를 사용한다.
- read service 메서드는 단건 조회를 `getOne`, 목록 조회를 `getList` 기준으로 이름 짓는다.
- 조건이 드러나야 하는 목록 조회는 `getMyList`, `getListByKlassId`처럼 `getList` 뒤에 조건을 붙인다.
- repository 메서드는 Spring Data JPA와 QueryDSL 관례에 맞춰 `findBy...`, `existsBy...` 이름을 사용할 수 있다.

read service는 read repository의 DTO projection 결과를 반환한다.

- service 단의 조회 DTO는 API마다 기계적으로 나누지 않고 도메인별 내부 조회 모델로 단순하게 유지한다.
- 예를 들어 klass 조회는 `KlassSummaryDto`, `KlassDetailDto`를 먼저 만들기보다 `KlassDto` 하나를 기본으로 사용한다.
- 목록과 상세의 응답 모양이 크게 달라지거나 성능상 필요한 컬럼 차이가 커질 때만 read DTO를 추가로 분리한다.
- 조회 DTO는 read service 또는 read repository가 소유한다.
- command DTO와 read DTO를 억지로 공용화하지 않는다.

## Controller DTO

controller 단에서는 외부 API 계약을 표현하기 위해 request/response DTO를 둔다.

예:

```text
com.liveklass.klass.readcontroller.request
com.liveklass.klass.readcontroller.response
com.liveklass.klass.commandcontroller.request
com.liveklass.klass.commandcontroller.response
```

- request DTO는 입력 형식과 Bean Validation을 담당한다.
- response DTO는 외부에 노출할 응답 필드와 이름을 담당한다.
- service DTO를 그대로 API 응답으로 노출하지 않고 controller에서 response DTO로 변환한다.
- service DTO는 내부 조회 모델이고, response DTO는 외부 API 계약이다.
- response DTO도 API마다 무조건 만들지 않고 응답 모양이 달라질 때만 분리한다.
- enum `FORM_OPTIONS`처럼 DB 조회나 도메인 흐름이 없는 폼 데이터는 service를 거치지 않고 controller의 `/form` API에서 바로 반환한다.

## DTO Validation

controller request DTO에는 Bean Validation을 사용한다.

예:

```java
public record CreateKlassRequest(
    @NotBlank String title,
    @NotNull @PositiveOrZero BigDecimal price,
    @NotNull @Positive Integer capacity
) {
}
```

- request 형식 검증은 DTO에서 처리한다.
- controller에는 `@Valid`를 붙인다.
- service/entity는 이미 검증된 값이 들어온다는 전제에서 도메인 규칙에 집중한다.
- 단, 상태 전이와 소유자 검증처럼 DB 상태가 필요한 검증은 service 또는 entity에서 처리한다.
