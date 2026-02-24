package com.dev.IbioScience.dto.admin.client;

import java.time.LocalDate;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.dev.IbioScience.enums.auth.DealerGrade;
import com.dev.IbioScience.enums.auth.MemberStatus;

public class ClientSearchCondition {

    public enum MemberType {
        GENERAL, COMPANY_BUYER, COMPANY_SELLER
    }

    public enum DateField {
        JOINED, WITHDREW
    }

    // paging
    private Integer page = 0;
    private Integer size = 10;

    // text search
    private String searchField; // mobile/username/name/tel/email/bizNo
    private String keyword;

    // member classification
    private java.util.Set<MemberType> memberTypes; // null or empty => 전체

    // dealer grade (기업 선택 시 의미)
    private DealerGrade grade; // null => 전체

    // status
    private MemberStatus status; // null => 전체

    // date range
    private DateField dateField = DateField.JOINED;
    private LocalDate from;
    private LocalDate to;

    // ✅ 정렬 (추가)
    private String sortKey;
    private String sortDir;

    // ===== select options helper =====
    public static java.util.List<SearchFieldOption> searchFieldOptions() {
        return java.util.List.of(
                new SearchFieldOption("mobile", "휴대폰번호"),
                new SearchFieldOption("username", "아이디"),
                new SearchFieldOption("name", "이름"),
                new SearchFieldOption("tel", "유선전화"),
                new SearchFieldOption("email", "이메일"),
                new SearchFieldOption("bizNo", "사업자등록번호")
        );
    }

    public static class SearchFieldOption {
        private final String value;
        private final String label;

        public SearchFieldOption(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }
    }

    // ===== thymeleaf pagination param builder =====
    public MultiValueMap<String, String> toQueryParams(int targetPage) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("page", String.valueOf(Math.max(targetPage, 0)));
        map.add("size", String.valueOf(size == null ? 10 : size));

        if (searchField != null && !searchField.isBlank()) {
            map.add("searchField", searchField);
        }
        if (keyword != null && !keyword.isBlank()) {
            map.add("keyword", keyword);
        }

        if (memberTypes != null && !memberTypes.isEmpty()) {
            for (MemberType t : memberTypes) {
                map.add("memberTypes", t.name());
            }
        }

        if (grade != null) {
            map.add("grade", grade.name());
        }
        if (status != null) {
            map.add("status", status.name());
        }

        if (dateField != null) {
            map.add("dateField", dateField.name());
        }
        if (from != null) {
            map.add("from", from.toString());
        }
        if (to != null) {
            map.add("to", to.toString());
        }

        // ✅ 정렬 포함 (추가)
        if (sortKey != null && !sortKey.isBlank()) {
            map.add("sortKey", sortKey);
        }
        if (sortDir != null && !sortDir.isBlank()) {
            map.add("sortDir", sortDir);
        }

        return map;
    }

    // ===== getters/setters =====
    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getSearchField() {
        return searchField;
    }

    public void setSearchField(String searchField) {
        this.searchField = searchField;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public java.util.Set<MemberType> getMemberTypes() {
        return memberTypes;
    }

    public void setMemberTypes(java.util.Set<MemberType> memberTypes) {
        this.memberTypes = memberTypes;
    }

    public DealerGrade getGrade() {
        return grade;
    }

    public void setGrade(DealerGrade grade) {
        this.grade = grade;
    }

    public MemberStatus getStatus() {
        return status;
    }

    public void setStatus(MemberStatus status) {
        this.status = status;
    }

    public DateField getDateField() {
        return dateField;
    }

    public void setDateField(DateField dateField) {
        this.dateField = dateField;
    }

    public LocalDate getFrom() {
        return from;
    }

    public void setFrom(LocalDate from) {
        this.from = from;
    }

    public LocalDate getTo() {
        return to;
    }

    public void setTo(LocalDate to) {
        this.to = to;
    }

    public String getSortKey() {
        return sortKey;
    }

    public void setSortKey(String sortKey) {
        this.sortKey = sortKey;
    }

    public String getSortDir() {
        return sortDir;
    }

    public void setSortDir(String sortDir) {
        this.sortDir = sortDir;
    }
}