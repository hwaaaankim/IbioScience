package com.dev.IbioScience.service.seller.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.enums.auth.DealerType;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.SellerDealerProfile;
import com.dev.IbioScience.repository.auth.SellerDealerProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerProductAccessService {

    private final SellerDealerProfileRepository sellerDealerProfileRepository;

    public SellerDealerProfile getSellerProfileOrThrow(Long loginMemberId) {
        SellerDealerProfile sellerProfile = sellerDealerProfileRepository.findByMemberIdWithMember(loginMemberId)
                .orElseThrow(() -> new IllegalArgumentException("판매딜러 프로필을 찾을 수 없습니다."));

        Member member = sellerProfile.getMember();
        if (member == null) {
            throw new IllegalArgumentException("판매딜러 회원 정보가 올바르지 않습니다.");
        }

        if (member.getDealerType() != DealerType.SELLER) {
            throw new IllegalArgumentException("판매딜러 권한이 없는 회원입니다.");
        }

        return sellerProfile;
    }
}