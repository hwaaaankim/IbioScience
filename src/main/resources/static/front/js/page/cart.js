/* global jQuery */

(function(window, $) {
	'use strict';

	var STORAGE_KEY_BASE = 'ibio_cart_v1';

	// =========================
	// 유틸
	// =========================
	function safeParseInt(v) {
		var n = parseInt(v, 10);
		return isNaN(n) ? 0 : n;
	}

	function safeString(v) {
		return (v === undefined || v === null) ? '' : String(v);
	}

	function nowTs() {
		return Date.now();
	}

	// =========================
	// 로그인 유저 정보 (frontScript에서 주입)
	// =========================
	function isAuthenticated() {
		return !!(window.__isAuthenticated === true);
	}

	function getCurrentMemberId() {
		// 비로그인 안전
		if (!isAuthenticated()) return null;

		var v = window.__loginMemberId;
		if (v === undefined || v === null || v === '') return null;

		var idNum = Number(v);
		if (!isFinite(idNum) || idNum <= 0) return null;

		return idNum;
	}

	function getStorageKeyForUser(memberId) {
		return STORAGE_KEY_BASE + '_u' + String(memberId);
	}

	// =========================
	// 장바구니 Core (Entry 단위: "담기 1회" = 1건)
	// =========================
	/**
	 * cart 구조 (유저별 저장):
	 * {
	 *   userId: 123,
	 *   items: [
	 *     {
	 *       cartEntryId: "E-...",
	 *       userId: 123,
	 *       productId: 10,
	 *       productName: "...",
	 *       productImageUrl: "...",
	 *       createdAt: 1700000000000,
	 *       options: [
	 *         { optionGroupId, optionGroupName, optionId, optionName, optionCode, unit, unitPrice, quantity, linePrice }
	 *       ],
	 *       entryPrice: 12345
	 *     }
	 *   ],
	 *   totalQuantity: 2, // ✅ "장바구니 건수" (Entry 개수)
	 *   totalPrice: 99999
	 * }
	 */
	var IbioCart = {

		loadCart: function() {
			var memberId = getCurrentMemberId();
			if (!memberId) {
				return { userId: null, items: [], totalQuantity: 0, totalPrice: 0 };
			}

			if (!window.localStorage) {
				return { userId: memberId, items: [], totalQuantity: 0, totalPrice: 0 };
			}

			var key = getStorageKeyForUser(memberId);
			var raw = localStorage.getItem(key);

			if (!raw) {
				return { userId: memberId, items: [], totalQuantity: 0, totalPrice: 0 };
			}

			try {
				var cart = JSON.parse(raw);

				if (!cart || !Array.isArray(cart.items)) {
					return { userId: memberId, items: [], totalQuantity: 0, totalPrice: 0 };
				}

				if (Number(cart.userId) !== Number(memberId)) {
					return { userId: memberId, items: [], totalQuantity: 0, totalPrice: 0 };
				}

				this.recalcTotals(cart);
				return cart;
			} catch (e) {
				console.error('[IbioCart] JSON parse error', e);
				return { userId: memberId, items: [], totalQuantity: 0, totalPrice: 0 };
			}
		},

		saveCart: function(cart) {
			var memberId = getCurrentMemberId();
			if (!memberId) return;
			if (!window.localStorage) return;

			cart = cart || { userId: memberId, items: [] };
			cart.userId = memberId;

			this.recalcTotals(cart);

			var key = getStorageKeyForUser(memberId);
			localStorage.setItem(key, JSON.stringify(cart));
		},

		recalcTotals: function(cart) {
			var totalEntryCount = 0;
			var totalPrice = 0;

			(cart.items || []).forEach(function(entry) {
				if (!Array.isArray(entry.options)) entry.options = [];

				var entryPrice = 0;

				entry.options.forEach(function(opt) {
					var q = safeParseInt(opt.quantity);
					var up = safeParseInt(opt.unitPrice);

					if (q < 1) q = 1;

					opt.quantity = q;
					opt.unitPrice = up;
					opt.linePrice = q * up;

					entryPrice += opt.linePrice;
				});

				entry.entryPrice = entryPrice;

				totalEntryCount += 1; // ✅ "담기 1회" = 1건
				totalPrice += entryPrice;
			});

			cart.totalQuantity = totalEntryCount;
			cart.totalPrice = totalPrice;
		},

		getCart: function() {
			return this.loadCart();
		},

		// ✅ 클릭 1회당 entry 1개 추가 (동일 제품이어도 합치지 않음)
		addEntry: function(entry) {
			var memberId = getCurrentMemberId();
			if (!memberId) {
				alert('로그인이 필요합니다.');
				return this.getCart();
			}

			var cart = this.getCart();

			entry.userId = memberId;
			entry.cartEntryId = 'E-' + memberId + '-' + nowTs() + '-' + Math.random().toString(16).slice(2);
			entry.createdAt = nowTs();

			if (!Array.isArray(entry.options)) entry.options = [];
			entry.entryPrice = 0;

			entry.options.forEach(function(opt) {
				var q = safeParseInt(opt.quantity);
				var up = safeParseInt(opt.unitPrice);
				if (q < 1) q = 1;

				opt.quantity = q;
				opt.unitPrice = up;
				opt.linePrice = q * up;

				entry.entryPrice += opt.linePrice;
			});

			cart.items.push(entry);

			this.saveCart(cart);
			return cart;
		},

		getTotalQuantity: function() {
			var cart = this.getCart();
			return cart.totalQuantity || 0;
		},

		getTotalPrice: function() {
			var cart = this.getCart();
			return cart.totalPrice || 0;
		},

		updateHeaderCount: function() {
			var memberId = getCurrentMemberId();
			var count = memberId ? this.getTotalQuantity() : 0;

			$('.ibio-index-m-qnum').each(function() {
				$(this).text(count + '건');
			});
			$('.front-header-cart-count').text(count);
		},

		// =========================
		// DOM → Option 변환
		// =========================
		buildOptionFromRow: function($row) {
			var $check = $row.find('.product-list-row-check');
			if (!$check.length || !$check.is(':checked')) {
				return null;
			}

			var qtyInput = $row.find('.product-list-qty-input');
			var qty = safeParseInt(qtyInput.val());
			if (qty < 1) qty = 1;

			var unitPrice = safeParseInt($row.data('price'));

			var optionId = $row.data('option-id');
			var optionGroupId = $row.data('option-group-id');
			var optionGroupName = $row.data('option-group-name') || '';
			var optionName = $row.data('option-name') || '';
			var optionCode = $row.data('option-code') || '';

			// 단위: 4번째 칸(td index 3) 기준
			var unit = '-';
			var $tds = $row.find('td');
			if ($tds.length >= 4) {
				var unitText = $.trim($($tds[3]).text());
				if (unitText) unit = unitText;
			}

			return {
				optionGroupId: optionGroupId != null ? Number(optionGroupId) : null,
				optionGroupName: safeString(optionGroupName),
				optionId: optionId != null ? Number(optionId) : null,
				optionName: safeString(optionName),
				optionCode: safeString(optionCode),
				unit: safeString(unit),

				unitPrice: unitPrice,
				quantity: qty,
				linePrice: unitPrice * qty
			};
		},

		buildOptionsFromPanel: function($panel) {
			var self = this;
			var options = [];

			$panel.find('tbody tr').each(function() {
				var $row = $(this);
				if (!$row.find('.product-list-row-check').length) return;

				var opt = self.buildOptionFromRow($row);
				if (opt) options.push(opt);
			});

			return options;
		},

		// =========================
		// 이벤트 바인딩
		// =========================
		bindEvents: function() {
			var self = this;

			// 1) 리스트/상세 공통: 옵션 패널 안 "장바구니담기" 클릭 → Entry 1개 생성
			$(document).on('click', '.product-list-option-panel .product-list-btn-buy', function(e) {
				e.preventDefault();

				if (!getCurrentMemberId()) {
					alert('로그인이 필요합니다.');
					return;
				}

				var $btn = $(this);
				var $panel = $btn.closest('.product-list-option-panel');

				// 상품 정보 찾기 (리스트/상세 공통)
				var productId = null;
				var productName = '';
				var productImageUrl = '';

				// (A) 리스트 페이지
				var $layout = $btn.closest('.product-layout');
				if ($layout.length) {
					var $listContainer = $layout.find('.product-item-container.list-container');
					if ($listContainer.length) {
						productId = $listContainer.data('product-id') || null;
						productName = $.trim($listContainer.find('h4 a').first().text()) || '';
						var $img = $listContainer.find('img').first();
						if ($img.length) productImageUrl = $img.attr('src') || '';
					}
				}

				// (B) 상세 페이지
				if (!productId) {
					var $prodBox = $('#product');
					if ($prodBox.length) {
						productId = $prodBox.data('product-id') || null;
						productName = $.trim($('.title-product h1').first().text()) || '';
						var $img2 = $('.content-product-left .large-image img').first();
						if ($img2.length) productImageUrl = $img2.attr('src') || '';
					}
				}

				if (!productId) {
					alert('상품 정보를 찾을 수 없습니다.');
					return;
				}

				var options = self.buildOptionsFromPanel($panel);
				if (!options.length) {
					alert('선택된 옵션이 없습니다.\n옵션을 선택 후 장바구니에 담아 주세요.');
					return;
				}

				// ✅ 옵션이 몇 개든, 수량이 얼마든 "장바구니 1건"만 추가
				self.addEntry({
					productId: Number(productId),
					productName: productName,
					productImageUrl: productImageUrl,
					options: options
				});

				self.updateHeaderCount();
				alert('장바구니에 담겼습니다.');
			});

			// 2) 상세페이지 상단 "장바구니담기" (#button-cart) - 옵션 없는 상품
			$(document).on('click', '#button-cart', function(e) {
				e.preventDefault();

				if (!getCurrentMemberId()) {
					alert('로그인이 필요합니다.');
					return;
				}

				var $prodBox = $('#product');
				if (!$prodBox.length) {
					alert('상품 정보를 찾을 수 없습니다.');
					return;
				}

				var productId = $prodBox.data('product-id') || null;
				if (!productId || Number(productId) === 0) {
					alert('상품 정보를 찾을 수 없습니다.');
					return;
				}

				// 옵션 상품이면 옵션 탭에서 담도록 유도
				var hasOptions = $('#tab-option .product-list-option-table .product-list-row-check').length > 0;
				if (hasOptions) {
					alert('옵션이 있는 상품입니다.\n옵션 탭에서 옵션을 선택한 후 장바구니에 담아 주세요.');
					return;
				}

				var qtyInput = $prodBox.find('input[name="quantity"]');
				var qty = safeParseInt(qtyInput.val());
				if (qty < 1) qty = 1;

				var productName = $.trim($('.title-product h1').first().text()) || '';
				var $img = $('.content-product-left .large-image img').first();
				var productImageUrl = $img.length ? ($img.attr('src') || '') : '';

				var unitPrice = safeParseInt($prodBox.data('product-sale-price'));

				// ✅ 옵션 없는 상품도 entry 1건
				self.addEntry({
					productId: Number(productId),
					productName: productName,
					productImageUrl: productImageUrl,
					options: [{
						optionGroupId: null,
						optionGroupName: '',
						optionId: null,
						optionName: '',
						optionCode: '',
						unit: '-',
						unitPrice: unitPrice,
						quantity: qty,
						linePrice: unitPrice * qty
					}]
				});

				self.updateHeaderCount();
				alert('장바구니에 담겼습니다.');
			});
		},

		init: function() {
			this.bindEvents();
			this.updateHeaderCount();
		}
	};

	window.IbioCart = IbioCart;

	$(function() {
		IbioCart.init();
	});

})(window, jQuery);
