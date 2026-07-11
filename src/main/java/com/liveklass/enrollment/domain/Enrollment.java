package com.liveklass.enrollment.domain;

import com.liveklass.common.BaseTimeEntity;
import com.liveklass.enrollment.domain.enums.EnrollmentStatus;
import com.liveklass.klass.domain.Klass;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "enrollments",
	indexes = {
		@Index(name = "idx_enrollments_klass_user", columnList = "klass_id,user_id"),
		@Index(name = "idx_enrollments_user_status", columnList = "user_id,status")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enrollment extends BaseTimeEntity {

	private static final int CANCEL_AVAILABLE_DAYS = 7;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "klass_id", nullable = false)
	private Klass klass;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EnrollmentStatus status;

	@Column(name = "confirmed_at")
	private LocalDateTime confirmedAt;

	@Column(name = "cancelled_at")
	private LocalDateTime cancelledAt;

	@Builder
	private Enrollment(Klass klass, Long userId) {
		this.klass = klass;
		this.userId = userId;
		this.status = EnrollmentStatus.PENDING;
	}

	public static Enrollment pending(Klass klass, Long userId) {
		return new Enrollment(klass, userId);
	}

	public void confirm(LocalDateTime confirmedAt) {
		if (status == EnrollmentStatus.CONFIRMED) {
			return;
		}
		if (status != EnrollmentStatus.PENDING) {
			throw new IllegalStateException("결제 대기 상태의 신청만 확정할 수 있습니다.");
		}
		this.status = EnrollmentStatus.CONFIRMED;
		this.confirmedAt = confirmedAt;
	}

	public boolean cancel(LocalDateTime now) {
		if (status == EnrollmentStatus.CANCELLED) {
			return false;
		}
		validateCancellable(now);
		this.status = EnrollmentStatus.CANCELLED;
		this.cancelledAt = now;
		return true;
	}

	public void validateOwner(Long requesterId) {
		if (requesterId == null || !requesterId.equals(userId)) {
			throw new IllegalStateException("신청자 본인만 수행할 수 있는 작업입니다.");
		}
	}

	private void validateCancellable(LocalDateTime now) {
		if (status == EnrollmentStatus.PENDING) {
			return;
		}
		if (status != EnrollmentStatus.CONFIRMED) {
			throw new IllegalStateException("취소할 수 없는 신청 상태입니다.");
		}
		if (confirmedAt == null) {
			throw new IllegalStateException("확정 시간이 없는 신청은 취소할 수 없습니다.");
		}
		if (now.isAfter(confirmedAt.plusDays(CANCEL_AVAILABLE_DAYS))) {
			throw new IllegalStateException("취소 가능 기간이 지났습니다.");
		}
		if (!now.isBefore(klass.getStartDate())) {
			throw new IllegalStateException("강의 시작 이후에는 취소할 수 없습니다.");
		}
	}
}
