(function() {
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

	const $dateFrom = $form.querySelector('input[name="dateFrom"]');
	const $dateTo = $form.querySelector('input[name="dateTo"]');

	let externalMappings = []; // /api/category/mapping/all (중-소 N:N)
	let currentPage = 1;

	function formatYMD(d) {
		const y = d.getFullYear();
		const m = String(d.getMonth() + 1).padStart(2, '0');
		const day = String(d.getDate()).padStart(2, '0');
		return `${y}-${m}-${day}`;
	}

	// 오늘/7일/1개월(=30일) 버튼: from/to 모두 설정, 자동 조회는 하지 않음
	document.querySelectorAll('.product-list-date-btn').forEach(btn => {
		btn.addEventListener('click', () => {
			const q = btn.dataset.q;
			const today = new Date();
			const to = new Date(today); // 종료일 = 오늘
			const from = new Date(today);

			if (q === 'TODAY') {
				// from = today, to = today
				// 그대로 유지
			} else if (q === 'D7') {
				from.setDate(today.getDate() - 7); // 7일 전 ~ 오늘
			} else if (q === 'M1') {
				from.setDate(today.getDate() - 30); // 30일 전 ~ 오늘
			}

			$dateFrom.value = formatYMD(from);
			$dateTo.value = formatYMD(to);
		});
	});

	// 분류 모드 변경 시 초기화 + 대분류 로딩
	$mode.addEventListener('change', () => {
		loadLarge();
	});

	// 대/중/소 체인
	$large.addEventListener('change', () => loadMedium());
	$medium.addEventListener('change', () => loadSmall());

	// 페이지 크기 변경 시 1페이지부터 재조회
	$size.addEventListener('change', () => { currentPage = 1; fetchList(); });

	// 검색
	$form.addEventListener('submit', function(e) {
		e.preventDefault();
		currentPage = 1;
		fetchList();
	});

	// 초기화 버튼: 모든 검색조건 초기화
	$reset.addEventListener('click', () => {
		// form reset
		$form.reset();

		// 분류 드롭다운 초기화
		$large.innerHTML = '<option value="">전체</option>';
		$medium.innerHTML = '<option value="">전체</option>';
		$small.innerHTML = '<option value="">전체</option>';

		// 페이지 크기 기본값 유지(필요 시 강제 설정)
		$size.value = '10';

		// 테이블/요약 초기 메시지
		$tbody.innerHTML = `<tr><td colspan="10" class="text-center py-4">검색해 주세요.</td></tr>`;
		$summary.textContent = '';

		// 첫 로딩용 대분류 새로 로드
		loadLarge();
	});

	// 외부분류 맵핑 미리 로딩(대/중/소 카운트 계산용)
	async function preloadExternalMapping() {
		try {
			const res = await fetch('/api/category/mapping/all');
			if (res.ok) externalMappings = await res.json();
		} catch (e) { console.warn('mapping load failed', e); }
	}

	async function loadLarge() {
		$large.innerHTML = '<option value="">전체</option>';
		$medium.innerHTML = '<option value="">전체</option>';
		$small.innerHTML = '<option value="">전체</option>';

		if ($mode.value === 'INTERNAL') {
			const res = await fetch('/api/internal-category/list-large');
			if (!res.ok) return;
			const data = await res.json();
			data.forEach(d => {
				// d: {id,name,mediumCount}
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

		if (!lid) { return; }

		if ($mode.value === 'INTERNAL') {
			const res = await fetch(`/api/internal-category/list-medium?largeId=${lid}`);
			if (!res.ok) return;
			const data = await res.json();
			data.forEach(d => {
				$medium.insertAdjacentHTML('beforeend',
					`<option value="${d.id}">${d.name} (${d.smallCount})</option>`);
			});
		} else if ($mode.value === 'EXTERNAL') {
			const res = await fetch(`/api/category/list-medium?largeId=${lid}`);
			if (!res.ok) return;
			const data = await res.json();
			// 중분류별 소분류 수는 mapping 으로 계산
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
		if (!mid) { return; }

		if ($mode.value === 'INTERNAL') {
			const res = await fetch(`/api/internal-category/list-small?mediumId=${mid}`);
			if (!res.ok) return;
			const data = await res.json();
			data.forEach(d => {
				$small.insertAdjacentHTML('beforeend', `<option value="${d.id}">${d.name}</option>`);
			});
		} else if ($mode.value === 'EXTERNAL') {
			const res = await fetch(`/api/category/list-small?mediumId=${mid}`);
			if (!res.ok) return;
			const data = await res.json();
			data.forEach(d => {
				$small.insertAdjacentHTML('beforeend', `<option value="${d.id}">${d.name}</option>`);
			});
		}
	}

	// 리스트 조회
	async function fetchList(page) {
		if (page) currentPage = page;

		const params = new URLSearchParams(new FormData($form));
		params.set('page', currentPage);

		const res = await fetch('/api/product/list?' + params.toString());
		if (!res.ok) {
			$tbody.innerHTML = `<tr><td colspan="10" class="text-center py-4">조회 실패</td></tr>`;
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

			const categoryText = (r.externalCategorySummary ?? '-');

			return `
        <tr>
          <td><input type="checkbox" data-id="${r.id}"></td>
          <td>${r.id}</td>
          <td>${r.internalProductCode ?? '-'}</td>
          <td>${categoryText}</td>
          <td>${img}</td>
          <td><a th:href="@{/admin/productDetail/${r.id}}" href="/admin/productDetail/${r.id}" class="text-decoration-underline">${r.name}</a></td>
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
		const number = page.number || 0; // 0-base
		const now = number + 1;

		const items = [];
		function li(p, label, disabled = false, active = false) {
			return `<li class="page-item ${disabled ? 'disabled' : ''} ${active ? 'active' : ''}">
        <a class="page-link" href="#" data-page="${p}">${label}</a>
      </li>`;
		}
		items.push(li(1, '처음', now === 1));
		items.push(li(Math.max(1, now - 1), '이전', now === 1));

		const span = 5;
		let from = Math.max(1, now - 2);
		let to = Math.min(totalPages, from + span - 1);
		from = Math.max(1, Math.min(from, to - span + 1));

		for (let p = from; p <= to; p++) items.push(li(p, p, false, p === now));

		items.push(li(Math.min(totalPages, now + 1), '다음', now === totalPages));
		items.push(li(totalPages, '마지막', now === totalPages));

		$page.innerHTML = items.join('');
		$page.querySelectorAll('a.page-link').forEach(a => {
			a.addEventListener('click', (e) => {
				e.preventDefault();
				const p = parseInt(a.dataset.page, 10);
				if (!isNaN(p)) fetchList(p);
			});
		});
	}

	// 초기화
	(async function init() {
		await preloadExternalMapping();
		await loadLarge();
	})();
})();
