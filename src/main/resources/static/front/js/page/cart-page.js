/* global jQuery */
(function(window, $) {
	'use strict';

	if (!window.IbioCart) {
		console.error('[cart-page] IbioCart not found. cart.js 로딩 순서를 확인하세요.');
		return;
	}

	// -------------------------
	// 유틸
	// -------------------------
	function safeParseInt(v) {
		var n = parseInt(v, 10);
		return isNaN(n) ? 0 : n;
	}

	function formatMoney(n) {
		n = Number(n || 0);
		return n.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
	}

	function escapeHtml(str) {
		str = (str === undefined || str === null) ? '' : String(str);
		return str
			.replace(/&/g, '&amp;')
			.replace(/</g, '&lt;')
			.replace(/>/g, '&gt;')
			.replace(/"/g, '&quot;')
			.replace(/'/g, '&#39;');
	}

	function buildOptionLabel(opt) {
		var parts = [];
		if (opt.optionGroupName) parts.push(opt.optionGroupName);
		if (opt.optionName) parts.push(opt.optionName);
		if (opt.optionCode) parts.push(opt.optionCode);
		var s = parts.join(' / ');
		return s ? s : '-';
	}

	function buildUnitText(opt) {
		return (opt && opt.unit) ? String(opt.unit) : '-';
	}

	function getCheckedRowKeys() {
		var keys = [];
		$('#cart-tbody').find('tr').each(function() {
			var $tr = $(this);
			var $chk = $tr.find('.row-check');
			if ($chk.is(':checked')) {
				keys.push({
					cartEntryId: $tr.data('cart-entry-id'),
					optIndex: safeParseInt($tr.data('opt-index'))
				});
			}
		});
		return keys;
	}

	// ✅ paymentStart로 넘길 sessionStorage 키
	function getPaymentSessionKey() {
		var memberId = (window.IbioCart && window.IbioCart.getCart && window.IbioCart.getCart().userId)
			? window.IbioCart.getCart().userId
			: null;
		return 'ibio_payment_payload_v1_u' + String(memberId || '');
	}

	// =========================
	// ✅ (추가) 빈 장바구니 접근 차단
	// =========================
	function redirectToIndexWithMessage(msg) {
		try { alert(msg); } catch (e) { }
		window.location.href = '/';
	}

	function guardCartOrRedirect() {
		var cart = window.IbioCart.getCart();

		// 비로그인(또는 인증 플래그 누락 등으로 memberId 못가져오는 경우 포함)
		if (!cart || !cart.userId) {
			redirectToIndexWithMessage('로그인이 필요합니다.');
			return false;
		}

		// 장바구니 비어있음
		if (!Array.isArray(cart.items) || cart.items.length === 0) {
			redirectToIndexWithMessage('장바구니에 담긴 상품이 없습니다.');
			return false;
		}

		return true;
	}

	// -------------------------
	// 렌더링(테이블)
	// -------------------------
	function renderCartTable() {
		var cart = window.IbioCart.getCart();

		var $tbody = $('#cart-tbody');
		$tbody.empty();

		if (!cart || !Array.isArray(cart.items) || cart.items.length === 0) {
			updateTotals();
			updateCheckAllState();
			return;
		}

		cart.items.forEach(function(entry) {
			var entryId = entry.cartEntryId;
			var productId = entry.productId;
			var productName = entry.productName || '';
			var productImageUrl = entry.productImageUrl || '/front/image/sample/100-100.png';

			var options = Array.isArray(entry.options) ? entry.options : [];
			if (options.length === 0) {
				options = [{
					optionGroupName: '',
					optionName: '',
					optionCode: '',
					unit: '-',
					unitPrice: 0,
					quantity: 1,
					linePrice: 0
				}];
			}

			options.forEach(function(opt, optIndex) {
				var unitPrice = safeParseInt(opt.unitPrice);
				var qty = safeParseInt(opt.quantity);
				if (qty < 1) qty = 1;

				var $tr = $('<tr></tr>');
				$tr.attr('data-cart-entry-id', entryId);
				$tr.attr('data-opt-index', optIndex);

				var $tdCheck = $('<td class="col-check"></td>');
				$tdCheck.append('<input type="checkbox" class="row-check">');

				var $tdCat = $('<td class="col-catno"></td>').text(productId != null ? String(productId) : '-');

				var $tdName = $('<td class="col-name"></td>');
				var nameHtml = ''
					+ '<div class="name-cell">'
					+ '  <img src="' + escapeHtml(productImageUrl) + '" class="thumb" alt="">'
					+ '  <div class="texts">'
					+ '    <div class="brand">-</div>'
					+ '    <div class="title"></div>'
					+ '    <div class="cat">Product ID : <span></span></div>'
					+ '  </div>'
					+ '</div>';
				$tdName.html(nameHtml);
				$tdName.find('.title').text(productName || '-');
				$tdName.find('.cat span').text(productId != null ? String(productId) : '-');

				var $tdOpt = $('<td class="col-calib"></td>');
				var optLabel = buildOptionLabel(opt);
				var $optInput = $('<input type="text" class="form-control form-control-sm option-input" readonly>');
				$optInput.val(optLabel);
				$tdOpt.append($optInput);

				var $tdPrice = $('<td class="col-price"></td>');
				$tdPrice.append('<span class="price unit-price" data-value="' + unitPrice + '">' + formatMoney(unitPrice) + '</span>원');

				var $tdDealer = $('<td class="col-dealer"></td>');
				$tdDealer.append('<strong class="dealer price" data-value="0">-</strong>');

				var unitText = buildUnitText(opt);
				var $tdUnit = $('<td class="col-unit"></td>').text(unitText);

				var $tdQty = $('<td class="col-qty"></td>');
				var qtyHtml = ''
					+ '<div class="qty-stepper" data-min="1">'
					+ '  <input type="number" class="qty-input" value="' + qty + '" min="1">'
					+ '  <div class="steppers">'
					+ '    <button type="button" class="btn-step up"><i class="fa fa-angle-up"></i></button>'
					+ '    <button type="button" class="btn-step down"><i class="fa fa-angle-down"></i></button>'
					+ '  </div>'
					+ '</div>';
				$tdQty.html(qtyHtml);

				$tr.append($tdCheck, $tdCat, $tdName, $tdOpt, $tdPrice, $tdDealer, $tdUnit, $tdQty);
				$tbody.append($tr);
			});
		});

		$('#cart-checkall').prop('checked', false);

		updateTotals();
		updateCheckAllState();
	}

	// -------------------------
	// 체크박스/합계
	// -------------------------
	function updateCheckAllState() {
		var $rows = $('#cart-tbody').find('.row-check');
		if ($rows.length === 0) {
			$('#cart-checkall').prop('checked', false);
			return;
		}
		var allChecked = true;
		$rows.each(function() {
			if (!$(this).is(':checked')) allChecked = false;
		});
		$('#cart-checkall').prop('checked', allChecked);
	}

	function calcSelectedTotalsFromDom() {
		var productsTotal = 0;

		$('#cart-tbody').find('tr').each(function() {
			var $tr = $(this);
			var checked = $tr.find('.row-check').is(':checked');
			if (!checked) return;

			var unitPrice = safeParseInt($tr.find('.unit-price').data('value'));
			var qty = safeParseInt($tr.find('.qty-input').val());
			if (qty < 1) qty = 1;

			productsTotal += (unitPrice * qty);
		});

		var vat = Math.floor(productsTotal * 0.10);
		var shipping = 0;
		var discount = 0;
		var finalTotal = productsTotal + vat + shipping - discount;

		return {
			productsTotal: productsTotal,
			vat: vat,
			shipping: shipping,
			discount: discount,
			finalTotal: finalTotal
		};
	}

	function updateTotals() {
		var t = calcSelectedTotalsFromDom();

		$('#total-products').text(formatMoney(t.productsTotal));
		$('#total-vat').text(formatMoney(t.vat));
		$('#total-shipping').text(formatMoney(t.shipping));
		$('#total-discount').text(formatMoney(t.discount));
		$('#total-final').text(formatMoney(t.finalTotal));
		$('#m-total-final').text(formatMoney(t.finalTotal));
	}

	// =========================
	// ✅ (추가) 모바일 상세내역 모달 렌더링
	// - 현재 선택된 항목 기준으로 상세내역을 보여줌
	// =========================
	function renderMobileDetailModal() {
		var t = calcSelectedTotalsFromDom();

		var $root = $('#detailModal');
		if (!$root.length) return;

		var $items = $root.find('.cart-mobile-modal-items');
		if (!$items.length) return;

		var html = ''
			+ '<div class="cart-mobile-modal-summary-list">'
			+ '  <div class="row"><div class="label black">총 상품금액</div><div class="value"><strong>' + formatMoney(t.productsTotal) + '</strong> 원</div></div>'
			+ '  <div class="row"><div class="label black">부가세(10%)</div><div class="value"><strong>' + formatMoney(t.vat) + '</strong> 원</div></div>'
			+ '  <div class="row"><div class="label black">총 배송비</div><div class="value"><strong>' + formatMoney(t.shipping) + '</strong> 원</div></div>'
			+ '  <div class="row"><div class="label black">총 할인금액</div><div class="value"><strong>' + formatMoney(t.discount) + '</strong> 원</div></div>'
			+ '  <div class="row total"><div class="label black">최종 결제 금액</div><div class="value"><strong>' + formatMoney(t.finalTotal) + '</strong> 원</div></div>'
			+ '</div>';

		$items.html(html);
	}

	// -------------------------
	// localStorage 반영(삭제/수량)
	// -------------------------
	function applyDomQuantitiesToStorage() {
		var cart = window.IbioCart.getCart();
		if (!cart || !Array.isArray(cart.items)) return;

		$('#cart-tbody').find('tr').each(function() {
			var $tr = $(this);
			var entryId = $tr.data('cart-entry-id');
			var optIndex = safeParseInt($tr.data('opt-index'));
			var qty = safeParseInt($tr.find('.qty-input').val());
			if (qty < 1) qty = 1;

			var entry = cart.items.find(function(e) { return e.cartEntryId === entryId; });
			if (!entry || !Array.isArray(entry.options)) return;
			if (!entry.options[optIndex]) return;

			entry.options[optIndex].quantity = qty;
			entry.options[optIndex].linePrice = safeParseInt(entry.options[optIndex].unitPrice) * qty;
		});

		window.IbioCart.saveCart(cart);
		window.IbioCart.updateHeaderCount();
	}

	function deleteSelectedRowsFromStorage(options) {
		options = options || {};
		var skipEmptyGuard = !!options.skipEmptyGuard;

		var keys = getCheckedRowKeys();
		if (keys.length === 0) {
			alert('선택된 항목이 없습니다.');
			return;
		}

		var cart = window.IbioCart.getCart();
		if (!cart || !Array.isArray(cart.items)) return;

		var map = {};
		keys.forEach(function(k) {
			if (!map[k.cartEntryId]) map[k.cartEntryId] = [];
			map[k.cartEntryId].push(k.optIndex);
		});

		cart.items = cart.items.filter(function(entry) {
			var delIdxs = map[entry.cartEntryId];
			if (!delIdxs) return true;

			if (!Array.isArray(entry.options)) entry.options = [];
			delIdxs.sort(function(a, b) { return b - a; }).forEach(function(idx) {
				if (idx >= 0 && idx < entry.options.length) {
					entry.options.splice(idx, 1);
				}
			});

			return entry.options.length > 0;
		});

		window.IbioCart.saveCart(cart);
		window.IbioCart.updateHeaderCount();
		renderCartTable();

		// ✅ 삭제 후 비었으면 바로 튕김 (단, 구매 이동 중이면 스킵)
		if (!skipEmptyGuard) {
			if (!guardCartOrRedirect()) return;
		}
	}


	// =========================
	// ✅ 주문서 payload 생성 + 이동 + 장바구니에서 제거
	// =========================
	function buildPaymentPayloadFromSelection() {
		var cart = window.IbioCart.getCart();
		var memberId = cart ? cart.userId : null;
		if (!memberId) return null;

		var selectedKeys = getCheckedRowKeys();
		if (!selectedKeys || selectedKeys.length === 0) return null;

		var map = {};
		selectedKeys.forEach(function(k) {
			if (!map[k.cartEntryId]) map[k.cartEntryId] = [];
			map[k.cartEntryId].push(k.optIndex);
		});

		var items = [];

		(cart.items || []).forEach(function(entry) {
			var idxs = map[entry.cartEntryId];
			if (!idxs || idxs.length === 0) return;

			idxs.forEach(function(optIndex) {
				var opt = (entry.options || [])[optIndex];
				if (!opt) return;

				items.push({
					cartEntryId: entry.cartEntryId,
					optIndex: optIndex,

					productId: entry.productId,
					productName: entry.productName || '',
					productImageUrl: entry.productImageUrl || '/front/image/sample/100-100.png',

					optionGroupId: opt.optionGroupId != null ? Number(opt.optionGroupId) : null,
					optionGroupName: opt.optionGroupName || '',
					optionId: opt.optionId != null ? Number(opt.optionId) : null,
					optionName: opt.optionName || '',
					optionCode: opt.optionCode || '',
					unit: opt.unit || '-',

					unitPrice: safeParseInt(opt.unitPrice),
					quantity: safeParseInt(opt.quantity),
					linePrice: safeParseInt(opt.linePrice)
				});
			});
		});

		if (items.length === 0) return null;

		return {
			version: 1,
			userId: memberId,
			createdAt: Date.now(),
			items: items
		};
	}

	function savePaymentPayloadAndRemoveFromCart(payload) {
		var key = getPaymentSessionKey();
		try {
			sessionStorage.setItem(key, JSON.stringify(payload));
		} catch (e) {
			console.error('[cart-page] sessionStorage save error', e);
			alert('주문서 정보를 저장할 수 없습니다. (브라우저 저장공간 문제)');
			return false;
		}

		deleteSelectedRowsFromStorage({ skipEmptyGuard: true });
		return true;
	}

	function goPaymentStart() {
		applyDomQuantitiesToStorage();

		var payload = buildPaymentPayloadFromSelection();
		if (!payload) {
			alert('선택된 상품이 없습니다.');
			return;
		}

		var ok = savePaymentPayloadAndRemoveFromCart(payload);
		if (!ok) return;

		window.location.href = '/customer/paymentStart';
	}

	// -------------------------
	// 이벤트
	// -------------------------
	function bindEvents() {
		$(document).on('change', '#cart-checkall', function() {
			var checked = $(this).is(':checked');
			$('#cart-tbody').find('.row-check').prop('checked', checked);
			updateTotals();
		});

		$(document).on('change', '#cart-tbody .row-check', function() {
			updateCheckAllState();
			updateTotals();
		});

		$(document).on('click', '#cart-tbody .btn-step.up', function() {
			var $tr = $(this).closest('tr');
			var $input = $tr.find('.qty-input');
			var v = safeParseInt($input.val());
			v = v + 1;
			if (v < 1) v = 1;
			$input.val(v);
			updateTotals();
		});

		$(document).on('click', '#cart-tbody .btn-step.down', function() {
			var $tr = $(this).closest('tr');
			var $input = $tr.find('.qty-input');
			var v = safeParseInt($input.val());
			v = v - 1;
			if (v < 1) v = 1;
			$input.val(v);
			updateTotals();
		});

		$(document).on('input', '#cart-tbody .qty-input', function() {
			var v = safeParseInt($(this).val());
			if (v < 1) v = 1;
			$(this).val(v);
			updateTotals();
		});

		$(document).on('click', '#btn-qty-apply', function() {
			applyDomQuantitiesToStorage();
			alert('수량이 반영되었습니다.');
		});

		$(document).on('click', '#btn-delete', function() {
			deleteSelectedRowsFromStorage();
		});

		// ⚠️ "장바구니 비우기" 버튼은 현재 코드가 '선택 삭제'로만 동작하고 있습니다.
		// 요구사항이 "전체 비우기"면 별도 함수가 필요하지만, 지금은 기존 기능 유지(환님 코드 유지).
		$(document).on('click', '#btn-empty', function() {
			deleteSelectedRowsFromStorage();
		});

		$(document).on('click', '#btn-buy-selected', function() {
			goPaymentStart();
		});

		$(document).on('click', '#btn-buy-all', function() {
			$('#cart-checkall').prop('checked', true).trigger('change');
			goPaymentStart();
		});

		// ✅ 모달 열릴 때 상세내역 렌더링 (Bootstrap modal 이벤트)
		$(document).on('show.bs.modal', '#detailModal', function() {
			renderMobileDetailModal();
		});

		// ✅ 체크/수량 변경 시 모달 열려있으면 즉시 반영
		$(document).on('change input', '#cart-checkall, #cart-tbody .row-check, #cart-tbody .qty-input', function() {
			updateTotals();
			if ($('#detailModal').hasClass('in') || $('#detailModal').is(':visible')) {
				renderMobileDetailModal();
			}
		});
	}

	function init() {
		// ✅ 진입 즉시 가드 (비었으면 안내 후 / 이동)
		if (!guardCartOrRedirect()) return;

		renderCartTable();
		bindEvents();
		window.IbioCart.updateHeaderCount();
	}

	$(function() {
		init();
	});

})(window, jQuery);
