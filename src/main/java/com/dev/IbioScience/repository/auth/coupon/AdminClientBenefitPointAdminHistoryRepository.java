package com.dev.IbioScience.repository.auth.coupon;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.model.product.coupon.MemberPointAdminHistory;

public interface AdminClientBenefitPointAdminHistoryRepository extends JpaRepository<MemberPointAdminHistory, Long> {

    @Query(value = """
        SELECT hist.change_type AS changeType,
               hist.amount AS amount,
               hist.order_no AS orderNo,
               hist.source_text AS sourceText,
               hist.occurred_at AS occurredAt
          FROM (
                SELECT CASE
                           WHEN h.action_type = 'GRANT' THEN 'PLUS'
                           ELSE 'MINUS'
                       END AS change_type,
                       h.amount AS amount,
                       NULL AS order_no,
                       CASE
                           WHEN h.action_type = 'GRANT' THEN '관리자 부여'
                           ELSE '관리자 차감'
                       END AS source_text,
                       h.created_at AS occurred_at
                  FROM tb_member_point_admin_history h
                 WHERE h.member_id = :memberId
                   AND (:fromAt IS NULL OR h.created_at >= :fromAt)
                   AND (:toAt IS NULL OR h.created_at < :toAt)

                UNION ALL

                SELECT 'MINUS' AS change_type,
                       o.point_used AS amount,
                       o.order_no AS order_no,
                       '주문 사용' AS source_text,
                       COALESCE(o.paid_at, o.created_at) AS occurred_at
                  FROM tb_order o
                 WHERE o.member_id = :memberId
                   AND o.point_used > 0
                   AND (:fromAt IS NULL OR COALESCE(o.paid_at, o.created_at) >= :fromAt)
                   AND (:toAt IS NULL OR COALESCE(o.paid_at, o.created_at) < :toAt)

                UNION ALL

                SELECT 'PLUS' AS change_type,
                       o.expect_point AS amount,
                       o.order_no AS order_no,
                       '주문 적립' AS source_text,
                       COALESCE(o.paid_at, o.created_at) AS occurred_at
                  FROM tb_order o
                 WHERE o.member_id = :memberId
                   AND o.expect_point > 0
                   AND o.status NOT IN ('PAYMENT_ERROR', 'CANCEL_FINISHED')
                   AND (:fromAt IS NULL OR COALESCE(o.paid_at, o.created_at) >= :fromAt)
                   AND (:toAt IS NULL OR COALESCE(o.paid_at, o.created_at) < :toAt)
          ) hist
         ORDER BY hist.occurred_at DESC
        """,
        countQuery = """
        SELECT COUNT(*)
          FROM (
                SELECT 1
                  FROM tb_member_point_admin_history h
                 WHERE h.member_id = :memberId
                   AND (:fromAt IS NULL OR h.created_at >= :fromAt)
                   AND (:toAt IS NULL OR h.created_at < :toAt)

                UNION ALL

                SELECT 1
                  FROM tb_order o
                 WHERE o.member_id = :memberId
                   AND o.point_used > 0
                   AND (:fromAt IS NULL OR COALESCE(o.paid_at, o.created_at) >= :fromAt)
                   AND (:toAt IS NULL OR COALESCE(o.paid_at, o.created_at) < :toAt)

                UNION ALL

                SELECT 1
                  FROM tb_order o
                 WHERE o.member_id = :memberId
                   AND o.expect_point > 0
                   AND o.status NOT IN ('PAYMENT_ERROR', 'CANCEL_FINISHED')
                   AND (:fromAt IS NULL OR COALESCE(o.paid_at, o.created_at) >= :fromAt)
                   AND (:toAt IS NULL OR COALESCE(o.paid_at, o.created_at) < :toAt)
          ) cnt
        """,
        nativeQuery = true)
    Page<AdminClientBenefitPointHistoryRowProjection> searchPointHistories(
            @Param("memberId") Long memberId,
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toAt") LocalDateTime toAt,
            Pageable pageable
    );
}