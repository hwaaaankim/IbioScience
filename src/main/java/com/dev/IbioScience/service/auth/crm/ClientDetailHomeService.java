package com.dev.IbioScience.service.auth.crm;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.customer.auth.crm.ClientDetailHomeDto;
import com.dev.IbioScience.dto.customer.auth.crm.MemoPageResponseDto;
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
import com.dev.IbioScience.model.product.category.CategoryLarge;
import com.dev.IbioScience.model.product.category.CategoryMedium;
import com.dev.IbioScience.model.product.category.CategorySmall;
import com.dev.IbioScience.repository.auth.MemberMemoRepository;
import com.dev.IbioScience.repository.auth.MemberRepository;
import com.dev.IbioScience.repository.auth.crm.CrmBuyerDealerProfileRepository;
import com.dev.IbioScience.repository.auth.crm.CrmDealerCategoryPermissionRepository;
import com.dev.IbioScience.repository.auth.crm.CrmDealerSettlementPolicyRepository;
import com.dev.IbioScience.repository.auth.crm.CrmSellerContactRepository;
import com.dev.IbioScience.repository.auth.crm.CrmSellerDealerProfileRepository;
import com.dev.IbioScience.service.settlement.DealerSettlementPolicyHistoryService;
import com.dev.IbioScience.service.util.SMSService;
import com.dev.IbioScience.utils.UploadPathHelper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    private final DealerSettlementPolicyHistoryService dealerSettlementPolicyHistoryService;
    private final MemberMemoRepository memberMemoRepository;

    private final PasswordEncoder passwordEncoder;
    private final SMSService smsService;
    private final UploadPathHelper uploadPathHelper;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public ClientDetailHomeDto getHomeDto(Long memberId) {

        Member m = memberRepository.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다. id=" + memberId));

        CompanyProfile cp = m.getCompanyProfile();
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

    @Transactional
    public void saveMemos(Long targetMemberId, Long writerMemberId, SaveMemosRequest req) {

        Member target = memberRepository.findById(targetMemberId)
            .orElseThrow(() -> new IllegalArgumentException("대상 회원이 존재하지 않습니다. id=" + targetMemberId));

        Member writer = memberRepository.findById(writerMemberId)
            .orElseThrow(() -> new IllegalArgumentException("작성자 회원이 존재하지 않습니다. id=" + writerMemberId));

        if (req.getDeleteIds() != null) {
            for (Long id : req.getDeleteIds()) {
                if (id == null) continue;
                memberMemoRepository.deleteByIdAndTargetMember_Id(id, targetMemberId);
            }
        }

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
            .content(result.getContent().stream().map(m -> MemoPageResponseDto.Item.builder()
                .id(m.getId())
                .content(m.getContent())
                .writerName(m.getWriterMember().getName())
                .createdAt(m.getCreatedAt())
                .build()
            ).toList())
            .build();
    }

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

        memberRepository.save(m);
    }

    @Transactional
    public void updateSellerAll(Long memberId, UpdateSellerProfileRequest req, MultipartFile logoFile, Long changedByMemberId) {

        SellerDealerProfile seller = crmSellerDealerProfileRepository.findByMember_Id(memberId)
            .orElseThrow(() -> new IllegalStateException("셀러 프로필이 없습니다."));

        validateAndApplySupplierCode(seller, req);
        applySellerBasicFields(seller, req);
        applySellerAddresses(seller, req);

        Path newLogoPath = null;
        Path oldLogoPath = toPathOrNull(seller.getLogoImagePath());
        boolean deleteOldLogoAfterCommit = false;

        String logoAction = normalizeLogoAction(req.getLogoAction());

        if ("DELETE".equals(logoAction)) {
            seller.setLogoImagePath(null);
            seller.setLogoImageRoad(null);
            deleteOldLogoAfterCommit = oldLogoPath != null;
        } else if ("REPLACE".equals(logoAction)) {
            if (logoFile == null || logoFile.isEmpty()) {
                throw new IllegalArgumentException("로고 변경(REPLACE) 시 이미지 파일은 필수입니다.");
            }
            validateLogoFile(logoFile);

            newLogoPath = uploadPathHelper.saveSellerLogoForCustomer(memberId, logoFile);
            seller.setLogoImagePath(newLogoPath.toString());
            seller.setLogoImageRoad(uploadPathHelper.publicUrlOf(newLogoPath));

            deleteOldLogoAfterCommit = oldLogoPath != null;
        }

        crmSellerDealerProfileRepository.save(seller);

        syncSettlementPolicy(seller, req.getSettlement(), changedByMemberId);
        syncSellerContacts(seller, req.getContacts());
        syncCategoryPermissions(seller, req);

        em.flush();

        registerLogoFileTransactionCallbacks(newLogoPath, oldLogoPath, deleteOldLogoAfterCommit);
    }

    private void validateAndApplySupplierCode(SellerDealerProfile seller, UpdateSellerProfileRequest req) {
        String supplierCode = req.getSupplierCode() == null ? "" : req.getSupplierCode().trim();

        if (!StringUtils.hasText(supplierCode)) {
            throw new IllegalArgumentException("공급사 코드는 필수입니다.");
        }

        boolean duplicated = crmSellerDealerProfileRepository.existsBySupplierCodeAndIdNot(supplierCode, seller.getId());
        if (duplicated) {
            throw new IllegalArgumentException("이미 사용 중인 공급사 코드입니다.");
        }

        seller.setSupplierCode(supplierCode);
    }

    private void applySellerBasicFields(SellerDealerProfile seller, UpdateSellerProfileRequest req) {
        seller.setShopName(nv(req.getShopName()));
        seller.setTel(nv(req.getTel()));
        seller.setFax(nv(req.getFax()));
        seller.setHomepageUrl(nv(req.getHomepageUrl()));
        seller.setProductTypeText(nv(req.getProductTypeText()));

        if (StringUtils.hasText(req.getTradingStatus())) {
            seller.setTradingStatus(TradingStatus.valueOf(req.getTradingStatus().trim()));
        }
        if (StringUtils.hasText(req.getSupplyType())) {
            seller.setSupplyType(SupplyType.valueOf(req.getSupplyType().trim()));
        }
        if (StringUtils.hasText(req.getSupplyStructure())) {
            seller.setSupplyStructure(SupplyStructure.valueOf(req.getSupplyStructure().trim()));
        }

        seller.setDealStartDate(req.getDealStartDate());
        seller.setDealStopDate(req.getDealStopDate());
    }

    private void applySellerAddresses(SellerDealerProfile seller, UpdateSellerProfileRequest req) {
        if (req.getBusinessAddress() != null) {
            seller.setBusinessAddress(Address.builder()
                .postcode(nv(req.getBusinessAddress().getPostcode()))
                .roadAddress(nv(req.getBusinessAddress().getRoadAddress()))
                .jibunAddress(nv(req.getBusinessAddress().getJibunAddress()))
                .detailAddress(nv(req.getBusinessAddress().getDetailAddress()))
                .build());
        }

        if (req.getReturnAddress() != null) {
            seller.setReturnAddress(Address.builder()
                .postcode(nv(req.getReturnAddress().getPostcode()))
                .roadAddress(nv(req.getReturnAddress().getRoadAddress()))
                .jibunAddress(nv(req.getReturnAddress().getJibunAddress()))
                .detailAddress(nv(req.getReturnAddress().getDetailAddress()))
                .build());
        }
    }

    private void validateLogoFile(MultipartFile logoFile) {
        String contentType = logoFile.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("로고 이미지는 이미지 파일만 업로드할 수 있습니다.");
        }
    }

    private void syncSettlementPolicy(
	    SellerDealerProfile seller,
	    UpdateSellerProfileRequest.SettlementPart settlement,
	    Long changedByMemberId
	) {
	    if (settlement == null) {
	        return;
	    }

	    DealerSettlementPolicy policy = crmDealerSettlementPolicyRepository
	        .findBySellerDealerProfile_Id(seller.getId())
	        .orElseGet(() -> DealerSettlementPolicy.builder()
	            .sellerDealerProfile(seller)
	            .commissionRate(settlement.getCommissionRate() != null ? settlement.getCommissionRate() : BigDecimal.ZERO)
	            .cycle(settlement.getCycle() != null ? SettlementCycle.valueOf(settlement.getCycle()) : SettlementCycle.MONTH_END)
	            .basis(settlement.getBasis() != null ? SettlementBasis.valueOf(settlement.getBasis()) : SettlementBasis.PAYMENT_COMPLETED)
	            .nextSettlementDate(settlement.getNextSettlementDate())
	            .build()
	        );

	    if (settlement.getCommissionRate() != null) {
	        policy.setCommissionRate(settlement.getCommissionRate());
	    }
	    if (StringUtils.hasText(settlement.getCycle())) {
	        policy.setCycle(SettlementCycle.valueOf(settlement.getCycle().trim()));
	    }
	    if (StringUtils.hasText(settlement.getBasis())) {
	        policy.setBasis(SettlementBasis.valueOf(settlement.getBasis().trim()));
	    }

	    policy.setNextSettlementDate(settlement.getNextSettlementDate());
	    DealerSettlementPolicy savedPolicy = crmDealerSettlementPolicyRepository.save(policy);

	    dealerSettlementPolicyHistoryService.syncHistoryOnPolicySave(
	        seller,
	        savedPolicy,
	        changedByMemberId
	    );
	}

    private void syncSellerContacts(SellerDealerProfile seller, List<UpdateSellerProfileRequest.ContactItem> reqContacts) {
        List<SellerContact> existing = crmSellerContactRepository.findBySellerDealerProfile_IdOrderByIdAsc(seller.getId());
        Map<Long, SellerContact> existingMap = existing.stream()
            .collect(Collectors.toMap(SellerContact::getId, x -> x));

        Set<Long> keepIds = new HashSet<>();

        if (reqContacts != null) {
            for (UpdateSellerProfileRequest.ContactItem item : reqContacts) {
                if (item == null) continue;

                String name = item.getName() == null ? "" : item.getName().trim();
                String phone = item.getPhone() == null ? "" : item.getPhone().trim();
                String email = item.getEmail() == null ? "" : item.getEmail().trim();

                boolean hasAny = StringUtils.hasText(name) || StringUtils.hasText(phone) || StringUtils.hasText(email);
                if (!hasAny) {
                    continue;
                }

                if (!StringUtils.hasText(name)) {
                    throw new IllegalArgumentException("담당자명은 필수입니다.");
                }

                if (item.getId() != null) {
                    SellerContact contact = existingMap.get(item.getId());
                    if (contact == null) {
                        throw new IllegalArgumentException("존재하지 않는 담당자입니다. id=" + item.getId());
                    }

                    contact.setName(name);
                    contact.setPhone(phone);
                    contact.setEmail(email);
                    crmSellerContactRepository.save(contact);
                    keepIds.add(contact.getId());
                } else {
                    SellerContact newContact = SellerContact.builder()
                        .sellerDealerProfile(seller)
                        .name(name)
                        .phone(phone)
                        .email(email)
                        .build();

                    SellerContact saved = crmSellerContactRepository.save(newContact);
                    keepIds.add(saved.getId());
                }
            }
        }

        for (SellerContact old : existing) {
            if (!keepIds.contains(old.getId())) {
                crmSellerContactRepository.delete(old);
            }
        }
    }

    private void syncCategoryPermissions(SellerDealerProfile seller, UpdateSellerProfileRequest req) {

        if (req.getDeletePermissionIds() != null) {
            for (Long id : req.getDeletePermissionIds()) {
                if (id == null) continue;
                crmDealerCategoryPermissionRepository.deleteByIdAndSellerDealerProfile_Id(id, seller.getId());
            }
        }

        if (req.getAddPermissions() == null) {
            return;
        }

        final Set<Long> largeIds = new HashSet<>();
        final Set<Long> largeAllSet = new HashSet<>();
        final Map<Long, Set<Long>> mediumAllByLarge = new HashMap<>();
        final Map<Long, Map<Long, Set<Long>>> smallByLargeMedium = new HashMap<>();

        for (UpdateSellerProfileRequest.AddPermissionItem add : req.getAddPermissions()) {
            if (add == null) continue;
            if (add.getLargeId() == null) continue;

            final Long largeId = add.getLargeId();
            final Long mediumId = add.getMediumId();
            final Long smallId = add.getSmallId();

            if (smallId != null && mediumId == null) {
                throw new IllegalArgumentException("소분류가 선택된 경우 중분류는 필수입니다.");
            }

            largeIds.add(largeId);

            if (mediumId == null && smallId == null) {
                largeAllSet.add(largeId);
                continue;
            }

            if (mediumId != null && smallId == null) {
                mediumAllByLarge.computeIfAbsent(largeId, k -> new HashSet<>()).add(mediumId);
                continue;
            }

            if (mediumId != null && smallId != null) {
                smallByLargeMedium
                    .computeIfAbsent(largeId, k -> new HashMap<>())
                    .computeIfAbsent(mediumId, k -> new HashSet<>())
                    .add(smallId);
            }
        }

        final List<Long> sortedLargeIds = new ArrayList<>(largeIds);
        Collections.sort(sortedLargeIds);

        for (Long largeId : sortedLargeIds) {

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

            final Set<Long> mediumAllSet = mediumAllByLarge.getOrDefault(largeId, Collections.emptySet());
            final List<Long> sortedMediumIds = new ArrayList<>(mediumAllSet);
            Collections.sort(sortedMediumIds);

            for (Long mediumId : sortedMediumIds) {
                DealerCategoryPermission perm = DealerCategoryPermission.builder()
                    .sellerDealerProfile(seller)
                    .large(refCategoryLarge(largeId))
                    .medium(refCategoryMedium(mediumId))
                    .small(null)
                    .build();

                crmDealerCategoryPermissionRepository.save(perm);
            }

            final Map<Long, Set<Long>> smallMap = smallByLargeMedium.getOrDefault(largeId, Collections.emptyMap());
            final List<Long> smallMediumIds = new ArrayList<>(smallMap.keySet());
            Collections.sort(smallMediumIds);

            for (Long mediumId : smallMediumIds) {
                if (mediumAllSet.contains(mediumId)) continue;

                final List<Long> sortedSmallIds = new ArrayList<>(smallMap.getOrDefault(mediumId, Collections.emptySet()));
                Collections.sort(sortedSmallIds);

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

    private void registerLogoFileTransactionCallbacks(Path newLogoPath, Path oldLogoPath, boolean deleteOldLogoAfterCommit) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            if (deleteOldLogoAfterCommit && oldLogoPath != null) {
                uploadPathHelper.deleteIfExistsQuietly(oldLogoPath);
            }
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (deleteOldLogoAfterCommit && oldLogoPath != null) {
                    uploadPathHelper.deleteIfExistsQuietly(oldLogoPath);
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED && newLogoPath != null) {
                    uploadPathHelper.deleteIfExistsQuietly(newLogoPath);
                }
            }
        });
    }

    private String normalizeLogoAction(String logoAction) {
        if (!StringUtils.hasText(logoAction)) {
            return "KEEP";
        }

        String v = logoAction.trim().toUpperCase();
        if (!"KEEP".equals(v) && !"DELETE".equals(v) && !"REPLACE".equals(v)) {
            throw new IllegalArgumentException("유효하지 않은 로고 처리 모드입니다.");
        }
        return v;
    }

    private Path toPathOrNull(String pathText) {
        if (!StringUtils.hasText(pathText)) {
            return null;
        }
        return Paths.get(pathText);
    }

    private CategoryLarge refCategoryLarge(Long id) {
        return em.getReference(CategoryLarge.class, id);
    }

    private CategoryMedium refCategoryMedium(Long id) {
        return em.getReference(CategoryMedium.class, id);
    }

    private CategorySmall refCategorySmall(Long id) {
        return em.getReference(CategorySmall.class, id);
    }

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
}