package com.dev.IbioScience.service.auth.admin.common;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.customer.auth.WithdrawApproveResultDto;
import com.dev.IbioScience.dto.customer.auth.WithdrawMemberDetailDto;
import com.dev.IbioScience.dto.customer.auth.WithdrawMemberRowDto;
import com.dev.IbioScience.dto.customer.auth.WithdrawSearchCondition;
import com.dev.IbioScience.enums.auth.MemberStatus;
import com.dev.IbioScience.model.auth.CompanyProfile;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.embedded.Address;
import com.dev.IbioScience.repository.auth.MemberRepository;
import com.dev.IbioScience.repository.auth.admin.client.ClientWithdrawMemberQueryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClientWithdrawManagerService {

	private final ClientWithdrawMemberQueryRepository queryRepository;
	private final MemberRepository memberRepository;

	public Page<WithdrawMemberRowDto> search(WithdrawSearchCondition cond, String sortKey, String sortDir) {

		int page = cond.getPage() == null ? 0 : Math.max(cond.getPage(), 0);
		int size = normalizeSize(cond.getSize() == null ? 10 : cond.getSize());

		PageRequest pageable = PageRequest.of(page, size);
		return queryRepository.search(cond, pageable, sortKey, sortDir);
	}

	private int normalizeSize(int size) {
		if (size == 30 || size == 50 || size == 100) return size;
		return 10;
	}

	@Transactional(readOnly = true)
	public WithdrawMemberDetailDto getDetail(Long memberId) {

		Member m = memberRepository.findById(memberId)
			.orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다. id=" + memberId));

		CompanyProfile cp = m.getCompanyProfile();
		Address ca = (cp == null ? null : cp.getCompanyAddress());

		return WithdrawMemberDetailDto.builder()
			.memberId(m.getId())
			.username(m.getUsername())
			.name(m.getName())
			.tel(m.getTel())
			.mobile(m.getMobile())
			.email(m.getEmail())
			.customerType(m.getCustomerType() == null ? null : m.getCustomerType().name())
			.dealerType(m.getDealerType() == null ? null : m.getDealerType().name())
			.status(m.getStatus() == null ? null : m.getStatus().name())
			.joinedAt(m.getJoinedAt())
			.withdrewAt(m.getWithdrewAt())
			.organizationName(m.getOrganizationName())

			.companyProfileId(cp == null ? null : cp.getId())
			.companyName(cp == null ? null : cp.getCompanyName())
			.department(cp == null ? null : cp.getDepartment())
			.ceoName(cp == null ? null : cp.getCeoName())
			.businessType(cp == null ? null : cp.getBusinessType())
			.businessItem(cp == null ? null : cp.getBusinessItem())
			.representativeTel(cp == null ? null : cp.getRepresentativeTel())
			.fax(cp == null ? null : cp.getFax())
			.invoiceEmail(cp == null ? null : cp.getInvoiceEmail())
			.businessRegistrationNumber(cp == null ? null : cp.getBusinessRegistrationNumber())
			.companyPostcode(ca == null ? null : ca.getPostcode())
			.companyRoadAddress(ca == null ? null : ca.getRoadAddress())
			.companyDetailAddress(ca == null ? null : ca.getDetailAddress())
			.organizationCategory(cp == null || cp.getOrganizationCategory() == null ? null : cp.getOrganizationCategory().name())
			.businessRegImageRoad(cp == null ? null : cp.getBusinessRegImageRoad())
			.build();
	}

	@Transactional
	public void approveOne(Long memberId) {

		Member m = memberRepository.findById(memberId)
			.orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다. id=" + memberId));

		if (m.getStatus() != MemberStatus.WITHDRAWN) {
			throw new IllegalStateException("탈퇴신청 상태가 아닙니다. 현재 상태=" + m.getStatus());
		}

		m.setStatus(MemberStatus.DELETED);

		if (m.getWithdrewAt() == null) {
			m.setWithdrewAt(LocalDateTime.now());
		}

		memberRepository.save(m);
	}

	@Transactional
	public WithdrawApproveResultDto approveBulk(List<Long> memberIds) {

		if (memberIds == null || memberIds.isEmpty()) {
			return WithdrawApproveResultDto.builder()
				.processedCount(0)
				.failedIds(List.of())
				.build();
		}

		int processed = 0;
		List<Long> failed = new ArrayList<>();

		for (Long id : memberIds) {
			try {
				approveOne(id);
				processed++;
			} catch (Exception e) {
				failed.add(id);
			}
		}

		return WithdrawApproveResultDto.builder()
			.processedCount(processed)
			.failedIds(failed)
			.build();
	}
}