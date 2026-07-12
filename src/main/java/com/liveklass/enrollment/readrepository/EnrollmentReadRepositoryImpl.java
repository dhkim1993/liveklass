package com.liveklass.enrollment.readrepository;

import static com.liveklass.enrollment.domain.QEnrollment.enrollment;
import static com.liveklass.klass.domain.QKlass.klass;

import com.liveklass.enrollment.readservice.EnrollmentDto;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EnrollmentReadRepositoryImpl implements EnrollmentReadRepository {

	private final JPAQueryFactory queryFactory;

	@Override
	public Page<EnrollmentDto> findByUserId(Long userId, Pageable pageable) {
		List<EnrollmentDto> content = queryFactory
			.select(Projections.constructor(
				EnrollmentDto.class,
				enrollment.id,
				klass.id,
				klass.title,
				enrollment.userId,
				enrollment.status,
				enrollment.confirmedAt,
				enrollment.cancelledAt,
				enrollment.createdAt,
				enrollment.updatedAt
			))
			.from(enrollment)
			.join(enrollment.klass, klass)
			.where(enrollment.userId.eq(userId))
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.orderBy(enrollment.id.desc())
			.fetch();

		Long total = queryFactory
			.select(enrollment.count())
			.from(enrollment)
			.where(enrollment.userId.eq(userId))
			.fetchOne();

		return new PageImpl<>(content, pageable, total == null ? 0 : total);
	}

	@Override
	public Page<EnrollmentDto> findByKlassId(Long klassId, Pageable pageable) {
		List<EnrollmentDto> content = queryFactory
			.select(Projections.constructor(
				EnrollmentDto.class,
				enrollment.id,
				klass.id,
				klass.title,
				enrollment.userId,
				enrollment.status,
				enrollment.confirmedAt,
				enrollment.cancelledAt,
				enrollment.createdAt,
				enrollment.updatedAt
			))
			.from(enrollment)
			.join(enrollment.klass, klass)
			.where(enrollment.klass.id.eq(klassId))
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.orderBy(enrollment.id.desc())
			.fetch();

		Long total = queryFactory
			.select(enrollment.count())
			.from(enrollment)
			.where(enrollment.klass.id.eq(klassId))
			.fetchOne();

		return new PageImpl<>(content, pageable, total == null ? 0 : total);
	}
}
