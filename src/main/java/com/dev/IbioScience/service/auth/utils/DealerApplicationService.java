package com.dev.IbioScience.service.auth.utils;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.enums.auth.CustomerType;
import com.dev.IbioScience.enums.auth.DealerApplicationStatus;
import com.dev.IbioScience.enums.auth.DealerType;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.utils.DealerApplyRequest;
import com.dev.IbioScience.model.auth.utils.DealerConversionApplication;
import com.dev.IbioScience.repository.auth.MemberRepository;
import com.dev.IbioScience.repository.auth.utils.DealerConversionApplicationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DealerApplicationService {

	private final MemberRepository memberRepository;
	private final DealerConversionApplicationRepository applicationRepository;

	/**
	 * 딜러 전환 신청 - 본인 요청만 허용 - 자격 검증 - 동일 toType 중복 대기 방지 - SELLER 신청 시 기존 BUYER
	 * PENDING 은 EXPIRED 로 종료
	 */
	@Transactional
	public DealerConversionApplication apply(Long principalMemberId, DealerApplyRequest req) {
		if (principalMemberId == null || !principalMemberId.equals(req.getMemberId())) {
			throw new IllegalArgumentException("본인 계정으로만 신청할 수 있습니다.");
		}

		Member m = memberRepository.findById(req.getMemberId())
				.orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

		DealerType fromDealer = m.getDealerType(); // NONE/BUYER/SELLER
		CustomerType fromCustomer = m.getCustomerType(); // PERSONAL/BUSINESS/STAFF

		// 자격 검증
		switch (req.getTargetDealerType()) {
		case BUYER -> {
			// 개인/법인 일반회원만 가능, 기존 딜러면 불가
			if (!(fromCustomer == CustomerType.PERSONAL || fromCustomer == CustomerType.BUSINESS)) {
				throw new IllegalArgumentException("구매딜러는 사업자 회원만 가능합니다.");
			}
			if (fromDealer != DealerType.NONE) {
				throw new IllegalArgumentException("이미 딜러 권한이 존재합니다.");
			}
		}
		case SELLER -> {
			// 법인 회원만 가능, 이미 SELLER면 불가
			if (fromCustomer != CustomerType.BUSINESS) {
				throw new IllegalArgumentException("판매딜러 신청은 사업자 회원만 가능합니다.");
			}
			if (fromDealer == DealerType.SELLER) {
				throw new IllegalArgumentException("이미 판매딜러입니다.");
			}
		}
		default -> throw new IllegalArgumentException("잘못된 전환 타입입니다.");
		}

		// 동일 toType PENDING 중복 방지
		boolean existsPendingSameToType = applicationRepository.existsByApplicant_IdAndStatusAndToDealerType(m.getId(),
				DealerApplicationStatus.PENDING, req.getTargetDealerType());
		if (existsPendingSameToType) {
			throw new IllegalArgumentException("이미 같은 전환 신청이 대기 중입니다.");
		}

		// 추가 규칙: SELLER 신청이면 기존 BUYER PENDING 은 EXPIRED 처리
		if (req.getTargetDealerType() == DealerType.SELLER) {
			List<DealerConversionApplication> buyerPendings = applicationRepository
					.findByApplicant_IdAndStatusAndToDealerType(m.getId(), DealerApplicationStatus.PENDING,
							DealerType.BUYER);

			if (!buyerPendings.isEmpty()) {
				LocalDateTime now = LocalDateTime.now();
				for (DealerConversionApplication p : buyerPendings) {
					p.setStatus(DealerApplicationStatus.EXPIRED);
					p.setProcessedAt(now); // BaseTimeEntity 에 updatedAt 이 있다면
					p.setExpiredAt(now); // 없다면 아래 엔티티 확장 참고
				}
				applicationRepository.saveAll(buyerPendings);
			}
		}

		DealerConversionApplication app = DealerConversionApplication.builder().applicant(m).fromDealerType(fromDealer)
				.fromCustomerType(fromCustomer).toDealerType(req.getTargetDealerType())
				.status(DealerApplicationStatus.PENDING).note(req.getNote()).requestedAt(LocalDateTime.now()).build();

		return applicationRepository.save(app);
	}
}