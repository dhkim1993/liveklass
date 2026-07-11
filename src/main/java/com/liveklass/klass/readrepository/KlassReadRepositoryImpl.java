package com.liveklass.klass.readrepository;

import static com.liveklass.klass.domain.QKlass.klass;

import com.liveklass.klass.domain.Klass;
import com.liveklass.klass.domain.enums.KlassStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
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
	public Page<Klass> findPage(KlassStatus status, Pageable pageable) {
		List<Klass> content = queryFactory
			.selectFrom(klass)
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

	private BooleanExpression statusEq(KlassStatus status) {
		return status == null ? null : klass.status.eq(status);
	}
}
