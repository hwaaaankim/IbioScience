package com.dev.IbioScience.service.product;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.model.product.InternalCategoryLarge;
import com.dev.IbioScience.model.product.InternalCategoryMedium;
import com.dev.IbioScience.model.product.InternalCategorySmall;
import com.dev.IbioScience.repository.product.InternalCategoryLargeRepository;
import com.dev.IbioScience.repository.product.InternalCategoryMediumRepository;
import com.dev.IbioScience.repository.product.InternalCategorySmallRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InternalCategoryService {

    private final InternalCategoryLargeRepository largeRepository;
    private final InternalCategoryMediumRepository mediumRepository;
    private final InternalCategorySmallRepository smallRepository;

    // 대분류 전체 조회
    @Transactional(readOnly = true)
    public List<InternalCategoryLarge> getAllLarge() {
        return largeRepository.findAllByOrderByIdAsc();
    }

    // 대분류 생성
    @Transactional
    public InternalCategoryLarge createLarge(String name) {
        if (largeRepository.existsByName(name)) {
            throw new IllegalArgumentException("이미 존재하는 대분류명입니다.");
        }
        InternalCategoryLarge entity = new InternalCategoryLarge();
        entity.setName(name);
        return largeRepository.save(entity);
    }

    // 대분류 수정
    @Transactional
    public void updateLarge(Long id, String name) {
        InternalCategoryLarge entity = largeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("대분류를 찾을 수 없습니다."));
        if (!entity.getName().equals(name) && largeRepository.existsByName(name)) {
            throw new IllegalArgumentException("이미 존재하는 대분류명입니다.");
        }
        entity.setName(name);
        largeRepository.save(entity);
    }

    // 대분류 삭제 (cascade로 중/소분류 모두 삭제)
    @Transactional
    public void deleteLarge(Long id) {
        if (!largeRepository.existsById(id)) {
            throw new IllegalArgumentException("대분류를 찾을 수 없습니다.");
        }
        largeRepository.deleteById(id);
    }

    // 특정 대분류의 중분류 리스트
    @Transactional(readOnly = true)
    public List<InternalCategoryMedium> getMediumsByLargeId(Long largeId) {
        InternalCategoryLarge large = largeRepository.findById(largeId)
                .orElseThrow(() -> new IllegalArgumentException("대분류를 찾을 수 없습니다."));
        return mediumRepository.findAllByLargeOrderByIdAsc(large);
    }

    // 중분류 생성
    @Transactional
    public InternalCategoryMedium createMedium(String name, Long largeId) {
        InternalCategoryLarge large = largeRepository.findById(largeId)
                .orElseThrow(() -> new IllegalArgumentException("대분류를 찾을 수 없습니다."));
        if (mediumRepository.existsByLargeAndName(large, name)) {
            throw new IllegalArgumentException("이미 존재하는 중분류명입니다.");
        }
        InternalCategoryMedium entity = new InternalCategoryMedium();
        entity.setLarge(large);
        entity.setName(name);
        return mediumRepository.save(entity);
    }

    // 중분류 수정
    @Transactional
    public void updateMedium(Long id, String name) {
        InternalCategoryMedium entity = mediumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("중분류를 찾을 수 없습니다."));
        // 동일 대분류 내 중복 체크
        if (!entity.getName().equals(name) &&
                mediumRepository.existsByLargeAndName(entity.getLarge(), name)) {
            throw new IllegalArgumentException("이미 존재하는 중분류명입니다.");
        }
        entity.setName(name);
        mediumRepository.save(entity);
    }

    // 중분류 삭제 (cascade로 소분류 모두 삭제)
    @Transactional
    public void deleteMedium(Long id) {
        if (!mediumRepository.existsById(id)) {
            throw new IllegalArgumentException("중분류를 찾을 수 없습니다.");
        }
        mediumRepository.deleteById(id);
    }

    // 특정 중분류의 소분류 리스트
    @Transactional(readOnly = true)
    public List<InternalCategorySmall> getSmallsByMediumId(Long mediumId) {
        InternalCategoryMedium medium = mediumRepository.findById(mediumId)
                .orElseThrow(() -> new IllegalArgumentException("중분류를 찾을 수 없습니다."));
        return smallRepository.findAllByMediumOrderByIdAsc(medium);
    }

    // 소분류 생성
    @Transactional
    public InternalCategorySmall createSmall(String name, Long mediumId) {
        InternalCategoryMedium medium = mediumRepository.findById(mediumId)
                .orElseThrow(() -> new IllegalArgumentException("중분류를 찾을 수 없습니다."));
        if (smallRepository.existsByMediumAndName(medium, name)) {
            throw new IllegalArgumentException("이미 존재하는 소분류명입니다.");
        }
        InternalCategorySmall entity = new InternalCategorySmall();
        entity.setMedium(medium);
        entity.setName(name);
        return smallRepository.save(entity);
    }

    // 소분류 수정
    @Transactional
    public void updateSmall(Long id, String name) {
        InternalCategorySmall entity = smallRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("소분류를 찾을 수 없습니다."));
        // 동일 중분류 내 중복 체크
        if (!entity.getName().equals(name) &&
                smallRepository.existsByMediumAndName(entity.getMedium(), name)) {
            throw new IllegalArgumentException("이미 존재하는 소분류명입니다.");
        }
        entity.setName(name);
        smallRepository.save(entity);
    }

    // 소분류 삭제
    @Transactional
    public void deleteSmall(Long id) {
        if (!smallRepository.existsById(id)) {
            throw new IllegalArgumentException("소분류를 찾을 수 없습니다.");
        }
        smallRepository.deleteById(id);
    }
}