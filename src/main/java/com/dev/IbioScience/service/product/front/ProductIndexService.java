package com.dev.IbioScience.service.product.front;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.dev.IbioScience.dto.page.index.ProductCardDTO;
import com.dev.IbioScience.enums.product.DisplayStatus;
import com.dev.IbioScience.enums.product.ProductImageType;
import com.dev.IbioScience.enums.product.ProductState;
import com.dev.IbioScience.enums.product.SaleStatus;
import com.dev.IbioScience.enums.product.dealer.ProductSourceType;
import com.dev.IbioScience.helper.index.FrontProductRouteHelper;
import com.dev.IbioScience.repository.product.ProductIndexRepository;
import com.dev.IbioScience.repository.product.dealer.DealerProductIndexRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductIndexService {

    /**
     * 주신 설명 기준으로 front 노출 대상은 ON / ON / NORMAL 로 가정했습니다.
     * enum 명이 실제 프로젝트에서 다르면 이 상수 3개만 맞추시면 됩니다.
     */
    private static final DisplayStatus FRONT_DISPLAY_STATUS = DisplayStatus.ON;
    private static final SaleStatus FRONT_SALE_STATUS = SaleStatus.ON;
    private static final ProductState FRONT_PRODUCT_STATE = ProductState.NORMAL;

    private final ProductIndexRepository repo;
    private final DealerProductIndexRepository dealerRepo;

    public List<ProductCardDTO> topViewed(int limit) {
        List<ProductCardDTO> company = mapCompany(repo.findTopViewedRaw(limit));
        List<ProductCardDTO> dealer = mapDealer(
                dealerRepo.findTopViewedRaw(
                        FRONT_DISPLAY_STATUS,
                        FRONT_SALE_STATUS,
                        FRONT_PRODUCT_STATE,
                        ProductImageType.MAIN,
                        PageRequest.of(0, limit)
                )
        );

        return mergeByIntMetric(company, dealer, ProductCardDTO::getViewCount, limit);
    }

    public List<ProductCardDTO> topSales(int limit) {
        List<ProductCardDTO> company = mapCompany(repo.findTopSalesRaw(limit));
        List<ProductCardDTO> dealer = mapDealer(
                dealerRepo.findTopSalesRaw(
                        FRONT_DISPLAY_STATUS,
                        FRONT_SALE_STATUS,
                        FRONT_PRODUCT_STATE,
                        ProductImageType.MAIN,
                        PageRequest.of(0, limit)
                )
        );

        return mergeByIntMetric(company, dealer, ProductCardDTO::getSalesCount, limit);
    }

    /**
     * 딜러상품 엔티티에는 프로모션/이벤트 정보가 없으므로
     * 이벤트 영역은 기존 우리회사 상품 기준만 유지합니다.
     */
    public List<ProductCardDTO> promotionOldest(int limit) {
        return mapCompany(repo.findPromotionOldestRaw(limit));
    }

    /**
     * 이벤트 상품이 0건이면 랜덤 fallback
     * fallback 에는 딜러상품도 함께 포함
     */
    public List<ProductCardDTO> promotionOldestOrRandom(int limit) {
        List<ProductCardDTO> promo = promotionOldest(limit);
        if (promo == null || promo.isEmpty()) {
            return random(limit);
        }
        return promo;
    }

    /**
     * 랜덤 상품 조회 (fallback)
     */
    public List<ProductCardDTO> random(int limit) {
        List<ProductCardDTO> company = mapCompany(repo.findRandomRaw(limit));
        List<ProductCardDTO> dealer = mapDealer(
                dealerRepo.findRandomRaw(
                        FRONT_DISPLAY_STATUS.name(),
                        FRONT_SALE_STATUS.name(),
                        FRONT_PRODUCT_STATE.name(),
                        PageRequest.of(0, limit)
                )
        );

        List<ProductCardDTO> merged = new ArrayList<>();
        merged.addAll(company);
        merged.addAll(dealer);

        Collections.shuffle(merged);

        if (merged.size() <= limit) {
            return merged;
        }
        return new ArrayList<>(merged.subList(0, limit));
    }

    private List<ProductCardDTO> mapCompany(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        return rows.stream()
                .map(r -> {
                    Long id = toLong(r[0]);
                    String name = (String) r[1];
                    Integer salePrice = toInt(r[2]);
                    Integer consumerPrice = toInt(r[3]);
                    Integer salesCount = toInt(r[4]);
                    Integer viewCount = toInt(r[5]);
                    Double averageRating = toDouble(r[6]);
                    Integer reviewCount = toInt(r[7]);
                    String mainImageUrl = (String) r[8];
                    Integer discountRate = toInt(r[9]);
                    String labelsConcat = (String) r[10];

                    Integer discountedPrice = null;
                    if (discountRate != null && salePrice != null) {
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
                            .productSourceType(ProductSourceType.COMPANY)
                            .productSourceLabel(ProductSourceType.COMPANY.getLabel())
                            .productKey(FrontProductRouteHelper.buildProductKey(ProductSourceType.COMPANY, id))
                            .detailUrl(FrontProductRouteHelper.buildDetailUrl(ProductSourceType.COMPANY, id))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<ProductCardDTO> mapDealer(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        return rows.stream()
                .map(r -> {
                    Long id = toLong(r[0]);
                    String name = (String) r[1];
                    Integer salePrice = toInt(r[2]);
                    Integer consumerPrice = toInt(r[3]);
                    Integer salesCount = toInt(r[4]);
                    Integer viewCount = toInt(r[5]);
                    String mainImageUrl = (String) r[8];

                    return ProductCardDTO.builder()
                            .id(id)
                            .name(name)
                            .salePrice(salePrice)
                            .consumerPrice(consumerPrice)
                            .salesCount(salesCount)
                            .viewCount(viewCount)
                            .averageRating(0.0)
                            .reviewCount(0)
                            .mainImageUrl(mainImageUrl)
                            .discountRate(null)
                            .discountedPrice(null)
                            .promotionLabels(Collections.emptyList())
                            .productSourceType(ProductSourceType.DEALER)
                            .productSourceLabel(ProductSourceType.DEALER.getLabel())
                            .productKey(FrontProductRouteHelper.buildProductKey(ProductSourceType.DEALER, id))
                            .detailUrl(FrontProductRouteHelper.buildDetailUrl(ProductSourceType.DEALER, id))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<ProductCardDTO> mergeByIntMetric(List<ProductCardDTO> company,
                                                  List<ProductCardDTO> dealer,
                                                  Function<ProductCardDTO, Integer> metricGetter,
                                                  int limit) {
        return Stream.concat(company.stream(), dealer.stream())
                .sorted(
                        Comparator.comparingInt((ProductCardDTO p) -> intOrMinusOne(metricGetter.apply(p)))
                                .reversed()
                                .thenComparing(ProductCardDTO::getId, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .limit(limit)
                .collect(Collectors.toList());
    }

    private static int intOrMinusOne(Integer value) {
        return value == null ? -1 : value;
    }

    private static Long toLong(Object o) {
        return o == null ? null : ((Number) o).longValue();
    }

    private static Integer toInt(Object o) {
        return o == null ? null : ((Number) o).intValue();
    }

    private static Double toDouble(Object o) {
        return o == null ? null : ((Number) o).doubleValue();
    }
}