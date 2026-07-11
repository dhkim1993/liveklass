package com.liveklass.enrollment.readrepository;

import static com.liveklass.enrollment.domain.QEnrollment.enrollment;
import static com.liveklass.klass.domain.QKlass.klass;

import com.liveklass.enrollment.domain.Enrollment;
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
	public Page<Enrollment> findByUserId(Long userId, Pageable pageable) {
		List<Enrollment> content = queryFactory
			.selectFrom(enrollment)
			.join(enrollment.klass, klass).fetchJoin()
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
	public Page<Enrollment> findByKlassId(Long klassId, Pageable pageable) {
		List<Enrollment> content = queryFactory
			.selectFrom(enrollment)
			.join(enrollment.klass, klass).fetchJoin()
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
