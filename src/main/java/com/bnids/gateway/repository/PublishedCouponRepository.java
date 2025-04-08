package com.bnids.gateway.repository;

import com.bnids.gateway.entity.PublishedCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PublishedCouponRepository extends JpaRepository<PublishedCoupon, Long> {
    @Query(value = "select * from local_db.published_coupon where visit_car_id = :visitCarId and coupon_status = 'USED'", nativeQuery=true)
    List<PublishedCoupon> findUsedCouponsByVisitCarId(@Param("visitCarId") Long visitCarId);
}
