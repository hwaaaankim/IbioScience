package com.dev.IbioScience.dto.board.qna;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.dev.IbioScience.model.board.qna.Qna;

import jakarta.persistence.criteria.Predicate;

public class QnaSpecs {

    public static Specification<Qna> search(String title, LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (title != null && !title.trim().isEmpty()) {
                predicates.add(cb.like(root.get("title"), "%" + title.trim() + "%"));
            }

            // 작성일(createdAt) 기준 필터
            if (from != null) {
                LocalDateTime fromDt = from.atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDt));
            }
            if (to != null) {
                // to의 23:59:59.999999
                LocalDateTime toDt = to.plusDays(1).atStartOfDay().minusNanos(1);
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDt));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}