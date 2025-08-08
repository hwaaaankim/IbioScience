package com.dev.IbioScience.service.product;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import com.dev.IbioScience.repository.product.ProductPromotionMappingRepository;
import com.dev.IbioScience.repository.product.ProductPromotionRepository;
import com.dev.IbioScience.repository.product.register.ProductRepository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ProductPromotionService {

    @Value("${spring.upload.path}")
    private String uploadPath; 
    
    private final ProductPromotionRepository productPromotionRepository;
    private final ProductPromotionMappingRepository productPromotionMappingRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;

    /**
     * 프로모션 등록
     * @param req 프로모션 등록 요청 DTO
     */
    @Transactional
    public void savePromotion(PromotionRegisterRequest req) {
        Promotion p = buildPromotionEntity(null, req, null, null);
        productPromotionRepository.save(p);
    }

    @Transactional
    public void updatePromotion(Long id, PromotionRegisterRequest req) {
        Promotion existing = productPromotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("프로모션이 존재하지 않습니다."));

        // 기존 아이콘 경로 백업
        String oldIconPath = existing.getIconPath();

        Promotion updated = buildPromotionEntity(id, req, existing.getIconUrl(), existing.getIconPath());
        // dirty checking
        existing.setName(updated.getName());
        existing.setActive(updated.getActive());
        existing.setTerm(updated.getTerm());
        existing.setType(updated.getType());
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());
        existing.setIconPath(updated.getIconPath());
        existing.setIconUrl(updated.getIconUrl());
        existing.setDiscountPercent(updated.getDiscountPercent());
        existing.setGiftProduct(updated.getGiftProduct());
        existing.setCoupon(updated.getCoupon());

        // 아이콘 교체 시 이전 파일 삭제
        if (oldIconPath != null && updated.getIconPath() != null && !oldIconPath.equals(updated.getIconPath())) {
            try { new File(oldIconPath).delete(); } catch (Exception ignore) {}
        }
    }

    /** 등록/수정 공용 엔티티 빌드 + 검증/파일저장 포함 */
    private Promotion buildPromotionEntity(Long idOrNull, PromotionRegisterRequest req,
                                           String prevIconUrl, String prevIconPath) {
        // 공통 필수
        if (!StringUtils.hasText(req.getName())) {
            throw new IllegalArgumentException("프로모션명을 입력해주세요.");
        }
        if (!StringUtils.hasText(req.getType())) {
            throw new IllegalArgumentException("프로모션 타입을 선택해주세요.");
        }
        if (!StringUtils.hasText(req.getTerm())) {
            throw new IllegalArgumentException("기간 정책을 선택해주세요.");
        }

        // enum 파싱
        final PromotionType type;
        try { type = PromotionType.valueOf(req.getType()); }
        catch (Exception e) { throw new IllegalArgumentException("잘못된 프로모션 타입입니다."); }

        final PromotionTerm term;
        try { term = PromotionTerm.valueOf(req.getTerm()); }
        catch (Exception e) { throw new IllegalArgumentException("잘못된 기간 정책입니다."); }

        // 기간
        LocalDate startDate = null, endDate = null;
        if (term == PromotionTerm.PERIOD) {
            if (!StringUtils.hasText(req.getStartDate()) || !StringUtils.hasText(req.getEndDate())) {
                throw new IllegalArgumentException("기간한정일 때 시작일과 종료일은 필수입니다.");
            }
            try {
                startDate = LocalDate.parse(req.getStartDate());
                endDate = LocalDate.parse(req.getEndDate());
            } catch (Exception ex) {
                throw new IllegalArgumentException("시작일/종료일 형식이 올바르지 않습니다(yyyy-MM-dd).");
            }
            if (endDate.isBefore(startDate)) {
                throw new IllegalArgumentException("종료일은 시작일 이후여야 합니다.");
            }
        }

        // 타입별 필수
        if (type == PromotionType.DISCOUNT) {
            BigDecimal dp = req.getDiscountPercent();
            if (dp == null || dp.compareTo(BigDecimal.ZERO) < 0 || dp.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("할인율(0~100)을 입력해주세요.");
            }
        } else if (type == PromotionType.GIFT) {
            if (req.getGiftProductId() == null) throw new IllegalArgumentException("증정 상품을 선택해주세요.");
        } else if (type == PromotionType.COUPON) {
            if (req.getCouponId() == null) throw new IllegalArgumentException("쿠폰을 선택해주세요.");
        }

        // 파일 저장(선택)
        String iconUrl = prevIconUrl;
        String iconPath = prevIconPath;
        try {
            if (req.getIconFile() != null && !req.getIconFile().isEmpty()) {
                FileSaveResult result = saveIconFile(req.getIconFile());
                iconUrl = result.getUrl();
                iconPath = result.getPath();
            }
        } catch (Exception e) {
            throw new RuntimeException("아이콘 파일 저장 실패: " + e.getMessage(), e);
        }

        // 엔티티 조립
        Promotion p = new Promotion();
        if (idOrNull != null) p.setId(idOrNull);
        p.setName(req.getName().trim());
        p.setActive("ACTIVE".equalsIgnoreCase(req.getStatus()));
        p.setTerm(term);
        p.setType(type);
        p.setStartDate(startDate);
        p.setEndDate(endDate);
        p.setIconPath(iconPath);
        p.setIconUrl(iconUrl);

        // 타입별 매핑 + 불필요 필드 정리
        if (type == PromotionType.DISCOUNT) {
            p.setDiscountPercent(req.getDiscountPercent());
            p.setGiftProduct(null);
            p.setCoupon(null);
        } else if (type == PromotionType.GIFT) {
            Product product = productRepository.findById(req.getGiftProductId())
                    .orElseThrow(() -> new IllegalArgumentException("증정상품이 존재하지 않습니다."));
            p.setGiftProduct(product);
            p.setDiscountPercent(null);
            p.setCoupon(null);
        } else if (type == PromotionType.COUPON) {
            Coupon coupon = couponRepository.findById(req.getCouponId())
                    .orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다."));
            p.setCoupon(coupon);
            p.setDiscountPercent(null);
            p.setGiftProduct(null);
        } else { // ONE_PLUS_ONE
            p.setDiscountPercent(null);
            p.setGiftProduct(null);
            p.setCoupon(null);
        }

        return p;
    }

    /** 아이콘 저장 */
    private FileSaveResult saveIconFile(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null ||
            !(contentType.equalsIgnoreCase("image/png")
                || contentType.equalsIgnoreCase("image/jpeg")
                || contentType.equalsIgnoreCase("image/gif"))) {
            throw new IllegalArgumentException("이미지 파일(png/jpg/gif)만 업로드 가능합니다.");
        }

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String folder = "promotion/" + today + "/icon/";
        File dir = new File(uploadPath, folder);
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("업로드 디렉토리 생성 실패: " + dir.getAbsolutePath());

        String orig = file.getOriginalFilename();
        String safe = (orig != null ? orig.replaceAll("[^\\w.\\-]", "_") : "icon");
        String fileName = System.currentTimeMillis() + "_" + safe;

        File dest = new File(dir, fileName);
        file.transferTo(dest);

        return new FileSaveResult("/upload/" + folder + fileName, dest.getAbsolutePath());
    }

    @Transactional(readOnly = true)
    public List<Promotion> searchPromotions(
            String name,
            PromotionType type,
            LocalDate startDate,
            LocalDate endDate,
            Boolean active
    ) {
        return productPromotionRepository.findBySearchConditions(name, type, startDate, endDate, active);
    }
    
    @Getter
    @AllArgsConstructor
    private static class FileSaveResult {
        private final String url;
        private final String path;
    }
    
    @Transactional(readOnly = true)
    public Page<Promotion> getPromotionPage(String name,
                                            Boolean active,
                                            PromotionTerm term,
                                            LocalDate startDate,
                                            LocalDate endDate,
                                            PromotionType type,
                                            int page,
                                            int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return productPromotionRepository.searchPage(name, active, type, term, startDate, endDate, pageable);
    }

    @Transactional(readOnly = true)
    public Promotion getOne(Long id) {
        return productPromotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("프로모션이 존재하지 않습니다."));
    }

    @Transactional
    public void delete(Long id) {
        long useCnt = productPromotionMappingRepository.countByPromotion_Id(id);
        if (useCnt > 0) {
            throw new IllegalStateException("해당 프로모션이 등록된 제품이 있어 삭제할 수 없습니다.");
        }
        productPromotionRepository.deleteById(id);
    }
}
