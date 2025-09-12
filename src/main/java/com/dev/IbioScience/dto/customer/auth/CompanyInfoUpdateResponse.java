package com.dev.IbioScience.dto.customer.auth;

import lombok.Builder;
import lombok.Data;

/**
 * 기업회원 정보수정 초기 바인딩 응답 DTO (서버 → 화면)
 * - 전화/휴대폰/팩스/사업자등록번호: 파트별 바인딩을 위해 분할 필드 제공
 */
@Data
@Builder
public class CompanyInfoUpdateResponse {

    // ===== 회원 =====
    private Long memberId;
    private String username;
    private String name;

    // 휴대폰(분할)
    private String mobile1; // 예: 010
    private String mobile2; // 예: 1234
    private String mobile3; // 예: 5678

    private String email;

    // 기본주소
    private String zip;
    private String road;
    private String jibun;   // 선택
    private String detail;

    // ===== 회사 =====
    private Long companyId;
    private String companyName;
    private String ceoName;
    private String department;
    private String companyEmail;
    private String bizType;
    private String bizItem;

    // 대표전화(분할)
    private String bizTel1; // 예: 02 또는 031 등
    private String bizTel2; // 예: 123
    private String bizTel3; // 예: 4567

    // 팩스(분할)
    private String fax1;
    private String fax2;
    private String fax3;

    // 사업자등록번호(분할) 3-2-5
    private String bizNo1; // 3자리
    private String bizNo2; // 2자리
    private String bizNo3; // 5자리

    // 사업장 주소
    private String workplaceZip;
    private String workplaceRoad;
    private String workplaceJibun;   // 선택
    private String workplaceDetail;

    // 사업자등록증 파일 (단일이지만 프리뷰를 위해 정보 제공)
    private String bizRegOriginalName; // 파일명 추출(없을 수 있음)
    private String bizRegPublicUrl;    // 접근 URL
    private String bizRegContentType;  // image/* or application/*
    private long   bizRegSize;         // 0이면 미지정
}