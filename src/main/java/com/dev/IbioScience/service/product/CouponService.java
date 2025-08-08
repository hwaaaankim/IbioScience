package com.dev.IbioScience.service.product;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.CouponDTO;
import com.dev.IbioScience.dto.CouponListRowDTO;
import com.dev.IbioScience.dto.CouponRegisterRequestDTO;
import com.dev.IbioScience.dto.CouponUpdateRequestDTO;
import com.dev.IbioScience.model.product.Coupon;
import com.dev.IbioScience.model.product.enums.CouponPolicy;
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
	    // 서버에서 무조건 ISSUED 로 고정
	    CouponStatus couponStatus = CouponStatus.ISSUED;

	    LocalDate sDate = (startDate != null && !startDate.isEmpty()) ? LocalDate.parse(startDate) : null;
	    LocalDate eDate = (endDate != null && !endDate.isEmpty()) ? LocalDate.parse(endDate) : null;

	    List<Coupon> list = couponRepository.searchCoupons(
	            couponStatus,
	            (name != null && !name.isEmpty()) ? name : null,
	            sDate, eDate
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
	
	/** 사이즈는 고정 10 (요청사항) */
    @Transactional(readOnly = true)
    public Page<CouponListRowDTO> getCouponPage(
            String name,
            CouponPolicy policy,
            CouponStatus status,
            LocalDate startDate,
            LocalDate endDate,
            Integer page
    ) {
        int pageIndex = (page == null || page < 0) ? 0 : page;
        Pageable pageable = PageRequest.of(pageIndex, 10, Sort.by(Sort.Direction.DESC, "id"));
        return couponRepository.searchCouponsPage(
                (name == null || name.isBlank()) ? null : name,
                policy,
                status,
                startDate,
                endDate,
                pageable
        );
    }
    
    @Transactional(readOnly = true)
    public Coupon getDetail(Long id) {
        return couponRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("쿠폰이 존재하지 않습니다. id=" + id));
    }

    @Transactional
    public Coupon update(CouponUpdateRequestDTO dto) {
        Coupon c = couponRepository.findById(dto.getId())
            .orElseThrow(() -> new NoSuchElementException("쿠폰이 존재하지 않습니다. id=" + dto.getId()));

        // 수정 필드 세팅
        c.setCouponName(dto.getCouponName());
        c.setMinPurchaseAmount(dto.getMinPurchaseAmount());
        c.setCouponAmount(dto.getCouponAmount());
        c.setStartDate(dto.getStartDate());
        c.setEndDate(dto.getEndDate());
        c.setCouponPolicy(dto.getCouponPolicy());
        c.setStatus(dto.getStatus());

        return couponRepository.save(c);
    }
}