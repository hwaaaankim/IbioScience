/* /front/js/common/cart.js */
/* global jQuery */
(function(window, $) {
	'use strict';

	var STORAGE_KEY = 'ibio_cart_v1';

	// =========================
	// 유틸
	// =========================
	function safeParseInt(v) {
		var n = parseInt(v, 10);
		return isNaN(n) ? 0 : n;
	}

	function formatMoney(n) {
		n = Number(n || 0);
		return n.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
	}

	// =========================
	// 장바구니 Core
	// =========================
	var IbioCart = {
		// 장바구니 로드
		loadCart: function() {
			if (!window.localStorage) {
				return { items: [], totalQuantity: 0, totalPrice: 0 };
			}
			var raw = localStorage.getItem(STORAGE_KEY);
			if (!raw) {
				return { items: [], totalQuantity: 0, totalPrice: 0 };
			}
			try {
				var cart = JSON.parse(raw);
				if (!cart || !Array.isArray(cart.items)) {
					return { items: [], totalQuantity: 0, totalPrice: 0 };
				}
				this.recalcTotals(cart);
				return cart;
			} catch (e) {
				console.error('[IbioCart] JSON parse error', e);
				return { items: [], totalQuantity: 0, totalPrice: 0 };
			}
		},

		// 장바구니 저장
		saveCart: function(cart) {
			if (!window.localStorage) return;
			this.recalcTotals(cart);
			localStorage.setItem(STORAGE_KEY, JSON.stringify(cart));
		},

		// totals 재계산
		recalcTotals: function(cart) {
			var totalOptionCount = 0;
			var totalPrice = 0;

			(cart.items || []).forEach(function(item) {
				var q = safeParseInt(item.quantity);
				var up = safeParseInt(item.unitPrice);
				if (q < 1) q = 1;

				item.quantity = q;
				item.linePrice = q * up;

				totalOptionCount++;   // ✅ 옵션 하나당 +1
				totalPrice += item.linePrice;
			});

			cart.totalQuantity = totalOptionCount;   // 🔥 총 수량 = 옵션 개수
			cart.totalPrice = totalPrice;
		},


		getCart: function() {
			return this.loadCart();
		},

		// 동일 productId + optionId (null 포함) 면 수량 합침
		addItem: function(item) {
			var cart = this.getCart();

			var productId = item.productId;
			var optionId = item.optionId != null ? String(item.optionId) : '';

			var exist = cart.items.find(function(it) {
				var itOptId = it.optionId != null ? String(it.optionId) : '';
				return String(it.productId) === String(productId) &&
					itOptId === optionId;
			});

			if (exist) {
				exist.quantity = safeParseInt(exist.quantity) + safeParseInt(item.quantity);
				// unitPrice 는 동일한 옵션이라고 가정
			} else {
				item.cartItemId = 'P' + productId + '-O' + (optionId || 'NONE') + '-' + Date.now();
				cart.items.push(item);
			}

			this.saveCart(cart);
			return cart;
		},

		// 여러 옵션 한 번에 추가
		addItems: function(items) {
			var self = this;
			var cart = this.getCart();
			items.forEach(function(item) {
				var productId = item.productId;
				var optionId = item.optionId != null ? String(item.optionId) : '';

				var exist = cart.items.find(function(it) {
					var itOptId = it.optionId != null ? String(it.optionId) : '';
					return String(it.productId) === String(productId) &&
						itOptId === optionId;
				});

				if (exist) {
					exist.quantity = safeParseInt(exist.quantity) + safeParseInt(item.quantity);
				} else {
					item.cartItemId = 'P' + productId + '-O' + (optionId || 'NONE') + '-' + Date.now();
					cart.items.push(item);
				}
			});
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

		// 헤더/모바일 장바구니 카운트 업데이트
		updateHeaderCount: function() {
			var count = this.getTotalQuantity();
			// 모바일 퀵 메뉴
			$('.ibio-index-m-qnum').each(function() {
				$(this).text(count + '건');
			});
			$('.front-header-cart-count').text(count);
			// 필요하다면 PC 헤더에도 별도 span 만들어서 여기서 같이 업데이트 가능
		},

		// =========================
		// DOM → CartItem 변환
		// =========================

		/**
		 * 옵션 행 하나를 Cart Item 으로 변환
		 * @param $row 옵션 tr
		 * @param context {productId, productName, productImageUrl}
		 */
		buildItemFromOptionRow: function($row, context) {
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

			// 단위는 4번째 칸(td index 3) 기준으로
			var unit = '-';
			var $tds = $row.find('td');
			if ($tds.length >= 4) {
				var unitText = $.trim($($tds[3]).text());
				if (unitText) unit = unitText;
			}

			var item = {
				productId: context.productId,
				productName: context.productName || '',
				productImageUrl: context.productImageUrl || '',

				optionGroupId: optionGroupId != null ? Number(optionGroupId) : null,
				optionGroupName: optionGroupName || '',
				optionId: optionId != null ? Number(optionId) : null,
				optionName: optionName || '',
				optionCode: optionCode || '',
				unit: unit,

				unitPrice: unitPrice,
				quantity: qty,
				linePrice: unitPrice * qty,
				createdAt: Date.now()
			};

			return item;
		},

		/**
		 * 옵션 패널(리스트/상세 공통 구조)에서 체크된 옵션들을 모두 장바구니 아이템 배열로 변환
		 * @param $panel 옵션 패널 .product-list-option-panel
		 * @param context {productId, productName, productImageUrl}
		 */
		buildItemsFromOptionPanel: function($panel, context) {
			var self = this;
			var items = [];
			$panel.find('tbody tr').each(function() {
				var $row = $(this);
				if (!$row.find('.product-list-row-check').length) return; // 그룹헤더/문구행 제외

				var item = self.buildItemFromOptionRow($row, context);
				if (item) {
					items.push(item);
				}
			});
			return items;
		},

		// =========================
		// 이벤트 바인딩
		// =========================
		bindEvents: function() {
			var self = this;

			// 1) 리스트 페이지 / 상세페이지 공통: 옵션 패널 안의 "장바구니담기(바로구매 버튼을 활용)" 클릭
			$(document).on('click', '.product-list-option-panel .product-list-btn-buy', function(e) {
				e.preventDefault();

				var $btn = $(this);
				var $panel = $btn.closest('.product-list-option-panel');

				// 어떤 상품의 옵션인지 찾기 (리스트/상세 공통)
				var productId = null;
				var productName = '';
				var productImageUrl = '';

				// (A) 리스트 페이지: .product-layout 기준
				var $layout = $btn.closest('.product-layout');
				if ($layout.length) {
					var $listContainer = $layout.find('.product-item-container.list-container');
					if ($listContainer.length) {
						productId = $listContainer.data('product-id') || null;
						productName = $.trim($listContainer.find('h4 a').first().text()) || '';
						var $img = $listContainer.find('img').first();
						if ($img.length) {
							productImageUrl = $img.attr('src') || '';
						}
					}
				}

				// (B) 상세 페이지: #product / 타이틀 영역 기준
				if (!productId) {
					var $prodBox = $('#product');
					if ($prodBox.length) {
						productId = $prodBox.data('product-id') || null;
						productName = $.trim($('.title-product h1').first().text()) || '';
						var $img2 = $('.content-product-left .large-image img').first();
						if ($img2.length) {
							productImageUrl = $img2.attr('src') || '';
						}
					}
				}

				if (!productId) {
					alert('상품 정보를 찾을 수 없습니다.');
					return;
				}

				var context = {
					productId: Number(productId),
					productName: productName,
					productImageUrl: productImageUrl
				};

				var items = self.buildItemsFromOptionPanel($panel, context);
				if (!items.length) {
					alert('선택된 옵션이 없습니다.\n옵션을 선택 후 장바구니에 담아 주세요.');
					return;
				}

				self.addItems(items);
				self.updateHeaderCount();

				alert(items.length + '개의 옵션이 장바구니에 담겼습니다.');
			});

			// 2) 상세페이지 상단 "장바구니담기" 버튼 (#button-cart)
			$(document).on('click', '#button-cart', function(e) {
				e.preventDefault();

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

				// 이 상품에 옵션이 있는지 확인
				var hasOptions = $('#tab-option .product-list-option-table .product-list-row-check').length > 0;
				if (hasOptions) {
					alert('옵션이 있는 상품입니다.\n옵션 탭에서 옵션을 선택한 후 장바구니에 담아 주세요.');
					return;
				}

				// 옵션이 없는 상품 → 수량만큼 기본 상품을 장바구니에 추가
				var qtyInput = $prodBox.find('input[name="quantity"]');
				var qty = safeParseInt(qtyInput.val());
				if (qty < 1) qty = 1;

				var productName = $.trim($('.title-product h1').first().text()) || '';
				var $img = $('.content-product-left .large-image img').first();
				var productImageUrl = $img.length ? ($img.attr('src') || '') : '';

				var unitPrice = safeParseInt($prodBox.data('product-sale-price'));

				var item = {
					productId: Number(productId),
					productName: productName,
					productImageUrl: productImageUrl,

					optionGroupId: null,
					optionGroupName: '',
					optionId: null,
					optionName: '',
					optionCode: '',
					unit: '-',

					unitPrice: unitPrice,
					quantity: qty,
					linePrice: unitPrice * qty,
					createdAt: Date.now()
				};

				self.addItem(item);
				self.updateHeaderCount();

				alert('장바구니에 ' + qty + '개가 담겼습니다.');
			});
		},

		init: function() {
			this.bindEvents();
			this.updateHeaderCount();
		}
	};

	// 전역으로 노출 (필요시 다른 JS에서도 사용 가능)
	window.IbioCart = IbioCart;

	$(function() {
		IbioCart.init();
	});

})(window, jQuery);
