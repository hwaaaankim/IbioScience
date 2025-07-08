package com.dev.IbioScience.service.product;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.ProductRegisterRequestDTO;
import com.dev.IbioScience.model.product.Keyword;
import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.ProductAnswer;
import com.dev.IbioScience.model.product.ProductAnswerDetailImage;
import com.dev.IbioScience.model.product.ProductBundleItem;
import com.dev.IbioScience.model.product.ProductDetailImage;
import com.dev.IbioScience.model.product.ProductExtraField;
import com.dev.IbioScience.model.product.ProductGradeBenefit;
import com.dev.IbioScience.model.product.ProductImage;
import com.dev.IbioScience.model.product.ProductKeyword;
import com.dev.IbioScience.model.product.ProductOption;
import com.dev.IbioScience.model.product.ProductOptionGroup;
import com.dev.IbioScience.model.product.ProductQuestion;
import com.dev.IbioScience.model.product.RelatedProduct;
import com.dev.IbioScience.model.product.category.CategorySmall;
import com.dev.IbioScience.model.product.relation.SmallProductCategory;
import com.dev.IbioScience.model.product.status.DealerGrade;
import com.dev.IbioScience.model.product.status.DisplayStatus;
import com.dev.IbioScience.model.product.status.PriceSign;
import com.dev.IbioScience.model.product.status.ProductImageType;
import com.dev.IbioScience.model.product.status.QuestionType;
import com.dev.IbioScience.model.product.status.RelatedType;
import com.dev.IbioScience.model.product.status.SaleStatus;
import com.dev.IbioScience.repository.category.CategorySmallRepository;
import com.dev.IbioScience.repository.category.SmallProductCategoryRepository;
import com.dev.IbioScience.repository.product.ProductQuestionRepository;
import com.dev.IbioScience.repository.product.register.KeywordRepository;
import com.dev.IbioScience.repository.product.register.ProductAnswerDetailImageRepository;
import com.dev.IbioScience.repository.product.register.ProductAnswerRepository;
import com.dev.IbioScience.repository.product.register.ProductBundleItemRepository;
import com.dev.IbioScience.repository.product.register.ProductDetailImageRepository;
import com.dev.IbioScience.repository.product.register.ProductExtraFieldRepository;
import com.dev.IbioScience.repository.product.register.ProductGradeBenefitRepository;
import com.dev.IbioScience.repository.product.register.ProductImageRepository;
import com.dev.IbioScience.repository.product.register.ProductKeywordRepository;
import com.dev.IbioScience.repository.product.register.ProductOptionGroupRepository;
import com.dev.IbioScience.repository.product.register.ProductOptionRepository;
import com.dev.IbioScience.repository.product.register.ProductRepository;
import com.dev.IbioScience.repository.product.register.RelatedProductRepository;
import com.dev.IbioScience.utils.FileStorageUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductRegisterService {

	private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductDetailImageRepository productDetailImageRepository;
    private final ProductOptionGroupRepository productOptionGroupRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ProductExtraFieldRepository productExtraFieldRepository;
    private final ProductBundleItemRepository productBundleItemRepository;
    private final RelatedProductRepository relatedProductRepository;
//    private final ProductDiscountRepository productDiscountRepository;
//    private final ProductDiscountMappingRepository productDiscountMappingRepository;
    private final ProductGradeBenefitRepository productGradeBenefitRepository;
    private final KeywordRepository keywordRepository;
    private final ProductKeywordRepository productKeywordRepository;
    private final CategorySmallRepository categorySmallRepository;
    private final SmallProductCategoryRepository smallProductCategoryRepository;
    private final ProductQuestionRepository productQuestionRepository;
    private final ProductAnswerRepository productAnswerRepository;
    private final ProductAnswerDetailImageRepository productAnswerDetailImageRepository;
    private final FileStorageUtil fileStorageUtil;

    @Value("${spring.upload.path}")
    private String uploadBasePath;

    @Transactional
    public Long registerProduct(ProductRegisterRequestDTO req) throws IOException {
        // 1. Product 저장 (Enum 변환)
        Product product = new Product();
        product.setName(req.getProductName());
        product.setCode(req.getProductCode());
        product.setDisplayStatus(DisplayStatus.valueOf(req.getDisplayStatus()));
        product.setSaleStatus(SaleStatus.valueOf(req.getSaleStatus()));
        product.setDetailHtml(req.getDetailHtml());
        product = productRepository.save(product);

        // 2. 소분류-제품 N:N 매핑
        if (req.getCategorySmallIds() != null) {
            for (Long smallId : req.getCategorySmallIds()) {
                CategorySmall small = categorySmallRepository.findById(smallId)
                    .orElseThrow(() -> new IllegalArgumentException("소분류 없음: " + smallId));
                SmallProductCategory mapping = new SmallProductCategory();
                mapping.setSmall(small);
                mapping.setProduct(product);
                smallProductCategoryRepository.save(mapping);
            }
        }

        // 3. 대표 이미지
        if (req.getMainImage() != null && !req.getMainImage().isEmpty()) {
            saveProductImage(product, req.getMainImage(), ProductImageType.MAIN, 1);
        }
        // 4. 추가 이미지
        if (req.getSubImages() != null) {
            int sortOrder = 1;
            for (MultipartFile file : req.getSubImages()) {
                if (file != null && !file.isEmpty()) {
                    saveProductImage(product, file, ProductImageType.ADDITIONAL, sortOrder++);
                }
            }
        }

        // 5. 추가 입력필드
        if (req.getExtraFields() != null) {
            for (ProductRegisterRequestDTO.ExtraFieldDTO dto : req.getExtraFields()) {
                ProductExtraField field = new ProductExtraField();
                field.setProduct(product);
                field.setLabel(dto.getLabel());
                field.setValue(dto.getValue());
                productExtraFieldRepository.save(field);
            }
        }

        // 6. 옵션 그룹/옵션
        if (req.getOptionGroups() != null) {
            for (ProductRegisterRequestDTO.OptionGroupDTO groupDto : req.getOptionGroups()) {
                ProductOptionGroup group = new ProductOptionGroup();
                group.setProduct(product);
                group.setName(groupDto.getName());
                group = productOptionGroupRepository.save(group);
                if (groupDto.getOptions() != null) {
                    for (ProductRegisterRequestDTO.OptionDTO optionDto : groupDto.getOptions()) {
                        ProductOption option = new ProductOption();
                        option.setGroup(group);
                        option.setName(optionDto.getName());
                        option.setValue(optionDto.getValue());
                        option.setExtraPrice(
                            optionDto.getExtraPrice() != null && !optionDto.getExtraPrice().isEmpty()
                                ? new BigDecimal(optionDto.getExtraPrice())
                                : BigDecimal.ZERO
                        );
                        option.setSign(
                            optionDto.getSign() != null ? PriceSign.valueOf(optionDto.getSign()) : null
                        );
                        option.setSortOrder(optionDto.getSortOrder());
                        productOptionRepository.save(option);
                    }
                }
            }
        }

        // 7. 키워드
        if (req.getKeywords() != null) {
            for (String word : req.getKeywords()) {
                Keyword keyword = keywordRepository.findByWord(word)
                    .orElse(null);
                if (keyword == null) {
                    keyword = new Keyword();
                    keyword.setWord(word);
                    keyword = keywordRepository.save(keyword);
                }
                ProductKeyword pk = new ProductKeyword();
                pk.setProduct(product);
                pk.setKeyword(keyword);
                productKeywordRepository.save(pk);
            }
        }

        // 8. 연관상품
        if (req.getRelatedProducts() != null) {
            int sortOrder = 1;
            for (ProductRegisterRequestDTO.RelatedProductDTO dto : req.getRelatedProducts()) {
                Product relatedProduct = productRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("연관상품 없음: " + dto.getId()));
                RelatedProduct related = new RelatedProduct();
                related.setBaseProduct(product);
                related.setRelatedProduct(relatedProduct);
                related.setType(dto.getType() != null ? RelatedType.valueOf(dto.getType()) : null);
                related.setSortOrder(sortOrder++);
                relatedProductRepository.save(related);
            }
        }

        // 9. 할인혜택
//        if (req.getDiscounts() != null) {
//            for (ProductRegisterRequestDTO.DiscountDTO dto : req.getDiscounts()) {
//                ProductDiscount discount = productDiscountRepository.findById(dto.getId())
//                    .orElseThrow(() -> new IllegalArgumentException("할인정책 없음: " + dto.getId()));
//                ProductDiscountMapping mapping = new ProductDiscountMapping();
//                mapping.setProduct(product);
//                mapping.setDiscount(discount);
//                productDiscountMappingRepository.save(mapping);
//            }
//        }

        // 10. 추가구성상품
        if (req.getBundleProductIds() != null) {
            int sortOrder = 1;
            for (Long bundleId : req.getBundleProductIds()) {
                Product bundleProduct = productRepository.findById(bundleId)
                    .orElseThrow(() -> new IllegalArgumentException("구성상품 없음: " + bundleId));
                ProductBundleItem item = new ProductBundleItem();
                item.setMainProduct(product);
                item.setBundleProduct(bundleProduct);
                item.setSortOrder(sortOrder++);
                productBundleItemRepository.save(item);
            }
        }

        if (req.getDealerDiscounts() != null) {
            for (Map.Entry<String, String> entry : req.getDealerDiscounts().entrySet()) {
                ProductGradeBenefit benefit = new ProductGradeBenefit();
                benefit.setProduct(product);
                benefit.setDealerGrade(DealerGrade.valueOf(entry.getKey())); // A, B, C, D
                benefit.setDiscountRate(new BigDecimal(entry.getValue()));
                productGradeBenefitRepository.save(benefit);
            }
        }

        // 12. 공통표시항목(질문/에디터/파일)
        // (1) 텍스트/셀렉트/에디터 타입
        if (req.getDisplayOptions() != null) {
            for (Map.Entry<String, String> entry : req.getDisplayOptions().entrySet()) {
                String key = entry.getKey();
                ProductQuestion question;
                if (key.startsWith("question_")) {
                    try {
                        Long questionId = Long.parseLong(key.replace("question_", ""));
                        question = productQuestionRepository.findById(questionId)
                            .orElseThrow(() -> new IllegalArgumentException("질문 없음(id): " + questionId));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("질문 key 포맷 오류: " + key, e);
                    }
                } else {
                    question = productQuestionRepository.findByLabel(key)
                        .orElseThrow(() -> new IllegalArgumentException("질문 없음(label): " + key));
                }
                ProductAnswer answer = new ProductAnswer();
                answer.setProduct(product);
                answer.setQuestion(question);
                answer.setValue(entry.getValue());
                productAnswerRepository.save(answer);
            }
        }
        // (2) 파일 타입만 별도 저장
        if (req.getDisplayOptionFiles() != null) {
            for (Map.Entry<String, MultipartFile> entry : req.getDisplayOptionFiles().entrySet()) {
                MultipartFile file = entry.getValue();
                if (file != null && !file.isEmpty()) {
                    String key = entry.getKey();
                    ProductQuestion question;
                    if (key.startsWith("question_")) {
                        try {
                            Long questionId = Long.parseLong(key.replace("question_", ""));
                            question = productQuestionRepository.findById(questionId)
                                .orElseThrow(() -> new IllegalArgumentException("질문 없음(id): " + questionId));
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("질문 key 포맷 오류: " + key, e);
                        }
                    } else {
                        question = productQuestionRepository.findByLabel(key)
                            .orElseThrow(() -> new IllegalArgumentException("질문 없음(label): " + key));
                    }
                    if (question.getType() == QuestionType.FILE) {
                        String savedPath = saveDisplayOptionFile(product, question, file);
                        ProductAnswer answer = new ProductAnswer();
                        answer.setProduct(product);
                        answer.setQuestion(question);
                        answer.setFileUrl(savedPath);
                        answer.setPath(savedPath);
                        answer.setFileName(file.getOriginalFilename());
                        productAnswerRepository.save(answer);
                    }
                }
            }
        }

        return product.getId();
    }

    private ProductImage saveProductImage(Product product, MultipartFile file, ProductImageType type, int sortOrder) {
        try {
            String filePath = fileStorageUtil.save(file, uploadBasePath + "/product/" + product.getId() + "/images");
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setType(type);
            image.setPath(filePath);
            image.setFileName(file.getOriginalFilename());
            image.setSortOrder(sortOrder);
            return productImageRepository.save(image);
        } catch (Exception e) {
            throw new RuntimeException("대표/추가 이미지 저장 실패", e);
        }
    }

    /**
     * 공통질문 FILE 타입 파일 저장
     * @throws IOException 
     */
    private String saveDisplayOptionFile(Product product, ProductQuestion question, MultipartFile file) throws IOException {
        if (question.getType() != QuestionType.FILE) {
            throw new IllegalArgumentException("FILE 타입 질문이 아님");
        }
        String dir = uploadBasePath + "/product/" + product.getId() + "/question_" + question.getId();
        return fileStorageUtil.save(file, dir); // 예: 실제 파일 저장 후 경로 리턴
    }

    /**
     * 임시 에디터 이미지 실제 폴더로 이동 및 DB 저장/HTML src 치환 (폴더구조 완전 반영)
     * @param productId 상품ID
     * @param type "detailHtml" 또는 "question"
     * @param key "detailHtml" 또는 "question_[답변ID]"
     * @param html 원본 HTML
     * @param tempImgList 임시 이미지 웹 경로 리스트
     * @return 실제 저장 후 HTML(이미지 src 변환)
     */
    @Transactional
    public String moveEditorImages(Long productId, String type, String key, String html, List<String> tempImgList) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("상품 없음: " + productId));

        String targetDir;
        List<String> newUrls = new ArrayList<>();

        if ("detailHtml".equals(type) && "detailHtml".equals(key)) {
            targetDir = uploadBasePath + "/product/" + productId + "/detail";
        } else if ("question".equals(type) && key != null && key.startsWith("question_")) {
            String answerIdStr = key.replace("question_", "");
            targetDir = uploadBasePath + "/product/" + productId + "/common/editor/" + answerIdStr;
        } else {
            throw new IllegalArgumentException("지원하지 않는 type/key: " + type + ", " + key);
        }

        File dir = new File(targetDir);
        if (!dir.exists()) dir.mkdirs();

        for (String tempImgUrl : tempImgList) {
            String relativePath = tempImgUrl.replaceFirst("/upload/", "");
            File tempFile = new File(uploadBasePath, relativePath);

            String fileName = tempImgUrl.substring(tempImgUrl.lastIndexOf('/') + 1);
            File targetFile = new File(dir, fileName);

            try {
                Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                throw new RuntimeException("임시파일 이동 실패: " + tempFile, e);
            }

            String webPath;
            if ("detailHtml".equals(type)) {
                webPath = "/upload/product/" + productId + "/detail/" + fileName;
                // 상세 이미지 DB 저장
                ProductDetailImage di = new ProductDetailImage();
                di.setProduct(product);
                di.setUrl(webPath);
                di.setPath(targetFile.getAbsolutePath());
                di.setFileName(fileName);
                di.setUploadedAt(LocalDateTime.now());
                productDetailImageRepository.save(di);
            } else {
                // 반드시 답변ID로 연결
                String answerIdStr = key.replace("question_", "");
                webPath = "/upload/product/" + productId + "/common/editor/" + answerIdStr + "/" + fileName;
                ProductAnswer answer = productAnswerRepository.findById(Long.valueOf(answerIdStr))
                    .orElseThrow(() -> new IllegalArgumentException("답변 없음: " + answerIdStr));
                ProductAnswerDetailImage ai = new ProductAnswerDetailImage();
                ai.setAnswer(answer);
                ai.setUrl(webPath);
                ai.setPath(targetFile.getAbsolutePath());
                ai.setFileName(fileName);
                ai.setUploadedAt(LocalDateTime.now());
                productAnswerDetailImageRepository.save(ai);
            }
            newUrls.add(webPath);
        }

        // HTML 내 src 치환
        String newHtml = html;
        for (int i = 0; i < tempImgList.size(); i++) {
            newHtml = newHtml.replace(tempImgList.get(i), newUrls.get(i));
        }

        // DB 컬럼 업데이트
        if ("detailHtml".equals(type)) {
            product.setDetailHtml(newHtml);
            productRepository.save(product);
        } else {
            String answerIdStr = key.replace("question_", "");
            ProductAnswer answer = productAnswerRepository.findById(Long.valueOf(answerIdStr))
                .orElseThrow(() -> new IllegalArgumentException("답변 없음: " + answerIdStr));
            answer.setValue(newHtml);
            productAnswerRepository.save(answer);
        }
        return newHtml;
    }
    
    /**
     * 에디터 이미지 임시 업로드 (폴더 구조: /upload/temp/yyyyMMdd/detailHtml/ 또는 /upload/temp/yyyyMMdd/question_{id}/ )
     * @param files 이미지 파일 리스트
     * @param type "detailHtml" 또는 "question"
     * @param key type이 question일 때 "question_1" 등 질문별 식별자
     * @return 업로드된 파일들의 웹 URL 리스트
     */
    public List<String> uploadEditorImages(List<MultipartFile> files, String type, String key) {
        List<String> urlList = new ArrayList<>();
        String dateStr = LocalDate.now().toString().replace("-", "");

        // 폴더 구조 결정
        String subDir;
        if ("detailHtml".equals(type)) {
            subDir = "detailHtml";
        } else if ("question".equals(type) && key != null && key.startsWith("question_")) {
            subDir = key; // 예: question_1
        } else {
            subDir = "etc";
        }

        Path tempDir = Paths.get(uploadBasePath, "temp", dateStr, subDir);
        File dir = tempDir.toFile();
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("임시 이미지 폴더 생성 실패: " + tempDir);
            }
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String origName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            String newName = UUID.randomUUID().toString().replace("-", "") + "_" + origName;
            Path savePath = tempDir.resolve(newName);

            try {
                file.transferTo(savePath);
            } catch (IOException e) {
                throw new RuntimeException("이미지 저장 실패: " + origName, e);
            }

            // 웹 URL: /upload/temp/yyyyMMdd/detailHtml/파일명 또는 /upload/temp/yyyyMMdd/question_1/파일명
            String url = "/upload/temp/" + dateStr + "/" + subDir + "/" + newName;
            urlList.add(url);
        }
        return urlList;
    }


}
