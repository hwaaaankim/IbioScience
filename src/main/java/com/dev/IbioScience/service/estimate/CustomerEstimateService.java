package com.dev.IbioScience.service.estimate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.estimate.EstimateCreateItemRequest;
import com.dev.IbioScience.dto.estimate.EstimateCreateResponse;
import com.dev.IbioScience.dto.estimate.EstimateFormInitResponse;
import com.dev.IbioScience.dto.estimate.EstimateListItemDto;
import com.dev.IbioScience.dto.estimate.EstimateListPageResponse;
import com.dev.IbioScience.dto.estimate.EstimateProductRowDto;
import com.dev.IbioScience.dto.estimate.EstimateProductSearchResponse;
import com.dev.IbioScience.enums.estimate.EstimateAnswerStatus;
import com.dev.IbioScience.enums.estimate.EstimateCheckStatus;
import com.dev.IbioScience.enums.product.ProductImageType;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.estimate.Estimate;
import com.dev.IbioScience.model.estimate.EstimateAttachment;
import com.dev.IbioScience.model.estimate.EstimateItem;
import com.dev.IbioScience.model.product.Brand;
import com.dev.IbioScience.model.product.Product;
import com.dev.IbioScience.model.product.ProductImage;
import com.dev.IbioScience.model.product.category.CategoryLarge;
import com.dev.IbioScience.model.product.category.CategoryMedium;
import com.dev.IbioScience.model.product.category.CategorySmall;
import com.dev.IbioScience.model.product.relation.MediumSmallProductCategory;
import com.dev.IbioScience.repository.auth.MemberRepository;
import com.dev.IbioScience.repository.category.MediumSmallProductCategoryRepository;
import com.dev.IbioScience.repository.estimate.EstimateProductQueryRepository;
import com.dev.IbioScience.repository.estimate.EstimateRepository;
import com.dev.IbioScience.repository.product.register.ProductImageRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerEstimateService {

	private static final int SUGGEST_LIMIT = 10;

	private final MemberRepository memberRepository;
	private final EstimateRepository estimateRepository;
	private final MediumSmallProductCategoryRepository mediumSmallProductCategoryRepository;
	private final EstimateProductQueryRepository estimateProductQueryRepository;
	private final ProductImageRepository productImageRepository;
	private final ObjectMapper objectMapper;

	@Value("${spring.upload.path}")
	private String uploadPath;

	private static final EstimateDeletePolicy DELETE_POLICY = EstimateDeletePolicy.ALWAYS;

	@Transactional(readOnly = true)
	public EstimateFormInitResponse getFormInitData(Long productId, Long mappingId) {
		List<EstimateProductRowDto> rows = estimateProductQueryRepository.findInitialSelectedItems(productId, mappingId);
		enrichMainImage(rows);
		return new EstimateFormInitResponse(rows);
	}

	@Transactional(readOnly = true)
	public EstimateProductSearchResponse searchProducts(Long largeId, Long mediumId, Long smallId,
			String productKeyword, String brandKeyword) {
		List<EstimateProductRowDto> rows = estimateProductQueryRepository.searchProducts(
				largeId,
				mediumId,
				smallId,
				normalize(productKeyword),
				normalize(brandKeyword)
		);

		enrichMainImage(rows);
		return new EstimateProductSearchResponse(rows);
	}

	@Transactional(readOnly = true)
	public List<String> searchBrandSuggestions(Long largeId, Long mediumId, Long smallId, String productKeyword,
			String brandKeyword) {
		return estimateProductQueryRepository.searchBrandSuggestions(
				largeId,
				mediumId,
				smallId,
				normalize(productKeyword),
				normalize(brandKeyword),
				SUGGEST_LIMIT
		);
	}

	@Transactional(readOnly = true)
	public List<String> searchProductSuggestions(Long largeId, Long mediumId, Long smallId, String productKeyword,
			String brandKeyword) {
		return estimateProductQueryRepository.searchProductSuggestions(
				largeId,
				mediumId,
				smallId,
				normalize(productKeyword),
				normalize(brandKeyword),
				SUGGEST_LIMIT
		);
	}

	@Transactional
	public EstimateCreateResponse createEstimate(Long loginMemberId, String title, String detailContent,
			String itemsJson, List<MultipartFile> files) throws IOException {

		if (loginMemberId == null) {
			throw new IllegalArgumentException("로그인 정보가 없습니다.");
		}

		Member member = memberRepository.findById(loginMemberId)
				.orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

		List<EstimateCreateItemRequest> itemRequests = parseItemRequests(itemsJson);
		if (itemRequests.isEmpty()) {
			throw new IllegalArgumentException("견적 상품이 없습니다.");
		}

		Set<Long> mappingIds = itemRequests.stream()
				.map(EstimateCreateItemRequest::getMappingId)
				.collect(Collectors.toCollection(LinkedHashSet::new));

		List<MediumSmallProductCategory> mappings = mediumSmallProductCategoryRepository.findAllByIdIn(mappingIds);
		Map<Long, MediumSmallProductCategory> mappingMap = mappings.stream()
				.collect(Collectors.toMap(MediumSmallProductCategory::getId, v -> v));

		if (mappingMap.size() != mappingIds.size()) {
			throw new IllegalArgumentException("존재하지 않는 상품 매핑이 포함되어 있습니다.");
		}

		Estimate estimate = new Estimate();
		estimate.setMember(member);
		estimate.setTitle(StringUtils.hasText(title) ? title.trim() : "제품견적 문의합니다");
		estimate.setDetailContent(StringUtils.hasText(detailContent) ? detailContent.trim() : null);
		estimate.setRequestedAt(LocalDateTime.now());
		estimate.setCheckStatus(EstimateCheckStatus.UNCHECKED);
		estimate.setAnswerStatus(EstimateAnswerStatus.WAITING);

		for (EstimateCreateItemRequest req : itemRequests) {
			if (req.getMappingId() == null) {
				throw new IllegalArgumentException("상품 매핑 정보가 없습니다.");
			}
			if (req.getQuantity() == null || req.getQuantity() <= 0) {
				throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
			}

			MediumSmallProductCategory mapping = mappingMap.get(req.getMappingId());
			Product product = mapping.getProduct();
			CategoryMedium medium = mapping.getMedium();
			CategoryLarge large = medium.getLarge();
			CategorySmall small = mapping.getSmall();
			Brand brand = product.getBrand();

			EstimateItem item = new EstimateItem();
			item.setMapping(mapping);
			item.setProduct(product);
			item.setQuantity(req.getQuantity());
			item.setLargeCategoryName(large.getName());
			item.setMediumCategoryName(medium.getName());
			item.setSmallCategoryName(small.getName());
			item.setBrandName(brand != null ? brand.getName() : null);
			item.setProductName(product.getName());
			item.setProductCode(product.getCode());

			estimate.addItem(item);
		}

		estimateRepository.saveAndFlush(estimate);

		List<Path> savedPaths = new ArrayList<>();

		try {
			saveAttachments(estimate, member.getId(), files, savedPaths);
		} catch (Exception e) {
			for (Path savedPath : savedPaths) {
				try {
					Files.deleteIfExists(savedPath);
				} catch (Exception ignore) {
				}
			}
			throw e;
		}

		estimateRepository.save(estimate);
		return new EstimateCreateResponse(estimate.getId(), "견적 문의가 정상 등록되었습니다.");
	}

	@Transactional(readOnly = true)
	public EstimateListPageResponse getMyEstimateList(Long loginMemberId, int page, int size, String titleKeyword,
			LocalDate from, LocalDate to, String sortBy, String sortDir) {
		if (loginMemberId == null) {
			throw new IllegalArgumentException("로그인 정보가 없습니다.");
		}

		Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
		String resolvedSortBy = resolveSortBy(sortBy);

		PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, resolvedSortBy));

		Specification<Estimate> spec = (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.equal(root.get("member").get("id"), loginMemberId));

			if (StringUtils.hasText(titleKeyword)) {
				predicates.add(
						cb.like(
								cb.lower(root.get("title")),
								"%" + titleKeyword.trim().toLowerCase() + "%"
						)
				);
			}

			if (from != null && to != null) {
				LocalDateTime fromDateTime = from.atStartOfDay();
				LocalDateTime toExclusive = to.plusDays(1).atStartOfDay();

				predicates.add(cb.greaterThanOrEqualTo(root.get("requestedAt"), fromDateTime));
				predicates.add(cb.lessThan(root.get("requestedAt"), toExclusive));
			} else if (from != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("requestedAt"), from.atStartOfDay()));
			} else if (to != null) {
				predicates.add(cb.lessThan(root.get("requestedAt"), to.plusDays(1).atStartOfDay()));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};

		Page<Estimate> result = estimateRepository.findAll(spec, pageable);

		List<EstimateListItemDto> content = result.getContent().stream()
				.map(e -> new EstimateListItemDto(
						e.getId(),
						e.getAnswerStatus().name(),
						e.getAnswerStatus().getLabel(),
						e.getCheckStatus().name(),
						e.getCheckStatus().getLabel(),
						e.getTitle(),
						buildProductSummary(e),
						e.getRequestedAt(),
						e.getCheckedAt(),
						e.getAnsweredAt()
				))
				.collect(Collectors.toList());

		return new EstimateListPageResponse(
				content,
				result.getNumber(),
				result.getSize(),
				result.getTotalElements(),
				result.getTotalPages(),
				resolvedSortBy,
				direction.name().toLowerCase()
		);
	}

	@Transactional
	public void deleteMyEstimates(Long loginMemberId, Collection<Long> estimateIds) {
		if (loginMemberId == null) {
			throw new IllegalArgumentException("로그인 정보가 없습니다.");
		}
		if (estimateIds == null || estimateIds.isEmpty()) {
			return;
		}

		List<Estimate> estimates = estimateRepository.findAllByIdInAndMember_Id(estimateIds, loginMemberId);

		if (estimates.isEmpty()) {
			return;
		}

		for (Estimate estimate : estimates) {
			validateDeletePolicy(estimate);
			deleteAttachmentFiles(estimate);
		}

		estimateRepository.deleteAll(estimates);
	}

	private void validateDeletePolicy(Estimate estimate) {
		switch (DELETE_POLICY) {
		case ALWAYS:
			return;

		case BEFORE_CHECK_ONLY:
			if (estimate.getCheckStatus() != EstimateCheckStatus.UNCHECKED) {
				throw new IllegalStateException("관리자 확인 이후의 견적은 삭제할 수 없습니다.");
			}
			return;

		case CHECKED_BUT_NOT_ANSWERED_ONLY:
			if (estimate.getCheckStatus() != EstimateCheckStatus.CHECKED
					|| estimate.getAnswerStatus() == EstimateAnswerStatus.ANSWERED) {
				throw new IllegalStateException("확인완료 + 답변대기 상태의 견적만 삭제할 수 있습니다.");
			}
			return;

		default:
			throw new IllegalStateException("삭제 정책이 올바르지 않습니다.");
		}
	}

	private void deleteAttachmentFiles(Estimate estimate) {
		if (estimate.getAttachments() == null || estimate.getAttachments().isEmpty()) {
			return;
		}

		for (EstimateAttachment attachment : estimate.getAttachments()) {
			if (!StringUtils.hasText(attachment.getFilePath())) {
				continue;
			}
			try {
				Files.deleteIfExists(Paths.get(attachment.getFilePath()));
			} catch (Exception e) {
				throw new RuntimeException("첨부파일 삭제 중 오류가 발생했습니다. path=" + attachment.getFilePath(), e);
			}
		}
	}

	private void saveAttachments(Estimate estimate, Long memberId, List<MultipartFile> files, List<Path> savedPaths)
			throws IOException {

		if (files == null || files.isEmpty()) {
			return;
		}

		Path dir = Paths.get(uploadPath, String.valueOf(memberId), "estimate", String.valueOf(estimate.getId()));
		Files.createDirectories(dir);

		int sortOrder = 0;

		for (MultipartFile file : files) {
			if (file == null || file.isEmpty()) {
				continue;
			}

			String originalFileName = file.getOriginalFilename();
			String ext = getExtension(originalFileName);

			if (!isAllowedExtension(ext)) {
				throw new IllegalArgumentException("첨부파일은 이미지 또는 PDF만 업로드할 수 있습니다.");
			}

			String storedFileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
			Path savePath = dir.resolve(storedFileName);

			Files.copy(file.getInputStream(), savePath, StandardCopyOption.REPLACE_EXISTING);
			savedPaths.add(savePath);

			EstimateAttachment attachment = new EstimateAttachment();
			attachment.setOriginalFileName(originalFileName);
			attachment.setStoredFileName(storedFileName);
			attachment.setFilePath(savePath.toAbsolutePath().toString());
			attachment.setFileUrl("/upload/" + memberId + "/estimate/" + estimate.getId() + "/" + storedFileName);
			attachment.setContentType(file.getContentType());
			attachment.setFileSize(file.getSize());
			attachment.setSortOrder(sortOrder++);

			estimate.addAttachment(attachment);
		}
	}

	private List<EstimateCreateItemRequest> parseItemRequests(String itemsJson) {
		try {
			List<EstimateCreateItemRequest> items = objectMapper.readValue(
					itemsJson,
					new TypeReference<List<EstimateCreateItemRequest>>() {
					}
			);

			if (items == null) {
				return new ArrayList<>();
			}

			return items;
		} catch (Exception e) {
			throw new IllegalArgumentException("상품 요청 데이터 형식이 올바르지 않습니다.", e);
		}
	}

	private void enrichMainImage(List<EstimateProductRowDto> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}

		List<Long> productIds = rows.stream()
				.map(EstimateProductRowDto::getProductId)
				.distinct()
				.collect(Collectors.toList());

		Map<Long, String> imageMap = new LinkedHashMap<>();

		List<ProductImage> images = productImageRepository
				.findByProduct_IdInAndTypeOrderByProduct_IdAscSortOrderAscIdAsc(productIds, ProductImageType.MAIN);

		for (ProductImage image : images) {
			Long productId = image.getProduct().getId();
			imageMap.putIfAbsent(productId, image.getUrl());
		}

		for (EstimateProductRowDto row : rows) {
			String imageUrl = imageMap.get(row.getProductId());
			row.setImageUrl(StringUtils.hasText(imageUrl) ? imageUrl : "/front/image/sample/100-100.png");
		}
	}

	private String buildProductSummary(Estimate estimate) {
		if (estimate.getItems() == null || estimate.getItems().isEmpty()) {
			return null;
		}

		String joined = estimate.getItems().stream()
				.map(EstimateItem::getProductName)
				.filter(StringUtils::hasText)
				.map(String::trim)
				.distinct()
				.collect(Collectors.joining(", "));

		return StringUtils.hasText(joined) ? joined : null;
	}

	private String resolveSortBy(String sortBy) {
		if (!StringUtils.hasText(sortBy)) {
			return "requestedAt";
		}

		switch (sortBy) {
		case "answerStatus":
		case "checkStatus":
		case "title":
		case "requestedAt":
		case "checkedAt":
		case "answeredAt":
			return sortBy;
		default:
			return "requestedAt";
		}
	}

	private String normalize(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String getExtension(String fileName) {
		if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
			throw new IllegalArgumentException("파일 확장자를 확인할 수 없습니다.");
		}
		return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
	}

	private boolean isAllowedExtension(String ext) {
		return Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp", "pdf").contains(ext);
	}

	private enum EstimateDeletePolicy {
		ALWAYS,
		BEFORE_CHECK_ONLY,
		CHECKED_BUT_NOT_ANSWERED_ONLY
	}
}