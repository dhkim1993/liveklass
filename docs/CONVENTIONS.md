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
- `@Builder`는 class level이 아니라 생성자에 붙인다.
- builder를 외부에 열어 도메인 기본값이나 상태 전이를 우회하지 않도록 주의한다.
- 생성 요청값의 null, blank, 범위 검증은 controller request DTO의 Bean Validation으로 처리한다.
- entity에는 상태 전이, 정원 관리, 취소 가능 기간 같은 도메인 규칙만 둔다.

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
  commandcontroller
  commandservice
  commandrepository
  readcontroller
  readservice
  readrepository

com.liveklass.enrollment
  domain
    enums
  commandcontroller
  commandservice
  commandrepository
  readcontroller
  readservice
  readrepository
```

- `config`에는 QueryDSL, datasource routing, Clock 같은 애플리케이션 설정을 둔다.
- `common`에는 여러 도메인에서 공유하는 기반 클래스와 공통 유틸리티를 둔다.
- 도메인 entity는 각 도메인의 `domain` 패키지에 둔다.
- 도메인 enum은 각 도메인의 `domain.enums` 패키지에 둔다.
- 빈 디렉터리는 만들지 않고, 해당 계층 구현 시점에 패키지를 생성한다.

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

    private final String label;
}
```

- enum name은 영문 code로 사용한다.
- `label`은 화면 표시용 한국어 값으로 사용한다.
- `FORM_OPTIONS`는 프론트엔드가 선택 가능한 enum 값을 구성할 수 있도록 제공한다.
- `FORM_OPTIONS`는 enum 선언 순서가 유지되는 `LinkedHashMap` 기반 불변 map으로 만든다.

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
    Page<Klass> findPage(KlassStatus status, Pageable pageable);
}

@Repository
@RequiredArgsConstructor
public class KlassReadRepositoryImpl implements KlassReadRepository {
}
```

- CUD repository에는 단순 저장, 삭제, 단건 조회, exists 계열만 둔다.
- 복잡한 목록 조회와 검색 조건은 read repository에서 처리한다.
- read repository 구현체는 조회만 담당하고 entity 상태를 변경하지 않는다.
- QueryDSL 조회에서 연관 엔티티가 응답에 필요하면 `fetchJoin`을 적용해 N+1을 방지한다.
- count query에는 불필요한 `fetchJoin`을 사용하지 않는다.

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
