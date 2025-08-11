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

	let externalMappings = []; // /api/category/mapping/all (중-소 N:N)
	let currentPage = 1;

	// 날짜 빠른선택
	document.querySelectorAll('.product-list-date-btn').forEach(btn => {
		btn.addEventListener('click', () => {
			$form.dateQuick.value = btn.dataset.q;
			if (btn.dataset.q !== 'RANGE') {
				$form.dateFrom.value = '';
				$form.dateTo.value = '';
			}
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
			const data = await res.json();
			data.forEach(d => {
				// d: {id,name,mediumCount}
				$large.insertAdjacentHTML('beforeend',
					`<option value="${d.id}">${d.name} (${d.mediumCount})</option>`);
			});
		} else if ($mode.value === 'EXTERNAL') {
			// 외부 대분류 목록 + (중분류 수)
			const res = await fetch('/api/category/large');
			const larges = await res.json();
			const mediums = await fetch('/api/category/medium').then(r => r.json());
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
			const data = await res.json();
			data.forEach(d => {
				$medium.insertAdjacentHTML('beforeend',
					`<option value="${d.id}">${d.name} (${d.smallCount})</option>`);
			});
		} else if ($mode.value === 'EXTERNAL') {
			const res = await fetch(`/api/category/list-medium?largeId=${lid}`);
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
			const data = await res.json();
			data.forEach(d => {
				$small.insertAdjacentHTML('beforeend', `<option value="${d.id}">${d.name}</option>`);
			});
		} else if ($mode.value === 'EXTERNAL') {
			const res = await fetch(`/api/category/list-small?mediumId=${mid}`);
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
		// 외부분류에서 large/medium 선택만 한 경우, small까지 계산해서 전달(서버는 smallId만 신뢰)
		if ($mode.value === 'EXTERNAL' && !params.get('smallId')) {
			// medium 선택 시 그 중분류에 매핑된 small 중 첫번째를 auto 로 보내지 않고,
			// 서버가 smallId만 지원하므로, 사용자가 최종 소분류까지 선택하도록 UX 유지
			// (필요시 smallIds[] 다중 파라미터로 확장 가능)
		}

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
	
	    // promotionTypes는 서버에서 Set<PromotionType>일 수 있어 배열로 보장
	    const promoArr = Array.isArray(r.promotionTypes)
	      ? r.promotionTypes
	      : (r.promotionTypes ? [...r.promotionTypes] : []);
	    const promo = (promoArr && promoArr.length) ? promoArr.join(' / ') : '-';
	
	    // 리스트의 분류 표시는 항상 소비자용 외부분류(요약)
	    const categoryText = (r.externalCategorySummary ?? '-');
	
	    return `
	      <tr>
	        <td><input type="checkbox" data-id="${r.id}"></td>
	        <td>${r.id}</td>
	        <td>${r.internalProductCode ?? '-'}</td>
	        <td>${categoryText}</td>
	        <td>${img}</td>
	        <td><a th:href="@{/productDetail/${r.id}}" href="/productDetail/${r.id}" class="text-decoration-underline">${r.name}</a></td>
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

		for (let p = from; p <= to; p++) {
			items.push(li(p, p, false, p === now));
		}

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