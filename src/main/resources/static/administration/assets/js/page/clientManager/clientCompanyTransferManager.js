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

	function csrfHeaders() {
		const token = document.getElementById('client-company-transfer-csrf-token')?.value || '';
		const header = document.getElementById('client-company-transfer-csrf-header')?.value || '';
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
		document.querySelectorAll('.client-company-transfer-sort-btn').forEach(btn => {
			btn.classList.remove('active');
			if (btn.dataset.sort === state.sortKey) btn.classList.add('active');
		});
	}

	function readFilters() {
		state.size = parseInt(document.getElementById('client-company-transfer-size').value || '10', 10);
		state.fromDate = document.getElementById('client-company-transfer-from').value || '';
		state.toDate = document.getElementById('client-company-transfer-to').value || '';
		state.searchType = document.getElementById('client-company-transfer-search-type').value || '';
		state.keyword = document.getElementById('client-company-transfer-keyword').value || '';
	}

	function resetFilters() {
		document.getElementById('client-company-transfer-size').value = '10';
		document.getElementById('client-company-transfer-from').value = '';
		document.getElementById('client-company-transfer-to').value = '';
		document.getElementById('client-company-transfer-search-type').value = '';
		document.getElementById('client-company-transfer-keyword').value = '';
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

		const url = `${API_BASE}/company-applications?` + qs({
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
		const tb = document.getElementById('client-company-transfer-tbody');
		tb.innerHTML = '';
	}

	function renderEmpty() {
		const tb = document.getElementById('client-company-transfer-tbody');
		tb.innerHTML = `<tr><td colspan="7" class="text-center text-muted">조회된 데이터가 없습니다.</td></tr>`;
	}

	function updateBulkButton() {
		const checked = document.querySelectorAll('.client-company-transfer-row-check:checked').length;
		const btn = document.getElementById('client-company-transfer-bulk-approve-btn');
		btn.disabled = checked <= 0;
	}

	function renderRows(pageData) {
		clearTbody();
		const tb = document.getElementById('client-company-transfer-tbody');

		if (!pageData.content || pageData.content.length === 0) {
			renderEmpty();
			return;
		}

		pageData.content.forEach(row => {
			const tr = document.createElement('tr');

			tr.innerHTML = `
				<td style="text-align:center;">
					<input type="checkbox" class="client-company-transfer-row-check" data-id="${row.applicationId}">
				</td>
				<td>${escapeHtml(row.username)}</td>
				<td>${escapeHtml(row.companyName)}</td>
				<td>${escapeHtml(row.name)}</td>
				<td>${escapeHtml(row.mobile)}</td>
				<td>${escapeHtml(row.requestedAt)}</td>
				<td>
					<button type="button" class="btn btn-sm btn-outline-primary client-company-transfer-approve-btn" data-id="${row.applicationId}">
						승인처리
					</button>
				</td>
			`;
			tb.appendChild(tr);
		});

		document.getElementById('client-company-transfer-check-all').checked = false;
		updateBulkButton();

		document.querySelectorAll('.client-company-transfer-row-check').forEach(chk => {
			chk.addEventListener('change', updateBulkButton);
		});

		document.querySelectorAll('.client-company-transfer-approve-btn').forEach(btn => {
			btn.addEventListener('click', () => openDetailModal(btn.dataset.id));
		});
	}

	function renderPagination(pageData) {
		const ul = document.getElementById('client-company-transfer-pagination');
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

		// 최대 5개 번호
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

		document.getElementById('client-company-transfer-total-text').textContent =
			`총 ${pageData.totalElements}건 / ${pageData.totalPages}페이지`;

		renderRows(pageData);
		renderPagination(pageData);
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

	async function openDetailModal(applicationId) {
		const res = await fetch(`${API_BASE}/company-applications/${applicationId}`, { method: 'GET' });
		const json = await res.json();
		if (!json || !json.success) {
			alert(json?.message || '상세 조회 실패');
			return;
		}
		const d = json.data;

		document.getElementById('client-company-transfer-modal-application-id').value = d.applicationId;
		document.getElementById('client-company-transfer-modal-username').value = d.username || '';
		document.getElementById('client-company-transfer-modal-name').value = d.name || '';
		document.getElementById('client-company-transfer-modal-mobile').value = d.mobile || '';
		document.getElementById('client-company-transfer-modal-email').value = d.email || '';
		document.getElementById('client-company-transfer-modal-requestedAt').value = d.requestedAt || '';

		document.getElementById('client-company-transfer-modal-companyName').value = d.companyName || '';
		document.getElementById('client-company-transfer-modal-ceoName').value = d.ceoName || '';
		document.getElementById('client-company-transfer-modal-bizNo').value = d.businessRegistrationNumber || '';
		document.getElementById('client-company-transfer-modal-representativeTel').value = d.representativeTel || '';
		document.getElementById('client-company-transfer-modal-fax').value = d.fax || '';

		const addr = [d.companyPostcode, d.companyRoadAddress, d.companyJibunAddress, d.companyDetailAddress]
			.filter(v => v && String(v).trim() !== '')
			.join(' / ');
		document.getElementById('client-company-transfer-modal-address').value = addr;

		const img = document.getElementById('client-company-transfer-modal-bizImg');
		const hint = document.getElementById('client-company-transfer-modal-bizImgHint');
		if (d.bizRegImageRoad) {
			img.src = d.bizRegImageRoad;
			img.style.display = '';
			hint.textContent = d.bizRegImageRoad;
		} else {
			img.removeAttribute('src');
			img.style.display = 'none';
			hint.textContent = '이미지 경로 없음';
		}

		document.getElementById('client-company-transfer-modal-processNote').value = '';

		const modal = new bootstrap.Modal(document.getElementById('client-company-transfer-approve-modal'));
		modal.show();
	}

	async function approveOne() {
		const applicationId = document.getElementById('client-company-transfer-modal-application-id').value;
		if (!applicationId) return;

		const processNote = document.getElementById('client-company-transfer-modal-processNote').value || '';

		if (!confirm('승인 처리하시겠습니까?\n(승인 후 신청서는 삭제됩니다)')) return;

		const res = await fetch(`${API_BASE}/company-applications/${applicationId}/approve`, {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json',
				...csrfHeaders()
			},
			body: JSON.stringify({ processNote })
		});
		const json = await res.json();
		if (!json || !json.success) {
			alert(json?.message || '승인 실패');
			return;
		}

		alert('승인 완료');
		bootstrap.Modal.getInstance(document.getElementById('client-company-transfer-approve-modal'))?.hide();
		refresh();
	}

	async function bulkApprove() {
		const ids = Array.from(document.querySelectorAll('.client-company-transfer-row-check:checked'))
			.map(chk => Number(chk.dataset.id))
			.filter(v => !!v);

		if (ids.length === 0) return;

		if (!confirm(`선택한 ${ids.length}건을 상세 확인 없이 일괄승인 하시겠습니까?\n(승인 후 신청서는 삭제됩니다)`)) return;

		const res = await fetch(`${API_BASE}/company-applications/bulk-approve`, {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json',
				...csrfHeaders()
			},
			body: JSON.stringify({ applicationIds: ids, processNote: '' })
		});
		const json = await res.json();
		if (!json || !json.success) {
			alert(json?.message || '일괄승인 실패');
			return;
		}

		const r = json.data;
		let msg = `요청 ${r.requestedCount}건 / 성공 ${r.successCount}건 / 실패 ${r.failCount}건`;
		if (r.failCount > 0 && r.failures) {
			msg += `\n\n[실패 목록]\n` + r.failures.map(f => `- ${f.applicationId}: ${f.reason}`).join('\n');
		}
		alert(msg);
		refresh();
	}

	function bindEvents() {

		// 라디오 이동
		document.querySelectorAll('input[name="client-company-transfer-kind"]').forEach(r => {
			r.addEventListener('change', () => {
				const v = document.querySelector('input[name="client-company-transfer-kind"]:checked')?.value;
				if (v === 'SELLER') location.href = '/admin/root/clientSellerTransferManager';
				if (v === 'ALL') alert('전체 화면(통합)은 현재 2페이지 구조로 구현되어 있습니다.\n필요하시면 통합 페이지도 추가해드리겠습니다.');
			});
		});

		document.getElementById('client-company-transfer-search-btn').addEventListener('click', () => {
			state.page = 0;
			refresh();
		});

		document.getElementById('client-company-transfer-reset-btn').addEventListener('click', () => {
			resetFilters();
			refresh();
		});

		document.getElementById('client-company-transfer-check-all').addEventListener('change', (e) => {
			const checked = e.target.checked;
			document.querySelectorAll('.client-company-transfer-row-check').forEach(chk => chk.checked = checked);
			updateBulkButton();
		});

		document.getElementById('client-company-transfer-bulk-approve-btn').addEventListener('click', bulkApprove);

		document.getElementById('client-company-transfer-modal-approve-btn').addEventListener('click', approveOne);

		document.querySelectorAll('.client-company-transfer-sort-btn').forEach(btn => {
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
	}

	document.addEventListener('DOMContentLoaded', () => {
		bindEvents();
		refresh();
	});

})();