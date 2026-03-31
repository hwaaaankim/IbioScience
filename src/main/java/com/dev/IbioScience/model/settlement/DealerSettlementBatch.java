package com.dev.IbioScience.model.settlement;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.dev.IbioScience.enums.settlement.SettlementBatchStatus;
import com.dev.IbioScience.model.auth.embedded.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "tb_dealer_settlement_batch",
    indexes = {
        @Index(name = "ix_settle_batch_started_at", columnList = "started_at"),
        @Index(name = "ix_settle_batch_status", columnList = "status")
    }
)
public class DealerSettlementBatch extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requested_from_date", nullable = false)
    private LocalDate requestedFromDate;

    @Column(name = "requested_to_date", nullable = false)
    private LocalDate requestedToDate;

    @Column(name = "requested_cycles_csv", length = 500)
    private String requestedCyclesCsv;

    @Column(name = "requested_bases_csv", length = 500)
    private String requestedBasesCsv;

    @Column(name = "keyword", length = 200)
    private String keyword;

    @Column(name = "requested_by_member_id")
    private Long requestedByMemberId;

    @Column(name = "requested_by_username", length = 100)
    private String requestedByUsername;

    @Column(name = "requested_by_name", length = 100)
    private String requestedByName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SettlementBatchStatus status;

    @Column(name = "target_history_count", nullable = false)
    private Integer targetHistoryCount;

    @Column(name = "created_settlement_count", nullable = false)
    private Integer createdSettlementCount;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
}