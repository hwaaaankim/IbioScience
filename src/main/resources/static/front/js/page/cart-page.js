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
		// 옵션 표시 규칙(필요시 여기만 조정)
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

	function isMobileView() {
		// CSS breakpoint와 동일하게 맞추세요(현재는 768 기준)
		return window.matchMedia && window.matchMedia('(max-width: 767px)').matches;
	}

	// -------------------------
	// 렌더링(테이블)
	// -------------------------
	function renderCartTable() {
		var cart = window.IbioCart.getCart();

		var $tbody = $('#cart-tbody');
		$tbody.empty();

		// 비어있으면 합계도 초기화
		if (!cart || !Array.isArray(cart.items) || cart.items.length === 0) {
			updateTotals(); // 0 처리
			updateCheckAllState();
			return;
		}

		// ✅ Entry 내부 options를 "행"으로 펼쳐서 표시
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

				// 체크
				var $tdCheck = $('<td class="col-check"></td>');
				$tdCheck.append('<input type="checkbox" class="row-check">');

				// CatNo (현재 저장 구조엔 catNo 없으니 productId로 표시)
				var $tdCat = $('<td class="col-catno"></td>').text(productId != null ? String(productId) : '-');

				// 품명(이미지+상품명)
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

				// 옵션(input readonly)
				var $tdOpt = $('<td class="col-calib"></td>');
				var optLabel = buildOptionLabel(opt);
				var $optInput = $('<input type="text" class="form-control form-control-sm option-input" readonly>');
				$optInput.val(optLabel);
				$tdOpt.append($optInput);

				// 판매가
				var $tdPrice = $('<td class="col-price"></td>');
				$tdPrice.append('<span class="price unit-price" data-value="' + unitPrice + '">' + formatMoney(unitPrice) + '</span>원');

				// 딜러가(현재 저장 구조에 없음 → 0 또는 -)
				var $tdDealer = $('<td class="col-dealer"></td>');
				$tdDealer.append('<strong class="dealer price" data-value="0">-</strong>');

				// 단위
				var unitText = buildUnitText(opt);
				var $tdUnit = $('<td class="col-unit"></td>').text(unitText);

				// 수량(stepper)
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

				// 조립
				$tr.append($tdCheck, $tdCat, $tdName, $tdOpt, $tdPrice, $tdDealer, $tdUnit, $tdQty);

				$tbody.append($tr);
			});
		});

		// 기본: 전체 체크 해제
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

		var vat = Math.floor(productsTotal * 0.10); // 부가세 10%
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

	// -------------------------
	// localStorage 반영(삭제/수량)
	// -------------------------
	function applyDomQuantitiesToStorage() {
		var cart = window.IbioCart.getCart();
		if (!cart || !Array.isArray(cart.items)) return;

		// DOM에서 (entryId, optIndex) 기준으로 quantity를 cart에 반영
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

	function deleteSelectedRowsFromStorage() {
		var keys = getCheckedRowKeys();
		if (keys.length === 0) {
			alert('선택된 항목이 없습니다.');
			return;
		}

		var cart = window.IbioCart.getCart();
		if (!cart || !Array.isArray(cart.items)) return;

		// (entryId -> 삭제할 optIndex 목록)으로 묶기
		var map = {};
		keys.forEach(function(k) {
			if (!map[k.cartEntryId]) map[k.cartEntryId] = [];
			map[k.cartEntryId].push(k.optIndex);
		});

		// 실제 삭제
		cart.items = cart.items.filter(function(entry) {
			var delIdxs = map[entry.cartEntryId];
			if (!delIdxs) return true;

			if (!Array.isArray(entry.options)) entry.options = [];
			// optIndex 내림차순 삭제
			delIdxs.sort(function(a, b) { return b - a; }).forEach(function(idx) {
				if (idx >= 0 && idx < entry.options.length) {
					entry.options.splice(idx, 1);
				}
			});

			// 옵션이 0개면 entry 자체 삭제
			return entry.options.length > 0;
		});

		window.IbioCart.saveCart(cart);
		window.IbioCart.updateHeaderCount();

		renderCartTable();
	}

	// -------------------------
	// 모바일 상세내역 모달 렌더링
	// -------------------------
	function buildModalItemHtml(item) {
		// item: { imageUrl, name, optionLabel, unitText, unitPrice, qty, lineTotal, productId }
		var html = ''
			+ '<div class="cart-mobile-modal-item">'
			+ '  <div class="cart-mobile-modal-item-thumb">'
			+ '    <img src="' + escapeHtml(item.imageUrl) + '" alt="">'
			+ '  </div>'
			+ '  <div class="cart-mobile-modal-item-info">'
			+ '    <div class="cart-mobile-modal-item-name">' + escapeHtml(item.name || '-') + '</div>'
			+ '    <div class="cart-mobile-modal-item-sub">'
			+ '      <div class="cart-mobile-modal-item-line"><span class="k">Product ID</span><span class="v">' + escapeHtml(item.productId) + '</span></div>'
			+ '      <div class="cart-mobile-modal-item-line"><span class="k">옵션</span><span class="v">' + escapeHtml(item.optionLabel || '-') + '</span></div>'
			+ '      <div class="cart-mobile-modal-item-line"><span class="k">단위</span><span class="v">' + escapeHtml(item.unitText || '-') + '</span></div>'
			+ '    </div>'
			+ '    <div class="cart-mobile-modal-item-prices">'
			+ '      <div class="cart-mobile-modal-item-price-row"><span class="k">단가</span><span class="v">' + formatMoney(item.unitPrice) + ' 원</span></div>'
			+ '      <div class="cart-mobile-modal-item-price-row"><span class="k">수량</span><span class="v">' + escapeHtml(item.qty) + '</span></div>'
			+ '      <div class="cart-mobile-modal-item-price-row strong"><span class="k">금액</span><span class="v">' + formatMoney(item.lineTotal) + ' 원</span></div>'
			+ '    </div>'
			+ '  </div>'
			+ '</div>';
		return html;
	}

	function renderMobileDetailModal() {
		var $modal = $('#detailModal');
		if ($modal.length === 0) return;

		var $itemsBox = $modal.find('.cart-mobile-modal-items');
		$itemsBox.empty();

		// 현재 체크된 행이 있으면 "체크된 것" 기준, 없으면 "전체" 기준으로 모달을 보여주기
		var checkedKeys = getCheckedRowKeys();
		var useSelection = checkedKeys && checkedKeys.length > 0;

		// DOM기준으로 정확히 현재 입력된 qty를 반영해서 계산/표시
		var modalItems = [];
		var productsTotal = 0;

		$('#cart-tbody').find('tr').each(function() {
			var $tr = $(this);
			var checked = $tr.find('.row-check').is(':checked');
			if (useSelection && !checked) return;

			var entryId = $tr.data('cart-entry-id');
			var optIndex = safeParseInt($tr.data('opt-index'));

			// 테이블에서 보이는 값들 추출
			var productId = $tr.find('.col-catno').text() || '-';
			var name = $tr.find('.col-name .title').text() || '-';
			var imageUrl = $tr.find('.col-name img.thumb').attr('src') || '/front/image/sample/100-100.png';
			var optionLabel = $tr.find('.option-input').val() || '-';
			var unitText = $tr.find('.col-unit').text() || '-';

			var unitPrice = safeParseInt($tr.find('.unit-price').data('value'));
			var qty = safeParseInt($tr.find('.qty-input').val());
			if (qty < 1) qty = 1;

			var lineTotal = unitPrice * qty;
			productsTotal += lineTotal;

			modalItems.push({
				cartEntryId: entryId,
				optIndex: optIndex,
				productId: productId,
				name: name,
				imageUrl: imageUrl,
				optionLabel: optionLabel,
				unitText: unitText,
				unitPrice: unitPrice,
				qty: qty,
				lineTotal: lineTotal
			});
		});

		var vat = Math.floor(productsTotal * 0.10);
		var shipping = 0;
		var discount = 0;
		var finalTotal = productsTotal + vat + shipping - discount;

		// 상단 요약(모달 내부)
		var summaryHtml = ''
			+ '<div class="cart-mobile-modal-summary">'
			+ '  <div class="cart-mobile-modal-summary-row"><span class="k">총 상품금액</span><span class="v">' + formatMoney(productsTotal) + ' 원</span></div>'
			+ '  <div class="cart-mobile-modal-summary-row"><span class="k">부가세(10%)</span><span class="v">' + formatMoney(vat) + ' 원</span></div>'
			+ '  <div class="cart-mobile-modal-summary-row"><span class="k">총 배송비</span><span class="v">' + formatMoney(shipping) + ' 원</span></div>'
			+ '  <div class="cart-mobile-modal-summary-row"><span class="k">총 할인금액</span><span class="v">-' + formatMoney(discount) + ' 원</span></div>'
			+ '  <div class="cart-mobile-modal-summary-row strong"><span class="k">최종 결제 금액</span><span class="v">' + formatMoney(finalTotal) + ' 원</span></div>'
			+ '</div>';

		$itemsBox.append(summaryHtml);

		// 아이템들
		if (!modalItems.length) {
			$itemsBox.append('<div class="cart-mobile-modal-empty">표시할 상품이 없습니다.</div>');
			return;
		}

		var listHtml = '<div class="cart-mobile-modal-list">';
		modalItems.forEach(function(mi) {
			listHtml += buildModalItemHtml(mi);
		});
		listHtml += '</div>';

		$itemsBox.append(listHtml);
	}

	function openMobileDetailModal() {
		var $modal = $('#detailModal');
		if ($modal.length === 0) return;

		// Bootstrap modal 동작(오버레이 클릭 닫힘: backdrop=true, keyboard=true)
		$modal.modal({
			backdrop: true,
			keyboard: true,
			show: true
		});
	}

	// -------------------------
	// 이벤트
	// -------------------------
	function bindEvents() {
		// 전체 체크
		$(document).on('change', '#cart-checkall', function() {
			var checked = $(this).is(':checked');
			$('#cart-tbody').find('.row-check').prop('checked', checked);
			updateTotals();
		});

		// 개별 체크
		$(document).on('change', '#cart-tbody .row-check', function() {
			updateCheckAllState();
			updateTotals();
		});

		// 수량 스텝퍼
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

		// 수량 직접 입력
		$(document).on('input', '#cart-tbody .qty-input', function() {
			var v = safeParseInt($(this).val());
			if (v < 1) v = 1;
			$(this).val(v);
			updateTotals();
		});

		// 수량 변경하기(저장 반영)
		$(document).on('click', '#btn-qty-apply', function() {
			applyDomQuantitiesToStorage();
			alert('수량이 반영되었습니다.');
		});

		// 삭제(체크된 행만)
		$(document).on('click', '#btn-delete', function() {
			deleteSelectedRowsFromStorage();
		});

		// 장바구니 비우기(요구사항대로: 체크된 것만 삭제)
		$(document).on('click', '#btn-empty', function() {
			deleteSelectedRowsFromStorage();
		});

		// 구매 버튼(현재는 동작만 안내)
		$(document).on('click', '#btn-buy-selected', function() {
			var keys = getCheckedRowKeys();
			if (keys.length === 0) {
				alert('선택된 상품이 없습니다.');
				return;
			}
			alert('선택상품 구매는 주문서 단계 구현 후 연결하겠습니다.');
		});

		$(document).on('click', '#btn-buy-all', function() {
			$('#cart-checkall').prop('checked', true).trigger('change');
			alert('전체구매는 주문서 단계 구현 후 연결하겠습니다.');
		});

		// ✅ 모바일 상세내역 버튼(모달 열기 + 내용 렌더링)
		$(document).on('click', '[data-target="#detailModal"]', function(e) {
			// PC에서도 눌릴 수 있지만, 의도상 모바일 UX이므로 모바일 기준으로만 렌더링해도 되고
			// 여기서는 둘 다 동작하도록 처리합니다.
			e.preventDefault();

			// 모달 열기
			openMobileDetailModal();
		});

		// ✅ 모달이 실제로 열린 뒤에 렌더링(애니메이션/레이아웃 안정화)
		$(document).on('shown.bs.modal', '#detailModal', function() {
			renderMobileDetailModal();
		});

		// ✅ 모달 닫힐 때 내부 초기화(선택)
		$(document).on('hidden.bs.modal', '#detailModal', function() {
			var $modal = $('#detailModal');
			$modal.find('.cart-mobile-modal-items').empty();
		});
	}

	function init() {
		renderCartTable();
		bindEvents();
		// 헤더 카운트는 cart.js에서 로드/저장 때 recalc하므로, 페이지 진입 시 한 번 갱신
		window.IbioCart.updateHeaderCount();
	}

	$(function() {
		init();
	});

})(window, jQuery);
