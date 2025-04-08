package com.bnids.gateway.service;

import com.bnids.gateway.entity.FeePolicy;
import com.bnids.gateway.entity.FeePolicyType;
import com.bnids.gateway.entity.PublishedCoupon;
import com.bnids.gateway.repository.FeePolicyRepository;
import com.bnids.gateway.repository.PublishedCouponRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingDiscountService {
    @NonNull
    private final PublishedCouponRepository publishedCouponRepository;

    @NonNull
    private final FeePolicyRepository feePolicyRepository;

    public int getTotalDiscountMinutes(Long visitCarId, LocalDateTime entryTime, Integer carSection) {
        // 할인권 적용 시간
        int couponDiscountMinutes = getCouponDiscountMinutes(visitCarId);

        // 무료주차시간
        int freeParkingMinutes = getFreeParkingMinutes();

        log.info("[할인시간 계산] 할인권적용시간: {}분, 무료주차시간: {}분, 총할인시간: {}분",
                couponDiscountMinutes, freeParkingMinutes, couponDiscountMinutes + freeParkingMinutes);

        return couponDiscountMinutes + freeParkingMinutes;
    }

    private int getCouponDiscountMinutes(Long visitCarId) {
        List<PublishedCoupon> usedCoupons = publishedCouponRepository.findUsedCouponsByVisitCarId(visitCarId);
        return usedCoupons.stream()
                .map(PublishedCoupon::getCoupon)
                .filter(coupon -> ("TIME").equals(coupon.getCouponType()))
                .mapToInt(coupon -> (int)coupon.getDiscount())
                .sum();
    }

    private int getFreeParkingMinutes() {
        Optional<FeePolicy> feePolicy = feePolicyRepository.findByFeePolicyTypeAndUseYn(FeePolicyType.NORMAL, "Y");

        if (!feePolicy.isPresent() || "N".equals(feePolicy.get().getFreeParkingTimeUseYn())) {
            log.info("[무료주차시간 조회] 무료주차시간 없음 (정책없음 또는 미사용)");
            return 0;
        }

        long freeParkingTime = feePolicy.get().getFreeParkingTime();
        log.info("[무료주차시간 조회] 기본 무료주차시간: {}분", freeParkingTime);
        return (int)freeParkingTime;
    }

}
