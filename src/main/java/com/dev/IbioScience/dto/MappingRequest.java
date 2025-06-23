package com.dev.IbioScience.dto;

import java.util.List;

import lombok.Data;

@Data
public class MappingRequest {
	private Long smallId;
	private List<Long> mediumIds;
}