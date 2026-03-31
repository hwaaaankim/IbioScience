/* settlement.js */
(function() {
	'use strict';

	const tbody = document.getElementById('settlement-excute-tbody');
	const btnSearch = document.getElementById('settlement-excute-btn-search');
	const btnReset = document.getElementById('settlement-excute-btn-reset');
	const btnRun = document.getElementById('settlement-excute-btn-run');
	const overlay = document.getElementById('settlement-excute-loading-overlay');
	const overlayMessage = document.getElementById('settlement-excute-loading-message');

	let latestPayload = null;
	let latestPreview = null;

	function getCheckedValues(prefix) {
		return Array.from(document.querySelectorAll(`input[id^="${prefix}"]:checked`)).map(x => x.value);
	}

	function toJsonSafe(res) {
		return res.json().catch(() => null);
	}

	function formatNumber(v) {
		return new Intl.NumberFormat('ko-KR').format(Number(v || 0));
	}

	function escapeHtml(str) {
		return String(str ?? '')
			.replaceAll('&', '&amp;')
			.replaceAll('<', '&lt;')
			.replaceAll('>', '&gt;')
			.replaceAll('"', '&quot;')
			.replaceAll("'", '&#039;');
	}

	function buildPayload() {
		return {
			fromDate: document.getElementById('settlement-excute-from-date').value || null,
			toDate: document.getElementById('settlement-excute-to-date').value || null,
			cycles: getCheckedValues('settlement-excute-cycle-'),
			bases: getCheckedValues('settlement-excute-basis-'),
			keyword: (document.getElementById('settlement-excute-keyword').value || '').trim() || null
		};
	}

	function validatePayload(payload) {
		if (payload.fromDate && payload.toDate && payload.fromDate > payload.toDate) {
			alert('시작일은 종료일보다 늦을 수 없습니다.');
			return false;
		}
		return true;
	}

	function setLoading(show, message) {
		overlay.style.display = show ? 'flex' : 'none';
		overlayMessage.textContent = message || '정산 데이터를 생성하고 있습니다.';
	}

	function renderSummary(data) {
		document.getElementById('settlement-excute-summary-count').textContent = formatNumber(data.count || 0);
		document.getElementById('settlement-excute-summary-gross').textContent = `${formatNumber(data.totalGrossAmount || 0)}원`;
		document.getElementById('settlement-excute-summary-commission').textContent = `${formatNumber(data.totalCommissionAmount || 0)}원`;
		document.getElementById('settlement-excute-summary-settlement').textContent = `${formatNumber(data.totalSettlementAmount || 0)}원`;
	}

	function renderRows(list) {
		if (!list || list.length === 0) {
			tbody.innerHTML = `<tr><td colspan="9" class="text-center text-muted py-5">생성 예정 정산내역이 없습니다.</td></tr>`;
			return;
		}

		tbody.innerHTML = list.map(row => `
            <tr>
                <td>
                    <div class="fw-semibold">${escapeHtml(row.memberUsername || '-')}</div>
                    <div class="small text-muted">${escapeHtml(row.memberName || '-')} / ${escapeHtml(row.shopName || '-')}</div>
                    <div class="small text-muted">${escapeHtml(row.companyName || '-')}</div>
                </td>
                <td>${escapeHtml(row.cycle || '-')}</td>
                <td>${escapeHtml(row.basis || '-')}</td>
                <td>${escapeHtml(row.periodStartDate || '-')} ~ ${escapeHtml(row.periodEndDate || '-')}</td>
                <td>${formatNumber(row.orderCount)}</td>
                <td>${formatNumber(row.itemCount)}</td>
                <td class="text-end">${formatNumber(row.grossAmount)}원</td>
                <td class="text-end text-danger">${formatNumber(row.commissionAmount)}원</td>
                <td class="text-end fw-semibold text-success">${formatNumber(row.settlementAmount)}원</td>
            </tr>
        `).join('');
	}

	function summarizeRows(rows) {
		const safeRows = Array.isArray(rows) ? rows : [];

		const totalGrossAmount = safeRows.reduce((sum, row) => sum + Number(row.grossAmount || 0), 0);
		const totalCommissionAmount = safeRows.reduce((sum, row) => sum + Number(row.commissionAmount || 0), 0);
		const totalSettlementAmount = safeRows.reduce((sum, row) => sum + Number(row.settlementAmount || 0), 0);

		return {
			count: safeRows.length,
			totalGrossAmount,
			totalCommissionAmount,
			totalSettlementAmount
		};
	}

	async function preview() {
		const payload = buildPayload();
		if (!validatePayload(payload)) {
			return;
		}

		const res = await fetch('/admin/root/api/settlement-execute/preview', {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify(payload)
		});
		const body = await toJsonSafe(res);

		if (!res.ok || !body || !body.success) {
			alert(body?.message || '미리보기 조회 실패');
			return;
		}

		latestPayload = payload;
		latestPreview = body.data;
		renderSummary(body.data || {});
		renderRows(body.data?.items || []);
	}

	async function run() {
		if (!latestPayload || !latestPreview) {
			alert('먼저 검색을 실행해 주세요.');
			return;
		}

		if (!latestPreview.items || latestPreview.items.length === 0) {
			alert('생성할 정산내역이 없습니다.');
			return;
		}

		if (!confirm(`총 ${latestPreview.count}건의 정산내역을 생성합니다. 진행하시겠습니까?`)) {
			return;
		}

		setLoading(true, '정산 실행 중입니다. 대상 기간이 길면 시간이 다소 걸릴 수 있습니다.');

		const res = await fetch('/admin/root/api/settlement-execute/run', {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify(latestPayload)
		});
		const body = await toJsonSafe(res);
		setLoading(false);

		if (!res.ok || !body || !body.success) {
			alert(body?.message || '정산 실행 실패');
			return;
		}

		const data = body.data || {};
		const createdItems = Array.isArray(data.createdItems) ? data.createdItems : [];

		alert(`정산 실행 완료\n배치ID: ${data.batchId}\n생성건수: ${data.createdCount || 0}`);

		renderRows(createdItems);

		const summary = summarizeRows(createdItems);
		renderSummary(summary);

		latestPreview = {
			count: summary.count,
			items: createdItems,
			totalGrossAmount: summary.totalGrossAmount,
			totalCommissionAmount: summary.totalCommissionAmount,
			totalSettlementAmount: summary.totalSettlementAmount
		};
	}

	function reset() {
		document.getElementById('settlement-excute-from-date').value = '';
		document.getElementById('settlement-excute-to-date').value = '';
		document.getElementById('settlement-excute-keyword').value = '';
		document.querySelectorAll('input[id^="settlement-excute-cycle-"], input[id^="settlement-excute-basis-"]').forEach(x => x.checked = false);

		latestPayload = null;
		latestPreview = null;
		renderSummary({ count: 0, totalGrossAmount: 0, totalCommissionAmount: 0, totalSettlementAmount: 0 });
		tbody.innerHTML = `<tr><td colspan="9" class="text-center text-muted py-5">검색 후 생성 예정 정산내역이 표시됩니다.</td></tr>`;
	}

	btnSearch.addEventListener('click', preview);
	btnRun.addEventListener('click', run);
	btnReset.addEventListener('click', reset);
})();