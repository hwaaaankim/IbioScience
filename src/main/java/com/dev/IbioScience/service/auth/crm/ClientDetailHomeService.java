package com.dev.IbioScience.service.auth.crm;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.customer.auth.crm.ClientDetailHomeDto;
import com.dev.IbioScience.dto.customer.auth.crm.MemoPageResponseDto;
import com.dev.IbioScience.dto.customer.auth.crm.MemoPageResponseDto.Item;
import com.dev.IbioScience.dto.customer.auth.crm.SaveMemosRequest;
import com.dev.IbioScience.dto.customer.auth.crm.UpdateAddressRequest;
import com.dev.IbioScience.dto.customer.auth.crm.UpdateBuyerGradeRequest;
import com.dev.IbioScience.dto.customer.auth.crm.UpdateSellerProfileRequest;
import com.dev.IbioScience.enums.auth.DealerGrade;
import com.dev.IbioScience.enums.product.SettlementBasis;
import com.dev.IbioScience.enums.product.SettlementCycle;
import com.dev.IbioScience.enums.product.SupplyStructure;
import com.dev.IbioScience.enums.product.SupplyType;
import com.dev.IbioScience.enums.product.TradingStatus;
import com.dev.IbioScience.model.auth.BuyerDealerProfile;
import com.dev.IbioScience.model.auth.CompanyProfile;
import com.dev.IbioScience.model.auth.DealerCategoryPermission;
import com.dev.IbioScience.model.auth.DealerSettlementPolicy;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.MemberMemo;
import com.dev.IbioScience.model.auth.SellerContact;
import com.dev.IbioScience.model.auth.SellerDealerProfile;
import com.dev.IbioScience.model.auth.embedded.Address;
import com.dev.IbioScience.repository.auth.MemberMemoRepository;
import com.dev.IbioScience.repository.auth.MemberRepository;
import com.dev.IbioScience.repository.auth.crm.CrmBuyerDealerProfileRepository;
import com.dev.IbioScience.repository.auth.crm.CrmDealerCategoryPermissionRepository;
import com.dev.IbioScience.repository.auth.crm.CrmDealerSettlementPolicyRepository;
import com.dev.IbioScience.repository.auth.crm.CrmSellerContactRepository;
import com.dev.IbioScience.repository.auth.crm.CrmSellerDealerProfileRepository;
import com.dev.IbioScience.service.util.SMSService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClientDetailHomeService {

    private final MemberRepository memberRepository;

    private final CrmBuyerDealerProfileRepository crmBuyerDealerProfileRepository;
    private final CrmSellerDealerProfileRepository crmSellerDealerProfileRepository;
    private final CrmSellerContactRepository crmSellerContactRepository;
    private final CrmDealerSettlementPolicyRepository crmDealerSettlementPolicyRepository;
    private final CrmDealerCategoryPermissionRepository crmDealerCategoryPermissionRepository;

    private final MemberMemoRepository memberMemoRepository;

    private final PasswordEncoder passwordEncoder;
    private final SMSService smsService;

    // =========================
    // 1) HOME VIEW DATA
    // =========================
    @Transactional(readOnly = true)
    public ClientDetailHomeDto getHomeDto(Long memberId) {

        Member m = memberRepository.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다. id=" + memberId));

        CompanyProfile cp = m.getCompanyProfile(); // nullable
        BuyerDealerProfile buyer = crmBuyerDealerProfileRepository.findByMember_Id(memberId).orElse(null);
        SellerDealerProfile seller = crmSellerDealerProfileRepository.findByMember_Id(memberId).orElse(null);

        List<MemberMemo> latest = memberMemoRepository.findTop5ByTargetMember_IdOrderByCreatedAtDesc(memberId);

        ClientDetailHomeDto.MemberSection memberSection = ClientDetailHomeDto.MemberSection.builder()
            .id(m.getId())
            .username(m.getUsername())
            .name(m.getName())
            .tel(m.getTel())
            .mobile(m.getMobile())
            .email(m.getEmail())
            .point(m.getPoint())
            .domain(m.getDomain())
            .customerType(m.getCustomerType())
            .dealerType(m.getDealerType())
            .role(m.getRole())
            .status(m.getStatus())
            .organizationName(m.getOrganizationName())
            .joinedAt(m.getJoinedAt())
            .withdrewAt(m.getWithdrewAt())
            .mustChangePassword(m.isMustChangePassword())
            .lastPasswordChangedAt(m.getLastPasswordChangedAt())
            .position(m.getPosition())
            .useYn(m.isUseYn())
            .isPrimary(m.isPrimary())
            .companyProfileId(cp != null ? cp.getId() : null)
            .postcode(m.getAddress() != null ? nv(m.getAddress().getPostcode()) : "")
            .roadAddress(m.getAddress() != null ? nv(m.getAddress().getRoadAddress()) : "")
            .jibunAddress(m.getAddress() != null ? nv(m.getAddress().getJibunAddress()) : "")
            .detailAddress(m.getAddress() != null ? nv(m.getAddress().getDetailAddress()) : "")
            .build();

        ClientDetailHomeDto.CompanySection companySection = null;
        if (cp != null) {
            Address ca = cp.getCompanyAddress();
            companySection = ClientDetailHomeDto.CompanySection.builder()
                .id(cp.getId())
                .companyName(cp.getCompanyName())
                .department(cp.getDepartment())
                .ceoName(cp.getCeoName())
                .businessType(cp.getBusinessType())
                .businessItem(cp.getBusinessItem())
                .representativeTel(cp.getRepresentativeTel())
                .fax(cp.getFax())
                .invoiceEmail(cp.getInvoiceEmail())
                .businessRegistrationNumber(cp.getBusinessRegistrationNumber())
                .businessRegImageRoad(cp.getBusinessRegImageRoad())
                .homepageUrl(cp.getHomepageUrl())
                .organizationCategory(cp.getOrganizationCategory())
                .postcode(ca != null ? nv(ca.getPostcode()) : "")
                .roadAddress(ca != null ? nv(ca.getRoadAddress()) : "")
                .jibunAddress(ca != null ? nv(ca.getJibunAddress()) : "")
                .detailAddress(ca != null ? nv(ca.getDetailAddress()) : "")
                .build();
        }

        ClientDetailHomeDto.BuyerSection buyerSection = null;
        if (buyer != null) {
            buyerSection = ClientDetailHomeDto.BuyerSection.builder()
                .id(buyer.getId())
                .grade(buyer.getGrade())
                .customDiscountRate(buyer.getCustomDiscountRate())
                .effectiveFrom(buyer.getEffectiveFrom())
                .build();
        }

        ClientDetailHomeDto.SellerSection sellerSection = null;
        if (seller != null) {
            List<SellerContact> contacts = crmSellerContactRepository
                .findBySellerDealerProfile_IdOrderByIdAsc(seller.getId());

            DealerSettlementPolicy sp = crmDealerSettlementPolicyRepository
                .findBySellerDealerProfile_Id(seller.getId()).orElse(null);

            List<DealerCategoryPermission> perms =
                crmDealerCategoryPermissionRepository.findBySellerDealerProfile_IdOrderByIdAsc(seller.getId());

            sellerSection = ClientDetailHomeDto.SellerSection.builder()
                .id(seller.getId())
                .companyProfileId(seller.getCompanyProfile() != null ? seller.getCompanyProfile().getId() : null)
                .shopName(seller.getShopName())
                .logoImageRoad(seller.getLogoImageRoad())
                .supplierCode(seller.getSupplierCode())
                .tradingStatus(seller.getTradingStatus())
                .supplyType(seller.getSupplyType())
                .supplyStructure(seller.getSupplyStructure())
                .productTypeText(seller.getProductTypeText())
                .tel(seller.getTel())
                .fax(seller.getFax())
                .homepageUrl(seller.getHomepageUrl())
                .dealStartDate(seller.getDealStartDate())
                .dealStopDate(seller.getDealStopDate())
                .bizPostcode(seller.getBusinessAddress() != null ? nv(seller.getBusinessAddress().getPostcode()) : "")
                .bizRoadAddress(seller.getBusinessAddress() != null ? nv(seller.getBusinessAddress().getRoadAddress()) : "")
                .bizJibunAddress(seller.getBusinessAddress() != null ? nv(seller.getBusinessAddress().getJibunAddress()) : "")
                .bizDetailAddress(seller.getBusinessAddress() != null ? nv(seller.getBusinessAddress().getDetailAddress()) : "")
                .returnPostcode(seller.getReturnAddress() != null ? nv(seller.getReturnAddress().getPostcode()) : "")
                .returnRoadAddress(seller.getReturnAddress() != null ? nv(seller.getReturnAddress().getRoadAddress()) : "")
                .returnJibunAddress(seller.getReturnAddress() != null ? nv(seller.getReturnAddress().getJibunAddress()) : "")
                .returnDetailAddress(seller.getReturnAddress() != null ? nv(seller.getReturnAddress().getDetailAddress()) : "")
                .contacts(contacts.stream().map(c -> ClientDetailHomeDto.SellerContactItem.builder()
                    .id(c.getId())
                    .name(c.getName())
                    .phone(c.getPhone())
                    .email(c.getEmail())
                    .build()
                ).collect(Collectors.toList()))
                .settlementPolicy(sp != null ? ClientDetailHomeDto.SettlementPolicy.builder()
                    .id(sp.getId())
                    .commissionRate(sp.getCommissionRate())
                    .cycle(sp.getCycle())
                    .basis(sp.getBasis())
                    .nextSettlementDate(sp.getNextSettlementDate())
                    .build() : null)
                .categoryPermissions(perms.stream().map(p -> ClientDetailHomeDto.CategoryPermissionItem.builder()
                    .id(p.getId())
                    .largeId(p.getLarge() != null ? p.getLarge().getId() : null)
                    .mediumId(p.getMedium() != null ? p.getMedium().getId() : null)
                    .smallId(p.getSmall() != null ? p.getSmall().getId() : null)
                    .build()
                ).collect(Collectors.toList()))
                .build();
        }

        return ClientDetailHomeDto.builder()
            .memberId(memberId)
            .activeTab("home")
            .member(memberSection)
            .company(companySection)
            .buyer(buyerSection)
            .seller(sellerSection)
            .latestMemos(latest.stream().map(mm -> ClientDetailHomeDto.MemoItem.builder()
                .id(mm.getId())
                .content(mm.getContent())
                .writerMemberId(mm.getWriterMember().getId())
                .writerName(mm.getWriterMember().getName())
                .createdAt(mm.getCreatedAt())
                .build()
            ).collect(Collectors.toList()))
            .dealerGrades(Arrays.stream(DealerGrade.values()).map(Enum::name).toList())
            .tradingStatuses(Arrays.stream(TradingStatus.values()).map(Enum::name).toList())
            .supplyTypes(Arrays.stream(SupplyType.values()).map(Enum::name).toList())
            .supplyStructures(Arrays.stream(SupplyStructure.values()).map(Enum::name).toList())
            .settlementCycles(Arrays.stream(SettlementCycle.values()).map(Enum::name).toList())
            .settlementBases(Arrays.stream(SettlementBasis.values()).map(Enum::name).toList())
            .build();
    }

    // =========================
    // 2) 비밀번호 초기화 + SMS 발송
    // =========================
    @Transactional
    public void resetPasswordAndSendSms(Long memberId) {
        Member m = memberRepository.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다. id=" + memberId));

        String mobile = m.getMobile();
        if (mobile == null || mobile.trim().isEmpty()) {
            throw new IllegalStateException("휴대폰 번호가 없어 SMS 발송이 불가능합니다.");
        }

        String newRaw = randomPassword(16);
        m.setPassword(passwordEncoder.encode(newRaw));
        m.setMustChangePassword(true);
        m.setLastPasswordChangedAt(LocalDateTime.now());

        memberRepository.save(m);

        String msg = "[아이바이오] 비밀번호 초기화 안내\n"
            + "ID: " + m.getUsername() + "\n"
            + "PW: " + newRaw;

        smsService.sendMessage(mobile, msg);
    }

    // =========================
    // 3) 메모 저장(추가/삭제) - 한번에 반영
    // =========================
    @Transactional
    public void saveMemos(Long targetMemberId, Long writerMemberId, SaveMemosRequest req) {

        Member target = memberRepository.findById(targetMemberId)
            .orElseThrow(() -> new IllegalArgumentException("대상 회원이 존재하지 않습니다. id=" + targetMemberId));

        Member writer = memberRepository.findById(writerMemberId)
            .orElseThrow(() -> new IllegalArgumentException("작성자 회원이 존재하지 않습니다. id=" + writerMemberId));

        // delete
        if (req.getDeleteIds() != null) {
            for (Long id : req.getDeleteIds()) {
                if (id == null) continue;
                memberMemoRepository.deleteByIdAndTargetMember_Id(id, targetMemberId);
            }
        }

        // add
        if (req.getAddContents() != null) {
            for (String c : req.getAddContents()) {
                if (c == null) continue;
                String content = c.trim();
                if (content.isEmpty()) continue;

                MemberMemo memo = MemberMemo.builder()
                    .targetMember(target)
                    .writerMember(writer)
                    .content(content)
                    .build();
                memberMemoRepository.save(memo);
            }
        }
    }

    // =========================
    // 4) 메모 전체보기(필터+페이지네이션)
    // =========================
    @Transactional(readOnly = true)
    public MemoPageResponseDto getMemoPage(Long memberId, LocalDate from, LocalDate to, int page, int size) {

        LocalDateTime fromDt = (from != null) ? from.atStartOfDay() : null;
        LocalDateTime toExclusive = (to != null) ? to.plusDays(1).atStartOfDay() : null;

        Pageable pageable = PageRequest.of(
            Math.max(page, 0),
            size,
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<MemberMemo> result = memberMemoRepository.searchMemos(memberId, fromDt, toExclusive, pageable);

        return MemoPageResponseDto.builder()
            .page(result.getNumber())
            .size(result.getSize())
            .totalElements(result.getTotalElements())
            .totalPages(result.getTotalPages())
            .content(result.getContent().stream().map(m -> Item.builder()
                .id(m.getId())
                .content(m.getContent())
                .writerName(m.getWriterMember().getName())
                .createdAt(m.getCreatedAt())
                .build()
            ).toList())
            .build();
    }

    // =========================
    // 5) 바이어 딜러그레이드 변경
    // =========================
    @Transactional
    public void updateBuyerGrade(Long memberId, UpdateBuyerGradeRequest req) {
        BuyerDealerProfile buyer = crmBuyerDealerProfileRepository.findByMember_Id(memberId)
            .orElseThrow(() -> new IllegalStateException("바이어 프로필이 없습니다."));

        if (req.getGrade() == null || req.getGrade().trim().isEmpty()) {
            throw new IllegalArgumentException("grade가 비어있습니다.");
        }

        DealerGrade grade = DealerGrade.valueOf(req.getGrade().trim());
        buyer.setGrade(grade);
        crmBuyerDealerProfileRepository.save(buyer);
    }

    // =========================
    // 6) 주소 변경
    // =========================
    @Transactional
    public void updateMemberAddress(Long memberId, UpdateAddressRequest req) {
        Member m = memberRepository.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다. id=" + memberId));

        m.setAddress(Address.builder()
            .postcode(nv(req.getPostcode()))
            .roadAddress(nv(req.getRoadAddress()))
            .jibunAddress(nv(req.getJibunAddress()))
            .detailAddress(nv(req.getDetailAddress()))
            .build());

        memberRepository.save(m);
    }

    @Transactional
    public void updateCompanyAddress(Long memberId, UpdateAddressRequest req) {
        Member m = memberRepository.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다. id=" + memberId));

        CompanyProfile cp = m.getCompanyProfile();
        if (cp == null) {
            throw new IllegalStateException("회사 프로필이 없습니다.");
        }

        cp.setCompanyAddress(Address.builder()
            .postcode(nv(req.getPostcode()))
            .roadAddress(nv(req.getRoadAddress()))
            .jibunAddress(nv(req.getJibunAddress()))
            .detailAddress(nv(req.getDetailAddress()))
            .build());

        memberRepository.save(m); // companyProfile은 연관관계에 따라 cascade 아니면 별도 repo 저장 필요하지만, 현재 구조상 member에 ManyToOne이라 이미 영속 상태이면 flush 됨
    }

 // =========================
 // 7) 셀러 프로필 + 정산 + 카테고리권한 + 주소 저장(한번에)
 // =========================
 @Transactional
 public void updateSellerAll(Long memberId, UpdateSellerProfileRequest req) {

     SellerDealerProfile seller = crmSellerDealerProfileRepository.findByMember_Id(memberId)
         .orElseThrow(() -> new IllegalStateException("셀러 프로필이 없습니다."));

     // 기본
     seller.setShopName(nv(req.getShopName()));
     seller.setTel(nv(req.getTel()));
     seller.setFax(nv(req.getFax()));
     seller.setHomepageUrl(nv(req.getHomepageUrl()));
     seller.setProductTypeText(nv(req.getProductTypeText()));

     if (req.getTradingStatus() != null && !req.getTradingStatus().trim().isEmpty()) {
         seller.setTradingStatus(TradingStatus.valueOf(req.getTradingStatus().trim()));
     }
     if (req.getSupplyType() != null && !req.getSupplyType().trim().isEmpty()) {
         seller.setSupplyType(SupplyType.valueOf(req.getSupplyType().trim()));
     }
     if (req.getSupplyStructure() != null && !req.getSupplyStructure().trim().isEmpty()) {
         seller.setSupplyStructure(SupplyStructure.valueOf(req.getSupplyStructure().trim()));
     }

     seller.setDealStartDate(req.getDealStartDate());
     seller.setDealStopDate(req.getDealStopDate());

     // 주소(사업장)
     if (req.getBusinessAddress() != null) {
         seller.setBusinessAddress(Address.builder()
             .postcode(nv(req.getBusinessAddress().getPostcode()))
             .roadAddress(nv(req.getBusinessAddress().getRoadAddress()))
             .jibunAddress(nv(req.getBusinessAddress().getJibunAddress()))
             .detailAddress(nv(req.getBusinessAddress().getDetailAddress()))
             .build());
     }

     // 주소(반품)
     if (req.getReturnAddress() != null) {
         seller.setReturnAddress(Address.builder()
             .postcode(nv(req.getReturnAddress().getPostcode()))
             .roadAddress(nv(req.getReturnAddress().getRoadAddress()))
             .jibunAddress(nv(req.getReturnAddress().getJibunAddress()))
             .detailAddress(nv(req.getReturnAddress().getDetailAddress()))
             .build());
     }

     crmSellerDealerProfileRepository.save(seller);

     // 정산
     if (req.getSettlement() != null) {
         DealerSettlementPolicy policy = crmDealerSettlementPolicyRepository
             .findBySellerDealerProfile_Id(seller.getId())
             .orElseGet(() -> DealerSettlementPolicy.builder()
                 .sellerDealerProfile(seller)
                 .commissionRate(req.getSettlement().getCommissionRate() != null ? req.getSettlement().getCommissionRate() : BigDecimal.ZERO)
                 .cycle(req.getSettlement().getCycle() != null ? SettlementCycle.valueOf(req.getSettlement().getCycle()) : SettlementCycle.MONTH_END)
                 .basis(req.getSettlement().getBasis() != null ? SettlementBasis.valueOf(req.getSettlement().getBasis()) : SettlementBasis.PAYMENT_COMPLETED)
                 .nextSettlementDate(req.getSettlement().getNextSettlementDate())
                 .build()
             );

         if (req.getSettlement().getCommissionRate() != null) policy.setCommissionRate(req.getSettlement().getCommissionRate());
         if (req.getSettlement().getCycle() != null && !req.getSettlement().getCycle().trim().isEmpty())
             policy.setCycle(SettlementCycle.valueOf(req.getSettlement().getCycle().trim()));
         if (req.getSettlement().getBasis() != null && !req.getSettlement().getBasis().trim().isEmpty())
             policy.setBasis(SettlementBasis.valueOf(req.getSettlement().getBasis().trim()));

         policy.setNextSettlementDate(req.getSettlement().getNextSettlementDate());

         crmDealerSettlementPolicyRepository.save(policy);
     }

     // 카테고리 권한 삭제
     if (req.getDeletePermissionIds() != null) {
         for (Long id : req.getDeletePermissionIds()) {
             if (id == null) continue;
             crmDealerCategoryPermissionRepository.deleteByIdAndSellerDealerProfile_Id(id, seller.getId());
         }
     }

     // 카테고리 권한 추가(large 필수 / medium 선택 / small 선택)
     if (req.getAddPermissions() != null) {

         // ✅ 서버 정규화(와일드카드 우선 + 중복 제거)
         // - [large]가 있으면 동일 large의 [large, medium], [large, medium, small]은 저장하지 않음
         // - [large, medium]이 있으면 동일 large+medium의 [large, medium, small]은 저장하지 않음
         // - 동일 조합 중복은 1건만 저장
         final java.util.Set<Long> largeIds = new java.util.HashSet<>();
         final java.util.Set<Long> largeAllSet = new java.util.HashSet<>(); // largeId
         final java.util.Map<Long, java.util.Set<Long>> mediumAllByLarge = new java.util.HashMap<>(); // largeId -> mediumId set
         final java.util.Map<Long, java.util.Map<Long, java.util.Set<Long>>> smallByLargeMedium = new java.util.HashMap<>(); // largeId -> (mediumId -> smallId set)

         for (UpdateSellerProfileRequest.AddPermissionItem add : req.getAddPermissions()) {
             if (add == null) continue;
             if (add.getLargeId() == null) continue;

             final Long largeId = add.getLargeId();
             final Long mediumId = add.getMediumId();
             final Long smallId = add.getSmallId();

             // small이 있으면 medium 필수
             if (smallId != null && mediumId == null) {
                 throw new IllegalArgumentException("소분류가 선택된 경우 중분류는 필수입니다.");
             }

             largeIds.add(largeId);

             // 1) [large]
             if (mediumId == null && smallId == null) {
                 largeAllSet.add(largeId);
                 continue;
             }

             // 2) [large, medium]
             if (mediumId != null && smallId == null) {
                 mediumAllByLarge.computeIfAbsent(largeId, k -> new java.util.HashSet<>()).add(mediumId);
                 continue;
             }

             // 3) [large, medium, small]
             if (mediumId != null && smallId != null) {
                 smallByLargeMedium
                     .computeIfAbsent(largeId, k -> new java.util.HashMap<>())
                     .computeIfAbsent(mediumId, k -> new java.util.HashSet<>())
                     .add(smallId);
             }
         }

         // 정규화 결과로 저장
         final java.util.List<Long> sortedLargeIds = new java.util.ArrayList<>(largeIds);
         java.util.Collections.sort(sortedLargeIds);

         for (Long largeId : sortedLargeIds) {

             // [large]가 있으면 해당 large는 large만 저장
             if (largeAllSet.contains(largeId)) {

                 DealerCategoryPermission perm = DealerCategoryPermission.builder()
                     .sellerDealerProfile(seller)
                     .large(refCategoryLarge(largeId))
                     .medium(null)
                     .small(null)
                     .build();

                 crmDealerCategoryPermissionRepository.save(perm);
                 continue;
             }

             // [large, medium] 저장
             final java.util.Set<Long> mediumAllSet = mediumAllByLarge.getOrDefault(largeId, java.util.Collections.emptySet());
             final java.util.List<Long> sortedMediumIds = new java.util.ArrayList<>(mediumAllSet);
             java.util.Collections.sort(sortedMediumIds);

             for (Long mediumId : sortedMediumIds) {

                 DealerCategoryPermission perm = DealerCategoryPermission.builder()
                     .sellerDealerProfile(seller)
                     .large(refCategoryLarge(largeId))
                     .medium(refCategoryMedium(mediumId))
                     .small(null)
                     .build();

                 crmDealerCategoryPermissionRepository.save(perm);
             }

             // [large, medium, small] 저장(단, 같은 medium에 [large, medium]이 있으면 small들은 저장하지 않음)
             final java.util.Map<Long, java.util.Set<Long>> smallMap = smallByLargeMedium.getOrDefault(largeId, java.util.Collections.emptyMap());
             final java.util.List<Long> smallMediumIds = new java.util.ArrayList<>(smallMap.keySet());
             java.util.Collections.sort(smallMediumIds);

             for (Long mediumId : smallMediumIds) {

                 // medium 전체가 있으면 해당 medium의 small은 불필요
                 if (mediumAllSet.contains(mediumId)) continue;

                 final java.util.List<Long> sortedSmallIds = new java.util.ArrayList<>(smallMap.getOrDefault(mediumId, java.util.Collections.emptySet()));
                 java.util.Collections.sort(sortedSmallIds);

                 for (Long smallId : sortedSmallIds) {

                     DealerCategoryPermission perm = DealerCategoryPermission.builder()
                         .sellerDealerProfile(seller)
                         .large(refCategoryLarge(largeId))
                         .medium(refCategoryMedium(mediumId))
                         .small(refCategorySmall(smallId))
                         .build();

                     crmDealerCategoryPermissionRepository.save(perm);
                 }
             }
         }
     }
 }
    // =========================
    // 내부 유틸
    // =========================
    private static String nv(String s) {
        return s == null ? "" : s;
    }

    private static String randomPassword(int len) {
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * ⚠️ 카테고리 엔티티 패키지/구조를 여기서 “추측”하지 않기 위해,
     * DealerCategoryPermission에 이미 사용 중인 CategoryLarge/Medium/Small 타입을 그대로 참조합니다.
     * 아래 import 경로는 “프로젝트에서 실제 사용 중인 패키지”로 맞춰주셔야 합니다.
     */
    private com.dev.IbioScience.model.product.category.CategoryLarge refCategoryLarge(Long id) {
        return jakarta.persistence.Persistence.getPersistenceUtil() != null
            ? getReference(com.dev.IbioScience.model.product.category.CategoryLarge.class, id)
            : getReference(com.dev.IbioScience.model.product.category.CategoryLarge.class, id);
    }

    private com.dev.IbioScience.model.product.category.CategoryMedium refCategoryMedium(Long id) {
        return getReference(com.dev.IbioScience.model.product.category.CategoryMedium.class, id);
    }

    private com.dev.IbioScience.model.product.category.CategorySmall refCategorySmall(Long id) {
        return getReference(com.dev.IbioScience.model.product.category.CategorySmall.class, id);
    }

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    private <T> T getReference(Class<T> clazz, Long id) {
        return em.getReference(clazz, id);
    }
}