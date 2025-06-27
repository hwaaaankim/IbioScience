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
import com.dev.IbioScience.model.product.ProductDiscount;
import com.dev.IbioScience.model.product.status.CouponPolicy;
import com.dev.IbioScience.model.product.status.DiscountTarget;
import com.dev.IbioScience.model.product.status.DiscountTerm;
import com.dev.IbioScience.model.product.status.DiscountType;
import com.dev.IbioScience.repository.product.ProductDiscountRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ProductDiscountService {

    private final ProductDiscountRepository productDiscountRepository;

    @Value("${spring.upload.path}")
    private String uploadPath; // ex) /home/ubuntu/IbioScience/files/

    @Transactional
    public void saveDiscount(ProductDiscountSaveRequest req) throws RuntimeException {
        ProductDiscount discount = new ProductDiscount();

        discount.setActive(Boolean.TRUE.equals(req.getActive()));
        discount.setType(DiscountType.valueOf(req.getType()));
        discount.setTerm(DiscountTerm.valueOf(req.getTerm()));
        discount.setName(req.getName());

        discount.setConditionEnabled(Boolean.TRUE.equals(req.getPeriodEnabled()));
        if (discount.getConditionEnabled()) {
            if (req.getStartDate() != null && !req.getStartDate().isEmpty()) {
                discount.setStartDate(LocalDate.parse(req.getStartDate()));
            }
            if (req.getEndDate() != null && !req.getEndDate().isEmpty()) {
                discount.setEndDate(LocalDate.parse(req.getEndDate()));
            }
        } else {
            discount.setStartDate(null);
            discount.setEndDate(null);
        }

        // 적용대상(정확하게 Enum 정상 세팅)
        if (Boolean.TRUE.equals(req.getApplyToAll())) {
            discount.setTarget(DiscountTarget.ALL);
        } else if (Boolean.TRUE.equals(req.getApplyToDealer())) {
            discount.setTarget(DiscountTarget.DEALER);
        } else if (Boolean.TRUE.equals(req.getApplyToRegular())) {
            discount.setTarget(DiscountTarget.NORMAL);
        } else {
            discount.setTarget(DiscountTarget.ALL);
        }

        if (req.getDiscountPercent() != null && !req.getDiscountPercent().isEmpty()) {
            discount.setDiscountPercent(new BigDecimal(req.getDiscountPercent()));
        } else {
            discount.setDiscountPercent(BigDecimal.ZERO);
        }

        discount.setCouponPolicy(CouponPolicy.valueOf(req.getCouponPolicy()));

        // 1차 저장(아이콘 파일경로를 위해 id 필요)
        discount = productDiscountRepository.save(discount);

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
            String dirPath = uploadPath + "/product/discount/icon/" + discount.getId() + "/" + today;
            File dir = new File(dirPath);
            if (!dir.exists()) dir.mkdirs();

            String savePath = dirPath + "/" + fileName;
            try {
                iconFile.transferTo(new File(savePath));
            } catch (Exception ex) {
                throw new RuntimeException("아이콘 파일 저장 실패: " + ex.getMessage());
            }
            String url = "/upload/product/discount/icon/" + discount.getId() + "/" + today + "/" + fileName;
            discount.setIconPath(savePath);
            discount.setIconUrl(url);

            // 2차 저장(파일 경로 반영)
            productDiscountRepository.save(discount);
        }
    }
}
