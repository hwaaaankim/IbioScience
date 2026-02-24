package com.dev.IbioScience.model.logging;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "tb_visit_fingerprint",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_visit_date_fingerprint", columnNames = {"visit_date", "fingerprint_hash"})
    },
    indexes = {
        @Index(name = "idx_visit_date", columnList = "visit_date")
    }
)
@Getter
@NoArgsConstructor
public class VisitFingerprint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "fingerprint_hash", nullable = false, length = 64)
    private String fingerprintHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}