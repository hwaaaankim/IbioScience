package com.dev.IbioScience.service.seller.order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.dev.IbioScience.dto.seller.order.SellerOrderBatchStatusUpdateRequest;
import com.dev.IbioScience.dto.seller.order.SellerOrderDetailItemDto;
import com.dev.IbioScience.dto.seller.order.SellerOrderDetailResponse;
import com.dev.IbioScience.dto.seller.order.SellerOrderListItemDto;
import com.dev.IbioScience.dto.seller.order.SellerOrderListResponse;
import com.dev.IbioScience.dto.seller.order.SellerOrderSearchCondition;
import com.dev.IbioScience.enums.auth.DealerType;
import com.dev.IbioScience.enums.order.OrderStatus;
import com.dev.IbioScience.enums.order.PaymentMethod;
import com.dev.IbioScience.enums.order.ShippingMethod;
import com.dev.IbioScience.enums.order.ShippingPayType;
import com.dev.IbioScience.enums.product.dealer.OrderItemProductType;
import com.dev.IbioScience.model.auth.CompanyProfile;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.SellerDealerProfile;
import com.dev.IbioScience.model.order.Order;
import com.dev.IbioScience.model.order.OrderItem;
import com.dev.IbioScience.model.product.dealer.DealerProduct;
import com.dev.IbioScience.repository.auth.SellerDealerProfileRepository;
import com.dev.IbioScience.repository.order.OrderItemRepository;
import com.dev.IbioScience.repository.order.OrderRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerOrderService {

    private static final Set<OrderStatus> MANAGEABLE_STATUSES = EnumSet.of(
        OrderStatus.ORDER_COMPLETED,
        OrderStatus.PRODUCT_PREPARING,
        OrderStatus.CANCEL_FINISHED,
        OrderStatus.DELIVERING
    );

    private final SellerDealerProfileRepository sellerDealerProfileRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public SellerOrderListResponse getSellerOrders(Long sellerMemberId, SellerOrderSearchCondition condition) {
        validateSeller(sellerMemberId);

        int page = condition.getPage() == null || condition.getPage() < 0 ? 0 : condition.getPage();
        int size = normalizePageSize(condition.getSize());
        Sort sort = buildSort(condition.getSortField(), condition.getSortDir());

        Page<Order> orderPage = orderRepository.findAll(
            buildSpecification(sellerMemberId, condition),
            PageRequest.of(page, size, sort)
        );

        List<SellerOrderListItemDto> content = orderPage.getContent().stream()
            .map(this::toListItemDto)
            .toList();

        return SellerOrderListResponse.builder()
            .content(content)
            .page(orderPage.getNumber())
            .size(orderPage.getSize())
            .totalElements(orderPage.getTotalElements())
            .totalPages(orderPage.getTotalPages())
            .first(orderPage.isFirst())
            .last(orderPage.isLast())
            .empty(orderPage.isEmpty())
            .sortField(condition.getSortField() == null ? "orderedAt" : condition.getSortField())
            .sortDir(normalizeSortDir(condition.getSortDir()))
            .build();
    }

    public SellerOrderDetailResponse getSellerOrderDetail(Long sellerMemberId, Long orderId) {
        validateSeller(sellerMemberId);

        Order order = findVisibleOrderOrThrow(sellerMemberId, orderId);

        List<OrderItem> visibleItems = deduplicateVisibleOrderItems(
            orderItemRepository.findSellerVisibleDealerItems(orderId, sellerMemberId)
        );

        long visibleSumPrice = visibleItems.stream()
            .map(OrderItem::getLinePrice)
            .filter(Objects::nonNull)
            .mapToLong(Long::longValue)
            .sum();

        List<SellerOrderDetailItemDto> items = visibleItems.stream()
            .map(this::toDetailItemDto)
            .toList();

        Member member = order.getMember();
        CompanyProfile companyProfile = member != null ? member.getCompanyProfile() : null;

        String companyName = companyProfile != null && StringUtils.hasText(companyProfile.getCompanyName())
            ? companyProfile.getCompanyName()
            : "-";

        String shopName = "-";
        if (member != null
                && member.getDealerType() == DealerType.SELLER
                && member.getSellerDealerProfile() != null
                && StringUtils.hasText(member.getSellerDealerProfile().getShopName())) {
            shopName = member.getSellerDealerProfile().getShopName();
        }

        return SellerOrderDetailResponse.builder()
            .orderId(order.getId())
            .orderNo(order.getOrderNo())
            .status(order.getStatus())
            .statusLabel(getOrderStatusLabel(order.getStatus()))
            .orderedAt(order.getCreatedAt())
            .paidAt(order.getPaidAt())
            .ordererName(resolveOrdererName(order))
            .ordererUsername(member != null ? nullSafe(member.getUsername()) : "-")
            .contact(resolveContact(order))
            .email(member != null ? nullSafe(member.getEmail()) : "-")
            .dealerType(member != null ? member.getDealerType() : null)
            .dealerTypeLabel(getDealerTypeLabel(member != null ? member.getDealerType() : null))
            .companyName(companyName)
            .shopName(shopName)
            .receiverName(nullSafe(order.getReceiverName()))
            .hp1(nullSafe(order.getHp1()))
            .hp2(nullSafe(order.getHp2()))
            .hp3(nullSafe(order.getHp3()))
            .tel1(nullSafe(order.getTel1()))
            .tel2(nullSafe(order.getTel2()))
            .tel3(nullSafe(order.getTel3()))
            .postcode(nullSafe(order.getPostcode()))
            .roadAddress(nullSafe(order.getRoadAddress()))
            .detailAddress(nullSafe(order.getDetailAddress()))
            .shippingMemo(nullSafe(order.getShippingMemo()))
            .paymentMethod(order.getPaymentMethod())
            .paymentMethodLabel(getPaymentMethodLabel(order.getPaymentMethod()))
            .shippingMethod(order.getShippingMethod())
            .shippingMethodLabel(getShippingMethodLabel(order.getShippingMethod()))
            .shippingPayType(order.getShippingPayType())
            .shippingPayTypeLabel(getShippingPayTypeLabel(order.getShippingPayType()))
            .visibleItemCount(items.size())
            .visibleItemSumPrice(visibleSumPrice)
            .items(items)
            .build();
    }

    @Transactional
    public void updateSellerOrderStatuses(Long sellerMemberId, SellerOrderBatchStatusUpdateRequest request) {
        validateSeller(sellerMemberId);

        List<Long> orderIds = request.getItems().stream()
            .map(item -> item.getOrderId())
            .distinct()
            .toList();

        List<Long> visibleIds = orderRepository.findVisibleOrderIdsForSeller(sellerMemberId, orderIds);
        Set<Long> visibleIdSet = new HashSet<>(visibleIds);

        if (visibleIdSet.size() != orderIds.size()) {
            throw new AccessDeniedException("접근할 수 없는 주문이 포함되어 있습니다.");
        }

        for (var item : request.getItems()) {
            validateManageableStatus(item.getStatus());

            Order order = orderRepository.findById(item.getOrderId())
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다. id=" + item.getOrderId()));

            order.setStatus(item.getStatus());
        }
    }

    @Transactional
    public void updateSellerOrderStatus(Long sellerMemberId, Long orderId, OrderStatus status) {
        validateSeller(sellerMemberId);
        validateManageableStatus(status);

        Order order = findVisibleOrderOrThrow(sellerMemberId, orderId);
        order.setStatus(status);
    }

    private Specification<Order> buildSpecification(Long sellerMemberId, SellerOrderSearchCondition condition) {
        return (root, query, cb) -> {
            query.distinct(true);

            Join<Order, Member> memberJoin = root.join("member", JoinType.INNER);
            Join<Member, CompanyProfile> companyJoin = memberJoin.join("companyProfile", JoinType.LEFT);

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(buildVisibleSellerPredicate(query.subquery(Long.class), root, cb, sellerMemberId));

            if (condition.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                    root.get("createdAt"),
                    condition.getFromDate().atStartOfDay()
                ));
            }

            if (condition.getToDate() != null) {
                predicates.add(cb.lessThan(
                    root.get("createdAt"),
                    condition.getToDate().plusDays(1).atStartOfDay()
                ));
            }

            if (condition.getDealerTypes() != null && !condition.getDealerTypes().isEmpty()) {
                predicates.add(memberJoin.get("dealerType").in(condition.getDealerTypes()));
            }

            if (condition.getStatuses() != null && !condition.getStatuses().isEmpty()) {
                predicates.add(root.get("status").in(condition.getStatuses()));
            }

            if (condition.getPaymentMethods() != null && !condition.getPaymentMethods().isEmpty()) {
                predicates.add(root.get("paymentMethod").in(condition.getPaymentMethods()));
            }

            if (condition.getShippingMethods() != null && !condition.getShippingMethods().isEmpty()) {
                predicates.add(root.get("shippingMethod").in(condition.getShippingMethods()));
            }

            if (condition.getShippingPayTypes() != null && !condition.getShippingPayTypes().isEmpty()) {
                predicates.add(root.get("shippingPayType").in(condition.getShippingPayTypes()));
            }

            if (StringUtils.hasText(condition.getKeyword()) && condition.getKeywordType() != null) {
                String keyword = "%" + condition.getKeyword().trim().toLowerCase() + "%";

                switch (condition.getKeywordType()) {
                    case MEMBER_NAME -> predicates.add(likeIgnoreCase(cb, memberJoin.get("name"), keyword));
                    case MEMBER_USERNAME -> predicates.add(likeIgnoreCase(cb, memberJoin.get("username"), keyword));
                    case COMPANY_NAME -> predicates.add(likeIgnoreCase(cb, companyJoin.get("companyName"), keyword));
                    case CONTACT -> predicates.add(cb.or(
                        likeIgnoreCase(cb, root.get("ordererPhone"), keyword),
                        likeIgnoreCase(cb, memberJoin.get("mobile"), keyword),
                        likeIgnoreCase(cb, memberJoin.get("tel"), keyword)
                    ));
                    case EMAIL -> predicates.add(likeIgnoreCase(cb, memberJoin.get("email"), keyword));
                    default -> {
                    }
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Predicate buildVisibleSellerPredicate(Subquery<Long> subquery,
                                                  Root<Order> orderRoot,
                                                  CriteriaBuilder cb,
                                                  Long sellerMemberId) {
        Root<OrderItem> itemRoot = subquery.from(OrderItem.class);
        Join<OrderItem, DealerProduct> dealerProductJoin = itemRoot.join("dealerProduct", JoinType.LEFT);
        Join<DealerProduct, SellerDealerProfile> sellerProfileJoin = dealerProductJoin.join("sellerDealerProfile", JoinType.LEFT);
        Join<SellerDealerProfile, Member> sellerMemberJoin = sellerProfileJoin.join("member", JoinType.LEFT);

        subquery.select(cb.literal(1L));
        subquery.where(
            cb.equal(itemRoot.get("order"), orderRoot),
            cb.equal(itemRoot.get("itemProductType"), OrderItemProductType.DEALER),
            cb.equal(sellerMemberJoin.get("id"), sellerMemberId)
        );

        return cb.exists(subquery);
    }

    private Predicate likeIgnoreCase(CriteriaBuilder cb, Path<String> path, String keyword) {
        return cb.like(cb.lower(cb.coalesce(path, "")), keyword);
    }

    private Sort buildSort(String sortField, String sortDir) {
        String resolvedSortField = sortField == null ? "orderedAt" : sortField;
        String resolvedSortDir = normalizeSortDir(sortDir);

        String property = switch (resolvedSortField) {
            case "ordererName" -> "member.name";
            case "contact" -> "ordererPhone";
            case "email" -> "member.email";
            case "status" -> "status";
            case "orderedAt" -> "createdAt";
            default -> "createdAt";
        };

        Sort.Direction direction = "asc".equalsIgnoreCase(resolvedSortDir)
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        return Sort.by(new Sort.Order(direction, property));
    }

    private int normalizePageSize(Integer size) {
        if (size == null) {
            return 10;
        }
        return switch (size) {
            case 10, 30, 50, 100 -> size;
            default -> 10;
        };
    }

    private String normalizeSortDir(String sortDir) {
        return "asc".equalsIgnoreCase(sortDir) ? "asc" : "desc";
    }

    private void validateSeller(Long sellerMemberId) {
        if (sellerMemberId == null) {
            throw new AccessDeniedException("로그인 정보가 없습니다.");
        }

        sellerDealerProfileRepository.findByMemberId(sellerMemberId)
            .orElseThrow(() -> new AccessDeniedException("판매딜러 권한이 없습니다."));
    }

    private void validateManageableStatus(OrderStatus status) {
        if (!MANAGEABLE_STATUSES.contains(status)) {
            throw new IllegalArgumentException("판매딜러가 변경할 수 없는 주문상태입니다: " + status);
        }
    }

    private Order findVisibleOrderOrThrow(Long sellerMemberId, Long orderId) {
        orderRepository.findVisibleOrderIdForSeller(sellerMemberId, orderId)
            .orElseThrow(() -> new AccessDeniedException("해당 주문에 접근할 수 없습니다."));

        return orderRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("주문이 존재하지 않습니다. id=" + orderId));
    }

    private List<OrderItem> deduplicateVisibleOrderItems(List<OrderItem> rawItems) {
        if (rawItems == null || rawItems.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, OrderItem> distinctMap = new LinkedHashMap<>();
        List<OrderItem> noIdItems = new ArrayList<>();

        for (OrderItem item : rawItems) {
            if (item == null) {
                continue;
            }

            if (item.getId() == null) {
                noIdItems.add(item);
                continue;
            }

            distinctMap.putIfAbsent(item.getId(), item);
        }

        List<OrderItem> result = new ArrayList<>(distinctMap.values());
        result.addAll(noIdItems);
        result.sort(Comparator.comparing(item -> item.getId() == null ? Long.MAX_VALUE : item.getId()));

        return result;
    }

    private SellerOrderListItemDto toListItemDto(Order order) {
        Member member = order.getMember();
        CompanyProfile companyProfile = member != null ? member.getCompanyProfile() : null;

        String companyName = companyProfile != null && StringUtils.hasText(companyProfile.getCompanyName())
            ? companyProfile.getCompanyName()
            : "-";

        String shopName = "-";
        if (member != null
                && member.getDealerType() == DealerType.SELLER
                && member.getSellerDealerProfile() != null
                && StringUtils.hasText(member.getSellerDealerProfile().getShopName())) {
            shopName = member.getSellerDealerProfile().getShopName();
        }

        DealerType dealerType = member != null ? member.getDealerType() : null;

        return SellerOrderListItemDto.builder()
            .id(order.getId())
            .orderNo(order.getOrderNo())
            .ordererName(resolveOrdererName(order))
            .contact(resolveContact(order))
            .email(member != null ? nullSafe(member.getEmail()) : "-")
            .companyName(companyName)
            .shopName(shopName)
            .dealerType(dealerType)
            .dealerTypeLabel(getDealerTypeLabel(dealerType))
            .orderedAt(order.getCreatedAt())
            .status(order.getStatus())
            .statusLabel(getOrderStatusLabel(order.getStatus()))
            .build();
    }

    private SellerOrderDetailItemDto toDetailItemDto(OrderItem item) {
        return SellerOrderDetailItemDto.builder()
            .orderItemId(item.getId())
            .dealerProductId(item.getDealerProduct() != null ? item.getDealerProduct().getId() : null)
            .productName(nullSafe(item.getProductName()))
            .productImageUrl(nullSafe(item.getProductImageUrl()))
            .optionGroupName(nullSafe(item.getOptionGroupName()))
            .optionName(nullSafe(item.getOptionName()))
            .optionCode(nullSafe(item.getOptionCode()))
            .unitText(nullSafe(item.getUnitText()))
            .unitPrice(item.getUnitPrice() != null ? item.getUnitPrice() : 0L)
            .quantity(item.getQuantity() != null ? item.getQuantity() : 0)
            .linePrice(item.getLinePrice() != null ? item.getLinePrice() : 0L)
            .productDetailUrl(item.getDealerProduct() != null
                ? "/dealerProductDetail/" + item.getDealerProduct().getId()
                : null)
            .build();
    }

    private String resolveOrdererName(Order order) {
        if (StringUtils.hasText(order.getOrdererName())) {
            return order.getOrdererName();
        }
        if (order.getMember() != null && StringUtils.hasText(order.getMember().getName())) {
            return order.getMember().getName();
        }
        return "-";
    }

    private String resolveContact(Order order) {
        if (StringUtils.hasText(order.getOrdererPhone())) {
            return order.getOrdererPhone();
        }
        if (order.getMember() != null) {
            if (StringUtils.hasText(order.getMember().getMobile())) {
                return order.getMember().getMobile();
            }
            if (StringUtils.hasText(order.getMember().getTel())) {
                return order.getMember().getTel();
            }
        }
        return "-";
    }

    private String nullSafe(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    private String getDealerTypeLabel(DealerType dealerType) {
        if (dealerType == null) {
            return "-";
        }
        return switch (dealerType) {
            case NONE -> "일반소비자";
            case BUYER -> "기업회원";
            case SELLER -> "판매딜러";
        };
    }

    private String getOrderStatusLabel(OrderStatus status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case ORDER_COMPLETED -> "주문완료";
            case PRODUCT_PREPARING -> "상품준비중";
            case CANCEL_FINISHED -> "취소완료";
            case DELIVERING -> "배송중";
            case PAYMENT_ERROR -> "결제에러";
        };
    }

    private String getPaymentMethodLabel(PaymentMethod method) {
        if (method == null) {
            return "-";
        }
        return switch (method) {
            case ACCOUNT_TRANSFER -> "계좌이체";
            case CREDIT_CARD -> "신용카드";
            case PSYS -> "PSYS";
        };
    }

    private String getShippingMethodLabel(ShippingMethod method) {
        if (method == null) {
            return "-";
        }
        return switch (method) {
            case PARCEL -> "택배배송";
            case POST -> "우편배송";
            case QUICK -> "퀵/당일";
        };
    }

    private String getShippingPayTypeLabel(ShippingPayType type) {
        if (type == null) {
            return "-";
        }
        return switch (type) {
            case PREPAID -> "선불";
            case COLLECT -> "착불";
        };
    }
}