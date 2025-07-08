package com.dev.IbioScience.controller.api.product;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.MoveEditorImageRequestDTO;
import com.dev.IbioScience.dto.ProductRegisterRequestDTO;
import com.dev.IbioScience.service.category.ProductService;
import com.dev.IbioScience.service.category.ProductService.ProductSimpleDto;
import com.dev.IbioScience.service.product.ProductRegisterService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductAPIController {
    
	private final ProductService productService;
    private final ProductRegisterService productRegisterService;
    
    @GetMapping("/list-simple")
    public List<ProductSimpleDto> listSimple(@RequestParam Long smallId) {
        return productService.getSimpleProductListBySmallId(smallId);
    }
    
    // 에디터 이미지 임시 업로드
    @PostMapping(value = "/editor-images", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadEditorImages(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "type", required = false) String type, // "detailHtml" 또는 "question"
            @RequestParam(value = "key", required = false) String key // "question_1" 등
    ) {
        // 만약 type, key가 필요 없다면 아래 한 줄로 유지
        List<String> urlList = productRegisterService.uploadEditorImages(files, type, key);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("imageUrls", urlList);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/{productId}/move-editor-images")
    public ResponseEntity<?> moveEditorImages(
            @PathVariable Long productId,
            @RequestBody MoveEditorImageRequestDTO request
    ) {
        String newHtml = productRegisterService.moveEditorImages(
            productId,
            request.getType(),
            request.getKey(),
            request.getHtml(),
            request.getTempImgList()
        );
        return ResponseEntity.ok(Map.of("success", true, "newHtml", newHtml));
    }
    
    @PostMapping(value = "/insert", consumes = {"multipart/form-data"})
    public ResponseEntity<?> registerProduct(
            @RequestParam Map<String, String> params,
            @RequestParam(required = false) Map<String, MultipartFile> files) throws IOException {

        // DTO 파싱
        ProductRegisterRequestDTO dto = mapToRegisterRequestDTO(params, files);

        // 디버깅 전체 출력
        System.out.println("==== [ProductRegisterRequestDTO 전체 출력] ====");
        System.out.println(dto);

        // 실제 서비스 처리
        Long productId = productRegisterService.registerProduct(dto);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("productId", productId);
        return ResponseEntity.ok(result);
    }

    // -- FormData → DTO 변환 로직
    private ProductRegisterRequestDTO mapToRegisterRequestDTO(Map<String, String> params, Map<String, MultipartFile> files) {
        ProductRegisterRequestDTO dto = new ProductRegisterRequestDTO();

        // 1. 소분류(카테고리)
        for (int i = 0; ; i++) {
            String v = params.get("categorySmallIds[" + i + "]");
            if (v == null) break;
            dto.getCategorySmallIds().add(Long.valueOf(v));
        }
        if (params.containsKey("categorySmallIds[]")) {
            // 배열 [] 형태도 병행 지원
            for (String v : params.get("categorySmallIds[]").split(",")) {
                if (v != null && !v.isEmpty()) dto.getCategorySmallIds().add(Long.valueOf(v));
            }
        }

        // 2. 공통표시옵션 (질문/옵션/CKEditor/파일)
        // - name=question_{id}
        params.forEach((k, v) -> {
            if (k.startsWith("question_")) {
                dto.getDisplayOptions().put(k, v);
            }
        });
        if (files != null) {
            files.forEach((k, v) -> {
                if (k.startsWith("question_")) {
                    dto.getDisplayOptionFiles().put(k, v);
                }
            });
        }

        // 3. 기본정보
        dto.setProductName(params.getOrDefault("productName", ""));
        dto.setProductCode(params.getOrDefault("productCode", ""));
        dto.setDisplayStatus(params.getOrDefault("displayStatus", ""));
        dto.setSaleStatus(params.getOrDefault("saleStatus", ""));
        dto.setDetailHtml(params.getOrDefault("detailHtml", ""));

        // 4. 대표/추가이미지
        if (files != null && files.containsKey("mainImage")) {
            dto.setMainImage(files.get("mainImage"));
        }
        if (files != null) {
            int i = 0;
            while (true) {
                MultipartFile f = files.get("subImages[" + i + "]");
                if (f == null) break;
                dto.getSubImages().add(f);
                i++;
            }
            // 배열 [] 방식 지원
            if (files.containsKey("subImages[]")) {
                dto.getSubImages().add(files.get("subImages[]"));
            }
        }

        // 5. 추가입력필드
        for (int i = 0; ; i++) {
            String label = params.get("extraFields[" + i + "].label");
            String value = params.get("extraFields[" + i + "].value");
            if (label == null && value == null) break;
            if (label != null || value != null) {
                ProductRegisterRequestDTO.ExtraFieldDTO ef = new ProductRegisterRequestDTO.ExtraFieldDTO();
                ef.setLabel(label);
                ef.setValue(value);
                dto.getExtraFields().add(ef);
            }
        }

        // 6. 옵션그룹/옵션
        for (int g = 0; ; g++) {
            String gName = params.get("optionGroups[" + g + "].name");
            if (gName == null) break;
            ProductRegisterRequestDTO.OptionGroupDTO og = new ProductRegisterRequestDTO.OptionGroupDTO();
            og.setName(gName);
            for (int o = 0; ; o++) {
                String name = params.get("optionGroups[" + g + "].options[" + o + "].name");
                if (name == null) break;
                ProductRegisterRequestDTO.OptionDTO opt = new ProductRegisterRequestDTO.OptionDTO();
                opt.setName(name);
                opt.setValue(params.get("optionGroups[" + g + "].options[" + o + "].value"));
                opt.setExtraPrice(params.get("optionGroups[" + g + "].options[" + o + "].extraPrice"));
                opt.setSign(params.get("optionGroups[" + g + "].options[" + o + "].sign"));
                String so = params.get("optionGroups[" + g + "].options[" + o + "].sortOrder");
                opt.setSortOrder(so != null && !so.isEmpty() ? Integer.valueOf(so) : null);
                og.getOptions().add(opt);
            }
            dto.getOptionGroups().add(og);
        }

        // 7. 키워드
        // 배열 방식: keywords[]
        if (params.containsKey("keywords[]")) {
            for (String kw : params.get("keywords[]").split(",")) {
                if (kw != null && !kw.isEmpty()) dto.getKeywords().add(kw);
            }
        }
        for (int i = 0; ; i++) {
            String kw = params.get("keywords[" + i + "]");
            if (kw == null) break;
            dto.getKeywords().add(kw);
        }

        // 8. 연관상품
        for (int i = 0; ; i++) {
            String id = params.get("relatedProducts[" + i + "].id");
            if (id == null) break;
            ProductRegisterRequestDTO.RelatedProductDTO rp = new ProductRegisterRequestDTO.RelatedProductDTO();
            rp.setId(Long.valueOf(id));
            rp.setType(params.get("relatedProducts[" + i + "].type"));
            dto.getRelatedProducts().add(rp);
        }

        // 9. 할인혜택
        for (int i = 0; ; i++) {
            String id = params.get("discounts[" + i + "].id");
            if (id == null) break;
            ProductRegisterRequestDTO.DiscountDTO d = new ProductRegisterRequestDTO.DiscountDTO();
            d.setId(Long.valueOf(id));
            d.setName(params.get("discounts[" + i + "].name"));
            d.setType(params.get("discounts[" + i + "].type"));
            d.setTerm(params.get("discounts[" + i + "].term"));
            d.setTarget(params.get("discounts[" + i + "].target"));
            d.setCouponPolicy(params.get("discounts[" + i + "].couponPolicy"));
            d.setStartDate(params.get("discounts[" + i + "].startDate"));
            d.setEndDate(params.get("discounts[" + i + "].endDate"));
            String active = params.get("discounts[" + i + "].active");
            d.setActive(active != null && (active.equals("true") || active.equals("on") || active.equals("1")));
            dto.getDiscounts().add(d);
        }

        // 10. 추가구성상품
        if (params.containsKey("bundleProductIds[]")) {
            for (String v : params.get("bundleProductIds[]").split(",")) {
                if (v != null && !v.isEmpty()) dto.getBundleProductIds().add(Long.valueOf(v));
            }
        }
        for (int i = 0; ; i++) {
            String id = params.get("bundleProductIds[" + i + "]");
            if (id == null) break;
            dto.getBundleProductIds().add(Long.valueOf(id));
        }

        // 11. 딜러별 등급 할인
        params.forEach((k, v) -> {
            if (k.startsWith("dealerDiscounts[")) {
                String grade = k.replaceAll("^dealerDiscounts\\[(.+)\\]$", "$1");
                dto.getDealerDiscounts().put(grade, v);
            }
        });

        return dto;
    }
}
