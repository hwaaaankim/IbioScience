/* global jQuery */

(function(window, $) {
	'use strict';

	var STORAGE_KEY_BASE = 'ibio_cart_v1';

	var PRODUCT_TYPE = {
		COMPANY: 'COMPANY',
		DEALER: 'DEALER'
	};

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

	function normalizeProductType(v) {
		var s = safeString(v).toUpperCase();
		return s === PRODUCT_TYPE.DEALER ? PRODUCT_TYPE.DEALER : PRODUCT_TYPE.COMPANY;
	}

	function createEmptyCart(memberId) {
		return {
			userId: memberId || null,
			itemsByType: {
				COMPANY: [],
				DEALER: []
			},
			items: [],
			totalQuantity: 0,
			totalPrice: 0,
			totalQuantityByType: {
				COMPANY: 0,
				DEALER: 0
			},
			totalPriceByType: {
				COMPANY: 0,
				DEALER: 0
			}
		};
	}

	function readProductTypeFromElement($el) {
		if (!$el || !$el.length) return PRODUCT_TYPE.COMPANY;

		var raw = $el.attr('data-product-type');
		if (raw === undefined || raw === null || raw === '') {
			raw = $el.data('product-type');
		}

		return normalizeProductType(raw);
	}

	// =========================
	// 로그인 유저 정보 (frontScript에서 주입)
	// =========================
	function isAuthenticated() {
		return !!(window.__isAuthenticated === true);
	}

	function getCurrentMemberId() {
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

	function ensureCartShape(cart, memberId) {
		if (!cart || typeof cart !== 'object') {
			cart = createEmptyCart(memberId);
		}

		if (!cart.itemsByType || typeof cart.itemsByType !== 'object') {
			cart.itemsByType = {
				COMPANY: [],
				DEALER: []
			};
		}

		if (!Array.isArray(cart.itemsByType.COMPANY)) {
			cart.itemsByType.COMPANY = [];
		}
		if (!Array.isArray(cart.itemsByType.DEALER)) {
			cart.itemsByType.DEALER = [];
		}

		var hasBucketData =
			cart.itemsByType.COMPANY.length > 0 ||
			cart.itemsByType.DEALER.length > 0;

		if (!hasBucketData && Array.isArray(cart.items) && cart.items.length > 0) {
			cart.items.forEach(function(entry) {
				var type = normalizeProductType(entry && entry.productType);
				entry.productType = type;

				if (type === PRODUCT_TYPE.DEALER) {
					cart.itemsByType.DEALER.push(entry);
				} else {
					cart.itemsByType.COMPANY.push(entry);
				}
			});
		}

		cart.items = [];
		cart.userId = memberId || cart.userId || null;

		if (!cart.totalQuantityByType || typeof cart.totalQuantityByType !== 'object') {
			cart.totalQuantityByType = {
				COMPANY: 0,
				DEALER: 0
			};
		}

		if (!cart.totalPriceByType || typeof cart.totalPriceByType !== 'object') {
			cart.totalPriceByType = {
				COMPANY: 0,
				DEALER: 0
			};
		}

		return cart;
	}

	// =========================
	// 장바구니 Core
	// =========================
	var IbioCart = {

		loadCart: function() {
			var memberId = getCurrentMemberId();
			if (!memberId) {
				return createEmptyCart(null);
			}

			if (!window.localStorage) {
				return createEmptyCart(memberId);
			}

			var key = getStorageKeyForUser(memberId);
			var raw = localStorage.getItem(key);

			if (!raw) {
				return createEmptyCart(memberId);
			}

			try {
				var cart = JSON.parse(raw);
				cart = ensureCartShape(cart, memberId);
				this.recalcTotals(cart);
				return cart;
			} catch (e) {
				console.error('[IbioCart] JSON parse error', e);
				return createEmptyCart(memberId);
			}
		},

		saveCart: function(cart) {
			var memberId = getCurrentMemberId();
			if (!memberId) return;
			if (!window.localStorage) return;

			cart = ensureCartShape(cart, memberId);
			cart.userId = memberId;

			this.recalcTotals(cart);

			var key = getStorageKeyForUser(memberId);
			localStorage.setItem(key, JSON.stringify(cart));
		},

		recalcTotals: function(cart) {
			cart = ensureCartShape(cart, cart ? cart.userId : null);

			var totalEntryCount = 0;
			var totalPrice = 0;

			var totalQuantityByType = {
				COMPANY: 0,
				DEALER: 0
			};

			var totalPriceByType = {
				COMPANY: 0,
				DEALER: 0
			};

			var flattened = [];

			[PRODUCT_TYPE.COMPANY, PRODUCT_TYPE.DEALER].forEach(function(bucketType) {
				var list = cart.itemsByType[bucketType] || [];

				list.forEach(function(entry) {
					if (!Array.isArray(entry.options)) entry.options = [];

					var entryType = normalizeProductType(entry.productType || bucketType);
					entry.productType = entryType;

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

					totalEntryCount += 1;
					totalPrice += entryPrice;

					totalQuantityByType[entryType] += 1;
					totalPriceByType[entryType] += entryPrice;

					flattened.push(entry);
				});
			});

			cart.items = flattened;
			cart.totalQuantity = totalEntryCount;
			cart.totalPrice = totalPrice;
			cart.totalQuantityByType = totalQuantityByType;
			cart.totalPriceByType = totalPriceByType;
		},

		getCart: function() {
			return this.loadCart();
		},

		addEntry: function(entry) {
			var memberId = getCurrentMemberId();
			if (!memberId) {
				alert('로그인이 필요합니다.');
				return this.getCart();
			}

			var cart = this.getCart();

			var productType = normalizeProductType(entry.productType);

			entry.userId = memberId;
			entry.productType = productType;
			entry.cartEntryId = 'E-' + productType + '-' + memberId + '-' + nowTs() + '-' + Math.random().toString(16).slice(2);
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

			if (!cart.itemsByType || !cart.itemsByType[productType]) {
				cart = ensureCartShape(cart, memberId);
			}

			cart.itemsByType[productType].push(entry);

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

		resolveProductInfo: function($trigger) {
			var info = {
				productId: null,
				productName: '',
				productImageUrl: '',
				productType: PRODUCT_TYPE.COMPANY
			};

			var $layout = $trigger.closest('.product-layout');
			if ($layout.length) {
				var $listContainer = $layout.find('.product-item-container.list-container').first();
				if ($listContainer.length) {
					info.productId = $listContainer.data('product-id') || null;
					info.productName = $.trim($listContainer.find('h4 a').first().text()) || '';
					info.productType = readProductTypeFromElement($listContainer);

					var $img = $listContainer.find('img').first();
					if ($img.length) info.productImageUrl = $img.attr('src') || '';

					return info;
				}
			}

			var $prodBox = $('#product');
			if ($prodBox.length) {
				info.productId = $prodBox.data('product-id') || null;
				info.productName = $.trim($('.title-product h1').first().text()) || '';
				info.productType = readProductTypeFromElement($prodBox);

				var $img2 = $('.content-product-left .large-image img').first();
				if ($img2.length) info.productImageUrl = $img2.attr('src') || '';
			}

			return info;
		},

		resolveListContainer: function($trigger) {
			var $layout = $trigger.closest('.product-layout');
			if ($layout.length) {
				return $layout.find('.product-item-container.list-container').first();
			}
			return $();
		},

		bindEvents: function() {
			var self = this;

			$(document).on('click', '.product-list-option-panel .product-list-btn-buy', function(e) {
				e.preventDefault();

				if (!getCurrentMemberId()) {
					alert('로그인이 필요합니다.');
					return;
				}

				var $btn = $(this);
				var $panel = $btn.closest('.product-list-option-panel');

				var info = self.resolveProductInfo($btn);

				if (!info.productId) {
					alert('상품 정보를 찾을 수 없습니다.');
					return;
				}

				var options = self.buildOptionsFromPanel($panel);
				if (!options.length) {
					alert('선택된 옵션이 없습니다.\n옵션을 선택 후 장바구니에 담아 주세요.');
					return;
				}

				self.addEntry({
					productType: info.productType,
					productId: Number(info.productId),
					productName: info.productName,
					productImageUrl: info.productImageUrl,
					options: options
				});

				self.updateHeaderCount();
				alert('장바구니에 담겼습니다.');
			});

			$(document).on('click', '.product-list-cart-btn', function(e) {
				e.preventDefault();

				if (!getCurrentMemberId()) {
					alert('로그인이 필요합니다.');
					return;
				}

				var $btn = $(this);
				var $layout = $btn.closest('.product-layout');
				var $listContainer = self.resolveListContainer($btn);

				if (!$layout.length || !$listContainer.length) {
					alert('상품 정보를 찾을 수 없습니다.');
					return;
				}

				var hasOptions = $layout.find('.product-list-option-panel .product-list-row-check').length > 0;
				if (hasOptions) {
					alert('옵션이 있는 상품입니다.\n옵션을 선택 후 장바구니에 담아 주세요.');
					return;
				}

				var info = self.resolveProductInfo($btn);
				if (!info.productId) {
					alert('상품 정보를 찾을 수 없습니다.');
					return;
				}

				var unitPrice = safeParseInt($listContainer.data('product-sale-price'));
				if (unitPrice <= 0) {
					alert('판매가격 정보가 없습니다.');
					return;
				}

				self.addEntry({
					productType: info.productType,
					productId: Number(info.productId),
					productName: info.productName,
					productImageUrl: info.productImageUrl,
					options: [{
						optionGroupId: null,
						optionGroupName: '',
						optionId: null,
						optionName: '',
						optionCode: '',
						unit: '-',
						unitPrice: unitPrice,
						quantity: 1,
						linePrice: unitPrice
					}]
				});

				self.updateHeaderCount();
				alert('장바구니에 담겼습니다.');
			});

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
				var productType = readProductTypeFromElement($prodBox);

				var unitPrice = safeParseInt($prodBox.data('product-sale-price'));

				self.addEntry({
					productType: productType,
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

	IbioCart.PRODUCT_TYPE = PRODUCT_TYPE;
	IbioCart.normalizeProductType = normalizeProductType;

	window.IbioCart = IbioCart;

	$(function() {
		IbioCart.init();
	});

})(window, jQuery);