package com.dev.IbioScience.service.auth.admin.common;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.customer.auth.transfer.AdminPageResponse;
import com.dev.IbioScience.dto.customer.auth.transfer.SellerDealerApproveRequest;
import com.dev.IbioScience.dto.customer.auth.transfer.SellerTransferDetailDto;
import com.dev.IbioScience.dto.customer.auth.transfer.SellerTransferRowDto;
import com.dev.IbioScience.dto.customer.auth.transfer.SellerTransferSearchRequest;
import com.dev.IbioScience.enums.auth.CustomerType;
import com.dev.IbioScience.enums.auth.DealerApplicationStatus;
import com.dev.IbioScience.enums.auth.DealerType;
import com.dev.IbioScience.enums.auth.MemberRole;
import com.dev.IbioScience.enums.auth.MemberStatus;
import com.dev.IbioScience.enums.product.SettlementBasis;
import com.dev.IbioScience.enums.product.SettlementCycle;
import com.dev.IbioScience.model.auth.CompanyProfile;
import com.dev.IbioScience.model.auth.DealerCategoryPermission;
import com.dev.IbioScience.model.auth.DealerSettlementPolicy;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.SellerContact;
import com.dev.IbioScience.model.auth.SellerDealerProfile;
import com.dev.IbioScience.model.auth.embedded.Address;
import com.dev.IbioScience.model.auth.utils.DealerConversionApplication;
import com.dev.IbioScience.model.product.category.CategoryLarge;
import com.dev.IbioScience.model.product.category.CategoryMedium;
import com.dev.IbioScience.model.product.category.CategorySmall;
import com.dev.IbioScience.repository.auth.MemberRepository;
import com.dev.IbioScience.repository.auth.utils.DealerConversionApplicationRepository;
import com.dev.IbioScience.service.settlement.DealerSettlementPolicyHistoryService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SellerDealerConversionAdminService {

    private final EntityManager em;

    private final DealerConversionApplicationRepository dealerConversionApplicationRepository;
    private final MemberRepository memberRepository;
    private final DealerSettlementPolicyHistoryService dealerSettlementPolicyHistoryService;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Transactional(readOnly = true)
    public AdminPageResponse<SellerTransferRowDto> searchPendingSeller(SellerTransferSearchRequest req) {

        int page = (req.getPage() == null || req.getPage() < 0) ? 0 : req.getPage();
        int size = normalizeSize(req.getSize());

        LocalDateTime from = parseFrom(req.getFromDate());
        LocalDateTime to = parseTo(req.getToDate());

        String searchType = emptyToNull(req.getSearchType());
        String keyword = emptyToNull(req.getKeyword());

        String sortKey = emptyToNull(req.getSortKey());
        String sortDir = emptyToNull(req.getSortDir());

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<DealerConversionApplication> cq = cb.createQuery(DealerConversionApplication.class);
        Root<DealerConversionApplication> root = cq.from(DealerConversionApplication.class);
        Join<DealerConversionApplication, Member> applicant = root.join("applicant", JoinType.INNER);
        Join<Member, CompanyProfile> companyProfile = applicant.join("companyProfile", JoinType.LEFT);

        List<Predicate> preds = new ArrayList<>();
        preds.add(cb.equal(root.get("status"), DealerApplicationStatus.PENDING));
        preds.add(cb.equal(root.get("toDealerType"), DealerType.SELLER));

        if (from != null) preds.add(cb.greaterThanOrEqualTo(root.get("requestedAt"), from));
        if (to != null) preds.add(cb.lessThanOrEqualTo(root.get("requestedAt"), to));

        if (keyword != null && searchType != null) {
            switch (searchType) {
                case "USERNAME" -> preds.add(cb.like(applicant.get("username"), "%" + keyword + "%"));
                case "MOBILE" -> preds.add(cb.like(applicant.get("mobile"), "%" + keyword + "%"));
                case "NAME" -> preds.add(cb.like(applicant.get("name"), "%" + keyword + "%"));
                default -> { }
            }
        }

        cq.where(preds.toArray(new Predicate[0]));
        cq.orderBy(buildSellerSort(cb, root, applicant, companyProfile, sortKey, sortDir));

        TypedQuery<DealerConversionApplication> q = em.createQuery(cq);
        q.setFirstResult(page * size);
        q.setMaxResults(size);
        List<DealerConversionApplication> rows = q.getResultList();

        CriteriaQuery<Long> countQ = cb.createQuery(Long.class);
        Root<DealerConversionApplication> countRoot = countQ.from(DealerConversionApplication.class);
        Join<DealerConversionApplication, Member> countApplicant = countRoot.join("applicant", JoinType.INNER);

        List<Predicate> countPreds = new ArrayList<>();
        countPreds.add(cb.equal(countRoot.get("status"), DealerApplicationStatus.PENDING));
        countPreds.add(cb.equal(countRoot.get("toDealerType"), DealerType.SELLER));
        if (from != null) countPreds.add(cb.greaterThanOrEqualTo(countRoot.get("requestedAt"), from));
        if (to != null) countPreds.add(cb.lessThanOrEqualTo(countRoot.get("requestedAt"), to));
        if (keyword != null && searchType != null) {
            switch (searchType) {
                case "USERNAME" -> countPreds.add(cb.like(countApplicant.get("username"), "%" + keyword + "%"));
                case "MOBILE" -> countPreds.add(cb.like(countApplicant.get("mobile"), "%" + keyword + "%"));
                case "NAME" -> countPreds.add(cb.like(countApplicant.get("name"), "%" + keyword + "%"));
                default -> { }
            }
        }
        countQ.select(cb.count(countRoot)).where(countPreds.toArray(new Predicate[0]));
        long total = em.createQuery(countQ).getSingleResult();
        int totalPages = (int) Math.ceil((double) total / (double) size);

        List<SellerTransferRowDto> content = rows.stream().map(app -> {
            Member m = app.getApplicant();
            String companyName = (m != null && m.getCompanyProfile() != null) ? m.getCompanyProfile().getCompanyName() : "- 없음-";
            return SellerTransferRowDto.builder()
                    .applicationId(app.getId())
                    .username(m != null ? m.getUsername() : "")
                    .companyName(companyName)
                    .name(m != null ? m.getName() : "")
                    .mobile(m != null ? m.getMobile() : "")
                    .requestedAt(app.getRequestedAt() != null ? app.getRequestedAt().format(DT) : "")
                    .build();
        }).toList();

        return AdminPageResponse.<SellerTransferRowDto>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(total)
                .totalPages(Math.max(totalPages, 1))
                .first(page <= 0)
                .last(page >= Math.max(totalPages - 1, 0))
                .build();
    }

    @Transactional(readOnly = true)
    public SellerTransferDetailDto getDetail(Long applicationId) {
        DealerConversionApplication app = dealerConversionApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("판매딜러 전환 신청을 찾을 수 없습니다."));

        Member m = app.getApplicant();
        String companyName = (m != null && m.getCompanyProfile() != null) ? m.getCompanyProfile().getCompanyName() : "- 없음-";
        String bizNo = (m != null && m.getCompanyProfile() != null) ? m.getCompanyProfile().getBusinessRegistrationNumber() : null;

        return SellerTransferDetailDto.builder()
                .applicationId(app.getId())
                .memberId(m != null ? m.getId() : null)
                .username(m != null ? m.getUsername() : null)
                .name(m != null ? m.getName() : null)
                .mobile(m != null ? m.getMobile() : null)
                .email(m != null ? m.getEmail() : null)
                .companyName(companyName)
                .businessRegistrationNumber(bizNo)
                .requestedAt(app.getRequestedAt() != null ? app.getRequestedAt().format(DT) : null)
                .note(app.getNote())
                .build();
    }

    @Transactional
    public void approveSeller(Long applicationId, Long processorMemberId, SellerDealerApproveRequest req) {

        if (processorMemberId == null) {
            throw new IllegalArgumentException("처리자(로그인 관리자) 정보가 없습니다.");
        }
        if (req == null) {
            throw new IllegalArgumentException("승인 입력값이 비어있습니다.");
        }

        DealerConversionApplication app = dealerConversionApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("판매딜러 전환 신청을 찾을 수 없습니다."));

        if (app.getStatus() != DealerApplicationStatus.PENDING) {
            throw new IllegalArgumentException("대기 상태(PENDING)인 신청만 승인할 수 있습니다.");
        }
        if (app.getToDealerType() != DealerType.SELLER) {
            throw new IllegalArgumentException("SELLER 전환 신청이 아닙니다.");
        }

        Member applicant = app.getApplicant();
        if (applicant == null || applicant.getId() == null) {
            throw new IllegalArgumentException("신청자 정보가 올바르지 않습니다.");
        }

        if (applicant.getStatus() != MemberStatus.ACTIVE) {
            throw new IllegalArgumentException("활성 회원만 승인 가능합니다.");
        }
        if (applicant.getCustomerType() != CustomerType.BUSINESS) {
            throw new IllegalArgumentException("사업자(BUSINESS) 회원만 판매딜러 승인 가능합니다.");
        }
        if (applicant.getDealerType() == DealerType.SELLER) {
            throw new IllegalArgumentException("이미 판매딜러입니다.");
        }
        if (applicant.getCompanyProfile() == null) {
            throw new IllegalArgumentException("회사정보(companyProfile)가 없는 회원은 판매딜러 승인 불가입니다.");
        }

        Long exists = em.createQuery(
                "select count(s.id) from SellerDealerProfile s where s.member.id = :mid",
                Long.class
        ).setParameter("mid", applicant.getId()).getSingleResult();
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("이미 판매딜러 프로필이 존재합니다.");
        }

        requireText(req.getShopName(), "입점몰명(shopName)");
        requireText(req.getTradingStatus(), "거래상태(tradingStatus)");
        requireText(req.getSupplyType(), "공급유형(supplyType)");
        requireText(req.getSupplyStructure(), "공급구조(supplyStructure)");
        requireText(req.getProductTypeText(), "거래상품유형(productTypeText)");
        requireText(req.getTel(), "일반전화(tel)");

        if (req.getBusinessAddress() == null) throw new IllegalArgumentException("사업장 주소가 필요합니다.");
        if (req.getReturnAddress() == null) throw new IllegalArgumentException("반품 주소가 필요합니다.");

        requireText(req.getBusinessAddress().getPostcode(), "사업장 우편번호");
        requireText(req.getBusinessAddress().getRoadAddress(), "사업장 도로명주소");
        requireText(req.getReturnAddress().getPostcode(), "반품 우편번호");
        requireText(req.getReturnAddress().getRoadAddress(), "반품 도로명주소");

        if (req.getCategoryPermissions() == null || req.getCategoryPermissions().isEmpty()) {
            throw new IllegalArgumentException("카테고리 권한은 1개 이상 등록되어야 합니다.");
        }

        if (req.getSettlementPolicy() == null) {
            throw new IllegalArgumentException("정산정책(settlementPolicy)이 필요합니다.");
        }
        if (req.getSettlementPolicy().getCommissionRate() == null) {
            throw new IllegalArgumentException("수수료율(commissionRate)은 필수입니다.");
        }
        BigDecimal commissionRate = req.getSettlementPolicy().getCommissionRate();
        if (commissionRate.compareTo(BigDecimal.ZERO) < 0 || commissionRate.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("수수료율은 0~100 범위여야 합니다.");
        }
        requireText(req.getSettlementPolicy().getCycle(), "정산주기(cycle)");
        requireText(req.getSettlementPolicy().getBasis(), "정산기준(basis)");

        SettlementCycle cycle = SettlementCycle.valueOf(req.getSettlementPolicy().getCycle());
        SettlementBasis basis = SettlementBasis.valueOf(req.getSettlementPolicy().getBasis());

        com.dev.IbioScience.enums.product.TradingStatus tradingStatus =
                com.dev.IbioScience.enums.product.TradingStatus.valueOf(req.getTradingStatus());
        com.dev.IbioScience.enums.product.SupplyType supplyType =
                com.dev.IbioScience.enums.product.SupplyType.valueOf(req.getSupplyType());
        com.dev.IbioScience.enums.product.SupplyStructure supplyStructure =
                com.dev.IbioScience.enums.product.SupplyStructure.valueOf(req.getSupplyStructure());

        String supplierCode = generateUniqueSupplierCode();

        SellerDealerProfile profile = SellerDealerProfile.builder()
                .member(applicant)
                .companyProfile(applicant.getCompanyProfile())
                .shopName(req.getShopName())
                .logoImagePath(null)
                .logoImageRoad(null)
                .supplierCode(supplierCode)
                .tradingStatus(tradingStatus)
                .supplyType(supplyType)
                .supplyStructure(supplyStructure)
                .productTypeText(req.getProductTypeText())
                .tel(req.getTel())
                .fax(emptyToNull(req.getFax()))
                .businessAddress(toAddress(req.getBusinessAddress()))
                .returnAddress(toAddress(req.getReturnAddress()))
                .homepageUrl(emptyToNull(req.getHomepageUrl()))
                .dealStartDate(LocalDate.now())
                .dealStopDate(null)
                .build();

        em.persist(profile);
        em.flush();

        DealerSettlementPolicy policy = DealerSettlementPolicy.builder()
                .sellerDealerProfile(profile)
                .commissionRate(commissionRate)
                .cycle(cycle)
                .basis(basis)
                .nextSettlementDate(calculateNextSettlementDate(LocalDate.now(), cycle))
                .build();
        em.persist(policy);
        em.flush();

        dealerSettlementPolicyHistoryService.syncHistoryOnPolicySave(
                profile,
                policy,
                processorMemberId
        );

        if (req.getContacts() != null) {
            for (SellerDealerApproveRequest.SellerContactDto c : req.getContacts()) {
                if (c == null) continue;
                boolean any = hasAnyText(c.getName()) || hasAnyText(c.getPhone()) || hasAnyText(c.getEmail());
                if (!any) continue;

                requireText(c.getName(), "담당자명(contact.name)");

                SellerContact sc = SellerContact.builder()
                        .sellerDealerProfile(profile)
                        .name(c.getName().trim())
                        .phone(emptyToNull(c.getPhone()))
                        .email(emptyToNull(c.getEmail()))
                        .build();

                em.persist(sc);
            }
        }

        Set<String> dedup = new LinkedHashSet<>();
        for (SellerDealerApproveRequest.CategoryPermissionDto p : req.getCategoryPermissions()) {
            if (p == null || p.getLargeId() == null) {
                throw new IllegalArgumentException("카테고리 권한 등록은 대분류(largeId)가 필수입니다.");
            }
            String key = p.getLargeId() + ":" + (p.getMediumId() == null ? "" : p.getMediumId()) + ":" + (p.getSmallId() == null ? "" : p.getSmallId());
            if (!dedup.add(key)) continue;

            CategoryLarge large = em.getReference(CategoryLarge.class, p.getLargeId());

            CategoryMedium medium = null;
            if (p.getMediumId() != null) {
                medium = em.getReference(CategoryMedium.class, p.getMediumId());
                Long midLargeId = medium.getLarge() != null ? medium.getLarge().getId() : null;
                if (midLargeId == null || !midLargeId.equals(p.getLargeId())) {
                    throw new IllegalArgumentException("중분류가 선택된 대분류의 하위가 아닙니다. (mediumId=" + p.getMediumId() + ")");
                }
            }

            CategorySmall small = null;
            if (p.getSmallId() != null) {
                small = em.getReference(CategorySmall.class, p.getSmallId());
                if (medium != null) {
                    Long cnt = em.createQuery(
                            "select count(ms.id) from MediumSmallProductCategory ms where ms.medium.id = :mid and ms.small.id = :sid",
                            Long.class
                    ).setParameter("mid", medium.getId())
                     .setParameter("sid", small.getId())
                     .getSingleResult();

                    if (cnt == null || cnt <= 0) {
                        throw new IllegalArgumentException("선택한 중분류-소분류 조합이 유효하지 않습니다. (mediumId=" + medium.getId() + ", smallId=" + small.getId() + ")");
                    }
                }
            }

            DealerCategoryPermission perm = DealerCategoryPermission.builder()
                    .sellerDealerProfile(profile)
                    .large(large)
                    .medium(medium)
                    .small(small)
                    .build();

            em.persist(perm);
        }

        applicant.setDealerType(DealerType.SELLER);
        applicant.setRole(MemberRole.SELLER_DEALER);
        memberRepository.save(applicant);

        app.setStatus(DealerApplicationStatus.APPROVED);
        app.setProcessor(em.getReference(Member.class, processorMemberId));
        app.setProcessNote(emptyToNull(req.getProcessNote()));
        app.setProcessedAt(LocalDateTime.now());
        dealerConversionApplicationRepository.save(app);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listCategoryLarges() {
        List<CategoryLarge> list = em.createQuery("select l from CategoryLarge l order by l.name asc", CategoryLarge.class)
                .getResultList();
        return list.stream().map(l -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", l.getId());
            m.put("name", l.getName());
            return m;
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listCategoryMediums(Long largeId) {
        List<CategoryMedium> list = em.createQuery(
                "select m from CategoryMedium m where m.large.id = :lid order by m.name asc",
                CategoryMedium.class
        ).setParameter("lid", largeId).getResultList();

        return list.stream().map(mm -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", mm.getId());
            m.put("name", mm.getName());
            return m;
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listCategorySmallsByMedium(Long mediumId) {
        List<CategorySmall> list = em.createQuery(
                "select distinct ms.small from MediumSmallProductCategory ms where ms.medium.id = :mid order by ms.small.name asc",
                CategorySmall.class
        ).setParameter("mid", mediumId).getResultList();

        return list.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("name", s.getName());
            return m;
        }).toList();
    }

    public Map<String, List<String>> getSellerEnums() {
        Map<String, List<String>> map = new HashMap<>();
        map.put("tradingStatus", enumNames(com.dev.IbioScience.enums.product.TradingStatus.values()));
        map.put("supplyType", enumNames(com.dev.IbioScience.enums.product.SupplyType.values()));
        map.put("supplyStructure", enumNames(com.dev.IbioScience.enums.product.SupplyStructure.values()));
        map.put("settlementCycle", enumNames(SettlementCycle.values()));
        map.put("settlementBasis", enumNames(SettlementBasis.values()));
        return map;
    }

    private static List<String> enumNames(Enum<?>[] values) {
        List<String> out = new ArrayList<>();
        for (Enum<?> e : values) out.add(e.name());
        return out;
    }

    private static int normalizeSize(Integer size) {
        if (size == null) return 10;
        return switch (size) {
            case 10, 30, 50, 100 -> size;
            default -> 10;
        };
    }

    private static String emptyToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean hasAnyText(String v) {
        return v != null && !v.trim().isEmpty();
    }

    private static void requireText(String v, String fieldName) {
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " 값은 필수입니다.");
        }
    }

    private static LocalDateTime parseFrom(String fromDate) {
        String s = emptyToNull(fromDate);
        if (s == null) return null;
        return LocalDate.parse(s).atStartOfDay();
    }

    private static LocalDateTime parseTo(String toDate) {
        String s = emptyToNull(toDate);
        if (s == null) return null;
        return LocalDate.parse(s).atTime(LocalTime.of(23, 59, 59));
    }

    private static List<Order> buildSellerSort(
            CriteriaBuilder cb,
            Root<DealerConversionApplication> root,
            Join<DealerConversionApplication, Member> applicant,
            Join<Member, CompanyProfile> companyProfile,
            String sortKey,
            String sortDir) {

        boolean asc = "asc".equalsIgnoreCase(sortDir);
        String key = (sortKey == null) ? "requestedAt" : sortKey;

        jakarta.persistence.criteria.Expression<?> expr = switch (key) {
            case "username" -> applicant.get("username");
            case "companyName" -> companyProfile.get("companyName");
            case "name" -> applicant.get("name");
            case "mobile" -> applicant.get("mobile");
            case "requestedAt" -> root.get("requestedAt");
            default -> root.get("requestedAt");
        };

        Order order = asc ? cb.asc(expr) : cb.desc(expr);
        Order order2 = cb.desc(root.get("id"));
        return List.of(order, order2);
    }

    private static Address toAddress(SellerDealerApproveRequest.AddressDto dto) {
        Address a = new Address();
        a.setPostcode(emptyToNull(dto.getPostcode()));
        a.setRoadAddress(emptyToNull(dto.getRoadAddress()));
        a.setJibunAddress(emptyToNull(dto.getJibunAddress()));
        a.setDetailAddress(emptyToNull(dto.getDetailAddress()));
        return a;
    }

    private static LocalDate calculateNextSettlementDate(LocalDate today, SettlementCycle cycle) {
        if (cycle == SettlementCycle.MONTH_END) {
            return today.withDayOfMonth(today.lengthOfMonth());
        }

        int day = cycle.getDay();
        LocalDate candidate = today.withDayOfMonth(Math.min(day, today.lengthOfMonth()));

        if (!candidate.isBefore(today)) {
            return candidate;
        }

        LocalDate nextMonth = today.plusMonths(1);
        return nextMonth.withDayOfMonth(Math.min(day, nextMonth.lengthOfMonth()));
    }

    private String generateUniqueSupplierCode() {
        for (int i = 0; i < 20; i++) {
            String code = "SUP_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
            Long cnt = em.createQuery(
                    "select count(s.id) from SellerDealerProfile s where s.supplierCode = :c",
                    Long.class
            ).setParameter("c", code).getSingleResult();

            if (cnt == null || cnt == 0) return code;
        }
        throw new IllegalStateException("공급사 코드 생성에 실패했습니다(중복).");
    }
}