/* eslint-disable */
(function(window, document) {
	"use strict";

	var STORAGE_KEY = 'recentProducts.v1';
	var MAX_ITEMS = 15;
	var GROUP_SIZE = 5;

	function todayYMD() {
		var d = new Date();
		var m = (d.getMonth() + 1).toString().padStart(2, '0');
		var day = d.getDate().toString().padStart(2, '0');
		return d.getFullYear() + '-' + m + '-' + day;
	}

	function load() {
		try {
			var raw = localStorage.getItem(STORAGE_KEY);
			if (!raw) return [];
			var arr = JSON.parse(raw);
			if (!Array.isArray(arr)) return [];
			return arr.filter(function(x) { return x && x.id != null; });
		} catch (e) {
			console.warn('RecentView load error', e);
			return [];
		}
	}

	function save(list) {
		try {
			localStorage.setItem(STORAGE_KEY, JSON.stringify(list || []));
		} catch (e) {
			console.warn('RecentView save error', e);
		}
	}

	function toInt(n) {
		var v = parseInt(n, 10);
		return isNaN(v) ? null : v;
	}

	function toFloat(n) {
		var v = parseFloat(n);
		return isNaN(v) ? 0 : v;
	}

	function clampRating(r) {
		r = toFloat(r);
		if (r < 0) return 0;
		if (r > 5) return 5;
		return r;
	}

	function formatPrice(n) {
		var v = toInt(n);
		if (v == null) return '';
		return v.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",") + '원';
	}

	function uniquePushFront(list, item) {
		var id = item.id;
		if (id == null) return list;
		// 중복 제거 후 맨 앞 삽입
		list = list.filter(function(x) { return String(x.id) !== String(id); });
		list.unshift(item);
		if (list.length > MAX_ITEMS) list = list.slice(0, MAX_ITEMS);
		return list;
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

	function chunk(arr, size) {
		var out = [];
		for (var i = 0; i < arr.length; i += size) out.push(arr.slice(i, i + size));
		return out;
	}

	function buildCardHTML(p, productDetailBase) {
		var href = (productDetailBase || '/product') + '/' + encodeURIComponent(p.id);
		var nameEsc = (p.name || '').replace(/"/g, '&quot;');
		var image = p.image || '/front/image/sample/80-80.png';

		return '' +
			'<div class="product-layout item-inner style1 ">' +
			'  <div class="item-image">' +
			'    <div class="item-img-info">' +
			'      <a href="' + href + '" title="' + nameEsc + '" ' +
			'         class="rvp-link" ' +
			'         data-rvp-id="' + p.id + '" ' +
			'         data-rvp-name="' + nameEsc + '" ' +
			'         data-rvp-image="' + image + '" ' +
			'         data-rvp-pricenew="' + (p.priceNew != null ? p.priceNew : '') + '" ' +
			'         data-rvp-priceold="' + (p.priceOld != null ? p.priceOld : '') + '" ' +
			'         data-rvp-rating="' + (p.rating != null ? p.rating : 0) + '" ' +
			'         data-rvp-review="' + (p.review != null ? p.review : 0) + '">' +
			'        <img src="' + image + '" alt="' + nameEsc + '">' +
			'      </a>' +
			'    </div>' +
			'  </div>' +
			'  <div class="item-info">' +
			'    <div class="item-title">' +
			'      <a href="' + href + '" target="_self" title="' + nameEsc + '" ' +
			'         class="rvp-link" ' +
			'         data-rvp-id="' + p.id + '" ' +
			'         data-rvp-name="' + nameEsc + '" ' +
			'         data-rvp-image="' + image + '" ' +
			'         data-rvp-pricenew="' + (p.priceNew != null ? p.priceNew : '') + '" ' +
			'         data-rvp-priceold="' + (p.priceOld != null ? p.priceOld : '') + '" ' +
			'         data-rvp-rating="' + (p.rating != null ? p.rating : 0) + '" ' +
			'         data-rvp-review="' + (p.review != null ? p.review : 0) + '">' +
			'        ' + nameEsc +
			'      </a>' +
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
		var html = '<div class="item ">';
		for (var i = 0; i < group.length; i++) {
			html += buildCardHTML(group[i], productDetailBase);
		}
		html += '</div>';
		return html;
	}

	var RecentView = {
		add: function(item) {
			if (!item || item.id == null) return;

			var pn = item.priceNew != null ? toInt(item.priceNew) : null;
			var po = item.priceOld != null && item.priceOld !== '' ? toInt(item.priceOld) : null;
			if (pn != null && (po == null)) {
				// 요구사항: 할인 없으면 동일가 저장
				po = pn;
			}

			var entry = {
				id: item.id,
				name: item.name || '',
				image: item.image || '',
				priceNew: pn,
				priceOld: po,
				rating: clampRating(item.rating != null ? item.rating : 0),
				review: toInt(item.review) || 0,
				viewedDate: item.viewedDate || todayYMD()
			};

			var list = load();
			list = uniquePushFront(list, entry);
			save(list);
		},

		addFromDataset: function(ds) {
			if (!ds) return;
			this.add({
				id: ds.rvpId || ds.rvpid || ds['rvp-id'],
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
			if (typeof max === 'number' && max > 0) {
				return arr.slice(0, max);
			}
			return arr;
		},

		render: function(containerSelector, options) {
			options = options || {};
			var productDetailBase = options.productDetailBase || '/product';
			var onEmptyHide = options.onEmptyHide || null;

			var container = document.querySelector(containerSelector);
			if (!container) return;

			var data = this.list(MAX_ITEMS);
			if (!data || data.length === 0) {
				if (onEmptyHide) {
					var wrap = document.querySelector(onEmptyHide);
					if (wrap) wrap.style.display = 'none';
				}
				container.innerHTML = '';
				return;
			}

			if (onEmptyHide) {
				var wrap2 = document.querySelector(onEmptyHide);
				if (wrap2) wrap2.style.display = '';
			}

			// priceOld 보정(혹시 저장 이전 데이터가 null인 경우)
			data = data.map(function(p) {
				if (p && p.priceNew != null && (p.priceOld == null)) {
					p.priceOld = p.priceNew;
				}
				return p;
			});

			var groups = (function chunk(arr, size) {
				var out = [];
				for (var i = 0; i < arr.length; i += size) out.push(arr.slice(i, i + size));
				return out;
			})(data, GROUP_SIZE);

			var html = '';
			for (var i = 0; i < groups.length; i++) {
				html += buildSlideHTML(groups[i], productDetailBase);
			}
			container.innerHTML = html;

			// 렌더된 카드에도 클릭 저장 훅
			container.querySelectorAll('.rvp-link').forEach(function(a) {
				a.addEventListener('click', function() {
					RecentView.addFromDataset(a.dataset);
				});
			});
		}
	};

	window.RecentView = RecentView;

})(window, document);
