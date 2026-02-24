package com.dev.IbioScience.service.logging;

import java.time.LocalDate;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VisitCounterService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void recordVisit(LocalDate visitDate, String fingerprintHash) {
        // 1) PV 무조건 +1 (해당 날짜 row 없으면 생성)
        upsertPv(visitDate);

        // 2) UV는 하루 1회만 (fingerprint 테이블 insert 성공시에만 +1)
        boolean isFirstToday = insertFingerprintIfFirst(visitDate, fingerprintHash);
        if (isFirstToday) {
            upsertUv(visitDate);
        }
    }

    private void upsertPv(LocalDate visitDate) {
        String sql = """
            INSERT INTO tb_visit_daily (visit_date, page_view_count, unique_count)
            VALUES (?, 1, 0)
            ON DUPLICATE KEY UPDATE page_view_count = page_view_count + 1
            """;
        jdbcTemplate.update(sql, visitDate);
    }

    private void upsertUv(LocalDate visitDate) {
        String sql = """
            INSERT INTO tb_visit_daily (visit_date, page_view_count, unique_count)
            VALUES (?, 0, 1)
            ON DUPLICATE KEY UPDATE unique_count = unique_count + 1
            """;
        jdbcTemplate.update(sql, visitDate);
    }

    private boolean insertFingerprintIfFirst(LocalDate visitDate, String fingerprintHash) {
        // UNIQUE(visit_date, fingerprint_hash) 덕분에 중복이면 0 rows
        String sql = """
            INSERT IGNORE INTO tb_visit_fingerprint (visit_date, fingerprint_hash)
            VALUES (?, ?)
            """;
        int affected = jdbcTemplate.update(sql, visitDate, fingerprintHash);
        return affected == 1;
    }
}