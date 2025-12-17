/* global window, document, fetch, URLSearchParams, FormData */

(function() {
	'use strict';

	const $form = document.getElementById('product-list-filter');
	const $tbody = document.getElementById('pl-tbody');
	const $page = document.getElementById('pl-pagination');
	const $summary = document.getElementById('pl-page-summary');
	const $size = document.getElementById('pl-size');
	const $mode = document.getElementById('pl-category-mode');
	const $large = document.getElementById('pl-large');
	const $medium = document.getElementById('pl-medium');
	const $small = document.getElementById('pl-small');
	const $reset = document.getElementById('pl-reset');
	const $colCategory = document.getElementById('pl-col-category');

	const $dateFrom = $form.querySelector('input[name="dateFrom"]');
	const $dateTo = $form.querySelector('input[name="dateTo"]');

	let externalMappings = []; // /api/category/mapping/all (중-소 N:N) - 드롭다운 카운트용
	let currentPage = 1;

	function formatYMD(d) {
		const y = d.getFullYear();
		const m = String(d.getMonth() + 1).padStart(2, '0');
		const day = String(d.getDate()).padStart(2, '0');
		return `${y}-${m}-${day}`;
	}

	function resetCategorySelects() {
		$large.innerHTML = '<option value="">전체</option>';
		$medium.innerHTML = '<option value="">전체</option>';
		$small.innerHTML = '<option value="">전체</option>';
	}

	function refreshCategoryHeader() {
		const mode = ($mode.value || '');
		if (mode === 'INTERNAL') $colCategory.textContent = '내부분류';
		else if (mode === 'EXTERNAL') $colCategory.textContent = '제품분류';
		else $colCategory.textContent = '분류';
	}

	// 오늘/7일/1개월 버튼: from/to만 설정 (자동조회 X)
	document.querySelectorAll('.product-list-date-btn').forEach(btn => {
		btn.addEventListener('click', () => {
			const q = btn.dataset.q;
			const today = new Date();
			const to = new Date(today);
			const from = new Date(today);

			if (q === 'D7') from.setDate(today.getDate() - 7);
			else if (q === 'M1') from.setDate(today.getDate() - 30);

			$dateFrom.value = formatYMD(from);
			$dateTo.value = formatYMD(to);
		});
	});

	// 분류 모드 변경: 체인 초기화 + 대분류 로딩
	$mode.addEventListener('change', () => {
		currentPage = 1;
		resetCategorySelects();
		refreshCategoryHeader();
		loadLarge();
	});

	$large.addEventListener('change', () => {
		currentPage = 1;
		loadMedium();
	});

	$medium.addEventListener('change', () => {
		currentPage = 1;
		loadSmall();
	});

	$size.addEventListener('change', () => {
		currentPage = 1;
		fetchList();
	});

	$form.addEventListener('submit', function(e) {
		e.preventDefault();
		currentPage = 1;
		fetchList();
	});

	$reset.addEventListener('click', () => {
		$form.reset();

		// 페이지 크기 기본값 유지
		$size.value = '10';

		resetCategorySelects();
		refreshCategoryHeader();

		$tbody.innerHTML = `<tr><td colspan="10" class="text-center py-4">검색해 주세요.</td></tr>`;
		$summary.textContent = '';
		currentPage = 1;

		loadLarge();
	});

	async function preloadExternalMapping() {
		try {
			const res = await fetch('/api/category/mapping/all');
			if (res.ok) externalMappings = await res.json();
		} catch (e) {
			console.warn('mapping load failed', e);
		}
	}

	async function loadLarge() {
		resetCategorySelects();

		if (!$mode.value) return;

		if ($mode.value === 'INTERNAL') {
			const res = await fetch('/api/internal-category/list-large');
			if (!res.ok) return;
			const data = await res.json();
			data.forEach(d => {
				$large.insertAdjacentHTML('beforeend',
					`<option value="${d.id}">${d.name} (${d.mediumCount})</option>`);
			});
		} else if ($mode.value === 'EXTERNAL') {
			const res = await fetch('/api/category/large');
			if (!res.ok) return;
			const larges = await res.json();
			const mediums = await fetch('/api/category/medium').then(r => r.ok ? r.json() : []);
			larges.forEach(L => {
				const cnt = mediums.filter(m => m.largeId === L.id || (m.large && m.large.id === L.id)).length;
				$large.insertAdjacentHTML('beforeend',
					`<option value="${L.id}">${L.name} (${cnt})</option>`);
			});
		}
	}

	async function loadMedium() {
		$medium.innerHTML = '<option value="">전체</option>';
		$small.innerHTML = '<option value="">전체</option>';

		const lid = $large.value;
		if (!lid) return;
		if (!$mode.value) return;

		if ($mode.value === 'INTERNAL') {
			const res = await fetch(`/api/internal-category/list-medium?largeId=${encodeURIComponent(lid)}`);
			if (!res.ok) return;
			const data = await res.json();
			data.forEach(d => {
				$medium.insertAdjacentHTML('beforeend',
					`<option value="${d.id}">${d.name} (${d.smallCount})</option>`);
			});
		} else if ($mode.value === 'EXTERNAL') {
			const res = await fetch(`/api/category/list-medium?largeId=${encodeURIComponent(lid)}`);
			if (!res.ok) return;
			const data = await res.json();
			data.forEach(m => {
				const smallCnt = externalMappings.filter(ms => ms.mediumId === m.id).length;
				$medium.insertAdjacentHTML('beforeend',
					`<option value="${m.id}">${m.name} (${smallCnt})</option>`);
			});
		}
	}

	async function loadSmall() {
		$small.innerHTML = '<option value="">전체</option>';

		const mid = $medium.value;
		if (!mid) return;
		if (!$mode.value) return;

		if ($mode.value === 'INTERNAL') {
			const res = await fetch(`/api/internal-category/list-small?mediumId=${encodeURIComponent(mid)}`);
			if (!res.ok) return;
			const data = await res.json();
			data.forEach(d => {
				$small.insertAdjacentHTML('beforeend', `<option value="${d.id}">${d.name}</option>`);
			});
		} else if ($mode.value === 'EXTERNAL') {
			const res = await fetch(`/api/category/list-small?mediumId=${encodeURIComponent(mid)}`);
			if (!res.ok) return;
			const data = await res.json();
			data.forEach(d => {
				$small.insertAdjacentHTML('beforeend', `<option value="${d.id}">${d.name}</option>`);
			});
		}
	}

	async function fetchList(page) {
		if (page) currentPage = page;

		const params = new URLSearchParams(new FormData($form));
		params.set('page', String(currentPage));

		let res;
		try {
			res = await fetch('/api/product/list?' + params.toString());
		} catch (e) {
			console.error(e);
			$tbody.innerHTML = `<tr><td colspan="10" class="text-center py-4">네트워크 오류</td></tr>`;
			return;
		}

		if (!res.ok) {
			const text = await res.text().catch(() => '');
			console.error('API failed:', res.status, res.statusText, text);
			$tbody.innerHTML = `<tr><td colspan="10" class="text-center py-4">조회 실패 (${res.status})</td></tr>`;
			return;
		}

		const ct = (res.headers.get('content-type') || '').toLowerCase();
		if (!ct.includes('application/json')) {
			const text = await res.text().catch(() => '');
			console.error('Not JSON response:', ct, text);
			$tbody.innerHTML = `<tr><td colspan="10" class="text-center py-4">서버 응답 형식 오류(JSON 아님)</td></tr>`;
			return;
		}

		const data = await res.json();
		renderTable(data);
		renderPagination(data);
	}

	function fmt(n) {
		if (n === null || n === undefined) return '-';
		return n.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
	}

	function renderTable(page) {
		const list = page.content || [];
		if (list.length === 0) {
			$tbody.innerHTML = `<tr><td colspan="10" class="text-center py-4">결과가 없습니다.</td></tr>`;
			$summary.textContent = '0건';
			return;
		}

		$tbody.innerHTML = list.map(r => {
			const img = r.imageUrl
				? `<img src="${r.imageUrl}" alt="" style="width:40px;height:40px;object-fit:cover;border-radius:8px;">`
				: '-';

			const dealer = r.dealerPrices
				? Object.entries(r.dealerPrices).map(([g, price]) => `${g} ${fmt(price)}`).join(' / ')
				: '-';

			const promoArr = Array.isArray(r.promotionTypes)
				? r.promotionTypes
				: (r.promotionTypes ? [...r.promotionTypes] : []);
			const promo = (promoArr && promoArr.length) ? promoArr.join(' / ') : '-';

			const categoryText = (r.categorySummary ?? '-');

			return `
				<tr>
					<td><input type="checkbox" data-id="${r.id}"></td>
					<td>${r.id}</td>
					<td>${r.internalProductCode ?? '-'}</td>
					<td>${categoryText}</td>
					<td>${img}</td>
					<td><a href="/admin/productDetail/${r.id}" class="text-decoration-underline">${r.name ?? '-'}</a></td>
					<td class="text-end">${fmt(r.consumerPrice)}</td>
					<td class="text-end">${fmt(r.salePrice)}</td>
					<td>${dealer}</td>
					<td>${promo}</td>
				</tr>`;
		}).join('');

		const start = page.number * page.size + 1;
		const end = start + list.length - 1;
		$summary.textContent = `${fmt(page.totalElements)}건 중 ${fmt(start)}–${fmt(end)}`;
	}

	function renderPagination(page) {
		const totalPages = page.totalPages || 1;
		const number = page.number || 0;
		const now = number + 1;

		const items = [];
		function li(p, label, disabled, active) {
			return `<li class="page-item ${disabled ? 'disabled' : ''} ${active ? 'active' : ''}">
				<a class="page-link" href="#" data-page="${p}">${label}</a>
			</li>`;
		}

		items.push(li(1, '처음', now === 1, false));
		items.push(li(Math.max(1, now - 1), '이전', now === 1, false));

		const span = 5;
		let from = Math.max(1, now - 2);
		let to = Math.min(totalPages, from + span - 1);
		from = Math.max(1, Math.min(from, to - span + 1));

		for (let p = from; p <= to; p++) items.push(li(p, p, false, p === now));

		items.push(li(Math.min(totalPages, now + 1), '다음', now === totalPages, false));
		items.push(li(totalPages, '마지막', now === totalPages, false));

		$page.innerHTML = items.join('');
		$page.querySelectorAll('a.page-link').forEach(a => {
			a.addEventListener('click', (e) => {
				e.preventDefault();
				const p = parseInt(a.dataset.page, 10);
				if (!isNaN(p)) fetchList(p);
			});
		});
	}

	(async function init() {
		await preloadExternalMapping();
		refreshCategoryHeader();
		await loadLarge();
	})();
})();
