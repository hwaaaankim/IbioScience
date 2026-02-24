package com.dev.IbioScience.repository.logging;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.logging.VisitDaily;

public interface VisitDailyRepository extends JpaRepository<VisitDaily, LocalDate> {
}