package com.dev.IbioScience.model.logging;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_visit_daily")
@Getter
@NoArgsConstructor
public class VisitDaily {

    @Id
    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "page_view_count", nullable = false)
    private long pageViewCount;

    @Column(name = "unique_count", nullable = false)
    private long uniqueCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}