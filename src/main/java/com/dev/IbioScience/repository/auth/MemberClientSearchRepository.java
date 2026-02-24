package com.dev.IbioScience.repository.auth;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.dev.IbioScience.dto.admin.client.ClientListRowDto;
import com.dev.IbioScience.dto.admin.client.ClientSearchCondition;

public interface MemberClientSearchRepository {
    Page<ClientListRowDto> searchClients(ClientSearchCondition cond, Pageable pageable, String sortKey, String sortDir);
}