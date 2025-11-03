package com.dev.IbioScience.utils.product;

import java.time.LocalDate;
import java.util.Objects;

import org.springframework.data.jpa.domain.Specification;

import com.dev.IbioScience.enums.product.DisplayStatus;
import com.dev.IbioScience.enums.product.SaleStatus;
import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.relation.SmallProductCategory;

import jakarta.persistence.criteria.Join;

public class ProductSpecifications {

    public static Specification<Product> hasLargeId(Long largeId) {
        return (root, query, cb) -> {
            if (largeId == null) return cb.conjunction();
            // tb_category_medium 과 tb_small_product_category 구조를 고려해 medium을 경유
            Join<Product, SmallProductCategory> spc = root.join("id"); // 불가: 직접 조인 불가
            // ==> 구조가 복잡하므로 여기서는 안전하게 no-op. 실제 medium/small 필터는 서비스에서 별도 조회 후 IN(id) 필터로 적용.
            return cb.conjunction();
        };
    }

    public static Specification<Product> hasBrandId(Long brandId) {
        return (root, query, cb) -> (brandId == null) ? cb.conjunction()
                : cb.equal(root.get("brand").get("id"), brandId);
    }

    public static Specification<Product> hasDisplayStatus(DisplayStatus status) {
        return (root, query, cb) -> (status == null) ? cb.conjunction()
                : cb.equal(root.get("displayStatus"), status);
    }

    public static Specification<Product> hasSaleStatus(SaleStatus status) {
        return (root, query, cb) -> (status == null) ? cb.conjunction()
                : cb.equal(root.get("saleStatus"), status);
    }

    public static Specification<Product> nameOrCodeContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.trim() + "%";
            return cb.or(
                    cb.like(root.get("name"), like),
                    cb.like(root.get("code"), like)
            );
        };
    }

    public static Specification<Product> priceBetween(Integer min, Integer max) {
        return (root, query, cb) -> {
            if (Objects.isNull(min) && Objects.isNull(max)) return cb.conjunction();
            if (Objects.isNull(min)) return cb.lessThanOrEqualTo(root.get("salePrice"), max);
            if (Objects.isNull(max)) return cb.greaterThanOrEqualTo(root.get("salePrice"), min);
            return cb.between(root.get("salePrice"), min, max);
        };
    }

    public static Specification<Product> validOn(LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) return cb.conjunction();
            return cb.and(
                cb.or(root.get("validFrom").isNull(), cb.lessThanOrEqualTo(root.get("validFrom"), date)),
                cb.or(root.get("validTo").isNull(), cb.greaterThanOrEqualTo(root.get("validTo"), date))
            );
        };
    }
}