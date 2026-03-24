package com.dev.IbioScience.helper.index;

import com.dev.IbioScience.enums.product.dealer.ProductSourceType;

public final class FrontProductRouteHelper {

    private FrontProductRouteHelper() {
    }

    /**
     * 우리회사 상품 상세 경로
     */
    public static final String COMPANY_DETAIL_BASE = "/productDetail";

    /**
     * 딜러 상품 상세 경로
     *
     * 반드시 실제 프로젝트의 프론트 딜러 상세 URL과 맞춰야 합니다.
     * 현재 주신 코드 기준으로 별도 딜러 상세 프론트 URL이 명시되지 않았으므로
     * 여기만 실제 경로로 맞추시면 됩니다.
     */
    public static final String DEALER_DETAIL_BASE = "/dealerProductDetail";

    public static String buildDetailUrl(ProductSourceType sourceType, Long id) {
        if (sourceType == null || id == null) {
            return "#";
        }

        return switch (sourceType) {
            case COMPANY -> COMPANY_DETAIL_BASE + "/" + id;
            case DEALER -> DEALER_DETAIL_BASE + "/" + id;
        };
    }

    public static String buildProductKey(ProductSourceType sourceType, Long id) {
        if (sourceType == null || id == null) {
            return null;
        }
        return sourceType.name() + "_" + id;
    }
}