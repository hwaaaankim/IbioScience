(function() {
	'use strict';

	const form = document.getElementById('client-apply-manager-filter-form');
	const pageInput = document.getElementById('client-apply-manager-page');
	const sortKeyInput = document.getElementById('client-apply-manager-sortKey');
	const sortDirInput = document.getElementById('client-apply-manager-sortDir');

	const checkAll = document.getElementById('client-apply-manager-check-all');
	const rowChecks = () => Array.from(document.querySelectorAll('.client-apply-manager-row-check'));
	const bulkBtn = document.getElementById('client-apply-manager-bulk-approve-btn');

	const resetBtn = document.getElementById('client-apply-manager-reset-btn');

	const modalEl = document.getElementById('client-apply-manager-detail-modal');
	const modalBody = document.getElementById('client-apply-manager-modal-body');
	const modalApproveBtn = document.getElementById('client-apply-manager-modal-approve-btn');
	const bsModal = modalEl ? new bootstrap.Modal(modalEl) : null;

	let currentMemberId = null;

	function getCsrf() {
		const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
		const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
		if (!token || !header) return null;
		return { token, header };
	}

	function submitWithPage(page) {
		pageInput.value = String(page);
		form.submit();
	}

	function setSort(sortKey) {
		const currentKey = (sortKeyInput.value || '').trim();
		const currentDir = (sortDirInput.value || 'desc').toLowerCase();

		let nextDir = 'desc';
		if (currentKey === sortKey) {
			nextDir = (currentDir === 'desc') ? 'asc' : 'desc';
		} else {
			nextDir = 'desc';
		}

		sortKeyInput.value = sortKey;
		sortDirInput.value = nextDir;
		pageInput.value = '0';
		form.submit();
	}

	function updateSortIcons() {
		const currentKey = (sortKeyInput.value || '').trim();
		const currentDir = (sortDirInput.value || 'desc').toLowerCase();

		document.querySelectorAll('.client-apply-manager-sort-btn').forEach(btn => {
			const key = btn.getAttribute('data-sort-key');
			const icons = btn.querySelector('.client-apply-manager-sort-icons');
			if (!icons) return;

			icons.querySelectorAll('span').forEach(sp => sp.classList.remove('active'));

			if (key === currentKey) {
				const target = icons.querySelector(`span[data-dir="${currentDir}"]`);
				if (target) target.classList.add('active');
			}
		});
	}

	function updateBulkState() {
		const checks = rowChecks();
		const checked = checks.filter(ch => ch.checked);

		bulkBtn.disabled = checked.length === 0;

		if (checks.length === 0) {
			checkAll.checked = false;
			checkAll.indeterminate = false;
			return;
		}

		if (checked.length === 0) {
			checkAll.checked = false;
			checkAll.indeterminate = false;
		} else if (checked.length === checks.length) {
			checkAll.checked = true;
			checkAll.indeterminate = false;
		} else {
			checkAll.checked = false;
			checkAll.indeterminate = true;
		}
	}

	async function apiFetch(url, options) {
		const csrf = getCsrf();
		const opt = options || {};
		opt.headers = opt.headers || {};

		if (csrf) {
			opt.headers[csrf.header] = csrf.token;
		}
		return fetch(url, opt);
	}

	function safeText(v) {
		if (v === null || v === undefined) return '-';
		const s = String(v).trim();
		return s.length ? s : '-';
	}

	function formatJoinedAt(joinedAt) {
		if (!joinedAt) return '-';
		// LocalDateTime 직렬화: "2026-02-26T12:34:56"
		const s = String(joinedAt);
		if (s.includes('T')) {
			return s.replace('T', ' ').substring(0, 16);
		}
		return s;
	}

	function renderDetail(dto) {
		const isBusiness = dto.customerType === 'BUSINESS';

		const memberBlock = `
      <div class="client-apply-manager-kv">
        <div class="client-apply-manager-modal-label">아이디</div><div class="value">${safeText(dto.username)}</div>
        <div class="client-apply-manager-modal-label">이름</div><div class="value">${safeText(dto.name)}</div>
        <div class="client-apply-manager-modal-label">연락처</div><div class="value">${safeText(dto.mobile) !== '-' ? safeText(dto.mobile) : safeText(dto.tel)}</div>
        <div class="client-apply-manager-modal-label">이메일</div><div class="value">${safeText(dto.email)}</div>
        <div class="client-apply-manager-modal-label">가입신청일</div><div class="value">${formatJoinedAt(dto.joinedAt)}</div>
        <div class="client-apply-manager-modal-label">주소</div>
        <div class="value">
          (${safeText(dto.aPostcode)}) ${safeText(dto.aRoadAddress)} ${safeText(dto.aDetailAddress)}
        </div>
      </div>
    `;

		const personalExtra = `
      <div class="client-apply-manager-divider"></div>
      <div class="client-apply-manager-kv">
        <div class="client-apply-manager-modal-label">업체/기관명(개인)</div><div class="value">${safeText(dto.organizationName)}</div>
      </div>
    `;

		const businessBlock = `
      <div class="client-apply-manager-divider"></div>
      <h6 class="mb-2">사업자 정보</h6>
      <div class="client-apply-manager-kv">
        <div class="client-apply-manager-modal-label">회사명</div><div class="value">${safeText(dto.companyName)}</div>
        <div class="client-apply-manager-modal-label">부서</div><div class="value">${safeText(dto.department)}</div>
        <div class="client-apply-manager-modal-label">대표자명</div><div class="value">${safeText(dto.ceoName)}</div>
        <div class="client-apply-manager-modal-label">업태</div><div class="value">${safeText(dto.businessType)}</div>
        <div class="client-apply-manager-modal-label">종목</div><div class="value">${safeText(dto.businessItem)}</div>
        <div class="client-apply-manager-modal-label">대표전화</div><div class="value">${safeText(dto.representativeTel)}</div>
        <div class="client-apply-manager-modal-label">팩스</div><div class="value">${safeText(dto.fax)}</div>
        <div class="client-apply-manager-modal-label">세금계산서 이메일</div><div class="value">${safeText(dto.invoiceEmail)}</div>
        <div class="client-apply-manager-modal-label">사업자등록번호</div><div class="value">${safeText(dto.businessRegistrationNumber)}</div>
        <div class="client-apply-manager-modal-label">사업장 주소</div>
        <div class="value">
          (${safeText(dto.cPostcode)}) ${safeText(dto.cRoadAddress)} ${safeText(dto.cDetailAddress)}
        </div>
      </div>

      <div class="client-apply-manager-divider"></div>
      <h6 class="mb-2">사업자등록증 이미지</h6>
      ${dto.businessRegImageRoad
				? `<img src="${dto.businessRegImageRoad}" alt="사업자등록증" class="img-fluid rounded border" />`
				: `<div class="text-muted">등록된 이미지가 없습니다.</div>`
			}
    `;

		const title = isBusiness ? '기업회원 신청 상세' : '일반회원 신청 상세';

		return `
      <div class="mb-2"><span class="badge bg-info">${title}</span></div>
      ${memberBlock}
      ${isBusiness ? businessBlock : personalExtra}
    `;
	}

	async function openDetailModal(memberId) {
		currentMemberId = memberId;
		modalBody.innerHTML = '불러오는 중...';
		modalApproveBtn.disabled = true;

		bsModal.show();

		try {
			const res = await apiFetch(`/admin/root/api/clientApplyManager/pending/${memberId}`, { method: 'GET' });
			const json = await res.json();

			if (!json || !json.success) {
				modalBody.innerHTML = `<div class="text-danger">${safeText(json?.message)}</div>`;
				modalApproveBtn.disabled = true;
				return;
			}

			modalBody.innerHTML = renderDetail(json.data);
			modalApproveBtn.disabled = false;
		} catch (e) {
			modalBody.innerHTML = `<div class="text-danger">상세 조회 중 오류가 발생했습니다.</div>`;
			modalApproveBtn.disabled = true;
		}
	}

	async function approveOne(memberId) {
		const res = await apiFetch(`/admin/root/api/clientApplyManager/approve/${memberId}`, {
			method: 'POST'
		});
		const json = await res.json();
		if (!json || !json.success) {
			alert(safeText(json?.message));
			return false;
		}
		return true;
	}

	async function approveBulk(ids) {
		const res = await apiFetch(`/admin/root/api/clientApplyManager/approve/bulk`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ memberIds: ids })
		});
		const json = await res.json();
		if (!json || !json.success) {
			alert(safeText(json?.message));
			return false;
		}
		return true;
	}

	// ===== 이벤트 바인딩 =====
	document.addEventListener('DOMContentLoaded', function() {
		updateSortIcons();
		updateBulkState();

		// 정렬 버튼
		document.querySelectorAll('.client-apply-manager-sort-btn').forEach(btn => {
			btn.addEventListener('click', function() {
				const key = btn.getAttribute('data-sort-key');
				setSort(key);
			});
		});

		// 페이지네이션
		document.querySelectorAll('.client-apply-manager-page-link').forEach(a => {
			a.addEventListener('click', function() {
				const li = a.closest('li');
				if (li && li.classList.contains('disabled')) return;
				const p = a.getAttribute('data-page');
				if (p === null || p === undefined) return;
				submitWithPage(Number(p));
			});
		});

		// 전체 체크
		if (checkAll) {
			checkAll.addEventListener('change', function() {
				const checked = checkAll.checked;
				rowChecks().forEach(ch => ch.checked = checked);
				updateBulkState();
			});
		}

		// 개별 체크
		document.addEventListener('change', function(e) {
			if (e.target && e.target.classList.contains('client-apply-manager-row-check')) {
				updateBulkState();
			}
		});

		// 초기화
		if (resetBtn) {
			resetBtn.addEventListener('click', function() {
				document.getElementById('client-apply-manager-size').value = '10';
				document.getElementById('client-apply-manager-fromDate').value = '';
				document.getElementById('client-apply-manager-toDate').value = '';
				document.getElementById('client-apply-manager-searchField').value = 'USERNAME';
				document.getElementById('client-apply-manager-keyword').value = '';
				document.getElementById('client-apply-manager-apply-all').checked = true;

				sortKeyInput.value = 'joinedAt';
				sortDirInput.value = 'desc';
				pageInput.value = '0';
				form.submit();
			});
		}

		// 행 승인처리(모달)
		document.addEventListener('click', function(e) {
			const btn = e.target?.closest?.('.client-apply-manager-open-modal');
			if (!btn) return;
			const id = btn.getAttribute('data-member-id');
			if (!id) return;
			openDetailModal(Number(id));
		});

		// 모달 승인하기
		if (modalApproveBtn) {
			modalApproveBtn.addEventListener('click', async function() {
				if (!currentMemberId) return;

				if (!confirm('해당 신청을 승인 처리하시겠습니까?')) return;

				modalApproveBtn.disabled = true;
				try {
					const ok = await approveOne(currentMemberId);
					if (ok) {
						bsModal.hide();
						// 현재 조건 유지한 채로 재조회
						form.submit();
					} else {
						modalApproveBtn.disabled = false;
					}
				} catch (e) {
					alert('승인 처리 중 오류가 발생했습니다.');
					modalApproveBtn.disabled = false;
				}
			});
		}

		// 일괄 승인
		if (bulkBtn) {
			bulkBtn.addEventListener('click', async function() {
				const ids = rowChecks().filter(ch => ch.checked).map(ch => Number(ch.value));
				if (ids.length === 0) return;

				if (!confirm(`선택한 ${ids.length}건을 상세 확인 없이 일괄 승인 처리하시겠습니까?`)) return;

				bulkBtn.disabled = true;
				try {
					const ok = await approveBulk(ids);
					if (ok) {
						form.submit();
					} else {
						updateBulkState();
					}
				} catch (e) {
					alert('일괄 승인 처리 중 오류가 발생했습니다.');
					updateBulkState();
				}
			});
		}
	});
})();