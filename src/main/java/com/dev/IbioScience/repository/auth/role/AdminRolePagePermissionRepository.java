package com.dev.IbioScience.repository.auth.role;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.enums.auth.MemberRole;
import com.dev.IbioScience.model.auth.role.AdminManagedPage;
import com.dev.IbioScience.model.auth.role.AdminRolePagePermission;

public interface AdminRolePagePermissionRepository extends JpaRepository<AdminRolePagePermission, Long> {

    @EntityGraph(attributePaths = {"page"})
    List<AdminRolePagePermission> findAllByPageIn(List<AdminManagedPage> pages);

    Optional<AdminRolePagePermission> findByPageAndRole(AdminManagedPage page, MemberRole role);
}