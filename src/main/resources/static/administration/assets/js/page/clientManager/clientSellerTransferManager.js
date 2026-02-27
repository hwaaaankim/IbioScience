(function() {

	const API_BASE = '/api/admin/root/clientTransfer';

	const state = {
		page: 0,
		size: 10,
		fromDate: '',
		toDate: '',
		searchType: '',
		keyword: '',
		sortKey: 'requestedAt',
		sortDir: 'desc',
	};

	// 모달 내 임시 상태(연락처/카테고리 권한)
	const modalState = {
		contacts: [], // {name, phone, email}
		categoryPermissions: [] // {key, largeId, mediumId, smallId, label}
	};

	function csrfHeaders() {
		const token = document.getElementById('client-seller-transfer-csrf-token')?.value || '';
		const header = document.getElementById('client-seller-transfer-csrf-header')?.value || '';
		if (!token || !header) return {};
		return { [header]: token };
	}

	function qs(params) {
		const sp = new URLSearchParams();
		Object.keys(params).forEach(k => {
			const v = params[k];
			if (v !== null && v !== undefined && String(v).trim() !== '') sp.append(k, v);
		});
		return sp.toString();
	}

	function setSortActiveButtons() {
		document.querySelectorAll('.client-seller-transfer-sort-btn').forEach(btn => {
			btn.classList.remove('active');
			if (btn.dataset.sort === state.sortKey) btn.classList.add('active');
		});
	}

	function readFilters() {
		state.size = parseInt(document.getElementById('client-seller-transfer-size').value || '10', 10);
		state.fromDate = document.getElementById('client-seller-transfer-from').value || '';
		state.toDate = document.getElementById('client-seller-transfer-to').value || '';
		state.searchType = document.getElementById('client-seller-transfer-search-type').value || '';
		state.keyword = document.getElementById('client-seller-transfer-keyword').value || '';
	}

	function resetFilters() {
		document.getElementById('client-seller-transfer-size').value = '10';
		document.getElementById('client-seller-transfer-from').value = '';
		document.getElementById('client-seller-transfer-to').value = '';
		document.getElementById('client-seller-transfer-search-type').value = '';
		document.getElementById('client-seller-transfer-keyword').value = '';
		state.page = 0;
		state.size = 10;
		state.fromDate = '';
		state.toDate = '';
		state.searchType = '';
		state.keyword = '';
		state.sortKey = 'requestedAt';
		state.sortDir = 'desc';
		setSortActiveButtons();
	}

	async function fetchList() {
		readFilters();

		const url = `${API_BASE}/seller-applications?` + qs({
			page: state.page,
			size: state.size,
			fromDate: state.fromDate,
			toDate: state.toDate,
			searchType: state.searchType,
			keyword: state.keyword,
			sortKey: state.sortKey,
			sortDir: state.sortDir
		});

		const res = await fetch(url, { method: 'GET' });
		const json = await res.json();
		if (!json || !json.success) {
			alert(json?.message || '조회 실패');
			return null;
		}
		return json.data;
	}

	function clearTbody() {
		document.getElementById('client-seller-transfer-tbody').innerHTML = '';
	}

	function renderEmpty() {
		document.getElementById('client-seller-transfer-tbody').innerHTML =
			`<tr><td colspan="7" class="text-center text-muted">조회된 데이터가 없습니다.</td></tr>`;
	}

	function escapeHtml(str) {
		if (str === null || str === undefined) return '';
		return String(str)
			.replaceAll('&', '&amp;')
			.replaceAll('<', '&lt;')
			.replaceAll('>', '&gt;')
			.replaceAll('"', '&quot;')
			.replaceAll("'", '&#039;');
	}

	function renderRows(pageData) {
		clearTbody();
		const tb = document.getElementById('client-seller-transfer-tbody');

		if (!pageData.content || pageData.content.length === 0) {
			renderEmpty();
			return;
		}

		const startNo = (pageData.page * pageData.size) + 1;

		pageData.content.forEach((row, idx) => {
			const tr = document.createElement('tr');
			tr.innerHTML = `
				<td>${startNo + idx}</td>
				<td>${escapeHtml(row.username)}</td>
				<td>${escapeHtml(row.companyName)}</td>
				<td>${escapeHtml(row.name)}</td>
				<td>${escapeHtml(row.mobile)}</td>
				<td>${escapeHtml(row.requestedAt)}</td>
				<td>
					<button type="button" class="btn btn-sm btn-outline-primary client-seller-transfer-approve-btn" data-id="${row.applicationId}">
						승인처리
					</button>
				</td>
			`;
			tb.appendChild(tr);
		});

		document.querySelectorAll('.client-seller-transfer-approve-btn').forEach(btn => {
			btn.addEventListener('click', () => openApproveModal(btn.dataset.id));
		});
	}

	function renderPagination(pageData) {
		const ul = document.getElementById('client-seller-transfer-pagination');
		ul.innerHTML = '';

		const totalPages = pageData.totalPages || 1;
		const current = pageData.page || 0;

		const makeItem = (label, targetPage, disabled, active) => {
			const li = document.createElement('li');
			li.className = 'page-item' + (disabled ? ' disabled' : '') + (active ? ' active' : '');
			const a = document.createElement('a');
			a.className = 'page-link';
			a.textContent = label;
			a.addEventListener('click', (e) => {
				e.preventDefault();
				if (disabled) return;
				state.page = targetPage;
				refresh();
			});
			li.appendChild(a);
			return li;
		};

		ul.appendChild(makeItem('FIRST', 0, current === 0, false));
		ul.appendChild(makeItem('PREV', Math.max(current - 1, 0), current === 0, false));

		const maxButtons = 5;
		let start = Math.max(current - 2, 0);
		let end = Math.min(start + maxButtons - 1, totalPages - 1);
		start = Math.max(end - (maxButtons - 1), 0);

		for (let p = start; p <= end; p++) {
			ul.appendChild(makeItem(String(p + 1), p, false, p === current));
		}

		ul.appendChild(makeItem('NEXT', Math.min(current + 1, totalPages - 1), current >= totalPages - 1, false));
		ul.appendChild(makeItem('LAST', totalPages - 1, current >= totalPages - 1, false));
	}

	async function refresh() {
		setSortActiveButtons();
		const pageData = await fetchList();
		if (!pageData) return;

		document.getElementById('client-seller-transfer-total-text').textContent =
			`총 ${pageData.totalElements}건 / ${pageData.totalPages}페이지`;

		renderRows(pageData);
		renderPagination(pageData);
	}

	/* =========================
	 * 모달: 로딩/초기화
	 * ========================= */

	function resetModalState() {
		modalState.contacts = [];
		modalState.categoryPermissions = [];

		document.getElementById('client-seller-transfer-form-shopName').value = '';
		document.getElementById('client-seller-transfer-form-productTypeText').value = '';
		document.getElementById('client-seller-transfer-form-tel').value = '';
		document.getElementById('client-seller-transfer-form-fax').value = '';
		document.getElementById('client-seller-transfer-form-homepageUrl').value = '';
		document.getElementById('client-seller-transfer-form-processNote').value = '';

		// ✅ 정산정책
		document.getElementById('client-seller-transfer-form-commissionRate').value = '';
		document.getElementById('client-seller-transfer-form-settlementCycle').value = '';
		document.getElementById('client-seller-transfer-form-settlementBasis').value = '';

		// ✅ 주소(daum으로만 세팅)
		document.getElementById('client-seller-transfer-form-biz-postcode').value = '';
		document.getElementById('client-seller-transfer-form-biz-road').value = '';
		document.getElementById('client-seller-transfer-form-biz-jibun').value = '';
		document.getElementById('client-seller-transfer-form-biz-detail').value = '';

		document.getElementById('client-seller-transfer-form-ret-postcode').value = '';
		document.getElementById('client-seller-transfer-form-ret-road').value = '';
		document.getElementById('client-seller-transfer-form-ret-jibun').value = '';
		document.getElementById('client-seller-transfer-form-ret-detail').value = '';

		renderContacts();
		renderCategoryChips();
	}

	async function loadEnums() {
		const res = await fetch(`${API_BASE}/seller-enums`, { method: 'GET' });
		const json = await res.json();
		if (!json || !json.success) {
			alert(json?.message || 'enum 조회 실패');
			return;
		}
		const data = json.data || {};

		fillSelect('client-seller-transfer-form-tradingStatus', data.tradingStatus, '거래상태 선택');
		fillSelect('client-seller-transfer-form-supplyType', data.supplyType, '공급유형 선택');
		fillSelect('client-seller-transfer-form-supplyStructure', data.supplyStructure, '공급구조 선택');

		// ✅ 정산정책 enum
		fillSelect('client-seller-transfer-form-settlementCycle', data.settlementCycle, '정산주기 선택');
		fillSelect('client-seller-transfer-form-settlementBasis', data.settlementBasis, '정산기준 선택');
	}

	function fillSelect(selectId, items, placeholder) {
		const el = document.getElementById(selectId);
		el.innerHTML = '';
		const opt0 = document.createElement('option');
		opt0.value = '';
		opt0.textContent = placeholder;
		el.appendChild(opt0);

		(items || []).forEach(v => {
			const opt = document.createElement('option');
			opt.value = v;
			opt.textContent = v;
			el.appendChild(opt);
		});
	}

	async function openApproveModal(applicationId) {
		resetModalState();
		await loadEnums();
		await loadCategoryLarges();

		const res = await fetch(`${API_BASE}/seller-applications/${applicationId}`, { method: 'GET' });
		const json = await res.json();
		if (!json || !json.success) {
			alert(json?.message || '상세 조회 실패');
			return;
		}
		const d = json.data;

		document.getElementById('client-seller-transfer-modal-application-id').value = d.applicationId;
		document.getElementById('client-seller-transfer-modal-username').value = d.username || '';
		document.getElementById('client-seller-transfer-modal-name').value = d.name || '';
		document.getElementById('client-seller-transfer-modal-mobile').value = d.mobile || '';
		document.getElementById('client-seller-transfer-modal-companyName').value = d.companyName || '';
		document.getElementById('client-seller-transfer-modal-requestedAt').value = d.requestedAt || '';
		document.getElementById('client-seller-transfer-modal-bizNo').value = d.businessRegistrationNumber || '';

		const modal = new bootstrap.Modal(document.getElementById('client-seller-transfer-approve-modal'));
		modal.show();
	}

	/* =========================
	 * ✅ Daum Postcode
	 * ========================= */

	function openDaumPostcode(target) {
		if (typeof daum === 'undefined' || !daum.Postcode) {
			alert('Daum Postcode 스크립트가 로드되지 않았습니다.');
			return;
		}

		const isBiz = (target === 'biz');
		const idPost = isBiz ? 'client-seller-transfer-form-biz-postcode' : 'client-seller-transfer-form-ret-postcode';
		const idRoad = isBiz ? 'client-seller-transfer-form-biz-road' : 'client-seller-transfer-form-ret-road';
		const idJibun = isBiz ? 'client-seller-transfer-form-biz-jibun' : 'client-seller-transfer-form-ret-jibun';
		const idDetail = isBiz ? 'client-seller-transfer-form-biz-detail' : 'client-seller-transfer-form-ret-detail';

		new daum.Postcode({
			oncomplete: function(data) {
				const zonecode = data.zonecode || '';
				const roadAddress = data.roadAddress || '';
				const jibunAddress = data.jibunAddress || '';

				document.getElementById(idPost).value = zonecode;
				document.getElementById(idRoad).value = roadAddress;
				document.getElementById(idJibun).value = jibunAddress;

				// 상세주소 입력 유도
				document.getElementById(idDetail).focus();
			}
		}).open();
	}

	/* =========================
	 * 연락처 동적
	 * ========================= */

	function renderContacts() {
		const tb = document.getElementById('client-seller-transfer-contact-tbody');
		tb.innerHTML = '';

		if (!modalState.contacts || modalState.contacts.length === 0) {
			tb.innerHTML = `<tr><td colspan="4" class="text-center text-muted">등록된 담당자가 없습니다.</td></tr>`;
			return;
		}

		modalState.contacts.forEach((c, idx) => {
			const tr = document.createElement('tr');
			tr.innerHTML = `
				<td><input type="text" class="form-control client-seller-transfer-contact-name" data-idx="${idx}" value="${escapeHtml(c.name)}"></td>
				<td><input type="text" class="form-control client-seller-transfer-contact-phone" data-idx="${idx}" value="${escapeHtml(c.phone)}"></td>
				<td><input type="text" class="form-control client-seller-transfer-contact-email" data-idx="${idx}" value="${escapeHtml(c.email)}"></td>
				<td class="text-center">
					<button type="button" class="btn btn-sm btn-outline-danger client-seller-transfer-contact-del" data-idx="${idx}">삭제</button>
				</td>
			`;
			tb.appendChild(tr);
		});

		tb.querySelectorAll('.client-seller-transfer-contact-del').forEach(btn => {
			btn.addEventListener('click', () => {
				const idx = parseInt(btn.dataset.idx, 10);
				modalState.contacts.splice(idx, 1);
				renderContacts();
			});
		});

		tb.querySelectorAll('.client-seller-transfer-contact-name').forEach(inp => {
			inp.addEventListener('input', () => {
				modalState.contacts[parseInt(inp.dataset.idx, 10)].name = inp.value;
			});
		});
		tb.querySelectorAll('.client-seller-transfer-contact-phone').forEach(inp => {
			inp.addEventListener('input', () => {
				modalState.contacts[parseInt(inp.dataset.idx, 10)].phone = inp.value;
			});
		});
		tb.querySelectorAll('.client-seller-transfer-contact-email').forEach(inp => {
			inp.addEventListener('input', () => {
				modalState.contacts[parseInt(inp.dataset.idx, 10)].email = inp.value;
			});
		});
	}

	function addContactRow() {
		if (!modalState.contacts) modalState.contacts = [];
		modalState.contacts.push({ name: '', phone: '', email: '' });
		renderContacts();
	}

	/* =========================
	 * 카테고리 권한 등록
	 * ========================= */

	async function loadCategoryLarges() {
		const res = await fetch(`${API_BASE}/categories/larges`, { method: 'GET' });
		const json = await res.json();
		if (!json || !json.success) {
			alert(json?.message || '대분류 조회 실패');
			return;
		}

		const sel = document.getElementById('client-seller-transfer-cat-large');
		sel.innerHTML = '<option value="">대분류 선택</option>';
		json.data.forEach(it => {
			sel.innerHTML += `<option value="${it.id}">${escapeHtml(it.name)}</option>`;
		});

		const med = document.getElementById('client-seller-transfer-cat-medium');
		const sm = document.getElementById('client-seller-transfer-cat-small');
		med.innerHTML = '<option value="">중분류 선택</option>';
		sm.innerHTML = '<option value="">소분류 선택</option>';
		med.disabled = true;
		sm.disabled = true;
	}

	async function loadCategoryMediums(largeId) {
		const res = await fetch(`${API_BASE}/categories/mediums?largeId=${encodeURIComponent(largeId)}`, { method: 'GET' });
		const json = await res.json();
		if (!json || !json.success) {
			alert(json?.message || '중분류 조회 실패');
			return;
		}

		const med = document.getElementById('client-seller-transfer-cat-medium');
		med.innerHTML = '<option value="">중분류 선택</option>';
		json.data.forEach(it => {
			med.innerHTML += `<option value="${it.id}">${escapeHtml(it.name)}</option>`;
		});
		med.disabled = false;

		const sm = document.getElementById('client-seller-transfer-cat-small');
		sm.innerHTML = '<option value="">소분류 선택</option>';
		sm.disabled = true;
	}

	async function loadCategorySmalls(mediumId) {
		const res = await fetch(`${API_BASE}/categories/smalls?mediumId=${encodeURIComponent(mediumId)}`, { method: 'GET' });
		const json = await res.json();
		if (!json || !json.success) {
			alert(json?.message || '소분류 조회 실패');
			return;
		}

		const sm = document.getElementById('client-seller-transfer-cat-small');
		sm.innerHTML = '<option value="">소분류 선택</option>';
		json.data.forEach(it => {
			sm.innerHTML += `<option value="${it.id}">${escapeHtml(it.name)}</option>`;
		});
		sm.disabled = false;
	}

	function renderCategoryChips() {
		const wrap = document.getElementById('client-seller-transfer-cat-chip-wrap');
		wrap.innerHTML = '';

		const countText = document.getElementById('client-seller-transfer-cat-count-text');

		if (!modalState.categoryPermissions || modalState.categoryPermissions.length === 0) {
			countText.textContent = '등록된 권한이 없습니다. (최소 1개 필요)';
			return;
		}

		modalState.categoryPermissions.forEach((p, idx) => {
			const div = document.createElement('div');
			div.className = 'client-seller-transfer-chip';
			div.innerHTML = `
				<span>${escapeHtml(p.label)}</span>
				<button type="button" class="client-seller-transfer-cat-del" data-idx="${idx}">X</button>
			`;
			wrap.appendChild(div);
		});

		wrap.querySelectorAll('.client-seller-transfer-cat-del').forEach(btn => {
			btn.addEventListener('click', () => {
				const idx = parseInt(btn.dataset.idx, 10);
				modalState.categoryPermissions.splice(idx, 1);
				renderCategoryChips();
			});
		});

		countText.textContent = `등록된 권한: ${modalState.categoryPermissions.length}개`;
	}

	function existsLargeAll(largeId) {
		return (modalState.categoryPermissions || []).some(p => p.largeId === largeId && !p.mediumId && !p.smallId);
	}

	function existsMediumAll(largeId, mediumId) {
		return (modalState.categoryPermissions || []).some(p => p.largeId === largeId && p.mediumId === mediumId && !p.smallId);
	}

	function removeByLarge(largeId) {
		modalState.categoryPermissions = (modalState.categoryPermissions || []).filter(p => p.largeId !== largeId);
	}

	function removeByMedium(largeId, mediumId) {
		modalState.categoryPermissions = (modalState.categoryPermissions || []).filter(p => !(p.largeId === largeId && p.mediumId === mediumId));
	}

	function addCategoryPermission() {
		const largeSel = document.getElementById('client-seller-transfer-cat-large');
		const mediumSel = document.getElementById('client-seller-transfer-cat-medium');
		const smallSel = document.getElementById('client-seller-transfer-cat-small');

		const largeId = largeSel.value ? Number(largeSel.value) : null;
		const mediumId = mediumSel.value ? Number(mediumSel.value) : null;
		const smallId = smallSel.value ? Number(smallSel.value) : null;

		if (!largeId) {
			alert('대분류는 필수입니다.');
			return;
		}

		const largeName = largeSel.options[largeSel.selectedIndex]?.text || '';
		const mediumName = mediumId ? (mediumSel.options[mediumSel.selectedIndex]?.text || '') : '';
		const smallName = smallId ? (smallSel.options[smallSel.selectedIndex]?.text || '') : '';

		// 레벨 판단
		const level = smallId ? 'SMALL' : (mediumId ? 'MEDIUM' : 'LARGE');

		// 규칙 적용:
		// 1) LARGE 등록: 동일 large의 기존 medium/small 권한 전부 제거 후 large만 남김
		// 2) MEDIUM 등록: large 전체 권한이 있으면 불필요 -> 차단
		//              동일 large+medium 하위(small 포함) 권한 제거 후 medium만 남김
		// 3) SMALL 등록: large 전체 권한이 있으면 차단, medium 전체 권한이 있으면 차단
		if (level === 'LARGE') {
			if (existsLargeAll(largeId)) {
				alert('이미 해당 대분류 전체 권한이 등록되어 있습니다.');
				return;
			}
			// 덮어쓰기: 동일 대분류의 기존 권한 모두 제거
			removeByLarge(largeId);

			const label = `${largeName} (전체)`;
			const key = `${largeId}::`;

			modalState.categoryPermissions.push({ key, largeId, mediumId: null, smallId: null, label });
		}

		if (level === 'MEDIUM') {
			if (existsLargeAll(largeId)) {
				alert('이미 해당 대분류 전체 권한이 등록되어 있어, 중분류를 추가 등록할 필요가 없습니다.');
				return;
			}

			if (!mediumId) {
				alert('중분류 선택이 올바르지 않습니다.');
				return;
			}

			// 덮어쓰기: 동일 대분류+중분류 하위 권한(small 포함) 제거 후 medium만 남김
			removeByMedium(largeId, mediumId);

			const label = `${largeName} > ${mediumName} (전체)`;
			const key = `${largeId}:${mediumId}:`;

			// 중복 체크(동일 medium 전체)
			const exists = (modalState.categoryPermissions || []).some(x => x.key === key);
			if (exists) {
				alert('이미 등록된 권한입니다.');
				return;
			}

			modalState.categoryPermissions.push({ key, largeId, mediumId, smallId: null, label });
		}

		if (level === 'SMALL') {
			if (existsLargeAll(largeId)) {
				alert('이미 해당 대분류 전체 권한이 등록되어 있어, 소분류를 추가 등록할 필요가 없습니다.');
				return;
			}
			if (!mediumId) {
				alert('소분류 등록은 중분류 선택이 필요합니다.');
				return;
			}
			if (existsMediumAll(largeId, mediumId)) {
				alert('이미 해당 중분류 전체 권한이 등록되어 있어, 소분류를 추가 등록할 필요가 없습니다.');
				return;
			}
			if (!smallId) {
				alert('소분류 선택이 올바르지 않습니다.');
				return;
			}

			const label = `${largeName} > ${mediumName} > ${smallName}`;
			const key = `${largeId}:${mediumId}:${smallId}`;

			const exists = (modalState.categoryPermissions || []).some(x => x.key === key);
			if (exists) {
				alert('이미 등록된 권한입니다.');
				return;
			}

			modalState.categoryPermissions.push({ key, largeId, mediumId, smallId, label });
		}

		// 셀렉트 초기화
		largeSel.value = '';
		mediumSel.innerHTML = '<option value="">중분류 선택</option>';
		smallSel.innerHTML = '<option value="">소분류 선택</option>';
		mediumSel.disabled = true;
		smallSel.disabled = true;

		renderCategoryChips();
	}

	/* =========================
	 * 승인 요청
	 * ========================= */

	function val(id) {
		return document.getElementById(id).value || '';
	}

	function requireText(v, msg) {
		if (!v || String(v).trim() === '') {
			alert(msg);
			throw new Error(msg);
		}
	}

	function requireRate(v, msg) {
		requireText(v, msg);
		const n = Number(v);
		if (Number.isNaN(n)) {
			alert('수수료율은 숫자여야 합니다.');
			throw new Error('수수료율은 숫자여야 합니다.');
		}
		if (n < 0 || n > 100) {
			alert('수수료율은 0~100 사이여야 합니다.');
			throw new Error('수수료율은 0~100 사이여야 합니다.');
		}
	}

	async function approveSeller() {
		const applicationId = document.getElementById('client-seller-transfer-modal-application-id').value;
		if (!applicationId) return;

		try {
			const shopName = val('client-seller-transfer-form-shopName');
			const tradingStatus = val('client-seller-transfer-form-tradingStatus');
			const supplyType = val('client-seller-transfer-form-supplyType');
			const supplyStructure = val('client-seller-transfer-form-supplyStructure');
			const productTypeText = val('client-seller-transfer-form-productTypeText');
			const tel = val('client-seller-transfer-form-tel');

			requireText(shopName, '입점몰명(shopName)은 필수입니다.');
			requireText(tradingStatus, '거래상태는 필수입니다.');
			requireText(supplyType, '공급유형은 필수입니다.');
			requireText(supplyStructure, '공급구조는 필수입니다.');
			requireText(productTypeText, '거래상품유형은 필수입니다.');
			requireText(tel, '일반전화(tel)은 필수입니다.');

			// ✅ 정산정책 필수
			const commissionRate = val('client-seller-transfer-form-commissionRate');
			const settlementCycle = val('client-seller-transfer-form-settlementCycle');
			const settlementBasis = val('client-seller-transfer-form-settlementBasis');

			requireRate(commissionRate, '수수료율(commissionRate)은 필수입니다.');
			requireText(settlementCycle, '정산주기(cycle)는 필수입니다.');
			requireText(settlementBasis, '정산기준(basis)는 필수입니다.');

			// ✅ 주소(daum 세팅값)
			const bizPost = val('client-seller-transfer-form-biz-postcode');
			const bizRoad = val('client-seller-transfer-form-biz-road');
			const retPost = val('client-seller-transfer-form-ret-postcode');
			const retRoad = val('client-seller-transfer-form-ret-road');

			requireText(bizPost, '사업장 우편번호는 필수입니다.');
			requireText(bizRoad, '사업장 도로명주소는 필수입니다.');
			requireText(retPost, '반품 우편번호는 필수입니다.');
			requireText(retRoad, '반품 도로명주소는 필수입니다.');

			if (!modalState.categoryPermissions || modalState.categoryPermissions.length < 1) {
				alert('카테고리 권한을 1개 이상 등록해 주세요.');
				return;
			}

			// contacts: 입력된 row만 전송(빈행 제거)
			const contacts = (modalState.contacts || [])
				.map(c => ({ name: (c.name || '').trim(), phone: (c.phone || '').trim(), email: (c.email || '').trim() }))
				.filter(c => c.name !== '' || c.phone !== '' || c.email !== '')
				.map(c => {
					if (c.name === '') {
						throw new Error('담당자 행에 입력이 있다면 담당자명은 필수입니다.');
					}
					return c;
				});

			const payload = {
				processNote: val('client-seller-transfer-form-processNote'),
				shopName,
				tradingStatus,
				supplyType,
				supplyStructure,
				productTypeText,
				tel,
				fax: val('client-seller-transfer-form-fax'),
				homepageUrl: val('client-seller-transfer-form-homepageUrl'),
				businessAddress: {
					postcode: bizPost,
					roadAddress: bizRoad,
					jibunAddress: val('client-seller-transfer-form-biz-jibun'),
					detailAddress: val('client-seller-transfer-form-biz-detail'),
				},
				returnAddress: {
					postcode: retPost,
					roadAddress: retRoad,
					jibunAddress: val('client-seller-transfer-form-ret-jibun'),
					detailAddress: val('client-seller-transfer-form-ret-detail'),
				},
				contacts,
				categoryPermissions: modalState.categoryPermissions.map(p => ({
					largeId: p.largeId,
					mediumId: p.mediumId || null,
					smallId: p.smallId || null
				})),
				// ✅ 정산정책
				settlementPolicy: {
					commissionRate: String(commissionRate), // BigDecimal 안전(문자열)
					cycle: settlementCycle,
					basis: settlementBasis
				}
			};

			if (!confirm('입력한 내용으로 판매딜러 승인 처리하시겠습니까?')) return;

			const res = await fetch(`${API_BASE}/seller-applications/${applicationId}/approve`, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
					...csrfHeaders()
				},
				body: JSON.stringify(payload)
			});
			const json = await res.json();
			if (!json || !json.success) {
				alert(json?.message || '승인 실패');
				return;
			}

			alert('판매딜러 승인 완료');
			bootstrap.Modal.getInstance(document.getElementById('client-seller-transfer-approve-modal'))?.hide();
			refresh();

		} catch (e) {
			if (e && e.message) alert(e.message);
		}
	}

	/* =========================
	 * 이벤트 바인딩
	 * ========================= */

	function bindEvents() {

		// 라디오 이동
		document.querySelectorAll('input[name="client-seller-transfer-kind"]').forEach(r => {
			r.addEventListener('change', () => {
				const v = document.querySelector('input[name="client-seller-transfer-kind"]:checked')?.value;
				if (v === 'COMPANY') location.href = '/admin/root/clientCompanyTransferManager';
				if (v === 'ALL') alert('전체 화면(통합)은 현재 2페이지 구조로 구현되어 있습니다.\n필요하시면 통합 페이지도 추가해드리겠습니다.');
			});
		});

		document.getElementById('client-seller-transfer-search-btn').addEventListener('click', () => {
			state.page = 0;
			refresh();
		});
		document.getElementById('client-seller-transfer-reset-btn').addEventListener('click', () => {
			resetFilters();
			refresh();
		});

		document.querySelectorAll('.client-seller-transfer-sort-btn').forEach(btn => {
			btn.addEventListener('click', () => {
				const key = btn.dataset.sort;
				if (!key) return;

				if (state.sortKey === key) {
					state.sortDir = (state.sortDir === 'asc') ? 'desc' : 'asc';
				} else {
					state.sortKey = key;
					state.sortDir = 'asc';
				}
				state.page = 0;
				refresh();
			});
		});

		document.getElementById('client-seller-transfer-contact-add-btn').addEventListener('click', addContactRow);

		// ✅ Daum 주소검색 버튼
		document.getElementById('client-seller-transfer-biz-addr-search-btn').addEventListener('click', () => openDaumPostcode('biz'));
		document.getElementById('client-seller-transfer-ret-addr-search-btn').addEventListener('click', () => openDaumPostcode('ret'));

		document.getElementById('client-seller-transfer-cat-large').addEventListener('change', async (e) => {
			const largeId = e.target.value;
			if (!largeId) {
				const med = document.getElementById('client-seller-transfer-cat-medium');
				const sm = document.getElementById('client-seller-transfer-cat-small');
				med.innerHTML = '<option value="">중분류 선택</option>'; med.disabled = true;
				sm.innerHTML = '<option value="">소분류 선택</option>'; sm.disabled = true;
				return;
			}
			await loadCategoryMediums(largeId);
		});

		document.getElementById('client-seller-transfer-cat-medium').addEventListener('change', async (e) => {
			const mid = e.target.value;
			const sm = document.getElementById('client-seller-transfer-cat-small');
			if (!mid) {
				sm.innerHTML = '<option value="">소분류 선택</option>';
				sm.disabled = true;
				return;
			}
			await loadCategorySmalls(mid);
		});

		document.getElementById('client-seller-transfer-cat-add-btn').addEventListener('click', addCategoryPermission);
		document.getElementById('client-seller-transfer-modal-approve-btn').addEventListener('click', approveSeller);
	}

	document.addEventListener('DOMContentLoaded', () => {
		bindEvents();
		refresh();
	});

})();