
/* eslint-disable */
(function() {
	// ===== 상태 =====
	const st = {
		data: null,
		brandId: null,
		largeId: null,
		mediumId: null,
		smallId: null,
		brandEnabled: false
	};

	// ===== 더미 데이터 (실서버 연동 전까지) =====
	function buildData() {
		const brands = Array.from({ length: 10 }, (_, i) => ({ id: i + 1, name: `BRAND ${String(i + 1).padStart(2, '0')}` }));
		const large = Array.from({ length: 10 }, (_, L) => {
			const lid = L + 1;
			const medium = Array.from({ length: 10 }, (_, M) => {
				const mid = lid * 100 + (M + 1);
				const small = Array.from({ length: 10 }, (_, S) => {
					const sid = mid * 100 + (S + 1);
					const products = Array.from({ length: 10 }, (_, P) => {
						const bid = Math.floor(Math.random() * 10) + 1;
						return {
							id: sid * 100 + (P + 1),
							name: `대분류 ${String(lid).padStart(2, '0')} - 중${String(M + 1).padStart(2, '0')} - 소${String(S + 1).padStart(2, '0')} - 제품${String(P + 1).padStart(2, '0')}`,
							brandId: bid
						};
					});
					return { id: sid, name: `대분류 ${String(lid).padStart(2, '0')} - 중${String(M + 1).padStart(2, '0')} - 소${String(S + 1).padStart(2, '0')}`, products };
				});
				return { id: mid, name: `대분류 ${String(lid).padStart(2, '0')} - 중${String(M + 1).padStart(2, '0')}`, small };
			});
			return { id: lid, name: `대분류 ${String(lid).padStart(2, '0')}`, medium };
		});
		return { brands, large };
	}
	st.data = buildData();

	// ===== 유틸 =====
	const $ = (s, r = document) => r.querySelector(s);
	const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));
	const byId = id => document.getElementById(id);
	const el = (t, c, h) => { const n = document.createElement(t); if (c) n.className = c; if (h != null) n.innerHTML = h; return n; };
	const clear = n => { if (n) n.innerHTML = ''; };

	function flatProductsOfLarge(id) { const L = st.data.large.find(x => x.id === id); return L ? L.medium.flatMap(m => m.small).flatMap(s => s.products) : []; }
	function flatProductsOfMedium(id) { const M = st.data.large.flatMap(l => l.medium).find(x => x.id === id); return M ? M.small.flatMap(s => s.products) : []; }
	function productsOfSmall(id) { const S = st.data.large.flatMap(l => l.medium).flatMap(m => m.small).find(x => x.id === id); return S ? S.products.slice() : []; }
	const applyBrandFilter = list => (st.brandId ? list.filter(p => p.brandId === st.brandId) : list);

	function enableBrand(on) {
		st.brandEnabled = on;
		const box = byId('ibio-index-brand-list');
		if (on) box.classList.remove('ibio-index-disabled'); else box.classList.add('ibio-index-disabled');
	}
	function resetBrand() {
		st.brandId = null; enableBrand(false); renderBrands();
	}

	// ===== PC/Tablet 메가메뉴 =====
	const mega = byId('ibio-index-mega');
	const openBtn = byId('ibio-index-allcat-trigger');
	const closeBtn = byId('ibio-index-mega-close');
	const ulLarge = byId('ibio-index-large-list');
	const ulMedium = byId('ibio-index-medium-list');
	const ulSmall = byId('ibio-index-small-list');
	const ulProd = byId('ibio-index-product-list');
	const brandList = byId('ibio-index-brand-list');

	function openMega(e) {
		if (e) e.preventDefault();
		if (window.matchMedia('(max-width: 991.98px)').matches) return;
		if (mega.getAttribute('aria-hidden') === 'false') { closeMega(); return; }
		mega.setAttribute('aria-hidden', 'false');
		st.largeId = st.mediumId = st.smallId = null; resetBrand();
		renderLarge();
		setEmpty(ulMedium, '중분류가 없습니다.');
		setEmpty(ulSmall, '소분류가 없습니다.');
		setEmpty(ulProd, '제품이 여기에 표시됩니다.');
		// ▼▼▼ 추가: 메가메뉴 위 페이지 스크롤 잠금
		enableScrollLockOverMega(true);
		setTimeout(() => document.addEventListener('click', outsideClose), 0);
	}

	function closeMega() {
		if (mega.getAttribute('aria-hidden') === 'true') return;
		mega.setAttribute('aria-hidden', 'true');
		// ▼▼▼ 추가: 잠금 해제
		enableScrollLockOverMega(false);
		document.removeEventListener('click', outsideClose);
	}

	function outsideClose(ev) {
		if (!mega.contains(ev.target) && !openBtn.contains(ev.target)) closeMega();
	}
	openBtn?.addEventListener('click', openMega);
	closeBtn?.addEventListener('click', closeMega);

	// 스크롤 시 메가메뉴 자동 닫힘
	/* ===== 스크롤 시 메가메뉴 자동 닫힘: 메뉴 밖에서만 닫힘 ===== */
	// 보호 영역(메뉴 위)을 마우스/터치가 지나고 있는지 추적
	const guardZones = [
		document.querySelector('.main-menu-w'), // 상단 메뉴 바 래퍼
		document.getElementById('ibio-index-mega') // 실제 펼쳐진 메가메뉴
	];

	let isPointerOverMenu = false;

	// 마우스 진입/이탈
	guardZones.forEach(z => {
		if (!z) return;
		z.addEventListener('mouseenter', () => { isPointerOverMenu = true; });
		z.addEventListener('mouseleave', () => { isPointerOverMenu = false; });
	});

	// 터치(모바일/트랙패드) – 터치 시작 지점이 보호영역이면 스크롤 중에도 보호
	function isInGuard(target) {
		return guardZones.some(z => z && z.contains(target));
	}
	let touchGuard = false;
	document.addEventListener('touchstart', (e) => { touchGuard = isInGuard(e.target); }, { passive: true });
	document.addEventListener('touchend', () => { touchGuard = false; }, { passive: true });
	document.addEventListener('touchcancel', () => { touchGuard = false; }, { passive: true });

	// 디바운스 스크롤 핸들러
	let lastY = window.scrollY;
	let scrollTimer = null;
	window.addEventListener('scroll', () => {
		if (scrollTimer) return;
		scrollTimer = setTimeout(() => {
			const now = window.scrollY;
			const moved = Math.abs(now - lastY) > 5;

			// 992 미만은 PC 메가메뉴를 사용하지 않으므로 무시
			const isMobile = window.matchMedia('(max-width: 991.98px)').matches;

			// 메뉴가 열려 있고, 실제로 스크롤 되었고, 모바일이 아니며,
			// 포인터/터치가 "메뉴 위"가 아닐 때만 닫기
			if (
				mega.getAttribute('aria-hidden') === 'false' &&
				moved && !isMobile &&
				!isPointerOverMenu && !touchGuard
			) {
				closeMega();
			}

			lastY = now;
			scrollTimer = null;
		}, 80);
	}, { passive: true });

	// 빈 상태도 동일 높이
	function setEmpty(list, text) {
		clear(list);
		list.classList.add('ibio-index-empty');
		list.innerHTML = `<li style="height:var(--ibio-item-h);display:flex;align-items:center;padding:0 12px;border-bottom:1px solid #f3f4f6;color:#9aa3ae;">${text}</li>`;
	}

	function renderLarge() {
		clear(ulLarge);
		st.data.large.forEach(L => {
			const li = el('li', null);
			const txt = el('span', 'ibio-index-text', L.name);
			const arr = el('span', 'ibio-index-arrow', '›');

			txt.addEventListener('click', () => {
				st.largeId = L.id; st.mediumId = st.smallId = null;
				ulLarge.querySelectorAll('li').forEach(x => x.classList.remove('active')); li.classList.add('active');
				enableBrand(true);
				renderProducts(applyBrandFilter(flatProductsOfLarge(L.id)), `${L.name} 제품`);
				setEmpty(ulMedium, '중분류가 없습니다.'); setEmpty(ulSmall, '소분류가 없습니다.');
			});
			arr.addEventListener('click', (e) => {
				e.stopPropagation();
				st.largeId = L.id; st.mediumId = st.smallId = null;
				ulLarge.querySelectorAll('li').forEach(x => x.classList.remove('active')); li.classList.add('active');
				renderMedium(L.id);
				setEmpty(ulProd, '제품이 여기에 표시됩니다.'); setEmpty(ulSmall, '소분류가 없습니다.'); resetBrand();
			});
			li.append(txt, arr); ulLarge.appendChild(li);
		});
	}
	function renderMedium(largeId) {
		clear(ulMedium); ulMedium.classList.remove('ibio-index-empty');
		const L = st.data.large.find(x => x.id === largeId); if (!L) return;
		L.medium.forEach(M => {
			const li = el('li', null);
			const txt = el('span', 'ibio-index-text', M.name);
			const arr = el('span', 'ibio-index-arrow', '›');
			txt.addEventListener('click', () => {
				st.mediumId = M.id; st.smallId = null;
				ulMedium.querySelectorAll('li').forEach(x => x.classList.remove('active')); li.classList.add('active');
				enableBrand(true);
				renderProducts(applyBrandFilter(flatProductsOfMedium(M.id)), `${M.name} 제품`);
				setEmpty(ulSmall, '소분류가 없습니다.');
			});
			arr.addEventListener('click', (e) => {
				e.stopPropagation();
				st.mediumId = M.id; st.smallId = null;
				ulMedium.querySelectorAll('li').forEach(x => x.classList.remove('active')); li.classList.add('active');
				renderSmall(M.id); setEmpty(ulProd, '제품이 여기에 표시됩니다.'); resetBrand();
			});
			li.append(txt, arr); ulMedium.appendChild(li);
		});
	}
	function renderSmall(mediumId) {
		clear(ulSmall); ulSmall.classList.remove('ibio-index-empty');
		const M = st.data.large.flatMap(l => l.medium).find(x => x.id === mediumId); if (!M) return;
		M.small.forEach(S => {
			const li = el('li', null);
			const txt = el('span', 'ibio-index-text', S.name);
			const arr = el('span', 'ibio-index-arrow', '›');
			const show = () => {
				st.smallId = S.id;
				ulSmall.querySelectorAll('li').forEach(x => x.classList.remove('active')); li.classList.add('active');
				enableBrand(true);
				renderProducts(applyBrandFilter(productsOfSmall(S.id)), `${S.name} 제품`);
			};
			txt.addEventListener('click', show);
			arr.addEventListener('click', (e) => { e.stopPropagation(); show(); });
			li.append(txt, arr); ulSmall.appendChild(li);
		});
	}
	function renderProducts(list, title) {
		clear(ulProd); ulProd.classList.remove('ibio-index-empty');
		if (!list.length) { setEmpty(ulProd, `${title}이(가) 없습니다.`); return; }
		list.forEach(p => {
			const li = el('li', null, `<span class="ibio-index-text">${p.name}</span>`);
			ulProd.appendChild(li);
		});
	}
	// 브랜드 패널 (토글)
	function renderBrands() {
		clear(brandList);
		st.data.brands.forEach(b => {
			const item = el('div', 'ibio-index-brand-item' + (st.brandId === b.id ? ' active' : ''), `
	<div class="ibio-index-brand-badge">150×30</div><span>${b.name}</span>
	`);
			item.addEventListener('click', () => {
				if (!st.brandEnabled) return;
				st.brandId = (st.brandId === b.id) ? null : b.id;
				$$('.ibio-index-brand-item', brandList).forEach(x => x.classList.remove('active'));
				if (st.brandId === b.id) item.classList.add('active');
				refreshProductsByScope();
			});
			brandList.appendChild(item);
		});
	}
	renderBrands();
	function refreshProductsByScope() {
		let list = [];
		if (st.smallId) list = productsOfSmall(st.smallId);
		else if (st.mediumId) list = flatProductsOfMedium(st.mediumId);
		else if (st.largeId) list = flatProductsOfLarge(st.largeId);
		renderProducts(applyBrandFilter(list), '제품');
	}

	// ===== 모바일 패널 =====
	const mOverlay = byId('ibio-index-m-overlay');
	const mPanel = byId('ibio-index-m-panel');
	const mOpenBtn = byId('ibio-index-hamburger');
	const mCloseBtn = byId('ibio-index-m-close');
	const mLarge = byId('ibio-index-m-large');
	const fab = byId('ibio-index-brand-fab');
	const bModal = byId('ibio-index-brand-modal');
	const bModalClose = byId('ibio-index-brand-modal-close');
	const bModalBody = byId('ibio-index-brand-modal-body');
	let bodyScrollTop = 0;

	function lockBody() { bodyScrollTop = window.scrollY || document.documentElement.scrollTop; document.body.classList.add('ibio-index-lock'); document.body.style.top = `-${bodyScrollTop}px`; }
	function unlockBody() { document.body.classList.remove('ibio-index-lock'); document.body.style.top = ''; window.scrollTo(0, bodyScrollTop); }

	function openMobile() {
		if (mOverlay.getAttribute('aria-hidden') === 'false') return;
		lockBody();
		renderMobileLarge();
		fab.disabled = true;
		mOverlay.setAttribute('aria-hidden', 'false');
		mPanel.dataset.state = 'showing';
		mPanel.addEventListener('animationend', function onEnd() { mPanel.dataset.state = 'visible'; mPanel.removeEventListener('animationend', onEnd); });
	}
	function closeMobile() {
		if (mOverlay.getAttribute('aria-hidden') === 'true') return;
		mPanel.dataset.state = 'hiding';
		mPanel.addEventListener('animationend', function onEnd() {
			mPanel.dataset.state = 'hidden'; mOverlay.setAttribute('aria-hidden', 'true'); unlockBody(); mPanel.removeEventListener('animationend', onEnd);
		});
	}
	mOpenBtn?.addEventListener('click', openMobile);
	mCloseBtn?.addEventListener('click', closeMobile);
	mOverlay?.addEventListener('click', (e) => { if (e.target === mOverlay) closeMobile(); });
	['touchmove', 'wheel'].forEach(ev => { mOverlay.addEventListener(ev, e => { if (!mPanel.contains(e.target)) e.preventDefault(); }, { passive: false }); });

	// 모바일 아코디언
	function slideOpen(elem) {
		if (elem.dataset.state === 'open') return;
		elem.style.display = 'block'; elem.style.overflow = 'hidden'; elem.style.height = '0px'; elem.classList.add('open');
		elem.getBoundingClientRect();
		const target = elem.scrollHeight;
		elem.style.transition = 'height .22s cubic-bezier(.25,.1,.25,1)'; elem.style.height = target + 'px';
		elem.addEventListener('transitionend', function onEnd(e) { if (e.propertyName !== 'height') return; elem.style.transition = ''; elem.style.height = 'auto'; elem.style.overflow = 'visible'; elem.dataset.state = 'open'; elem.removeEventListener('transitionend', onEnd); });
	}
	function slideClose(elem) {
		if (elem.dataset.state !== 'open' && elem.style.display === 'none') return;
		elem.style.overflow = 'hidden'; const start = elem.scrollHeight; elem.style.height = start + 'px'; elem.getBoundingClientRect();
		elem.style.transition = 'height .22s cubic-bezier(.25,.1,.25,1)'; elem.style.height = '0px';
		elem.addEventListener('transitionend', function onEnd(e) { if (e.propertyName !== 'height') return; elem.style.transition = ''; elem.style.height = '0px'; elem.style.display = 'none'; elem.classList.remove('open'); elem.dataset.state = 'closed'; elem.removeEventListener('transitionend', onEnd); });
	}
	function slideToggle(elem, builderIfEmpty) { if (builderIfEmpty && elem.childElementCount === 0) builderIfEmpty(); if (elem.dataset.state === 'open') slideClose(elem); else slideOpen(elem); }

	function renderMobileLarge() {
		clear(mLarge);
		st.data.large.forEach(L => {
			const li = el('li', 'ibio-index-m-li');
			const row = el('div', 'ibio-index-m-row', `<span>${L.name}</span><span class="ibio-index-m-arrow">›</span>`);
			const sub = el('div', 'ibio-index-m-sub'); sub.style.display = 'none';
			row.addEventListener('click', () => { slideToggle(sub, () => renderMobileMedium(sub, L.id)); });
			li.append(row, sub); mLarge.appendChild(li);
		});
	}
	function renderMobileMedium(container, largeId) {
		const L = st.data.large.find(x => x.id === largeId); if (!L) return;
		clear(container);
		L.medium.forEach(M => {
			const li = el('div', 'ibio-index-m-li');
			const row = el('div', 'ibio-index-m-row', `<span>${M.name}</span><span class="ibio-index-m-arrow">›</span>`);
			const sub = el('div', 'ibio-index-m-sub'); sub.style.display = 'none';
			row.addEventListener('click', () => { slideToggle(sub, () => renderMobileSmall(sub, M.id)); });
			li.append(row, sub); container.appendChild(li);
		});
	}
	function renderMobileSmall(container, mediumId) {
		const M = st.data.large.flatMap(l => l.medium).find(x => x.id === mediumId); if (!M) return;
		clear(container);
		M.small.forEach(S => {
			const li = el('div', 'ibio-index-m-li');
			const row = el('div', 'ibio-index-m-row', `<span>${S.name}</span>`);
			const sub = el('div', 'ibio-index-m-sub'); sub.style.display = 'none';
			row.addEventListener('click', () => {
				fab.disabled = false;
				if (sub.childElementCount === 0) {
					const list = applyBrandFilter(productsOfSmall(S.id));
					list.forEach(p => { const pd = el('div', 'ibio-index-m-prod', p.name + ` (BRAND ${String(p.brandId).padStart(2, '0')})`); sub.appendChild(pd); });
				}
				st.smallId = S.id; st.mediumId = M.id; st.largeId = Math.floor(M.id / 100);
				slideToggle(sub);
			});
			li.append(row, sub); container.appendChild(li);
		});
	}
	// 모바일 브랜드 모달
	function renderBrandModal() {
		const cur = st.brandId; clear(bModalBody);
		st.data.brands.forEach(b => {
			const btn = el('button', 'ibio-index-brand-btn' + (cur === b.id ? ' active' : ''), `<div class="ibio-index-brand-badge">150×30</div><span>${b.name}</span>`);
			btn.type = 'button';
			btn.addEventListener('click', () => { st.brandId = (st.brandId === b.id) ? null : b.id; renderBrandModal(); if (st.smallId) renderMobileLarge(); bModal.setAttribute('aria-hidden', 'true'); });
			bModalBody.appendChild(btn);
		});
	}
	byId('ibio-index-brand-fab')?.addEventListener('click', () => { if (fab.disabled) return; renderBrandModal(); bModal.setAttribute('aria-hidden', 'true') === false; bModal.setAttribute('aria-hidden', 'false'); });
	bModalClose?.addEventListener('click', () => bModal.setAttribute('aria-hidden', 'true'));
	bModal?.addEventListener('click', (e) => { if (e.target === bModal) bModal.setAttribute('aria-hidden', 'true'); });

	/* =========================================================
	   반응형 전환: 992 미만 시 즉시 모바일 UX (메가메뉴 사용 금지)
	========================================================= */
	const mqMobile = window.matchMedia('(max-width: 991.98px)');
	function onBreakpointChange(e) {
		// PC→모바일 전환 시, 열린 메가메뉴 강제 닫기
		if (e.matches) { closeMega(); }
	}
	mqMobile.addEventListener('change', onBreakpointChange);
	onBreakpointChange(mqMobile); // 초기 1회 평가

	/* =========================================================
	   검색 UX (모바일 전체폭 모달 + PC 최근검색어 드롭다운)
	========================================================= */
	const RECENT_KEY = 'ibio-index-recent-search';
	function loadRecent() {
		try { return JSON.parse(localStorage.getItem(RECENT_KEY) || '[]'); } catch (_) { return []; }
	}
	function saveRecent(list) { localStorage.setItem(RECENT_KEY, JSON.stringify(list.slice(0, 10))); }
	function pushRecent(term) {
		const t = (term || '').trim(); if (!t) return;
		const list = loadRecent().filter(x => x !== t); list.unshift(t); saveRecent(list);
	}
	function renderRecentChips(container, chipClass) {
		const list = loadRecent();
		clear(container);
		if (!list.length) {
			container.appendChild(el('div', 'ibio-index-empty-recent', '최근 검색어가 없습니다.'));
			return;
		}
		list.forEach(q => {
			const chip = el('button', chipClass, `<i class="fa fa-clock-o"></i><span>${q}</span>`);
			chip.type = 'button';
			chip.addEventListener('click', () => {
				// 칩 클릭 → 해당 검색어로 즉시 검색 제출
				if (container.id === 'ibio-index-m-search-recent-list') {
					byId('ibio-index-m-search-input').value = q;
					byId('ibio-index-m-search-form').requestSubmit();
				} else {
					byId('ibio-index-pc-search-input').value = q;
					byId('ibio-index-pc-search-form').requestSubmit();
				}
			});
			container.appendChild(chip);
		});
	}

	/* --- 모바일: 돋보기 버튼 → 전체폭 모달 --- */
	const mSearchOverlay = byId('ibio-index-m-search-overlay');
	const mSearchOpenBtn = byId('ibio-index-mobile-search');
	const mSearchCloseBtn = byId('ibio-index-m-search-close');
	const mSearchForm = byId('ibio-index-m-search-form');
	const mSearchInput = byId('ibio-index-m-search-input');
	const mRecentWrap = byId('ibio-index-m-search-recent-list');
	const mClearBtn = byId('ibio-index-m-search-clear');

	function openMobileSearch() {
		mSearchOverlay.setAttribute('aria-hidden', 'false');
		renderRecentChips(mRecentWrap, 'ibio-index-m-search-chip');
		setTimeout(() => mSearchInput.focus(), 0);
	}
	function closeMobileSearch() {
		mSearchOverlay.setAttribute('aria-hidden', 'true');
	}
	mSearchOpenBtn?.addEventListener('click', openMobileSearch);
	mSearchCloseBtn?.addEventListener('click', closeMobileSearch);
	mSearchOverlay?.addEventListener('click', (e) => { if (e.target === mSearchOverlay) closeMobileSearch(); });
	mClearBtn?.addEventListener('click', () => { saveRecent([]); renderRecentChips(mRecentWrap, 'ibio-index-m-search-chip'); });

	mSearchForm?.addEventListener('submit', (e) => {
		e.preventDefault();
		const q = mSearchInput.value;
		pushRecent(q);
		// TODO: 실제 검색 엔드포인트 연결
		closeMobileSearch();
	});

	/* --- PC: 입력 클릭 → 최근검색어 드롭다운 --- */
	const pcInput = byId('ibio-index-pc-search-input');
	const pcForm = byId('ibio-index-pc-search-form');
	const dd = byId('ibio-index-search-dropdown');
	const ddList = byId('ibio-index-search-dd-list');
	const ddClear = byId('ibio-index-search-dd-clear');

	function openDropdown() {
		if (window.matchMedia('(max-width: 991.98px)').matches) return; // 모바일에서는 미사용
		renderRecentChips(ddList, 'ibio-index-search-dd-chip');
		dd.setAttribute('aria-hidden', 'false');
	}
	function closeDropdown() { dd.setAttribute('aria-hidden', 'true'); }
	function outsideCloseForDD(e) {
		if (!dd.contains(e.target) && e.target !== pcInput) closeDropdown();
	}
	pcInput?.addEventListener('focus', openDropdown);
	pcInput?.addEventListener('click', openDropdown);
	document.addEventListener('click', outsideCloseForDD);
	ddClear?.addEventListener('click', () => { saveRecent([]); renderRecentChips(ddList, 'ibio-index-search-dd-chip'); });

	pcForm?.addEventListener('submit', (e) => {
		// 실제 전송 전에 최근검색 저장
		pushRecent(pcInput.value);
		// TODO: 실제 검색 엔드포인트 연결
		closeDropdown();
	});

	// ESC로 드롭다운/모바일 검색 닫기
	document.addEventListener('keydown', (e) => {
		if (e.key === 'Escape') {
			closeDropdown();
			closeMobileSearch();
		}
	});
	/* ===== 메가메뉴 위에서는 페이지 스크롤 금지 (PC 전용) ===== */
	(function() {
		const root = document;
		const WHEEL_OPTS = { passive: false };
		let touchStartY = 0;
		let lockEnabled = false;

		// 대상 내에서 스크롤 가능한 가장 가까운 컨테이너 찾기
		function findScrollableAncestor(start, scopeRoot) {
			let el = start;
			while (el && el !== scopeRoot.parentElement) {
				if (el === scopeRoot) break;
				const st = getComputedStyle(el);
				const can = /(auto|scroll)/.test(st.overflowY) && (el.scrollHeight > el.clientHeight + 1);
				if (can) return el;
				el = el.parentElement;
			}
			return scopeRoot.querySelector('.ibio-index-col-list') || scopeRoot;
		}

		function clampScroll(el, deltaY) {
			const prev = el.scrollTop;
			el.scrollTop = prev + deltaY;
		}

		function onWheel(e) {
			if (!lockEnabled) return;
			if (window.matchMedia('(max-width: 991.98px)').matches) return;
			if (mega.getAttribute('aria-hidden') === 'true') return;
			if (!mega.contains(e.target)) return;

			const scroller = findScrollableAncestor(e.target, mega);
			clampScroll(scroller, e.deltaY);
			e.preventDefault();
		}

		function onTouchStart(e) {
			if (!mega.contains(e.target)) return;
			touchStartY = e.touches[0]?.clientY || 0;
		}

		function onTouchMove(e) {
			if (!lockEnabled) return;
			if (window.matchMedia('(max-width: 991.98px)').matches) return;
			if (mega.getAttribute('aria-hidden') === 'true') return;
			if (!mega.contains(e.target)) return;

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
			if (window.matchMedia('(max-width: 991.98px)').matches) return;
			if (mega.getAttribute('aria-hidden') === 'true') return;

			const focusEl = root.activeElement || root.body;
			if (!mega.contains(focusEl)) return;

			const keys = ['ArrowDown', 'ArrowUp', 'PageDown', 'PageUp', 'Home', 'End', ' '];
			if (!keys.includes(e.key)) return;

			const scroller = findScrollableAncestor(focusEl, mega);
			let delta = 0;
			if (e.key === 'ArrowDown') delta = KEY_STEP;
			else if (e.key === 'ArrowUp') delta = -KEY_STEP;
			else if (e.key === 'PageDown') delta = scroller.clientHeight - 20;
			else if (e.key === 'PageUp') delta = -(scroller.clientHeight - 20);
			else if (e.key === 'Home') scroller.scrollTop = 0;
			else if (e.key === 'End') scroller.scrollTop = scroller.scrollHeight;
			else if (e.key === ' ') delta = KEY_STEP * (e.shiftKey ? -1 : 1);

			if (delta !== 0 || ['Home', 'End'].includes(e.key)) {
				e.preventDefault();
				if (delta) clampScroll(scroller, delta);
			}
		}

		// 외부에서 켜고 끄는 진입점 - openMega/closeMega에서 호출함
		window.enableScrollLockOverMega = function(enable) {
			if (enable === lockEnabled) return;
			lockEnabled = !!enable;

			if (lockEnabled) {
				root.addEventListener('wheel', onWheel, WHEEL_OPTS);
				root.addEventListener('touchstart', onTouchStart, { passive: true });
				root.addEventListener('touchmove', onTouchMove, WHEEL_OPTS);
				root.addEventListener('keydown', onKeydown, false);
			} else {
				root.removeEventListener('wheel', onWheel, WHEEL_OPTS);
				root.removeEventListener('touchstart', onTouchStart, { passive: true });
				root.removeEventListener('touchmove', onTouchMove, WHEEL_OPTS);
				root.removeEventListener('keydown', onKeydown, false);
			}
		};
	})();

})();

