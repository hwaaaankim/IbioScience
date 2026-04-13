package com.dev.IbioScience.repository.auth.role;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.auth.role.AdminManagedPage;

public interface AdminManagedPageRepository extends JpaRepository<AdminManagedPage, Long> {

    List<AdminManagedPage> findAllByUseYnTrueOrderByMenuOrderAscPageOrderAscIdAsc();

    Optional<AdminManagedPage> findByPageCode(String pageCode);

    Optional<AdminManagedPage> findByPageUrl(String pageUrl);
}