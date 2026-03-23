package com.dev.IbioScience.service.seller.product;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.seller.product.SellerProductFormMetaResponse;
import com.dev.IbioScience.enums.product.DisplayStatus;
import com.dev.IbioScience.enums.product.PriceExposeTarget;
import com.dev.IbioScience.enums.product.PriceSign;
import com.dev.IbioScience.enums.product.ProductNewState;
import com.dev.IbioScience.enums.product.ProductState;
import com.dev.IbioScience.enums.product.SaleStatus;
import com.dev.IbioScience.model.auth.DealerCategoryPermission;
import com.dev.IbioScience.model.auth.SellerDealerProfile;
import com.dev.IbioScience.model.product.category.CategoryLarge;
import com.dev.IbioScience.model.product.category.CategoryMedium;
import com.dev.IbioScience.model.product.category.CategorySmall;
import com.dev.IbioScience.model.product.relation.MediumSmallCategory;
import com.dev.IbioScience.repository.auth.DealerCategoryPermissionRepository;
import com.dev.IbioScience.repository.category.CategoryMediumRepository;
import com.dev.IbioScience.repository.category.MediumSmallCategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerProductFormQueryService {

    private final SellerProductAccessService sellerProductAccessService;
    private final DealerCategoryPermissionRepository dealerCategoryPermissionRepository;
    private final CategoryMediumRepository categoryMediumRepository;
    private final MediumSmallCategoryRepository mediumSmallCategoryRepository;

    public SellerProductFormMetaResponse getFormMeta(Long loginMemberId) {
        SellerDealerProfile sellerProfile = sellerProductAccessService.getSellerProfileOrThrow(loginMemberId);

        List<DealerCategoryPermission> permissions =
                dealerCategoryPermissionRepository.findAllWithCategoryBySellerProfileId(sellerProfile.getId());

        if (permissions.isEmpty()) {
            throw new IllegalArgumentException("등록 가능한 카테고리 권한이 없습니다.");
        }

        List<SellerProductFormMetaResponse.LargeNode> allowedCategories = buildAllowedCategories(permissions);

        return SellerProductFormMetaResponse.builder()
                .shopName(sellerProfile.getShopName())
                .displayStatuses(toDisplayStatusOptions())
                .saleStatuses(toSaleStatusOptions())
                .productStates(toProductStateOptions())
                .newStates(toNewStateOptions())
                .priceExposeTargets(toPriceExposeTargetOptions())
                .priceSigns(toPriceSignOptions())
                .allowedCategories(allowedCategories)
                .build();
    }

    private List<SellerProductFormMetaResponse.LargeNode> buildAllowedCategories(List<DealerCategoryPermission> permissions) {
        Set<Long> largeIds = permissions.stream()
                .map(p -> p.getLarge().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<CategoryMedium> allMediums = categoryMediumRepository.findByLargeIdInOrderByNameAsc(largeIds);

        Map<Long, List<CategoryMedium>> mediumsByLargeId = allMediums.stream()
                .collect(Collectors.groupingBy(
                        medium -> medium.getLarge().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Set<Long> mediumIds = allMediums.stream()
                .map(CategoryMedium::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, List<CategorySmall>> smallsByMediumId = new LinkedHashMap<>();
        if (!mediumIds.isEmpty()) {
            List<MediumSmallCategory> mediumSmallCategories = mediumSmallCategoryRepository.findAllByMediumIds(mediumIds);
            for (MediumSmallCategory relation : mediumSmallCategories) {
                Long mediumId = relation.getMedium().getId();
                smallsByMediumId.computeIfAbsent(mediumId, k -> new ArrayList<>());
                List<CategorySmall> smalls = smallsByMediumId.get(mediumId);

                boolean exists = smalls.stream().anyMatch(s -> s.getId().equals(relation.getSmall().getId()));
                if (!exists) {
                    smalls.add(relation.getSmall());
                }
            }
        }

        Map<Long, SellerProductFormMetaResponse.LargeNode> largeMap = new LinkedHashMap<>();

        for (DealerCategoryPermission permission : permissions) {
            CategoryLarge large = permission.getLarge();
            CategoryMedium medium = permission.getMedium();
            CategorySmall small = permission.getSmall();

            SellerProductFormMetaResponse.LargeNode largeNode = largeMap.computeIfAbsent(
                    large.getId(),
                    k -> SellerProductFormMetaResponse.LargeNode.builder()
                            .id(large.getId())
                            .name(large.getName())
                            .mediums(new ArrayList<>())
                            .build()
            );

            if (medium == null) {
                List<CategoryMedium> mediums = mediumsByLargeId.getOrDefault(large.getId(), List.of());
                for (CategoryMedium item : mediums) {
                    SellerProductFormMetaResponse.MediumNode mediumNode = getOrCreateMediumNode(largeNode, item);
                    addAllSmalls(mediumNode, smallsByMediumId.getOrDefault(item.getId(), List.of()));
                }
                continue;
            }

            SellerProductFormMetaResponse.MediumNode mediumNode = getOrCreateMediumNode(largeNode, medium);

            if (small == null) {
                addAllSmalls(mediumNode, smallsByMediumId.getOrDefault(medium.getId(), List.of()));
            } else {
                addSmall(mediumNode, small);
            }
        }

        return new ArrayList<>(largeMap.values());
    }

    private SellerProductFormMetaResponse.MediumNode getOrCreateMediumNode(
            SellerProductFormMetaResponse.LargeNode largeNode,
            CategoryMedium medium
    ) {
        for (SellerProductFormMetaResponse.MediumNode existing : largeNode.getMediums()) {
            if (existing.getId().equals(medium.getId())) {
                return existing;
            }
        }

        SellerProductFormMetaResponse.MediumNode created =
                SellerProductFormMetaResponse.MediumNode.builder()
                        .id(medium.getId())
                        .name(medium.getName())
                        .smalls(new ArrayList<>())
                        .build();

        largeNode.getMediums().add(created);
        return created;
    }

    private void addAllSmalls(SellerProductFormMetaResponse.MediumNode mediumNode, Collection<CategorySmall> smalls) {
        for (CategorySmall small : smalls) {
            addSmall(mediumNode, small);
        }
    }

    private void addSmall(SellerProductFormMetaResponse.MediumNode mediumNode, CategorySmall small) {
        boolean exists = mediumNode.getSmalls().stream().anyMatch(s -> s.getId().equals(small.getId()));
        if (!exists) {
            mediumNode.getSmalls().add(
                    SellerProductFormMetaResponse.SmallNode.builder()
                            .id(small.getId())
                            .name(small.getName())
                            .build()
            );
        }
    }

    private List<SellerProductFormMetaResponse.EnumOption> toDisplayStatusOptions() {
        List<SellerProductFormMetaResponse.EnumOption> list = new ArrayList<>();
        for (DisplayStatus e : DisplayStatus.values()) {
            list.add(SellerProductFormMetaResponse.EnumOption.builder()
                    .value(e.name())
                    .label(e.getLabel())
                    .build());
        }
        return list;
    }

    private List<SellerProductFormMetaResponse.EnumOption> toSaleStatusOptions() {
        List<SellerProductFormMetaResponse.EnumOption> list = new ArrayList<>();
        for (SaleStatus e : SaleStatus.values()) {
            list.add(SellerProductFormMetaResponse.EnumOption.builder()
                    .value(e.name())
                    .label(e.getLabel())
                    .build());
        }
        return list;
    }

    private List<SellerProductFormMetaResponse.EnumOption> toProductStateOptions() {
        List<SellerProductFormMetaResponse.EnumOption> list = new ArrayList<>();
        for (ProductState e : ProductState.values()) {
            list.add(SellerProductFormMetaResponse.EnumOption.builder()
                    .value(e.name())
                    .label(e.getLabel())
                    .build());
        }
        return list;
    }

    private List<SellerProductFormMetaResponse.EnumOption> toNewStateOptions() {
        List<SellerProductFormMetaResponse.EnumOption> list = new ArrayList<>();
        for (ProductNewState e : ProductNewState.values()) {
            list.add(SellerProductFormMetaResponse.EnumOption.builder()
                    .value(e.name())
                    .label(e.getLabel())
                    .build());
        }
        return list;
    }

    private List<SellerProductFormMetaResponse.EnumOption> toPriceExposeTargetOptions() {
        List<SellerProductFormMetaResponse.EnumOption> list = new ArrayList<>();
        for (PriceExposeTarget e : PriceExposeTarget.values()) {
            list.add(SellerProductFormMetaResponse.EnumOption.builder()
                    .value(e.name())
                    .label(e.name())
                    .build());
        }
        return list;
    }

    private List<SellerProductFormMetaResponse.EnumOption> toPriceSignOptions() {
        List<SellerProductFormMetaResponse.EnumOption> list = new ArrayList<>();
        for (PriceSign e : PriceSign.values()) {
            list.add(SellerProductFormMetaResponse.EnumOption.builder()
                    .value(e.name())
                    .label(e.getLabel())
                    .build());
        }
        return list;
    }
}