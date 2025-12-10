/* global jQuery */
(function($) {
	"use strict";

	$(function() {
		// ============================
		// ====== 옵션보기 스크롤 ======
		// ============================
		var $optBtn = $("#btn-go-option");
		var $tabLink = $('.tabsslider .nav.nav-tabs a[href="#tab-option"]');
		var $scrollBlock = $("#tab-option");
		var shouldScroll = false; // 옵션보기 버튼으로 연 탭인지 여부

		function scrollToOption() {
			if (!$scrollBlock.length) return;
			setTimeout(function() {
				var top = $scrollBlock.offset().top - 200; // 200px 여백
				$("html, body").stop().animate({ scrollTop: top }, 450, "swing");
			}, 0);
		}

		$tabLink.on("shown.bs.tab.optScroll", function() {
			if (shouldScroll) {
				shouldScroll = false;
				scrollToOption();
			}
		});

		$optBtn.on("click", function(e) {
			e.preventDefault();

			var isActive =
				$tabLink.attr("aria-selected") === "true" ||
				$tabLink.parent().hasClass("active");

			if (isActive) {
				scrollToOption();
			} else {
				shouldScroll = true;
				$tabLink.tab("show");
			}
		});

		// ==============================
		// ====== 옵션 총합 계산 ======
		// ==============================
		function recalcOptionTotal() {
			var total = 0;
			$(".product-list-option-table tbody tr").each(function() {
				var $row = $(this);
				var $check = $row.find(".product-list-row-check");
				if (!$check.length || !$check.is(":checked")) {
					return;
				}
				var price = parseInt($check.data("price"), 10);
				if (isNaN(price)) {
					price = 0;
				}
				var qty = parseInt($row.find(".product-list-qty-input").val(), 10);
				if (isNaN(qty) || qty < 1) {
					qty = 1;
				}
				total += price * qty;
			});
			$(".product-list-total-price").text(total.toLocaleString("ko-KR"));
		}

		$(document).on("change", ".product-list-row-check", recalcOptionTotal);
		$(document).on("change keyup", ".product-list-qty-input", recalcOptionTotal);

		// ============================
		// ====== 리뷰 관련 로직 ======
		// ============================

		var $reviewContent = $("#review-content");
		var $reviewImages = $("#review-images");
		var $reviewButton = $("#button-review");

		// 안내 문구 기본값
		var defaultReviewPlaceholder = "리뷰를 작성 해 주세요.";

		// product_id hidden input 은 이미 상단에 있음
		var productId = $("input[name='product_id']").val();

		// 권한 정보 캐시 (작성/수정/취소 시 사용)
		var reviewPermission = null;

		// 현재 수정 중인 리뷰 ID (null 이면 "새 리뷰 작성")
		var editingReviewId = null;
		var $currentEditLink = null;

		// 안내는 textarea placeholder 하나로만 사용
		function updateReviewFormState(canWrite, placeholderText) {
			if (!placeholderText) {
				if (reviewPermission && reviewPermission.message) {
					// 서버에서 내려준 안내 메시지(4가지 중 하나라고 가정)
					placeholderText = reviewPermission.message;
				} else {
					placeholderText = defaultReviewPlaceholder;
				}
			}
			$reviewContent.attr("placeholder", placeholderText);

			if (canWrite) {
				$reviewContent.prop("readonly", false);
				$reviewImages.prop("disabled", false);
				$reviewButton.removeClass("disabled").prop("disabled", false);
			} else {
				$reviewContent.prop("readonly", true);
				$reviewImages.prop("disabled", true);
				$reviewButton.addClass("disabled").prop("disabled", true);
			}
		}

		// ============================
		// ====== 권한 체크 호출 ======
		// ============================
		function loadReviewPermission() {
			if (!productId) {
				updateReviewFormState(false, "상품 정보가 없어 리뷰를 작성할 수 없습니다.");
				return;
			}

			$.ajax({
				url: "/api/front/product/" + productId + "/review/permission",
				method: "GET"
			}).done(function(res) {
				if (!res) {
					// 서버 응답이 비어있는 경우 - 로그인 안내로 통일
					reviewPermission = { canWrite: false, message: "로그인 후 이용 가능합니다" };
					updateReviewFormState(false, reviewPermission.message);
					return;
				}
				// res.message 는 아래 4가지 중 하나라고 가정
				// 1) "이미 리뷰를 작성하였습니다"
				// 2) "로그인 후 이용 가능합니다"
				// 3) "구매자만 작성하실 수 있습니다"
				// 4) "리뷰를 작성 해 주세요."
				reviewPermission = res;

				// 아직 수정 모드가 아니면 권한대로 세팅
				if (!editingReviewId) {
					var ph = res.message;
					if (!ph) {
						ph = res.canWrite ? defaultReviewPlaceholder : "";
					}
					updateReviewFormState(res.canWrite, ph);
				}
			}).fail(function() {
				// 네트워크 등 예외 상황
				reviewPermission = { canWrite: false, message: "리뷰를 작성할 수 없습니다." };
				updateReviewFormState(false, reviewPermission.message);
			});
		}

		loadReviewPermission();

		// ============================
		// ====== 수정 모드 진입 ======
		// ============================
		$(document).on("click", ".review-edit-link", function(e) {
			e.preventDefault();

			var $link = $(this);
			var $row = $link.closest("tr");
			var reviewId = $link.data("review-id");

			if (!reviewId) {
				alert("리뷰 정보를 찾을 수 없습니다.");
				return;
			}

			// 이미 이 링크가 '수정취소' 상태라면 → 수정 취소
			if ($link.hasClass("editing")) {
				// 폼 초기화
				editingReviewId = null;
				if ($currentEditLink) {
					$currentEditLink.removeClass("editing").text("수정");
					$currentEditLink = null;
				}

				$reviewContent.val("");
				$("input[name='rating']").prop("checked", false);
				$reviewImages.val("");

				// 다시 원래 권한 상태로
				if (reviewPermission) {
					updateReviewFormState(reviewPermission.canWrite, reviewPermission.message);
				} else {
					updateReviewFormState(false, "로그인 후 이용 가능합니다");
				}

				$reviewButton.text("리뷰작성");
				return;
			}

			// 다른 리뷰 수정 중이었다면 해제
			if ($currentEditLink && $currentEditLink.get(0) !== $link.get(0)) {
				$currentEditLink.removeClass("editing").text("수정");
			}

			// 이 리뷰를 수정 모드로
			editingReviewId = reviewId;
			$currentEditLink = $link;
			$link.addClass("editing").text("수정취소");

			var rating = $row.data("rating");
			var content = $row.data("content");

			// 폼에 값 채우기
			$reviewContent.val(content);
			$("input[name='rating']").prop("checked", false);
			if (rating) {
				$("input[name='rating'][value='" + rating + "']").prop("checked", true);
			}

			// 이미지 input 은 기존 이미지가 있어도 비워둔다.
			// → 수정 시 파일을 선택하지 않으면 "이미지 전체 삭제"로 처리 (서비스 로직에서 구현)
			$reviewImages.val("");

			// 수정 모드에서는 무조건 입력 가능하도록 enable
			updateReviewFormState(true, defaultReviewPlaceholder);
			$reviewButton.text("리뷰 수정 저장");
		});

		// ============================
		// ====== 리뷰 삭제 요청 ======
		// ============================
		$(document).on("click", ".review-delete-link", function(e) {
			e.preventDefault();

			var reviewId = $(this).data("review-id");
			if (!productId || !reviewId) {
				alert("리뷰 정보를 찾을 수 없습니다.");
				return;
			}

			if (!confirm("해당 리뷰를 삭제하시겠습니까?")) {
				return;
			}

			$.ajax({
				url: "/api/front/product/" + productId + "/review/" + reviewId,
				method: "DELETE"
			}).done(function() {
				alert("리뷰가 삭제되었습니다.");
				location.reload();
			}).fail(function(xhr) {
				var msg = xhr.responseText || "리뷰 삭제 중 오류가 발생했습니다.";
				alert(msg);
			});
		});

		// ============================
		// ====== 리뷰 작성/수정 ======
		// ============================
		$reviewButton.on("click", function(e) {
			e.preventDefault();

			if ($reviewButton.prop("disabled")) {
				// 비활성화 상태에서는 placeholder 내용을 그대로 안내로 사용
				var msg = $reviewContent.attr("placeholder") || "리뷰를 작성할 수 없습니다.";
				alert(msg);
				return;
			}

			var rating = $("input[name='rating']:checked").val();
			var content = $reviewContent.val();

			if (!rating) {
				alert("별점을 선택해 주세요.");
				return;
			}
			if (!content || $.trim(content).length === 0) {
				alert("리뷰 내용을 입력해 주세요.");
				return;
			}
			if (!productId) {
				alert("상품 정보가 없어 리뷰를 작성할 수 없습니다.");
				return;
			}

			var formData = new FormData();
			formData.append("rating", rating);
			formData.append("content", content);

			var files = $reviewImages[0].files;
			if (files && files.length > 0) {
				for (var i = 0; i < files.length; i++) {
					formData.append("images", files[i]);
				}
			}

			var url;
			var method;

			if (editingReviewId) {
				// 수정
				url = "/api/front/product/" + productId + "/review/" + editingReviewId;
				method = "PUT";
			} else {
				// 신규 작성
				url = "/api/front/product/" + productId + "/review";
				method = "POST";
			}

			$.ajax({
				url: url,
				method: method,
				processData: false,
				contentType: false,
				data: formData
			}).done(function() {
				if (editingReviewId) {
					alert("리뷰가 정상적으로 수정되었습니다.");
				} else {
					alert("리뷰가 정상적으로 등록되었습니다.");
				}
				location.reload();
			}).fail(function(xhr) {
				var msg = xhr.responseText || "리뷰 처리 중 오류가 발생했습니다.";
				alert(msg);
			});
		});
	});
})(jQuery);