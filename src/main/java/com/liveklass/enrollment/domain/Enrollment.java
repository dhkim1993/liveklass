package com.liveklass.enrollment.domain;

import com.liveklass.common.BaseTimeEntity;
import com.liveklass.common.exception.ErrorCode;
import com.liveklass.common.exception.LiveKlassException;
import com.liveklass.enrollment.domain.enums.EnrollmentStatus;
import com.liveklass.klass.domain.Klass;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
            throw new LiveKlassException(ErrorCode.INVALID_ENROLLMENT_STATUS);
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
            throw new LiveKlassException(ErrorCode.FORBIDDEN_ENROLLMENT_ACCESS);
        }
    }

    private void validateCancellable(LocalDateTime now) {
        if (status == EnrollmentStatus.PENDING) {
            return;
        }
        if (status != EnrollmentStatus.CONFIRMED) {
            throw new LiveKlassException(ErrorCode.INVALID_ENROLLMENT_STATUS);
        }
        if (confirmedAt == null) {
            throw new LiveKlassException(ErrorCode.INVALID_ENROLLMENT_STATUS);
        }
        EnrollmentCancellationPolicy.validateConfirmedCancellation(confirmedAt, klass.getStartDate(), now);
    }
}
