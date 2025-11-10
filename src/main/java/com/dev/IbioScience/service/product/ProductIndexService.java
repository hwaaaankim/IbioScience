package com.dev.IbioScience.service.product;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.dev.IbioScience.dto.page.index.ProductCardDTO;
import com.dev.IbioScience.repository.product.ProductIndexRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductIndexService {

    private final ProductIndexRepository repo;

    public List<ProductCardDTO> topViewed(int limit) {
        return map(repo.findTopViewedRaw(limit));
    }

    public List<ProductCardDTO> topSales(int limit) {
        return map(repo.findTopSalesRaw(limit));
    }

    public List<ProductCardDTO> promotionOldest(int limit) {
        return map(repo.findPromotionOldestRaw(limit));
    }

    private List<ProductCardDTO> map(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) return Collections.emptyList();
        return rows.stream().map(r -> {
            Long id                 = toLong(r[0]);
            String name             = (String) r[1];
            Integer salePrice       = toInt(r[2]);
            Integer consumerPrice   = toInt(r[3]);
            Integer salesCount      = toInt(r[4]);
            Integer viewCount       = toInt(r[5]);
            Double averageRating    = toDouble(r[6]);
            Integer reviewCount     = toInt(r[7]);
            String mainImageUrl     = (String) r[8];
            Integer discountRate    = toInt(r[9]);
            String labelsConcat     = (String) r[10];

            Integer discountedPrice = null;
            if (discountRate != null && salePrice != null) {
                // (정책) 할인율은 salePrice 기준으로 계산
                discountedPrice = Math.max(0, Math.round(salePrice * (100 - discountRate) / 100f));
            }

            List<String> labels = (labelsConcat == null || labelsConcat.isBlank())
                    ? Collections.emptyList()
                    : Arrays.stream(labelsConcat.split("\\|\\|"))
                            .filter(s -> s != null && !s.isBlank())
                            .distinct()
                            .collect(Collectors.toList());

            return ProductCardDTO.builder()
                    .id(id)
                    .name(name)
                    .salePrice(salePrice)
                    .consumerPrice(consumerPrice)
                    .salesCount(salesCount)
                    .viewCount(viewCount)
                    .averageRating(averageRating == null ? 0.0 : averageRating)
                    .reviewCount(reviewCount == null ? 0 : reviewCount)
                    .mainImageUrl(mainImageUrl)
                    .discountRate(discountRate)
                    .discountedPrice(discountedPrice)
                    .promotionLabels(labels)
                    .build();
        }).collect(Collectors.toList());
    }

    private static Long toLong(Object o) { return o == null ? null : ((Number)o).longValue(); }
    private static Integer toInt(Object o){ return o == null ? null : ((Number)o).intValue(); }
    private static Double toDouble(Object o){ return o == null ? null : ((Number)o).doubleValue(); }
}