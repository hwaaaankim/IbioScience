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

	// FadeIn/Out 토글
	function openMega(e) {
		if (e) e.preventDefault();
		if (mega.getAttribute('aria-hidden') === 'false') { closeMega(); return; } // 요청 2: '전체카테고리'로 닫히기
		mega.setAttribute('aria-hidden', 'false');          // CSS 트랜지션으로 부드럽게
		st.largeId = st.mediumId = st.smallId = null; resetBrand();
		renderLarge();
		setEmpty(ulMedium, '중분류가 없습니다.');
		setEmpty(ulSmall, '소분류가 없습니다.');
		setEmpty(ulProd, '제품이 여기에 표시됩니다.');
		// 바깥 클릭으로 닫기
		setTimeout(() => document.addEventListener('click', outsideClose), 0);
	}
	function closeMega() {
		if (mega.getAttribute('aria-hidden') === 'true') return;
		mega.setAttribute('aria-hidden', 'true');           // CSS 트랜지션 -> fadeOut
		document.removeEventListener('click', outsideClose);
	}
	function outsideClose(ev) {
		if (!mega.contains(ev.target) && !openBtn.contains(ev.target)) closeMega();
	}
	openBtn?.addEventListener('click', openMega);
	closeBtn?.addEventListener('click', closeMega);

	// 요청 3: 스크롤 시 메가메뉴 자동 닫힘 (디바운스)
	let lastY = window.scrollY;
	let scrollTimer = null;
	window.addEventListener('scroll', () => {
		if (scrollTimer) return;
		scrollTimer = setTimeout(() => {
			const now = window.scrollY;
			if (Math.abs(now - lastY) > 5) closeMega();
			lastY = now;
			scrollTimer = null;
		}, 80);
	}, { passive: true });

	// ★ 빈 상태도 "한 행 높이"로 맞춤 (열 높이 일치)
	function setEmpty(list, text) {
		clear(list);
		list.classList.add('ibio-index-empty');
		list.innerHTML = `
      <li style="height: var(--ibio-item-h); display:flex; align-items:center; padding:0 12px; border-bottom:1px solid #f3f4f6; color:#9aa3ae;">
        ${text}
      </li>`;
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
				setEmpty(ulProd, '제품이 여기에 표시됩니다.'); setEmpty(ulSmall, '소분류가 없습니다.');
				resetBrand();
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

	// ===== 모바일 =====
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

	function lockBody() {
		bodyScrollTop = window.scrollY || document.documentElement.scrollTop;
		document.body.classList.add('ibio-index-lock');
		document.body.style.top = `-${bodyScrollTop}px`;
	}
	function unlockBody() {
		document.body.classList.remove('ibio-index-lock');
		document.body.style.top = '';
		window.scrollTo(0, bodyScrollTop);
	}

	// 요청 4: 좌->우로 슬라이드 인, 우->좌로 슬라이드 아웃
	function openMobile() {
		if (mOverlay.getAttribute('aria-hidden') === 'false') return;
		lockBody();
		renderMobileLarge();
		fab.disabled = true;

		mOverlay.setAttribute('aria-hidden', 'false');
		mPanel.dataset.state = 'showing'; // CSS 애니메이션 트리거
		// 애니메이션 종료 후 표시 상태 고정
		mPanel.addEventListener('animationend', function onEnd() {
			mPanel.dataset.state = 'visible';
			mPanel.removeEventListener('animationend', onEnd);
		});
	}
	function closeMobile() {
		if (mOverlay.getAttribute('aria-hidden') === 'true') return;
		mPanel.dataset.state = 'hiding';
		mPanel.addEventListener('animationend', function onEnd() {
			mPanel.dataset.state = 'hidden';
			mOverlay.setAttribute('aria-hidden', 'true');
			unlockBody();
			mPanel.removeEventListener('animationend', onEnd);
		});
	}

	mOpenBtn?.addEventListener('click', openMobile);
	mCloseBtn?.addEventListener('click', closeMobile);
	mOverlay?.addEventListener('click', (e) => { if (e.target === mOverlay) closeMobile(); });

	// 패널 내부 스크롤만 허용(바운스 방지)
	['touchmove', 'wheel'].forEach(ev => {
		mOverlay.addEventListener(ev, e => { if (!mPanel.contains(e.target)) e.preventDefault(); }, { passive: false });
	});

	// ===== 모바일 아코디언 (부드럽고, "틈" 없는 토글) =====
	// 핵심: 열릴 때 display:block; height:0 -> height:scrollHeight로 transition,
	// 닫힐 때 height:scrollHeight -> 0, 끝나면 display:none; open 클래스 제거
	function slideOpen(elem) {
		if (elem.dataset.state === 'open') return;
		elem.style.display = 'block';
		elem.style.overflow = 'hidden';
		elem.style.height = '0px';
		elem.classList.add('open');
		// 강제 리플로우
		elem.getBoundingClientRect();
		const target = elem.scrollHeight;
		elem.style.transition = 'height .22s cubic-bezier(.25,.1,.25,1)';
		elem.style.height = target + 'px';
		elem.addEventListener('transitionend', function onEnd(e) {
			if (e.propertyName !== 'height') return;
			elem.style.transition = '';
			elem.style.height = 'auto';
			elem.style.overflow = 'visible';
			elem.dataset.state = 'open';
			elem.removeEventListener('transitionend', onEnd);
		});
	}
	function slideClose(elem) {
		if (elem.dataset.state !== 'open' && elem.style.display === 'none') return;
		elem.style.overflow = 'hidden';
		const start = elem.scrollHeight;
		elem.style.height = start + 'px';
		// 강제 리플로우
		elem.getBoundingClientRect();
		elem.style.transition = 'height .22s cubic-bezier(.25,.1,.25,1)';
		elem.style.height = '0px';
		elem.addEventListener('transitionend', function onEnd(e) {
			if (e.propertyName !== 'height') return;
			elem.style.transition = '';
			elem.style.height = '0px';
			elem.style.display = 'none';
			elem.classList.remove('open');     // ★ 틈(갭) 원인 제거: open 유지 금지
			elem.dataset.state = 'closed';
			elem.removeEventListener('transitionend', onEnd);
		});
	}
	function slideToggle(elem, builderIfEmpty) {
		if (builderIfEmpty && elem.childElementCount === 0) builderIfEmpty();
		if (elem.dataset.state === 'open') slideClose(elem);
		else slideOpen(elem);
	}

	function renderMobileLarge() {
		clear(mLarge);
		st.data.large.forEach(L => {
			const li = el('li', 'ibio-index-m-li');
			const row = el('div', 'ibio-index-m-row', `<span>${L.name}</span><span class="ibio-index-m-arrow">›</span>`);
			const sub = el('div', 'ibio-index-m-sub'); // 중분류 래퍼
			sub.style.display = 'none';

			row.addEventListener('click', () => {
				slideToggle(sub, () => renderMobileMedium(sub, L.id));
			});

			li.append(row, sub); mLarge.appendChild(li);
		});
	}

	function renderMobileMedium(container, largeId) {
		const L = st.data.large.find(x => x.id === largeId); if (!L) return;
		clear(container);
		L.medium.forEach(M => {
			const li = el('div', 'ibio-index-m-li');
			const row = el('div', 'ibio-index-m-row', `<span>${M.name}</span><span class="ibio-index-m-arrow">›</span>`);
			const sub = el('div', 'ibio-index-m-sub'); // 소분류 래퍼
			sub.style.display = 'none';

			row.addEventListener('click', () => {
				slideToggle(sub, () => renderMobileSmall(sub, M.id));
			});
			li.append(row, sub); container.appendChild(li);
		});
	}

	function renderMobileSmall(container, mediumId) {
		const M = st.data.large.flatMap(l => l.medium).find(x => x.id === mediumId); if (!M) return;
		clear(container);
		M.small.forEach(S => {
			const li = el('div', 'ibio-index-m-li');
			const row = el('div', 'ibio-index-m-row', `<span>${S.name}</span>`);
			const sub = el('div', 'ibio-index-m-sub'); // 제품 래퍼
			sub.style.display = 'none';

			row.addEventListener('click', () => {
				fab.disabled = false; // 소분류 선택 → FAB 활성화
				if (sub.childElementCount === 0) {
					const list = applyBrandFilter(productsOfSmall(S.id));
					list.forEach(p => {
						const pd = el('div', 'ibio-index-m-prod', p.name + ` (BRAND ${String(p.brandId).padStart(2, '0')})`);
						sub.appendChild(pd);
					});
				}
				// 현재 선택 저장
				st.smallId = S.id;
				st.mediumId = M.id;
				st.largeId = Math.floor(M.id / 100);
				slideToggle(sub);
			});

			li.append(row, sub); container.appendChild(li);
		});
	}

	// 모바일 브랜드 모달
	function renderBrandModal() {
		const cur = st.brandId;
		clear(bModalBody);
		st.data.brands.forEach(b => {
			const btn = el('button', 'ibio-index-brand-btn' + (cur === b.id ? ' active' : ''), `
        <div class="ibio-index-brand-badge">150×30</div><span>${b.name}</span>
      `);
			btn.type = 'button';
			btn.addEventListener('click', () => {
				st.brandId = (st.brandId === b.id) ? null : b.id; // 토글
				renderBrandModal();
				if (st.smallId) renderMobileLarge();              // 간단 재구성
				bModal.setAttribute('aria-hidden', 'true');
			});
			bModalBody.appendChild(btn);
		});
	}

	byId('ibio-index-brand-fab')?.addEventListener('click', () => {
		if (fab.disabled) return;
		renderBrandModal();
		bModal.setAttribute('aria-hidden', 'false');
	});
	bModalClose?.addEventListener('click', () => bModal.setAttribute('aria-hidden', 'true'));
	bModal?.addEventListener('click', (e) => { if (e.target === bModal) bModal.setAttribute('aria-hidden', 'true'); });

})();