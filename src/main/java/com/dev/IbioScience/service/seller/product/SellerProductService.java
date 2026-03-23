package com.dev.IbioScience.service.seller.product;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.seller.product.SellerProductDetailResponse;
import com.dev.IbioScience.dto.seller.product.SellerProductUpdateRequest;
import com.dev.IbioScience.enums.product.ProductImageType;
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
import com.dev.IbioScience.model.product.util.Keyword;
import com.dev.IbioScience.repository.auth.DealerCategoryPermissionRepository;
import com.dev.IbioScience.repository.auth.SellerDealerProfileRepository;
import com.dev.IbioScience.repository.category.CategoryMediumRepository;
import com.dev.IbioScience.repository.category.CategorySmallRepository;
import com.dev.IbioScience.repository.category.MediumSmallCategoryRepository;
import com.dev.IbioScience.repository.product.dealer.DealerProductRepository;
import com.dev.IbioScience.repository.product.register.KeywordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SellerProductService {

    private final SellerDealerProfileRepository sellerDealerProfileRepository;
    private final DealerProductRepository dealerProductRepository;
    private final DealerCategoryPermissionRepository dealerCategoryPermissionRepository;
    private final CategoryMediumRepository categoryMediumRepository;
    private final CategorySmallRepository categorySmallRepository;
    private final MediumSmallCategoryRepository mediumSmallCategoryRepository;
    private final KeywordRepository keywordRepository;
    private final SellerProductFileService sellerProductFileService;

    @Transactional(readOnly = true)
    public SellerProductDetailResponse getProductDetail(Long sellerMemberId, Long dealerProductId) {
        DealerProduct product = getOwnedProduct(sellerMemberId, dealerProductId);

        DealerProductImage representativeImage = product.getImages().stream()
                .filter(image -> image.getType() == ProductImageType.MAIN)
                .findFirst()
                .orElse(null);

        List<DealerProductImage> additionalImages = product.getImages().stream()
                .filter(image -> image.getType() == ProductImageType.ADDITIONAL)
                .sorted(Comparator.comparing(image -> image.getSortOrder() == null ? Integer.MAX_VALUE : image.getSortOrder()))
                .toList();

        return SellerProductDetailResponse.builder()
                .id(product.getId())
                .displayStatus(product.getDisplayStatus())
                .saleStatus(product.getSaleStatus())
                .state(product.getState())
                .newState(product.getNewState())
                .name(product.getName())
                .code(product.getCode())
                .manufacturerText(product.getManufacturerText())
                .supplierText(product.getSupplierText())
                .manufacturedAt(product.getManufacturedAt())
                .expiredAt(product.getExpiredAt())
                .detailHtml(product.getDetailHtml())
                .summaryDescription(product.getSummaryDescription())
                .shortDescription(product.getShortDescription())
                .internalProductCode(product.getInternalProductCode())
                .consumerPrice(product.getConsumerPrice())
                .salePrice(product.getSalePrice())
                .priceExposeTarget(product.getPriceExposeTarget())
                .usePriceReplacementText(product.getUsePriceReplacementText())
                .priceReplacementText(product.getPriceReplacementText())
                .rewardRate(product.getRewardRate())
                .validFrom(product.getValidFrom())
                .validTo(product.getValidTo())
                .useIconPeriod(product.getUseIconPeriod())
                .iconStartDate(product.getIconStartDate())
                .iconEndDate(product.getIconEndDate())
                .representativeImage(representativeImage == null ? null :
                        SellerProductDetailResponse.SimpleImageResponse.builder()
                                .url(representativeImage.getUrl())
                                .fileName(representativeImage.getFileName())
                                .build())
                .iconImage(hasText(product.getIconUrl()) ?
                        SellerProductDetailResponse.SimpleImageResponse.builder()
                                .url(product.getIconUrl())
                                .fileName(product.getIconFileName())
                                .build() : null)
                .additionalImages(additionalImages.stream()
                        .map(image -> SellerProductDetailResponse.AdditionalImageResponse.builder()
                                .id(image.getId())
                                .url(image.getUrl())
                                .fileName(image.getFileName())
                                .sortOrder(image.getSortOrder())
                                .build())
                        .toList())
                .categoryMappings(product.getCategoryMappings().stream()
                        .map(mapping -> SellerProductDetailResponse.CategoryMappingResponse.builder()
                                .largeId(mapping.getMedium().getLarge().getId())
                                .largeName(mapping.getMedium().getLarge().getName())
                                .mediumId(mapping.getMedium().getId())
                                .mediumName(mapping.getMedium().getName())
                                .smallId(mapping.getSmall().getId())
                                .smallName(mapping.getSmall().getName())
                                .build())
                        .toList())
                .extraFields(product.getExtraFields().stream()
                        .map(field -> SellerProductDetailResponse.ExtraFieldResponse.builder()
                                .label(field.getLabel())
                                .value(field.getValue())
                                .build())
                        .toList())
                .keywords(product.getKeywordMappings().stream()
                        .map(mapping -> mapping.getKeyword().getWord())
                        .toList())
                .optionGroups(product.getOptionGroups().stream()
                        .sorted(Comparator.comparing(group -> group.getSortOrder() == null ? Integer.MAX_VALUE : group.getSortOrder()))
                        .map(group -> SellerProductDetailResponse.OptionGroupResponse.builder()
                                .name(group.getName())
                                .sortOrder(group.getSortOrder())
                                .options(group.getOptions().stream()
                                        .sorted(Comparator.comparing(option -> option.getSortOrder() == null ? Integer.MAX_VALUE : option.getSortOrder()))
                                        .map(option -> SellerProductDetailResponse.OptionResponse.builder()
                                                .name(option.getName())
                                                .value(option.getValue())
                                                .extraPrice(option.getExtraPrice())
                                                .sign(option.getSign())
                                                .sortOrder(option.getSortOrder())
                                                .build())
                                        .toList())
                                .build())
                        .toList())
                .build();
    }

    public void updateProduct(
            Long sellerMemberId,
            Long dealerProductId,
            SellerProductUpdateRequest request,
            MultipartFile representativeImage,
            MultipartFile iconImage,
            List<String> newAdditionalImageUids,
            List<MultipartFile> newAdditionalImages
    ) {
        SellerDealerProfile sellerProfile = sellerDealerProfileRepository.findByMemberId(sellerMemberId)
                .orElseThrow(() -> new IllegalArgumentException("판매딜러 프로필을 찾을 수 없습니다."));

        DealerProduct product = getOwnedProduct(sellerMemberId, dealerProductId);

        validateRequest(sellerProfile, product, request, representativeImage);

        applyBasicFields(product, request);
        syncCategoryMappings(product, request.getCategoryMappings(), sellerProfile);
        syncRepresentativeImage(product, sellerMemberId, request, representativeImage);
        syncIconImage(product, sellerMemberId, request, iconImage);
        syncAdditionalImages(product, sellerMemberId, request, newAdditionalImageUids, newAdditionalImages);
        syncDetailHtmlAndDetailImages(product, sellerMemberId, request.getDetailHtml());
        syncExtraFields(product, request.getExtraFields());
        syncKeywords(product, request.getKeywords());
        syncOptionGroups(product, request.getOptionGroups());

        product.setUpdatedAt(LocalDateTime.now());
    }

    private void validateRequest(
            SellerDealerProfile sellerProfile,
            DealerProduct product,
            SellerProductUpdateRequest request,
            MultipartFile representativeImage
    ) {
        if (!hasText(request.getName())) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }
        if (!hasText(request.getCode())) {
            throw new IllegalArgumentException("품목코드는 필수입니다.");
        }
        if (request.getCategoryMappings() == null || request.getCategoryMappings().isEmpty()) {
            throw new IllegalArgumentException("최소 1개의 분류를 선택해야 합니다.");
        }
        if (request.getKeywords() == null || request.getKeywords().stream().filter(this::hasText).findAny().isEmpty()) {
            throw new IllegalArgumentException("키워드는 최소 1개 이상 입력해야 합니다.");
        }

        boolean currentlyHasRepresentative = product.getImages().stream()
                .anyMatch(image -> image.getType() == ProductImageType.MAIN);

        boolean hasNewRepresentative = representativeImage != null && !representativeImage.isEmpty();
        boolean willRemoveRepresentative = Boolean.TRUE.equals(request.getRemoveRepresentativeImage());

        boolean finalRepresentativeExists =
                hasNewRepresentative || (currentlyHasRepresentative && !willRemoveRepresentative);

        if (!finalRepresentativeExists) {
            throw new IllegalArgumentException("대표 이미지는 반드시 1장 유지되어야 합니다.");
        }

        boolean codeDuplicated = dealerProductRepository.existsBySellerDealerProfileIdAndCodeAndIdNot(
                sellerProfile.getId(),
                request.getCode().trim(),
                product.getId()
        );
        if (codeDuplicated) {
            throw new IllegalArgumentException("이미 사용 중인 품목코드입니다.");
        }

        validateCategoryMappingsAllowed(sellerProfile.getId(), request.getCategoryMappings());
        validateAdditionalImageOrders(request.getAdditionalImageOrders());
    }

    private void validateAdditionalImageOrders(List<SellerProductUpdateRequest.AdditionalImageOrderRequest> orders) {
        if (orders == null) {
            return;
        }

        Set<String> seen = new HashSet<>();
        for (SellerProductUpdateRequest.AdditionalImageOrderRequest order : orders) {
            if (order.getType() == null) {
                throw new IllegalArgumentException("추가 이미지 정렬 정보가 올바르지 않습니다.");
            }

            String key;
            if (order.getType() == SellerProductUpdateRequest.AdditionalImageSourceType.EXISTING) {
                if (order.getImageId() == null) {
                    throw new IllegalArgumentException("기존 추가 이미지 ID 가 누락되었습니다.");
                }
                key = "EXISTING:" + order.getImageId();
            } else {
                if (!hasText(order.getUploadUid())) {
                    throw new IllegalArgumentException("신규 추가 이미지 UID 가 누락되었습니다.");
                }
                key = "NEW:" + order.getUploadUid();
            }

            if (!seen.add(key)) {
                throw new IllegalArgumentException("추가 이미지 정렬 정보에 중복이 있습니다.");
            }
        }
    }

    private void validateCategoryMappingsAllowed(
            Long sellerDealerProfileId,
            List<SellerProductUpdateRequest.CategoryMappingRequest> categoryMappings
    ) {
        Set<String> allowedKeys = buildAllowedCategoryKeys(sellerDealerProfileId);

        for (SellerProductUpdateRequest.CategoryMappingRequest mapping : categoryMappings) {
            if (mapping.getMediumId() == null || mapping.getSmallId() == null) {
                throw new IllegalArgumentException("중분류/소분류 정보가 누락되었습니다.");
            }

            boolean linked = mediumSmallCategoryRepository.existsByMediumIdAndSmallId(mapping.getMediumId(), mapping.getSmallId());
            if (!linked) {
                throw new IllegalArgumentException("중분류와 소분류 연결이 올바르지 않습니다.");
            }

            String key = mapping.getMediumId() + ":" + mapping.getSmallId();
            if (!allowedKeys.contains(key)) {
                throw new IllegalArgumentException("해당 분류에는 상품을 등록/수정할 수 없습니다.");
            }
        }
    }

    private Set<String> buildAllowedCategoryKeys(Long sellerDealerProfileId) {
        List<DealerCategoryPermission> permissions =
                dealerCategoryPermissionRepository.findBySellerDealerProfileId(sellerDealerProfileId);

        Set<String> result = new HashSet<>();

        for (DealerCategoryPermission permission : permissions) {
            Long largeId = permission.getLarge().getId();
            Long mediumId = permission.getMedium() != null ? permission.getMedium().getId() : null;
            Long smallId = permission.getSmall() != null ? permission.getSmall().getId() : null;

            if (mediumId == null && smallId == null) {
                List<CategoryMedium> mediums = categoryMediumRepository.findByLargeId(largeId);
                for (CategoryMedium medium : mediums) {
                    mediumSmallCategoryRepository.findByMediumId(medium.getId())
                            .forEach(ms -> result.add(ms.getMedium().getId() + ":" + ms.getSmall().getId()));
                }
                continue;
            }

            if (mediumId != null && smallId == null) {
                mediumSmallCategoryRepository.findByMediumId(mediumId)
                        .forEach(ms -> result.add(ms.getMedium().getId() + ":" + ms.getSmall().getId()));
                continue;
            }

            if (mediumId != null) {
                result.add(mediumId + ":" + smallId);
            }
        }

        return result;
    }

    private void applyBasicFields(DealerProduct product, SellerProductUpdateRequest request) {
        product.setDisplayStatus(request.getDisplayStatus());
        product.setSaleStatus(request.getSaleStatus());
        product.setState(request.getState());
        product.setNewState(request.getNewState());

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
        product.setPriceExposeTarget(request.getPriceExposeTarget());
        product.setUsePriceReplacementText(Boolean.TRUE.equals(request.getUsePriceReplacementText()));
        product.setPriceReplacementText(trimToNull(request.getPriceReplacementText()));
        product.setRewardRate(request.getRewardRate());

        product.setValidFrom(request.getValidFrom());
        product.setValidTo(request.getValidTo());

        product.setUseIconPeriod(Boolean.TRUE.equals(request.getUseIconPeriod()));
        product.setIconStartDate(request.getIconStartDate());
        product.setIconEndDate(request.getIconEndDate());
    }

    private void syncCategoryMappings(
            DealerProduct product,
            List<SellerProductUpdateRequest.CategoryMappingRequest> categoryMappings,
            SellerDealerProfile sellerProfile
    ) {
        List<SellerProductUpdateRequest.CategoryMappingRequest> requestedList =
                categoryMappings == null ? List.of() : categoryMappings;

        LinkedHashMap<String, SellerProductUpdateRequest.CategoryMappingRequest> requestedMap = new LinkedHashMap<>();

        for (SellerProductUpdateRequest.CategoryMappingRequest item : requestedList) {
            if (item == null || item.getMediumId() == null || item.getSmallId() == null) {
                continue;
            }

            String key = item.getMediumId() + ":" + item.getSmallId();
            requestedMap.putIfAbsent(key, item);
        }

        Map<Long, CategoryMedium> mediumMap = categoryMediumRepository.findAllById(
                requestedMap.values().stream()
                        .map(SellerProductUpdateRequest.CategoryMappingRequest::getMediumId)
                        .distinct()
                        .toList()
        ).stream().collect(Collectors.toMap(CategoryMedium::getId, medium -> medium));

        Map<Long, CategorySmall> smallMap = categorySmallRepository.findAllById(
                requestedMap.values().stream()
                        .map(SellerProductUpdateRequest.CategoryMappingRequest::getSmallId)
                        .distinct()
                        .toList()
        ).stream().collect(Collectors.toMap(CategorySmall::getId, small -> small));

        Iterator<DealerMediumSmallProductCategory> iterator = product.getCategoryMappings().iterator();

        while (iterator.hasNext()) {
            DealerMediumSmallProductCategory existing = iterator.next();

            Long existingMediumId = existing.getMedium() != null ? existing.getMedium().getId() : null;
            Long existingSmallId = existing.getSmall() != null ? existing.getSmall().getId() : null;

            String key = existingMediumId + ":" + existingSmallId;

            if (requestedMap.containsKey(key)) {
                // 이미 DB/영속성 컨텍스트에 있는 동일 매핑이면 유지
                requestedMap.remove(key);
                continue;
            }

            // 요청에서 빠진 기존 매핑만 제거
            iterator.remove();
        }

        // 요청에는 있지만 기존에 없던 매핑만 신규 추가
        for (SellerProductUpdateRequest.CategoryMappingRequest item : requestedMap.values()) {
            CategoryMedium medium = mediumMap.get(item.getMediumId());
            CategorySmall small = smallMap.get(item.getSmallId());

            if (medium == null || small == null) {
                throw new IllegalArgumentException("카테고리 정보가 올바르지 않습니다.");
            }

            DealerMediumSmallProductCategory mapping = new DealerMediumSmallProductCategory();
            mapping.setDealerProduct(product);
            mapping.setMedium(medium);
            mapping.setSmall(small);

            product.getCategoryMappings().add(mapping);
        }
    }

    private void syncRepresentativeImage(
            DealerProduct product,
            Long sellerMemberId,
            SellerProductUpdateRequest request,
            MultipartFile representativeImage
    ) {
        DealerProductImage currentMain = product.getImages().stream()
                .filter(image -> image.getType() == ProductImageType.MAIN)
                .findFirst()
                .orElse(null);

        if (representativeImage != null && !representativeImage.isEmpty()) {
            SellerProductFileService.StoredFile stored = sellerProductFileService.saveRepresentativeImage(sellerMemberId, representativeImage);

            if (currentMain == null) {
                currentMain = new DealerProductImage();
                currentMain.setDealerProduct(product);
                currentMain.setType(ProductImageType.MAIN);
                product.getImages().add(currentMain);
            } else {
                sellerProductFileService.deleteFileQuietly(currentMain.getPath());
            }

            currentMain.setUrl(stored.getUrl());
            currentMain.setPath(stored.getPath());
            currentMain.setFileName(stored.getFileName());
            currentMain.setSortOrder(0);
            return;
        }

        if (Boolean.TRUE.equals(request.getRemoveRepresentativeImage()) && currentMain != null) {
            sellerProductFileService.deleteFileQuietly(currentMain.getPath());
            product.getImages().remove(currentMain);
        }

        boolean hasFinalMain = product.getImages().stream().anyMatch(image -> image.getType() == ProductImageType.MAIN);
        if (!hasFinalMain) {
            throw new IllegalArgumentException("대표 이미지는 반드시 1장 유지되어야 합니다.");
        }
    }

    private void syncIconImage(
            DealerProduct product,
            Long sellerMemberId,
            SellerProductUpdateRequest request,
            MultipartFile iconImage
    ) {
        if (iconImage != null && !iconImage.isEmpty()) {
            SellerProductFileService.StoredFile stored = sellerProductFileService.saveIconImage(sellerMemberId, iconImage);

            sellerProductFileService.deleteFileQuietly(product.getIconPath());

            product.setIconUrl(stored.getUrl());
            product.setIconPath(stored.getPath());
            product.setIconFileName(stored.getFileName());
            return;
        }

        if (Boolean.TRUE.equals(request.getRemoveIconImage())) {
            sellerProductFileService.deleteFileQuietly(product.getIconPath());
            product.setIconUrl(null);
            product.setIconPath(null);
            product.setIconFileName(null);
        }
    }

    private void syncAdditionalImages(
            DealerProduct product,
            Long sellerMemberId,
            SellerProductUpdateRequest request,
            List<String> newAdditionalImageUids,
            List<MultipartFile> newAdditionalImages
    ) {
        Map<String, MultipartFile> newImageMap = pairNewFiles(newAdditionalImageUids, newAdditionalImages);

        if (request.getAdditionalImageOrders() == null) {
            if (!newImageMap.isEmpty()) {
                throw new IllegalArgumentException("추가 이미지 정렬 정보가 누락되었습니다.");
            }
            return;
        }

        List<SellerProductUpdateRequest.AdditionalImageOrderRequest> orders = request.getAdditionalImageOrders();

        LinkedHashSet<Long> keepExistingImageIds = orders.stream()
                .filter(order -> order.getType() == SellerProductUpdateRequest.AdditionalImageSourceType.EXISTING)
                .map(SellerProductUpdateRequest.AdditionalImageOrderRequest::getImageId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Iterator<DealerProductImage> iterator = product.getImages().iterator();
        while (iterator.hasNext()) {
            DealerProductImage image = iterator.next();

            if (image.getType() != ProductImageType.ADDITIONAL) {
                continue;
            }

            if (image.getId() == null || !keepExistingImageIds.contains(image.getId())) {
                sellerProductFileService.deleteFileQuietly(image.getPath());
                iterator.remove();
            }
        }

        Map<Long, DealerProductImage> existingAdditionalMap = product.getImages().stream()
                .filter(image -> image.getType() == ProductImageType.ADDITIONAL)
                .filter(image -> image.getId() != null)
                .collect(Collectors.toMap(DealerProductImage::getId, image -> image));

        for (int i = 0; i < orders.size(); i++) {
            SellerProductUpdateRequest.AdditionalImageOrderRequest order = orders.get(i);

            if (order.getType() == SellerProductUpdateRequest.AdditionalImageSourceType.EXISTING) {
                DealerProductImage existing = existingAdditionalMap.get(order.getImageId());
                if (existing == null) {
                    throw new IllegalArgumentException("기존 추가 이미지 정보를 찾을 수 없습니다.");
                }

                existing.setSortOrder(i);
                continue;
            }

            MultipartFile newFile = newImageMap.remove(order.getUploadUid());
            if (newFile == null || newFile.isEmpty()) {
                throw new IllegalArgumentException("신규 추가 이미지 정보가 올바르지 않습니다.");
            }

            SellerProductFileService.StoredFile stored =
                    sellerProductFileService.saveAdditionalImage(sellerMemberId, newFile);

            DealerProductImage created = new DealerProductImage();
            created.setDealerProduct(product);
            created.setType(ProductImageType.ADDITIONAL);
            created.setUrl(stored.getUrl());
            created.setPath(stored.getPath());
            created.setFileName(stored.getFileName());
            created.setSortOrder(i);

            product.getImages().add(created);
        }

        if (!newImageMap.isEmpty()) {
            throw new IllegalArgumentException("추가 이미지 정렬 정보와 업로드 파일 정보가 일치하지 않습니다.");
        }
    }

    private Map<String, MultipartFile> pairNewFiles(List<String> uids, List<MultipartFile> files) {
        Map<String, MultipartFile> result = new LinkedHashMap<>();

        if ((uids == null || uids.isEmpty()) && (files == null || files.isEmpty())) {
            return result;
        }

        if (uids == null || files == null || uids.size() != files.size()) {
            throw new IllegalArgumentException("신규 추가 이미지 데이터가 올바르지 않습니다.");
        }

        for (int i = 0; i < uids.size(); i++) {
            result.put(uids.get(i), files.get(i));
        }

        return result;
    }

    private void syncDetailHtmlAndDetailImages(
            DealerProduct product,
            Long sellerMemberId,
            String rawDetailHtml
    ) {
        try {
            SellerProductFileService.EditorSyncResult syncResult =
                    sellerProductFileService.moveTempEditorImagesToDetailDirectory(sellerMemberId, rawDetailHtml);

            product.setDetailHtml(syncResult.getHtml());

            LinkedHashMap<String, SellerProductFileService.StoredFile> finalImageMap = new LinkedHashMap<>();
            for (SellerProductFileService.StoredFile stored : syncResult.getImages()) {
                finalImageMap.putIfAbsent(stored.getUrl(), stored);
            }

            Iterator<DealerProductDetailImage> iterator = product.getDetailImages().iterator();
            while (iterator.hasNext()) {
                DealerProductDetailImage existing = iterator.next();

                if (!finalImageMap.containsKey(existing.getUrl())) {
                    sellerProductFileService.deleteFileQuietly(existing.getPath());
                    iterator.remove();
                }
            }

            Map<String, DealerProductDetailImage> existingMap = product.getDetailImages().stream()
                    .collect(Collectors.toMap(DealerProductDetailImage::getUrl, image -> image, (a, b) -> a));

            int sortOrder = 0;
            for (SellerProductFileService.StoredFile stored : finalImageMap.values()) {
                DealerProductDetailImage image = existingMap.get(stored.getUrl());

                if (image == null) {
                    image = new DealerProductDetailImage();
                    image.setDealerProduct(product);
                    image.setUploadedAt(LocalDateTime.now());
                    product.getDetailImages().add(image);
                }

                image.setUrl(stored.getUrl());
                image.setPath(stored.getPath());
                image.setFileName(stored.getFileName());
                image.setOriginalFilename(stored.getOriginalFilename());
                image.setSize(stored.getSize());
                image.setSortOrder(sortOrder++);
                image.setInUse(true);
            }

        } catch (IOException e) {
            throw new IllegalStateException("상세 설명 이미지 처리 중 오류가 발생했습니다.", e);
        }
    }

    private void syncExtraFields(DealerProduct product, List<SellerProductUpdateRequest.ExtraFieldRequest> extraFields) {
        product.getExtraFields().clear();

        if (extraFields == null) {
            return;
        }

        for (SellerProductUpdateRequest.ExtraFieldRequest item : extraFields) {
            if (!hasText(item.getLabel()) && !hasText(item.getValue())) {
                continue;
            }

            DealerProductExtraField field = new DealerProductExtraField();
            field.setDealerProduct(product);
            field.setLabel(trimToNull(item.getLabel()));
            field.setValue(trimToNull(item.getValue()));
            product.getExtraFields().add(field);
        }
    }

    private void syncKeywords(DealerProduct product, List<String> keywords) {
        LinkedHashSet<String> requestedWords = keywords == null
                ? new LinkedHashSet<>()
                : keywords.stream()
                        .map(this::trimToNull)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        if (requestedWords.isEmpty()) {
            throw new IllegalArgumentException("키워드는 최소 1개 이상 입력해야 합니다.");
        }

        Set<String> existingWords = new HashSet<>();

        Iterator<DealerProductKeyword> iterator = product.getKeywordMappings().iterator();
        while (iterator.hasNext()) {
            DealerProductKeyword mapping = iterator.next();

            String currentWord = null;
            if (mapping.getKeyword() != null) {
                currentWord = trimToNull(mapping.getKeyword().getWord());
            }

            if (currentWord == null || !requestedWords.contains(currentWord)) {
                iterator.remove();
                continue;
            }

            existingWords.add(currentWord);
        }

        Map<String, Keyword> existingKeywordMap = keywordRepository.findByWordIn(new ArrayList<>(requestedWords)).stream()
                .collect(Collectors.toMap(Keyword::getWord, keyword -> keyword));

        for (String word : requestedWords) {
            if (existingWords.contains(word)) {
                continue;
            }

            Keyword keyword = existingKeywordMap.get(word);

            if (keyword == null) {
                keyword = new Keyword();
                keyword.setWord(word);
                keyword = keywordRepository.save(keyword);
            }

            DealerProductKeyword mapping = new DealerProductKeyword();
            mapping.setDealerProduct(product);
            mapping.setKeyword(keyword);

            product.getKeywordMappings().add(mapping);
        }
    }

    private void syncOptionGroups(DealerProduct product, List<SellerProductUpdateRequest.OptionGroupRequest> optionGroups) {
        product.getOptionGroups().clear();

        if (optionGroups == null) {
            return;
        }

        List<SellerProductUpdateRequest.OptionGroupRequest> sortedGroups = optionGroups.stream()
                .sorted(Comparator.comparing(group -> group.getSortOrder() == null ? Integer.MAX_VALUE : group.getSortOrder()))
                .toList();

        for (int i = 0; i < sortedGroups.size(); i++) {
            SellerProductUpdateRequest.OptionGroupRequest groupRequest = sortedGroups.get(i);

            if (!hasText(groupRequest.getName()) && (groupRequest.getOptions() == null || groupRequest.getOptions().isEmpty())) {
                continue;
            }

            DealerProductOptionGroup group = new DealerProductOptionGroup();
            group.setDealerProduct(product);
            group.setName(trimToNull(groupRequest.getName()));
            group.setSortOrder(i);

            List<SellerProductUpdateRequest.OptionRequest> sortedOptions = groupRequest.getOptions() == null ? List.of() :
                    groupRequest.getOptions().stream()
                            .sorted(Comparator.comparing(option -> option.getSortOrder() == null ? Integer.MAX_VALUE : option.getSortOrder()))
                            .toList();

            for (int j = 0; j < sortedOptions.size(); j++) {
                SellerProductUpdateRequest.OptionRequest optionRequest = sortedOptions.get(j);

                if (!hasText(optionRequest.getName()) &&
                        !hasText(optionRequest.getValue()) &&
                        optionRequest.getExtraPrice() == null) {
                    continue;
                }

                DealerProductOption option = new DealerProductOption();
                option.setGroup(group);
                option.setName(trimToNull(optionRequest.getName()));
                option.setValue(trimToNull(optionRequest.getValue()));
                option.setExtraPrice(optionRequest.getExtraPrice() == null ? BigDecimal.ZERO : optionRequest.getExtraPrice());
                option.setSign(optionRequest.getSign());
                option.setSortOrder(j);
                group.getOptions().add(option);
            }

            product.getOptionGroups().add(group);
        }
    }

    private DealerProduct getOwnedProduct(Long sellerMemberId, Long dealerProductId) {
        DealerProduct product = dealerProductRepository.findOwnedByIdAndSellerMemberId(dealerProductId, sellerMemberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품을 찾을 수 없거나 수정 권한이 없습니다."));

        initializeOwnedProductAssociations(product);
        return product;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }
    
    private void initializeOwnedProductAssociations(DealerProduct product) {
        Hibernate.initialize(product.getImages());
        Hibernate.initialize(product.getDetailImages());
        Hibernate.initialize(product.getExtraFields());
        Hibernate.initialize(product.getKeywordMappings());
        Hibernate.initialize(product.getOptionGroups());
        Hibernate.initialize(product.getCategoryMappings());

        product.getKeywordMappings().forEach(mapping -> {
            if (mapping.getKeyword() != null) {
                Hibernate.initialize(mapping.getKeyword());
            }
        });

        product.getOptionGroups().forEach(group -> {
            Hibernate.initialize(group.getOptions());
        });

        product.getCategoryMappings().forEach(mapping -> {
            if (mapping.getMedium() != null) {
                Hibernate.initialize(mapping.getMedium());

                if (mapping.getMedium().getLarge() != null) {
                    Hibernate.initialize(mapping.getMedium().getLarge());
                }
            }

            if (mapping.getSmall() != null) {
                Hibernate.initialize(mapping.getSmall());
            }
        });
    }
}