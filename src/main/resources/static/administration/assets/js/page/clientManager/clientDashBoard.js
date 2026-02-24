/* /administration/assets/js/page/clientManager/clientDashBoard.js */
(function () {
	'use strict';

	const state = {
		date: (window.__CLIENT_DASHBOARD__ && window.__CLIENT_DASHBOARD__.selectedDate) ? window.__CLIENT_DASHBOARD__.selectedDate : '',
		selectedAction: null,
		selectedLabel: null,
		page: 0,
		size: 10,
		totalPages: 0,
	};

	const elDate = document.getElementById('client-dashboard-date');
	const elBtnSearch = document.getElementById('client-dashboard-btn-search');
	const elCards = document.getElementById('client-dashboard-summary-cards');

	const elDetailTitle = document.getElementById('client-dashboard-detail-title');
	const elDetailSubtitle = document.getElementById('client-dashboard-detail-subtitle');
	const elSelectedChip = document.getElementById('client-dashboard-selected-chip');
	const elSelectedLabel = document.getElementById('client-dashboard-selected-label');

	const elTbody = document.getElementById('client-dashboard-detail-tbody');
	const elPagination = document.getElementById('client-dashboard-pagination');

	const numberFmt = new Intl.NumberFormat('ko-KR');

	function escapeHtml(str) {
		if (str === null || str === undefined) return '';
		return String(str)
			.replaceAll('&', '&amp;')
			.replaceAll('<', '&lt;')
			.replaceAll('>', '&gt;')
			.replaceAll('"', '&quot;')
			.replaceAll("'", '&#039;');
	}

	function setLoadingCards() {
		elCards.innerHTML = `
			<div class="col-12">
				<div class="text-muted">요약 정보를 불러오는 중입니다...</div>
			</div>
		`;
	}

	function setLoadingTable() {
		elTbody.innerHTML = `
			<tr>
				<td colspan="7" class="text-center text-muted py-5">
					목록을 불러오는 중입니다...
				</td>
			</tr>
		`;
		elPagination.innerHTML = '';
	}

	function renderCards(summary) {
		// summary 구조:
		// {
		//   date: "2026-02-24",
		//   visitPv: 123,
		//   visitUv: 45,
		//   items: [{key, label, count, drilldown}]
		// }

		const cards = summary.items || [];

		elCards.innerHTML = cards.map(item => {
			const isEmpty = !item.count || item.count <= 0;
			const valueHtml = isEmpty
				? `<div class="client-dashboard-metric-value client-dashboard-empty">없음</div>`
				: `<div class="client-dashboard-metric-value">${numberFmt.format(item.count)}</div>`;

			const disabledClass = item.drilldown ? '' : 'client-dashboard-disabled';
			const buttonHtml = item.drilldown
				? `<button type="button" class="btn btn-outline-primary btn-sm client-dashboard-btn-open"
						data-action="${escapeHtml(item.key)}"
						data-label="${escapeHtml(item.label)}">
						상세보기
					</button>`
				: `<button type="button" class="btn btn-outline-secondary btn-sm" disabled>
						상세없음
					</button>`;

			return `
				<div class="col-12 col-md-6 col-lg-3">
					<div class="client-dashboard-metric ${disabledClass}">
						<div class="client-dashboard-metric-title">${escapeHtml(item.label)}</div>
						<div class="client-dashboard-metric-sub">${escapeHtml(summary.date)} 기준</div>
						${valueHtml}
						<div class="client-dashboard-metric-action">
							${buttonHtml}
						</div>
					</div>
				</div>
			`;
		}).join('');

		// 카드 버튼 클릭 핸들링 (event delegation)
	}

	function renderTable(pageResult) {
		// pageResult:
		// { content: [...], number, size, totalPages, totalElements }

		const rows = pageResult.content || [];
		if (rows.length === 0) {
			elTbody.innerHTML = `
				<tr>
					<td colspan="7" class="text-center text-muted py-5">
						해당 날짜에 기록이 없습니다.
					</td>
				</tr>
			`;
			return;
		}

		elTbody.innerHTML = rows.map(r => {
			const dealerBadge = r.dealerType && r.dealerType !== 'NONE'
				? `<span class="client-dashboard-badge client-dashboard-badge-dealer">딜러</span>`
				: `<span class="client-dashboard-badge client-dashboard-badge-nondealer">일반</span>`;

			return `
				<tr>
					<td>${escapeHtml(r.loggedAt)}</td>
					<td>${escapeHtml(r.username)}</td>
					<td>${escapeHtml(r.name)}</td>
					<td>${escapeHtml(r.customerType)}</td>
					<td>${escapeHtml(r.role)}</td>
					<td>${dealerBadge}</td>
					<td>${escapeHtml(r.domain)}</td>
				</tr>
			`;
		}).join('');
	}

	function renderPagination(pageResult) {
		const totalPages = pageResult.totalPages || 0;
		const current = pageResult.number || 0;

		state.totalPages = totalPages;

		// totalPages=0이면 pagination 비움
		if (totalPages <= 1) {
			elPagination.innerHTML = '';
			return;
		}

		const lastIndex = totalPages - 1;

		function pageItem(label, targetPage, disabled, active, dataRole) {
			const liClass = ['page-item'];
			if (disabled) liClass.push('disabled');
			if (active) liClass.push('active');

			const attrs = [];
			if (!disabled && targetPage !== null && targetPage !== undefined) {
				attrs.push(`data-page="${targetPage}"`);
			}
			if (dataRole) attrs.push(`data-role="${dataRole}"`);

			return `
				<li class="${liClass.join(' ')}">
					<a class="page-link" href="javascript:void(0);" ${attrs.join(' ')}>
						${label}
					</a>
				</li>
			`;
		}

		// 숫자 5개만 노출
		let start = Math.max(0, current - 2);
		let end = Math.min(lastIndex, start + 4);
		start = Math.max(0, end - 4);

		let html = '';
		html += pageItem('First', 0, current === 0, false, 'first');
		html += pageItem('Prev', Math.max(0, current - 1), current === 0, false, 'prev');

		for (let p = start; p <= end; p++) {
			html += pageItem(String(p + 1), p, false, p === current, 'num');
		}

		html += pageItem('Next', Math.min(lastIndex, current + 1), current === lastIndex, false, 'next');
		html += pageItem('Last', lastIndex, current === lastIndex, false, 'last');

		elPagination.innerHTML = html;
	}

	async function fetchSummary(dateStr) {
		const url = `/administration/api/client-dashboard/summary?date=${encodeURIComponent(dateStr)}`;
		const res = await fetch(url, { headers: { 'Accept': 'application/json' } });
		if (!res.ok) {
			throw new Error('요약 조회 실패');
		}
		return await res.json();
	}

	async function fetchLogs(dateStr, action, page, size) {
		const url =
			`/administration/api/client-dashboard/logs?date=${encodeURIComponent(dateStr)}&action=${encodeURIComponent(action)}&page=${page}&size=${size}`;
		const res = await fetch(url, { headers: { 'Accept': 'application/json' } });
		if (!res.ok) {
			throw new Error('로그 목록 조회 실패');
		}
		return await res.json();
	}

	function setSelectedAction(action, label) {
		state.selectedAction = action;
		state.selectedLabel = label;

		elDetailTitle.textContent = '상세 목록';
		elDetailSubtitle.textContent = `${state.date} 기준 ${label} 로그 목록입니다.`;

		elSelectedChip.classList.remove('d-none');
		elSelectedLabel.textContent = label;
	}

	function clearSelectedAction() {
		state.selectedAction = null;
		state.selectedLabel = null;
		state.page = 0;

		elDetailTitle.textContent = '상세 목록';
		elDetailSubtitle.textContent = '상단에서 로그 항목을 선택해주세요.';
		elSelectedChip.classList.add('d-none');

		elTbody.innerHTML = `
			<tr>
				<td colspan="7" class="text-center text-muted py-5">
					상단 카드에서 로그 항목을 선택하면 목록이 표시됩니다.
				</td>
			</tr>
		`;
		elPagination.innerHTML = '';
	}

	async function loadSummary() {
		try {
			setLoadingCards();
			const summary = await fetchSummary(state.date);
			renderCards(summary);
			// summary 갱신 시, 기존 선택 액션은 유지하되(선택되어 있으면) 재조회는 별도
		} catch (e) {
			elCards.innerHTML = `
				<div class="col-12">
					<div class="alert alert-danger mb-0">
						요약 정보를 불러오지 못했습니다. (네트워크/권한/서버 로그 확인)
					</div>
				</div>
			`;
		}
	}

	async function loadLogs(page) {
		if (!state.selectedAction) return;

		try {
			setLoadingTable();
			const result = await fetchLogs(state.date, state.selectedAction, page, state.size);
			state.page = result.number || 0;
			renderTable(result);
			renderPagination(result);
		} catch (e) {
			elTbody.innerHTML = `
				<tr>
					<td colspan="7" class="text-center text-danger py-5">
						목록을 불러오지 못했습니다. (네트워크/권한/서버 로그 확인)
					</td>
				</tr>
			`;
			elPagination.innerHTML = '';
		}
	}

	function bindEvents() {
		// 조회 버튼
		elBtnSearch.addEventListener('click', async () => {
			const v = elDate.value;
			if (!v) return;
			state.date = v;
			await loadSummary();

			// 날짜가 바뀌면 선택된 액션은 유지하되, 테이블은 다시 로딩
			if (state.selectedAction) {
				await loadLogs(0);
			} else {
				clearSelectedAction();
			}
		});

		// 카드 "상세보기" 버튼 클릭 (event delegation)
		elCards.addEventListener('click', async (e) => {
			const btn = e.target.closest('.client-dashboard-btn-open');
			if (!btn) return;

			const action = btn.getAttribute('data-action');
			const label = btn.getAttribute('data-label');
			if (!action) return;

			setSelectedAction(action, label);
			await loadLogs(0);
		});

		// pagination 클릭 (event delegation)
		elPagination.addEventListener('click', async (e) => {
			const a = e.target.closest('a.page-link');
			if (!a) return;
			const pageStr = a.getAttribute('data-page');
			if (pageStr === null || pageStr === undefined) return;

			const page = parseInt(pageStr, 10);
			if (Number.isNaN(page)) return;

			await loadLogs(page);
		});
	}

	// init
	(function init() {
		if (elDate && state.date) elDate.value = state.date;

		bindEvents();
		loadSummary();
	})();
})();