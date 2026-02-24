package com.dev.IbioScience.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.dev.IbioScience.enums.auth.DealerGrade;

@Component
public class StringToDealerGradeConverter implements Converter<String, DealerGrade> {

	private static final Logger log = LoggerFactory.getLogger(StringToDealerGradeConverter.class);

	@Override
	public DealerGrade convert(String source) {
		if (source == null) return null;

		String v = source.trim();
		if (v.isEmpty()) return null;

		// ✅ "ALL"은 '등급 무관' 의미 => null로 처리
		if ("ALL".equalsIgnoreCase(v)) return null;

		try {
			return DealerGrade.valueOf(v.toUpperCase());
		} catch (IllegalArgumentException e) {
			// 운영 안정성: 알 수 없는 값은 null 처리(필터 미적용)
			log.warn("Unknown DealerGrade value: {}", source);
			return null;
		}
	}
}