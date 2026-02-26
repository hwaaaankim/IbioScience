package com.dev.IbioScience.service.front.myPgae;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.customer.auth.MyPageActionStatusData;
import com.dev.IbioScience.enums.auth.CompanyConversionStatus;
import com.dev.IbioScience.enums.auth.CustomerType;
import com.dev.IbioScience.enums.auth.DealerApplicationStatus;
import com.dev.IbioScience.enums.auth.DealerType;
import com.dev.IbioScience.enums.auth.MemberStatus;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.PrincipalDetails;
import com.dev.IbioScience.model.auth.utils.CompanyConversionApplication;
import com.dev.IbioScience.model.auth.utils.DealerConversionApplication;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class MyPageActionStatusService {

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public MyPageActionStatusData getActionStatusData(PrincipalDetails principal) {

        if (principal == null || principal.getMember() == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        Member member = principal.getMember();

        boolean withdrawApplied = (member.getStatus() == MemberStatus.WITHDRAWN);

        // 직원은 요구사항에 명시가 없으므로 액션 최소화(탈퇴만 정책상 허용)
        if (member.getCustomerType() == CustomerType.STAFF) {
            return MyPageActionStatusData.builder()
                    .mode("NONE")
                    .withdrawApplied(withdrawApplied)
                    .companyConversionPending(false)
                    .sellerConversionPending(false)
                    .showWithdrawButton(!withdrawApplied)
                    .showCompanyConvertButton(false)
                    .showSellerApplyButton(false)
                    .build();
        }

        boolean isCompany = (member.getCompanyProfile() != null);
        DealerType dealerType = (member.getDealerType() == null ? DealerType.NONE : member.getDealerType());

        boolean companyConversionPending = existsCompanyConversionPending(member.getId());
        boolean sellerConversionPending = existsSellerConversionPending(member.getId());

        String mode;
        boolean showWithdrawButton = !withdrawApplied; // WITHDRAWN이면 버튼 대신 메시지
        boolean showCompanyConvertButton = false;
        boolean showSellerApplyButton = false;

        // 1) 일반회원(기업 아님)
        if (!isCompany) {
            mode = "PERSONAL";
            showCompanyConvertButton = !companyConversionPending;
            showSellerApplyButton = false;
        }
        // 2) 기업회원 + SELLER 아님
        else if (dealerType != DealerType.SELLER) {
            mode = "COMPANY_NOT_SELLER";
            showCompanyConvertButton = false;
            showSellerApplyButton = !sellerConversionPending;
        }
        // 3) SELLER
        else {
            mode = "COMPANY_SELLER";
            showCompanyConvertButton = false;
            showSellerApplyButton = false;
        }

        return MyPageActionStatusData.builder()
                .mode(mode)
                .withdrawApplied(withdrawApplied)
                .companyConversionPending(companyConversionPending)
                .sellerConversionPending(sellerConversionPending)
                .showWithdrawButton(showWithdrawButton)
                .showCompanyConvertButton(showCompanyConvertButton)
                .showSellerApplyButton(showSellerApplyButton)
                .build();
    }

    private boolean existsCompanyConversionPending(Long memberId) {
        Long cnt = em.createQuery(
                        "select count(a) from " + CompanyConversionApplication.class.getSimpleName() + " a " +
                                "where a.applicant.id = :memberId and a.status = :status", Long.class)
                .setParameter("memberId", memberId)
                .setParameter("status", CompanyConversionStatus.PENDING)
                .getSingleResult();
        return cnt != null && cnt > 0;
    }

    private boolean existsSellerConversionPending(Long memberId) {
        Long cnt = em.createQuery(
                        "select count(a) from " + DealerConversionApplication.class.getSimpleName() + " a " +
                                "where a.applicant.id = :memberId " +
                                "and a.toDealerType = :toType " +
                                "and a.status = :status", Long.class)
                .setParameter("memberId", memberId)
                .setParameter("toType", DealerType.SELLER)
                .setParameter("status", DealerApplicationStatus.PENDING)
                .getSingleResult();
        return cnt != null && cnt > 0;
    }
}