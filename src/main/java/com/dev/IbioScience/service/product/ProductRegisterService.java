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
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.productRegister.ProductRegisterRequestDTO;
import com.dev.IbioScience.model.product.Brand;
import com.dev.IbioScience.model.product.InternalCategorySmall;
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
import com.dev.IbioScience.model.product.Promotion;
import com.dev.IbioScience.model.product.RelatedProduct;
import com.dev.IbioScience.model.product.category.CategorySmall;
import com.dev.IbioScience.model.product.enums.DealerGrade;
import com.dev.IbioScience.model.product.enums.DisplayStatus;
import com.dev.IbioScience.model.product.enums.PriceExposeTarget;
import com.dev.IbioScience.model.product.enums.PriceSign;
import com.dev.IbioScience.model.product.enums.ProductImageType;
import com.dev.IbioScience.model.product.enums.ProductNewState;
import com.dev.IbioScience.model.product.enums.ProductState;
import com.dev.IbioScience.model.product.enums.QuestionType;
import com.dev.IbioScience.model.product.enums.RelatedType;
import com.dev.IbioScience.model.product.enums.SaleStatus;
import com.dev.IbioScience.model.product.relation.ProductPromotionMapping;
import com.dev.IbioScience.model.product.relation.SmallProductCategory;
import com.dev.IbioScience.repository.category.CategorySmallRepository;
import com.dev.IbioScience.repository.category.SmallProductCategoryRepository;
import com.dev.IbioScience.repository.product.BrandRepository;
import com.dev.IbioScience.repository.product.InternalCategorySmallRepository;
import com.dev.IbioScience.repository.product.ProductPromotionMappingRepository;
import com.dev.IbioScience.repository.product.ProductPromotionRepository;
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
	private final BrandRepository brandRepository;
	private final CategorySmallRepository categorySmallRepository;
	private final SmallProductCategoryRepository smallProductCategoryRepository;
	private final ProductImageRepository productImageRepository;
	private final ProductDetailImageRepository productDetailImageRepository;
	private final ProductOptionGroupRepository productOptionGroupRepository;
	private final ProductOptionRepository productOptionRepository;
	private final ProductExtraFieldRepository productExtraFieldRepository;
	private final ProductBundleItemRepository productBundleItemRepository;
	private final RelatedProductRepository relatedProductRepository;
	private final ProductGradeBenefitRepository productGradeBenefitRepository;
	private final KeywordRepository keywordRepository;
	private final ProductKeywordRepository productKeywordRepository;
	private final ProductQuestionRepository productQuestionRepository;
	private final ProductAnswerRepository productAnswerRepository;
	private final ProductAnswerDetailImageRepository productAnswerDetailImageRepository;
	private final ProductPromotionRepository productPromotionRepository;
	private final ProductPromotionMappingRepository productPromotionMappingRepository;
	private final InternalCategorySmallRepository internalCategorySmallRepository;
	private final FileStorageUtil fileStorageUtil;

	@Value("${spring.upload.path}")
	private String uploadBasePath;

	@Transactional
	public Long registerProduct(ProductRegisterRequestDTO req) throws IOException {
		validate(req);

		Product product = new Product();
		product.setName(req.getProductName());
		product.setCode(req.getProductCode());
		product.setDisplayStatus(DisplayStatus.valueOf(req.getDisplayStatus()));
		product.setSaleStatus(SaleStatus.valueOf(req.getSaleStatus()));
		product.setDetailHtml(req.getDetailHtml());
		product.setManufacturerText(req.getManufacturerText());
		product.setSupplierText(req.getSupplierText());
		if (req.getBrandId() != null) {
			Brand brand = brandRepository.findById(req.getBrandId())
					.orElseThrow(() -> new IllegalArgumentException("브랜드 없음: " + req.getBrandId()));
			product.setBrand(brand);
		}
		product.setManufacturedAt(req.getManufacturedAt());
		product.setExpiredAt(req.getExpiredAt());
		product.setSummaryDescription(req.getSummaryDescription());
		product.setShortDescription(req.getShortDescription());
		product.setInternalProductCode(req.getInternalProductCode());
		if (req.getConsumerPrice() != null)
			product.setConsumerPrice(req.getConsumerPrice());
		if (req.getSalePrice() != null)
			product.setSalePrice(req.getSalePrice());
		if (StringUtils.hasText(req.getPriceExposeTarget()))
			product.setPriceExposeTarget(PriceExposeTarget.valueOf(req.getPriceExposeTarget()));
		product.setUsePriceReplacementText(Boolean.TRUE.equals(req.getUsePriceReplacementText()));
		product.setPriceReplacementText(req.getPriceReplacementText());
		if (req.getRewardRate() != null)
			product.setRewardRate(req.getRewardRate());
		product.setValidFrom(req.getValidFrom());
		product.setValidTo(req.getValidTo());
		product.setUseRelatedProducts(Boolean.TRUE.equals(req.getUseRelatedProducts()));
		product.setUseBundleItems(Boolean.TRUE.equals(req.getUseBundleItems()));
		if (req.getInternalCategorySmallId() != null) {
			InternalCategorySmall ics = internalCategorySmallRepository.findById(req.getInternalCategorySmallId())
					.orElseThrow(() -> new IllegalArgumentException("내부 소분류 없음: " + req.getInternalCategorySmallId()));
			product.setInternalCategorySmall(ics);
		}
		if (StringUtils.hasText(req.getNewState()))
			product.setNewState(ProductNewState.valueOf(req.getNewState()));
		product.setUseIconPeriod(Boolean.TRUE.equals(req.getUseIconPeriod()));
		product.setIconStartDate(req.getIconStartDate());
		product.setIconEndDate(req.getIconEndDate());
		product.setCreatedAt(LocalDateTime.now());
		product.setUpdatedAt(LocalDateTime.now());
		product.setState(ProductState.NORMAL);
		product = productRepository.save(product);

		if (req.getCategorySmallIds() != null) {
			for (Long smallId : req.getCategorySmallIds()) {
				CategorySmall small = categorySmallRepository.findById(smallId)
						.orElseThrow(() -> new IllegalArgumentException("소분류 없음: " + smallId));
				SmallProductCategory m = new SmallProductCategory();
				m.setSmall(small);
				m.setProduct(product);
				smallProductCategoryRepository.save(m);
			}
		}

		if (req.getMainImage() != null && !req.getMainImage().isEmpty()) {
			saveProductImage(product, req.getMainImage(), ProductImageType.MAIN, 1);
		}
		if (req.getSubImages() != null) {
			int sort = 1;
			for (MultipartFile f : req.getSubImages()) {
				if (f != null && !f.isEmpty())
					saveProductImage(product, f, ProductImageType.ADDITIONAL, sort++);
			}
		}

		if (req.getIconImage() != null && !req.getIconImage().isEmpty()) {
			String dir = uploadBasePath + "/product/" + product.getId() + "/icon";
			String savedPath = fileStorageUtil.save(req.getIconImage(), dir);
			String fileName = req.getIconImage().getOriginalFilename();
			String url = toPublicUrl(savedPath);
			product.setIconPath(savedPath);
			product.setIconFileName(fileName);
			product.setIconUrl(url);
			productRepository.save(product);
		}

		if (req.getExtraFields() != null) {
			for (ProductRegisterRequestDTO.ExtraFieldDTO f : req.getExtraFields()) {
				if (!StringUtils.hasText(f.getLabel()) && !StringUtils.hasText(f.getValue()))
					continue;
				ProductExtraField ef = new ProductExtraField();
				ef.setProduct(product);
				ef.setLabel(f.getLabel());
				ef.setValue(f.getValue());
				productExtraFieldRepository.save(ef);
			}
		}

		if (req.getOptionGroups() != null) {
			int gsort = 1;
			for (ProductRegisterRequestDTO.OptionGroupDTO g : req.getOptionGroups()) {
				if (!StringUtils.hasText(g.getName()))
					continue;
				ProductOptionGroup og = new ProductOptionGroup();
				og.setProduct(product);
				og.setName(g.getName());
				og.setSortOrder(g.getSortOrder() != null ? g.getSortOrder() : gsort++);
				og = productOptionGroupRepository.save(og);
				int osort = 1;
				if (g.getOptions() != null) {
					for (ProductRegisterRequestDTO.OptionDTO o : g.getOptions()) {
						if (!StringUtils.hasText(o.getName()))
							continue;
						ProductOption opt = new ProductOption();
						opt.setGroup(og);
						opt.setName(o.getName());
						opt.setValue(o.getValue());
						if (StringUtils.hasText(o.getExtraPrice()))
							opt.setExtraPrice(new BigDecimal(o.getExtraPrice()));
						else
							opt.setExtraPrice(BigDecimal.ZERO);
						if (StringUtils.hasText(o.getSign()))
							opt.setSign(PriceSign.valueOf(o.getSign()));
						opt.setSortOrder(o.getSortOrder() != null ? o.getSortOrder() : osort++);
						productOptionRepository.save(opt);
					}
				}
			}
		}

		if (req.getKeywords() != null) {
			for (String w : req.getKeywords()) {
				if (!StringUtils.hasText(w))
					continue;
				Keyword k = keywordRepository.findByWord(w).orElse(null);
				if (k == null) {
					k = new Keyword();
					k.setWord(w);
					k = keywordRepository.save(k);
				}
				ProductKeyword pk = new ProductKeyword();
				pk.setProduct(product);
				pk.setKeyword(k);
				productKeywordRepository.save(pk);
			}
		}

		if (Boolean.TRUE.equals(req.getUseBundleItems()) && req.getBundleProducts() != null) {
			for (ProductRegisterRequestDTO.BundleProductDTO b : req.getBundleProducts()) {
				Product bundle = productRepository.findById(b.getId())
						.orElseThrow(() -> new IllegalArgumentException("구성상품 없음: " + b.getId()));
				ProductBundleItem item = new ProductBundleItem();
				item.setMainProduct(product);
				item.setBundleProduct(bundle);
				item.setSortOrder(b.getSortOrder() != null ? b.getSortOrder() : 0);
				productBundleItemRepository.save(item);
			}
		}

		if (Boolean.TRUE.equals(req.getUseRelatedProducts()) && req.getRelatedProducts() != null) {
			for (ProductRegisterRequestDTO.RelatedProductDTO r : req.getRelatedProducts()) {
				Product rel = productRepository.findById(r.getId())
						.orElseThrow(() -> new IllegalArgumentException("연관상품 없음: " + r.getId()));
				RelatedType type = StringUtils.hasText(r.getType()) ? RelatedType.valueOf(r.getType())
						: RelatedType.RECIPROCAL;
				upsertRelated(product, rel, type, r.getSortOrder() != null ? r.getSortOrder() : 0);
				if (type == RelatedType.RECIPROCAL)
					upsertRelated(rel, product, type, r.getSortOrder() != null ? r.getSortOrder() : 0);
			}
		}

		if (req.getDiscounts() != null) {
			for (ProductRegisterRequestDTO.DiscountDTO d : req.getDiscounts()) {
				Promotion promo = productPromotionRepository.findById(d.getId())
						.orElseThrow(() -> new IllegalArgumentException("프로모션 없음: " + d.getId()));
				if (!productPromotionMappingRepository.existsByProductAndPromotion(product, promo)) {
					ProductPromotionMapping m = new ProductPromotionMapping();
					m.setProduct(product);
					m.setPromotion(promo);
					productPromotionMappingRepository.save(m);
				}
			}
		}

		if (req.getDealerDiscounts() != null) {
			for (Map.Entry<String, String> e : req.getDealerDiscounts().entrySet()) {
				if (!StringUtils.hasText(e.getValue()))
					continue;
				ProductGradeBenefit b = new ProductGradeBenefit();
				b.setProduct(product);
				b.setDealerGrade(DealerGrade.valueOf(e.getKey()));
				b.setDiscountRate(new BigDecimal(e.getValue()));
				productGradeBenefitRepository.save(b);
			}
		}

		if (req.getDisplayOptions() != null) {
			for (Map.Entry<String, String> en : req.getDisplayOptions().entrySet()) {
				String key = en.getKey();
				Long qid = parseQuestionKey(key);
				ProductQuestion question = productQuestionRepository.findById(qid)
						.orElseThrow(() -> new IllegalArgumentException("질문 없음(id): " + qid));
				if (question.getType() == QuestionType.FILE)
					continue;
				ProductAnswer a = new ProductAnswer();
				a.setProduct(product);
				a.setQuestion(question);
				a.setValue(en.getValue());
				productAnswerRepository.save(a);
			}
		}
		if (req.getDisplayOptionFiles() != null) {
			for (Map.Entry<String, MultipartFile> en : req.getDisplayOptionFiles().entrySet()) {
				String key = en.getKey();
				MultipartFile file = en.getValue();
				if (file == null || file.isEmpty())
					continue;
				Long qid = parseQuestionKey(key);
				ProductQuestion question = productQuestionRepository.findById(qid)
						.orElseThrow(() -> new IllegalArgumentException("질문 없음(id): " + qid));
				if (question.getType() != QuestionType.FILE)
					continue;
				String savedPath = saveDisplayOptionFile(product, question, file);
				ProductAnswer a = new ProductAnswer();
				a.setProduct(product);
				a.setQuestion(question);
				a.setFileUrl(toPublicUrl(savedPath));
				a.setPath(savedPath);
				a.setFileName(file.getOriginalFilename());
				productAnswerRepository.save(a);
			}
		}

		return product.getId();
	}

	private void validate(ProductRegisterRequestDTO req) {
		if (!StringUtils.hasText(req.getProductName()))
			throw new IllegalArgumentException("제품명 필수");
		if (!StringUtils.hasText(req.getProductCode()))
			throw new IllegalArgumentException("제품코드 필수");
		if (!StringUtils.hasText(req.getDisplayStatus()))
			throw new IllegalArgumentException("진열상태 필수");
		if (!StringUtils.hasText(req.getSaleStatus()))
			throw new IllegalArgumentException("판매상태 필수");
		if (req.getCategorySmallIds() == null || req.getCategorySmallIds().isEmpty())
			throw new IllegalArgumentException("카테고리 최소 1개 선택");
		if (Boolean.TRUE.equals(req.getUsePriceReplacementText())
				&& !StringUtils.hasText(req.getPriceReplacementText()))
			throw new IllegalArgumentException("가격대체문구 사용 시 문구 필수");
		if (Boolean.TRUE.equals(req.getUseIconPeriod())) {
			LocalDate s = req.getIconStartDate(), e = req.getIconEndDate();
			if (s == null || e == null || e.isBefore(s))
				throw new IllegalArgumentException("아이콘 기간 오류");
		}
		if (req.getOptionGroups() != null) {
			for (ProductRegisterRequestDTO.OptionGroupDTO g : req.getOptionGroups()) {
				if (!StringUtils.hasText(g.getName()))
					throw new IllegalArgumentException("옵션그룹명 필수");
				if (g.getOptions() != null) {
					for (ProductRegisterRequestDTO.OptionDTO o : g.getOptions()) {
						if (!StringUtils.hasText(o.getName()))
							throw new IllegalArgumentException("옵션명 필수");
					}
				}
			}
		}
	}

	public List<String> uploadEditorImages(List<MultipartFile> files, String type, String key) {
		List<String> urlList = new ArrayList<>();
		String dateStr = LocalDate.now().toString().replace("-", "");
		String subDir;
		if ("detailHtml".equals(type))
			subDir = "detailHtml";
		else if ("question".equals(type) && key != null && key.startsWith("question_"))
			subDir = key;
		else
			subDir = "etc";
		Path tempDir = Paths.get(uploadBasePath, "temp", dateStr, subDir);
		File dir = tempDir.toFile();
		if (!dir.exists() && !dir.mkdirs())
			throw new RuntimeException("임시 폴더 생성 실패");
		for (MultipartFile file : files) {
			if (file.isEmpty())
				continue;
			String orig = org.springframework.util.StringUtils
					.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
			String newName = UUID.randomUUID().toString().replace("-", "") + "_" + orig;
			Path savePath = tempDir.resolve(newName);
			try {
				file.transferTo(savePath);
			} catch (IOException e) {
				throw new RuntimeException("이미지 저장 실패: " + orig, e);
			}
			String url = "/upload/temp/" + dateStr + "/" + subDir + "/" + newName;
			urlList.add(url);
		}
		return urlList;
	}

	@Transactional
	public String moveEditorImages(Long productId, String type, String key, String html, List<String> tempImgList) {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new IllegalArgumentException("상품 없음: " + productId));
		String targetDir;
		List<String> newUrls = new ArrayList<>();

		if ("detailHtml".equals(type) && "detailHtml".equals(key)) {
			targetDir = uploadBasePath + "/product/" + productId + "/detail";
		} else if ("question".equals(type) && key != null && key.startsWith("question_")) {
			String idx = key.replace("question_", "");
			Long idNum = parseLongSafe(idx);
			ProductAnswer answer = findAnswerByKey(productId, idNum);
			targetDir = uploadBasePath + "/product/" + productId + "/common/editor/" + answer.getId();
		} else {
			throw new IllegalArgumentException("지원하지 않는 type/key");
		}

		File dir = new File(targetDir);
		if (!dir.exists())
			dir.mkdirs();

		for (String tempUrl : tempImgList) {
			String relative = tempUrl.replaceFirst("/upload/", "");
			File tempFile = new File(uploadBasePath, relative);
			String fileName = tempUrl.substring(tempUrl.lastIndexOf('/') + 1);
			File targetFile = new File(dir, fileName);
			try {
				Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (Exception e) {
				throw new RuntimeException("임시파일 이동 실패", e);
			}
			String webPath;
			if ("detailHtml".equals(type)) {
				webPath = "/upload/product/" + productId + "/detail/" + fileName;
				ProductDetailImage di = new ProductDetailImage();
				di.setProduct(product);
				di.setUrl(webPath);
				di.setPath(targetFile.getAbsolutePath());
				di.setFileName(fileName);
				di.setUploadedAt(LocalDateTime.now());
				di.setInUse(true);
				productDetailImageRepository.save(di);
			} else {
				String idx = key.replace("question_", "");
				Long idNum = parseLongSafe(idx);
				ProductAnswer answer = findAnswerByKey(productId, idNum);
				webPath = "/upload/product/" + productId + "/common/editor/" + answer.getId() + "/" + fileName;
				ProductAnswerDetailImage ai = new ProductAnswerDetailImage();
				ai.setAnswer(answer);
				ai.setUrl(webPath);
				ai.setPath(targetFile.getAbsolutePath());
				ai.setFileName(fileName);
				ai.setUploadedAt(LocalDateTime.now());
				ai.setInUse(true);
				productAnswerDetailImageRepository.save(ai);
			}
			newUrls.add(webPath);
		}

		String newHtml = html;
		for (int i = 0; i < tempImgList.size(); i++)
			newHtml = newHtml.replace(tempImgList.get(i), newUrls.get(i));

		if ("detailHtml".equals(type)) {
			product.setDetailHtml(newHtml);
			productRepository.save(product);
		} else {
			String idx = key.replace("question_", "");
			Long idNum = parseLongSafe(idx);
			ProductAnswer answer = findAnswerByKey(productId, idNum);
			answer.setValue(newHtml);
			productAnswerRepository.save(answer);
		}
		return newHtml;
	}

	private ProductImage saveProductImage(Product product, MultipartFile file, ProductImageType type, int sortOrder) {
	    String subDir;
	    if (type == ProductImageType.MAIN) {
	        subDir = "/product/" + product.getId() + "/rep"; // 대표 이미지
	    } else {
	        subDir = "/product/" + product.getId() + "/images"; // 추가 이미지
	    }

	    String filePath = fileStorageUtil.save(file, uploadBasePath + subDir);
	    ProductImage image = new ProductImage();
	    image.setProduct(product);
	    image.setType(type);
	    image.setPath(filePath);
	    image.setFileName(file.getOriginalFilename());
	    image.setUrl(toPublicUrl(filePath));
	    image.setSortOrder(sortOrder);
	    return productImageRepository.save(image);
	}


	private String saveDisplayOptionFile(Product product, ProductQuestion question, MultipartFile file)
			throws IOException {
		String dir = uploadBasePath + "/product/" + product.getId() + "/question_" + question.getId();
		return fileStorageUtil.save(file, dir);
	}

	private void upsertRelated(Product base, Product rel, RelatedType type, int sortOrder) {
		boolean exists = relatedProductRepository.existsByBaseProductAndRelatedProductAndType(base, rel, type);
		if (!exists) {
			RelatedProduct r = new RelatedProduct();
			r.setBaseProduct(base);
			r.setRelatedProduct(rel);
			r.setType(type);
			r.setSortOrder(sortOrder);
			relatedProductRepository.save(r);
		}
	}

	private Long parseQuestionKey(String key) {
		if (key.startsWith("question_"))
			return Long.parseLong(key.substring("question_".length()));
		throw new IllegalArgumentException("질문 key 포맷 오류: " + key);
	}

	// ProductRegisterService 내부
	private String toPublicUrl(String absoluteSavedPath) {
	    String base = new File(uploadBasePath).getAbsolutePath().replace("\\", "/");
	    String abs  = new File(absoluteSavedPath).getAbsolutePath().replace("\\", "/");

	    // uploadBasePath 이후의 상대경로 산출
	    String rel = abs.replace(base, "");
	    if (!rel.startsWith("/")) rel = "/" + rel;

	    // ★ 정적 리소스 핸들러와 일치하도록 '/upload' 접두어를 강제
	    return ("/upload" + rel).replaceAll("/+", "/");
	}


	private Long parseLongSafe(String s) {
		try {
			return Long.valueOf(s);
		} catch (Exception e) {
			return null;
		}
	}

	private ProductAnswer findAnswerByKey(Long productId, Long idNum) {
		if (idNum == null)
			throw new IllegalArgumentException("key 식별자 없음");
		Optional<ProductAnswer> byId = productAnswerRepository.findById(idNum);
		if (byId.isPresent())
			return byId.get();
		ProductQuestion q = productQuestionRepository.findById(idNum)
				.orElseThrow(() -> new IllegalArgumentException("질문/답변 식별 불가: " + idNum));
		return productAnswerRepository.findTopByProductIdAndQuestionIdOrderByIdAsc(productId, q.getId())
				.orElseThrow(() -> new IllegalArgumentException("해당 제품/질문에 대한 답변 없음"));
	}

}
