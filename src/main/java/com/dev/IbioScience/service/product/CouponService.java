package com.dev.IbioScience.service.product;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.CouponDTO;
import com.dev.IbioScience.dto.CouponRegisterRequestDTO;
import com.dev.IbioScience.model.product.Coupon;
import com.dev.IbioScience.model.product.enums.CouponStatus;
import com.dev.IbioScience.repository.product.CouponRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponService {

	private final CouponRepository couponRepository;

	@Transactional
	public Coupon registerCoupon(CouponRegisterRequestDTO dto) {
	    String couponCode;
	    do {
	        couponCode = "CPN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
	    } while (couponRepository.existsByCouponCode(couponCode));

	    Coupon coupon = new Coupon();
	    coupon.setCouponCode(couponCode);
	    coupon.setCouponName(dto.getCouponName());
	    coupon.setMinPurchaseAmount(dto.getMinPurchaseAmount());
	    coupon.setCouponAmount(dto.getCouponAmount());
	    coupon.setStartDate(dto.getStartDate());
	    coupon.setEndDate(dto.getEndDate());
	    coupon.setCouponPolicy(dto.getCouponPolicy());
	    // status가 null로 들어올 경우 무조건 ISSUED로 대입
	    coupon.setStatus(dto.getStatus() == null ? CouponStatus.ISSUED : dto.getStatus());

	    return couponRepository.save(coupon);
	}
	
	@Transactional(readOnly = true)
    public List<CouponDTO> searchCoupons(String status, String name, String startDate, String endDate) {
        CouponStatus couponStatus = null;
        if (status != null && !status.isEmpty()) {
            couponStatus = CouponStatus.valueOf(status);
        }
        LocalDate sDate = (startDate != null && !startDate.isEmpty()) ? LocalDate.parse(startDate) : null;
        LocalDate eDate = (endDate != null && !endDate.isEmpty()) ? LocalDate.parse(endDate) : null;

        List<Coupon> list = couponRepository.searchCoupons(
                couponStatus,
                (name != null && !name.isEmpty()) ? name : null,
                sDate,
                eDate
        );

        return list.stream().map(c -> {
            CouponDTO dto = new CouponDTO();
            dto.setId(c.getId());
            dto.setCouponName(c.getCouponName());
            dto.setStartDate(c.getStartDate().toString());
            dto.setEndDate(c.getEndDate().toString());
            dto.setStatus(c.getStatus().name());
            return dto;
        }).collect(Collectors.toList());
    }
}