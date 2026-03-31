/* settlementManager.js */
(function() {
	'use strict';

	const tbody = document.getElementById('settlement-manager-tbody');
	const pagination = document.getElementById('settlement-manager-pagination');
	const btnSearch = document.getElementById('settlement-manager-btn-search');
	const btnReset = document.getElementById('settlement-manager-btn-reset');
	const btnSaveStatus = document.getElementById('settlement-manager-btn-save-status');
	const orderModalEl = document.getElementById('settlement-manager-order-modal');
	const orderModal = orderModalEl ? new bootstrap.Modal(orderModalEl) : null;
	const orderModalTbody = document.getElementById('settlement-manager-order-modal-tbody');

	const changedStatusMap = new Map();
	let currentPage = 0;

	function toJsonSafe(res) {
		return res.json().catch(() => null);
	}

	function formatDateTime(v) {
		if (!v) return '-';
		return String(v).replace('T', ' ');
	}

	function formatDate(v) {
		return v || '-';
	}

	function formatNumber(v) {
		return new Intl.NumberFormat('ko-KR').format(Number(v || 0));
	}

	function getCheckedValues(prefix) {
		return Array.from(document.querySelectorAll(`input[id^="${prefix}"]:checked`)).map(x => x.value);
	}

	function buildQuery(page) {
		const params = new URLSearchParams();
		params.set('page', String(page));
		params.set('size', document.getElementById('settlement-manager-size').value);
		const payStatus = document.getElementById('settlement-manager-pay-status').value;
		const keyword = (document.getElementById('settlement-manager-keyword').value || '').trim();

		getCheckedValues('settlement-manager-basis-').forEach(v => params.append('bases', v));
		getCheckedValues('settlement-manager-cycle-').forEach(v => params.append('cycles', v));

		if (payStatus) params.set('payStatus', payStatus);
		if (keyword) params.set('keyword', keyword);

		return params.toString();
	}

	function refreshSaveButton() {
		btnSaveStatus.disabled = changedStatusMap.size === 0;
	}

	function renderRows(data) {
		if (!data.content || data.content.length === 0) {
			tbody.innerHTML = `<tr><td colspan="14" class="settlement-manager-empty">조회 결과가 없습니다.</td></tr>`;
			return;
		}

		tbody.innerHTML = data.content.map(row => `
            <tr>
                <td>${row.id}</td>
                <td class="settlement-manager-user-box">
                    <div class="fw-semibold">${escapeHtml(row.memberUsername || '-')}</div>
                    <div class="small text-muted">${escapeHtml(row.memberName || '-')} / ${escapeHtml(row.shopName || '-')}</div>
                    <div class="small text-muted">${escapeHtml(row.companyName || '-')}</div>
                </td>
                <td>${escapeHtml(row.cycle || '-')}</td>
                <td>${escapeHtml(row.basis || '-')}</td>
                <td class="settlement-manager-period">${formatDate(row.periodStartDate)} ~ ${formatDate(row.periodEndDate)}</td>
                <td class="text-center">${formatNumber(row.orderCount)}</td>
                <td class="text-center">${formatNumber(row.itemCount)}</td>
                <td class="text-end">${formatNumber(row.grossAmount)}원</td>
                <td class="text-end text-danger">${formatNumber(row.commissionAmount)}원</td>
                <td class="text-end fw-semibold text-success">${formatNumber(row.settlementAmount)}원</td>
                <td>${formatDateTime(row.executedAt)}</td>
                <td>${formatDateTime(row.paidAt)}</td>
                <td>
                    <button type="button" class="btn btn-outline-primary btn-sm settlement-manager-order-btn"
                        data-settlement-id="${row.id}">
                        주문보기
                    </button>
                </td>
                <td>
                    <select class="form-select form-select-sm settlement-manager-status-select"
                        data-settlement-id="${row.id}" data-original="${row.payStatus}">
                        <option value="UNPAID" ${row.payStatus === 'UNPAID' ? 'selected' : ''}>미지급</option>
                        <option value="PAID" ${row.payStatus === 'PAID' ? 'selected' : ''}>정산완료</option>
                    </select>
                </td>
            </tr>
        `).join('');
	}

	function renderPagination(data) {
		pagination.innerHTML = '';
		if (!data.totalPages || data.totalPages <= 1) {
			return;
		}

		const makeItem = (text, page, disabled, active) => {
			const li = document.createElement('li');
			li.className = `page-item${disabled ? ' disabled' : ''}${active ? ' active' : ''}`;
			const a = document.createElement('a');
			a.className = 'page-link';
			a.href = 'javascript:void(0);';
			a.textContent = text;
			a.addEventListener('click', () => {
				if (disabled) return;
				load(page);
			});
			li.appendChild(a);
			return li;
		};

		pagination.appendChild(makeItem('<<', 0, data.page === 0, false));
		pagination.appendChild(makeItem('<', Math.max(data.page - 1, 0), data.page === 0, false));

		for (let p = 0; p < data.totalPages; p++) {
			pagination.appendChild(makeItem(String(p + 1), p, false, p === data.page));
		}

		pagination.appendChild(makeItem('>', Math.min(data.page + 1, data.totalPages - 1), data.page >= data.totalPages - 1, false));
		pagination.appendChild(makeItem('>>', data.totalPages - 1, data.page >= data.totalPages - 1, false));
	}

	async function load(page) {
		currentPage = page;
		const res = await fetch(`/admin/root/api/settlement-manager/search?${buildQuery(page)}`);
		const body = await toJsonSafe(res);

		if (!res.ok || !body || !body.success) {
			tbody.innerHTML = `<tr><td colspan="14" class="text-center text-danger py-5">${body?.message || '조회 실패'}</td></tr>`;
			pagination.innerHTML = '';
			return;
		}

		renderRows(body.data);
		renderPagination(body.data);
	}

	async function loadOrders(settlementId) {
		const res = await fetch(`/admin/root/api/settlement-manager/${settlementId}/orders`);
		const body = await toJsonSafe(res);

		if (!res.ok || !body || !body.success) {
			orderModalTbody.innerHTML = `<tr><td colspan="6" class="text-center text-danger">조회 실패</td></tr>`;
			orderModal.show();
			return;
		}

		const list = body.data || [];
		if (list.length === 0) {
			orderModalTbody.innerHTML = `<tr><td colspan="6" class="text-center text-muted">포함 주문이 없습니다.</td></tr>`;
		} else {
			orderModalTbody.innerHTML = list.map(row => `
                <tr>
                    <td>${row.orderId}</td>
                    <td>${escapeHtml(row.orderNo || '-')}</td>
                    <td>${escapeHtml(row.ordererName || '-')}</td>
                    <td>${formatDateTime(row.basisDate)}</td>
                    <td class="text-center">${formatNumber(row.dealerItemCount)}</td>
                    <td class="text-end">${formatNumber(row.dealerItemAmount)}원</td>
                </tr>
            `).join('');
		}
		orderModal.show();
	}

	async function saveStatus() {
		if (changedStatusMap.size === 0) {
			return;
		}

		const payload = {
			items: Array.from(changedStatusMap.entries()).map(([settlementId, payStatus]) => ({
				settlementId: Number(settlementId),
				payStatus: payStatus
			}))
		};

		const res = await fetch('/admin/root/api/settlement-manager/status', {
			method: 'PATCH',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify(payload)
		});
		const body = await toJsonSafe(res);

		if (!res.ok || !body || !body.success) {
			alert(body?.message || '상태 변경 실패');
			return;
		}

		alert(body.message || '상태 변경 완료');
		changedStatusMap.clear();
		refreshSaveButton();
		load(currentPage);
	}

	function resetFilters() {
		document.getElementById('settlement-manager-size').value = '10';
		document.getElementById('settlement-manager-pay-status').value = '';
		document.getElementById('settlement-manager-keyword').value = '';
		document.querySelectorAll('#settlement-manager-basis-wrap input[type="checkbox"], #settlement-manager-cycle-wrap input[type="checkbox"]').forEach(x => x.checked = false);
	}

	function escapeHtml(str) {
		return String(str ?? '')
			.replaceAll('&', '&amp;')
			.replaceAll('<', '&lt;')
			.replaceAll('>', '&gt;')
			.replaceAll('"', '&quot;')
			.replaceAll("'", '&#039;');
	}

	btnSearch.addEventListener('click', () => load(0));
	btnReset.addEventListener('click', () => {
		resetFilters();
		load(0);
	});
	btnSaveStatus.addEventListener('click', saveStatus);

	tbody.addEventListener('click', (e) => {
		const btn = e.target.closest('.settlement-manager-order-btn');
		if (!btn) return;
		loadOrders(btn.dataset.settlementId);
	});

	tbody.addEventListener('change', (e) => {
		const select = e.target.closest('.settlement-manager-status-select');
		if (!select) return;

		const settlementId = select.dataset.settlementId;
		const original = select.dataset.original;
		const current = select.value;

		if (original === current) {
			changedStatusMap.delete(settlementId);
		} else {
			changedStatusMap.set(settlementId, current);
		}
		refreshSaveButton();
	});

	load(0);
})();