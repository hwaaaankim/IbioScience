/* eslint-disable */
; (function() {
	"use strict";

	/* =========================================================
	   0) API 어댑터: window.ibioMenu가 없으면 정의 (중복 방지)
	========================================================== */
	(function ensureIbioMenu() {
		if (window.ibioMenu) return;

		const API = {
			brands: "/api/menu/brands",
			large: "/api/menu/categories/large",
			medium: (largeId) =>
				`/api/menu/categories/medium?largeId=${encodeURIComponent(largeId)}`,
			small: (mediumId) =>
				`/api/menu/categories/small?mediumId=${encodeURIComponent(mediumId)}`,
			products: (params) => {
				const q = [];
				if (params.largeId) q.push(`largeId=${encodeURIComponent(params.largeId)}`);
				if (params.mediumId) q.push(`mediumId=${encodeURIComponent(params.mediumId)}`);
				if (params.smallId) q.push(`smallId=${encodeURIComponent(params.smallId)}`);
				if (params.brandId) q.push(`brandId=${encodeURIComponent(params.brandId)}`);
				return `/api/menu/products${q.length ? `?${q.join("&")}` : ""}`;
			},
		};

		async function jget(url) {
			const r = await fetch(url, { credentials: "same-origin" });
			if (!r.ok) throw new Error(`HTTP ${r.status}`);
			return r.json();
		}

		const ibioMenu = {
			/** 초기 부트스트랩: 전역 대분류 + 브랜드 */
			async bootstrap() {
				const large = Array.isArray(window.__IBIO_LARGE_CATEGORIES__)
					? window.__IBIO_LARGE_CATEGORIES__
					: [];
				const brands = await jget(API.brands);

				return {
					brands: brands.map((b) => ({
						id: b.id,
						name: b.name,
						imageUrl: b.imageUrl || b.imageRoad || null,
					})),
					large: large.map((l) => ({ id: l.id, name: l.name, medium: [] })),
				};
			},

			async fetchMedium(largeId) {
				const list = await jget(API.medium(largeId));
				return list.map((m) => ({ id: m.id, name: m.name, small: [] }));
			},

			async fetchSmall(mediumId) {
				const list = await jget(API.small(mediumId));
				return list.map((s) => ({ id: s.id, name: s.name }));
			},

			/** 교집합 제품 조회 */
			async fetchProductsByScope({
				largeId = null,
				mediumId = null,
				smallId = null,
				brandId = null,
			}) {
				const list = await jget(API.products({ largeId, mediumId, smallId, brandId }));
				return list.map((p) => ({
					id: p.id,
					name: p.name,
					brandId: p.brandId || null,
				}));
			},
		};

		window.ibioMenu = ibioMenu;
	})();

	/* =========================================================
	   1) 상태 및 유틸
	========================================================== */
	const st = {
		data: { brands: [], large: [] },
		brandId: null,
		largeId: null,
		mediumId: null,
		smallId: null,
		brandEnabled: false,
		// 모바일 현재 소분류 제품영역 참조(브랜드 변경 시 해당 영역만 갱신)
		mobileCtx: {
			prodContainer: null,
			largeId: null,
			mediumId: null,
			smallId: null,
		},
	};

	const $ = (s, r = document) => r.querySelector(s);
	const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));
	const byId = (id) => document.getElementById(id);
	const el = (t, c, h) => {
		const n = document.createElement(t);
		if (c) n.className = c;
		if (h != null) n.innerHTML = h;
		return n;
	};
	const clear = (n) => {
		if (n) n.innerHTML = "";
	};
	const todayStr = () => {
		const d = new Date();
		const y = d.getFullYear();
		const m = String(d.getMonth() + 1).padStart(2, "0");
		const day = String(d.getDate()).padStart(2, "0");
		return `${y}-${m}-${day}`;
	};

	function flatProductsOfLarge(id) {
		const L = st.data.large.find((x) => x.id === id);
		return L ? L.medium.flatMap((m) => m.small).flatMap((s) => s.products || []) : [];
	}
	function flatProductsOfMedium(id) {
		const M = st.data.large.flatMap((l) => l.medium).find((x) => x.id === id);
		return M ? M.small.flatMap((s) => s.products || []) : [];
	}
	function productsOfSmall(id) {
		const S = st.data.large
			.flatMap((l) => l.medium)
			.flatMap((m) => m.small)
			.find((x) => x.id === id);
		return S ? (S.products ? S.products.slice() : []) : [];
	}
	const applyBrandFilter = (list) =>
		st.brandId ? list.filter((p) => p.brandId === st.brandId) : list;

	function enableBrand(on) {
		st.brandEnabled = on;
		const box = byId("ibio-index-brand-list");
		if (on) box?.classList.remove("ibio-index-disabled");
		else box?.classList.add("ibio-index-disabled");
	}
	function resetBrand() {
		st.brandId = null;
		enableBrand(false);
		renderBrands();
	}

	/* =========================================================
	   2) 최근검색어 (PC/모바일 공통)
	========================================================== */
	const RECENT_KEY = "ibio-index-recent-search-v2";
	const MAX_STORE = 100;
	const MAX_PC_VIEW = 10;
	const MAX_M_VIEW = 7;

	function loadRecent() {
		try {
			const r = JSON.parse(localStorage.getItem(RECENT_KEY) || "[]");
			if (Array.isArray(r))
				return r.filter((x) => x && typeof x.q === "string" && typeof x.date === "string");
			return [];
		} catch (_) {
			return [];
		}
	}
	function saveRecent(list) {
		const trimmed = list.slice(0, MAX_STORE);
		localStorage.setItem(RECENT_KEY, JSON.stringify(trimmed));
	}
	function upsertRecent(term) {
		const q = String(term || "").trim();
		if (!q) return;
		let list = loadRecent();
		list = list.filter((x) => x.q !== q);
		list.unshift({ q, date: todayStr() });
		if (list.length > MAX_STORE) list = list.slice(0, MAX_STORE);
		saveRecent(list);
	}
	function removeRecentItem(q) {
		const list = loadRecent().filter((x) => x.q !== q);
		saveRecent(list);
	}
	function clearAllRecent() {
		saveRecent([]);
	}
	function filterRecent(list, keyword) {
		const k = String(keyword || "").trim().toLowerCase();
		if (!k) return list;
		return list.filter((x) => x.q.toLowerCase().includes(k));
	}
	function renderRecentList(container, options) {
		const { maxView, currentQuery, onClickKeyword, onDelete } = options;
		let list = loadRecent();
		list = filterRecent(list, currentQuery);
		clear(container);

		if (!list.length) {
			container.appendChild(
				el("div", "ibio-index-empty-recent", "최근 검색어가 없습니다.")
			);
			return;
		}

		const toShow = list.slice(0, maxView);
		toShow.forEach((item) => {
			const row = el("div", "ibio-index-recent-row");
			const left = el("div", "ibio-index-recent-left");
			const kw = el("span", "ibio-index-recent-keyword", item.q);
			const date = el("span", "ibio-index-recent-date", `(${item.date})`);

			kw.addEventListener("click", () => onClickKeyword && onClickKeyword(item.q));

			left.appendChild(kw);
			left.appendChild(date);

			const del = el("button", "ibio-index-recent-del", "삭제");
			del.type = "button";
			del.addEventListener("click", (e) => {
				e.stopPropagation();
				onDelete && onDelete(item.q);
			});

			row.appendChild(left);
			row.appendChild(del);
			container.appendChild(row);
		});
	}

	/* =========================================================
	   3) PC 메가메뉴
	========================================================== */
	const mega = byId("ibio-index-mega");
	const openBtn = byId("ibio-index-allcat-trigger");
	const closeBtn = byId("ibio-index-mega-close");
	const ulLarge = byId("ibio-index-large-list");
	const ulMedium = byId("ibio-index-medium-list");
	const ulSmall = byId("ibio-index-small-list");
	const ulProd = byId("ibio-index-product-list");
	const brandList = byId("ibio-index-brand-list");
	const productListLink = byId("ibio-index-product-list-link");

	async function ensureMediumLoaded(largeId) {
		const L = st.data.large.find((x) => x.id === largeId);
		if (!L) return null;
		if (!L.medium || L.medium.length === 0) {
			L.medium = await window.ibioMenu.fetchMedium(largeId);
		}
		return L;
	}
	async function ensureSmallLoaded(mediumId) {
		const M = st.data.large.flatMap((l) => l.medium).find((x) => x.id === mediumId);
		if (!M) return null;
		if (!M.small || M.small.length === 0) {
			M.small = await window.ibioMenu.fetchSmall(mediumId);
		}
		return M;
	}

	function setEmpty(list, text) {
		clear(list);
		list.classList.add("ibio-index-empty");
		list.innerHTML =
			'<li style="height:var(--ibio-item-h);display:flex;align-items:center;padding:0 12px;border-bottom:1px solid #f3f4f6;color:#9aa3ae;">' +
			text +
			"</li>";
	}

	function hasCategoryScope() {
		// 최소 대분류가 선택되어 있어야 제품리스트 링크 활성화
		return !!st.largeId;
	}

	function buildProductListUrl({ largeId, mediumId, smallId, brandId, page, size, sort }) {
		const params = [];
		if (largeId) params.push(`largeId=${encodeURIComponent(largeId)}`);
		if (mediumId) params.push(`mediumId=${encodeURIComponent(mediumId)}`);
		if (smallId) params.push(`smallId=${encodeURIComponent(smallId)}`);
		if (brandId) params.push(`brandId=${encodeURIComponent(brandId)}`);
		if (page != null) params.push(`page=${encodeURIComponent(page)}`);
		if (size != null) params.push(`size=${encodeURIComponent(size)}`);
		if (sort) params.push(`sort=${encodeURIComponent(sort)}`);
		return `/productList${params.length ? `?${params.join("&")}` : ""}`;
	}

	function updateProductListLinkState() {
		if (!productListLink) return;
		if (hasCategoryScope()) {
			productListLink.classList.remove("disabled");
			productListLink.setAttribute("aria-disabled", "false");
		} else {
			productListLink.classList.add("disabled");
			productListLink.setAttribute("aria-disabled", "true");
		}
	}

	function openMega(e) {
		if (e) e.preventDefault();
		if (window.matchMedia("(max-width: 991.98px)").matches) return;
		if (mega?.getAttribute("aria-hidden") === "false") {
			closeMega();
			return;
		}
		mega?.setAttribute("aria-hidden", "false");
		st.largeId = st.mediumId = st.smallId = null;
		resetBrand();
		updateProductListLinkState();
		renderLarge();
		setEmpty(ulMedium, "중분류가 없습니다.");
		setEmpty(ulSmall, "소분류가 없습니다.");
		setEmpty(ulProd, "제품이 여기에 표시됩니다.");
		enableScrollLockOverMega(true);
		setTimeout(() => document.addEventListener("click", outsideClose), 0);
	}

	function closeMega() {
		if (mega?.getAttribute("aria-hidden") === "true") return;
		mega?.setAttribute("aria-hidden", "true");
		enableScrollLockOverMega(false);
		document.removeEventListener("click", outsideClose);
	}

	function outsideClose(ev) {
		if (!mega.contains(ev.target) && !openBtn.contains(ev.target)) closeMega();
	}
	openBtn?.addEventListener("click", openMega);
	closeBtn?.addEventListener("click", closeMega);

	// 제품리스트페이지 링크 클릭 → 현재 선택된 범위 & 브랜드 정보로 /productList 이동
	productListLink?.addEventListener("click", (e) => {
		e.preventDefault();
		if (!hasCategoryScope()) return; // 대분류 미선택 시 무시
		const url = buildProductListUrl({
			largeId: st.largeId,
			mediumId: st.mediumId,
			smallId: st.smallId,
			brandId: st.brandId,
			page: 0,
			size: 15, // 기본 15개씩 보기
			sort: "CREATED_AT_DESC", // 기본 등록일순(최신)
		});
		window.location.href = url;
	});

	const guardZones = [document.querySelector(".main-menu-w"), document.getElementById("ibio-index-mega")];
	let isPointerOverMenu = false;
	guardZones.forEach((z) => {
		if (!z) return;
		z.addEventListener("mouseenter", () => {
			isPointerOverMenu = true;
		});
		z.addEventListener("mouseleave", () => {
			isPointerOverMenu = false;
		});
	});
	function isInGuard(target) {
		return guardZones.some((z) => z && z.contains(target));
	}
	let touchGuard = false;
	document.addEventListener(
		"touchstart",
		(e) => {
			touchGuard = isInGuard(e.target);
		},
		{ passive: true }
	);
	document.addEventListener(
		"touchend",
		() => {
			touchGuard = false;
		},
		{ passive: true }
	);
	document.addEventListener(
		"touchcancel",
		() => {
			touchGuard = false;
		},
		{ passive: true }
	);

	let lastY = window.scrollY;
	let scrollTimer = null;
	window.addEventListener(
		"scroll",
		() => {
			if (scrollTimer) return;
			scrollTimer = setTimeout(() => {
				const now = window.scrollY;
				const moved = Math.abs(now - lastY) > 5;
				const isMobile = window.matchMedia("(max-width: 991.98px)").matches;
				if (
					mega?.getAttribute("aria-hidden") === "false" &&
					moved &&
					!isMobile &&
					!isPointerOverMenu &&
					!touchGuard
				) {
					closeMega();
				}
				lastY = now;
				scrollTimer = null;
			}, 80);
		},
		{ passive: true }
	);

	function renderLarge() {
		clear(ulLarge);
		st.data.large.forEach((L) => {
			const li = el("li", null);
			const txt = el("span", "ibio-index-text", L.name);
			const arr = el("span", "ibio-index-arrow", "›");

			// 텍스트 클릭: 해당 대분류의 모든 제품(브랜드 필터 적용)
			txt.addEventListener("click", async () => {
				st.largeId = L.id;
				st.mediumId = st.smallId = null;
				ulLarge.querySelectorAll("li").forEach((x) => x.classList.remove("active"));
				li.classList.add("active");

				updateProductListLinkState();

				await ensureMediumLoaded(L.id);
				// medium들의 small 로드 및 제품 로딩
				const mediums = st.data.large.find((x) => x.id === L.id)?.medium || [];
				for (const M of mediums) {
					await ensureSmallLoaded(M.id);
					if (Array.isArray(M.small)) {
						for (const S of M.small) {
							if (!S.products) {
								S.products = await window.ibioMenu.fetchProductsByScope({
									largeId: L.id,
									mediumId: M.id,
									smallId: S.id,
									brandId: null,
								});
							}
						}
					}
				}

				enableBrand(true);
				renderProducts(applyBrandFilter(flatProductsOfLarge(L.id)), `${L.name} 제품`);
				setEmpty(ulMedium, "중분류가 없습니다.");
				setEmpty(ulSmall, "소분류가 없습니다.");
			});

			// 화살표 클릭: 중분류 목록 펼치기 (이 경우도 대분류 선택으로 간주)
			arr.addEventListener("click", async (e) => {
				e.stopPropagation();
				st.largeId = L.id;
				st.mediumId = st.smallId = null;
				ulLarge.querySelectorAll("li").forEach((x) => x.classList.remove("active"));
				li.classList.add("active");

				updateProductListLinkState();

				await renderMedium(L.id);
				setEmpty(ulProd, "제품이 여기에 표시됩니다.");
				setEmpty(ulSmall, "소분류가 없습니다.");
				resetBrand();
			});

			li.append(txt, arr);
			ulLarge.appendChild(li);
		});
	}

	async function renderMedium(largeId) {
		clear(ulMedium);
		ulMedium.classList.remove("ibio-index-empty");
		const L = await ensureMediumLoaded(largeId);
		if (!L) return;

		L.medium.forEach((M) => {
			const li = el("li", null);
			const txt = el("span", "ibio-index-text", M.name);
			const arr = el("span", "ibio-index-arrow", "›");

			// 텍스트 클릭: 중분류 하위 전체 제품
			txt.addEventListener("click", async () => {
				st.mediumId = M.id;
				st.smallId = null;
				ulMedium.querySelectorAll("li").forEach((x) => x.classList.remove("active"));
				li.classList.add("active");

				updateProductListLinkState();

				await ensureSmallLoaded(M.id);
				const mm = L.medium.find((x) => x.id === M.id);
				if (mm && Array.isArray(mm.small)) {
					for (const S of mm.small) {
						if (!S.products) {
							S.products = await window.ibioMenu.fetchProductsByScope({
								largeId: largeId,
								mediumId: M.id,
								smallId: S.id,
								brandId: null,
							});
						}
					}
				}

				enableBrand(true);
				renderProducts(applyBrandFilter(flatProductsOfMedium(M.id)), `${M.name} 제품`);
				setEmpty(ulSmall, "소분류가 없습니다.");
			});

			// 화살표 클릭: 소분류 목록
			arr.addEventListener("click", async (e) => {
				e.stopPropagation();
				st.mediumId = M.id;
				st.smallId = null;
				ulMedium.querySelectorAll("li").forEach((x) => x.classList.remove("active"));
				li.classList.add("active");

				updateProductListLinkState();

				await renderSmall(M.id);
				setEmpty(ulProd, "제품이 여기에 표시됩니다.");
				resetBrand();
			});

			li.append(txt, arr);
			ulMedium.appendChild(li);
		});
	}

	// ★★★ 수정된 부분: PC용 소분류 클릭 로직 분리 ★★★
	async function renderSmall(mediumId) {
		clear(ulSmall);
		ulSmall.classList.remove("ibio-index-empty");

		const M = await ensureSmallLoaded(mediumId);
		if (!M) return;

		M.small.forEach((S) => {
			const li = el("li", null);
			const txt = el("span", "ibio-index-text", S.name);
			const arr = el("span", "ibio-index-arrow", "›");

			// PC: 소분류 텍스트/화살표 클릭 시, 해당 소분류 제품을 우측 제품 리스트에 렌더
			const show = async () => {
				st.smallId = S.id;
				ulSmall.querySelectorAll("li").forEach((x) => x.classList.remove("active"));
				li.classList.add("active");

				updateProductListLinkState();

				// 소분류 하위 제품 선로딩
				if (!S.products) {
					S.products = await window.ibioMenu.fetchProductsByScope({
						largeId: st.largeId,
						mediumId: st.mediumId,
						smallId: S.id,
						brandId: null,
					});
				}

				enableBrand(true);
				// 우측 제품 리스트 영역(PC) 렌더링
				renderProducts(
					applyBrandFilter(productsOfSmall(S.id)),
					`${S.name} 제품`
				);
			};

			txt.addEventListener("click", show);
			arr.addEventListener("click", (e) => {
				e.stopPropagation();
				show();
			});

			li.append(txt, arr);
			ulSmall.appendChild(li);
		});
	}

	function renderProducts(list, title) {
		clear(ulProd);
		ulProd.classList.remove("ibio-index-empty");

		if (!list.length) {
			setEmpty(ulProd, `${title}이(가) 없습니다.`);
			return;
		}

		list.forEach((p) => {
			const li = el("li", null);
			const a = el("a", "ibio-index-text");
			a.href = `/productDetail/${encodeURIComponent(p.id)}`;
			a.textContent = p.name;
			li.appendChild(a);
			ulProd.appendChild(li);
		});
	}

	function renderBrands() {
		clear(brandList);
		st.data.brands.forEach((b) => {
			const logo = b.imageUrl
				? `<img class="ibio-index-brand-badge" src="${b.imageUrl}" alt="${b.name}">`
				: `<div class="ibio-index-brand-badge">150×30</div>`;
			const item = el(
				"div",
				"ibio-index-brand-item" + (st.brandId === b.id ? " active" : ""),
				`${logo}<span>${b.name}</span>`
			);
			item.addEventListener("click", () => {
				if (!st.brandEnabled) return;
				st.brandId = st.brandId === b.id ? null : b.id;
				$$(".ibio-index-brand-item", brandList).forEach((x) =>
					x.classList.remove("active")
				);
				if (st.brandId === b.id) item.classList.add("active");
				refreshProductsByScope(); // PC 패널 갱신
				refreshMobileProductsByScope(); // 모바일 패널 갱신
			});
			brandList.appendChild(item);
		});
	}

	function refreshProductsByScope() {
		let list = [];
		if (st.smallId) list = productsOfSmall(st.smallId);
		else if (st.mediumId) list = flatProductsOfMedium(st.mediumId);
		else if (st.largeId) list = flatProductsOfLarge(st.largeId);
		renderProducts(applyBrandFilter(list), "제품");
	}

	/* =========================================================
	   4) 모바일 패널/브랜드 모달
	   - (A) slideOpenAsync/slideToggleAsync: 콘텐츠 로드 후 오픈
	   - (B) 각 단계의 '>' 버튼 → /productList 이동
	   - (C) 대분류 선택 시부터 브랜드 FAB 활성화
	========================================================== */
	const mOverlay = byId("ibio-index-m-overlay");
	const mPanel = byId("ibio-index-m-panel");
	const mOpenBtn = byId("ibio-index-hamburger");
	const mCloseBtn = byId("ibio-index-m-close");
	const mLarge = byId("ibio-index-m-large");
	const fab = byId("ibio-index-brand-fab");
	const bModal = byId("ibio-index-brand-modal");
	const bModalClose = byId("ibio-index-brand-modal-close");
	const bModalBody = byId("ibio-index-brand-modal-body");
	let bodyScrollTop = 0;

	function lockBody() {
		bodyScrollTop = window.scrollY || document.documentElement.scrollTop;
		document.body.classList.add("ibio-index-lock");
		document.body.style.top = `-${bodyScrollTop}px`;
	}
	function unlockBody() {
		document.body.classList.remove("ibio-index-lock");
		document.body.style.top = "";
		window.scrollTo(0, bodyScrollTop);
	}

	function openMobile() {
		if (mOverlay?.getAttribute("aria-hidden") === "false") return;
		lockBody();
		renderMobileLarge();
		fab && (fab.disabled = true); // 처음엔 비활성화
		mOverlay?.setAttribute("aria-hidden", "false");
		if (mPanel) {
			mPanel.dataset.state = "showing";
			mPanel.addEventListener("animationend", function onEnd() {
				mPanel.dataset.state = "visible";
				mPanel.removeEventListener("animationend", onEnd);
			});
		}
	}
	function closeMobile() {
		if (mOverlay?.getAttribute("aria-hidden") === "true") return;
		if (mPanel) {
			mPanel.dataset.state = "hiding";
			mPanel.addEventListener("animationend", function onEnd() {
				mPanel.dataset.state = "hidden";
				mOverlay?.setAttribute("aria-hidden", "true");
				unlockBody();
				mPanel.removeEventListener("animationend", onEnd);
			});
		} else {
			mOverlay?.setAttribute("aria-hidden", "true");
			unlockBody();
		}
	}
	mOpenBtn?.addEventListener("click", openMobile);
	mCloseBtn?.addEventListener("click", closeMobile);
	mOverlay?.addEventListener("click", (e) => {
		if (e.target === mOverlay) closeMobile();
	});
	["touchmove", "wheel"].forEach((ev) => {
		mOverlay?.addEventListener(
			ev,
			(e) => {
				if (!mPanel?.contains(e.target)) e.preventDefault();
			},
			{ passive: false }
		);
	});

	// ===== 아코디언: after-load slide =====
	async function slideOpenAsync(elem, buildFn) {
		if (elem.dataset.state === "open") return;
		if (elem.dataset.busy === "1") return;
		elem.dataset.busy = "1";

		// 1) 표시/초기화
		elem.style.display = "block";
		elem.style.overflow = "hidden";
		elem.style.height = "0px";
		elem.classList.add("open");

		// 2) 콘텐츠 선로딩/빌드
		if (typeof buildFn === "function") {
			await buildFn(); // 콘텐츠를 전부 만든 뒤
		}

		// 3) 최종 높이 측정 후 애니메이션 시작
		// (빌드 이후 reflow 보장)
		elem.getBoundingClientRect();
		const target = elem.scrollHeight;
		elem.style.transition = "height .22s cubic-bezier(.25,.1,.25,1)";
		requestAnimationFrame(() => {
			elem.style.height = target + "px";
		});

		// 4) 종료 처리
		const onEnd = (e) => {
			if (e.propertyName !== "height") return;
			elem.style.transition = "";
			elem.style.height = "auto";
			elem.style.overflow = "visible";
			elem.dataset.state = "open";
			elem.dataset.busy = "0";
			elem.removeEventListener("transitionend", onEnd);
		};
		elem.addEventListener("transitionend", onEnd);
	}

	function slideClose(elem) {
		if (elem.dataset.state !== "open" && elem.style.display === "none") return;
		if (elem.dataset.busy === "1") return;
		elem.dataset.busy = "1";

		elem.style.overflow = "hidden";
		const start = elem.scrollHeight;
		elem.style.height = start + "px";
		elem.getBoundingClientRect();
		elem.style.transition = "height .22s cubic-bezier(.25,.1,.25,1)";
		elem.style.height = "0px";

		const onEnd = (e) => {
			if (e.propertyName !== "height") return;
			elem.style.transition = "";
			elem.style.height = "0px";
			elem.style.display = "none";
			elem.classList.remove("open");
			elem.dataset.state = "closed";
			elem.dataset.busy = "0";
			elem.removeEventListener("transitionend", onEnd);
		};
		elem.addEventListener("transitionend", onEnd);
	}

	async function slideToggleAsync(elem, buildFnIfOpen) {
		if (elem.dataset.state === "open") slideClose(elem);
		else await slideOpenAsync(elem, buildFnIfOpen);
	}

	function renderMobileLarge() {
		clear(mLarge);
		st.data.large.forEach((L) => {
			const li = el("li", "ibio-index-m-li");

			const row = el("div", "ibio-index-m-row");
			row.innerHTML = `<span>${L.name}</span><span class="ibio-index-m-arrow">›</span>`;
			const sub = el("div", "ibio-index-m-sub");
			sub.style.display = "none";

			const arrow = row.querySelector(".ibio-index-m-arrow");

			// 텍스트/행 클릭: 하위 중분류 아코디언
			row.addEventListener("click", () => {
				st.largeId = L.id;
				// 모바일에서도 대분류 선택 시점부터 브랜드 FAB 활성화
				if (fab) fab.disabled = false;

				slideToggleAsync(sub, async () => {
					await renderMobileMedium(sub, L.id);
				});
			});

			// '>' 클릭: 현재 대분류 + (선택된 브랜드) 기준 제품리스트 페이지로 이동
			arrow.addEventListener("click", (ev) => {
				ev.stopPropagation();
				st.largeId = L.id;
				st.mediumId = null;
				st.smallId = null;
				const url = buildProductListUrl({
					largeId: L.id,
					mediumId: null,
					smallId: null,
					brandId: st.brandId,
					page: 0,
					size: 15,
					sort: "CREATED_AT_DESC",
				});
				window.location.href = url;
			});

			li.append(row, sub);
			mLarge.appendChild(li);
		});
	}

	async function renderMobileMedium(container, largeId) {
		const L = await ensureMediumLoaded(largeId);
		if (!L) return;
		clear(container);
		(L.medium || []).forEach((M) => {
			const li = el("div", "ibio-index-m-li");

			const row = el("div", "ibio-index-m-row");
			row.innerHTML = `<span>${M.name}</span><span class="ibio-index-m-arrow">›</span>`;
			const sub = el("div", "ibio-index-m-sub");
			sub.style.display = "none";

			const arrow = row.querySelector(".ibio-index-m-arrow");

			// 텍스트/행 클릭: 하위 소분류 아코디언
			row.addEventListener("click", () => {
				st.mediumId = M.id;
				slideToggleAsync(sub, async () => {
					await renderMobileSmall(sub, M.id);
				});
			});

			// '>' 클릭: 대+중+브랜드 기준 제품리스트 페이지로 이동
			arrow.addEventListener("click", (ev) => {
				ev.stopPropagation();
				st.mediumId = M.id;
				st.smallId = null;
				const url = buildProductListUrl({
					largeId: largeId,
					mediumId: M.id,
					smallId: null,
					brandId: st.brandId,
					page: 0,
					size: 15,
					sort: "CREATED_AT_DESC",
				});
				window.location.href = url;
			});

			li.append(row, sub);
			container.appendChild(li);
		});
	}

	async function renderMobileSmall(container, mediumId) {
		const M = await ensureSmallLoaded(mediumId);
		if (!M) return;
		clear(container);
		(M.small || []).forEach((S) => {
			const li = el("div", "ibio-index-m-li");

			const row = el(
				"div",
				"ibio-index-m-row",
				`<span>${S.name}</span><span class="ibio-index-m-arrow">›</span>`
			);
			const sub = el("div", "ibio-index-m-sub");
			sub.style.display = "none";

			const arrow = row.querySelector(".ibio-index-m-arrow");

			// 행 클릭: 소분류 제품 리스트(아코디언) 오픈
			row.addEventListener("click", async () => {
				st.smallId = S.id;
				st.mobileCtx.prodContainer = sub;
				st.mobileCtx.largeId = st.largeId;
				st.mobileCtx.mediumId = st.mediumId;
				st.mobileCtx.smallId = S.id;

				// 데이터 로드 → DOM 빌드 → 슬라이드 오픈
				await slideToggleAsync(sub, async () => {
					await openSmallAfterLoad(S, sub);
				});

				enableBrand(true);
			});

			// '>' 클릭: 대+중+소+브랜드 기준으로 제품리스트 페이지 이동
			arrow.addEventListener("click", (ev) => {
				ev.stopPropagation();
				st.smallId = S.id;
				const url = buildProductListUrl({
					largeId: st.largeId,
					mediumId: st.mediumId,
					smallId: S.id,
					brandId: st.brandId,
					page: 0,
					size: 15,
					sort: "CREATED_AT_DESC",
				});
				window.location.href = url;
			});

			li.append(row, sub);
			container.appendChild(li);
		});
	}

	// ★★★ 수정된 부분: 모바일 전용 소분류 데이터 로드 함수 ★★★
	// 소분류 클릭 시, 데이터 선로딩 후 제품 목록 구성 → 그 다음 슬라이드 오픈
	async function openSmallAfterLoad(S, subEl) {
		const container = subEl || st.mobileCtx.prodContainer;
		// PC에서 잘못 호출되는 경우를 방지하기 위한 안전장치
		if (!container) return;

		if (!S.products) {
			S.products = await window.ibioMenu.fetchProductsByScope({
				largeId: st.largeId,
				mediumId: st.mediumId,
				smallId: S.id,
				brandId: null,
			});
		}
		await renderMobileProductsInto(container, S.id);
	}

	async function renderMobileProductsInto(container, smallId) {
		clear(container);
		const list = applyBrandFilter(productsOfSmall(smallId));
		if (!list.length) {
			const empty = el("a", "ibio-index-m-prod");
			empty.href = "javascript:void(0)";
			empty.setAttribute("aria-disabled", "true");
			empty.classList.add("is-empty");
			empty.textContent = "해당 조건의 제품이 없습니다.";
			container.appendChild(empty);
			return;
		}
		list.forEach((p) => {
			const pd = el("a", "ibio-index-m-prod");
			pd.href = `/productDetail/${encodeURIComponent(p.id)}`;
			pd.textContent =
				p.name + (p.brandId != null ? ` (BRAND ${String(p.brandId).padStart(2, "0")})` : "");
			container.appendChild(pd);
		});
	}

	// 브랜드 모달(모바일) 및 토글시, 현재 열린 소분류 제품 리스트만 교체 렌더
	function refreshMobileProductsByScope() {
		const ctx = st.mobileCtx;
		if (!ctx || !ctx.prodContainer || ctx.smallId == null) return;
		renderMobileProductsInto(ctx.prodContainer, ctx.smallId);
	}

	function renderBrandModal() {
		const cur = st.brandId;
		clear(bModalBody);
		st.data.brands.forEach((b) => {
			const logo = b.imageUrl
				? `<img class="ibio-index-brand-badge" src="${b.imageUrl}" alt="${b.name}">`
				: `<div class="ibio-index-brand-badge">150×30</div>`;
			const btn = el(
				"button",
				"ibio-index-brand-btn" + (cur === b.id ? " active" : ""),
				`${logo}<span>${b.name}</span>`
			);
			btn.type = "button";
			btn.addEventListener("click", () => {
				st.brandId = st.brandId === b.id ? null : b.id;
				renderBrandModal(); // 모달 내부 버튼 active 토글
				refreshMobileProductsByScope(); // 현재 열린 소분류 제품 리스트만 교체
				bModal?.setAttribute("aria-hidden", "true");
			});
			bModalBody.appendChild(btn);
		});
	}
	byId("ibio-index-brand-fab")?.addEventListener("click", () => {
		if (fab?.disabled) return;
		renderBrandModal();
		bModal?.setAttribute("aria-hidden", "false");
	});
	bModalClose?.addEventListener("click", () =>
		bModal?.setAttribute("aria-hidden", "true")
	);
	bModal?.addEventListener("click", (e) => {
		if (e.target === bModal) bModal?.setAttribute("aria-hidden", "true");
	});

	/* =========================================================
	   5) 반응형 전환: 992 미만 즉시 모바일 UX
	========================================================== */
	const mqMobile = window.matchMedia("(max-width: 991.98px)");
	function onBreakpointChange(e) {
		if (e.matches) {
			closeMega();
		}
	}
	mqMobile.addEventListener("change", onBreakpointChange);
	onBreakpointChange(mqMobile);

	/* =========================================================
	   6) 검색 UX (기존 유지)
	========================================================== */
	// PC
	const pcInput = byId("ibio-index-pc-search-input");
	const pcForm = byId("ibio-index-pc-search-form");
	const dd = byId("ibio-index-search-dropdown");
	const ddList = byId("ibio-index-search-dd-list");
	const ddClear = byId("ibio-index-search-dd-clear");

	// Mobile
	const mSearchOverlay = byId("ibio-index-m-search-overlay");
	const mSearchOpenBtn = byId("ibio-index-mobile-search");
	const mSearchCloseBtn = byId("ibio-index-m-search-close");
	const mSearchForm = byId("ibio-index-m-search-form");
	const mSearchInput = byId("ibio-index-m-search-input");
	const mRecentWrap = byId("ibio-index-m-search-recent-list");
	const mClearBtn = byId("ibio-index-m-search-clear");

	function submitSearchFromPC(q) {
		upsertRecent(q);
		pcForm?.requestSubmit();
		closeDropdown();
	}
	function submitSearchFromMobile(q) {
		upsertRecent(q);
		mSearchForm?.requestSubmit();
		closeMobileSearch();
	}

	function openDropdown() {
		if (window.matchMedia("(max-width: 991.98px)").matches) return;
		renderRecentList(ddList, {
			maxView: MAX_PC_VIEW,
			currentQuery: pcInput?.value || "",
			onClickKeyword: (kw) => {
				if (pcInput) pcInput.value = kw;
				submitSearchFromPC(kw);
			},
			onDelete: (kw) => {
				removeRecentItem(kw);
				renderRecentList(ddList, {
					maxView: MAX_PC_VIEW,
					currentQuery: pcInput?.value || "",
					onClickKeyword: (k) => {
						if (pcInput) pcInput.value = k;
						submitSearchFromPC(k);
					},
					onDelete: (k) => {
						removeRecentItem(k);
						openDropdown();
					},
				});
			},
		});
		dd?.setAttribute("aria-hidden", "false");
	}
	function closeDropdown() {
		dd?.setAttribute("aria-hidden", "true");
	}
	function outsideCloseForDD(e) {
		if (!dd?.contains(e.target) && e.target !== pcInput) closeDropdown();
	}

	pcInput?.addEventListener("focus", openDropdown);
	pcInput?.addEventListener("click", openDropdown);
	document.addEventListener("click", outsideCloseForDD);

	pcInput?.addEventListener("input", () => {
		if (dd?.getAttribute("aria-hidden") === "true") return;
		renderRecentList(ddList, {
			maxView: MAX_PC_VIEW,
			currentQuery: pcInput.value,
			onClickKeyword: (kw) => {
				pcInput.value = kw;
				submitSearchFromPC(kw);
			},
			onDelete: (kw) => {
				removeRecentItem(kw);
				openDropdown();
			},
		});
	});

	ddClear?.addEventListener("click", () => {
		clearAllRecent();
		renderRecentList(ddList, {
			maxView: MAX_PC_VIEW,
			currentQuery: pcInput?.value || "",
			onClickKeyword: (k) => {
				pcInput.value = k;
				submitSearchFromPC(k);
			},
			onDelete: (k) => {
				removeRecentItem(k);
				openDropdown();
			},
		});
	});

	pcForm?.addEventListener("submit", () => {
		const q = pcInput?.value || "";
		upsertRecent(q);
		closeDropdown();
	});

	function openMobileSearch() {
		mSearchOverlay?.setAttribute("aria-hidden", "false");
		renderRecentList(mRecentWrap, {
			maxView: MAX_M_VIEW,
			currentQuery: mSearchInput?.value || "",
			onClickKeyword: (kw) => {
				if (mSearchInput) mSearchInput.value = kw;
				submitSearchFromMobile(kw);
			},
			onDelete: (kw) => {
				removeRecentItem(kw);
				openMobileSearch();
			},
		});
		setTimeout(() => mSearchInput?.focus(), 0);
	}
	function closeMobileSearch() {
		mSearchOverlay?.setAttribute("aria-hidden", "true");
	}
	mSearchOpenBtn?.addEventListener("click", openMobileSearch);
	mSearchCloseBtn?.addEventListener("click", closeMobileSearch);
	mSearchOverlay?.addEventListener("click", (e) => {
		if (e.target === mSearchOverlay) closeMobileSearch();
	});

	mClearBtn?.addEventListener("click", () => {
		clearAllRecent();
		renderRecentList(mRecentWrap, {
			maxView: MAX_M_VIEW,
			currentQuery: mSearchInput?.value || "",
			onClickKeyword: (k) => {
				mSearchInput.value = k;
				submitSearchFromMobile(k);
			},
			onDelete: (k) => {
				removeRecentItem(k);
				openMobileSearch();
			},
		});
	});

	mSearchInput?.addEventListener("input", () => {
		renderRecentList(mRecentWrap, {
			maxView: MAX_M_VIEW,
			currentQuery: mSearchInput.value,
			onClickKeyword: (kw) => {
				mSearchInput.value = kw;
				submitSearchFromMobile(kw);
			},
			onDelete: (kw) => {
				removeRecentItem(kw);
				openMobileSearch();
			},
		});
	});

	mSearchForm?.addEventListener("submit", () => {
		const q = mSearchInput?.value || "";
		upsertRecent(q);
		closeMobileSearch();
	});

	document.addEventListener("keydown", (e) => {
		if (e.key === "Escape") {
			closeDropdown();
			closeMobileSearch();
		}
	});

	/* =========================================================
	   7) 메가메뉴 스크롤 락 (PC 전용)
	========================================================== */
	(function() {
		const root = document;
		const WHEEL_OPTS = { passive: false };
		let touchStartY = 0;
		let lockEnabled = false;

		function findScrollableAncestor(start, scopeRoot) {
			let elx = start;
			while (elx && elx !== scopeRoot.parentElement) {
				if (elx === scopeRoot) break;
				const stx = getComputedStyle(elx);
				const can =
					/(auto|scroll)/.test(stx.overflowY) && elx.scrollHeight > elx.clientHeight + 1;
				if (can) return elx;
				elx = elx.parentElement;
			}
			return scopeRoot.querySelector(".ibio-index-col-list") || scopeRoot;
		}

		function clampScroll(elx, dy) {
			const prev = elx.scrollTop;
			elx.scrollTop = prev + dy;
		}

		function onWheel(e) {
			if (!lockEnabled) return;
			if (window.matchMedia("(max-width: 991.98px)").matches) return;
			if (mega?.getAttribute("aria-hidden") === "true") return;
			if (!mega?.contains(e.target)) return;
			const scroller = findScrollableAncestor(e.target, mega);
			clampScroll(scroller, e.deltaY);
			e.preventDefault();
		}

		function onTouchStart(e) {
			if (!mega?.contains(e.target)) return;
			touchStartY = e.touches[0]?.clientY || 0;
		}

		function onTouchMove(e) {
			if (!lockEnabled) return;
			if (window.matchMedia("(max-width: 991.98px)").matches) return;
			if (mega?.getAttribute("aria-hidden") === "true") return;
			if (!mega?.contains(e.target)) return;

			const curY = e.touches[0]?.clientY || 0;
			const deltaY = touchStartY - curY;
			const scroller = findScrollableAncestor(e.target, mega);
			clampScroll(scroller, deltaY);
			touchStartY = curY;
			e.preventDefault();
		}

		const KEY_STEP = 48;
		function onKeydown(e) {
			if (!lockEnabled) return;
			if (window.matchMedia("(max-width: 991.98px)").matches) return;
			if (mega?.getAttribute("aria-hidden") === "true") return;

			const focusEl = root.activeElement || root.body;
			if (!mega?.contains(focusEl)) return;

			const keys = ["ArrowDown", "ArrowUp", "PageDown", "PageUp", "Home", "End", " "];
			if (!keys.includes(e.key)) return;

			const scroller = findScrollableAncestor(focusEl, mega);
			let delta = 0;
			if (e.key === "ArrowDown") delta = KEY_STEP;
			else if (e.key === "ArrowUp") delta = -KEY_STEP;
			else if (e.key === "PageDown") delta = scroller.clientHeight - 20;
			else if (e.key === "PageUp") delta = -(scroller.clientHeight - 20);
			else if (e.key === "Home") scroller.scrollTop = 0;
			else if (e.key === "End") scroller.scrollTop = scroller.scrollHeight;
			else if (e.key === " ") delta = KEY_STEP * (e.shiftKey ? -1 : 1);

			if (delta !== 0 || ["Home", "End"].includes(e.key)) {
				e.preventDefault();
				if (delta) clampScroll(scroller, delta);
			}
		}

		// 외부에서 켜고 끄는 진입점
		window.enableScrollLockOverMega = function(enable) {
			if (enable === lockEnabled) return;
			lockEnabled = !!enable;

			if (lockEnabled) {
				root.addEventListener("wheel", onWheel, WHEEL_OPTS);
				root.addEventListener("touchstart", onTouchStart, { passive: true });
				root.addEventListener("touchmove", onTouchMove, WHEEL_OPTS);
				root.addEventListener("keydown", onKeydown, false);
			} else {
				root.removeEventListener("wheel", onWheel, WHEEL_OPTS);
				root.removeEventListener("touchstart", onTouchStart, { passive: true });
				root.removeEventListener("touchmove", onTouchMove, WHEEL_OPTS);
				root.removeEventListener("keydown", onKeydown, false);
			}
		};
	})();

	/* =========================================================
	   8) 부트스트랩 시작: 실제 데이터 로드
	========================================================== */
	(async function bootstrap() {
		try {
			const boot = await window.ibioMenu.bootstrap();
			st.data = boot;
			renderBrands(); // 초기 브랜드 패널
			updateProductListLinkState(); // 처음에는 비활성
			// (PC 메가 메뉴는 사용자가 열 때 동적 로딩/렌더링)
		} catch (e) {
			console.error("메뉴 부트스트랩 실패", e);
			renderBrands();
			updateProductListLinkState();
		}
	})();
})();
