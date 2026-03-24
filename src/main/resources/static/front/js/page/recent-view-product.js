/* eslint-disable */
(function(window, document) {
	"use strict";

	var STORAGE_KEY = 'recentProducts.v2';
	var LEGACY_STORAGE_KEY = 'recentProducts.v1';
	var MAX_ITEMS = 10;
	var GROUP_SIZE = 5;
	var STYLE_ID = 'ibio-recent-view-style';

	function nowMs() {
		return Date.now ? Date.now() : new Date().getTime();
	}

	function todayYMD() {
		var d = new Date();
		var month = (d.getMonth() + 1).toString();
		var day = d.getDate().toString();

		if (month.length < 2) month = '0' + month;
		if (day.length < 2) day = '0' + day;

		return d.getFullYear() + '-' + month + '-' + day;
	}

	function safeString(v) {
		return v == null ? '' : String(v);
	}

	function escapeHtml(str) {
		return safeString(str)
			.replace(/&/g, '&amp;')
			.replace(/</g, '&lt;')
			.replace(/>/g, '&gt;')
			.replace(/"/g, '&quot;')
			.replace(/'/g, '&#39;');
	}

	function escapeAttr(str) {
		return escapeHtml(str);
	}

	function toInt(n) {
		if (n == null || n === '') return null;
		var v = parseInt(n, 10);
		return isNaN(v) ? null : v;
	}

	function toFloat(n) {
		if (n == null || n === '') return 0;
		var v = parseFloat(n);
		return isNaN(v) ? 0 : v;
	}

	function clampRating(r) {
		r = toFloat(r);
		if (r < 0) return 0;
		if (r > 5) return 5;
		return r;
	}

	function normalizeSourceType(v) {
		var sourceType = safeString(v).trim().toUpperCase();
		return sourceType === 'DEALER' ? 'DEALER' : 'COMPANY';
	}

	function defaultSourceLabel(sourceType) {
		return sourceType === 'DEALER' ? '딜러제품' : '우리회사제품';
	}

	function formatPrice(n) {
		var v = toInt(n);
		if (v == null) return '';
		return v.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",") + '원';
	}

	function mkStarHtml(rating) {
		var rounded = Math.round(clampRating(rating));
		var html = '';

		for (var i = 1; i <= 5; i++) {
			if (i <= rounded) {
				html += '<span class="fa fa-stack"><i class="fa fa-star fa-stack-2x"></i></span>';
			} else {
				html += '<span class="fa fa-stack"><i class="fa fa-star-o fa-stack-2x"></i></span>';
			}
		}

		return html;
	}

	function ensureStyle() {
		if (document.getElementById(STYLE_ID)) return;

		var style = document.createElement('style');
		style.id = STYLE_ID;
		style.type = 'text/css';
		style.textContent =
			'#recentViewModule .ibio-rvp-title-wrap {' +
			'	display:flex;' +
			'	align-items:center;' +
			'	gap:6px;' +
			'	flex-wrap:wrap;' +
			'}' +
			'#recentViewModule .ibio-rvp-title-link {' +
			'	display:inline-block;' +
			'	max-width:100%;' +
			'}' +
			'#recentViewModule .ibio-rvp-source-badge {' +
			'	display:inline-block;' +
			'	padding:2px 6px;' +
			'	border-radius:999px;' +
			'	font-size:11px;' +
			'	line-height:1.2;' +
			'	font-weight:700;' +
			'	vertical-align:middle;' +
			'}' +
			'#recentViewModule .ibio-rvp-source-company {' +
			'	background:#eef2ff;' +
			'	color:#3742fa;' +
			'	border:1px solid #c7d2fe;' +
			'}' +
			'#recentViewModule .ibio-rvp-source-dealer {' +
			'	background:#ecfdf5;' +
			'	color:#047857;' +
			'	border:1px solid #a7f3d0;' +
			'}' +
			'#recentViewModule .ibio-rvp-simple-nav {' +
			'	display:flex;' +
			'	align-items:center;' +
			'	justify-content:center;' +
			'	gap:8px;' +
			'	margin-top:10px;' +
			'}' +
			'#recentViewModule .ibio-rvp-simple-nav button {' +
			'	border:1px solid #ddd;' +
			'	background:#fff;' +
			'	padding:4px 10px;' +
			'	border-radius:4px;' +
			'	cursor:pointer;' +
			'}' +
			'#recentViewModule .ibio-rvp-simple-nav button:disabled {' +
			'	opacity:0.5;' +
			'	cursor:default;' +
			'}';

		document.head.appendChild(style);
	}

	function readStorage(key) {
		try {
			if (!window.localStorage) return [];
			var raw = localStorage.getItem(key);
			if (!raw) return [];
			var arr = JSON.parse(raw);
			return Array.isArray(arr) ? arr : [];
		} catch (e) {
			console.warn('RecentView readStorage error', e);
			return [];
		}
	}

	function save(list) {
		try {
			if (!window.localStorage) return;
			localStorage.setItem(STORAGE_KEY, JSON.stringify(list || []));
		} catch (e) {
			console.warn('RecentView save error', e);
		}
	}

	function buildUniqueKey(item) {
		var productKey = safeString(item.productKey || item.key).trim();
		if (productKey !== '') return productKey;

		var sourceType = normalizeSourceType(item.sourceType);
		var id = safeString(item.id).trim();
		if (id === '') return '';

		return sourceType + ':' + id;
	}

	function normalizeStoredItem(item) {
		if (!item) return null;

		var id = item.id;
		if (id == null || safeString(id).trim() === '') return null;

		var sourceType = normalizeSourceType(item.sourceType);
		var sourceLabel = safeString(item.sourceLabel).trim();
		if (sourceLabel === '') {
			sourceLabel = defaultSourceLabel(sourceType);
		}

		var productKey = safeString(item.productKey || item.key).trim();
		var uniqueKey = buildUniqueKey({
			id: id,
			productKey: productKey,
			sourceType: sourceType
		});

		if (uniqueKey === '') return null;

		var priceNew = toInt(item.priceNew);
		var priceOld = toInt(item.priceOld);
		if (priceNew != null && priceOld == null) {
			priceOld = priceNew;
		}

		return {
			uniqueKey: uniqueKey,
			productKey: productKey !== '' ? productKey : uniqueKey,
			id: id,
			name: safeString(item.name).trim(),
			image: safeString(item.image).trim(),
			detailUrl: safeString(item.detailUrl).trim(),
			sourceType: sourceType,
			sourceLabel: sourceLabel,
			priceNew: priceNew,
			priceOld: priceOld,
			rating: clampRating(item.rating),
			review: toInt(item.review) || 0,
			viewedDate: safeString(item.viewedDate).trim() || todayYMD(),
			viewedAt: toInt(item.viewedAt) || 0
		};
	}

	function dedupeAndSort(list) {
		var map = {};
		var out = [];

		for (var i = 0; i < list.length; i++) {
			var normalized = normalizeStoredItem(list[i]);
			if (!normalized) continue;

			var key = normalized.uniqueKey;
			if (!map[key]) {
				map[key] = normalized;
			} else {
				if ((normalized.viewedAt || 0) > (map[key].viewedAt || 0)) {
					map[key] = normalized;
				}
			}
		}

		for (var k in map) {
			if (Object.prototype.hasOwnProperty.call(map, k)) {
				out.push(map[k]);
			}
		}

		out.sort(function(a, b) {
			return (b.viewedAt || 0) - (a.viewedAt || 0);
		});

		if (out.length > MAX_ITEMS) {
			out = out.slice(0, MAX_ITEMS);
		}

		return out;
	}

	function load() {
		var list = readStorage(STORAGE_KEY);

		if (list.length > 0) {
			list = dedupeAndSort(list);
			save(list);
			return list;
		}

		var legacy = readStorage(LEGACY_STORAGE_KEY);
		if (legacy.length > 0) {
			var migrated = dedupeAndSort(legacy);
			save(migrated);

			try {
				localStorage.removeItem(LEGACY_STORAGE_KEY);
			} catch (e) {
				console.warn('RecentView legacy remove error', e);
			}

			return migrated;
		}

		return [];
	}

	function pickString(newValue, oldValue) {
		var nv = safeString(newValue).trim();
		if (nv !== '') return nv;
		return safeString(oldValue).trim();
	}

	function pickNumber(newValue, oldValue) {
		return newValue != null ? newValue : oldValue;
	}

	function upsertToFront(list, entry) {
		var uniqueKey = entry.uniqueKey;
		if (!uniqueKey) return list;

		var foundIndex = -1;
		for (var i = 0; i < list.length; i++) {
			if (safeString(list[i].uniqueKey) === safeString(uniqueKey)) {
				foundIndex = i;
				break;
			}
		}

		if (foundIndex >= 0) {
			var exist = list[foundIndex] || {};

			exist.uniqueKey = uniqueKey;
			exist.productKey = pickString(entry.productKey, exist.productKey);
			exist.id = pickString(entry.id, exist.id);
			exist.name = pickString(entry.name, exist.name);
			exist.image = pickString(entry.image, exist.image);
			exist.detailUrl = pickString(entry.detailUrl, exist.detailUrl);
			exist.sourceType = pickString(entry.sourceType, exist.sourceType) || 'COMPANY';
			exist.sourceLabel = pickString(entry.sourceLabel, exist.sourceLabel) || defaultSourceLabel(exist.sourceType);
			exist.priceNew = pickNumber(entry.priceNew, exist.priceNew);
			exist.priceOld = pickNumber(entry.priceOld, exist.priceOld);
			exist.rating = pickNumber(entry.rating, exist.rating);
			exist.review = pickNumber(entry.review, exist.review);
			exist.viewedAt = entry.viewedAt;
			exist.viewedDate = entry.viewedDate;

			list.splice(foundIndex, 1);
			list.unshift(exist);
		} else {
			list.unshift(entry);
		}

		if (list.length > MAX_ITEMS) {
			list = list.slice(0, MAX_ITEMS);
		}

		return list;
	}

	function buildHref(p, productDetailBase) {
		var detailUrl = safeString(p.detailUrl).trim();
		if (detailUrl !== '') return detailUrl;

		var base = safeString(productDetailBase).trim() || '/productDetail';
		if (p.id == null || safeString(p.id).trim() === '') return '#';

		return base + '/' + encodeURIComponent(p.id);
	}

	function buildSourceBadgeHtml(p) {
		var sourceType = normalizeSourceType(p.sourceType);
		var sourceLabel = safeString(p.sourceLabel).trim() || defaultSourceLabel(sourceType);
		var sourceClass = sourceType === 'DEALER'
			? 'ibio-rvp-source-badge ibio-rvp-source-dealer'
			: 'ibio-rvp-source-badge ibio-rvp-source-company';

		return '<span class="' + sourceClass + '">' + escapeHtml(sourceLabel) + '</span>';
	}

	function buildCardHTML(p, productDetailBase) {
		var href = buildHref(p, productDetailBase);
		var nameHtml = escapeHtml(p.name || '');
		var nameAttr = escapeAttr(p.name || '');
		var image = escapeAttr(p.image || '/front/image/sample/80-80.png');
		var sourceType = normalizeSourceType(p.sourceType);
		var sourceLabel = safeString(p.sourceLabel).trim() || defaultSourceLabel(sourceType);
		var productKey = safeString(p.productKey).trim() || safeString(p.uniqueKey).trim();

		return '' +
			'<div class="product-layout item-inner style1">' +
			'  <div class="item-image">' +
			'    <div class="item-img-info">' +
			'      <a href="' + escapeAttr(href) + '" title="' + nameAttr + '"' +
			'         class="rvp-link"' +
			'         data-rvp-id="' + escapeAttr(p.id) + '"' +
			'         data-rvp-key="' + escapeAttr(productKey) + '"' +
			'         data-rvp-product-key="' + escapeAttr(productKey) + '"' +
			'         data-rvp-source-type="' + escapeAttr(sourceType) + '"' +
			'         data-rvp-source-label="' + escapeAttr(sourceLabel) + '"' +
			'         data-rvp-detail-url="' + escapeAttr(href) + '"' +
			'         data-rvp-name="' + nameAttr + '"' +
			'         data-rvp-image="' + image + '"' +
			'         data-rvp-pricenew="' + (p.priceNew != null ? escapeAttr(p.priceNew) : '') + '"' +
			'         data-rvp-priceold="' + (p.priceOld != null ? escapeAttr(p.priceOld) : '') + '"' +
			'         data-rvp-rating="' + (p.rating != null ? escapeAttr(p.rating) : '0') + '"' +
			'         data-rvp-review="' + (p.review != null ? escapeAttr(p.review) : '0') + '">' +
			'        <img src="' + image + '" alt="' + nameAttr + '">' +
			'      </a>' +
			'    </div>' +
			'  </div>' +
			'  <div class="item-info">' +
			'    <div class="item-title">' +
			'      <div class="ibio-rvp-title-wrap">' +
			'        <a href="' + escapeAttr(href) + '" target="_self" title="' + nameAttr + '"' +
			'           class="rvp-link ibio-rvp-title-link"' +
			'           data-rvp-id="' + escapeAttr(p.id) + '"' +
			'           data-rvp-key="' + escapeAttr(productKey) + '"' +
			'           data-rvp-product-key="' + escapeAttr(productKey) + '"' +
			'           data-rvp-source-type="' + escapeAttr(sourceType) + '"' +
			'           data-rvp-source-label="' + escapeAttr(sourceLabel) + '"' +
			'           data-rvp-detail-url="' + escapeAttr(href) + '"' +
			'           data-rvp-name="' + nameAttr + '"' +
			'           data-rvp-image="' + image + '"' +
			'           data-rvp-pricenew="' + (p.priceNew != null ? escapeAttr(p.priceNew) : '') + '"' +
			'           data-rvp-priceold="' + (p.priceOld != null ? escapeAttr(p.priceOld) : '') + '"' +
			'           data-rvp-rating="' + (p.rating != null ? escapeAttr(p.rating) : '0') + '"' +
			'           data-rvp-review="' + (p.review != null ? escapeAttr(p.review) : '0') + '">' +
			'          ' + nameHtml +
			'        </a>' +
			'        ' + buildSourceBadgeHtml(p) +
			'      </div>' +
			'    </div>' +
			'    <div class="rating">' + mkStarHtml(p.rating) + '</div>' +
			'    <div class="content_price price">' +
			'      <p class="price-new product-price">' + formatPrice(p.priceNew) + '</p>' +
			'      <p class="price-old">' + formatPrice(p.priceOld != null ? p.priceOld : p.priceNew) + '</p>' +
			'    </div>' +
			'  </div>' +
			'</div>';
	}

	function buildSlideHTML(group, productDetailBase) {
		var html = '<div class="item">';
		for (var i = 0; i < group.length; i++) {
			html += buildCardHTML(group[i], productDetailBase);
		}
		html += '</div>';
		return html;
	}

	function chunk(arr, size) {
		var out = [];
		for (var i = 0; i < arr.length; i += size) {
			out.push(arr.slice(i, i + size));
		}
		return out;
	}

	function parseProductIdFromPath() {
		var path = (window.location && window.location.pathname) ? window.location.pathname : '';
		var m = path.match(/\/(\d+)(\/)?$/i);
		if (m && m[1]) return m[1];
		return null;
	}

	function getProductDetailBase() {
		var b = document.body;
		if (!b) return '/productDetail';
		return b.getAttribute('data-product-detail-base') || '/productDetail';
	}

	function bindClickSave(root) {
		var scope = root || document;
		var links = scope.querySelectorAll('.rvp-link');

		if (!links || links.length === 0) return;

		Array.prototype.forEach.call(links, function(a) {
			if (a.__rvpBound) return;
			a.__rvpBound = true;

			a.addEventListener('click', function() {
				if (!window.RecentView) return;
				window.RecentView.addFromDataset(a.dataset);
			});
		});
	}

	function autoSaveOnDetailPage() {
		var id = parseProductIdFromPath();
		if (!id) return;

		var autoEl = document.getElementById('rvpAutoData');
		if (autoEl && autoEl.dataset) {
			window.RecentView.addFromDataset(autoEl.dataset);
			return;
		}

		var any = document.querySelector('[data-rvp-id]');
		if (any && any.dataset) {
			window.RecentView.addFromDataset(any.dataset);
			return;
		}

		window.RecentView.add({
			id: id,
			detailUrl: window.location.pathname + (window.location.search || '')
		});
	}

	function removeSimplePager(container) {
		if (!container) return;

		var host = container.parentNode;
		if (!host) return;

		var oldPager = host.querySelector('.ibio-rvp-simple-nav');
		if (oldPager) {
			oldPager.parentNode.removeChild(oldPager);
		}

		var items = container.children;
		for (var i = 0; i < items.length; i++) {
			items[i].style.display = '';
		}
	}

	function initSimplePager(container, pageCount) {
		removeSimplePager(container);

		if (!container || pageCount <= 1) return;

		var host = container.parentNode;
		if (!host) return;

		var items = container.children;
		if (!items || items.length === 0) return;

		var current = 0;

		var nav = document.createElement('div');
		nav.className = 'ibio-rvp-simple-nav';

		var prevBtn = document.createElement('button');
		prevBtn.type = 'button';
		prevBtn.textContent = '이전';

		var counter = document.createElement('span');
		counter.className = 'ibio-rvp-simple-counter';

		var nextBtn = document.createElement('button');
		nextBtn.type = 'button';
		nextBtn.textContent = '다음';

		function renderPage() {
			for (var i = 0; i < items.length; i++) {
				items[i].style.display = (i === current) ? '' : 'none';
			}
			counter.textContent = (current + 1) + ' / ' + pageCount;
			prevBtn.disabled = current <= 0;
			nextBtn.disabled = current >= pageCount - 1;
		}

		prevBtn.addEventListener('click', function() {
			if (current <= 0) return;
			current -= 1;
			renderPage();
		});

		nextBtn.addEventListener('click', function() {
			if (current >= pageCount - 1) return;
			current += 1;
			renderPage();
		});

		nav.appendChild(prevBtn);
		nav.appendChild(counter);
		nav.appendChild(nextBtn);
		host.appendChild(nav);

		renderPage();
	}

	function initOwlSlider(container, pageCount) {
		if (!container || pageCount <= 1) return false;

		var $ = window.jQuery || window.$;
		if (!$ || !$.fn || typeof $.fn.owlCarousel !== 'function') {
			return false;
		}

		var $container = $(container);

		try {
			$container.trigger('destroy.owl.carousel');
		} catch (e) {
			// ignore
		}

		container.classList.remove('owl-loaded', 'owl-hidden');
		container.removeAttribute('style');

		var delay = toFloat(container.getAttribute('data-delay'));
		if (!delay || delay <= 0) delay = 4;

		var speed = toFloat(container.getAttribute('data-speed'));
		if (!speed || speed <= 0) speed = 0.6;

		var margin = toInt(container.getAttribute('data-margin'));
		if (margin == null) margin = 0;

		var autoplay = safeString(container.getAttribute('data-autoplay')).toLowerCase() === 'yes';
		var rtl = safeString(container.getAttribute('data-rtl')).toLowerCase() === 'yes';
		var nav = safeString(container.getAttribute('data-arrows')).toLowerCase() === 'yes';
		var dots = safeString(container.getAttribute('data-pagination')).toLowerCase() === 'yes';
		var loop = safeString(container.getAttribute('data-loop')).toLowerCase() === 'yes';
		var lazyLoad = safeString(container.getAttribute('data-lazyload')).toLowerCase() === 'yes';
		var hoverPause = safeString(container.getAttribute('data-hoverpause')).toLowerCase() === 'yes';

		try {
			$container.owlCarousel({
				items: 1,
				rtl: rtl,
				autoplay: autoplay,
				autoplayTimeout: Math.round(delay * 1000),
				autoplayHoverPause: hoverPause,
				smartSpeed: Math.round(speed * 1000),
				margin: margin,
				nav: nav,
				dots: dots,
				loop: loop && pageCount > 1,
				lazyLoad: lazyLoad,
				mouseDrag: pageCount > 1,
				touchDrag: pageCount > 1,
				pullDrag: pageCount > 1,
				rewind: !loop,
				navText: ['<i class="fa fa-angle-left"></i>', '<i class="fa fa-angle-right"></i>'],
				responsive: {
					0: { items: 1 },
					480: { items: 1 },
					768: { items: 1 },
					992: { items: 1 },
					1200: { items: 1 }
				}
			});

			return true;
		} catch (e2) {
			console.warn('RecentView owl init error', e2);
			return false;
		}
	}

	var RecentView = {
		add: function(item) {
			if (!item || item.id == null || safeString(item.id).trim() === '') return;

			var sourceType = normalizeSourceType(item.sourceType);
			var sourceLabel = safeString(item.sourceLabel).trim() || defaultSourceLabel(sourceType);
			var priceNew = item.priceNew != null ? toInt(item.priceNew) : null;
			var priceOld = item.priceOld != null && item.priceOld !== '' ? toInt(item.priceOld) : null;

			if (priceNew != null && priceOld == null) {
				priceOld = priceNew;
			}

			var productKey = safeString(item.productKey || item.key).trim();
			var uniqueKey = buildUniqueKey({
				id: item.id,
				productKey: productKey,
				sourceType: sourceType
			});

			if (!uniqueKey) return;

			var entry = {
				uniqueKey: uniqueKey,
				productKey: productKey !== '' ? productKey : uniqueKey,
				id: item.id,
				name: safeString(item.name).trim(),
				image: safeString(item.image).trim(),
				detailUrl: safeString(item.detailUrl).trim(),
				sourceType: sourceType,
				sourceLabel: sourceLabel,
				priceNew: priceNew,
				priceOld: priceOld,
				rating: clampRating(item.rating != null ? item.rating : 0),
				review: toInt(item.review) || 0,
				viewedDate: todayYMD(),
				viewedAt: nowMs()
			};

			var list = load();
			list = upsertToFront(list, entry);
			save(list);
		},

		addFromDataset: function(ds) {
			if (!ds) return;

			this.add({
				id: ds.rvpId || ds.rvpid || ds['rvp-id'],
				key: ds.rvpKey || ds.rvpProductKey || ds['rvp-key'] || ds['rvp-product-key'],
				productKey: ds.rvpProductKey || ds.rvpKey || ds['rvp-product-key'] || ds['rvp-key'],
				sourceType: ds.rvpSourceType || ds['rvp-source-type'],
				sourceLabel: ds.rvpSourceLabel || ds['rvp-source-label'],
				detailUrl: ds.rvpDetailUrl || ds['rvp-detail-url'],
				name: ds.rvpName || '',
				image: ds.rvpImage || '',
				priceNew: ds.rvpPricenew || ds.rvpPriceNew,
				priceOld: ds.rvpPriceold || ds.rvpPriceOld,
				rating: ds.rvpRating || 0,
				review: ds.rvpReview || 0
			});
		},

		list: function(max) {
			var arr = load();

			arr = arr.map(function(p) {
				return normalizeStoredItem(p);
			}).filter(function(p) {
				return !!p;
			});

			arr.sort(function(a, b) {
				return (b.viewedAt || 0) - (a.viewedAt || 0);
			});

			if (typeof max === 'number' && max > 0) {
				return arr.slice(0, max);
			}

			return arr;
		},

		render: function(containerSelector, options) {
			options = options || {};

			var productDetailBase = options.productDetailBase || '/productDetail';
			var onEmptyHide = options.onEmptyHide || null;

			var container = document.querySelector(containerSelector);
			if (!container) return;

			ensureStyle();

			var data = this.list(MAX_ITEMS);

			if (!data || data.length === 0) {
				if (onEmptyHide) {
					var wrap = document.querySelector(onEmptyHide);
					if (wrap) wrap.style.display = 'none';
				}

				removeSimplePager(container);
				container.innerHTML = '';
				return;
			}

			if (onEmptyHide) {
				var wrap2 = document.querySelector(onEmptyHide);
				if (wrap2) wrap2.style.display = '';
			}

			var groups = chunk(data, GROUP_SIZE);
			var html = '';

			for (var i = 0; i < groups.length; i++) {
				html += buildSlideHTML(groups[i], productDetailBase);
			}

			removeSimplePager(container);
			container.innerHTML = html;

			bindClickSave(container);

			if (groups.length > 1) {
				if (!initOwlSlider(container, groups.length)) {
					initSimplePager(container, groups.length);
				}
			}
		}
	};

	window.RecentView = RecentView;

	function initGlobalRecentView() {
		bindClickSave(document);
		autoSaveOnDetailPage();

		var module = document.getElementById('recentViewModule');
		if (module) {
			var base = getProductDetailBase();

			RecentView.render('#recentViewModule .extraslider-inner', {
				onEmptyHide: '#recentViewModule',
				productDetailBase: base
			});
		}
	}

	if (document.readyState === 'loading') {
		document.addEventListener('DOMContentLoaded', initGlobalRecentView);
	} else {
		initGlobalRecentView();
	}

})(window, document);