(function() {
	const $ = (s, p = document) => p.querySelector(s);
	const $$ = (s, p = document) => Array.from(p.querySelectorAll(s));

	// ===== 샘플 데이터 =====
	// 제품: id, 분류(L/M/S), 브랜드, 이름, 이미지
	const PRODUCTS = [
		{ id: 1, L: '장비', M: '원심분리기', S: '벤chtop', brand: 'iBIO', name: 'iBIO Benchtop Centrifuge 12-slot', img: '/front/image/sample/100-100.png' },
		{ id: 2, L: '장비', M: '원심분리기', S: '고속형', brand: 'GENECO', name: 'GENECO High-speed Centrifuge HS20', img: '/front/image/sample/100-100.png' },
		{ id: 3, L: '장비', M: 'PCR 장비', S: '써모사이클러', brand: 'LABTEX', name: 'LABTEX Thermocycler TC-96', img: '/front/image/sample/100-100.png' },
		{ id: 4, L: '장비', M: 'PCR 장비', S: '리얼타임 PCR', brand: 'OTHERS', name: 'QRT Real-time PCR System', img: '/front/image/sample/100-100.png' },

		{ id: 5, L: '소모품', M: '튜브', S: '15ml', brand: 'iBIO', name: 'Conical Tube 15 mL (Rack 50)', img: '/front/image/sample/100-100.png' },
		{ id: 6, L: '소모품', M: '튜브', S: '50ml', brand: 'iBIO', name: 'Conical Tube 50 mL (Bag 25)', img: '/front/image/sample/100-100.png' },
		{ id: 7, L: '소모품', M: '팁', S: '필터팁', brand: 'GENECO', name: 'Filtered Pipette Tips 200 μL', img: '/front/image/sample/100-100.png' },
		{ id: 8, L: '소모품', M: '팁', S: '일반팁', brand: 'LABTEX', name: 'Universal Pipette Tips 1000 μL', img: '/front/image/sample/100-100.png' },

		{ id: 9, L: '시약', M: 'DNA', S: '추출키트', brand: 'GENECO', name: 'DNA Extraction Kit 50 tests', img: '/front/image/sample/100-100.png' },
		{ id: 10, L: '시약', M: 'DNA', S: 'PCR 마스터믹스', brand: 'iBIO', name: '2X PCR Master Mix 5 mL', img: '/front/image/sample/100-100.png' },
		{ id: 11, L: '시약', M: 'Protein', S: '정제키트', brand: 'LABTEX', name: 'Protein Purification Kit 10 preps', img: '/front/image/sample/100-100.png' },
		{ id: 12, L: '시약', M: 'Protein', S: 'Marker', brand: 'OTHERS', name: 'Protein Ladder 10–200 kDa', img: '/front/image/sample/100-100.png' },

		{ id: 13, L: '소모품', M: '튜브', S: '15ml', brand: 'LABTEX', name: 'Conical Tube 15 mL Sterile 500/CS', img: '/front/image/sample/100-100.png' },
		{ id: 14, L: '소모품', M: '팁', S: '필터팁', brand: 'OTHERS', name: 'Low-retention Filter Tips 10 μL', img: '/front/image/sample/100-100.png' },
	];

	// 브랜드 목록 도출
	const BRANDS = Array.from(new Set(PRODUCTS.map(p => p.brand))).sort();

	// 분류 트리(대→중→소) 생성
	const tree = {};
	PRODUCTS.forEach(p => {
		tree[p.L] ??= {};
		tree[p.L][p.M] ??= new Set();
		tree[p.L][p.M].add(p.S);
	});

	// ===== 엘리먼트 =====
	const selL = $('#estimate-page-catL');
	const selM = $('#estimate-page-catM');
	const selS = $('#estimate-page-catS');
	const selB = $('#estimate-page-brand');
	const nameI = $('#estimate-page-name');
	const btnSearch = $('#estimate-page-search');

	const tbody = $('#estimate-page-tbody');
	const checkAll = $('#estimate-page-checkAll');
	const applyBtn = $('#estimate-page-applyBtn');

	// ===== 셀렉트 초기화 =====
	function setOptions(select, list, placeholder) {
		select.innerHTML = '';
		const opt0 = document.createElement('option');
		opt0.value = ''; opt0.textContent = placeholder ?? '전체';
		select.appendChild(opt0);
		list.forEach(v => {
			const opt = document.createElement('option');
			opt.value = v; opt.textContent = v;
			select.appendChild(opt);
		});
	}

	setOptions(selL, Object.keys(tree), '대분류 전체');
	setOptions(selM, [], '중분류 전체');
	setOptions(selS, [], '소분류 전체');
	setOptions(selB, BRANDS, '브랜드 전체');

	// 대분류 변경 → 중/소 리셋
	selL.addEventListener('change', () => {
		const L = selL.value;
		if (!L) {
			setOptions(selM, [], '중분류 전체');
			setOptions(selS, [], '소분류 전체');
			return;
		}
		const mids = Object.keys(tree[L] || {});
		setOptions(selM, mids, '중분류 전체');
		setOptions(selS, [], '소분류 전체');
	});

	// 중분류 변경 → 소 리셋
	selM.addEventListener('change', () => {
		const L = selL.value, M = selM.value;
		const smalls = (L && M) ? Array.from(tree[L]?.[M] || []) : [];
		setOptions(selS, smalls, '소분류 전체');
	});

	// ===== 검색 =====
	btnSearch.addEventListener('click', () => {
		const L = selL.value.trim();
		const M = selM.value.trim();
		const S = selS.value.trim();
		const B = selB.value.trim();
		const kw = nameI.value.trim().toLowerCase();

		const results = PRODUCTS.filter(p => {
			if (L && p.L !== L) return false;
			if (M && p.M !== M) return false;
			if (S && p.S !== S) return false;
			if (B && p.brand !== B) return false;
			if (kw && !p.name.toLowerCase().includes(kw)) return false;
			return true;
		});

		renderList(results);
	});

	// 최초 렌더(전체)
	renderList(PRODUCTS);

	// ===== 리스트 렌더링 =====
	function renderList(list) {
		tbody.innerHTML = '';
		checkAll.checked = false;
		checkAll.indeterminate = false;
		updateApplyBtn();

		list.forEach(p => {
			const tr = document.createElement('tr');

			// 체크박스
			const tdChk = document.createElement('td');
			const chk = document.createElement('input');
			chk.type = 'checkbox'; chk.className = 'estimate-page-check'; chk.dataset.id = p.id;
			tdChk.appendChild(chk);
			tr.appendChild(tdChk);

			// 분류
			const tdCat = document.createElement('td');
			tdCat.textContent = `${p.L} > ${p.M} > ${p.S}`;
			tr.appendChild(tdCat);

			// 브랜드
			const tdBrand = document.createElement('td');
			tdBrand.textContent = p.brand;
			tr.appendChild(tdBrand);

			// 제품 (이미지 + 이름)
			const tdProd = document.createElement('td');
			tdProd.className = 'estimate-page-td-prod';
			const box = document.createElement('div'); box.className = 'estimate-page-prod';
			const img = document.createElement('img'); img.src = p.img; img.alt = p.name;
			const nm = document.createElement('span'); nm.className = 'estimate-page-prod-name'; nm.textContent = p.name;
			box.appendChild(img); box.appendChild(nm);
			tdProd.appendChild(box);
			tr.appendChild(tdProd);

			// 수량
			const tdQty = document.createElement('td');
			const qty = document.createElement('input');
			qty.type = 'number'; qty.min = '1'; qty.value = '1'; qty.className = 'estimate-page-qty';
			tdQty.appendChild(qty);
			tr.appendChild(tdQty);

			tbody.appendChild(tr);
		});
	}

	// ===== 전체선택/개별선택 =====
	checkAll.addEventListener('change', () => {
		$$('.estimate-page-check', tbody).forEach(c => c.checked = checkAll.checked);
		updateApplyBtn();
	});

	tbody.addEventListener('change', (e) => {
		if (!e.target.classList.contains('estimate-page-check')) return;
		const all = $$('.estimate-page-check', tbody);
		const checked = all.filter(c => c.checked).length;
		checkAll.checked = checked === all.length && all.length > 0;
		checkAll.indeterminate = checked > 0 && checked < all.length;
		updateApplyBtn();
	});

	function updateApplyBtn() {
		const any = $$('.estimate-page-check', tbody).some(c => c.checked);
		$('#estimate-page-applyBtn').disabled = !any;
	}

	// ===== 견적문의 버튼(데모) =====
	$('#estimate-page-applyBtn').addEventListener('click', () => {
		const rows = $$('.estimate-page-check', tbody).filter(c => c.checked)
			.map(c => {
				const tr = c.closest('tr');
				const name = tr.querySelector('.estimate-page-prod-name')?.textContent?.trim() ?? '';
				const qty = tr.querySelector('.estimate-page-qty')?.value ?? '1';
				const id = c.dataset.id;
				return { id, name, qty: Number(qty) };
			});

		if (!rows.length) return;
		console.log('[견적문의 요청]', rows);
		alert(`견적 문의 요청 (${rows.length}건) 이 콘솔에 출력되었습니다.`);
		// TODO: 실제 제출 로직으로 교체
	});
})();
