package com.dev.IbioScience.repository.auth.role;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.enums.auth.role.AdminPermissionAction;
import com.dev.IbioScience.model.auth.role.AdminManagedPage;
import com.dev.IbioScience.model.auth.role.AdminManagedPageEndpoint;

public interface AdminManagedPageEndpointRepository extends JpaRepository<AdminManagedPageEndpoint, Long> {

    @EntityGraph(attributePaths = {"page"})
    List<AdminManagedPageEndpoint> findAllByUseYnTrueOrderByIdAsc();

    Optional<AdminManagedPageEndpoint> findByPageAndHttpMethodAndPathPatternAndAction(
            AdminManagedPage page,
            String httpMethod,
            String pathPattern,
            AdminPermissionAction action
    );
}