package com.dev.IbioScience.controller.api.product;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.internal.InternalLargeListDTO;
import com.dev.IbioScience.dto.internal.InternalMediumListDTO;
import com.dev.IbioScience.dto.internal.InternalSmallListDTO;
import com.dev.IbioScience.model.product.InternalCategoryLarge;
import com.dev.IbioScience.model.product.InternalCategoryMedium;
import com.dev.IbioScience.model.product.InternalCategorySmall;
import com.dev.IbioScience.service.product.InternalCategoryQueryService;
import com.dev.IbioScience.service.product.InternalCategoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/internal-category")
@RequiredArgsConstructor
public class InternalCategoryAPIController {

    private final InternalCategoryService internalCategoryService;
    private final InternalCategoryQueryService service;
    
    // 대분류 전체 조회
    @GetMapping("/large")
    public List<Map<String, Object>> getLargeList() {
        List<InternalCategoryLarge> largeList = internalCategoryService.getAllLarge();
        List<Map<String, Object>> result = largeList.stream().map(large -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", large.getId());
            map.put("name", large.getName());
            map.put("mediumCount", large.getMediums() != null ? large.getMediums().size() : 0);
            return map;
        }).collect(Collectors.toList());
        return result;
    }

    // 2. 대분류 등록
    @PostMapping("/large")
    public ResponseEntity<?> addLarge(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (!StringUtils.hasText(name)) return ResponseEntity.badRequest().body("대분류명이 필요합니다.");
        internalCategoryService.createLarge(name.trim());
        return ResponseEntity.ok().build();
    }

    // 3. 대분류 수정
    @PutMapping("/large/{id}")
    public ResponseEntity<?> updateLarge(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (!StringUtils.hasText(name)) return ResponseEntity.badRequest().body("대분류명이 필요합니다.");
        internalCategoryService.updateLarge(id, name.trim());
        return ResponseEntity.ok().build();
    }

    // 4. 대분류 삭제
    @DeleteMapping("/large/{id}")
    public ResponseEntity<?> deleteLarge(@PathVariable Long id) {
        internalCategoryService.deleteLarge(id);
        return ResponseEntity.ok().build();
    }

    // 5. 중분류(대분류별) 리스트
    @GetMapping("/medium")
    public List<Map<String, Object>> getMediumList(@RequestParam Long largeId) {
        List<InternalCategoryMedium> mediumList = internalCategoryService.getMediumsByLargeId(largeId);
        List<Map<String, Object>> result = mediumList.stream().map(medium -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", medium.getId());
            map.put("name", medium.getName());
            map.put("smallCount", medium.getSmalls() != null ? medium.getSmalls().size() : 0);
            return map;
        }).collect(Collectors.toList());
        return result;
    }

    // 6. 중분류 등록
    @PostMapping("/medium")
    public ResponseEntity<?> addMedium(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Object largeIdObj = body.get("largeId");
        if (!StringUtils.hasText(name)) return ResponseEntity.badRequest().body("중분류명이 필요합니다.");
        if (largeIdObj == null) return ResponseEntity.badRequest().body("대분류ID가 필요합니다.");
        Long largeId;
        try {
            largeId = Long.valueOf(String.valueOf(largeIdObj));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("잘못된 대분류ID입니다.");
        }
        internalCategoryService.createMedium(name.trim(), largeId);
        return ResponseEntity.ok().build();
    }

    // 7. 중분류 수정
    @PutMapping("/medium/{id}")
    public ResponseEntity<?> updateMedium(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (!StringUtils.hasText(name)) return ResponseEntity.badRequest().body("중분류명이 필요합니다.");
        internalCategoryService.updateMedium(id, name.trim());
        return ResponseEntity.ok().build();
    }

    // 8. 중분류 삭제
    @DeleteMapping("/medium/{id}")
    public ResponseEntity<?> deleteMedium(@PathVariable Long id) {
        internalCategoryService.deleteMedium(id);
        return ResponseEntity.ok().build();
    }

    // 9. 소분류(중분류별) 리스트
    @GetMapping("/small")
    public List<Map<String, Object>> getSmallList(@RequestParam Long mediumId) {
        List<InternalCategorySmall> smallList = internalCategoryService.getSmallsByMediumId(mediumId);
        List<Map<String, Object>> result = smallList.stream().map(small -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", small.getId());
            map.put("name", small.getName());
            return map;
        }).collect(Collectors.toList());
        return result;
    }

    // 10. 소분류 등록
    @PostMapping("/small")
    public ResponseEntity<?> addSmall(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Object mediumIdObj = body.get("mediumId");
        if (!StringUtils.hasText(name)) return ResponseEntity.badRequest().body("소분류명이 필요합니다.");
        if (mediumIdObj == null) return ResponseEntity.badRequest().body("중분류ID가 필요합니다.");
        Long mediumId;
        try {
            mediumId = Long.valueOf(String.valueOf(mediumIdObj));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("잘못된 중분류ID입니다.");
        }
        internalCategoryService.createSmall(name.trim(), mediumId);
        return ResponseEntity.ok().build();
    }

    // 11. 소분류 수정
    @PutMapping("/small/{id}")
    public ResponseEntity<?> updateSmall(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (!StringUtils.hasText(name)) return ResponseEntity.badRequest().body("소분류명이 필요합니다.");
        internalCategoryService.updateSmall(id, name.trim());
        return ResponseEntity.ok().build();
    }

    // 12. 소분류 삭제
    @DeleteMapping("/small/{id}")
    public ResponseEntity<?> deleteSmall(@PathVariable Long id) {
        internalCategoryService.deleteSmall(id);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/list-large")
    public ResponseEntity<List<InternalLargeListDTO>> listLarge() {
        return ResponseEntity.ok(service.listLarge());
    }

    @GetMapping("/list-medium")
    public ResponseEntity<List<InternalMediumListDTO>> listMedium(@RequestParam Long largeId) {
        return ResponseEntity.ok(service.listMedium(largeId));
    }

    @GetMapping("/list-small")
    public ResponseEntity<List<InternalSmallListDTO>> listSmall(@RequestParam Long mediumId) {
        return ResponseEntity.ok(service.listSmall(mediumId));
    }
}