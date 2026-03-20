package com.dev.IbioScience.service.order;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.order.OrderCreateItemDTO;
import com.dev.IbioScience.dto.order.OrderCreateRequestDTO;
import com.dev.IbioScience.dto.order.OrderCreateResponseDTO;
import com.dev.IbioScience.dto.order.OrderDetailItemViewDTO;
import com.dev.IbioScience.dto.order.OrderDetailViewDTO;
import com.dev.IbioScience.dto.order.OrderStatusUpdateResponseDTO;
import com.dev.IbioScience.enums.order.OrderStatus;
import com.dev.IbioScience.enums.order.PaymentMethod;
import com.dev.IbioScience.enums.order.ShippingMethod;
import com.dev.IbioScience.enums.order.ShippingPayType;
import com.dev.IbioScience.enums.product.CouponStatus;
import com.dev.IbioScience.enums.product.PromotionType;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.order.Order;
import com.dev.IbioScience.model.order.OrderItem;
import com.dev.IbioScience.model.product.Coupon;
import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.ProductOption;
import com.dev.IbioScience.model.product.ProductOptionGroup;
import com.dev.IbioScience.model.product.Promotion;
import com.dev.IbioScience.model.product.relation.MemberCoupon;
import com.dev.IbioScience.model.product.relation.ProductPromotionMapping;
import com.dev.IbioScience.repository.auth.MemberCouponRepository;
import com.dev.IbioScience.repository.auth.MemberRepository;
import com.dev.IbioScience.repository.order.OrderRepository;
import com.dev.IbioScience.repository.product.register.ProductOptionGroupRepository;
import com.dev.IbioScience.repository.product.register.ProductOptionRepository;
import com.dev.IbioScience.repository.product.register.ProductRepository;
import com.dev.IbioScience.service.auth.crm.benefit.AdminClientBenefitService;

@Service
public class OrderService {

    /** ✅ 적립률 10% */
    private static final double EXPECT_POINT_RATE = 0.10;

    private final OrderRepository orderRepository;

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final ProductOptionGroupRepository productOptionGroupRepository;
    private final ProductOptionRepository productOptionRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final AdminClientBenefitService adminClientBenefitService;

    public OrderService(
            OrderRepository orderRepository,
            MemberRepository memberRepository,
            ProductRepository productRepository,
            ProductOptionGroupRepository productOptionGroupRepository,
            ProductOptionRepository productOptionRepository,
            MemberCouponRepository memberCouponRepository,
            AdminClientBenefitService adminClientBenefitService
    ) {
        this.orderRepository = orderRepository;
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
        this.productOptionGroupRepository = productOptionGroupRepository;
        this.productOptionRepository = productOptionRepository;
        this.memberCouponRepository = memberCouponRepository;
        this.adminClientBenefitService = adminClientBenefitService;
    }

    // =========================
    // 배송비 정책 (paymentStart.js와 동일)
    // =========================
    private long calcShippingFee(ShippingMethod method) {
        if (method == null) return 0;
        return switch (method) {
            case PARCEL -> 3000;
            case POST -> 5000;
            case QUICK -> 20000;
        };
    }

    // =========================
    // 주문번호 생성 (중복 시 재시도)
    // =========================
    private String generateOrderNo() {
        // 예: 20260102-AB12CD34
        String ymd = LocalDate.now().toString().replace("-", "");
        String rand = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return ymd + "-" + rand;
    }

    private String generateUniqueOrderNo() {
        for (int i = 0; i < 10; i++) {
            String no = generateOrderNo();
            if (!orderRepository.existsByOrderNo(no)) return no;
        }
        return LocalDate.now().toString().replace("-", "") + "-" + System.currentTimeMillis();
    }

    // =========================
    // 1) 결제대기 주문 생성
    // =========================
    @Transactional
    public OrderCreateResponseDTO createPendingOrder(Long loginMemberId, OrderCreateRequestDTO req) {

        if (loginMemberId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        if (req == null) {
            throw new IllegalArgumentException("요청이 비어있습니다.");
        }
        if (req.getUserId() == null || !req.getUserId().equals(loginMemberId)) {
            throw new IllegalArgumentException("잘못된 사용자 정보입니다.");
        }
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("주문 상품이 없습니다.");
        }

        Member member = memberRepository.findById(loginMemberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        PaymentMethod paymentMethod = parsePaymentMethod(req.getPaymentMethod());
        ShippingMethod shippingMethod = parseShippingMethod(req.getShippingMethod());
        ShippingPayType shippingPayType = parseShippingPayType(req.getShippingPayType());

        requireText(req.getReceiverName(), "수령인 이름");
        requireText(req.getHp1(), "휴대폰(앞자리)");
        requireText(req.getHp2(), "휴대폰(중간)");
        requireText(req.getHp3(), "휴대폰(끝)");
        requireText(req.getPostcode(), "우편번호");
        requireText(req.getRoadAddress(), "도로명주소");
        requireText(req.getDetailAddress(), "상세주소");

        long sumPrice = 0L;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderCreateItemDTO it : req.getItems()) {
            if (it.getProductId() == null) {
                throw new IllegalArgumentException("상품ID 누락");
            }
            if (it.getQuantity() == null || it.getQuantity() < 1) {
                throw new IllegalArgumentException("수량 오류");
            }
            if (it.getUnitPrice() == null || it.getUnitPrice() < 0) {
                throw new IllegalArgumentException("단가 오류");
            }

            Product product = productRepository.findById(it.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. productId=" + it.getProductId()));

            ProductOptionGroup og = null;
            ProductOption op = null;

            if (it.getOptionGroupId() != null) {
                og = productOptionGroupRepository.findById(it.getOptionGroupId())
                        .orElseThrow(() -> new IllegalArgumentException("옵션그룹을 찾을 수 없습니다. optionGroupId=" + it.getOptionGroupId()));
            }
            if (it.getOptionId() != null) {
                op = productOptionRepository.findById(it.getOptionId())
                        .orElseThrow(() -> new IllegalArgumentException("옵션을 찾을 수 없습니다. optionId=" + it.getOptionId()));
            }

            long line = it.getUnitPrice() * it.getQuantity();
            sumPrice += line;

            long itemEarnPoint = (long) Math.floor(line * EXPECT_POINT_RATE);

            OrderItem oi = OrderItem.builder()
                    .product(product)
                    .productOptionGroup(og)
                    .productOption(op)
                    .productName(nvl(it.getProductName(), product.getName()))
                    .productImageUrl(nvl(it.getProductImageUrl(), ""))
                    .optionGroupName(nvl(it.getOptionGroupName(), ""))
                    .optionName(nvl(it.getOptionName(), ""))
                    .optionCode(nvl(it.getOptionCode(), ""))
                    .unitText(nvl(it.getUnit(), "-"))
                    .unitPrice(it.getUnitPrice())
                    .quantity(it.getQuantity())
                    .linePrice(line)
                    .itemEarnPoint(itemEarnPoint)
                    .build();

            orderItems.add(oi);
        }

        long baseDiscount = 0L;

        long couponDiscount = 0L;
        MemberCoupon memberCoupon = null;
        String couponCode = null;
        String couponName = null;

        if (req.getMemberCouponId() != null) {
            memberCoupon = findUsableMemberCoupon(loginMemberId, req.getMemberCouponId());

            if (memberCoupon.getMember() == null || !Objects.equals(memberCoupon.getMember().getId(), loginMemberId)) {
                throw new IllegalArgumentException("본인 쿠폰만 사용할 수 있습니다.");
            }

            Coupon coupon = memberCoupon.getCoupon();
            validateCouponMasterPeriod(coupon);

            long min = safeLong(coupon.getMinPurchaseAmount());
            long amt = safeLong(coupon.getCouponAmount());

            if (sumPrice < min) {
                throw new IllegalArgumentException("쿠폰 최소 구매금액 조건을 만족하지 않습니다.");
            }

            couponDiscount = amt;
            couponCode = coupon.getCouponCode();
            couponName = coupon.getCouponName();
        }

        long pointAvail = member.getPoint() == null ? 0L : member.getPoint();
        long pointUse = req.getPointUse() == null ? 0L : Math.max(0L, req.getPointUse());
        if (pointUse > pointAvail) {
            pointUse = pointAvail;
        }

        long shippingFee = calcShippingFee(shippingMethod);
        long shipToPayNow = (shippingPayType == ShippingPayType.PREPAID) ? shippingFee : 0L;
        long grandTotal = Math.max(0L, sumPrice - baseDiscount - couponDiscount - pointUse + shipToPayNow);

        long expectPoint = orderItems.stream()
                .mapToLong(oi -> oi.getItemEarnPoint() == null ? 0L : oi.getItemEarnPoint())
                .sum();

        Order order = Order.builder()
                .orderNo(generateUniqueOrderNo())
                .member(member)
                .status(OrderStatus.ORDER_COMPLETED)
                .paymentMethod(paymentMethod)
                .shippingMethod(shippingMethod)
                .shippingPayType(shippingPayType)

                .receiverName(req.getReceiverName())
                .hp1(req.getHp1())
                .hp2(req.getHp2())
                .hp3(req.getHp3())
                .tel1(nvl(req.getTel1(), ""))
                .tel2(nvl(req.getTel2(), ""))
                .tel3(nvl(req.getTel3(), ""))
                .postcode(req.getPostcode())
                .roadAddress(req.getRoadAddress())
                .detailAddress(req.getDetailAddress())
                .shippingMemo(nvl(req.getShippingMemo(), ""))

                .sumPrice(sumPrice)
                .shippingFee(shippingFee)
                .baseDiscount(baseDiscount)
                .couponDiscount(couponDiscount)
                .pointUsed(pointUse)
                .grandTotal(grandTotal)
                .expectPoint(expectPoint)

                .memberCoupon(memberCoupon)
                .couponCode(couponCode)
                .couponName(couponName)

                .ordererName(nvl(req.getOrdererName(), ""))
                .ordererPhone(nvl(req.getOrdererPhone(), ""))
                .orderSmsAgree(req.getOrderSmsAgree() != null ? req.getOrderSmsAgree() : Boolean.TRUE)

                .build();

        for (OrderItem oi : orderItems) {
            order.addItem(oi);
        }

        orderRepository.save(order);

        return OrderCreateResponseDTO.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .status(order.getStatus().name())
                .build();
    }

    // =========================
    // 2) 결제완료 처리
    // =========================
    @Transactional
    public OrderStatusUpdateResponseDTO markPaymentCompleted(Long loginMemberId, String orderNo) {

        Order order = orderRepository.findByMember_IdAndOrderNo(loginMemberId, orderNo)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        if (order.getStatus() != OrderStatus.ORDER_COMPLETED && order.getStatus() != OrderStatus.PAYMENT_ERROR) {
            throw new IllegalArgumentException("현재 상태에서는 결제완료 처리할 수 없습니다. status=" + order.getStatus());
        }

        LocalDateTime paidAt = LocalDateTime.now();

        order.setStatus(OrderStatus.PRODUCT_PREPARING);
        order.setPaidAt(paidAt);

        // =========================
        // 1) 실제 사용된 쿠폰만 USED 처리 + 사용이력 저장
        // =========================
        if (order.getMemberCoupon() != null && nvlLong(order.getCouponDiscount()) > 0L) {
            MemberCoupon usedMemberCoupon = order.getMemberCoupon();
            usedMemberCoupon.setUsedAt(paidAt);
            usedMemberCoupon.setStatus(CouponStatus.USED);

            adminClientBenefitService.recordCouponUse(usedMemberCoupon, order);
        }

        // =========================
        // 2) 포인트 차감 + 적립
        // =========================
        Member member = order.getMember();
        long cur = member.getPoint() == null ? 0L : member.getPoint();
        long used = order.getPointUsed() == null ? 0L : order.getPointUsed();

        long earn = 0L;
        if (order.getItems() != null) {
            earn = order.getItems().stream()
                    .mapToLong(oi -> oi.getItemEarnPoint() == null ? 0L : oi.getItemEarnPoint())
                    .sum();
        }

        order.setExpectPoint(earn);

        long next = cur - used + earn;
        if (next < 0) {
            next = 0;
        }
        member.setPoint(next);

        // =========================
        // 3) 구매 상품의 프로모션 중 쿠폰 타입이 있으면 회원쿠폰 발급 + 발급이력 저장
        // =========================
        issuePromotionCouponsByOrder(order, paidAt);

        return OrderStatusUpdateResponseDTO.builder()
                .orderNo(order.getOrderNo())
                .status(order.getStatus().name())
                .build();
    }
    
    private void validateCouponMasterPeriod(Coupon coupon) {
        if (coupon == null) {
            throw new IllegalArgumentException("쿠폰 정보가 비어있습니다.");
        }

        LocalDate today = LocalDate.now();

        if (coupon.getStartDate() != null && today.isBefore(coupon.getStartDate())) {
            throw new IllegalArgumentException("아직 사용 시작 전인 쿠폰입니다.");
        }

        if (coupon.getEndDate() != null && today.isAfter(coupon.getEndDate())) {
            throw new IllegalArgumentException("사용 기간이 종료된 쿠폰입니다.");
        }
    }
    
    private void issuePromotionCouponsByOrder(Order order, LocalDateTime issuedAt) {
        if (order == null || order.getMember() == null || order.getMember().getId() == null) {
            return;
        }

        if (order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }

        Long memberId = order.getMember().getId();
        LocalDate baseDate = issuedAt != null ? issuedAt.toLocalDate() : LocalDate.now();

        // 같은 주문에서 동일 coupon_id 중복 발급 방지
        Set<Long> processedCouponIds = new HashSet<>();

        for (OrderItem item : order.getItems()) {
            if (item == null || item.getProduct() == null) {
                continue;
            }

            Product product = item.getProduct();
            if (product.getDiscountMappings() == null || product.getDiscountMappings().isEmpty()) {
                continue;
            }

            for (ProductPromotionMapping mapping : product.getDiscountMappings()) {
                if (mapping == null || mapping.getPromotion() == null) {
                    continue;
                }

                Promotion promotion = mapping.getPromotion();

                if (!isIssuableCouponPromotion(promotion, baseDate)) {
                    continue;
                }

                Coupon coupon = promotion.getCoupon();
                if (coupon == null || coupon.getId() == null) {
                    continue;
                }

                // 같은 주문 내 같은 coupon 중복 처리 방지
                if (!processedCouponIds.add(coupon.getId())) {
                    continue;
                }

                // 현재 DB unique(member_id, coupon_id) 구조상 이미 있으면 재발급 불가
                if (memberCouponRepository.existsByMember_IdAndCoupon_Id(memberId, coupon.getId())) {
                    continue;
                }

                MemberCoupon memberCoupon = new MemberCoupon();
                memberCoupon.setMember(order.getMember());
                memberCoupon.setCoupon(coupon);
                memberCoupon.setStatus(CouponStatus.ISSUED);
                memberCoupon.setIssuedAt(issuedAt);
                memberCoupon.setExpiredAt(coupon.getEndDate() != null ? coupon.getEndDate().atTime(LocalTime.MAX) : null);
                memberCoupon.setDeletedYn(false);

                MemberCoupon savedMemberCoupon = memberCouponRepository.save(memberCoupon);

                adminClientBenefitService.recordCouponIssueByOrder(savedMemberCoupon, order);
            }
        }
    }

    private boolean isIssuableCouponPromotion(Promotion promotion, LocalDate baseDate) {
        if (promotion == null) {
            return false;
        }

        if (promotion.getType() != PromotionType.COUPON) {
            return false;
        }

        if (!Boolean.TRUE.equals(promotion.getActive())) {
            return false;
        }

        if (promotion.getCoupon() == null) {
            return false;
        }

        if (promotion.getStartDate() != null && baseDate.isBefore(promotion.getStartDate())) {
            return false;
        }

        if (promotion.getEndDate() != null && baseDate.isAfter(promotion.getEndDate())) {
            return false;
        }

        return true;
    }
    // =========================
    // 3) 결제에러 처리
    // =========================
    @Transactional
    public OrderStatusUpdateResponseDTO markPaymentError(Long loginMemberId, String orderNo) {

        Order order = orderRepository.findByMember_IdAndOrderNo(loginMemberId, orderNo)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        if (order.getStatus() != OrderStatus.ORDER_COMPLETED) {
            throw new IllegalArgumentException("현재 상태에서는 결제에러 처리할 수 없습니다. status=" + order.getStatus());
        }

        order.setStatus(OrderStatus.PAYMENT_ERROR);

        return OrderStatusUpdateResponseDTO.builder()
                .orderNo(order.getOrderNo())
                .status(order.getStatus().name())
                .build();
    }

    // =========================
    // 4) 완료페이지 조회 DTO
    // =========================
    @Transactional(readOnly = true)
    public OrderDetailViewDTO getOrderDetailForView(Long loginMemberId, String orderNo) {

        Order order = orderRepository.findByMember_IdAndOrderNo(loginMemberId, orderNo)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        String fullAddr = order.getRoadAddress() + " " + order.getDetailAddress();

        List<OrderDetailItemViewDTO> itemViews = order.getItems().stream()
                .map(oi -> {
                    String optLabel = buildOptionLabel(oi);
                    long subtotal = (oi.getUnitPrice() == null ? 0L : oi.getUnitPrice()) * (oi.getQuantity() == null ? 0 : oi.getQuantity());

                    return OrderDetailItemViewDTO.builder()
                            .productId(oi.getProduct() != null ? oi.getProduct().getId() : null)
                            .productName(oi.getProductName())
                            .productImageUrl(nvl(oi.getProductImageUrl(), "/front/image/sample/100-100.png"))
                            .optionLabel(optLabel)
                            .unitText(nvl(oi.getUnitText(), "-"))
                            .unitPrice(nvlLong(oi.getUnitPrice()))
                            .quantity(oi.getQuantity() == null ? 0 : oi.getQuantity())
                            .discount(0L)
                            .subtotal(subtotal)
                            .itemEarnPoint(nvlLong(oi.getItemEarnPoint()))
                            .shippingText("(공통)") // ✅ 라인 표기는 공통 처리
                            .build();
                })
                .collect(Collectors.toList());

        return OrderDetailViewDTO.builder()
                .orderNo(order.getOrderNo())
                .status(order.getStatus().name())
                .receiverName(order.getReceiverName())
                .fullAddress(fullAddr)

                .sumPrice(nvlLong(order.getSumPrice()))
                .shippingFee(nvlLong(order.getShippingFee()))
                .baseDiscount(nvlLong(order.getBaseDiscount()))
                .couponDiscount(nvlLong(order.getCouponDiscount()))
                .pointUsed(nvlLong(order.getPointUsed()))
                .grandTotal(nvlLong(order.getGrandTotal()))
                .expectPoint(nvlLong(order.getExpectPoint()))

                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "")
                .bankInfoText("국민은행 000-00-00000")
                .items(itemViews)
                .build();
    }

    // =========================
    // 내부 유틸
    // =========================
    private PaymentMethod parsePaymentMethod(String v) {
        if (v == null) throw new IllegalArgumentException("결제수단 누락");
        return switch (v) {
            case "ACCOUNT_TRANSFER" -> PaymentMethod.ACCOUNT_TRANSFER;
            case "CREDIT_CARD" -> PaymentMethod.CREDIT_CARD;
            case "PSYS" -> PaymentMethod.PSYS;
            default -> throw new IllegalArgumentException("결제수단 오류: " + v);
        };
    }

    private ShippingMethod parseShippingMethod(String v) {
        if (v == null) throw new IllegalArgumentException("배송방법 누락");
        return switch (v) {
            case "PARCEL" -> ShippingMethod.PARCEL;
            case "POST" -> ShippingMethod.POST;
            case "QUICK" -> ShippingMethod.QUICK;
            default -> throw new IllegalArgumentException("배송방법 오류: " + v);
        };
    }

    private ShippingPayType parseShippingPayType(String v) {
        if (v == null) throw new IllegalArgumentException("배송 결제방식 누락");
        return switch (v) {
            case "PREPAID" -> ShippingPayType.PREPAID;
            case "COLLECT" -> ShippingPayType.COLLECT;
            default -> throw new IllegalArgumentException("배송 결제방식 오류: " + v);
        };
    }

    private void requireText(String s, String label) {
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException(label + "은(는) 필수입니다.");
        }
    }

    private String nvl(String s, String def) {
        return (s == null) ? def : s;
    }

    private long nvlLong(Long v) {
        return v == null ? 0L : v;
    }

    private long safeLong(java.math.BigDecimal bd) {
        if (bd == null) return 0L;
        try {
            return bd.longValue();
        } catch (Exception e) {
            return 0L;
        }
    }

    private String buildOptionLabel(OrderItem oi) {
        List<String> parts = new ArrayList<>();
        if (oi.getOptionGroupName() != null && !oi.getOptionGroupName().isBlank()) parts.add(oi.getOptionGroupName());
        if (oi.getOptionName() != null && !oi.getOptionName().isBlank()) parts.add(oi.getOptionName());
        if (oi.getOptionCode() != null && !oi.getOptionCode().isBlank()) parts.add(oi.getOptionCode());
        String s = String.join(" / ", parts);
        return s.isBlank() ? "-" : s;
    }
    
    private MemberCoupon findUsableMemberCoupon(Long loginMemberId, Long memberCouponId) {
        return memberCouponRepository.findUsableMemberCouponForOrder(
                        loginMemberId,
                        memberCouponId,
                        CouponStatus.ISSUED,
                        LocalDateTime.now()
                )
                .orElseThrow(() -> new IllegalArgumentException("사용 가능한 쿠폰이 아니거나 삭제된 쿠폰입니다."));
    }
}