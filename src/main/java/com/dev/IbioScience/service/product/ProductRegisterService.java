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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.productRegister.ProductRegisterRequestDTO;
import com.dev.IbioScience.enums.auth.DealerGrade;
import com.dev.IbioScience.enums.product.DisplayStatus;
import com.dev.IbioScience.enums.product.PriceExposeTarget;
import com.dev.IbioScience.enums.product.PriceSign;
import com.dev.IbioScience.enums.product.ProductImageType;
import com.dev.IbioScience.enums.product.ProductNewState;
import com.dev.IbioScience.enums.product.ProductState;
import com.dev.IbioScience.enums.product.QuestionType;
import com.dev.IbioScience.enums.product.RelatedType;
import com.dev.IbioScience.enums.product.SaleStatus;
import com.dev.IbioScience.model.product.Brand;
import com.dev.IbioScience.model.product.InternalCategorySmall;
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
import com.dev.IbioScience.model.product.category.CategoryMedium;
import com.dev.IbioScience.model.product.category.CategorySmall;
import com.dev.IbioScience.model.product.relation.MediumSmallProductCategory;
import com.dev.IbioScience.model.product.relation.ProductPromotionMapping;
import com.dev.IbioScience.model.product.util.Keyword;
import com.dev.IbioScience.repository.category.CategoryMediumRepository;
import com.dev.IbioScience.repository.category.CategorySmallRepository;
import com.dev.IbioScience.repository.category.MediumSmallCategoryRepository;
import com.dev.IbioScience.repository.category.MediumSmallProductCategoryRepository;
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
	private final CategoryMediumRepository categoryMediumRepository;
	private final MediumSmallProductCategoryRepository mediumSmallProductCategoryRepository;
	private final MediumSmallCategoryRepository mediumSmallCategoryRepository;

	@Value("${spring.upload.path}")
	private String uploadBasePath;

	@PersistenceContext
	private EntityManager em;

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

		if (!CollectionUtils.isEmpty(req.getCategoryPaths())) {

		    for (ProductRegisterRequestDTO.CategoryPathDTO path : req.getCategoryPaths()) {

		        if (path.getMediumId() == null || path.getSmallId() == null) {
		            throw new IllegalArgumentException("카테고리 경로가 올바르지 않습니다. (mediumId/smallId 누락)");
		        }

		        // 1) 중-소 관계가 실제로 존재하는지 검증 (tb_medium_small_category 기반)
		        boolean valid = mediumSmallCategoryRepository.existsByMediumIdAndSmallId(path.getMediumId(), path.getSmallId());
		        if (!valid) {
		            throw new IllegalArgumentException(
		                "중분류-소분류 매핑이 존재하지 않습니다. mediumId=" + path.getMediumId() + ", smallId=" + path.getSmallId()
		            );
		        }

		        // 2) 엔티티 로딩
		        CategoryMedium medium = categoryMediumRepository.findById(path.getMediumId())
		                .orElseThrow(() -> new IllegalArgumentException("중분류 없음: " + path.getMediumId()));

		        CategorySmall small = categorySmallRepository.findById(path.getSmallId())
		                .orElseThrow(() -> new IllegalArgumentException("소분류 없음: " + path.getSmallId()));

		        // 3) productId+mediumId+smallId 중복 방지 (DB unique 제약 전에 선 차단)
		        if (mediumSmallProductCategoryRepository.existsByProductIdAndMediumIdAndSmallId(
		                product.getId(), medium.getId(), small.getId())) {
		            continue;
		        }

		        // 4) 경로 저장
		        MediumSmallProductCategory mspc = new MediumSmallProductCategory();
		        mspc.setProduct(product);
		        mspc.setMedium(medium);
		        mspc.setSmall(small);

		        mediumSmallProductCategoryRepository.save(mspc);
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
	    if ("detailHtml".equals(type)) {
	        subDir = "detailHtml";
	    } else if ("question".equals(type) && key != null && key.startsWith("question_")) {
	        subDir = key;
	    } else {
	        subDir = "etc";
	    }

	    Path tempDir = Paths.get(uploadBasePath, "temp", dateStr, subDir);
	    File dir = tempDir.toFile();
	    if (!dir.exists() && !dir.mkdirs()) {
	        throw new RuntimeException("임시 폴더 생성 실패");
	    }

	    for (MultipartFile file : files) {
	        if (file == null || file.isEmpty()) {
	            continue;
	        }

	        String orig = Objects.requireNonNull(file.getOriginalFilename(), "원본 파일명이 없습니다.");

	        // 1) 확장자만 추출 (원본 파일명 본문은 사용하지 않음)
	        String ext = "";
	        int dotIdx = orig.lastIndexOf('.');
	        if (dotIdx >= 0 && dotIdx < orig.length() - 1) {
	            ext = orig.substring(dotIdx + 1).toLowerCase(); // "jpg"
	        }

	        // 2) 확장자가 없으면 저장 자체를 막거나, 기본 확장자를 부여해도 됩니다.
	        //    지금은 "막는" 방식으로 처리합니다.
	        if (ext.isBlank()) {
	            throw new RuntimeException("확장자가 없는 파일은 업로드할 수 없습니다: " + orig);
	        }

	        // 3) 저장 파일명은 UID만 (영문/숫자) + "." + ext
	        String uid = UUID.randomUUID().toString().replace("-", ""); // 영문/숫자 32자
	        String newName = uid + "." + ext;

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

	private List<String> extractTempUrlsFromHtml(String html) {
		List<String> out = new ArrayList<>();
		if (html == null || html.isEmpty())
			return out;
		try {
			java.util.regex.Pattern p = java.util.regex.Pattern
					.compile("(?i)(?:https?:)?(?://[^\"'\\s>]+)?(/upload/temp/[^\"'>\\s]+)");
			java.util.regex.Matcher m = p.matcher(html);
			while (m.find()) {
				String u = normalizeUploadUrls(m.group(1));
				out.add(u);
			}
		} catch (Exception ignore) {
		}
		return out;
	}

	@Transactional
	public String moveEditorImages(Long productId, String type, String key, String html, List<String> tempImgList) {
		System.out.println(key);
		System.out.println(type);
	    Product product = productRepository.findById(productId)
	            .orElseThrow(() -> new IllegalArgumentException("상품 없음: " + productId));

	    if ((tempImgList == null || tempImgList.isEmpty()) && html != null) {
	        tempImgList = extractTempUrlsFromHtml(html);
	        System.out.println(">>> [DEBUG] extracted temp from html: " + tempImgList.size());
	    }

	    String targetDir;
	    ProductAnswer targetAnswer = null;

	    if ("detailHtml".equals(type) && "detailHtml".equals(key)) {
	        targetDir = uploadBasePath + "/product/" + productId + "/detail";

	    } else if ("question".equals(type) && key != null && key.startsWith("question_")) {
	        String idx = key.replace("question_", "");
	        Long questionId = parseLongSafe(idx);

	        // ✅ ✅ ✅ [핵심 변경] : 없으면 생성해서 반환 (예외 금지)
	        targetAnswer = findOrCreateAnswerByKey(product, questionId);

	        targetDir = uploadBasePath + "/product/" + productId + "/common/editor/" + targetAnswer.getId();

	    } else {
	        throw new IllegalArgumentException("지원하지 않는 type/key");
	    }

		File dir = new File(targetDir);
		if (!dir.exists() && !dir.mkdirs())
			throw new RuntimeException("타겟 폴더 생성 실패: " + targetDir);

		// 2) temp -> target 이동 + 치환 맵 구성
		Map<String, String> replaceMap = new java.util.LinkedHashMap<>();
		List<String> movedFileNames = new ArrayList<>();
		List<String> temps = (tempImgList == null) ? java.util.Collections.emptyList() : tempImgList;

		if (tempImgList != null) {
			for (String u : tempImgList)
				System.out.println("  [TEMP_URL] " + u);
		}

		for (String tempUrlRaw : temps) {
			String uploadUrl = extractUploadPath(tempUrlRaw); // '/upload/temp/...'
			String relative = toUploadRelativePath(uploadUrl); // 'temp/...'
			File tempFile = new File(uploadBasePath, relative);
			String fileName = extractFileNameOnly(uploadUrl);

			File targetFile = new File(dir, fileName);
			try {
				System.out.println("tempFile.toPath() : " + tempFile.toPath());
				System.out.println("targetFile.toPath() : " + targetFile.toPath());
				// Files.move 직전
				System.out.println(">>> [MOVE] uploadBasePath = " + uploadBasePath);
				System.out.println(">>> [MOVE] uploadUrl     = " + uploadUrl);
				System.out.println(">>> [MOVE] relative      = " + relative);
				System.out.println(">>> [MOVE] tempFileAbs   = " + tempFile.getAbsolutePath());
				System.out.println(">>> [MOVE] tempExists    = " + tempFile.exists());
				System.out.println(">>> [MOVE] tempIsFile    = " + tempFile.isFile());
				System.out.println(">>> [MOVE] tempReadable  = " + tempFile.canRead());
				System.out.println(">>> [MOVE] targetAbs     = " + targetFile.getAbsolutePath());
				System.out.println(">>> [MOVE] targetDirAbs  = " + dir.getAbsolutePath());
				System.out.println(">>> [MOVE] targetDirOk   = " + dir.exists() + ", canWrite=" + dir.canWrite());
				if (!tempFile.exists() || !tempFile.isFile()) {
				    throw new RuntimeException("임시파일이 실제로 존재하지 않습니다. tempFileAbs=" + tempFile.getAbsolutePath()
				            + " | uploadUrl=" + uploadUrl + " | relative=" + relative);
				}
				Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (Exception e) {
				throw new RuntimeException("임시파일 이동 실패: " + uploadUrl, e);
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
				productDetailImageRepository.save(di);
			} else {
				
				webPath = "/upload/product/" + productId + "/common/editor/" + targetAnswer.getId() + "/" + fileName;
				ProductAnswerDetailImage ai = new ProductAnswerDetailImage();
				ai.setAnswer(targetAnswer);
				ai.setUrl(webPath);
				ai.setPath(targetFile.getAbsolutePath());
				ai.setFileName(fileName);
				ai.setUploadedAt(LocalDateTime.now());
				productAnswerDetailImageRepository.save(ai);
			}

			replaceMap.put(uploadUrl, webPath);
			movedFileNames.add(fileName);
		}

		// 3) HTML 치환
		String newHtml = (html == null) ? "" : html;

		// 3-1) 정확 매핑
		for (Map.Entry<String, String> e : replaceMap.entrySet()) {
			newHtml = replaceSrcExact(newHtml, e.getKey(), e.getValue());
		}
		// 3-2) 파일명 폴백
		for (String fileName : movedFileNames) {
			String newUrl = resolveNewUrlByFileName(replaceMap, fileName);
			if (newUrl != null)
				newHtml = replaceImageSrcByFilename(newHtml, fileName, newUrl);
		}
		// 3-3) 폴더 폴백
		if ("detailHtml".equals(type)) {
		    newHtml = newHtml.replaceAll(
		        // 파일명 부분을 ([^"'>\\s]+) 로 캡처
		        "(?i)(?<=src\\s*=\\s*['\"])(?:https?:)?(?://[^\"'\\s>]+)?/*upload/temp/\\d{8}/detailHtml/([^\"'>\\s]+)(?=['\"])",
		        // 베이스 + $1(캡처된 파일명)
		        java.util.regex.Matcher.quoteReplacement("/upload/product/" + productId + "/detail/") + "$1"
		    );
		} else {
		    String answerBase = "/upload/product/" + productId + "/common/editor/" + targetAnswer.getId() + "/";
		    newHtml = newHtml.replaceAll(
		        "(?i)(?<=src\\s*=\\s*['\"])(?:https?:)?(?://[^\"'\\s>]+)?/*upload/temp/\\d{8}/question_\\d+/([^\"'>\\s]+)(?=['\"])",
		        java.util.regex.Matcher.quoteReplacement(answerBase) + "$1"
		    );
		}

		newHtml = normalizeUploadUrls(newHtml);

		int afterTemp = Math.max(0, newHtml.split("/upload/temp/").length - 1);
		System.out.println("[MOVE_HTML] AFTER  tempCount=" + afterTemp);

		// 4) 최종 참조 URL(/upload/product/...) 수집 + 정규화
		java.util.Set<String> finalRefs = new java.util.LinkedHashSet<>();
		try {
			java.util.regex.Pattern p = java.util.regex.Pattern
					.compile("(?i)(?:https?:)?(?://[^\"'\\s>]+)?(/upload/product/[^\"'>\\s]+)");
			java.util.regex.Matcher m = p.matcher(newHtml);
			while (m.find())
				finalRefs.add(normalizeUploadUrls(m.group(1)));
		} catch (Exception ignore) {
			ignore.getStackTrace();
		}

		// 5) 기존 레코드 로드 및 삭제/보강 (기존 그대로)
		if ("detailHtml".equals(type)) {
			// ===== 여기서 디버깅 시작 =====
		    List<ProductDetailImage> olds = em
		            .createQuery("select d from ProductDetailImage d where d.product = :p", ProductDetailImage.class)
		            .setParameter("p", product).getResultList();
		    olds.forEach(d -> System.out.println("DB : " + normalizeUploadUrls(d.getUrl())));

		    finalRefs.forEach(r -> System.out.println("HTML: " + r));

		    olds.forEach(d -> {
		        String dbUrl = normalizeUploadUrls(d.getUrl());
		        boolean keep = finalRefs.contains(dbUrl);
		        System.out.println("KEEP? " + keep + " | DB=" + dbUrl);
		    });
		    // ===== 디버깅 끝 =====

			for (ProductDetailImage d : olds) {
				if (!finalRefs.contains(normalizeUploadUrls(d.getUrl()))) {
					deletePhysicalFileSafe(d.getPath());
					productDetailImageRepository.delete(d);
				}
			}
			java.util.Set<String> exists = olds.stream().map(x -> normalizeUploadUrls(x.getUrl()))
					.collect(java.util.stream.Collectors.toSet());
			for (String url : finalRefs) {
				if (!exists.contains(url)) {
					String rel = toUploadRelativePath(url);
					File f = new File(uploadBasePath, rel);
					String fileName = extractFileNameOnly(url);
					ProductDetailImage di = new ProductDetailImage();
					di.setProduct(product);
					di.setUrl(url);
					di.setPath(f.getAbsolutePath());
					di.setFileName(fileName);
					di.setUploadedAt(LocalDateTime.now());
					productDetailImageRepository.save(di);
				}
			}
			product.setDetailHtml(newHtml);
			productRepository.save(product);
		} else {
			List<ProductAnswerDetailImage> olds = em
					.createQuery("select a from ProductAnswerDetailImage a where a.answer = :ans",
							ProductAnswerDetailImage.class)
					.setParameter("ans", targetAnswer).getResultList();

			java.util.Set<String> exists = olds.stream().map(x -> normalizeUploadUrls(x.getUrl()))
					.collect(java.util.stream.Collectors.toSet());
			for (String url : finalRefs) {
				if (!exists.contains(url)) {
					String rel = toUploadRelativePath(url);
					File f = new File(uploadBasePath, rel);
					String fileName = extractFileNameOnly(url);
					ProductAnswerDetailImage ai = new ProductAnswerDetailImage();
					ai.setAnswer(targetAnswer);
					ai.setUrl(url);
					ai.setPath(f.getAbsolutePath());
					ai.setFileName(fileName);
					ai.setUploadedAt(LocalDateTime.now());
					productAnswerDetailImageRepository.save(ai);
				}
			}
			targetAnswer.setValue(newHtml);
			productAnswerRepository.save(targetAnswer);
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
		String abs = new File(absoluteSavedPath).getAbsolutePath().replace("\\", "/");

		// uploadBasePath 이후의 상대경로 산출
		String rel = abs.replace(base, "");
		if (!rel.startsWith("/"))
			rel = "/" + rel;

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

	/** HTML 내 img src에서 '/upload/...' 부분만 추출 (절대 URL이어도 /upload 부터) */
	private String extractUploadPath(String url) {
		if (url == null)
			return null;
		int idx = url.indexOf("/upload/");
		return (idx >= 0) ? url.substring(idx) : url; // '/upload/...' 또는 원문
	}

	/** '/upload/...'를 파일시스템 상대경로('temp/...')로 변환 */
	private String toUploadRelativePath(String uploadUrl) {
		if (uploadUrl == null)
			return null;
		String p = uploadUrl;
		if (p.startsWith("/"))
			p = p.substring(1); // 'upload/...'
		if (p.startsWith("upload/"))
			p = p.substring("upload/".length()); // 'temp/...'
		return p;
	}

	/** URL에서 파일명만 추출 (쿼리/해시 제거) */
	private String extractFileNameOnly(String url) {
		if (url == null)
			return null;
		int q = url.indexOf('?');
		if (q >= 0)
			url = url.substring(0, q);
		int h = url.indexOf('#');
		if (h >= 0)
			url = url.substring(0, h);
		int slash = url.lastIndexOf('/');
		return (slash >= 0) ? url.substring(slash + 1) : url;
	}

	/** html에서 특정 파일명으로 끝나는 img src 전체를 newUrl로 교체 (절대/상대/쿼리/해시 전부 포괄) */
	private String replaceImageSrcByFilename(String html, String fileName, String newUrl) {
		if (html == null || fileName == null || fileName.isEmpty() || newUrl == null)
			return html;
		String escapedFile = Pattern.quote(fileName);
		// src=".../파일명[?쿼리][#해시]" 형태 전체를 newUrl로 교체
		String pattern = "(?i)(?<=src\\s*=\\s*['\"])" + "[^\"'>\\s]*" // 스킴/호스트/경로 아무거나
				+ "/" + escapedFile + "(?:\\?[^\"'>#]*)?" // 선택적 쿼리
				+ "(?:#[^\"'>]*)?" // 선택적 해시
				+ "(?=['\"])";
		return html.replaceAll(pattern, Matcher.quoteReplacement(newUrl));
	}

	/** html 내에서 정확한 src="...old..." 만 new 로 교체 (절대/상대/쿼리/해시/도메인/슬래시 모두 허용) */
	private String replaceSrcExact(String html, String oldUrl, String newUrl) {
		if (html == null || oldUrl == null || newUrl == null)
			return html;

		// oldUrl에서 "/upload/..."만 추출
		String core = extractUploadPath(oldUrl); // "/upload/temp/..."
		String coreNoSlash = core.replaceFirst("^/+", ""); // "upload/temp/..."
		String justPath = coreNoSlash.replaceFirst("^upload/", ""); // "temp/..."

		// src="…(도메인/프로토콜/슬래시 상관없이)…/temp/.../파일[?쿼리][#해시]" → newUrl
		String quotedFile = Pattern.quote(extractFileNameOnly(core));
		String anyPrefix = "(?i)(?<=src\\s*=\\s*['\"])" + "(?:https?:)?(?://[^\"'\\s>]+)?" // 프로토콜/도메인 optional
				+ "/*" // 선행 슬래시 0개 이상
				+ "(?:upload/)?" + "(?:temp/[^\"'>\\s]*/)" + "([^\"'>\\s]*/)*" + quotedFile
				+ "(?:\\?[^\"'>#]*)?(?:#[^\"'>]*)?" + "(?=['\"])";

		// 우선 정규식 치환
		String out = html.replaceAll(anyPrefix, Matcher.quoteReplacement(newUrl));

		// 문자열 그대로가 남아있다면 마지막으로 문자열 치환도 한 번
		out = out.replace(core, newUrl).replace(coreNoSlash, newUrl).replace("/" + justPath, newUrl).replace(justPath,
				newUrl);

		return out;
	}

	/** 결과 html의 슬래시/프로토콜 정규화 */
	private String normalizeUploadUrls(String html) {
		if (html == null)
			return null;
		String out = html;

		// //upload → /upload
		out = out.replaceAll("(?i)(['\"])\\s*//+upload/", "$1/upload/");

		// /upload 앞에 다중 슬래시 정리
		out = out.replaceAll("(?i)/+upload/", "/upload/");

		// 중복 슬래시 정리 (http:// 는 건드리지 않음)
		out = out.replaceAll("(?<!:)//+", "/");

		return out;
	}

	/**
	 * 매핑 테이블(replaceMap)에서 파일명으로 새 URL 찾기 - replaceMap:
	 * key=/upload/temp/.../파일명.png , value=/upload/product/.../파일명.png
	 */
	private String resolveNewUrlByFileName(Map<String, String> map, String fileName) {
		if (map == null || fileName == null || fileName.isEmpty())
			return null;

		// 1) key가 .../파일명 으로 끝나는 항목 찾기
		for (Map.Entry<String, String> e : map.entrySet()) {
			String oldUrl = e.getKey();
			if (oldUrl != null && oldUrl.endsWith("/" + fileName)) {
				return e.getValue();
			}
		}
		// 2) 파일명만 추출해서 비교
		for (Map.Entry<String, String> e : map.entrySet()) {
			String oldUrl = e.getKey();
			String keyName = extractFileNameOnly(oldUrl);
			if (fileName.equals(keyName)) {
				return e.getValue();
			}
		}
		return null;
	}

	private void deletePhysicalFileSafe(String path) {
		if (path == null || path.isBlank())
			return;
		try {
			Files.deleteIfExists(Path.of(path));
		} catch (Exception ignore) {
		}
	}
	
	/**
	 * ✅ 핵심: 제품-질문 Answer가 없으면 생성해서 반환
	 * - "제품 등록 후 공통질문이 추가"된 케이스를 안전하게 처리
	 * - register / update 둘 다에서 동일하게 사용 가능
	 */
	private ProductAnswer findOrCreateAnswerByKey(Product product, Long questionId) {
	    if (product == null || product.getId() == null) {
	        throw new IllegalArgumentException("상품 없음");
	    }
	    if (questionId == null) {
	        throw new IllegalArgumentException("questionId 없음");
	    }

	    ProductQuestion q = productQuestionRepository.findById(questionId)
	            .orElseThrow(() -> new IllegalArgumentException("질문 없음(id): " + questionId));

	    List<ProductAnswer> exists = em.createQuery(
	                    "select a from ProductAnswer a where a.product = :p and a.question = :q order by a.id asc",
	                    ProductAnswer.class)
	            .setParameter("p", product)
	            .setParameter("q", q)
	            .getResultList();

	    if (exists != null && !exists.isEmpty()) {
	        return exists.get(0);
	    }

	    ProductAnswer a = new ProductAnswer();
	    a.setProduct(product);
	    a.setQuestion(q);
	    a.setValue("");       // 에디터의 html이 곧 들어올 예정이므로 빈값 허용
	    a.setFileUrl(null);
	    a.setPath(null);
	    a.setFileName(null);
	    return productAnswerRepository.save(a);
	}
}
