package com.dev.IbioScience.service.category;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.CategoryLargeApiDto;
import com.dev.IbioScience.dto.CategoryMediumApiDto;
import com.dev.IbioScience.dto.CategorySmallApiDto;
import com.dev.IbioScience.model.product.category.CategoryLarge;
import com.dev.IbioScience.model.product.category.CategoryMedium;
import com.dev.IbioScience.model.product.category.CategorySmall;
import com.dev.IbioScience.model.product.relation.MediumSmallCategory;
import com.dev.IbioScience.repository.category.CategoryLargeRepository;
import com.dev.IbioScience.repository.category.CategoryMediumRepository;
import com.dev.IbioScience.repository.category.CategorySmallRepository;
import com.dev.IbioScience.repository.category.MediumSmallCategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {

	private final CategoryLargeRepository largeRepo;
	private final CategoryMediumRepository mediumRepo;
	private final CategorySmallRepository smallRepo;
	private final MediumSmallCategoryRepository mappingRepo;

	@Transactional(readOnly = true)
    public List<MediumSmallCategory> getAllMappings() {
        return mappingRepo.findAll();
    }
	
	// ------------------ 대분류 ------------------ //
	@Transactional(readOnly = true)
	public List<CategoryLarge> getAllLarge() {
		return largeRepo.findAll();
	}

	@Transactional
	public CategoryLarge createLarge(String name) {
		if (largeRepo.existsByName(name))
			throw new IllegalArgumentException("중복된 대분류명입니다.");
		CategoryLarge entity = new CategoryLarge();
		entity.setName(name);
		return largeRepo.save(entity);
	}

	@Transactional
	public CategoryLarge updateLarge(Long id, String name) {
		CategoryLarge entity = largeRepo.findById(id).orElseThrow(() -> new NoSuchElementException("대분류 없음"));
		if (!entity.getName().equals(name) && largeRepo.existsByName(name))
			throw new IllegalArgumentException("중복된 대분류명입니다.");
		entity.setName(name);
		return entity;
	}

	@Transactional
	public void deleteLarge(Long id) {
		CategoryLarge entity = largeRepo.findById(id).orElseThrow(() -> new NoSuchElementException("대분류 없음"));
		if (!entity.getMediums().isEmpty())
			throw new IllegalStateException("하위 중분류가 존재하여 삭제할 수 없습니다.");
		largeRepo.delete(entity);
	}

	// ------------------ 중분류 ------------------ //
	@Transactional(readOnly = true)
	public List<CategoryMedium> getAllMedium() {
		return mediumRepo.findAll();
	}

	@Transactional
	public CategoryMedium createMedium(Long largeId, String name) {
		CategoryLarge large = largeRepo.findById(largeId).orElseThrow(() -> new NoSuchElementException("대분류 없음"));
		if (mediumRepo.existsByNameAndLarge(name, large))
			throw new IllegalArgumentException("중복된 중분류명입니다.");
		CategoryMedium medium = new CategoryMedium();
		medium.setLarge(large);
		medium.setName(name);
		return mediumRepo.save(medium);
	}

	@Transactional
	public CategoryMedium updateMedium(Long id, String name) {
		CategoryMedium entity = mediumRepo.findById(id).orElseThrow(() -> new NoSuchElementException("중분류 없음"));
		if (!entity.getName().equals(name) && mediumRepo.existsByNameAndLarge(name, entity.getLarge()))
			throw new IllegalArgumentException("중복된 중분류명입니다.");
		entity.setName(name);
		return entity;
	}

	@Transactional
	public void deleteMedium(Long id) {
		CategoryMedium medium = mediumRepo.findById(id).orElseThrow(() -> new NoSuchElementException("중분류 없음"));
		if (mappingRepo.existsByMedium(medium))
			throw new IllegalStateException("매핑된 소분류가 있어 삭제할 수 없습니다.");
		mediumRepo.delete(medium);
	}

	// ------------------ 소분류 ------------------ //
	@Transactional(readOnly = true)
	public List<CategorySmall> getAllSmall() {
		return smallRepo.findAll();
	}

	@Transactional
	public CategorySmall createSmall(String name) {
		if (smallRepo.existsByName(name))
			throw new IllegalArgumentException("중복된 소분류명입니다.");
		CategorySmall entity = new CategorySmall();
		entity.setName(name);
		return smallRepo.save(entity);
	}

	@Transactional
	public CategorySmall updateSmall(Long id, String name) {
		CategorySmall entity = smallRepo.findById(id).orElseThrow(() -> new NoSuchElementException("소분류 없음"));
		if (!entity.getName().equals(name) && smallRepo.existsByName(name))
			throw new IllegalArgumentException("중복된 소분류명입니다.");
		entity.setName(name);
		return entity;
	}

	@Transactional
	public void deleteSmall(Long id) {
		CategorySmall small = smallRepo.findById(id).orElseThrow(() -> new NoSuchElementException("소분류 없음"));
		if (mappingRepo.existsBySmall(small))
			throw new IllegalStateException("매핑된 중분류가 있어 삭제할 수 없습니다.");
		smallRepo.delete(small);
	}

	// ------------------ 중분류-소분류 매핑 ------------------ //
	@Transactional(readOnly = true)
	public List<MediumSmallCategory> getMappingsBySmall(Long smallId) {
		CategorySmall small = smallRepo.findById(smallId).orElseThrow(() -> new NoSuchElementException("소분류 없음"));
		return mappingRepo.findBySmall(small);
	}

	@Transactional
	public void createMappings(Long smallId, List<Long> mediumIds) {
	    CategorySmall small = smallRepo.findById(smallId)
	            .orElseThrow(() -> new NoSuchElementException("소분류 없음"));
	    List<CategoryMedium> mediums = mediumRepo.findAllById(mediumIds);

	    // 중복 매핑 체크
	    for (CategoryMedium medium : mediums) {
	        if (mappingRepo.existsByMediumAndSmall(medium, small)) {
	            // 중복 발견시 바로 예외
	            throw new IllegalArgumentException("이미 연결되어 있습니다.");
	        }
	    }
	    // 중복 없는 것만 등록
	    for (CategoryMedium medium : mediums) {
	        MediumSmallCategory mapping = new MediumSmallCategory();
	        mapping.setSmall(small);
	        mapping.setMedium(medium);
	        mappingRepo.save(mapping);
	    }
	}

	@Transactional
	public void deleteMapping(Long mappingId) {
		MediumSmallCategory mapping = mappingRepo.findById(mappingId)
				.orElseThrow(() -> new NoSuchElementException("매핑 없음"));
		mappingRepo.delete(mapping);
	}
	
	public List<CategoryLargeApiDto> getLargeCategories() {
        return largeRepo.findAllByOrderByNameAsc()
            .stream().map(CategoryLargeApiDto::from).collect(Collectors.toList());
    }
	
    public List<CategoryMediumApiDto> getMediumCategories(Long largeId) {
        return mediumRepo.findByLargeIdOrderByNameAsc(largeId)
            .stream().map(CategoryMediumApiDto::from).collect(Collectors.toList());
    }
    
    public List<CategorySmallApiDto> getSmallCategories(Long mediumId) {
        return mappingRepo.findSmallByMediumId(mediumId)
            .stream().map(CategorySmallApiDto::from).collect(Collectors.toList());
    }
}