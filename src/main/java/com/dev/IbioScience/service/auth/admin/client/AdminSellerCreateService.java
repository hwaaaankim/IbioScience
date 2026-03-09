package com.dev.IbioScience.service.auth.admin.client;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.dev.IbioScience.dto.admin.client.AdminSellerCategoryPermissionRequest;
import com.dev.IbioScience.dto.admin.client.AdminSellerCreateRequest;
import com.dev.IbioScience.enums.auth.CustomerType;
import com.dev.IbioScience.enums.auth.DealerGrade;
import com.dev.IbioScience.enums.auth.DealerType;
import com.dev.IbioScience.enums.auth.MemberDomain;
import com.dev.IbioScience.enums.auth.MemberRole;
import com.dev.IbioScience.enums.auth.MemberStatus;
import com.dev.IbioScience.model.auth.BuyerDealerProfile;
import com.dev.IbioScience.model.auth.CompanyProfile;
import com.dev.IbioScience.model.auth.DealerCategoryPermission;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.SellerDealerProfile;
import com.dev.IbioScience.model.auth.embedded.Address;
import com.dev.IbioScience.model.product.category.CategoryLarge;
import com.dev.IbioScience.model.product.category.CategoryMedium;
import com.dev.IbioScience.repository.auth.BuyerDealerProfileRepository;
import com.dev.IbioScience.repository.auth.CompanyProfileRepository;
import com.dev.IbioScience.repository.auth.DealerCategoryPermissionRepository;
import com.dev.IbioScience.repository.auth.MemberRepository;
import com.dev.IbioScience.repository.auth.SellerDealerProfileRepository;
import com.dev.IbioScience.repository.category.CategoryLargeRepository;
import com.dev.IbioScience.repository.category.CategoryMediumRepository;
import com.dev.IbioScience.repository.category.CategorySmallRepository;
import com.dev.IbioScience.repository.category.MediumSmallProductCategoryRepository;
import com.dev.IbioScience.utils.UploadPathHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminSellerCreateService {

	private final ObjectMapper objectMapper;

	private final MemberRepository memberRepository;
	private final CompanyProfileRepository companyProfileRepository;
	private final BuyerDealerProfileRepository buyerDealerProfileRepository;
	private final SellerDealerProfileRepository sellerDealerProfileRepository;
	private final DealerCategoryPermissionRepository dealerCategoryPermissionRepository;

	private final CategoryLargeRepository categoryLargeRepository;
	private final CategoryMediumRepository categoryMediumRepository;
	private final CategorySmallRepository categorySmallRepository;
	private final MediumSmallProductCategoryRepository mediumSmallProductCategoryRepository;

	private final PasswordEncoder passwordEncoder;
	private final UploadPathHelper uploadPathHelper;

	@Transactional
	public Long createSeller(AdminSellerCreateRequest request) {
		validateRequest(request);

		List<AdminSellerCategoryPermissionRequest> rawPermissions = parsePermissions(request.getCategoryPermissionsJson());
		List<AdminSellerCategoryPermissionRequest> normalizedPermissions = normalizePermissions(rawPermissions);

		if (normalizedPermissions.isEmpty()) {
			throw new IllegalArgumentException("판매 가능 카테고리를 1개 이상 등록해 주세요.");
		}

		CompanyProfile companyProfile = new CompanyProfile();
		companyProfile.setCompanyName(req(request.getCompanyName(), "업체명은 필수입니다."));
		companyProfile.setDepartment(nvl(request.getDepartment()));
		companyProfile.setCeoName(req(request.getCeoName(), "대표자명은 필수입니다."));
		companyProfile.setBusinessType(req(request.getBusinessType(), "업태는 필수입니다."));
		companyProfile.setBusinessItem(req(request.getBusinessItem(), "종목은 필수입니다."));
		companyProfile.setRepresentativeTel(formatPhone(request.getRepresentativeTel()));
		companyProfile.setFax(formatPhone(request.getFax()));
		companyProfile.setInvoiceEmail(req(request.getInvoiceEmail(), "계산서 이메일은 필수입니다."));
		companyProfile.setBusinessRegistrationNumber(formatBusinessNumber(req(request.getBusinessRegistrationNumber(), "사업자등록번호는 필수입니다.")));
		companyProfile.setBusinessRegImagePath(null);
		companyProfile.setBusinessRegImageRoad(null);
		companyProfile.setCompanyAddress(buildAddress(
				request.getCPostcode(),
				request.getCRoadAddress(),
				request.getCJibunAddress(),
				request.getCDetailAddress()
		));
		companyProfile.setOrganizationCategory(
				Optional.ofNullable(request.getOrganizationCategory())
						.orElseThrow(() -> new IllegalArgumentException("기관 분류를 선택해 주세요."))
		);
		companyProfile.setHomepageUrl(nvl(request.getCompanyHomepageUrl()));

		companyProfile = companyProfileRepository.save(companyProfile);

		Member member = new Member();
		member.setUsername(req(request.getUsername(), "아이디는 필수입니다."));
		member.setPassword(passwordEncoder.encode(req(request.getPassword(), "비밀번호는 필수입니다.")));
		member.setName(req(request.getName(), "담당자명은 필수입니다."));
		member.setTel(formatPhone(request.getTel()));
		member.setMobile(formatPhone(req(request.getMobile(), "휴대폰번호는 필수입니다.")));
		member.setEmail(req(request.getEmail(), "이메일은 필수입니다."));
		member.setPoint(request.getPoint() == null ? 0L : request.getPoint());
		member.setAddress(buildAddress(
				request.getMPostcode(),
				request.getMRoadAddress(),
				request.getMJibunAddress(),
				request.getMDetailAddress()
		));

		member.setDomain(MemberDomain.CUSTOMER);
		member.setCustomerType(CustomerType.BUSINESS);
		member.setDealerType(DealerType.SELLER);
		member.setRole(MemberRole.SELLER_DEALER);

		member.setStatus(request.getStatus() == null ? MemberStatus.ACTIVE : request.getStatus());
		member.setCompanyProfile(companyProfile);
		member.setOrganizationName(nvl(request.getOrganizationName()));
		member.setJoinedAt(LocalDateTime.now());
		member.setWithdrewAt(null);
		member.setMustChangePassword(request.isMustChangePassword());
		member.setLastPasswordChangedAt(LocalDateTime.now());
		member.setPosition(nvl(request.getPosition()));
		member.setUseYn(request.isUseYn());
		member.setPrimary(request.isPrimary());

		member = memberRepository.save(member);

		BuyerDealerProfile buyerDealerProfile = new BuyerDealerProfile();
		buyerDealerProfile.setMember(member);
		buyerDealerProfile.setGrade(
				Optional.ofNullable(request.getBuyerGrade())
						.orElseThrow(() -> new IllegalArgumentException("구매딜러 등급을 선택해 주세요."))
		);

		if (request.getBuyerGrade() == DealerGrade.CUSTOM) {
			if (request.getBuyerCustomDiscountRate() == null) {
				throw new IllegalArgumentException("CUSTOM 등급일 때는 커스텀 할인율이 필수입니다.");
			}
			buyerDealerProfile.setCustomDiscountRate(request.getBuyerCustomDiscountRate());
		} else {
			buyerDealerProfile.setCustomDiscountRate(null);
		}

		buyerDealerProfile.setEffectiveFrom(
				request.getBuyerEffectiveFrom() == null ? LocalDate.now() : request.getBuyerEffectiveFrom()
		);

		buyerDealerProfileRepository.save(buyerDealerProfile);

		SellerDealerProfile sellerDealerProfile = new SellerDealerProfile();
		sellerDealerProfile.setMember(member);
		sellerDealerProfile.setCompanyProfile(companyProfile);
		sellerDealerProfile.setShopName(req(request.getShopName(), "입점몰명은 필수입니다."));
		sellerDealerProfile.setLogoImagePath(null);
		sellerDealerProfile.setLogoImageRoad(null);
		sellerDealerProfile.setSupplierCode(resolveSupplierCode(request.getSupplierCode()));
		sellerDealerProfile.setTradingStatus(
				Optional.ofNullable(request.getTradingStatus())
						.orElseThrow(() -> new IllegalArgumentException("거래상태를 선택해 주세요."))
		);
		sellerDealerProfile.setSupplyType(
				Optional.ofNullable(request.getSupplyType())
						.orElseThrow(() -> new IllegalArgumentException("공급유형을 선택해 주세요."))
		);
		sellerDealerProfile.setSupplyStructure(
				Optional.ofNullable(request.getSupplyStructure())
						.orElseThrow(() -> new IllegalArgumentException("공급구조를 선택해 주세요."))
		);
		sellerDealerProfile.setProductTypeText(nvl(request.getProductTypeText()));
		sellerDealerProfile.setTel(formatPhone(request.getSellerTel()));
		sellerDealerProfile.setFax(formatPhone(request.getSellerFax()));
		sellerDealerProfile.setBusinessAddress(buildAddress(
				request.getBPostcode(),
				request.getBRoadAddress(),
				request.getBJibunAddress(),
				request.getBDetailAddress()
		));
		sellerDealerProfile.setReturnAddress(buildAddress(
				request.getRPostcode(),
				request.getRRoadAddress(),
				request.getRJibunAddress(),
				request.getRDetailAddress()
		));
		sellerDealerProfile.setHomepageUrl(nvl(request.getSellerHomepageUrl()));
		sellerDealerProfile.setDealStartDate(request.getDealStartDate());
		sellerDealerProfile.setDealStopDate(request.getDealStopDate());

		sellerDealerProfile = sellerDealerProfileRepository.save(sellerDealerProfile);

		List<DealerCategoryPermission> permissionEntities = new ArrayList<>();
		for (AdminSellerCategoryPermissionRequest item : normalizedPermissions) {
			DealerCategoryPermission permission = new DealerCategoryPermission();
			permission.setSellerDealerProfile(sellerDealerProfile);
			permission.setLarge(categoryLargeRepository.findById(item.getLargeId())
					.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 대분류입니다.")));

			if (item.getMediumId() != null) {
				permission.setMedium(categoryMediumRepository.findById(item.getMediumId())
						.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 중분류입니다.")));
			}

			if (item.getSmallId() != null) {
				permission.setSmall(categorySmallRepository.findById(item.getSmallId())
						.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 소분류입니다.")));
			}

			permissionEntities.add(permission);
		}
		dealerCategoryPermissionRepository.saveAll(permissionEntities);

		if (request.getBusinessRegFile() != null && !request.getBusinessRegFile().isEmpty()) {
			Path saved = uploadPathHelper.saveBizRegFileForCustomer(member.getId(), request.getBusinessRegFile());
			companyProfile.setBusinessRegImagePath(saved.toString());
			companyProfile.setBusinessRegImageRoad(uploadPathHelper.publicUrlOf(saved));
			companyProfileRepository.save(companyProfile);
		}

		if (request.getSellerLogoFile() != null && !request.getSellerLogoFile().isEmpty()) {
			Path savedLogo = uploadPathHelper.saveSellerLogoForCustomer(member.getId(), request.getSellerLogoFile());
			sellerDealerProfile.setLogoImagePath(savedLogo.toString());
			sellerDealerProfile.setLogoImageRoad(uploadPathHelper.publicUrlOf(savedLogo));
			sellerDealerProfileRepository.save(sellerDealerProfile);
		}

		return member.getId();
	}

	private void validateRequest(AdminSellerCreateRequest request) {
		String username = req(request.getUsername(), "아이디는 필수입니다.");

		if (memberRepository.existsByUsername(username)) {
			throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
		}

		String password = req(request.getPassword(), "비밀번호는 필수입니다.");
		String passwordConfirm = req(request.getPasswordConfirm(), "비밀번호 확인은 필수입니다.");

		if (!password.equals(passwordConfirm)) {
			throw new IllegalArgumentException("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
		}
	}

	private List<AdminSellerCategoryPermissionRequest> parsePermissions(String json) {
		if (!StringUtils.hasText(json)) {
			return Collections.emptyList();
		}

		try {
			return objectMapper.readValue(
					json,
					new TypeReference<List<AdminSellerCategoryPermissionRequest>>() {}
			);
		} catch (Exception e) {
			throw new IllegalArgumentException("카테고리 권한 데이터가 올바르지 않습니다.");
		}
	}

	private List<AdminSellerCategoryPermissionRequest> normalizePermissions(List<AdminSellerCategoryPermissionRequest> input) {
		List<AdminSellerCategoryPermissionRequest> result = new ArrayList<>();

		for (AdminSellerCategoryPermissionRequest raw : input) {
			if (raw == null || raw.getLargeId() == null) {
				continue;
			}

			validatePermissionReference(raw);

			Long largeId = raw.getLargeId();
			Long mediumId = raw.getMediumId();
			Long smallId = raw.getSmallId();

			// 1) 대분류 전체
			if (mediumId == null) {
				result.removeIf(p -> Objects.equals(p.getLargeId(), largeId));
				result.add(new AdminSellerCategoryPermissionRequest(largeId, null, null));
				continue;
			}

			// 2) 중분류 전체
			if (smallId == null) {
				boolean hasLargeWildcard = result.stream()
						.anyMatch(p -> Objects.equals(p.getLargeId(), largeId) && p.getMediumId() == null);

				if (hasLargeWildcard) {
					continue;
				}

				result.removeIf(p ->
						Objects.equals(p.getLargeId(), largeId)
								&& Objects.equals(p.getMediumId(), mediumId)
				);

				boolean existsSameMedium = result.stream().anyMatch(p ->
						Objects.equals(p.getLargeId(), largeId)
								&& Objects.equals(p.getMediumId(), mediumId)
								&& p.getSmallId() == null
				);

				if (!existsSameMedium) {
					result.add(new AdminSellerCategoryPermissionRequest(largeId, mediumId, null));
				}
				continue;
			}

			// 3) 소분류 단건
			boolean hasLargeWildcard = result.stream()
					.anyMatch(p -> Objects.equals(p.getLargeId(), largeId) && p.getMediumId() == null);

			if (hasLargeWildcard) {
				continue;
			}

			boolean hasMediumWildcard = result.stream().anyMatch(p ->
					Objects.equals(p.getLargeId(), largeId)
							&& Objects.equals(p.getMediumId(), mediumId)
							&& p.getSmallId() == null
			);

			if (hasMediumWildcard) {
				continue;
			}

			boolean existsExact = result.stream().anyMatch(p ->
					Objects.equals(p.getLargeId(), largeId)
							&& Objects.equals(p.getMediumId(), mediumId)
							&& Objects.equals(p.getSmallId(), smallId)
			);

			if (!existsExact) {
				result.add(new AdminSellerCategoryPermissionRequest(largeId, mediumId, smallId));
			}
		}

		return result;
	}

	private void validatePermissionReference(AdminSellerCategoryPermissionRequest item) {
		CategoryLarge large = categoryLargeRepository.findById(item.getLargeId())
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 대분류입니다."));

		if (item.getMediumId() == null) {
			if (item.getSmallId() != null) {
				throw new IllegalArgumentException("소분류는 중분류 없이 등록할 수 없습니다.");
			}
			return;
		}

		CategoryMedium medium = categoryMediumRepository.findById(item.getMediumId())
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 중분류입니다."));

		if (!Objects.equals(medium.getLarge().getId(), large.getId())) {
			throw new IllegalArgumentException("중분류가 선택한 대분류에 속하지 않습니다.");
		}

		if (item.getSmallId() == null) {
			return;
		}

		categorySmallRepository.findById(item.getSmallId())
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 소분류입니다."));

		boolean linked = mediumSmallProductCategoryRepository.existsByMedium_IdAndSmall_Id(
				item.getMediumId(),
				item.getSmallId()
		);

		if (!linked) {
			throw new IllegalArgumentException("소분류가 선택한 중분류에 속하지 않습니다.");
		}
	}

	private String resolveSupplierCode(String rawSupplierCode) {
		String input = nvl(rawSupplierCode);

		if (StringUtils.hasText(input)) {
			if (sellerDealerProfileRepository.existsBySupplierCode(input)) {
				throw new IllegalArgumentException("이미 사용 중인 공급사 코드입니다.");
			}
			return input;
		}

		for (int i = 0; i < 20; i++) {
			String generated = "SUP-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
					+ "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

			if (!sellerDealerProfileRepository.existsBySupplierCode(generated)) {
				return generated;
			}
		}

		throw new IllegalArgumentException("공급사 코드 생성에 실패했습니다.");
	}

	private Address buildAddress(String postcode, String roadAddress, String jibunAddress, String detailAddress) {
		boolean empty = !StringUtils.hasText(postcode)
				&& !StringUtils.hasText(roadAddress)
				&& !StringUtils.hasText(jibunAddress)
				&& !StringUtils.hasText(detailAddress);

		if (empty) {
			return null;
		}

		return Address.builder()
				.postcode(nvl(postcode))
				.roadAddress(nvl(roadAddress))
				.jibunAddress(nvl(jibunAddress))
				.detailAddress(nvl(detailAddress))
				.build();
	}

	private String formatBusinessNumber(String value) {
		String digits = onlyDigits(value);

		if (digits.length() > 10) {
			digits = digits.substring(0, 10);
		}

		if (digits.length() == 10) {
			return digits.replaceFirst("(\\d{3})(\\d{2})(\\d{5})", "$1-$2-$3");
		}

		return digits;
	}

	private String formatPhone(String value) {
		String digits = onlyDigits(value);

		if (!StringUtils.hasText(digits)) {
			return null;
		}

		if (digits.startsWith("02")) {
			if (digits.length() <= 2) return digits;
			if (digits.length() <= 5) return digits.replaceFirst("(\\d{2})(\\d+)", "$1-$2");
			if (digits.length() <= 9) return digits.replaceFirst("(\\d{2})(\\d{3})(\\d+)", "$1-$2-$3");
			return digits.replaceFirst("(\\d{2})(\\d{4})(\\d+)", "$1-$2-$3");
		}

		if (digits.length() <= 3) return digits;
		if (digits.length() <= 7) return digits.replaceFirst("(\\d{3})(\\d+)", "$1-$2");
		if (digits.length() <= 10) return digits.replaceFirst("(\\d{3})(\\d{3})(\\d+)", "$1-$2-$3");
		return digits.replaceFirst("(\\d{3})(\\d{4})(\\d+)", "$1-$2-$3");
	}

	private String onlyDigits(String value) {
		if (value == null) return "";
		return value.replaceAll("\\D", "");
	}

	private String req(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException(message);
		}
		return value.trim();
	}

	private String nvl(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}