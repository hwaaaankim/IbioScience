package com.dev.IbioScience.service.product;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.productDetail.ProductUpdateRequestDTO;
import com.dev.IbioScience.enums.product.DealerGrade;
import com.dev.IbioScience.enums.product.DisplayStatus;
import com.dev.IbioScience.enums.product.PriceExposeTarget;
import com.dev.IbioScience.enums.product.PriceSign;
import com.dev.IbioScience.enums.product.ProductImageType;
import com.dev.IbioScience.enums.product.ProductNewState;
import com.dev.IbioScience.enums.product.QuestionType;
import com.dev.IbioScience.enums.product.RelatedType;
import com.dev.IbioScience.enums.product.SaleStatus;
import com.dev.IbioScience.model.product.Brand;
import com.dev.IbioScience.model.product.InternalCategorySmall;
import com.dev.IbioScience.model.product.Keyword;
import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.ProductAnswer;
import com.dev.IbioScience.model.product.ProductAnswerDetailImage;
import com.dev.IbioScience.model.product.ProductBundleItem;
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
import com.dev.IbioScience.repository.product.register.ProductAnswerRepository;
import com.dev.IbioScience.repository.product.register.ProductBundleItemRepository;
import com.dev.IbioScience.repository.product.register.ProductExtraFieldRepository;
import com.dev.IbioScience.repository.product.register.ProductGradeBenefitRepository;
import com.dev.IbioScience.repository.product.register.ProductImageRepository;
import com.dev.IbioScience.repository.product.register.ProductKeywordRepository;
import com.dev.IbioScience.repository.product.register.ProductOptionGroupRepository;
import com.dev.IbioScience.repository.product.register.ProductOptionRepository;
import com.dev.IbioScience.repository.product.register.ProductRepository;
import com.dev.IbioScience.repository.product.register.RelatedProductRepository;
import com.dev.IbioScience.utils.FileStorageUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductUpdateService {

	private final ProductRepository productRepository;
	private final BrandRepository brandRepository;
	private final CategorySmallRepository categorySmallRepository;
	private final SmallProductCategoryRepository smallProductCategoryRepository;
	private final ProductImageRepository productImageRepository;
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
	private final ProductPromotionRepository productPromotionRepository;
	private final ProductPromotionMappingRepository productPromotionMappingRepository;
	private final InternalCategorySmallRepository internalCategorySmallRepository;
	private final FileStorageUtil fileStorageUtil;

	@PersistenceContext
	private EntityManager em;

	@Value("${spring.upload.path}")
	private String uploadBasePath;

	// ===== 공용: 등록 서비스의 saveProductImage 재사용을 위해 동일 로직 구현 =====
	private ProductImage saveProductImage(Product product, MultipartFile file, ProductImageType type, int sortOrder) {
		String subDir;
		if (type == ProductImageType.MAIN) {
			subDir = "/product/" + product.getId() + "/rep";
		} else {
			subDir = "/product/" + product.getId() + "/images";
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

	private String toPublicUrl(String absoluteSavedPath) {
		String base = new File(uploadBasePath).getAbsolutePath().replace("\\", "/");
		String abs = new File(absoluteSavedPath).getAbsolutePath().replace("\\", "/");
		String rel = abs.replace(base, "");
		if (!rel.startsWith("/"))
			rel = "/" + rel;
		return ("/upload" + rel).replaceAll("/+", "/");
	}

	// ===== 실제 업데이트 =====
	@Transactional
	public void updateProduct(Long productId, ProductUpdateRequestDTO req) throws IOException {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new IllegalArgumentException("상품 없음: " + productId));

		// ========== 1) 기본 필드 ==========
		if (StringUtils.hasText(req.getProductName()))
			product.setName(req.getProductName());
		if (StringUtils.hasText(req.getProductCode()))
			product.setCode(req.getProductCode());
		if (StringUtils.hasText(req.getDisplayStatus()))
			product.setDisplayStatus(DisplayStatus.valueOf(req.getDisplayStatus()));
		if (StringUtils.hasText(req.getSaleStatus()))
			product.setSaleStatus(SaleStatus.valueOf(req.getSaleStatus()));

		product.setDetailHtml(req.getDetailHtml());

		product.setManufacturerText(req.getManufacturerText());
		product.setSupplierText(req.getSupplierText());

		if (req.getBrandId() != null) {
			Brand brand = brandRepository.findById(req.getBrandId())
					.orElseThrow(() -> new IllegalArgumentException("브랜드 없음: " + req.getBrandId()));
			product.setBrand(brand);
		} else {
			product.setBrand(null);
		}

		product.setManufacturedAt(req.getManufacturedAt());
		product.setExpiredAt(req.getExpiredAt());
		product.setSummaryDescription(req.getSummaryDescription());
		product.setShortDescription(req.getShortDescription());
		product.setInternalProductCode(req.getInternalProductCode());

		if (req.getConsumerPrice() != null)
			product.setConsumerPrice(req.getConsumerPrice());
		else
			product.setConsumerPrice(null);

		if (req.getSalePrice() != null)
			product.setSalePrice(req.getSalePrice());
		else
			product.setSalePrice(null);

		if (StringUtils.hasText(req.getPriceExposeTarget()))
			product.setPriceExposeTarget(PriceExposeTarget.valueOf(req.getPriceExposeTarget()));
		product.setUsePriceReplacementText(Boolean.TRUE.equals(req.getUsePriceReplacementText()));
		product.setPriceReplacementText(req.getPriceReplacementText());

		if (req.getRewardRate() != null)
			product.setRewardRate(req.getRewardRate());
		else
			product.setRewardRate(null);

		product.setValidFrom(req.getValidFrom());
		product.setValidTo(req.getValidTo());

		product.setUseRelatedProducts(Boolean.TRUE.equals(req.getUseRelatedProducts()));
		product.setUseBundleItems(Boolean.TRUE.equals(req.getUseBundleItems()));

		if (req.getInternalCategorySmallId() != null) {
			InternalCategorySmall ics = internalCategorySmallRepository.findById(req.getInternalCategorySmallId())
					.orElseThrow(() -> new IllegalArgumentException("내부 소분류 없음: " + req.getInternalCategorySmallId()));
			product.setInternalCategorySmall(ics);
		} else {
			product.setInternalCategorySmall(null);
		}

		if (StringUtils.hasText(req.getNewState()))
			product.setNewState(ProductNewState.valueOf(req.getNewState()));
		else
			product.setNewState(null);

		product.setUseIconPeriod(Boolean.TRUE.equals(req.getUseIconPeriod()));
		product.setIconStartDate(req.getIconStartDate());
		product.setIconEndDate(req.getIconEndDate());

		product.setUpdatedAt(LocalDateTime.now());
		productRepository.save(product);

		// ========== 2) 외부 카테고리(대입력: 전체 재구성) ==========
		em.createQuery("delete from SmallProductCategory spc where spc.product = :p").setParameter("p", product)
				.executeUpdate();
		if (req.getCategorySmallIds() != null) {
			for (Long sid : req.getCategorySmallIds()) {
				CategorySmall small = categorySmallRepository.findById(sid)
						.orElseThrow(() -> new IllegalArgumentException("소분류 없음: " + sid));
				SmallProductCategory m = new SmallProductCategory();
				m.setSmall(small);
				m.setProduct(product);
				smallProductCategoryRepository.save(m);
			}
		}

		// ========== 3) 대표/추가 이미지 ==========
		// 3-1) 대표
		String mainAction = (req.getMainImageAction() == null) ? "KEEP" : req.getMainImageAction();
		if ("DELETE".equalsIgnoreCase(mainAction) || "REPLACE".equalsIgnoreCase(mainAction)) {
			// 기존 대표 이미지들 제거
			List<ProductImage> mains = em
					.createQuery("select i from ProductImage i where i.product = :p and i.type = :t",
							ProductImage.class)
					.setParameter("p", product).setParameter("t", ProductImageType.MAIN).getResultList();
			for (ProductImage mi : mains) {
				deletePhysicalFileSafe(mi.getPath());
				productImageRepository.delete(mi);
			}
		}
		if ("REPLACE".equalsIgnoreCase(mainAction) && req.getMainImage() != null && !req.getMainImage().isEmpty()) {
			saveProductImage(product, req.getMainImage(), ProductImageType.MAIN, 1);
		}

		// 3-2) 추가 이미지: URL로 특정 삭제 + 신규 추가
		if (req.getSubImageDeleteUrls() != null && !req.getSubImageDeleteUrls().isEmpty()) {
			List<ProductImage> dels = em
					.createQuery("select i from ProductImage i where i.product = :p and i.type = :t and i.url in :urls",
							ProductImage.class)
					.setParameter("p", product).setParameter("t", ProductImageType.ADDITIONAL)
					.setParameter("urls", req.getSubImageDeleteUrls()).getResultList();
			for (ProductImage pi : dels) {
				deletePhysicalFileSafe(pi.getPath());
				productImageRepository.delete(pi);
			}
		}
		if (req.getSubImages() != null && !req.getSubImages().isEmpty()) {
			// 마지막 sort 다음부터
			Integer maxSort = em.createQuery(
					"select coalesce(max(i.sortOrder),0) from ProductImage i where i.product = :p and i.type = :t",
					Integer.class).setParameter("p", product).setParameter("t", ProductImageType.ADDITIONAL)
					.getSingleResult();
			int sort = maxSort == null ? 0 : maxSort;
			for (MultipartFile f : req.getSubImages()) {
				if (f != null && !f.isEmpty()) {
					saveProductImage(product, f, ProductImageType.ADDITIONAL, ++sort);
				}
			}
		}

		// ========== 4) 아이콘 ==========
		String iconAction = (req.getIconImageAction() == null) ? "KEEP" : req.getIconImageAction();
		if ("DELETE".equalsIgnoreCase(iconAction) || "REPLACE".equalsIgnoreCase(iconAction)) {
			// 기존 아이콘 파일 제거 + 필드 초기화
			deletePhysicalFileSafe(product.getIconPath());
			product.setIconPath(null);
			product.setIconFileName(null);
			product.setIconUrl(null);
		}
		if ("REPLACE".equalsIgnoreCase(iconAction) && req.getIconImage() != null && !req.getIconImage().isEmpty()) {
			String dir = uploadBasePath + "/product/" + product.getId() + "/icon";
			String savedPath = fileStorageUtil.save(req.getIconImage(), dir);
			String fileName = req.getIconImage().getOriginalFilename();
			String url = toPublicUrl(savedPath);
			product.setIconPath(savedPath);
			product.setIconFileName(fileName);
			product.setIconUrl(url);
		}
		productRepository.save(product);

		// ========== 5) 추가 입력필드(전체 재구성) ==========
		em.createQuery("delete from ProductExtraField ef where ef.product = :p").setParameter("p", product)
				.executeUpdate();
		if (req.getExtraFields() != null) {
			for (ProductUpdateRequestDTO.ExtraFieldDTO f : req.getExtraFields()) {
				if (!StringUtils.hasText(f.getLabel()) && !StringUtils.hasText(f.getValue()))
					continue;
				ProductExtraField ef = new ProductExtraField();
				ef.setProduct(product);
				ef.setLabel(f.getLabel());
				ef.setValue(f.getValue());
				productExtraFieldRepository.save(ef);
			}
		}

		// ========== 6) 옵션(전체 재구성) ==========
		// 옵션 항목부터 삭제
		em.createQuery("delete from ProductOption o where o.group.id in "
				+ "(select g.id from ProductOptionGroup g where g.product = :p)").setParameter("p", product)
				.executeUpdate();
		em.createQuery("delete from ProductOptionGroup g where g.product = :p").setParameter("p", product)
				.executeUpdate();

		if (req.getOptionGroups() != null) {
			int gsort = 1;
			for (ProductUpdateRequestDTO.OptionGroupDTO g : req.getOptionGroups()) {
				if (!StringUtils.hasText(g.getName()))
					continue;
				ProductOptionGroup og = new ProductOptionGroup();
				og.setProduct(product);
				og.setName(g.getName());
				og.setSortOrder(g.getSortOrder() != null ? g.getSortOrder() : gsort++);
				og = productOptionGroupRepository.save(og);

				int osort = 1;
				if (g.getOptions() != null) {
					for (ProductUpdateRequestDTO.OptionDTO o : g.getOptions()) {
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

		// ========== 7) 키워드(전체 재구성) ==========
		em.createQuery("delete from ProductKeyword pk where pk.product = :p").setParameter("p", product)
				.executeUpdate();
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

		// ========== 8) 번들/관련(전체 재구성) ==========
		em.createQuery("delete from ProductBundleItem b where b.mainProduct = :p").setParameter("p", product)
				.executeUpdate();
		if (Boolean.TRUE.equals(req.getUseBundleItems()) && req.getBundleProducts() != null) {
			for (ProductUpdateRequestDTO.BundleProductDTO b : req.getBundleProducts()) {
				Product bundle = productRepository.findById(b.getId())
						.orElseThrow(() -> new IllegalArgumentException("구성상품 없음: " + b.getId()));
				ProductBundleItem item = new ProductBundleItem();
				item.setMainProduct(product);
				item.setBundleProduct(bundle);
				item.setSortOrder(b.getSortOrder() != null ? b.getSortOrder() : 0);
				productBundleItemRepository.save(item);
			}
		}

		// 관련상품은 상호/일방 모두 재구성: 기존 양방향도 일괄 삭제
		em.createQuery("delete from RelatedProduct r where r.baseProduct = :p or r.relatedProduct = :p")
				.setParameter("p", product).executeUpdate();
		if (Boolean.TRUE.equals(req.getUseRelatedProducts()) && req.getRelatedProducts() != null) {
			for (ProductUpdateRequestDTO.RelatedProductDTO r : req.getRelatedProducts()) {
				Product rel = productRepository.findById(r.getId())
						.orElseThrow(() -> new IllegalArgumentException("연관상품 없음: " + r.getId()));
				RelatedType type = StringUtils.hasText(r.getType()) ? RelatedType.valueOf(r.getType())
						: RelatedType.RECIPROCAL;
				upsertRelated(product, rel, type, r.getSortOrder() != null ? r.getSortOrder() : 0);
				if (type == RelatedType.RECIPROCAL) {
					upsertRelated(rel, product, type, r.getSortOrder() != null ? r.getSortOrder() : 0);
				}
			}
		}

		// ========== 9) 프로모션(동기화) ==========
		// 현재 매핑 조회
		List<ProductPromotionMapping> curMaps = em
				.createQuery("select m from ProductPromotionMapping m where m.product = :p",
						ProductPromotionMapping.class)
				.setParameter("p", product).getResultList();
		Set<Long> newIds = req.getDiscounts() == null ? Set.of()
				: req.getDiscounts().stream().map(ProductUpdateRequestDTO.DiscountDTO::getId)
						.collect(Collectors.toSet());
		// 삭제 대상
		for (ProductPromotionMapping m : curMaps) {
			if (m.getPromotion() == null || !newIds.contains(m.getPromotion().getId())) {
				productPromotionMappingRepository.delete(m);
			}
		}
		// 추가 대상
		if (req.getDiscounts() != null) {
			Set<Long> curIds = curMaps.stream().filter(x -> x.getPromotion() != null).map(x -> x.getPromotion().getId())
					.collect(Collectors.toSet());
			for (ProductUpdateRequestDTO.DiscountDTO d : req.getDiscounts()) {
				if (!curIds.contains(d.getId())) {
					Promotion promo = productPromotionRepository.findById(d.getId())
							.orElseThrow(() -> new IllegalArgumentException("프로모션 없음: " + d.getId()));
					ProductPromotionMapping m = new ProductPromotionMapping();
					m.setProduct(product);
					m.setPromotion(promo);
					productPromotionMappingRepository.save(m);
				}
			}
		}

		// ========== 10) 딜러 할인율(전체 재구성) ==========
		em.createQuery("delete from ProductGradeBenefit b where b.product = :p").setParameter("p", product)
				.executeUpdate();
		if (req.getDealerDiscounts() != null) {
			for (Map.Entry<String, String> e : req.getDealerDiscounts().entrySet()) {
				String grade = e.getKey();
				String val = e.getValue();
				if (!StringUtils.hasText(val))
					continue;
				ProductGradeBenefit b = new ProductGradeBenefit();
				b.setProduct(product);
				b.setDealerGrade(DealerGrade.valueOf(grade));
				b.setDiscountRate(new BigDecimal(val));
				productGradeBenefitRepository.save(b);
			}
		}
		// ========== 11) 공통표시항목(질문/답변) ==========
		List<ProductQuestion> questions = productQuestionRepository.findAll();

		// === (변경) 에디터형(CKEDITOR)과 파일형(FILE)은 update에서 건드리지 않도록 제외 집합 구성
		final java.util.Set<QuestionType> EXCLUDED_TYPES = java.util.EnumSet.of(QuestionType.FILE,
				QuestionType.CKEDITOR);

		/*
		 * 11-1) 에디터형/파일형을 제외한 기존 답변 삭제 (자식부터) - editor(CKEDITOR)는 moveEditorImages()에서
		 * 관리되므로 이곳에서 절대 삭제/갱신하지 않음 - file(FILE)은 아래 11-3 단계에서 별도 처리
		 */
		List<ProductAnswer> answersToDelete = em
				.createQuery("select a from ProductAnswer a join a.question q "
						+ "where a.product = :p and q.type not in :excluded", ProductAnswer.class)
				.setParameter("p", product).setParameter("excluded", EXCLUDED_TYPES).getResultList();

		deleteAnswersAndChildren(answersToDelete);

		/*
		 * 11-2) 요청 값으로 '에디터형/파일형을 제외한' 비파일형 답변 재삽입 - CKEDITOR는 moveEditorImages()로,
		 * FILE은 11-3에서 다룸
		 */
		if (req.getDisplayOptions() != null) {
			for (Map.Entry<String, String> en : req.getDisplayOptions().entrySet()) {
				String key = en.getKey(); // "question_{id}"
				Long qid = parseQuestionKey(key);
				ProductQuestion q = findQuestion(questions, qid);

				// === (변경) 에디터/파일형은 완전히 제외
				if (EXCLUDED_TYPES.contains(q.getType())) {
					continue;
				}

				ProductAnswer a = new ProductAnswer();
				a.setProduct(product);
				a.setQuestion(q);
				a.setValue(en.getValue());
				productAnswerRepository.save(a);
			}
		}

		/*
		 * 11-3) 파일형(KEEP/DELETE/REPLACE) - 기존 로직 유지, 다만 널가드 추가
		 */
		Map<String, String> fileActions = (req.getDisplayOptionFileActions() != null)
				? req.getDisplayOptionFileActions()
				: java.util.Collections.emptyMap();

		for (ProductQuestion q : questions) {
			if (q.getType() != QuestionType.FILE) {
				continue;
			}

			String baseKey = "question_" + q.getId();
			String act = fileActions.getOrDefault(baseKey + "_fileAction", "KEEP");

			// 기존 파일형 답변 로드
			List<ProductAnswer> fileAnswers = em
					.createQuery("select a from ProductAnswer a where a.product = :p and a.question = :q",
							ProductAnswer.class)
					.setParameter("p", product).setParameter("q", q).getResultList();

			if ("DELETE".equalsIgnoreCase(act) || "REPLACE".equalsIgnoreCase(act)) {
				// 파일형도 동일하게: 자식 -> flush -> 부모 (물리파일 포함) 일괄 삭제
				deleteAnswersAndChildren(fileAnswers);
			}

			if ("REPLACE".equalsIgnoreCase(act)) {
				List<MultipartFile> newFiles = (req.getDisplayOptionFiles() != null)
						? req.getDisplayOptionFiles().get(baseKey)
						: null;
				if (newFiles != null) {
					for (MultipartFile f : newFiles) {
						if (f == null || f.isEmpty())
							continue;

						String dir = uploadBasePath + "/product/" + product.getId() + "/question_" + q.getId();
						String savedPath = fileStorageUtil.save(f, dir);

						ProductAnswer a = new ProductAnswer();
						a.setProduct(product);
						a.setQuestion(q);
						a.setFileUrl(toPublicUrl(savedPath));
						a.setPath(savedPath);
						a.setFileName(f.getOriginalFilename());
						productAnswerRepository.save(a);
					}
				}
			}
			// KEEP은 아무 것도 하지 않음
		}
	}

	private void deleteAnswersAndChildren(Collection<ProductAnswer> answers) {
		if (answers == null || answers.isEmpty())
			return;

		// 1) 디테일 이미지 물리파일 삭제
		List<ProductAnswerDetailImage> imgs = em
				.createQuery("select i from ProductAnswerDetailImage i where i.answer in :answers",
						ProductAnswerDetailImage.class)
				.setParameter("answers", answers).getResultList();
		for (ProductAnswerDetailImage img : imgs) {
			deletePhysicalFileSafe(img.getPath());
		}

		// 2) 답변 자체의 물리파일 방어적 삭제
		for (ProductAnswer a : answers) {
			deletePhysicalFileSafe(a.getPath());
		}

		// 3) DB 삭제 (자식 -> 부모, 순서 보장)
		em.createQuery("delete from ProductAnswerDetailImage i where i.answer in :answers")
				.setParameter("answers", answers).executeUpdate();
		em.flush(); // FK 제약 순서 보장

		em.createQuery("delete from ProductAnswer a where a in :answers").setParameter("answers", answers)
				.executeUpdate();
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

	private void deletePhysicalFileSafe(String path) {
		if (path == null || path.isBlank())
			return;
		try {
			Files.deleteIfExists(Path.of(path));
		} catch (Exception ignore) {
		}
	}

	private Long parseQuestionKey(String key) {
		if (key != null && key.startsWith("question_")) {
			return Long.parseLong(key.substring("question_".length()));
		}
		throw new IllegalArgumentException("질문 key 포맷 오류: " + key);
	}

	private ProductQuestion findQuestion(List<ProductQuestion> list, Long id) {
		return list.stream().filter(q -> Objects.equals(q.getId(), id)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("질문 없음(id): " + id));
	}
}