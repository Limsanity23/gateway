package com.bnids.gateway.repository;

import com.bnids.gateway.dto.InterlockResponseDto;
import com.bnids.gateway.dto.WarningCarAutoRegistRulesDto;
import com.bnids.gateway.entity.VisitCar;
import com.bnids.gateway.entity.WarningCarRegistEnum;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.bnids.gateway.entity.QVisitCar.visitCar;

@Repository
@Slf4j
public class VisitCarRepositorySupport extends QuerydslRepositorySupport {

    private final JPAQueryFactory queryFactory;
    public VisitCarRepositorySupport(JPAQueryFactory queryFactory) {
        super(VisitCar.class);
        this.queryFactory = queryFactory;
    }

    public List<InterlockResponseDto> findVisitCarListForRegistWarningCar(WarningCarAutoRegistRulesDto rules, boolean isRegistCar) {
        BooleanBuilder builder = generate(rules, isRegistCar);
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

    public BooleanBuilder generate(WarningCarAutoRegistRulesDto dto, boolean isRegistCar) {
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
            //20230811 수정 - [간편버튼 항목으로 경고차량정책을 등록한 경우]
            //간편버튼 입차 후 방문예약 등록하여 출차하였을 때
            //visitCar 에는 carSection이 간편버튼 -> 예약방문으로 수정되지 않고 그대로 남아있으므로
            //차후 간편버튼으로 입출차시 주차시간 위반이 되면 경고차량으로 자동등록된다
            //주차시간위반 조건에 동호 정보가 없는 경우만 계산하도록 조건 추가(방문예약으로 입출차시 동호정보를 남기게 되어있으므로) - 등록차량을 경고차량으로 등록하는 케이스는 제외
            //  ->20240215 추가 - 키오스크 세대방문이 정책일 경우 동호정보가 없는 조건이면 경고차량 등록이 되지 않으므로 제외시킨다
            if (!isRegistCar && dto.getCarSection().intValue() != 4)  builder.and(visitCar.visitDong.isNull()).and(visitCar.visitHo.isNull());

//            builder.and(Expressions.numberTemplate(Long.class, "timestampdiff(HOUR, {0}, {1})", visitCar.entvhclDt , visitCar.lvvhclDt).goe(dto.getParkingTime()));

            int totalParkingMinutes = dto.getParkingTime() * 60 + dto.getParkingTimeMinutes();
            builder.and(Expressions.numberTemplate(Long.class, "timestampdiff(MINUTE, {0}, {1})", visitCar.entvhclDt, visitCar.lvvhclDt)
                    .goe(totalParkingMinutes));
        }

        return builder;
    }

}