/* eslint-disable */
/* global jQuery */
(function(window, $) {
	"use strict";

	if (!$) return;

	const $doc = $(document);

	function safeParseInt(v) {
		const n = parseInt(v, 10);
		return isNaN(n) ? 0 : n;
	}

	function normalizeProductType(v) {
		const s = String(v == null ? '' : v).toUpperCase();
		return s === 'DEALER' ? 'DEALER' : 'COMPANY';
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

	function goProductDetail(detailUrl) {
		if (!detailUrl) {
			alert('상품 상세 경로가 설정되지 않았습니다.');
			return;
		}
		location.href = detailUrl;
	}

	function getSelectedItems() {
		const items = [];
		$('.wishList-check:checked').each(function() {
			const targetId = $(this).data('id');
			const productType = $(this).data('product-type');

			if (targetId != null && productType) {
				items.push({
					productType: String(productType),
					targetId: Number(targetId)
				});
			}
		});
		return items;
	}

	function apiRemoveBatch(items) {
		return $.ajax({
			url: '/api/customer/wishlist/remove-batch',
			method: 'POST',
			contentType: 'application/json',
			dataType: 'text',
			data: JSON.stringify({ items: items })
		});
	}

	function openPanel($row, $panel) {
		if (!$row || !$row.length || !$panel || !$panel.length) return;

		$row.attr('aria-hidden', 'false');
		$panel.css({ overflow: 'hidden', maxHeight: '0px' });

		requestAnimationFrame(function() {
			if (!$panel.hasClass('is-open')) {
				$panel.addClass('is-open');
			}

			const h = $panel.get(0).scrollHeight;
			$panel.css({ overflow: 'hidden', maxHeight: h + 'px' });

			$panel.off('transitionend.wishlist').on('transitionend.wishlist', function(e) {
				if (e && e.originalEvent && e.originalEvent.propertyName !== 'max-height') return;
				if (!$panel.hasClass('is-open')) return;
				$panel.css({ maxHeight: 'none', overflow: 'visible' });
			});

			setTimeout(function() {
				if (!$panel.hasClass('is-open')) return;
				const curr = String($panel.css('max-height') || '');
				if (curr !== 'none') {
					$panel.css({ maxHeight: $panel.get(0).scrollHeight + 'px' });
				}
			}, 50);
		});
	}

	function closePanel($row, $panel) {
		if (!$row || !$row.length || !$panel || !$panel.length) return;

		$panel.off('transitionend.wishlist');

		const h = $panel.get(0).scrollHeight;
		$panel.css({ overflow: 'hidden', maxHeight: h + 'px' });
		$panel.get(0).offsetHeight;
		$panel.css({ maxHeight: '0px', overflow: 'hidden' }).removeClass('is-open');
		$row.attr('aria-hidden', 'true');
	}

	function bindSelectUI() {
		const $checkAll = $('#wishList-checkAll');
		const $tbody = $('#wishList-tbody');
		const $btnDelete = $('#wishList-deleteBtn');

		function $checks() { return $('.wishList-check'); }

		function syncButtons() {
			const any = $checks().toArray().some(el => el.checked);
			$btnDelete.prop('disabled', !any);

			const all = $checks().toArray();
			if (!all.length) {
				$checkAll.prop('checked', false);
				$checkAll.prop('indeterminate', false);
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

		$btnDelete.on('click', function() {
			if (!requireLogin()) return;

			const items = getSelectedItems();
			if (!items.length) {
				alert('삭제할 상품을 선택해 주세요.');
				return;
			}

			if (!confirm('선택한 관심상품을 삭제하시겠습니까?')) return;

			apiRemoveBatch(items)
				.done(function(resCount) {
					if (window.IbioWish && typeof window.IbioWish.refreshCount === 'function') {
						window.IbioWish.refreshCount();
					} else {
						$('.front-header-wish-count').text(Number(resCount || 0));
					}

					alert('선택한 관심상품이 삭제되었습니다.');
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

	function bindOptionToggle() {
		let opened = null;

		$doc.on('click', '.wishList-add-toggle-btn', function() {
			if ($(this).prop('disabled')) return;

			const $btn = $(this);
			const targetKey = String($btn.data('target-key') || '');
			const $row = $('#wishList-add-panel-' + targetKey);
			if (!$row.length) return;

			const $panel = $row.find('.wishList-add-optpanel');
			if (!$panel.length) return;

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

		$doc.on('input', '.wishList-add-optqty', function() {
			const v = safeParseInt($(this).val());
			if (v < 1) $(this).val('1');
		});
	}

	function bindGoDetail() {
		$doc.on('click', '.wishList-go-detail', function() {
			const $btn = $(this);
			const $row = $btn.closest('.wishList-row');

			const detailUrl =
				$btn.attr('data-detail-url') ||
				$btn.data('detail-url') ||
				$row.attr('data-detail-url') ||
				$row.data('detail-url') ||
				'';

			goProductDetail(detailUrl);
		});
	}

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

		$('#wishList-searchBtn').on('click', function() {
			const $form = $('#wishListForm');
			if (!$form.length) return;
			$form.find('input[name="page"]').val('0');
		});
	}

	function bindAddCart() {
		$doc.on('click', '.wishList-add-cart', function() {
			if (!requireLogin()) return;

			if (!window.IbioCart || typeof window.IbioCart.addEntry !== 'function') {
				alert('장바구니 모듈(IbioCart)을 찾을 수 없습니다. cart.js 로딩 순서를 확인해 주세요.');
				return;
			}

			const $panel = $(this).closest('.wishList-add-optpanel');
			const $optRow = $panel.closest('.wishList-add-optrow');
			const productId = Number($optRow.data('parent-id'));
			const productType = normalizeProductType($optRow.data('product-type'));

			if (!productId || productId <= 0) {
				alert('상품 정보를 찾을 수 없습니다.');
				return;
			}

			const rowKey = String($optRow.data('row-key') || '');
			const $productRow = $('.wishList-row[data-row-key="' + rowKey + '"]');
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
				productType: productType,
				productId: productId,
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