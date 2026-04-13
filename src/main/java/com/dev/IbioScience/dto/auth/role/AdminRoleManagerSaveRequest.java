package com.dev.IbioScience.dto.auth.role;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminRoleManagerSaveRequest {

    private List<PagePermissionItem> pages = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagePermissionItem {
        private Long pageId;
        private PermissionState master;
        private PermissionState operator;
        private PermissionState employ;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionState {
        private boolean view;
        private boolean create;
        private boolean update;
        private boolean delete;
    }
}