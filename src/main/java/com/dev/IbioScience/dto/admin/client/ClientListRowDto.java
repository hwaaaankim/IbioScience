package com.dev.IbioScience.dto.admin.client;

import java.time.LocalDateTime;

public class ClientListRowDto {

	private Long memberId;

	private LocalDateTime joinedAt;
	private String name;
	private String username;

	// 등급(일반/구매딜러/판매딜러)
	private String memberGradeText;

	// 회원구분(일반/사업자)
	private String memberTypeText;

	private String companyName;

	private String tel;
	private String mobile;

	private String recentOrderNo;

	private Long totalOrderAmount;
	private Long totalOrderCount;

	// 현재 페이지 내 순위
	private Integer pageRank;

	public ClientListRowDto(Long memberId, LocalDateTime joinedAt, String name, String username, String memberGradeText,
			String memberTypeText, String companyName, String tel, String mobile, String recentOrderNo,
			Long totalOrderAmount, Long totalOrderCount) {
		this.memberId = memberId;
		this.joinedAt = joinedAt;
		this.name = name;
		this.username = username;
		this.memberGradeText = memberGradeText;
		this.memberTypeText = memberTypeText;
		this.companyName = companyName;
		this.tel = tel;
		this.mobile = mobile;
		this.recentOrderNo = recentOrderNo;
		this.totalOrderAmount = totalOrderAmount == null ? 0L : totalOrderAmount;
		this.totalOrderCount = totalOrderCount == null ? 0L : totalOrderCount;
	}

	// getters/setters
	public Long getMemberId() {
		return memberId;
	}

	public LocalDateTime getJoinedAt() {
		return joinedAt;
	}

	public String getName() {
		return name;
	}

	public String getUsername() {
		return username;
	}

	public String getMemberGradeText() {
		return memberGradeText;
	}

	public String getMemberTypeText() {
		return memberTypeText;
	}

	public String getCompanyName() {
		return companyName;
	}

	public String getTel() {
		return tel;
	}

	public String getMobile() {
		return mobile;
	}

	public String getRecentOrderNo() {
		return recentOrderNo;
	}

	public Long getTotalOrderAmount() {
		return totalOrderAmount;
	}

	public Long getTotalOrderCount() {
		return totalOrderCount;
	}

	public Integer getPageRank() {
		return pageRank;
	}

	public void setPageRank(Integer pageRank) {
		this.pageRank = pageRank;
	}
}