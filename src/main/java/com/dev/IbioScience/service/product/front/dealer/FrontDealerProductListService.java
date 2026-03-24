package com.dev.IbioScience.service.product.front.dealer;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.enums.front.dealerProductList.DealerProductListItemDto;
import com.dev.IbioScience.enums.front.dealerProductList.DealerProductListOptionRowDto;
import com.dev.IbioScience.enums.front.dealerProductList.DealerProductListSort;
import com.dev.IbioScience.model.product.dealer.DealerProduct;
import com.dev.IbioScience.model.product.dealer.DealerProductImage;
import com.dev.IbioScience.model.product.dealer.DealerProductOption;
import com.dev.IbioScience.repository.product.dealer.DealerProductImageRepository;
import com.dev.IbioScience.repository.product.dealer.DealerProductOptionRepository;
import com.dev.IbioScience.repository.product.dealer.DealerProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FrontDealerProductListService {

    private static final String DEFAULT_PRODUCT_IMAGE_URL = "/front/image/sample/450-300.png";
    private static final String DEFAULT_DETAIL_BASE_URL = "/dealerProductDetail/";

    private final DealerProductRepository dealerProductRepository;
    private final DealerProductImageRepository dealerProductImageRepository;
    private final DealerProductOptionRepository dealerProductOptionRepository;

    public Page<DealerProductListItemDto> getDealerProductPage(
            String sortValue,
            Integer sizeValue,
            Integer pageValue
    ) {
        DealerProductListSort sort = DealerProductListSort.from(sortValue);
        int size = normalizeSize(sizeValue);
        int page = normalizePage(pageValue);

        PageRequest pageable = PageRequest.of(page, size);

        Page<Long> idPage = switch (sort) {
            case NAME_DESC -> dealerProductRepository.findFrontActiveIdsOrderByNameDesc(pageable);
            case PRICE_ASC -> dealerProductRepository.findFrontActiveIdsOrderByPriceAsc(pageable);
            case PRICE_DESC -> dealerProductRepository.findFrontActiveIdsOrderByPriceDesc(pageable);
            case NAME_ASC -> dealerProductRepository.findFrontActiveIdsOrderByNameAsc(pageable);
        };

        if (idPage.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, idPage.getTotalElements());
        }

        List<Long> productIds = idPage.getContent();

        List<DealerProduct> products = dealerProductRepository.findAllWithSellerByIdIn(productIds);
        List<DealerProductImage> mainImages = dealerProductImageRepository.findMainImagesByDealerProductIds(productIds);
        List<DealerProductOption> options = dealerProductOptionRepository.findFrontOptionsByDealerProductIds(productIds);

        Map<Long, DealerProduct> productMap = new LinkedHashMap<>();
        for (DealerProduct product : products) {
            productMap.put(product.getId(), product);
        }

        Map<Long, String> mainImageMap = new LinkedHashMap<>();
        for (DealerProductImage image : mainImages) {
            if (!mainImageMap.containsKey(image.getDealerProduct().getId())) {
                mainImageMap.put(image.getDealerProduct().getId(), hasText(image.getUrl()) ? image.getUrl() : DEFAULT_PRODUCT_IMAGE_URL);
            }
        }

        Map<Long, List<DealerProductListOptionRowDto>> optionRowMap = new LinkedHashMap<>();
        for (DealerProductOption option : options) {
            DealerProduct product = option.getGroup().getDealerProduct();
            Long productId = product.getId();

            optionRowMap.computeIfAbsent(productId, key -> new ArrayList<>())
                    .add(buildOptionRow(product, option));
        }

        List<DealerProductListItemDto> content = new ArrayList<>();
        for (Long productId : productIds) {
            DealerProduct product = productMap.get(productId);
            if (product == null) {
                continue;
            }

            List<DealerProductListOptionRowDto> optionRows =
                    optionRowMap.getOrDefault(productId, Collections.emptyList());

            boolean hasOptions = !optionRows.isEmpty();
            Integer salePrice = product.getSalePrice();

            content.add(
                DealerProductListItemDto.builder()
                    .productId(product.getId())
                    .productType("DEALER")
                    .name(defaultString(product.getName(), "-"))
                    .code(defaultString(product.getCode(), ""))
                    .shortDescription(resolveShortDescription(product))
                    .brandName(resolveBrandName(product))
                    .imageUrl(mainImageMap.getOrDefault(productId, DEFAULT_PRODUCT_IMAGE_URL))
                    .detailUrl(DEFAULT_DETAIL_BASE_URL + product.getId())
                    .salePrice(salePrice)
                    .displayPriceText(resolveDisplayPriceText(product))
                    .hasOptions(hasOptions)
                    .canDirectCart(!hasOptions && salePrice != null && salePrice > 0)
                    .optionRows(optionRows)
                    .build()
            );
        }

        return new PageImpl<>(content, pageable, idPage.getTotalElements());
    }

    private DealerProductListOptionRowDto buildOptionRow(DealerProduct product, DealerProductOption option) {
        int finalPrice = resolveOptionFinalPrice(product.getSalePrice(), option.getExtraPrice(), option.getSign() == null ? null : option.getSign().name());

        String catNo = hasText(option.getValue()) ? option.getValue() : defaultString(product.getCode(), "-");
        String optionCode = hasText(option.getValue()) ? option.getValue() : "";
        String optionName = defaultString(option.getName(), "-");
        String optionGroupName = defaultString(option.getGroup().getName(), "");
        String displayName = hasText(optionGroupName) ? optionGroupName + " - " + optionName : optionName;

        return DealerProductListOptionRowDto.builder()
                .optionGroupId(option.getGroup().getId())
                .optionGroupName(optionGroupName)
                .optionId(option.getId())
                .optionName(displayName)
                .optionCode(optionCode)
                .catNo(catNo)
                .unit("-")
                .unitPrice(finalPrice)
                .unitPriceText(formatMoney(finalPrice) + "원")
                .build();
    }

    private int resolveOptionFinalPrice(Integer baseSalePrice, BigDecimal extraPrice, String signName) {
        int base = baseSalePrice == null ? 0 : baseSalePrice;
        int extra = extraPrice == null ? 0 : extraPrice.intValue();

        if ("MINUS".equalsIgnoreCase(signName)) {
            return Math.max(0, base - extra);
        }

        return base + extra;
    }

    private String resolveShortDescription(DealerProduct product) {
        if (hasText(product.getShortDescription())) {
            return product.getShortDescription();
        }
        if (hasText(product.getSummaryDescription())) {
            return product.getSummaryDescription();
        }
        return "-";
    }

    private String resolveBrandName(DealerProduct product) {
        if (product.getSellerDealerProfile() == null) {
            return "-";
        }
        String shopName = product.getSellerDealerProfile().getShopName();
        return hasText(shopName) ? shopName : "-";
    }

    private String resolveDisplayPriceText(DealerProduct product) {
        if (Boolean.TRUE.equals(product.getUsePriceReplacementText()) && hasText(product.getPriceReplacementText())) {
            return product.getPriceReplacementText();
        }

        if (product.getSalePrice() != null) {
            return formatMoney(product.getSalePrice()) + "원";
        }

        return "-";
    }

    private int normalizeSize(Integer sizeValue) {
        int size = (sizeValue == null ? 15 : sizeValue);
        return switch (size) {
            case 15, 25, 50, 75, 100 -> size;
            default -> 15;
        };
    }

    private int normalizePage(Integer pageValue) {
        if (pageValue == null || pageValue < 0) {
            return 0;
        }
        return pageValue;
    }

    private String formatMoney(int value) {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(value);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String defaultString(String value, String defaultValue) {
        return hasText(value) ? value : defaultValue;
    }
}