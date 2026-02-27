package com.dev.IbioScience.controller.admin.member.api;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.common.CommonAPIResponse;
import com.dev.IbioScience.dto.customer.auth.crm.MemoPageResponseDto;
import com.dev.IbioScience.dto.customer.auth.crm.SaveMemosRequest;
import com.dev.IbioScience.dto.customer.auth.crm.UpdateAddressRequest;
import com.dev.IbioScience.dto.customer.auth.crm.UpdateBuyerGradeRequest;
import com.dev.IbioScience.dto.customer.auth.crm.UpdateSellerProfileRequest;
import com.dev.IbioScience.model.auth.PrincipalDetails;
import com.dev.IbioScience.service.auth.crm.ClientDetailHomeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/root/api/clientDetail/{memberId}")
public class ClientDetailCrmApiController {

    private final ClientDetailHomeService clientDetailHomeService;

    // 1) 비밀번호 초기화(즉시 반영)
    @PostMapping("/resetPassword")
    public CommonAPIResponse<Void> resetPassword(@PathVariable Long memberId) {
        clientDetailHomeService.resetPasswordAndSendSms(memberId);
        return CommonAPIResponse.ok("비밀번호 초기화 및 SMS 발송 완료", null);
    }

    // 2) 메모 저장(추가/삭제) - 한번에 반영
    @PostMapping("/memos/save")
    public CommonAPIResponse<Void> saveMemos(
        @PathVariable Long memberId,
        @AuthenticationPrincipal PrincipalDetails pd,
        @RequestBody SaveMemosRequest req
    ) {
        if (pd == null || pd.getMember() == null) {
            return CommonAPIResponse.fail(401, "인증이 필요합니다.", null);
        }
        clientDetailHomeService.saveMemos(memberId, pd.getMember().getId(), req);
        return CommonAPIResponse.ok("메모 변경사항 저장 완료", null);
    }

    // 2-1) 메모 전체보기(필터+페이지네이션)
    @GetMapping("/memos")
    public CommonAPIResponse<MemoPageResponseDto> memoPage(
        @PathVariable Long memberId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to
    ) {
        MemoPageResponseDto dto = clientDetailHomeService.getMemoPage(memberId, from, to, page, size);
        return CommonAPIResponse.ok(dto);
    }

    // 3) 바이어 딜러그레이드 변경
    @PostMapping("/buyer/grade")
    public CommonAPIResponse<Void> updateBuyerGrade(
        @PathVariable Long memberId,
        @RequestBody UpdateBuyerGradeRequest req
    ) {
        clientDetailHomeService.updateBuyerGrade(memberId, req);
        return CommonAPIResponse.ok("딜러 등급 변경 완료", null);
    }

    // 4) 회원 주소 변경
    @PostMapping("/member/address")
    public CommonAPIResponse<Void> updateMemberAddress(
        @PathVariable Long memberId,
        @RequestBody UpdateAddressRequest req
    ) {
        clientDetailHomeService.updateMemberAddress(memberId, req);
        return CommonAPIResponse.ok("회원 주소 변경 완료", null);
    }

    // 4-1) 회사 주소 변경
    @PostMapping("/company/address")
    public CommonAPIResponse<Void> updateCompanyAddress(
        @PathVariable Long memberId,
        @RequestBody UpdateAddressRequest req
    ) {
        clientDetailHomeService.updateCompanyAddress(memberId, req);
        return CommonAPIResponse.ok("회사 주소 변경 완료", null);
    }

    // 5) 셀러 전체 저장(프로필 + 주소 + 정산 + 카테고리권한)
    @PostMapping("/seller/saveAll")
    public CommonAPIResponse<Void> saveSellerAll(
        @PathVariable Long memberId,
        @RequestBody UpdateSellerProfileRequest req
    ) {
        clientDetailHomeService.updateSellerAll(memberId, req);
        return CommonAPIResponse.ok("셀러 변경사항 저장 완료", null);
    }
}