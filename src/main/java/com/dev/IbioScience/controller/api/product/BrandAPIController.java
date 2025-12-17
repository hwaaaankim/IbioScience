package com.dev.IbioScience.controller.api.product;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.BrandSearchDTO;
import com.dev.IbioScience.model.product.Brand;
import com.dev.IbioScience.repository.product.BrandRepository;
import com.dev.IbioScience.service.product.BrandService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/brand")
@RequiredArgsConstructor
public class BrandAPIController {


    private final BrandService brandService;
    private final BrandRepository brandRepository;

    @Value("${spring.upload.path}")
    private String uploadPath;

    @PostMapping("/insert")
    public ResponseEntity<?> registerBrand(@RequestParam("name") String name,
                                           @RequestParam("image") MultipartFile image) throws IOException {
        if (!StringUtils.hasText(name) || image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body("브랜드명 또는 이미지가 비어있습니다.");
        }
        Brand brand = brandService.saveBrand(name, image);
        
        return ResponseEntity.ok(brand);
    }

    @GetMapping("/list")
    public ResponseEntity<?> getBrandList(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "") String keyword) {
        Page<Brand> result = brandService.getBrands(keyword, PageRequest.of(page, 18));
        System.out.println(result.getTotalPages());
        System.out.println();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateBrand(@RequestParam("id") Long id,
                                         @RequestParam("name") String name,
                                         @RequestParam(value = "image", required = false) MultipartFile image) throws IOException {
        brandService.updateBrand(id, name, image);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteBrand(@PathVariable("id") Long id) throws IOException {
        try {
            brandService.deleteBrand(id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            // 서비스에서 던진 “연결 제품 존재” 케이스
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (DataIntegrityViolationException e) {
            // FK 제약 등 DB 레벨에서 막힌 경우도 사용자 메시지로 변환
            return ResponseEntity.status(HttpStatus.CONFLICT).body("해당 브랜드는 등록된 제품과 연결되어 있어 삭제할 수 없습니다.");
        }
    }

    // === 브랜드 이미지만 삭제 ===
    @PostMapping("/image/delete/{id}")
    public ResponseEntity<?> deleteBrandImage(@PathVariable("id") Long id) throws IOException {
        brandService.deleteBrandImage(id);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<BrandSearchDTO>> searchBrand(@RequestParam String keyword) {
        List<Brand> brands = brandRepository.findByNameContainingIgnoreCase(keyword);
        List<BrandSearchDTO> result = brands.stream().map(BrandSearchDTO::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
    
}
