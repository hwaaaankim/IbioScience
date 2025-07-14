package com.dev.IbioScience.service.product;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.ProductDiscountSaveRequest;
import com.dev.IbioScience.model.product.Promotion;
import com.dev.IbioScience.model.product.enums.PromotionTarget;
import com.dev.IbioScience.model.product.enums.PromotionTerm;
import com.dev.IbioScience.model.product.enums.PromotionType;
import com.dev.IbioScience.repository.product.ProductPromotionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ProductPromotionService {

    private final ProductPromotionRepository productPromotionRepository;

    @Value("${spring.upload.path}")
    private String uploadPath; // ex) /home/ubuntu/IbioScience/files/

    @Transactional
    public void saveDiscount(ProductDiscountSaveRequest req) throws RuntimeException {
        Promotion promotion = new Promotion();

        promotion.setActive(Boolean.TRUE.equals(req.getActive()));
        promotion.setType(PromotionType.valueOf(req.getType()));
        promotion.setTerm(PromotionTerm.valueOf(req.getTerm()));
        promotion.setName(req.getName());

        promotion.setConditionEnabled(Boolean.TRUE.equals(req.getPeriodEnabled()));
        if (promotion.getConditionEnabled()) {
            if (req.getStartDate() != null && !req.getStartDate().isEmpty()) {
            	promotion.setStartDate(LocalDate.parse(req.getStartDate()));
            }
            if (req.getEndDate() != null && !req.getEndDate().isEmpty()) {
            	promotion.setEndDate(LocalDate.parse(req.getEndDate()));
            }
        } else {
        	promotion.setStartDate(null);
        	promotion.setEndDate(null);
        }

    	promotion.setTarget(PromotionTarget.NORMAL);

        if (req.getDiscountPercent() != null && !req.getDiscountPercent().isEmpty()) {
        	promotion.setDiscountPercent(new BigDecimal(req.getDiscountPercent()));
        } else {
        	promotion.setDiscountPercent(BigDecimal.ZERO);
        }

        // 1차 저장(아이콘 파일경로를 위해 id 필요)
        promotion = productPromotionRepository.save(promotion);

        // 아이콘 파일 저장
        MultipartFile iconFile = req.getIconFile();
        if (iconFile != null && !iconFile.isEmpty()) {
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String ext = "";
            String originalName = iconFile.getOriginalFilename();
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID() + ext;
            String dirPath = uploadPath + "/product/discount/icon/" + promotion.getId() + "/" + today;
            File dir = new File(dirPath);
            if (!dir.exists()) dir.mkdirs();

            String savePath = dirPath + "/" + fileName;
            try {
                iconFile.transferTo(new File(savePath));
            } catch (Exception ex) {
                throw new RuntimeException("아이콘 파일 저장 실패: " + ex.getMessage());
            }
            String url = "/upload/product/discount/icon/" + promotion.getId() + "/" + today + "/" + fileName;
            promotion.setIconPath(savePath);
            promotion.setIconUrl(url);

            // 2차 저장(파일 경로 반영)
            productPromotionRepository.save(promotion);
        }
    }
}
