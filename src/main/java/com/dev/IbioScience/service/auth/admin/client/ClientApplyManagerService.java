package com.dev.IbioScience.service.auth.admin.client;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.customer.auth.ClientApplyDetailDto;
import com.dev.IbioScience.dto.customer.auth.ClientApplyRowDto;
import com.dev.IbioScience.dto.customer.auth.ClientApplySearchCondition;
import com.dev.IbioScience.repository.auth.admin.client.ClientApplyManagerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClientApplyManagerService {

    private final ClientApplyManagerRepository clientApplyManagerRepository;

    public Page<ClientApplyRowDto> searchPending(ClientApplySearchCondition cond, String sortKey, String sortDir) {
        normalize(cond);

        PageRequest pageable = PageRequest.of(cond.getPage(), cond.getSize());
        return clientApplyManagerRepository.searchPending(cond, sortKey, sortDir, pageable);
    }

    public ClientApplyDetailDto getPendingDetail(Long memberId) {
        return clientApplyManagerRepository.findPendingDetail(memberId);
    }

    @Transactional
    public int approveOne(Long memberId) {
        return clientApplyManagerRepository.approveOne(memberId);
    }

    @Transactional
    public int approveBulk(List<Long> memberIds) {
        return clientApplyManagerRepository.approveBulk(memberIds);
    }

    public void normalize(ClientApplySearchCondition cond) {
        if (cond == null) return;

        if (cond.getPage() == null || cond.getPage() < 0) cond.setPage(0);

        int size = (cond.getSize() == null) ? 10 : cond.getSize();
        if (size != 10 && size != 30 && size != 50 && size != 100) size = 10;
        cond.setSize(size);

        if (cond.getApplyType() == null) cond.setApplyType(ClientApplySearchCondition.ApplyType.ALL);
        if (cond.getSearchField() == null) cond.setSearchField(ClientApplySearchCondition.SearchField.USERNAME);
    }
}