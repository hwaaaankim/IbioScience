package com.dev.IbioScience.model.auth.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class Address {
	/** 우편번호 */
	@Column(length = 10)
	private String postcode;
	/** 도로명주소 */
	@Column(length = 255)
	private String roadAddress;
	/** 지번주소 */
	@Column(length = 255)
	private String jibunAddress;
	/** 상세주소 */
	@Column(length = 255)
	private String detailAddress;
}