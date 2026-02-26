package com.dev.IbioScience.service.auth.customer.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.customer.auth.transfer.AdminPageResponse;
import com.dev.IbioScience.dto.customer.auth.transfer.BulkApproveResultDto;
import com.dev.IbioScience.dto.customer.auth.transfer.CompanyTransferDetailDto;
import com.dev.IbioScience.dto.customer.auth.transfer.CompanyTransferRowDto;
import com.dev.IbioScience.dto.customer.auth.transfer.CompanyTransferSearchRequest;
import com.dev.IbioScience.enums.auth.CompanyConversionStatus;
import com.dev.IbioScience.enums.auth.CustomerType;
import com.dev.IbioScience.enums.auth.DealerType;
import com.dev.IbioScience.enums.auth.MemberDomain;
import com.dev.IbioScience.enums.auth.MemberRole;
import com.dev.IbioScience.enums.auth.MemberStatus;
import com.dev.IbioScience.enums.logging.MemberAuditAction;
import com.dev.IbioScience.model.auth.BuyerDealerProfile;
import com.dev.IbioScience.model.auth.CompanyProfile;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.embedded.Address;
import com.dev.IbioScience.model.auth.utils.CompanyConversionApplication;
import com.dev.IbioScience.repository.auth.BuyerDealerProfileRepository;
import com.dev.IbioScience.repository.auth.CompanyProfileRepository;
import com.dev.IbioScience.repository.auth.MemberRepository;
import com.dev.IbioScience.repository.auth.utils.CompanyConversionApplicationRepository;
import com.dev.IbioScience.service.logging.MemberAuditLogService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompanyConversionAdminService {

	private final EntityManager em;

	private final CompanyConversionApplicationRepository companyConversionApplicationRepository;
	private final MemberRepository memberRepository;
	private final CompanyProfileRepository companyProfileRepository;
	private final BuyerDealerProfileRepository buyerDealerProfileRepository;
	private final MemberAuditLogService memberAuditLogService;

	private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	/**
	 * ✅ PENDING 기업전환 신청 목록 검색(페이지/정렬/필터)
	 */
	@Transactional(readOnly = true)
	public AdminPageResponse<CompanyTransferRowDto> searchPending(CompanyTransferSearchRequest req) {

		int page = (req.getPage() == null || req.getPage() < 0) ? 0 : req.getPage();
		int size = normalizeSize(req.getSize());

		LocalDateTime from = parseFrom(req.getFromDate());
		LocalDateTime to = parseTo(req.getToDate());

		String searchType = emptyToNull(req.getSearchType());
		String keyword = emptyToNull(req.getKeyword());

		String sortKey = emptyToNull(req.getSortKey());
		String sortDir = emptyToNull(req.getSortDir());

		CriteriaBuilder cb = em.getCriteriaBuilder();

		// ===== content query =====
		CriteriaQuery<CompanyConversionApplication> cq = cb.createQuery(CompanyConversionApplication.class);
		Root<CompanyConversionApplication> root = cq.from(CompanyConversionApplication.class);
		Join<CompanyConversionApplication, Member> applicant = root.join("applicant", JoinType.INNER);

		List<Predicate> preds = new ArrayList<>();
		preds.add(cb.equal(root.get("status"), CompanyConversionStatus.PENDING));

		if (from != null) preds.add(cb.greaterThanOrEqualTo(root.get("requestedAt"), from));
		if (to != null) preds.add(cb.lessThanOrEqualTo(root.get("requestedAt"), to));

		if (keyword != null && searchType != null) {
			switch (searchType) {
				case "USERNAME" -> preds.add(cb.like(applicant.get("username"), "%" + keyword + "%"));
				case "MOBILE" -> preds.add(cb.like(applicant.get("mobile"), "%" + keyword + "%"));
				case "NAME" -> preds.add(cb.like(applicant.get("name"), "%" + keyword + "%"));
				default -> { /* ignore */ }
			}
		}

		cq.where(preds.toArray(new Predicate[0]));
		cq.orderBy(buildCompanySort(cb, root, applicant, sortKey, sortDir));

		TypedQuery<CompanyConversionApplication> q = em.createQuery(cq);
		q.setFirstResult(page * size);
		q.setMaxResults(size);
		List<CompanyConversionApplication> rows = q.getResultList();

		// ===== count query =====
		CriteriaQuery<Long> countQ = cb.createQuery(Long.class);
		Root<CompanyConversionApplication> countRoot = countQ.from(CompanyConversionApplication.class);
		Join<CompanyConversionApplication, Member> countApplicant = countRoot.join("applicant", JoinType.INNER);

		List<Predicate> countPreds = new ArrayList<>();
		countPreds.add(cb.equal(countRoot.get("status"), CompanyConversionStatus.PENDING));
		if (from != null) countPreds.add(cb.greaterThanOrEqualTo(countRoot.get("requestedAt"), from));
		if (to != null) countPreds.add(cb.lessThanOrEqualTo(countRoot.get("requestedAt"), to));
		if (keyword != null && searchType != null) {
			switch (searchType) {
				case "USERNAME" -> countPreds.add(cb.like(countApplicant.get("username"), "%" + keyword + "%"));
				case "MOBILE" -> countPreds.add(cb.like(countApplicant.get("mobile"), "%" + keyword + "%"));
				case "NAME" -> countPreds.add(cb.like(countApplicant.get("name"), "%" + keyword + "%"));
				default -> { /* ignore */ }
			}
		}

		countQ.select(cb.count(countRoot)).where(countPreds.toArray(new Predicate[0]));
		long total = em.createQuery(countQ).getSingleResult();
		int totalPages = (int) Math.ceil((double) total / (double) size);

		List<CompanyTransferRowDto> content = rows.stream().map(app -> CompanyTransferRowDto.builder()
				.applicationId(app.getId())
				.username(app.getApplicant().getUsername())
				.companyName(app.getCompanyName())
				.name(app.getApplicant().getName())
				.mobile(app.getApplicant().getMobile())
				.requestedAt(app.getRequestedAt() != null ? app.getRequestedAt().format(DT) : "")
				.build()
		).toList();

		return AdminPageResponse.<CompanyTransferRowDto>builder()
				.content(content)
				.page(page)
				.size(size)
				.totalElements(total)
				.totalPages(Math.max(totalPages, 1))
				.first(page <= 0)
				.last(page >= Math.max(totalPages - 1, 0))
				.build();
	}

	@Transactional(readOnly = true)
	public CompanyTransferDetailDto getDetail(Long applicationId) {
		CompanyConversionApplication app = companyConversionApplicationRepository.findById(applicationId)
				.orElseThrow(() -> new IllegalArgumentException("전환 신청을 찾을 수 없습니다."));

		Member m = app.getApplicant();

		Address addr = app.getCompanyAddress();

		return CompanyTransferDetailDto.builder()
				.applicationId(app.getId())

				.memberId(m != null ? m.getId() : null)
				.username(m != null ? m.getUsername() : null)
				.name(m != null ? m.getName() : null)
				.mobile(m != null ? m.getMobile() : null)
				.email(m != null ? m.getEmail() : null)

				.requestedAt(app.getRequestedAt() != null ? app.getRequestedAt().format(DT) : null)

				.companyName(app.getCompanyName())
				.department(app.getDepartment())
				.ceoName(app.getCeoName())
				.businessType(app.getBusinessType())
				.businessItem(app.getBusinessItem())

				.representativeTel(app.getRepresentativeTel())
				.fax(app.getFax())
				.invoiceEmail(app.getInvoiceEmail())
				.businessRegistrationNumber(app.getBusinessRegistrationNumber())

				.bizRegImageRoad(app.getBusinessRegImageRoad())

				.companyPostcode(addr != null ? addr.getPostcode() : null)
				.companyRoadAddress(addr != null ? addr.getRoadAddress() : null)
				.companyJibunAddress(addr != null ? addr.getJibunAddress() : null)
				.companyDetailAddress(addr != null ? addr.getDetailAddress() : null)

				.organizationCategory(app.getOrganizationCategory() != null ? app.getOrganizationCategory().name() : null)
				.homepageUrl(app.getHomepageUrl())

				.note(app.getNote())
				.build();
	}

	/**
	 * ✅ 단건 승인 + 승인 후 신청서 삭제(요구사항 반영)
	 */
	@Transactional
	public void approveAndDelete(Long applicationId, Long processorMemberId, String processNote) {

		if (processorMemberId == null) {
			throw new IllegalArgumentException("처리자(로그인 관리자) 정보가 없습니다.");
		}

		CompanyConversionApplication app = companyConversionApplicationRepository.findById(applicationId)
				.orElseThrow(() -> new IllegalArgumentException("전환 신청을 찾을 수 없습니다."));

		if (app.getStatus() != CompanyConversionStatus.PENDING) {
			throw new IllegalArgumentException("대기 상태(PENDING)인 신청만 승인할 수 있습니다.");
		}

		Member applicant = app.getApplicant();
		if (applicant == null || applicant.getId() == null) {
			throw new IllegalArgumentException("신청자 정보가 올바르지 않습니다.");
		}

		Member processor = memberRepository.findById(processorMemberId)
				.orElseThrow(() -> new IllegalArgumentException("처리자(관리자) 정보를 찾을 수 없습니다."));

		// 승인 시점 재검증
		if (applicant.getStatus() != MemberStatus.ACTIVE) {
			throw new IllegalArgumentException("승인 완료된 회원만 기업 전환 승인 처리가 가능합니다.");
		}
		if (applicant.getCompanyProfile() != null || applicant.getCustomerType() == CustomerType.BUSINESS) {
			throw new IllegalArgumentException("이미 기업회원이어서 승인할 수 없습니다.");
		}

		CustomerType beforeType = applicant.getCustomerType();
		Long beforeCompanyId = (applicant.getCompanyProfile() != null) ? applicant.getCompanyProfile().getId() : null;

		// 1) CompanyProfile 생성
		CompanyProfile profile = new CompanyProfile();
		profile.setCompanyName(app.getCompanyName());
		profile.setDepartment(app.getDepartment());
		profile.setCeoName(app.getCeoName());
		profile.setBusinessType(app.getBusinessType());
		profile.setBusinessItem(app.getBusinessItem());
		profile.setInvoiceEmail(app.getInvoiceEmail());
		profile.setBusinessRegistrationNumber(app.getBusinessRegistrationNumber());
		profile.setRepresentativeTel(app.getRepresentativeTel());
		profile.setFax(app.getFax());
		profile.setOrganizationCategory(app.getOrganizationCategory());
		profile.setCompanyAddress(app.getCompanyAddress());
		profile.setBusinessRegImagePath(app.getBusinessRegImagePath());
		profile.setBusinessRegImageRoad(app.getBusinessRegImageRoad());
		profile.setHomepageUrl(app.getHomepageUrl());

		profile = companyProfileRepository.save(profile);

		// 2) Member 반영(권한/등급 변경)
		applicant.setCompanyProfile(profile);
		applicant.setCustomerType(CustomerType.BUSINESS);
		applicant.setDealerType(DealerType.BUYER);

		// ✅ role도 BUYER_DEALER로 변경(보안 권한이 role 기준이면 필수)
		if (applicant.getRole() == MemberRole.USER) {
			applicant.setRole(MemberRole.BUYER_DEALER);
		}

		if (applicant.getDomain() == null) applicant.setDomain(MemberDomain.CUSTOMER);

		memberRepository.save(applicant);

		// 3) BuyerDealerProfile 보장
		ensureBuyerDealerProfileExists(applicant);

		// 4) 감사로그
		memberAuditLogService.logEvent(
				applicant,
				MemberAuditAction.PERSONAL_TO_COMPANY_CONVERSION,
				"COMPANY_CONVERSION_APPROVED(appDeleted) appId=" + app.getId() + ", companyProfileId=" + profile.getId(),
				processorMemberId
		);

		memberAuditLogService.logFieldChange(
				applicant,
				MemberAuditAction.PERSONAL_TO_COMPANY_CONVERSION,
				"customerType",
				beforeType != null ? beforeType.name() : null,
				applicant.getCustomerType() != null ? applicant.getCustomerType().name() : null,
				processorMemberId
		);

		memberAuditLogService.logFieldChange(
				applicant,
				MemberAuditAction.PERSONAL_TO_COMPANY_CONVERSION,
				"companyProfileId",
				beforeCompanyId != null ? String.valueOf(beforeCompanyId) : null,
				String.valueOf(profile.getId()),
				processorMemberId
		);

		memberAuditLogService.logFieldChange(
				applicant,
				MemberAuditAction.PERSONAL_TO_COMPANY_CONVERSION,
				"dealerType",
				DealerType.NONE.name(),
				applicant.getDealerType().name(),
				processorMemberId
		);

		// 5) ✅ 신청서 삭제(요구사항)
		companyConversionApplicationRepository.delete(app);

		// (선택) 처리자 메모를 어딘가 남겨야 한다면 AuditLog detail에 포함시키는 방식이 안전합니다.
		if (processNote != null && !processNote.isBlank()) {
			memberAuditLogService.logEvent(
					applicant,
					MemberAuditAction.PERSONAL_TO_COMPANY_CONVERSION,
					"COMPANY_CONVERSION_PROCESS_NOTE " + processNote,
					processorMemberId
			);
		}

		// processor 변수 미사용 경고 방지(추후 필요 시 app 삭제 대신 보관 정책이면 processor 저장)
		if (processor == null) {
			throw new IllegalStateException("처리자 정보 누락");
		}
	}

	/**
	 * ✅ 일괄 승인(체크된 것만) - 실패 항목은 failures로 반환
	 */
	public BulkApproveResultDto bulkApproveAndDelete(List<Long> applicationIds, Long processorMemberId, String processNote) {

		if (applicationIds == null || applicationIds.isEmpty()) {
			return BulkApproveResultDto.builder()
					.requestedCount(0).successCount(0).failCount(0)
					.build();
		}

		int success = 0;
		List<BulkApproveResultDto.FailureItem> failures = new ArrayList<>();

		for (Long id : applicationIds) {
			try {
				approveAndDelete(id, processorMemberId, processNote);
				success++;
			} catch (Exception e) {
				failures.add(BulkApproveResultDto.FailureItem.builder()
						.applicationId(id)
						.reason(e.getMessage() != null ? e.getMessage() : "처리 실패")
						.build());
			}
		}

		return BulkApproveResultDto.builder()
				.requestedCount(applicationIds.size())
				.successCount(success)
				.failCount(failures.size())
				.failures(failures)
				.build();
	}

	private void ensureBuyerDealerProfileExists(Member member) {
		if (member == null || member.getId() == null) return;

		if (member.getCustomerType() != CustomerType.BUSINESS) return;

		if (buyerDealerProfileRepository.existsByMember_Id(member.getId())) return;

		BuyerDealerProfile profile = BuyerDealerProfile.builder()
				.member(member)
				.grade(com.dev.IbioScience.enums.auth.DealerGrade.EXCEPTION)
				.customDiscountRate(null)
				.effectiveFrom(LocalDate.now())
				.build();

		buyerDealerProfileRepository.save(profile);
	}

	private static int normalizeSize(Integer size) {
		if (size == null) return 10;
		return switch (size) {
			case 10, 30, 50, 100 -> size;
			default -> 10;
		};
	}

	private static String emptyToNull(String v) {
		if (v == null) return null;
		String t = v.trim();
		return t.isEmpty() ? null : t;
	}

	private static LocalDateTime parseFrom(String fromDate) {
		String s = emptyToNull(fromDate);
		if (s == null) return null;
		return LocalDate.parse(s).atStartOfDay();
	}

	private static LocalDateTime parseTo(String toDate) {
		String s = emptyToNull(toDate);
		if (s == null) return null;
		return LocalDate.parse(s).atTime(LocalTime.of(23, 59, 59));
	}

	private static List<Order> buildCompanySort(
			CriteriaBuilder cb,
			Root<CompanyConversionApplication> root,
			Join<CompanyConversionApplication, Member> applicant,
			String sortKey,
			String sortDir) {

		boolean asc = "asc".equalsIgnoreCase(sortDir);

		String key = (sortKey == null) ? "requestedAt" : sortKey;

		jakarta.persistence.criteria.Expression<?> expr = switch (key) {
			case "username" -> applicant.get("username");
			case "companyName" -> root.get("companyName");
			case "name" -> applicant.get("name");
			case "mobile" -> applicant.get("mobile");
			case "requestedAt" -> root.get("requestedAt");
			default -> root.get("requestedAt");
		};

		Order order = asc ? cb.asc(expr) : cb.desc(expr);

		// tie-breaker
		Order order2 = cb.desc(root.get("id"));

		return List.of(order, order2);
	}
}