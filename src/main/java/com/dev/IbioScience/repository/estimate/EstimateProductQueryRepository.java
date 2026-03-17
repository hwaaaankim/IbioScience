package com.dev.IbioScience.repository.estimate;

import java.util.List;

import com.dev.IbioScience.dto.estimate.EstimateProductRowDto;

public interface EstimateProductQueryRepository {

    List<EstimateProductRowDto> findInitialSelectedItems(Long productId, Long mappingId);
    

    List<EstimateProductRowDto> searchProducts(
            Long largeId,
            Long mediumId,
            Long smallId,
            String productKeyword,
            String brandKeyword
    );

    List<String> searchBrandSuggestions(
            Long largeId,
            Long mediumId,
            Long smallId,
            String productKeyword,
            String brandKeyword,
            int limit
    );

    List<String> searchProductSuggestions(
            Long largeId,
            Long mediumId,
            Long smallId,
            String productKeyword,
            String brandKeyword,
            int limit
    );
}