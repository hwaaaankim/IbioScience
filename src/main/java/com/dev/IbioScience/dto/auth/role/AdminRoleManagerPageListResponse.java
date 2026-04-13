package com.dev.IbioScience.dto.auth.role;

import java.util.ArrayList;
import java.util.List;

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
public class AdminRoleManagerPageListResponse {

    @Builder.Default
    private List<PageRow> pages = new ArrayList<>();

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageRow {
        private Long pageId;
        private String pageCode;
        private String pageName;
        private String pageUrl;
        private String menuGroupName;
        private Integer menuOrder;
        private Integer pageOrder;
        private PermissionState master;
        private PermissionState operator;
        private PermissionState employ;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionState {
        private boolean view;
        private boolean create;
        private boolean update;
        private boolean delete;
    }
}