package com.dev.IbioScience.service.seller.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.seller.product.DealerProductCreateRequest;
import com.dev.IbioScience.dto.seller.product.DealerProductCreateResponse;
import com.dev.IbioScience.enums.product.DisplayStatus;
import com.dev.IbioScience.enums.product.PriceExposeTarget;
import com.dev.IbioScience.enums.product.PriceSign;
import com.dev.IbioScience.enums.product.ProductImageType;
import com.dev.IbioScience.enums.product.ProductNewState;
import com.dev.IbioScience.enums.product.ProductState;
import com.dev.IbioScience.enums.product.SaleStatus;
import com.dev.IbioScience.model.auth.DealerCategoryPermission;
import com.dev.IbioScience.model.auth.SellerDealerProfile;
import com.dev.IbioScience.model.product.category.CategoryMedium;
import com.dev.IbioScience.model.product.category.CategorySmall;
import com.dev.IbioScience.model.product.dealer.DealerMediumSmallProductCategory;
import com.dev.IbioScience.model.product.dealer.DealerProduct;
import com.dev.IbioScience.model.product.dealer.DealerProductDetailImage;
import com.dev.IbioScience.model.product.dealer.DealerProductExtraField;
import com.dev.IbioScience.model.product.dealer.DealerProductImage;
import com.dev.IbioScience.model.product.dealer.DealerProductKeyword;
import com.dev.IbioScience.model.product.dealer.DealerProductOption;
import com.dev.IbioScience.model.product.dealer.DealerProductOptionGroup;
import com.dev.IbioScience.model.product.relation.MediumSmallCategory;
import com.dev.IbioScience.model.product.util.Keyword;
import com.dev.IbioScience.repository.auth.DealerCategoryPermissionRepository;
import com.dev.IbioScience.repository.category.CategoryMediumRepository;
import com.dev.IbioScience.repository.category.MediumSmallCategoryRepository;
import com.dev.IbioScience.repository.product.dealer.DealerProductRepository;
import com.dev.IbioScience.repository.product.register.KeywordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SellerProductCommandService {

    private final SellerProductAccessService sellerProductAccessService;
    private final DealerCategoryPermissionRepository dealerCategoryPermissionRepository;
    private final CategoryMediumRepository categoryMediumRepository;
    private final MediumSmallCategoryRepository mediumSmallCategoryRepository;
    private final KeywordRepository keywordRepository;
    private final DealerProductRepository dealerProductRepository;
    private final SellerProductFileService sellerProductFileService;

    public DealerProductCreateResponse createProduct(
            Long loginMemberId,
            DealerProductCreateRequest request,
            MultipartFile representativeImage,
            List<MultipartFile> additionalImages,
            MultipartFile iconImage
    ) {
        SellerDealerProfile sellerProfile = sellerProductAccessService.getSellerProfileOrThrow(loginMemberId);

        validateRequest(sellerProfile, request, representativeImage);

        DealerProduct product = new DealerProduct();
        product.setSellerDealerProfile(sellerProfile);
        product.setDisplayStatus(request.getDisplayStatus() == null ? DisplayStatus.ON : request.getDisplayStatus());
        product.setSaleStatus(request.getSaleStatus() == null ? SaleStatus.ON : request.getSaleStatus());
        product.setState(request.getState() == null ? ProductState.NORMAL : request.getState());
        product.setNewState(request.getNewState() == null ? ProductNewState.NEW : request.getNewState());

        product.setName(trimToNull(request.getName()));
        product.setCode(trimToNull(request.getCode()));
        product.setManufacturerText(trimToNull(request.getManufacturerText()));
        product.setSupplierText(trimToNull(request.getSupplierText()));
        product.setManufacturedAt(request.getManufacturedAt());
        product.setExpiredAt(request.getExpiredAt());
        product.setSummaryDescription(trimToNull(request.getSummaryDescription()));
        product.setShortDescription(trimToNull(request.getShortDescription()));
        product.setInternalProductCode(trimToNull(request.getInternalProductCode()));
        product.setConsumerPrice(request.getConsumerPrice());
        product.setSalePrice(request.getSalePrice());
        product.setPriceExposeTarget(request.getPriceExposeTarget() == null ? PriceExposeTarget.MEMBER : request.getPriceExposeTarget());
        product.setUsePriceReplacementText(Boolean.TRUE.equals(request.getUsePriceReplacementText()));
        product.setPriceReplacementText(trimToNull(request.getPriceReplacementText()));
        product.setRewardRate(request.getRewardRate());
        product.setValidFrom(request.getValidFrom());
        product.setValidTo(request.getValidTo());
        product.setUseIconPeriod(Boolean.TRUE.equals(request.getUseIconPeriod()));
        product.setIconStartDate(request.getIconStartDate());
        product.setIconEndDate(request.getIconEndDate());

        product.setSalesCount(0);
        product.setViewCount(0);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        SellerProductFileService.FinalizedDetailHtml detailResult =
                sellerProductFileService.finalizeDetailHtml(loginMemberId, request.getDetailHtml());

        product.setDetailHtml(detailResult.getFinalHtml());

        for (DealerProductDetailImage detailImage : detailResult.getDetailImages()) {
            detailImage.setDealerProduct(product);
            product.getDetailImages().add(detailImage);
        }

        SellerProductFileService.StoredFileInfo repInfo =
                sellerProductFileService.storeRepresentativeImage(loginMemberId, representativeImage);

        DealerProductImage repImage = new DealerProductImage();
        repImage.setDealerProduct(product);
        repImage.setType(ProductImageType.MAIN);
        repImage.setUrl(repInfo.getUrl());
        repImage.setPath(repInfo.getPath());
        repImage.setFileName(repInfo.getFileName());
        repImage.setSortOrder(0);
        product.getImages().add(repImage);

        List<SellerProductFileService.StoredFileInfo> additionalInfos =
                sellerProductFileService.storeAdditionalImages(loginMemberId, additionalImages);

        int addSort = 0;
        for (SellerProductFileService.StoredFileInfo fileInfo : additionalInfos) {
            DealerProductImage addImage = new DealerProductImage();
            addImage.setDealerProduct(product);
            addImage.setType(ProductImageType.ADDITIONAL);
            addImage.setUrl(fileInfo.getUrl());
            addImage.setPath(fileInfo.getPath());
            addImage.setFileName(fileInfo.getFileName());
            addImage.setSortOrder(addSort++);
            product.getImages().add(addImage);
        }

        if (iconImage != null && !iconImage.isEmpty()) {
            SellerProductFileService.StoredFileInfo iconInfo =
                    sellerProductFileService.storeIconImage(loginMemberId, iconImage);

            product.setIconUrl(iconInfo.getUrl());
            product.setIconPath(iconInfo.getPath());
            product.setIconFileName(iconInfo.getFileName());
        }

        for (DealerProductCreateRequest.CategoryMappingRequest item : normalizeCategoryMappings(request.getCategoryMappings())) {
            DealerMediumSmallProductCategory mapping = new DealerMediumSmallProductCategory();

            CategoryMedium mediumRef = new CategoryMedium();
            mediumRef.setId(item.getMediumId());

            CategorySmall smallRef = new CategorySmall();
            smallRef.setId(item.getSmallId());

            mapping.setDealerProduct(product);
            mapping.setMedium(mediumRef);
            mapping.setSmall(smallRef);

            product.getCategoryMappings().add(mapping);
        }

        for (DealerProductCreateRequest.ExtraFieldRequest field : normalizeExtraFields(request.getExtraFields())) {
            DealerProductExtraField extraField = new DealerProductExtraField();
            extraField.setDealerProduct(product);
            extraField.setLabel(field.getLabel().trim());
            extraField.setValue(field.getValue().trim());
            product.getExtraFields().add(extraField);
        }

        List<String> normalizedKeywords = normalizeKeywords(request.getKeywords());
        Map<String, Keyword> keywordMap = new LinkedHashMap<>();
        for (Keyword keyword : keywordRepository.findByWordIn(normalizedKeywords)) {
            keywordMap.put(keyword.getWord(), keyword);
        }

        List<Keyword> newKeywords = new ArrayList<>();
        for (String word : normalizedKeywords) {
            if (!keywordMap.containsKey(word)) {
                Keyword keyword = new Keyword();
                keyword.setWord(word);
                newKeywords.add(keyword);
                keywordMap.put(word, keyword);
            }
        }

        if (!newKeywords.isEmpty()) {
            keywordRepository.saveAll(newKeywords);
        }

        for (String word : normalizedKeywords) {
            DealerProductKeyword mapping = new DealerProductKeyword();
            mapping.setDealerProduct(product);
            mapping.setKeyword(keywordMap.get(word));
            product.getKeywordMappings().add(mapping);
        }

        List<DealerProductCreateRequest.OptionGroupRequest> groupRequests = normalizeOptionGroups(request.getOptionGroups());
        int groupSort = 0;
        for (DealerProductCreateRequest.OptionGroupRequest groupRequest : groupRequests) {
            DealerProductOptionGroup group = new DealerProductOptionGroup();
            group.setDealerProduct(product);
            group.setName(groupRequest.getName().trim());
            group.setSortOrder(groupRequest.getSortOrder() == null ? groupSort : groupRequest.getSortOrder());

            List<DealerProductCreateRequest.OptionRequest> options = groupRequest.getOptions() == null
                    ? List.of()
                    : groupRequest.getOptions();

            int optionSort = 0;
            for (DealerProductCreateRequest.OptionRequest optionRequest : options) {
                if (isBlank(optionRequest.getName()) && isBlank(optionRequest.getValue())) {
                    continue;
                }

                if (isBlank(optionRequest.getName())) {
                    throw new IllegalArgumentException("옵션명은 비어 있을 수 없습니다.");
                }
                if (isBlank(optionRequest.getValue())) {
                    throw new IllegalArgumentException("옵션값은 비어 있을 수 없습니다.");
                }

                DealerProductOption option = new DealerProductOption();
                option.setGroup(group);
                option.setName(optionRequest.getName().trim());
                option.setValue(optionRequest.getValue().trim());
                option.setExtraPrice(optionRequest.getExtraPrice() == null ? BigDecimal.ZERO : optionRequest.getExtraPrice());
                option.setSign(optionRequest.getSign() == null ? PriceSign.PLUS : optionRequest.getSign());
                option.setSortOrder(optionRequest.getSortOrder() == null ? optionSort : optionRequest.getSortOrder());

                group.getOptions().add(option);
                optionSort++;
            }

            if (!group.getOptions().isEmpty()) {
                product.getOptionGroups().add(group);
                groupSort++;
            }
        }

        DealerProduct saved = dealerProductRepository.save(product);

        return DealerProductCreateResponse.builder()
                .dealerProductId(saved.getId())
                .message("딜러 상품이 등록되었습니다.")
                .build();
    }

    private void validateRequest(SellerDealerProfile sellerProfile, DealerProductCreateRequest request, MultipartFile representativeImage) {
        if (request == null) {
            throw new IllegalArgumentException("상품 등록 데이터가 없습니다.");
        }

        if (isBlank(request.getName())) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }

        if (isBlank(request.getCode())) {
            throw new IllegalArgumentException("품목코드는 필수입니다.");
        }

        if (representativeImage == null || representativeImage.isEmpty()) {
            throw new IllegalArgumentException("대표 이미지는 필수입니다.");
        }

        if (request.getCategoryMappings() == null || request.getCategoryMappings().isEmpty()) {
            throw new IllegalArgumentException("최소 1개의 상품 분류를 선택해야 합니다.");
        }

        List<String> keywords = normalizeKeywords(request.getKeywords());
        if (keywords.isEmpty()) {
            throw new IllegalArgumentException("키워드는 최소 1개 이상 입력해야 합니다.");
        }

        if (dealerProductRepository.existsBySellerDealerProfileIdAndCode(sellerProfile.getId(), request.getCode().trim())) {
            throw new IllegalArgumentException("이미 사용 중인 품목코드입니다.");
        }

        validateCategoryPermissions(sellerProfile.getId(), request.getCategoryMappings());

        if (Boolean.TRUE.equals(request.getUsePriceReplacementText()) && isBlank(request.getPriceReplacementText())) {
            throw new IllegalArgumentException("판매가 대체문구 사용 시 문구를 입력해야 합니다.");
        }

        if (request.getValidFrom() != null && request.getValidTo() != null && request.getValidFrom().isAfter(request.getValidTo())) {
            throw new IllegalArgumentException("유효기간 시작일은 종료일보다 늦을 수 없습니다.");
        }

        if (Boolean.TRUE.equals(request.getUseIconPeriod())
                && request.getIconStartDate() != null
                && request.getIconEndDate() != null
                && request.getIconStartDate().isAfter(request.getIconEndDate())) {
            throw new IllegalArgumentException("아이콘 시작일은 종료일보다 늦을 수 없습니다.");
        }
    }

    private void validateCategoryPermissions(Long sellerProfileId, List<DealerProductCreateRequest.CategoryMappingRequest> categoryMappings) {
        Set<String> allowedKeys = buildAllowedCategoryKeys(sellerProfileId);

        for (DealerProductCreateRequest.CategoryMappingRequest mapping : normalizeCategoryMappings(categoryMappings)) {
            String key = mapping.getMediumId() + "_" + mapping.getSmallId();
            if (!allowedKeys.contains(key)) {
                throw new IllegalArgumentException("등록 권한이 없는 분류가 포함되어 있습니다. mediumId="
                        + mapping.getMediumId() + ", smallId=" + mapping.getSmallId());
            }
        }
    }

    private Set<String> buildAllowedCategoryKeys(Long sellerProfileId) {
        List<DealerCategoryPermission> permissions =
                dealerCategoryPermissionRepository.findAllWithCategoryBySellerProfileId(sellerProfileId);

        Set<Long> largeIds = new LinkedHashSet<>();
        for (DealerCategoryPermission permission : permissions) {
            largeIds.add(permission.getLarge().getId());
        }

        List<CategoryMedium> mediums = categoryMediumRepository.findByLargeIdInOrderByNameAsc(largeIds);

        Map<Long, List<CategoryMedium>> mediumsByLargeId = new LinkedHashMap<>();
        for (CategoryMedium medium : mediums) {
            mediumsByLargeId.computeIfAbsent(medium.getLarge().getId(), k -> new ArrayList<>()).add(medium);
        }

        Set<Long> mediumIds = new LinkedHashSet<>();
        for (CategoryMedium medium : mediums) {
            mediumIds.add(medium.getId());
        }

        Map<Long, List<Long>> smallIdsByMediumId = new LinkedHashMap<>();
        if (!mediumIds.isEmpty()) {
            List<MediumSmallCategory> relations = mediumSmallCategoryRepository.findAllByMediumIds(mediumIds);
            for (MediumSmallCategory relation : relations) {
                smallIdsByMediumId.computeIfAbsent(relation.getMedium().getId(), k -> new ArrayList<>())
                        .add(relation.getSmall().getId());
            }
        }

        Set<String> allowedKeys = new LinkedHashSet<>();

        for (DealerCategoryPermission permission : permissions) {
            Long largeId = permission.getLarge().getId();
            CategoryMedium medium = permission.getMedium();
            CategorySmall small = permission.getSmall();

            if (medium == null) {
                List<CategoryMedium> mediumList = mediumsByLargeId.getOrDefault(largeId, List.of());
                for (CategoryMedium item : mediumList) {
                    for (Long smallId : smallIdsByMediumId.getOrDefault(item.getId(), List.of())) {
                        allowedKeys.add(item.getId() + "_" + smallId);
                    }
                }
                continue;
            }

            if (small == null) {
                for (Long smallId : smallIdsByMediumId.getOrDefault(medium.getId(), List.of())) {
                    allowedKeys.add(medium.getId() + "_" + smallId);
                }
            } else {
                allowedKeys.add(medium.getId() + "_" + small.getId());
            }
        }

        return allowedKeys;
    }

    private List<DealerProductCreateRequest.CategoryMappingRequest> normalizeCategoryMappings(
            List<DealerProductCreateRequest.CategoryMappingRequest> mappings
    ) {
        if (mappings == null) {
            return List.of();
        }

        Map<String, DealerProductCreateRequest.CategoryMappingRequest> map = new LinkedHashMap<>();
        for (DealerProductCreateRequest.CategoryMappingRequest item : mappings) {
            if (item == null || item.getMediumId() == null || item.getSmallId() == null) {
                continue;
            }
            map.put(item.getMediumId() + "_" + item.getSmallId(), item);
        }
        return new ArrayList<>(map.values());
    }

    private List<DealerProductCreateRequest.ExtraFieldRequest> normalizeExtraFields(
            List<DealerProductCreateRequest.ExtraFieldRequest> fields
    ) {
        if (fields == null) {
            return List.of();
        }

        List<DealerProductCreateRequest.ExtraFieldRequest> result = new ArrayList<>();
        for (DealerProductCreateRequest.ExtraFieldRequest field : fields) {
            if (field == null) {
                continue;
            }

            boolean labelBlank = isBlank(field.getLabel());
            boolean valueBlank = isBlank(field.getValue());

            if (labelBlank && valueBlank) {
                continue;
            }

            if (labelBlank || valueBlank) {
                throw new IllegalArgumentException("추가 항목은 질문과 답변을 모두 입력해야 합니다.");
            }

            result.add(field);
        }
        return result;
    }

    private List<String> normalizeKeywords(List<String> keywords) {
        if (keywords == null) {
            return List.of();
        }

        Set<String> set = new LinkedHashSet<>();
        for (String keyword : keywords) {
            String value = trimToNull(keyword);
            if (value != null) {
                set.add(value);
            }
        }
        return new ArrayList<>(set);
    }

    private List<DealerProductCreateRequest.OptionGroupRequest> normalizeOptionGroups(
            List<DealerProductCreateRequest.OptionGroupRequest> groups
    ) {
        if (groups == null) {
            return List.of();
        }

        List<DealerProductCreateRequest.OptionGroupRequest> result = new ArrayList<>();
        for (DealerProductCreateRequest.OptionGroupRequest group : groups) {
            if (group == null) {
                continue;
            }

            if (isBlank(group.getName()) && (group.getOptions() == null || group.getOptions().isEmpty())) {
                continue;
            }

            if (isBlank(group.getName())) {
                throw new IllegalArgumentException("옵션 그룹명은 비어 있을 수 없습니다.");
            }

            result.add(group);
        }
        return result;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}