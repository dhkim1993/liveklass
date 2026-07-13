package com.liveklass.klass.read.repository;

import static com.liveklass.klass.domain.QKlass.klass;

import com.liveklass.klass.domain.enums.KlassStatus;
import com.liveklass.klass.read.service.KlassDto;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class KlassReadRepositoryImpl implements KlassReadRepository {

	private final JPAQueryFactory queryFactory;

	@Override
	public Page<KlassDto> findPage(KlassStatus status, Pageable pageable) {
		List<KlassDto> content = queryFactory
			.select(Projections.constructor(
				KlassDto.class,
				klass.id,
				klass.creatorId,
				klass.title,
				klass.description,
				klass.price,
				klass.capacity,
				klass.enrolledCount,
				klass.startDate,
				klass.endDate,
				klass.status,
				klass.createdAt,
				klass.updatedAt
			))
			.from(klass)
			.where(statusEq(status))
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.orderBy(klass.id.desc())
			.fetch();

		Long total = queryFactory
			.select(klass.count())
			.from(klass)
			.where(statusEq(status))
			.fetchOne();

		return new PageImpl<>(content, pageable, total == null ? 0 : total);
	}

	@Override
	public Optional<KlassDto> findById(Long klassId) {
		KlassDto detail = queryFactory
			.select(Projections.constructor(
				KlassDto.class,
				klass.id,
				klass.creatorId,
				klass.title,
				klass.description,
				klass.price,
				klass.capacity,
				klass.enrolledCount,
				klass.startDate,
				klass.endDate,
				klass.status,
				klass.createdAt,
				klass.updatedAt
			))
			.from(klass)
			.where(klass.id.eq(klassId))
			.fetchOne();

		return Optional.ofNullable(detail);
	}

	private BooleanExpression statusEq(KlassStatus status) {
		return status == null ? null : klass.status.eq(status);
	}
}
