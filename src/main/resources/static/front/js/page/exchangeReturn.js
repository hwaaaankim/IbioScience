(function() {
	const $ = (s, p = document) => p.querySelector(s);
	const $$ = (s, p = document) => Array.from(p.querySelectorAll(s));

	// ===== 샘플 주문 데이터 =====
	// 주문: id, date(YYYY-MM-DD), status, available(가능여부), items[{name, option, qty, price}]
	const ORDERS = [
		{
			id: 'ORD-250701-001', date: '2025-07-01', status: '배송완료', available: '가능',
			items: [
				{ name: 'Conical Tube 15 mL', option: 'Rack 50', qty: 2, price: 18000 },
				{ name: 'Pipette Tips 200 μL', option: 'Filtered', qty: 3, price: 9000 },
			],
		},
		{
			id: 'ORD-250709-112', date: '2025-07-09', status: '상품준비중', available: '불가',
			items: [
				{ name: 'PCR Master Mix 2X', option: '5 mL', qty: 1, price: 27000 },
				{ name: 'DNA Extraction Kit', option: '50 tests', qty: 1, price: 56000 },
				{ name: 'Protein Ladder', option: '10–200 kDa', qty: 2, price: 12000 },
			],
		},
		{
			id: 'ORD-250718-007', date: '2025-07-18', status: '배송중', available: '가능',
			items: [
				{ name: 'Benchtop Centrifuge', option: '12-slot', qty: 1, price: 350000 },
				{ name: 'Universal Tips 1000 μL', option: 'Bag 500', qty: 1, price: 14000 },
			],
		},
		{
			id: 'ORD-250801-030', date: '2025-08-01', status: '결제완료', available: '가능',
			items: [
				{ name: 'Conical Tube 50 mL', option: 'Bag 25', qty: 4, price: 21000 },
				{ name: 'Filtered Tips 10 μL', option: 'Low-retention', qty: 2, price: 16000 },
			],
		},
		{
			id: 'ORD-250812-054', date: '2025-08-12', status: '주문완료', available: '가능',
			items: [
				{ name: 'DNA Extraction Kit', option: '50 tests', qty: 1, price: 56000 },
				{ name: 'Protein Purification Kit', option: '10 preps', qty: 1, price: 92000 },
			],
		},
		// 필요 시 더 추가…
	];

	// ===== 필터 기본값: 최근 한 달 =====
	const startI = $('#exchangeReturn-page-start');
	const endI = $('#exchangeReturn-page-end');
	const today = new Date();
	const prev30 = new Date(today.getTime() - 29 * 24 * 3600 * 1000);

	startI.value = fmt(prev30);
	endI.value = fmt(today);

	function fmt(d) {
		const y = d.getFullYear(), m = String(d.getMonth() + 1).padStart(2, '0'), da = String(d.getDate()).padStart(2, '0');
		return `${y}-${m}-${da}`;
	}

	// ===== 리스트 렌더링 =====
	const tbody = $('#exchangeReturn-page-tbody');
	const checkAll = $('#exchangeReturn-page-checkAll');

	function render(orders) {
		tbody.innerHTML = '';
		checkAll.checked = false; checkAll.indeterminate = false;

		orders.forEach(o => {
			const tr = document.createElement('tr');

			// 체크박스
			const tdChk = document.createElement('td');
			const chk = document.createElement('input');
			chk.type = 'checkbox'; chk.className = 'exchangeReturn-page-check'; chk.dataset.id = o.id;
			tdChk.appendChild(chk);
			tr.appendChild(tdChk);

			// 오더내용
			const tdInfo = document.createElement('td');
			tdInfo.className = 'text-start';
			const box = document.createElement('div'); box.className = 'exchangeReturn-page-orderbox';

			// 첫 줄: 주문번호 / 주문일
			const head = document.createElement('div');
			head.className = 'exchangeReturn-page-item';
			head.innerHTML = `<strong>${o.id}</strong> <span class="text-muted ms-2">${o.date}</span>`;
			box.appendChild(head);

			// 아이템들
			o.items.forEach(it => {
				const row = document.createElement('div');
				row.className = 'exchangeReturn-page-item';
				const nm = document.createElement('span'); nm.className = 'exchangeReturn-page-item-name'; nm.textContent = it.name + (it.option ? ` (${it.option})` : '');
				const qty = document.createElement('span'); qty.className = 'exchangeReturn-page-item-qty ms-2'; qty.textContent = `x${it.qty}`;
				row.appendChild(nm); row.appendChild(qty);
				box.appendChild(row);
			});
			tdInfo.appendChild(box);
			tr.appendChild(tdInfo);

			// 전체가격
			const tdPrice = document.createElement('td');
			const total = o.items.reduce((s, it) => s + it.price * it.qty, 0);
			tdPrice.textContent = total.toLocaleString();
			tr.appendChild(tdPrice);

			// 상태
			const tdStatus = document.createElement('td');
			tdStatus.textContent = o.status;
			tr.appendChild(tdStatus);

			// 가능여부
			const tdAvail = document.createElement('td');
			tdAvail.textContent = o.available;
			tr.appendChild(tdAvail);

			tbody.appendChild(tr);
		});
	}

	// 초기 전체 렌더
	render(ORDERS);

	// ===== 검색 =====
	$('#exchangeReturn-page-search').addEventListener('click', () => {
		const s = new Date(startI.value);
		const e = new Date(endI.value);
		// 종료일 23:59:59 포함
		e.setHours(23, 59, 59, 999);

		const list = ORDERS.filter(o => {
			const d = new Date(o.date);
			return d >= s && d <= e;
		});
		render(list);
	});

	// ===== 전체 선택 / 개별 선택 =====
	checkAll.addEventListener('change', () => {
		$$('.exchangeReturn-page-check', tbody).forEach(c => c.checked = checkAll.checked);
	});
	tbody.addEventListener('change', (e) => {
		if (!e.target.classList.contains('exchangeReturn-page-check')) return;
		const all = $$('.exchangeReturn-page-check', tbody);
		const checked = all.filter(c => c.checked).length;
		checkAll.checked = checked === all.length && all.length > 0;
		checkAll.indeterminate = checked > 0 && checked < all.length;
	});

	// ===== 파일 업로드 =====
	const fileInput = $('#exchangeReturn-page-file');
	const fileList = $('#exchangeReturn-page-fileList');
	const clearBtn = $('#exchangeReturn-page-clear');

	/** @type {File[]} */
	let files = [];

	fileInput.addEventListener('change', (e) => {
		const sel = Array.from(e.target.files || []);
		if (!sel.length) return;
		files.push(...sel);          // 누적
		fileInput.value = '';        // 같은 파일 다시 선택 가능
		renderFiles();
	});

	clearBtn.addEventListener('click', () => {
		if (!files.length) return;
		if (!confirm('등록된 모든 파일을 삭제하시겠습니까?')) return;
		files = [];
		renderFiles();
	});

	function renderFiles() {
		fileList.innerHTML = '';
		files.forEach((f, idx) => {
			const isImg = /^image\//.test(f.type);
			if (isImg) {
				const box = document.createElement('div');
				box.className = 'exchangeReturn-page-thumb';
				const img = document.createElement('img');
				const reader = new FileReader();
				reader.onload = ev => img.src = ev.target.result;
				reader.readAsDataURL(f);
				const del = document.createElement('button');
				del.type = 'button'; del.className = 'exchangeReturn-page-remove'; del.textContent = '×';
				del.addEventListener('click', () => removeAt(idx));
				box.appendChild(img); box.appendChild(del);
				fileList.appendChild(box);
			} else {
				const chip = document.createElement('div');
				chip.className = 'exchangeReturn-page-fileChip';
				const name = document.createElement('div');
				name.className = 'exchangeReturn-page-fileChip-name';
				name.textContent = `${getExt(f.name)} · ${f.name}`;
				const del = document.createElement('button');
				del.type = 'button'; del.className = 'exchangeReturn-page-remove'; del.textContent = '×';
				del.addEventListener('click', () => removeAt(idx));
				chip.appendChild(name); chip.appendChild(del);
				fileList.appendChild(chip);
			}
		});
	}

	function removeAt(i) {
		files.splice(i, 1);
		renderFiles();
	}

	function getExt(name) {
		const i = name.lastIndexOf('.');
		return i > 0 ? name.slice(i + 1).toUpperCase() : 'FILE';
	}
})();
