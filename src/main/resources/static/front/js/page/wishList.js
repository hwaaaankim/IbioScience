/* eslint-disable */
/* global jQuery */
(function(window, $) {
	"use strict";

	if (!$) return;

	const $doc = $(document);

	// -------------------------
	// 유틸
	// -------------------------
	function safeParseInt(v) {
		const n = parseInt(v, 10);
		return isNaN(n) ? 0 : n;
	}

	function isAuthenticated() {
		return !!(window.__isAuthenticated === true);
	}

	function requireLogin() {
		if (!isAuthenticated()) {
			alert('로그인이 필요합니다.');
			return false;
		}
		return true;
	}

	function goProductDetail(productId) {
		if (!productId) return;
		location.href = '/productDetail/' + productId;
	}

	// ✅ 선택된 productIds 수집
	function getSelectedProductIds() {
		const ids = [];
		$('.wishList-check:checked').each(function() {
			const pid = $(this).data('id');
			if (pid != null) ids.push(Number(pid));
		});
		return ids;
	}

	// ✅ 배치 삭제 API
	function apiRemoveBatch(productIds) {
		return $.ajax({
			url: '/api/customer/wishlist/remove-batch',
			method: 'POST',
			dataType: 'text',
			traditional: true, // ✅ productIds=1&productIds=2 형태로 보내기
			data: { productIds: productIds }
		});
	}

	// -------------------------
	// ✅ 옵션 패널 높이/토글 안정화 유틸
	// -------------------------

	/**
	 * 패널 열기:
	 * 1) scrollHeight(px)로 열기 애니메이션
	 * 2) transition 종료 후 max-height:none으로 풀어 내용 증가(안내문구/버튼 등)에도 안 잘리게 처리
	 */
	function openPanel($row, $panel) {
		if (!$row || !$row.length || !$panel || !$panel.length) return;

		$row.attr('aria-hidden', 'false');

		// 혹시 이전에 none으로 풀려있다면 다시 애니메이션 가능하도록 px로 초기화
		$panel.css({ overflow: 'hidden', maxHeight: '0px' });

		// 다음 프레임에서 실제 높이 반영
		requestAnimationFrame(function() {
			if (!$panel.hasClass('is-open')) {
				$panel.addClass('is-open');
			}

			const h = $panel.get(0).scrollHeight;
			$panel.css({ overflow: 'hidden', maxHeight: h + 'px' });

			// max-height transition이 끝나면 max-height 제한 해제(= 잘림 방지)
			$panel.off('transitionend.wishlist').on('transitionend.wishlist', function(e) {
				// max-height 트랜지션만 처리
				if (e && e.originalEvent && e.originalEvent.propertyName !== 'max-height') return;
				if (!$panel.hasClass('is-open')) return;

				// ✅ 핵심: 제한 해제
				$panel.css({ maxHeight: 'none', overflow: 'visible' });
			});

			// 폰트/레이아웃이 뒤늦게 변하는 경우 대비: 한 번 더 보정
			setTimeout(function() {
				if (!$panel.hasClass('is-open')) return;

				// 아직 none으로 안 풀렸다면(transition이 없거나 느린 경우) scrollHeight 재반영
				const curr = String($panel.css('max-height') || '');
				if (curr !== 'none') {
					$panel.css({ maxHeight: $panel.get(0).scrollHeight + 'px' });
				}
			}, 50);
		});
	}

	/**
	 * 패널 닫기:
	 * - max-height:none 상태일 수 있으므로 현재 scrollHeight(px)로 고정한 뒤 0으로 내려야 애니메이션이 정상 동작
	 */
	function closePanel($row, $panel) {
		if (!$row || !$row.length || !$panel || !$panel.length) return;

		$panel.off('transitionend.wishlist');

		// 현재 높이를 px로 잡아두고 -> 0으로
		const h = $panel.get(0).scrollHeight;
		$panel.css({ overflow: 'hidden', maxHeight: h + 'px' });

		// reflow 강제
		$panel.get(0).offsetHeight;

		$panel.css({ maxHeight: '0px', overflow: 'hidden' }).removeClass('is-open');
		$row.attr('aria-hidden', 'true');
	}

	// ===== 체크박스/선택삭제 =====
	function bindSelectUI() {
		const $checkAll = $('#wishList-checkAll');
		const $tbody = $('#wishList-tbody');
		const $btnDelete = $('#wishList-deleteBtn');

		function $checks() { return $('.wishList-check'); }

		function syncButtons() {
			const any = $checks().toArray().some(el => el.checked);
			$btnDelete.prop('disabled', !any);

			// 체크박스가 0개일 경우 전체체크도 초기화
			const all = $checks().toArray();
			if (!all.length) {
				$checkAll.prop('checked', false);
				$checkAll.prop('indeterminate', false);
				return;
			}
		}

		$checkAll.on('change', function() {
			$checks().prop('checked', this.checked);
			syncButtons();
		});

		$tbody.on('change', '.wishList-check', function() {
			const all = $checks().toArray();
			const checkedCount = all.filter(el => el.checked).length;
			$checkAll.prop('checked', all.length > 0 && checkedCount === all.length);
			$checkAll.prop('indeterminate', checkedCount > 0 && checkedCount < all.length);
			syncButtons();
		});

		// ✅ 선택삭제 실제 연결
		$btnDelete.on('click', function() {
			if (!requireLogin()) return;

			const ids = getSelectedProductIds();
			if (!ids.length) {
				alert('삭제할 상품을 선택해 주세요.');
				return;
			}

			if (!confirm('선택한 관심상품을 삭제하시겠습니까?')) return;

			apiRemoveBatch(ids)
				.done(function(resCount) {
					// 헤더 카운트도 동기화
					if (window.IbioWish && typeof window.IbioWish.refreshCount === 'function') {
						window.IbioWish.refreshCount();
					} else {
						$('.front-header-wish-count').text(Number(resCount || 0));
					}

					alert('선택한 관심상품이 삭제되었습니다.');
					// 가장 안전: 페이지 리로드(페이징/필터/옵션패널 상태 깨짐 방지)
					location.reload();
				})
				.fail(function(xhr) {
					if (xhr && xhr.status === 401) {
						alert('로그인이 필요합니다.');
					} else {
						alert('삭제 처리 중 오류가 발생했습니다.');
					}
				});
		});

		syncButtons();
	}

	// ===== 옵션 패널 토글 =====
	function bindOptionToggle() {
		let opened = null;

		$doc.on('click', '.wishList-add-toggle-btn', function() {
			const $btn = $(this);
			const target = String($btn.data('target') || '');
			const $row = $('#wishList-add-panel-' + target);
			if (!$row.length) return;

			const $panel = $row.find('.wishList-add-optpanel');
			if (!$panel.length) return;

			// 다른 패널 닫기
			if (opened && opened.get(0) !== $panel.get(0)) {
				const $openedRow = opened.closest('.wishList-add-optrow');
				closePanel($openedRow, opened);
				opened = null;
			}

			const isOpen = $panel.hasClass('is-open');
			if (isOpen) {
				closePanel($row, $panel);
				opened = null;
			} else {
				openPanel($row, $panel);
				opened = $panel;
			}
		});

		// ✅ 수량 최소 1 보정
		$doc.on('input', '.wishList-add-optqty', function() {
			const v = safeParseInt($(this).val());
			if (v < 1) $(this).val('1');
		});
	}

	// ===== 상품바로가기 버튼 =====
	function bindGoDetail() {
		$doc.on('click', '.wishList-go-detail', function() {
			const pid = $(this).data('product-id');
			goProductDetail(pid);
		});
	}

	// ===== 페이지네이션 =====
	function bindPagination() {
		$doc.on('click', '.pagination a.page-link', function(e) {
			e.preventDefault();
			const page = $(this).data('page');
			if (page === undefined || page === null) return;

			const $form = $('#wishListForm');
			if (!$form.length) return;

			$form.find('input[name="page"]').val(page);
			$form.trigger('submit');
		});

		// ✅ 검색 버튼 눌렀을 때만 page=0
		$('#wishList-searchBtn').on('click', function() {
			const $form = $('#wishListForm');
			if (!$form.length) return;
			$form.find('input[name="page"]').val('0');
		});
	}

	// ===== 장바구니 담기 =====
	function bindAddCart() {
		$doc.on('click', '.wishList-add-cart', function() {
			if (!requireLogin()) return;

			if (!window.IbioCart || typeof window.IbioCart.addEntry !== 'function') {
				alert('장바구니 모듈(IbioCart)을 찾을 수 없습니다. cart.js 로딩 순서를 확인해 주세요.');
				return;
			}

			const $panel = $(this).closest('.wishList-add-optpanel');
			const $optRow = $panel.closest('.wishList-add-optrow');
			const productId = $optRow.data('parent');

			const $productRow = $('.wishList-row[data-product-id="' + productId + '"]');
			const productName = $.trim($productRow.find('.wishList-pname').text()) || '';
			const productImageUrl = $productRow.find('.wishList-thumb img').attr('src') || '';

			const picks = [];
			$panel.find('tbody tr').each(function() {
				const $tr = $(this);
				const $chk = $tr.find('.wishList-add-optpick');
				if (!$chk.length || !$chk.is(':checked')) return;

				const qty = Math.max(1, safeParseInt($tr.find('.wishList-add-optqty').val()));
				const unitPrice = safeParseInt($tr.data('price'));

				picks.push({
					optionGroupId: $tr.data('option-group-id') != null ? Number($tr.data('option-group-id')) : null,
					optionGroupName: String($tr.data('option-group-name') || ''),
					optionId: $tr.data('option-id') != null ? Number($tr.data('option-id')) : null,
					optionName: String($tr.data('option-name') || ''),
					optionCode: String($tr.data('option-code') || ''),
					unit: '-',
					unitPrice: unitPrice,
					quantity: qty,
					linePrice: unitPrice * qty
				});
			});

			if (!picks.length) {
				alert('옵션을 1개 이상 선택해 주세요.');
				return;
			}

			window.IbioCart.addEntry({
				productId: Number(productId),
				productName: productName,
				productImageUrl: productImageUrl,
				options: picks
			});
			window.IbioCart.updateHeaderCount();

			alert('장바구니에 담겼습니다.');
		});
	}

	$(function() {
		bindSelectUI();
		bindOptionToggle();
		bindGoDetail();
		bindPagination();
		bindAddCart();
	});

})(window, jQuery);
