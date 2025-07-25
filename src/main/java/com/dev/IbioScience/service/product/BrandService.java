package com.dev.IbioScience.service.product;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.model.product.Brand;
import com.dev.IbioScience.repository.product.BrandRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BrandService {

	private final BrandRepository brandRepository;

    @Value("${spring.upload.path}")
    private String uploadPath;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional
    public Brand saveBrand(String name, MultipartFile imageFile) throws IOException {
        Brand brand = new Brand();
        brand.setName(name);
        brand = brandRepository.save(brand);

        if (imageFile != null && !imageFile.isEmpty()) {
            String today = LocalDate.now().format(FORMATTER);
            String relativeDir = String.format("brand/%d/%s/", brand.getId(), today);
            String saveDirPath = uploadPath + File.separator + relativeDir;
            File dir = new File(saveDirPath);
            if (!dir.exists()) dir.mkdirs();

            String originalFilename = imageFile.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            String fileName = "brand" + fileExtension;
            String saveFullPath = saveDirPath + fileName;
            imageFile.transferTo(new File(saveFullPath));

            String imageUrl = "/upload/" + relativeDir + fileName;
            brand.setImagePath(saveFullPath);
            brand.setImageRoad(imageUrl);

            brand = brandRepository.save(brand);
        }
        return brand;
    }

    @Transactional(readOnly = true)
    public Page<Brand> getBrands(String keyword, Pageable pageable) {
        return brandRepository.findByNameContaining(keyword, pageable);
    }

    @Transactional
    public void updateBrand(Long id, String name, MultipartFile imageFile) throws IOException {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("브랜드를 찾을 수 없습니다. ID = " + id));
        brand.setName(name);

        if (imageFile != null && !imageFile.isEmpty()) {
            // 기존 이미지 삭제
            if (StringUtils.hasText(brand.getImagePath())) {
                File oldFile = new File(brand.getImagePath());
                if (oldFile.exists()) oldFile.delete();
            }
            // 새 이미지 저장
            String today = LocalDate.now().format(FORMATTER);
            String relativeDir = String.format("brand/%d/%s/", brand.getId(), today);
            String saveDirPath = uploadPath + File.separator + relativeDir;
            File dir = new File(saveDirPath);
            if (!dir.exists()) dir.mkdirs();

            String originalFilename = imageFile.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            String fileName = "brand" + fileExtension;
            String saveFullPath = saveDirPath + fileName;
            imageFile.transferTo(new File(saveFullPath));

            String imageUrl = "/upload/" + relativeDir + fileName;

            brand.setImagePath(saveFullPath);
            brand.setImageRoad(imageUrl);
        }
        brandRepository.save(brand);
    }

    @Transactional
    public void deleteBrand(Long id) throws IOException {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("브랜드를 찾을 수 없습니다. ID=" + id));
        if (StringUtils.hasText(brand.getImagePath())) {
            File oldFile = new File(brand.getImagePath());
            if (oldFile.exists()) oldFile.delete();
        }
        brandRepository.delete(brand);
    }

    // === 이미지만 삭제 ===
    @Transactional
    public void deleteBrandImage(Long id) throws IOException {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("브랜드를 찾을 수 없습니다. ID=" + id));
        if (StringUtils.hasText(brand.getImagePath())) {
            File oldFile = new File(brand.getImagePath());
            if (oldFile.exists()) oldFile.delete();
        }
        brand.setImagePath(null);
        brand.setImageRoad(null);
        brandRepository.save(brand);
    }
}