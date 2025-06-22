package com.dev.IbioScience.controller.api.category;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.model.product.category.CategoryLarge;
import com.dev.IbioScience.model.product.category.CategoryMedium;
import com.dev.IbioScience.model.product.category.CategorySmall;
import com.dev.IbioScience.model.product.relation.MediumSmallCategory;
import com.dev.IbioScience.service.category.CategoryService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

	private final CategoryService categoryService;

	// ----------- 대분류 -----------
	@GetMapping("/large")
	public List<CategoryLargeDto> getAllLarge() {
		return categoryService.getAllLarge().stream().map(CategoryLargeDto::from).collect(Collectors.toList());
	}

	@PostMapping("/large")
	public CategoryLargeDto createLarge(@RequestBody Map<String, String> body) {
		return CategoryLargeDto.from(categoryService.createLarge(body.get("name")));
	}

	@PutMapping("/large/{id}")
	public CategoryLargeDto updateLarge(@PathVariable Long id, @RequestBody Map<String, String> body) {
		return CategoryLargeDto.from(categoryService.updateLarge(id, body.get("name")));
	}

	@DeleteMapping("/large/{id}")
	public void deleteLarge(@PathVariable Long id) {
		categoryService.deleteLarge(id);
	}

	// ----------- 중분류 -----------
	@GetMapping("/medium")
	public List<CategoryMediumDto> getAllMedium() {
		return categoryService.getAllMedium().stream().map(CategoryMediumDto::from).collect(Collectors.toList());
	}

	@PostMapping("/medium")
	public CategoryMediumDto createMedium(@RequestBody Map<String, Object> body) {
	    String name = (String) body.get("name");
	    Object largeIdObj = body.get("largeId");
	    Long largeId;

	    if (largeIdObj instanceof Number) {
	        largeId = ((Number) largeIdObj).longValue();
	    } else if (largeIdObj instanceof String) {
	        largeId = Long.parseLong((String) largeIdObj);
	    } else {
	        throw new IllegalArgumentException("largeId 값이 올바르지 않습니다.");
	    }
	    return CategoryMediumDto.from(categoryService.createMedium(largeId, name));
	}


	@PutMapping("/medium/{id}")
	public CategoryMediumDto updateMedium(@PathVariable Long id, @RequestBody Map<String, String> body) {
		return CategoryMediumDto.from(categoryService.updateMedium(id, body.get("name")));
	}

	@DeleteMapping("/medium/{id}")
	public void deleteMedium(@PathVariable Long id) {
		categoryService.deleteMedium(id);
	}

	// ----------- 소분류 -----------
	@GetMapping("/small")
	public List<CategorySmallDto> getAllSmall() {
		return categoryService.getAllSmall().stream().map(CategorySmallDto::from).collect(Collectors.toList());
	}

	@PostMapping("/small")
	public CategorySmallDto createSmall(@RequestBody Map<String, String> body) {
		return CategorySmallDto.from(categoryService.createSmall(body.get("name")));
	}

	@PutMapping("/small/{id}")
	public CategorySmallDto updateSmall(@PathVariable Long id, @RequestBody Map<String, String> body) {
		return CategorySmallDto.from(categoryService.updateSmall(id, body.get("name")));
	}

	@DeleteMapping("/small/{id}")
	public void deleteSmall(@PathVariable Long id) {
		categoryService.deleteSmall(id);
	}

	// ----------- 중분류-소분류 매핑 -----------
	@GetMapping("/mapping/small/{smallId}")
	public List<MediumSmallMappingDto> getMappingBySmall(@PathVariable Long smallId) {
		return categoryService.getMappingsBySmall(smallId).stream().map(MediumSmallMappingDto::from)
				.collect(Collectors.toList());
	}

	@PostMapping("/mapping")
	public Map<String, Object> createMappings(@RequestBody MappingRequest body) {
	    categoryService.createMappings(body.getSmallId(), body.getMediumIds());
	    return Map.of("result", "ok");
	}

	@DeleteMapping("/mapping/{mappingId}")
	public Map<String, Object> deleteMapping(@PathVariable Long mappingId) {
	    categoryService.deleteMapping(mappingId);
	    return Map.of("result", "ok");
	}


	// ------ DTO 선언부 -------
	@Data
	@AllArgsConstructor
	static class CategoryLargeDto {
		private Long id;
		private String name;

		public static CategoryLargeDto from(CategoryLarge l) {
			return new CategoryLargeDto(l.getId(), l.getName());
		}
	}

	@Data
	@AllArgsConstructor
	static class CategoryMediumDto {
		private Long id;
		private String name;
		private Long largeId;

		public static CategoryMediumDto from(CategoryMedium m) {
			return new CategoryMediumDto(m.getId(), m.getName(), m.getLarge().getId());
		}
	}

	@Data
	@AllArgsConstructor
	static class CategorySmallDto {
		private Long id;
		private String name;

		public static CategorySmallDto from(CategorySmall s) {
			return new CategorySmallDto(s.getId(), s.getName());
		}
	}

	@Data
	@AllArgsConstructor
	static class MediumSmallMappingDto {
		private Long id;
		private Long mediumId;
		private Long smallId;

		public static MediumSmallMappingDto from(MediumSmallCategory m) {
			return new MediumSmallMappingDto(m.getId(), m.getMedium().getId(), m.getSmall().getId());
		}
	}

	@Data
	static class MappingRequest {
		private Long smallId;
		private List<Long> mediumIds;
	}
}