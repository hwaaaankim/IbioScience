package com.dev.IbioScience.service.product;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.PromotionRegisterRequest;
import com.dev.IbioScience.model.product.Coupon;
import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.Promotion;
import com.dev.IbioScience.model.product.enums.PromotionTerm;
import com.dev.IbioScience.model.product.enums.PromotionType;
import com.dev.IbioScience.repository.product.CouponRepository;
import com.dev.IbioScience.repository.product.ProductPromotionRepository;
import com.dev.IbioScience.repository.product.register.ProductRepository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ProductPromotionService {

    @Value("${spring.upload.path}")
    private String uploadPath; // ex) /home/ubuntu/IbioScience/files/
    
    private final ProductPromotionRepository productPromotionRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;

    /**
     * 프로모션 등록
     * @param req 프로모션 등록 요청 DTO
     */
    @Transactional
    public void savePromotion(PromotionRegisterRequest req) {
        // 1. 타입/필수값 검증
        if (!StringUtils.hasText(req.getName()) || !StringUtils.hasText(req.getType())) {
            throw new IllegalArgumentException("프로모션명, 타입 필수");
        }

        PromotionType type;
        try {
            type = PromotionType.valueOf(req.getType());
        } catch (Exception e) {
            throw new IllegalArgumentException("잘못된 프로모션 타입입니다.");
        }

        if (type == PromotionType.DISCOUNT) {
            if (req.getDiscountPercent() == null 
                    || req.getDiscountPercent().compareTo(BigDecimal.ZERO) < 0
                    || req.getDiscountPercent().compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("할인율(0~100%)을 입력해주세요.");
            }
        }
        if (type == PromotionType.GIFT && req.getGiftProductId() == null) {
            throw new IllegalArgumentException("증정 상품을 선택해주세요.");
        }
        if (type == PromotionType.COUPON && req.getCouponId() == null) {
            throw new IllegalArgumentException("쿠폰을 선택해주세요.");
        }

        // 2. 파일 저장 (아이콘)
        String iconUrl = null;
        String iconPath = null;
        try {
            if (req.getIconFile() != null && !req.getIconFile().isEmpty()) {
                FileSaveResult result = saveIconFile(req.getIconFile());
                iconUrl = result.getUrl();
                iconPath = result.getPath();
            }
        } catch (Exception e) {
            throw new RuntimeException("아이콘 파일 저장 실패: " + e.getMessage(), e);
        }

        // 3. Promotion 생성 및 저장
        Promotion promotion = new Promotion();
        promotion.setName(req.getName());
        promotion.setActive("ACTIVE".equals(req.getStatus()));
        promotion.setTerm(PromotionTerm.valueOf(req.getTerm()));
        promotion.setType(type);

        promotion.setStartDate(LocalDate.parse(req.getStartDate()));
        promotion.setEndDate(LocalDate.parse(req.getEndDate()));
        promotion.setIconPath(iconPath);
        promotion.setIconUrl(iconUrl);

        // 타입별 설정
        if (type == PromotionType.DISCOUNT) {
            promotion.setDiscountPercent(req.getDiscountPercent());
        } else if (type == PromotionType.GIFT) {
            Product product = productRepository.findById(req.getGiftProductId())
                    .orElseThrow(() -> new IllegalArgumentException("증정상품이 존재하지 않습니다."));
            promotion.setGiftProduct(product);
        } else if (type == PromotionType.COUPON) {
            Coupon coupon = couponRepository.findById(req.getCouponId())
                    .orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다."));
            promotion.setCoupon(coupon);
            coupon.setPromotion(promotion); // 양방향 연결
        }
        // ONE_PLUS_ONE 타입은 별도 처리 없음

        try {
            productPromotionRepository.save(promotion);
        } catch (Exception dbEx) {
            // DB 저장 실패시 파일 삭제
            if (iconPath != null) {
                try { new File(iconPath).delete(); }
                catch (Exception delEx) { /* 로그만 */ }
            }
            throw dbEx;
        }
    }

    /**
     * 아이콘 파일 저장 처리 및 URL 반환
     */
    private FileSaveResult saveIconFile(MultipartFile file) throws IOException {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String folder = "promotion/" + today + "/icon/";
        File dir = new File(uploadPath, folder);
        if (!dir.exists()) dir.mkdirs();

        String origName = file.getOriginalFilename();
        String ext = "";
        if (origName != null && origName.contains(".")) {
            ext = origName.substring(origName.lastIndexOf("."));
        }
        String fileName = System.currentTimeMillis() + "_" + (origName != null ? origName.replaceAll("[^\\w.]", "_") : "icon") + ext;

        File dest = new File(dir, fileName);
        file.transferTo(dest);

        String url = "/upload/" + folder + fileName;
        String path = dest.getAbsolutePath();
        return new FileSaveResult(url, path);
    }

    @Getter
    @AllArgsConstructor
    private static class FileSaveResult {
        private final String url;
        private final String path;
    }
}
