package com.liveklass.domain.klass;

import com.liveklass.domain.common.BaseTimeEntity;
import com.liveklass.domain.klass.enums.KlassStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "klasses",
	indexes = {
		@Index(name = "idx_klasses_status", columnList = "status"),
		@Index(name = "idx_klasses_creator_status", columnList = "creator_id,status")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Klass extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "creator_id", nullable = false)
	private Long creatorId;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, length = 1000)
	private String description;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Column(nullable = false)
	private int capacity;

	@Column(name = "enrolled_count", nullable = false)
	private int enrolledCount;

	@Column(name = "start_date", nullable = false)
	private LocalDateTime startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDateTime endDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private KlassStatus status;

	@Version
	private Long version;

	@Builder
	private Klass(
		Long creatorId,
		String title,
		String description,
		BigDecimal price,
		int capacity,
		LocalDateTime startDate,
		LocalDateTime endDate
	) {
		this.creatorId = creatorId;
		this.title = title;
		this.description = description;
		this.price = price;
		this.capacity = capacity;
		this.enrolledCount = 0;
		this.startDate = startDate;
		this.endDate = endDate;
		this.status = KlassStatus.DRAFT;
	}

	public static Klass create(
		Long creatorId,
		String title,
		String description,
		BigDecimal price,
		int capacity,
		LocalDateTime startDate,
		LocalDateTime endDate
	) {
		return new Klass(creatorId, title, description, price, capacity, startDate, endDate);
	}

	public void open(Long requesterId) {
		validateCreator(requesterId);
		if (status != KlassStatus.DRAFT) {
			throw new IllegalStateException("DRAFT 상태의 강의만 모집 시작할 수 있습니다.");
		}
		status = KlassStatus.OPEN;
	}

	public void close(Long requesterId) {
		validateCreator(requesterId);
		if (status == KlassStatus.CLOSED) {
			throw new IllegalStateException("이미 모집 마감된 강의입니다.");
		}
		status = KlassStatus.CLOSED;
	}

	public void occupyCapacity() {
		if (status != KlassStatus.OPEN) {
			throw new IllegalStateException("모집 중인 강의만 신청할 수 있습니다.");
		}
		if (enrolledCount >= capacity) {
			throw new IllegalStateException("수강 정원을 초과할 수 없습니다.");
		}
		enrolledCount++;
	}

	public void releaseCapacity() {
		if (enrolledCount <= 0) {
			throw new IllegalStateException("복구할 수강 정원이 없습니다.");
		}
		enrolledCount--;
	}

	public void validateCreator(Long requesterId) {
		if (requesterId == null || !requesterId.equals(creatorId)) {
			throw new IllegalStateException("강의 생성자만 수행할 수 있는 작업입니다.");
		}
	}

}
