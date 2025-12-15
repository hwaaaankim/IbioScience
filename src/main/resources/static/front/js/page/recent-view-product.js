/* eslint-disable */
(function(window, document) {
	"use strict";

	var STORAGE_KEY = 'recentProducts.v1';
	var MAX_ITEMS = 10;     // ✅ 요구사항: 총 10개
	var GROUP_SIZE = 5;

	function nowMs() {
		return Date.now ? Date.now() : new Date().getTime();
	}

	function todayYMD() {
		var d = new Date();
		var m = (d.getMonth() + 1).toString().padStart(2, '0');
		var day = d.getDate().toString().padStart(2, '0');
		return d.getFullYear() + '-' + m + '-' + day;
	}

	function load() {
		try {
			if (!window.localStorage) return [];
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
			if (!window.localStorage) return;
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

	/**
	 * ✅ 요구사항 핵심:
	 * - 동일 id가 이미 있으면 "중복 추가"가 아니라
	 *   1) viewedAt/viewedDate 최신 갱신
	 *   2) 해당 항목을 맨 앞으로 이동
	 * - 없으면 새로 unshift
	 * - MAX_ITEMS 유지
	 */
	function upsertToFront(list, entry) {
		var id = entry.id;
		if (id == null) return list;

		var foundIndex = -1;
		for (var i = 0; i < list.length; i++) {
			if (String(list[i].id) === String(id)) {
				foundIndex = i;
				break;
			}
		}

		if (foundIndex >= 0) {
			var exist = list[foundIndex] || {};
			// 기존 데이터가 비어있고 새 데이터가 있으면 보강 (가능한 범위에서만)
			exist.name = (exist.name && exist.name !== '') ? exist.name : (entry.name || '');
			exist.image = (exist.image && exist.image !== '') ? exist.image : (entry.image || '');
			exist.priceNew = (exist.priceNew != null) ? exist.priceNew : entry.priceNew;
			exist.priceOld = (exist.priceOld != null) ? exist.priceOld : entry.priceOld;
			exist.rating = (exist.rating != null) ? exist.rating : entry.rating;
			exist.review = (exist.review != null) ? exist.review : entry.review;

			// ✅ 접속시간 최신 갱신
			exist.viewedAt = entry.viewedAt;
			exist.viewedDate = entry.viewedDate;

			// ✅ 맨 앞으로 이동
			list.splice(foundIndex, 1);
			list.unshift(exist);
		} else {
			list.unshift(entry);
		}

		if (list.length > MAX_ITEMS) list = list.slice(0, MAX_ITEMS);
		return list;
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

	function chunk(arr, size) {
		var out = [];
		for (var i = 0; i < arr.length; i += size) out.push(arr.slice(i, i + size));
		return out;
	}

	function parseProductIdFromPath() {
		var path = (window.location && window.location.pathname) ? window.location.pathname : '';
		// ✅ 요구사항: /productDetail/{id}
		// ✅ 오타까지 방어: /prorductDetail/{id}
		var m = path.match(/\/(productDetail|prorductDetail)\/(\d+)(\/)?$/i);
		if (m && m[2]) return m[2];

		// 프로젝트에 따라 /product/{id}로 쓰는 경우도 많아서 안전하게 추가
		var m2 = path.match(/\/product\/(\d+)(\/)?$/i);
		if (m2 && m2[1]) return m2[1];

		return null;
	}

	function getProductDetailBase() {
		var b = document.body;
		if (!b) return '/product';
		return b.getAttribute('data-product-detail-base') || '/product';
	}

	function bindClickSave(root) {
		var scope = root || document;
		var links = scope.querySelectorAll('.rvp-link');
		if (!links || links.length === 0) return;

		Array.prototype.forEach.call(links, function(a) {
			// 중복 바인딩 방지
			if (a.__rvpBound) return;
			a.__rvpBound = true;

			a.addEventListener('click', function() {
				if (!window.RecentView) return;
				window.RecentView.addFromDataset(a.dataset);
			});
		});
	}

	function autoSaveOnDetailPage() {
		// ✅ 상세페이지 접속 시 "무조건 저장"
		var id = parseProductIdFromPath();
		if (!id) return;

		// 1) 가장 권장: 상세페이지 템플릿에 아래처럼 심어두면 데이터 100% 확보 가능
		//    <div id="rvpAutoData"
		//      th:attr="data-rvp-id=${productDetail.id},
		//               data-rvp-name=${productDetail.name},
		//               data-rvp-image=${productDetail.mainImageUrl},
		//               data-rvp-pricenew=${productDetail.discountedPrice != null ? productDetail.discountedPrice : productDetail.salePrice},
		//               data-rvp-priceold=${productDetail.discountedPrice != null ? productDetail.consumerPrice : productDetail.salePrice},
		//               data-rvp-rating=${productDetail.averageRating},
		//               data-rvp-review=${productDetail.reviewCount}">
		//    </div>
		var autoEl = document.getElementById('rvpAutoData');
		if (autoEl && autoEl.dataset) {
			window.RecentView.addFromDataset(autoEl.dataset);
			return;
		}

		// 2) 차선: 페이지 내에 data-rvp-id 가진 링크/요소가 있으면 첫 번째로 저장
		var any = document.querySelector('[data-rvp-id]');
		if (any && any.dataset) {
			window.RecentView.addFromDataset(any.dataset);
			return;
		}

		// 3) 최후: id만이라도 저장(이 경우 사이드 카드 정보가 비어 보일 수 있음)
		window.RecentView.add({ id: id });
	}

	var RecentView = {
		add: function(item) {
			if (!item || item.id == null) return;

			var pn = item.priceNew != null ? toInt(item.priceNew) : null;
			var po = item.priceOld != null && item.priceOld !== '' ? toInt(item.priceOld) : null;

			// 요구사항: 할인 없으면 동일가 저장(가능하면)
			if (pn != null && (po == null)) po = pn;

			var entry = {
				id: item.id,
				name: item.name || '',
				image: item.image || '',
				priceNew: pn,
				priceOld: po,
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

			// 혹시 과거 데이터에 viewedAt이 없으면 보정
			arr = arr.map(function(p) {
				if (p && p.viewedAt == null) p.viewedAt = 0;
				if (p && p.viewedDate == null) p.viewedDate = todayYMD();
				if (p && p.priceNew != null && p.priceOld == null) p.priceOld = p.priceNew;
				return p;
			});

			// 최신순 정렬 안전장치(viewedAt 기준)
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

			var groups = chunk(data, GROUP_SIZE);

			var html = '';
			for (var i = 0; i < groups.length; i++) {
				html += buildSlideHTML(groups[i], productDetailBase);
			}
			container.innerHTML = html;

			// 렌더된 카드에도 클릭 저장 훅(전역과 별개로 안전하게)
			bindClickSave(container);
		}
	};

	window.RecentView = RecentView;

	// ✅ 전역 초기화: 어느 페이지든 동작
	function initGlobalRecentView() {
		// 1) 전역 클릭 저장 훅
		bindClickSave(document);

		// 2) 상세 페이지면 "접속만으로" 저장 (새로고침 포함)
		autoSaveOnDetailPage();

		// 3) 사이드 모듈이 있으면 렌더
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
