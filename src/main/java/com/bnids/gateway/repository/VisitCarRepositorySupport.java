package com.bnids.gateway.repository;

import com.bnids.gateway.dto.InterlockResponseDto;
import com.bnids.gateway.dto.WarningCarAutoRegistRulesDto;
import com.bnids.gateway.entity.WarningCarRegistEnum;
import com.bnids.gateway.entity.VisitCar;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.bnids.gateway.entity.QVisitCar.visitCar;

@Repository
public class VisitCarRepositorySupport extends QuerydslRepositorySupport {

    private final JPAQueryFactory queryFactory;
    public VisitCarRepositorySupport(JPAQueryFactory queryFactory) {
        super(VisitCar.class);
        this.queryFactory = queryFactory;
    }

    public List<InterlockResponseDto> findVisitCarListForRegistWarningCar(WarningCarAutoRegistRulesDto rules) {
        BooleanBuilder builder = generate(rules);
        final JPQLQuery<InterlockResponseDto> query;
        query = queryFactory
                .select(Projections.bean(InterlockResponseDto.class, visitCar.visitCarId))
                .from(visitCar)
                .where(builder);
        final List<InterlockResponseDto> visitCars = getQuerydsl().applySorting(Sort.by(Sort.Direction.DESC, "visitCarId"), query).fetch();
        return visitCars;
    }

    /*public BooleanBuilder generate(WarningCarAutoRegistRulesDto dto) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(visitCar.carNo.eq(dto.getCarNo()));
        if( dto.getDeletedDt() != null ) {
            builder.and(visitCar.entvhclDt.after(dto.getDeletedDt()));
        }
        if( dto.getCarSection().intValue() > 0 ) {
            builder.and(visitCar.carSection.eq(dto.getCarSection().intValue()));
        }
        if( dto.getRegistDay() > 0 ) {
            builder.and(visitCar.entvhclDt.after(LocalDateTime.now().minusDays(dto.getRegistDay())));
        }
        if( dto.getWarinigCarRulesSection().equals(WarningCarRegistEnum.PARKING_DURATION_VIOLATION) ) {
            builder.and(Expressions.numberTemplate(Long.class, "timestampdiff(HOUR, {0}, {1})", visitCar.entvhclDt , visitCar.lvvhclDt).gt(dto.getParkingTime()));
        }
        return builder;
    }*/

    public BooleanBuilder generate(WarningCarAutoRegistRulesDto dto) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(visitCar.carNo.eq(dto.getCarNo()));
        if( dto.getDeletedDt() != null ) {
            builder.and(visitCar.entvhclDt.after(dto.getDeletedDt()));
        }
        if( dto.getCarSection().intValue() > 0 ) {
            builder.and(visitCar.carSection.eq(dto.getCarSection().intValue()));
        }
        if( dto.getRegistDay() > 0 ) {
            LocalDateTime startDate = LocalDateTime.now().minusDays(dto.getRegistDay());
            //시행일시를 사용하는 경우
            if ( dto.getApplyDt() != null ) {
                //단속시작기간이 시행일 이전이면 무시, 시행일 이후 출입내역만 단속한다
                if ( startDate.isAfter(dto.getApplyDt()) ) {
                    builder.and(visitCar.entvhclDt.after(startDate));
                } else {
                    builder.and(visitCar.entvhclDt.after(dto.getApplyDt()));
                }
            } else {
                //시행일시가 없으면 기존로직 유지
                builder.and(visitCar.entvhclDt.after(startDate));
            }
        }

        if( dto.getWarinigCarRulesSection().equals(WarningCarRegistEnum.PARKING_DURATION_VIOLATION) ) {
            builder.and(Expressions.numberTemplate(Long.class, "timestampdiff(HOUR, {0}, {1})", visitCar.entvhclDt , visitCar.lvvhclDt).gt(dto.getParkingTime()));
        }

        return builder;
    }

}