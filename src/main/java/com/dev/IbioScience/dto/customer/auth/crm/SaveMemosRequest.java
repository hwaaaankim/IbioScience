package com.dev.IbioScience.dto.customer.auth.crm;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveMemosRequest {
	private List<String> addContents; // 신규 추가 메모 내용들
	private List<Long> deleteIds; // 삭제할 memo id들
}