package com.bnids.gateway.repository;

import com.bnids.core.base.BaseJPARepository;
import com.bnids.gateway.entity.VisitCar;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VisitCarRepository extends BaseJPARepository<VisitCar, Long> {
    Optional<VisitCar> findById(Long id);

    @Query(value = "SELECT visit_car_id, created_dt, site_code, updated_dt, car_no,\n" +
            "       car_section, entrance_gate_id, entrance_gate_status, entrance_gate_transit_mode,\n" +
            "       entrance_worker, entvhcl_dt, exit_gate_id, exit_gate_status, exit_gate_transit_mode,\n" +
            "       exit_worker, logic_code, lpr_car_no, lvvhcl_dt, note, restrict_leave_car,\n" +
            "       tel_no, visit_allowable_time, visit_dong, visit_ho, visit_place_name, visit_purpose \n" +
            "FROM   visit_car \n" +
            "WHERE  visit_car_id IN ( \n" +
            "               SELECT max(visit_car_id) FROM visit_car WHERE  car_no = :carNo \n" +
            "               AND (entvhcl_dt = lvvhcl_dt OR lvvhcl_dt IS NULL ) \n" +
            ")", nativeQuery = true) //221214 cks 본 쿼리메서드를  네이티브로 변경함 - 입출차가 같은 row가 조건에 추가 됨
    Optional<VisitCar> findTopByCarNoAndLvvhclDtIsNullOrderByEntvhclDtDesc(String carNo);

    Optional<VisitCar> findTopByCarNoOrderByEntvhclDtDesc(String carNo);

    @Query(value = 
    "SELECT ifnull(sum(TIMESTAMPDIFF(MINUTE, entvhcl_dt, lvvhcl_dt)),0) / 60 AS HOURS " +
    "FROM ( " +
    "SELECT IF(entvhcl_dt < DATE_FORMAT(NOW() ,'%Y-%m-01'), DATE_FORMAT(NOW() ,'%Y-%m-01'), entvhcl_dt) as entvhcl_dt, ifnull(lvvhcl_dt, NOW()) as lvvhcl_dt FROM local_db.visit_car " +
    "where car_section in (4, 5) " +
    "and visit_dong = :visit_dong " +
    "and visit_ho = :visit_ho " +
    "and ( entvhcl_dt between  DATE_FORMAT(NOW() ,'%Y-%m-01') AND NOW() " +
    "     OR " +
    "      lvvhcl_dt between  DATE_FORMAT(NOW() ,'%Y-%m-01') AND NOW() ) " +
    ") a    "
    , nativeQuery=true)
    // car_section 4 or 5의 입차일 혹은 출차일이 월초부터 현재까지인 해당 동호 방문 차량의 주차시간을 합산
    Double getSumVisitCar45ParkingHours(@Param("visit_dong") String visit_dong, @Param("visit_ho") String visit_ho);

}
