// src/main/resources/static/administration/assets/js/page/clientManager/clientWithDrawManager.js
(() => {
	'use strict';

	const qs = (sel) => document.querySelector(sel);
	const qsa = (sel) => Array.from(document.querySelectorAll(sel));

	function getCsrf() {
		const token = qs('#client-withdraw-manager-csrf-token')?.value || '';
		const header = qs('#client-withdraw-manager-csrf-header')?.value || '';
		return { token, header };
	}

	function setBulkButtonState() {
		const anyChecked = qsa('.client-withdraw-manager-row-check').some(chk => chk.checked);
		const btn = qs('#client-withdraw-manager-bulk-approve-btn');
		if (btn) btn.disabled = !anyChecked;
	}

	function handleCheckAll() {
		const all = qs('#client-withdraw-manager-check-all');
		if (!all) return;

		all.addEventListener('change', () => {
			const checked = all.checked;
			qsa('.client-withdraw-manager-row-check').forEach(chk => chk.checked = checked);
			setBulkButtonState();
		});
	}

	function handleRowChecks() {
		qsa('.client-withdraw-manager-row-check').forEach(chk => {
			chk.addEventListener('change', () => {
				const all = qs('#client-withdraw-manager-check-all');
				if (all) {
					const rows = qsa('.client-withdraw-manager-row-check');
					all.checked = rows.length > 0 && rows.every(x => x.checked);
				}
				setBulkButtonState();
			});
		});
	}

	async function apiFetch(url, options = {}) {
		const { token, header } = getCsrf();
		const headers = options.headers || {};

		if (token && header) headers[header] = token;

		const res = await fetch(url, {
			credentials: 'same-origin',
			...options,
			headers
		});

		const json = await res.json().catch(() => null);
		if (!res.ok) {
			const msg = json?.message || `요청 실패 (${res.status})`;
			throw new Error(msg);
		}
		if (json && json.success === false) {
			throw new Error(json.message || '처리 실패');
		}
		return json;
	}

	function renderDetailModal(data) {
		const body = qs('#client-withdraw-manager-modal-body');
		if (!body) return;

		const isBusiness = data?.customerType === 'BUSINESS';

		const safe = (v) => (v === null || v === undefined || String(v).trim() === '' ? '-' : v);

		const accountHtml = `
			<div class="row g-3">
				<div class="col-12">
					<h6 class="mb-2">계정 정보</h6>
					<dl class="row client-withdraw-manager-modal-kv">
						<dt class="col-sm-2 text-muted">아이디</dt><dd class="col-sm-10">${safe(data.username)}</dd>
						<dt class="col-sm-2 text-muted">이름</dt><dd class="col-sm-10">${safe(data.name)}</dd>
						<dt class="col-sm-2 text-muted">연락처</dt><dd class="col-sm-10">${safe(data.mobile || data.tel)}</dd>
						<dt class="col-sm-2 text-muted">이메일</dt><dd class="col-sm-10">${safe(data.email)}</dd>
						<dt class="col-sm-2 text-muted">회원유형</dt><dd class="col-sm-10">${safe(data.customerType)}</dd>
						<dt class="col-sm-2 text-muted">딜러유형</dt><dd class="col-sm-10">${safe(data.dealerType)}</dd>
						<dt class="col-sm-2 text-muted">상태</dt><dd class="col-sm-10">${safe(data.status)}</dd>
						<dt class="col-sm-2 text-muted">가입일</dt><dd class="col-sm-10">${safe(data.joinedAt)}</dd>
						<dt class="col-sm-2 text-muted">탈퇴신청일</dt><dd class="col-sm-10">${safe(data.withdrewAt)}</dd>
					</dl>
				</div>
			</div>
		`;

		let companyHtml = '';
		if (isBusiness) {
			const imgHtml = data.businessRegImageRoad
				? `<img src="${data.businessRegImageRoad}" alt="사업자등록증" class="client-withdraw-manager-img">`
				: `<div class="text-muted">사업자등록증 이미지가 없습니다.</div>`;

			companyHtml = `
				<hr class="my-3"/>
				<div class="row g-3">
					<div class="col-12">
						<h6 class="mb-2">사업자 정보</h6>
					</div>
					<div class="col-lg-7">
						<dl class="row client-withdraw-manager-modal-kv">
							<dt class="col-sm-3 text-muted">업체명</dt><dd class="col-sm-9">${safe(data.companyName)}</dd>
							<dt class="col-sm-3 text-muted">부서</dt><dd class="col-sm-9">${safe(data.department)}</dd>
							<dt class="col-sm-3 text-muted">대표자</dt><dd class="col-sm-9">${safe(data.ceoName)}</dd>
							<dt class="col-sm-3 text-muted">사업자번호</dt><dd class="col-sm-9">${safe(data.businessRegistrationNumber)}</dd>
							<dt class="col-sm-3 text-muted">업태</dt><dd class="col-sm-9">${safe(data.businessType)}</dd>
							<dt class="col-sm-3 text-muted">종목</dt><dd class="col-sm-9">${safe(data.businessItem)}</dd>
							<dt class="col-sm-3 text-muted">대표전화</dt><dd class="col-sm-9">${safe(data.representativeTel)}</dd>
							<dt class="col-sm-3 text-muted">팩스</dt><dd class="col-sm-9">${safe(data.fax)}</dd>
							<dt class="col-sm-3 text-muted">세금계산서 이메일</dt><dd class="col-sm-9">${safe(data.invoiceEmail)}</dd>
							<dt class="col-sm-3 text-muted">주소</dt>
							<dd class="col-sm-9">
								${safe(data.companyPostcode)}<br/>
								${safe(data.companyRoadAddress)}<br/>
								${safe(data.companyDetailAddress)}
							</dd>
						</dl>
					</div>
					<div class="col-lg-5">
						<h6 class="mb-2">사업자등록증</h6>
						${imgHtml}
					</div>
				</div>
			`;
		} else {
			companyHtml = `
				<hr class="my-3"/>
				<div class="row g-3">
					<div class="col-12">
						<h6 class="mb-2">일반회원 정보</h6>
						<dl class="row client-withdraw-manager-modal-kv">
							<dt class="col-sm-2 text-muted">기관/업체명</dt><dd class="col-sm-10">${safe(data.organizationName)}</dd>
						</dl>
					</div>
				</div>
			`;
		}

		body.innerHTML = accountHtml + companyHtml;
	}

	function handleApproveButtons() {
		const modalEl = qs('#client-withdraw-manager-approve-modal');
		if (!modalEl) return;
		const modal = new bootstrap.Modal(modalEl);

		qsa('.client-withdraw-manager-approve-btn').forEach(btn => {
			btn.addEventListener('click', async () => {
				const memberId = btn.getAttribute('data-member-id');
				if (!memberId) return;

				try {
					const json = await apiFetch(`/admin/root/api/clientWithdraw/${memberId}`, { method: 'GET' });
					const data = json?.data;
					renderDetailModal(data);

					const approveBtn = qs('#client-withdraw-manager-modal-approve-btn');
					if (approveBtn) approveBtn.setAttribute('data-member-id', memberId);

					modal.show();
				} catch (e) {
					alert(e.message || '상세 조회 실패');
				}
			});
		});

		const approveBtn = qs('#client-withdraw-manager-modal-approve-btn');
		if (approveBtn) {
			approveBtn.addEventListener('click', async () => {
				const memberId = approveBtn.getAttribute('data-member-id');
				if (!memberId) return;

				if (!confirm('해당 회원의 탈퇴신청을 승인하시겠습니까? (DELETED 처리)')) return;

				try {
					await apiFetch(`/admin/root/api/clientWithdraw/approve/${memberId}`, { method: 'POST' });
					modal.hide();
					window.location.reload();
				} catch (e) {
					alert(e.message || '승인 실패');
				}
			});
		}
	}

	function handleBulkApprove() {
		const btn = qs('#client-withdraw-manager-bulk-approve-btn');
		if (!btn) return;

		btn.addEventListener('click', async () => {
			const ids = qsa('.client-withdraw-manager-row-check')
				.filter(chk => chk.checked)
				.map(chk => Number(chk.value))
				.filter(v => !Number.isNaN(v));

			if (ids.length === 0) return;

			if (!confirm(`선택한 ${ids.length}건을 일괄 승인하시겠습니까? (상세 확인 없이 DELETED 처리)`)) return;

			try {
				const json = await apiFetch('/admin/root/api/clientWithdraw/approve-bulk', {
					method: 'POST',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify({ memberIds: ids })
				});

				const failed = json?.data?.failedIds || [];
				if (failed.length > 0) {
					alert(`일괄 승인 완료. 일부 실패: ${failed.join(', ')}`);
				} else {
					alert('일괄 승인 완료');
				}
				window.location.reload();
			} catch (e) {
				alert(e.message || '일괄 승인 실패');
			}
		});
	}

	function handleAutoSubmitSize() {
		const sizeSel = qs('#client-withdraw-manager-size');
		const form = qs('#client-withdraw-manager-search-form');
		if (!sizeSel || !form) return;

		sizeSel.addEventListener('change', () => {
			// size 변경 시 첫 페이지로
			const pageInput = form.querySelector('input[name="page"]');
			if (pageInput) pageInput.value = '0';
			form.submit();
		});
	}

	document.addEventListener('DOMContentLoaded', () => {
		handleCheckAll();
		handleRowChecks();
		setBulkButtonState();
		handleApproveButtons();
		handleBulkApprove();
		handleAutoSubmitSize();
	});
})();