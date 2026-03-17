package com.dev.IbioScience.dto.estimate.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.dev.IbioScience.enums.estimate.admin.AdminEstimateDateType;
import com.dev.IbioScience.enums.estimate.admin.AdminEstimateMemberSearchType;
import com.dev.IbioScience.enums.estimate.admin.AdminEstimateProgressFilter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminEstimateListSearchRequest {

    private Integer page = 1;
    private Integer size = 10;

    private AdminEstimateMemberSearchType searchType = AdminEstimateMemberSearchType.USER_ID;
    private String keyword;

    private AdminEstimateDateType dateType = AdminEstimateDateType.REQUESTED_AT;
    private LocalDate fromDate;
    private LocalDate toDate;

    private List<AdminEstimateProgressFilter> states = new ArrayList<>();

    public int getPageValue() {
        return (page == null || page < 1) ? 1 : page;
    }

    public int getSizeValue() {
        if (size == null) {
            return 10;
        }

        if (size == 10 || size == 30 || size == 50 || size == 100) {
            return size;
        }

        return 10;
    }

    public int getOffset() {
        return (getPageValue() - 1) * getSizeValue();
    }

    public String getNormalizedKeyword() {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    public LocalDateTime getFromDateTime() {
        return fromDate == null ? null : fromDate.atStartOfDay();
    }

    public LocalDateTime getToDateTime() {
        return toDate == null ? null : toDate.atTime(LocalTime.MAX);
    }

    public List<AdminEstimateProgressFilter> getNormalizedStates() {
        if (CollectionUtils.isEmpty(states)) {
            return new ArrayList<>();
        }

        Set<AdminEstimateProgressFilter> unique = new LinkedHashSet<>(states);
        return new ArrayList<>(unique);
    }
}