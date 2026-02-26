package com.dev.IbioScience.controller.customer.api;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * 마이페이지 버튼 노출/비활성 판정 + 탈퇴요청 처리 API
 *
 * ✅ 기존 마이페이지 렌더링 컨트롤러를 수정하지 않기 위해:
 * - 클라이언트(마이페이지 JS)가 이 API로 상태를 조회
 * - 버튼 노출/disable/메시지는 JS가 처리
 */
@RestController
@RequestMapping("/api/customer/mypage")
@RequiredArgsConstructor
public class CustomerMyPageApiController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * ✅ 마이페이지 버튼 상태 조회
     * - 개인/기업/판매딜러 구분
     * - 기업전환 신청(PENDING) 여부
     * - 판매딜러전환 신청(PENDING) 여부
     * - 탈퇴요청(WITHDRAWN) 여부
     */
    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MyPageStatusResponse> status(Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(401).body(
                    MyPageStatusResponse.builder()
                            .success(false)
                            .message("로그인이 필요합니다.")
                            .build()
            );
        }

        // member 기본정보 조회
        Map<String, Object> memberRow = findMemberRowByUsername(principal.getName());
        if (memberRow == null) {
            return ResponseEntity.status(401).body(
                    MyPageStatusResponse.builder()
                            .success(false)
                            .message("로그인이 필요합니다.")
                            .build()
            );
        }

        long memberId = ((Number) memberRow.get("id")).longValue();
        String status = String.valueOf(memberRow.get("status")); // ACTIVE / WITHDRAWN ...
        Object companyProfileId = memberRow.get("company_profile_id"); // null이면 개인

        boolean isCompany = (companyProfileId != null);
        boolean isSellerApproved = existsSellerDealerProfile(memberId); // seller_dealer_profile 존재 여부
        boolean withdrawAlready = "WITHDRAWN".equalsIgnoreCase(status);

        // 기업전환 신청(PENDING) 여부 (개인일 때만 의미)
        boolean companyConversionPending = existsCompanyConversionPending(memberId);

        // 판매딜러 전환 신청(PENDING) 여부 (기업+비판매딜러일 때만 의미)
        boolean sellerConversionPending = existsSellerConversionPending(memberId);

        // ✅ 버튼 노출 규칙(요구사항)
        boolean showWithdraw = true;
        boolean showCompanyConvert = (!isCompany);                 // 개인이면 기업전환 버튼 노출
        boolean showSellerApply = (isCompany && !isSellerApproved); // 기업이고 아직 판매딜러 아니면 판매딜러전환요청 노출

        return ResponseEntity.ok(
                MyPageStatusResponse.builder()
                        .success(true)
                        .message("OK")
                        .memberId(memberId)
                        .memberStatus(status)
                        .isCompany(isCompany)
                        .isSellerApproved(isSellerApproved)
                        .showWithdraw(showWithdraw)
                        .withdrawAlready(withdrawAlready)
                        .showCompanyConvert(showCompanyConvert)
                        .companyConversionPending(companyConversionPending)
                        .showSellerApply(showSellerApply)
                        .sellerConversionPending(sellerConversionPending)
                        .build()
        );
    }

    /**
     * ✅ 탈퇴요청: member.status = WITHDRAWN
     * - 완료 후 즉시 로그아웃 처리(세션 유지로 계속 로그인 상태인 문제 방지)
     */
    @PostMapping(
            value = "/withdraw",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Transactional
    public ResponseEntity<SimpleResponse> withdraw(
            @Valid @RequestBody WithdrawRequest req,
            Principal principal,
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        if (principal == null) {
            return ResponseEntity.status(401).body(SimpleResponse.fail("로그인이 필요합니다."));
        }

        // 본인 확인 (username + memberId)
        Map<String, Object> memberRow = findMemberRowByIdAndUsername(req.getMemberId(), principal.getName());
        if (memberRow == null) {
            return ResponseEntity.status(400).body(SimpleResponse.fail("잘못된 요청입니다."));
        }

        String currentStatus = String.valueOf(memberRow.get("status"));
        if ("WITHDRAWN".equalsIgnoreCase(currentStatus)) {
            return ResponseEntity.badRequest().body(SimpleResponse.fail("이미 탈퇴신청 되었습니다."));
        }

        // 상태 변경(요구사항: WITHDRAWN)
        // (withdrewAt 컬럼명은 네이밍 전략에 따라 달라질 수 있어, 여기서는 status만 확정 반영)
        int updated = jdbcTemplate.update(
                "UPDATE `member` SET `status` = 'WITHDRAWN' WHERE `id` = ? AND `username` = ?",
                req.getMemberId(), principal.getName()
        );

        if (updated != 1) {
            return ResponseEntity.status(500).body(SimpleResponse.fail("탈퇴요청 처리 중 오류가 발생했습니다."));
        }

        // ✅ 즉시 로그아웃(세션 무효화)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, auth);
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(SimpleResponse.ok("탈퇴요청이 접수되었습니다."));
    }

    /* =========================
     *  내부 조회 헬퍼
     * ========================= */

    private Map<String, Object> findMemberRowByUsername(String username) {
        // 필요한 컬럼만 조회
        // company_profile_id는 @JoinColumn(name="company_profile_id")로 명시되어 있으므로 컬럼명 확정
        try {
            return jdbcTemplate.queryForMap(
                    "SELECT `id`, `status`, `company_profile_id` FROM `member` WHERE `username` = ? LIMIT 1",
                    username
            );
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> findMemberRowByIdAndUsername(Long memberId, String username) {
        try {
            return jdbcTemplate.queryForMap(
                    "SELECT `id`, `status` FROM `member` WHERE `id` = ? AND `username` = ? LIMIT 1",
                    memberId, username
            );
        } catch (Exception e) {
            return null;
        }
    }

    private boolean existsSellerDealerProfile(long memberId) {
        // seller_dealer_profile: @JoinColumn(name="member_id")로 확정
        Integer found = jdbcTemplate.query(
                "SELECT 1 FROM `seller_dealer_profile` WHERE `member_id` = ? LIMIT 1",
                ps -> ps.setLong(1, memberId),
                rs -> rs.next() ? 1 : null
        );
        return found != null;
    }

    private boolean existsCompanyConversionPending(long memberId) {
        // company_conversion_application: applicant_id, status 컬럼명 확정(@Index, @Column(name="status"))
        Integer found = jdbcTemplate.query(
                "SELECT 1 FROM `company_conversion_application` WHERE `applicant_id` = ? AND `status` = 'PENDING' LIMIT 1",
                ps -> ps.setLong(1, memberId),
                rs -> rs.next() ? 1 : null
        );
        return found != null;
    }

    private boolean existsSellerConversionPending(long memberId) {
        // dealer_conversion_application: applicant_id, to_dealer_type, status 컬럼명 확정(@Column name 지정)
        Integer found = jdbcTemplate.query(
                "SELECT 1 FROM `dealer_conversion_application` WHERE `applicant_id` = ? AND `to_dealer_type` = 'SELLER' AND `status` = 'PENDING' LIMIT 1",
                ps -> ps.setLong(1, memberId),
                rs -> rs.next() ? 1 : null
        );
        return found != null;
    }

    /* =========================
     *  DTO
     * ========================= */

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WithdrawRequest {
        @NotNull
        private Long memberId;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleResponse {
        private boolean success;
        private String message;

        public static SimpleResponse ok(String msg) {
            return SimpleResponse.builder().success(true).message(msg).build();
        }

        public static SimpleResponse fail(String msg) {
            return SimpleResponse.builder().success(false).message(msg).build();
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyPageStatusResponse {
        private boolean success;
        private String message;

        private Long memberId;
        private String memberStatus; // ACTIVE/WITHDRAWN/...

        private boolean isCompany;         // company_profile_id != null
        private boolean isSellerApproved;  // seller_dealer_profile 존재 여부

        private boolean showWithdraw;
        private boolean withdrawAlready;

        private boolean showCompanyConvert;
        private boolean companyConversionPending;

        private boolean showSellerApply;
        private boolean sellerConversionPending;
    }
}