package com.dev.IbioScience.service.auth.crm.benefit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.dev.IbioScience.dto.admin.benefit.AdminPointAdjustRequest;
import com.dev.IbioScience.dto.admin.benefit.BenefitPageResponse;
import com.dev.IbioScience.dto.admin.benefit.CouponGrantRequest;
import com.dev.IbioScience.dto.admin.benefit.CouponRowResponse;
import com.dev.IbioScience.dto.admin.benefit.CouponSourceDetailResponse;
import com.dev.IbioScience.dto.admin.benefit.PointHistoryRowResponse;
import com.dev.IbioScience.dto.admin.benefit.PointSummaryResponse;
import com.dev.IbioScience.enums.product.CouponStatus;
import com.dev.IbioScience.enums.product.coupon.MemberCouponHistoryActionType;
import com.dev.IbioScience.enums.product.coupon.MemberCouponHistorySourceType;
import com.dev.IbioScience.enums.product.coupon.MemberPointAdminActionType;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.order.Order;
import com.dev.IbioScience.model.product.Coupon;
import com.dev.IbioScience.model.product.coupon.MemberCouponHistory;
import com.dev.IbioScience.model.product.coupon.MemberPointAdminHistory;
import com.dev.IbioScience.model.product.relation.MemberCoupon;
import com.dev.IbioScience.repository.auth.coupon.AdminClientBenefitCouponHistoryRepository;
import com.dev.IbioScience.repository.auth.coupon.AdminClientBenefitCouponMasterRepository;
import com.dev.IbioScience.repository.auth.coupon.AdminClientBenefitCouponRepository;
import com.dev.IbioScience.repository.auth.coupon.AdminClientBenefitMemberRepository;
import com.dev.IbioScience.repository.auth.coupon.AdminClientBenefitOrderRepository;
import com.dev.IbioScience.repository.auth.coupon.AdminClientBenefitPointAdminHistoryRepository;
import com.dev.IbioScience.repository.auth.coupon.AdminClientBenefitPointHistoryRowProjection;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminClientBenefitService {

    private static final int FIXED_PAGE_SIZE = 10;

    private final AdminClientBenefitMemberRepository memberRepository;
    private final AdminClientBenefitPointAdminHistoryRepository pointAdminHistoryRepository;
    private final AdminClientBenefitCouponRepository memberCouponRepository;
    private final AdminClientBenefitCouponMasterRepository couponRepository;
    private final AdminClientBenefitCouponHistoryRepository couponHistoryRepository;
    private final AdminClientBenefitOrderRepository orderRepository;

    public PointSummaryResponse getPointSummary(Long memberId) {
        Member member = findMember(memberId);
        return PointSummaryResponse.builder()
                .currentPoint(nvl(member.getPoint()))
                .build();
    }

    public BenefitPageResponse<PointHistoryRowResponse> getPointHistories(
            Long memberId,
            LocalDate fromDate,
            LocalDate toDate,
            int page
    ) {
        findMember(memberId);

        LocalDateTime fromAt = toStartOfDay(fromDate);
        LocalDateTime toAtExclusive = toExclusive(toDate);

        Page<AdminClientBenefitPointHistoryRowProjection> resultPage =
                pointAdminHistoryRepository.searchPointHistories(
                        memberId,
                        fromAt,
                        toAtExclusive,
                        PageRequest.of(normalizePage(page), FIXED_PAGE_SIZE)
                );

        Page<PointHistoryRowResponse> mappedPage = resultPage.map(row ->
                PointHistoryRowResponse.builder()
                        .changeType(row.getChangeType())
                        .amount(nvl(row.getAmount()))
                        .orderNo(row.getOrderNo())
                        .sourceText(row.getSourceText())
                        .occurredAt(row.getOccurredAt())
                        .build()
        );

        return BenefitPageResponse.from(mappedPage);
    }

    @Transactional
    public PointSummaryResponse grantPoint(Long memberId, AdminPointAdjustRequest request) {
        Member member = findMemberForUpdate(memberId);

        long amount = positiveAmount(request.getAmount());
        long before = nvl(member.getPoint());
        long after = before + amount;

        member.setPoint(after);

        MemberPointAdminHistory history = MemberPointAdminHistory.builder()
                .member(member)
                .actionType(MemberPointAdminActionType.GRANT)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(after)
                .adminUsername(currentAdminUsername())
                .description("관리자 적립금 부여")
                .build();

        pointAdminHistoryRepository.save(history);

        return PointSummaryResponse.builder()
                .currentPoint(after)
                .build();
    }

    @Transactional
    public PointSummaryResponse deductPoint(Long memberId, AdminPointAdjustRequest request) {
        Member member = findMemberForUpdate(memberId);

        long amount = positiveAmount(request.getAmount());
        long before = nvl(member.getPoint());

        if (before < amount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 적립금보다 큰 금액은 차감할 수 없습니다.");
        }

        long after = before - amount;
        member.setPoint(after);

        MemberPointAdminHistory history = MemberPointAdminHistory.builder()
                .member(member)
                .actionType(MemberPointAdminActionType.DEDUCT)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(after)
                .adminUsername(currentAdminUsername())
                .description("관리자 적립금 차감")
                .build();

        pointAdminHistoryRepository.save(history);

        return PointSummaryResponse.builder()
                .currentPoint(after)
                .build();
    }

    public BenefitPageResponse<CouponRowResponse> getCoupons(
            Long memberId,
            LocalDate fromDate,
            LocalDate toDate,
            List<CouponStatus> statuses,
            int page
    ) {
        findMember(memberId);

        List<CouponStatus> normalizedStatuses = normalizeStatuses(statuses);

        Page<MemberCoupon> couponPage = memberCouponRepository.searchActiveCoupons(
                memberId,
                toStartOfDay(fromDate),
                toExclusive(toDate),
                normalizedStatuses,
                PageRequest.of(normalizePage(page), FIXED_PAGE_SIZE)
        );

        List<Long> memberCouponIds = couponPage.getContent().stream()
                .map(MemberCoupon::getId)
                .toList();

        Map<Long, List<MemberCouponHistory>> historyMap = memberCouponIds.isEmpty()
                ? Collections.emptyMap()
                : couponHistoryRepository.findByMemberCouponIdInOrderByCreatedAtDesc(memberCouponIds)
                        .stream()
                        .collect(Collectors.groupingBy(history -> history.getMemberCoupon().getId()));

        Page<CouponRowResponse> mappedPage = couponPage.map(memberCoupon -> {
            List<MemberCouponHistory> histories = historyMap.getOrDefault(memberCoupon.getId(), List.of());

            MemberCouponHistory issueHistory = histories.stream()
                    .filter(history -> history.getActionType() == MemberCouponHistoryActionType.ISSUE)
                    .findFirst()
                    .orElse(null);

            MemberCouponHistory useHistory = histories.stream()
                    .filter(history -> history.getActionType() == MemberCouponHistoryActionType.USE)
                    .findFirst()
                    .orElse(null);

            String sourceText = resolveCouponSourceText(issueHistory);

            String usedOrderNo = null;
            if (useHistory != null && useHistory.getOrder() != null) {
                usedOrderNo = useHistory.getOrder().getOrderNo();
            } else if (memberCoupon.getStatus() == CouponStatus.USED) {
                usedOrderNo = orderRepository.findFirstByMemberCouponIdOrderByCreatedAtDesc(memberCoupon.getId())
                        .map(Order::getOrderNo)
                        .orElse(null);
            }

            return CouponRowResponse.builder()
                    .memberCouponId(memberCoupon.getId())
                    .couponName(memberCoupon.getCoupon().getCouponName())
                    .startDate(memberCoupon.getCoupon().getStartDate())
                    .endDate(memberCoupon.getCoupon().getEndDate())
                    .status(memberCoupon.getStatus())
                    .statusLabel(memberCoupon.getStatus().getLabel())
                    .sourceText(sourceText)
                    .usedOrderNo(usedOrderNo)
                    .issuedAt(memberCoupon.getIssuedAt())
                    .build();
        });

        return BenefitPageResponse.from(mappedPage);
    }

    public CouponSourceDetailResponse getCouponSourceDetail(Long memberId, Long memberCouponId) {
        MemberCoupon memberCoupon = memberCouponRepository.findActiveDetail(memberCouponId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원쿠폰을 찾을 수 없습니다."));

        List<MemberCouponHistory> histories = couponHistoryRepository.findByMemberCouponIdOrderByCreatedAtDesc(memberCouponId);

        MemberCouponHistory issueHistory = histories.stream()
                .filter(history -> history.getActionType() == MemberCouponHistoryActionType.ISSUE)
                .findFirst()
                .orElse(null);

        MemberCouponHistory useHistory = histories.stream()
                .filter(history -> history.getActionType() == MemberCouponHistoryActionType.USE)
                .findFirst()
                .orElse(null);

        String usedOrderNo = null;
        LocalDateTime usedOccurredAt = null;

        if (useHistory != null) {
            usedOccurredAt = useHistory.getCreatedAt();
            if (useHistory.getOrder() != null) {
                usedOrderNo = useHistory.getOrder().getOrderNo();
            }
        }

        if (usedOrderNo == null && memberCoupon.getStatus() == CouponStatus.USED) {
            Optional<Order> orderOpt = orderRepository.findFirstByMemberCouponIdOrderByCreatedAtDesc(memberCouponId);
            if (orderOpt.isPresent()) {
                usedOrderNo = orderOpt.get().getOrderNo();
                usedOccurredAt = orderOpt.get().getPaidAt() != null ? orderOpt.get().getPaidAt() : orderOpt.get().getCreatedAt();
            }
        }

        return CouponSourceDetailResponse.builder()
                .memberCouponId(memberCoupon.getId())
                .couponName(memberCoupon.getCoupon().getCouponName())
                .issueSourceText(resolveCouponSourceText(issueHistory))
                .issueOccurredAt(issueHistory != null ? issueHistory.getCreatedAt() : memberCoupon.getIssuedAt())
                .issueOrderNo(issueHistory != null && issueHistory.getOrder() != null ? issueHistory.getOrder().getOrderNo() : null)
                .issueAdminUsername(issueHistory != null ? issueHistory.getAdminUsername() : null)
                .usedOrderNo(usedOrderNo)
                .usedOccurredAt(usedOccurredAt)
                .build();
    }

    @Transactional
    public void grantCoupon(Long memberId, CouponGrantRequest request) {
        Member member = findMember(memberId);

        validateCouponGrantRequest(request);

        if (couponRepository.existsByCouponCode(request.getCouponCode().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 사용 중인 쿠폰코드입니다.");
        }

        Coupon coupon = new Coupon();
        coupon.setCouponCode(request.getCouponCode().trim());
        coupon.setCouponName(request.getCouponName().trim());
        coupon.setMinPurchaseAmount(request.getMinPurchaseAmount());
        coupon.setCouponAmount(request.getCouponAmount());
        coupon.setCouponPolicy(request.getCouponPolicy());
        coupon.setStartDate(request.getStartDate());
        coupon.setEndDate(request.getEndDate());
        coupon.setStatus(CouponStatus.ISSUED);

        Coupon savedCoupon = couponRepository.save(coupon);

        MemberCoupon memberCoupon = new MemberCoupon();
        memberCoupon.setMember(member);
        memberCoupon.setCoupon(savedCoupon);
        memberCoupon.setStatus(CouponStatus.ISSUED);
        memberCoupon.setIssuedAt(LocalDateTime.now());
        memberCoupon.setExpiredAt(request.getEndDate().atTime(LocalTime.MAX));
        memberCoupon.setDeletedYn(false);

        MemberCoupon savedMemberCoupon = memberCouponRepository.save(memberCoupon);

        MemberCouponHistory history = MemberCouponHistory.builder()
                .memberCoupon(savedMemberCoupon)
                .member(member)
                .coupon(savedCoupon)
                .order(null)
                .actionType(MemberCouponHistoryActionType.ISSUE)
                .sourceType(MemberCouponHistorySourceType.ADMIN)
                .adminUsername(currentAdminUsername())
                .description("관리자 쿠폰 발급")
                .build();

        couponHistoryRepository.save(history);
    }

    @Transactional
    public void deleteCoupon(Long memberId, Long memberCouponId) {
        MemberCoupon memberCoupon = memberCouponRepository.findActiveDetail(memberCouponId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "삭제 대상 쿠폰을 찾을 수 없습니다."));

        if (memberCoupon.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 삭제된 쿠폰입니다.");
        }

        memberCoupon.markDeleted(currentAdminUsername());

        MemberCouponHistory history = MemberCouponHistory.builder()
                .memberCoupon(memberCoupon)
                .member(memberCoupon.getMember())
                .coupon(memberCoupon.getCoupon())
                .order(null)
                .actionType(MemberCouponHistoryActionType.DELETE)
                .sourceType(MemberCouponHistorySourceType.ADMIN)
                .adminUsername(currentAdminUsername())
                .description("관리자 쿠폰 삭제")
                .build();

        couponHistoryRepository.save(history);
    }

    /**
     * 주문/프로모션으로 쿠폰이 회원에게 발급될 때 호출
     */
    @Transactional
    public void recordCouponIssueByOrder(MemberCoupon memberCoupon, Order order) {
        if (memberCoupon == null || order == null) {
            return;
        }

        boolean exists = couponHistoryRepository.existsByMemberCouponIdAndActionTypeAndOrderId(
                memberCoupon.getId(),
                MemberCouponHistoryActionType.ISSUE,
                order.getId()
        );

        if (exists) {
            return;
        }

        MemberCouponHistory history = MemberCouponHistory.builder()
                .memberCoupon(memberCoupon)
                .member(memberCoupon.getMember())
                .coupon(memberCoupon.getCoupon())
                .order(order)
                .actionType(MemberCouponHistoryActionType.ISSUE)
                .sourceType(MemberCouponHistorySourceType.ORDER_PROMOTION)
                .adminUsername(null)
                .description("주문/프로모션 쿠폰 발급")
                .build();

        couponHistoryRepository.save(history);
    }

    /**
     * 주문에서 쿠폰이 사용될 때 호출
     */
    @Transactional
    public void recordCouponUse(MemberCoupon memberCoupon, Order order) {
        if (memberCoupon == null || order == null) {
            return;
        }

        boolean exists = couponHistoryRepository.existsByMemberCouponIdAndActionTypeAndOrderId(
                memberCoupon.getId(),
                MemberCouponHistoryActionType.USE,
                order.getId()
        );

        if (exists) {
            return;
        }

        MemberCouponHistory history = MemberCouponHistory.builder()
                .memberCoupon(memberCoupon)
                .member(memberCoupon.getMember())
                .coupon(memberCoupon.getCoupon())
                .order(order)
                .actionType(MemberCouponHistoryActionType.USE)
                .sourceType(MemberCouponHistorySourceType.ORDER_USE)
                .adminUsername(null)
                .description("주문 쿠폰 사용")
                .build();

        couponHistoryRepository.save(history);
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
    }

    private Member findMemberForUpdate(Long memberId) {
        return memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private long nvl(Long value) {
        return value == null ? 0L : value;
    }

    private long positiveAmount(Long amount) {
        if (amount == null || amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "금액은 1 이상이어야 합니다.");
        }
        return amount;
    }

    private LocalDateTime toStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime toExclusive(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay();
    }

    private List<CouponStatus> normalizeStatuses(List<CouponStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return Arrays.asList(CouponStatus.values());
        }
        return statuses;
    }

    private String resolveCouponSourceText(MemberCouponHistory issueHistory) {
        if (issueHistory == null) {
            return "이력 없음";
        }

        if (issueHistory.getSourceType() == MemberCouponHistorySourceType.ADMIN) {
            return "관리자 발급";
        }

        if (issueHistory.getSourceType() == MemberCouponHistorySourceType.ORDER_PROMOTION) {
            return "주문/프로모션 발급";
        }

        return issueHistory.getSourceType().getLabel();
    }

    private void validateCouponGrantRequest(CouponGrantRequest request) {
        if (request.getCouponCode() == null || request.getCouponCode().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "쿠폰코드는 필수입니다.");
        }
        if (request.getCouponName() == null || request.getCouponName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "쿠폰명은 필수입니다.");
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "쿠폰 시작일은 종료일보다 늦을 수 없습니다.");
        }
        if (request.getMinPurchaseAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "최소 구매금액은 0 이상이어야 합니다.");
        }
        if (request.getCouponAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "쿠폰 금액은 0 이상이어야 합니다.");
        }
    }

    private String currentAdminUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "unknown-admin";
        }
        return authentication.getName();
    }
}