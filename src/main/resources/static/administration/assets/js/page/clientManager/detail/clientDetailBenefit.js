(function() {
	'use strict';

	const memberId = window.CLIENT_DETAIL_BENEFIT_MANAGER_MEMBER_ID;
	if (!memberId) {
		return;
	}

	const baseUrl = `/admin/root/api/clientDetail/${memberId}/benefit`;

	const state = {
		pointPage: 0,
		couponPage: 0
	};

	document.addEventListener('DOMContentLoaded', init);

	function init() {
		bindEvents();
		loadPointSummary();
		loadPointHistories(0);
		loadCoupons(0);
	}

	function bindEvents() {
		const pointSearchBtn = document.getElementById('client-detail-benefitManager-point-search-btn');
		const pointGrantBtn = document.getElementById('client-detail-benefitManager-point-grant-btn');
		const pointDeductBtn = document.getElementById('client-detail-benefitManager-point-deduct-btn');
		const couponSearchBtn = document.getElementById('client-detail-benefitManager-coupon-search-btn');
		const couponGrantBtn = document.getElementById('client-detail-benefitManager-grant-coupon-btn');
		const couponTbody = document.getElementById('client-detail-benefitManager-coupon-tbody');

		if (pointSearchBtn) {
			pointSearchBtn.addEventListener('click', function() {
				loadPointHistories(0);
			});
		}

		if (pointGrantBtn) {
			pointGrantBtn.addEventListener('click', async function() {
				const amount = getPositiveNumberValue('client-detail-benefitManager-point-amount');
				if (!amount) {
					alert('적립금 금액을 정확히 입력해주세요.');
					return;
				}

				try {
					await fetchJson(`${baseUrl}/point/grant`, {
						method: 'POST',
						body: JSON.stringify({ amount: amount })
					});
					document.getElementById('client-detail-benefitManager-point-amount').value = '';
					await loadPointSummary();
					await loadPointHistories(state.pointPage);
					alert('적립금이 부여되었습니다.');
				} catch (e) {
					alert(e.message);
				}
			});
		}

		if (pointDeductBtn) {
			pointDeductBtn.addEventListener('click', async function() {
				const amount = getPositiveNumberValue('client-detail-benefitManager-point-amount');
				if (!amount) {
					alert('적립금 금액을 정확히 입력해주세요.');
					return;
				}

				try {
					await fetchJson(`${baseUrl}/point/deduct`, {
						method: 'POST',
						body: JSON.stringify({ amount: amount })
					});
					document.getElementById('client-detail-benefitManager-point-amount').value = '';
					await loadPointSummary();
					await loadPointHistories(state.pointPage);
					alert('적립금이 차감되었습니다.');
				} catch (e) {
					alert(e.message);
				}
			});
		}

		if (couponSearchBtn) {
			couponSearchBtn.addEventListener('click', function() {
				loadCoupons(0);
			});
		}

		if (couponGrantBtn) {
			couponGrantBtn.addEventListener('click', async function() {
				const payload = {
					couponCode: getValue('client-detail-benefitManager-grant-coupon-code'),
					couponName: getValue('client-detail-benefitManager-grant-coupon-name'),
					minPurchaseAmount: getValue('client-detail-benefitManager-grant-min-purchase'),
					couponAmount: getValue('client-detail-benefitManager-grant-coupon-amount'),
					couponPolicy: getValue('client-detail-benefitManager-grant-policy'),
					startDate: getValue('client-detail-benefitManager-grant-start-date'),
					endDate: getValue('client-detail-benefitManager-grant-end-date')
				};

				if (!payload.couponCode || !payload.couponName || !payload.couponPolicy || !payload.startDate || !payload.endDate) {
					alert('쿠폰코드, 쿠폰명, 쿠폰정책, 시작일, 종료일은 필수입니다.');
					return;
				}

				try {
					await fetchJson(`${baseUrl}/coupons/grant`, {
						method: 'POST',
						body: JSON.stringify(payload)
					});

					resetCouponGrantForm();
					await loadCoupons(0);
					alert('쿠폰이 발급되었습니다.');
				} catch (e) {
					alert(e.message);
				}
			});
		}

		if (couponTbody) {
			couponTbody.addEventListener('click', async function(event) {
				const sourceBtn = event.target.closest('[data-role="coupon-source-detail"]');
				if (sourceBtn) {
					const memberCouponId = sourceBtn.getAttribute('data-member-coupon-id');
					await openCouponSourceModal(memberCouponId);
					return;
				}

				const deleteBtn = event.target.closest('[data-role="coupon-delete"]');
				if (deleteBtn) {
					const memberCouponId = deleteBtn.getAttribute('data-member-coupon-id');
					await deleteCoupon(memberCouponId);
				}
			});
		}
	}

	async function loadPointSummary() {
		const summary = await fetchJson(`${baseUrl}/point-summary`);
		const currentPointEl = document.getElementById('client-detail-benefitManager-current-point');
		if (currentPointEl) {
			currentPointEl.textContent = formatNumber(summary.currentPoint || 0);
		}
	}

	async function loadPointHistories(page) {
		state.pointPage = page;

		const params = new URLSearchParams();
		params.set('page', page);

		const fromDate = getValue('client-detail-benefitManager-point-from');
		const toDate = getValue('client-detail-benefitManager-point-to');

		if (fromDate) params.set('fromDate', fromDate);
		if (toDate) params.set('toDate', toDate);

		const response = await fetchJson(`${baseUrl}/point-histories?${params.toString()}`);
		renderPointTable(response.content || []);
		renderPagination(
			'client-detail-benefitManager-point-pagination',
			response,
			loadPointHistories
		);
	}

	async function loadCoupons(page) {
		state.couponPage = page;

		const params = new URLSearchParams();
		params.set('page', page);

		const fromDate = getValue('client-detail-benefitManager-coupon-from');
		const toDate = getValue('client-detail-benefitManager-coupon-to');

		if (fromDate) params.set('fromDate', fromDate);
		if (toDate) params.set('toDate', toDate);

		document.querySelectorAll('.client-detail-benefitManager-coupon-status:checked').forEach(function(checkbox) {
			params.append('statuses', checkbox.value);
		});

		const response = await fetchJson(`${baseUrl}/coupons?${params.toString()}`);
		renderCouponTable(response.content || []);
		renderPagination(
			'client-detail-benefitManager-coupon-pagination',
			response,
			loadCoupons
		);
	}

	function renderPointTable(rows) {
		const tbody = document.getElementById('client-detail-benefitManager-point-tbody');
		if (!tbody) return;

		if (!rows.length) {
			tbody.innerHTML = `
                <tr>
                    <td colspan="5" class="client-detail-benefitManager-empty-row">조회된 적립금 내역이 없습니다.</td>
                </tr>
            `;
			return;
		}

		tbody.innerHTML = rows.map(function(row) {
			const isPlus = row.changeType === 'PLUS';
			const badgeClass = isPlus ? 'bg-success-subtle text-success' : 'bg-danger-subtle text-danger';
			const signText = isPlus ? '+' : '-';

			return `
                <tr>
                    <td>
                        <span class="badge ${badgeClass}">${signText}</span>
                    </td>
                    <td>${signText}${formatNumber(row.amount || 0)}</td>
                    <td>${escapeHtml(row.orderNo || '-')}</td>
                    <td>${escapeHtml(row.sourceText || '-')}</td>
                    <td>${formatDateTime(row.occurredAt)}</td>
                </tr>
            `;
		}).join('');
	}

	function renderCouponTable(rows) {
		const tbody = document.getElementById('client-detail-benefitManager-coupon-tbody');
		if (!tbody) return;

		if (!rows.length) {
			tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="client-detail-benefitManager-empty-row">조회된 쿠폰이 없습니다.</td>
                </tr>
            `;
			return;
		}

		tbody.innerHTML = rows.map(function(row) {
			return `
                <tr>
                    <td>${escapeHtml(row.couponName || '-')}</td>
                    <td>${formatDate(row.startDate)} ~ ${formatDate(row.endDate)}</td>
                    <td>${escapeHtml(row.statusLabel || '-')}</td>
                    <td>
                        <div class="d-flex" style="align-items:center;">
                            <span>${escapeHtml(row.sourceText || '-')}
                            <button type="button" class="btn btn-sm btn-outline-primary"
                                data-role="coupon-source-detail" style="width:80px; margin-left:10px; height:25px;"
                                data-member-coupon-id="${row.memberCouponId}">
                                상세보기
                            </button></span>
                        </div>
                    </td>
                    <td>${escapeHtml(row.usedOrderNo || '-')}</td>
                    <td>
                        <button type="button" class="btn btn-sm btn-outline-danger"
                            data-role="coupon-delete"
                            data-member-coupon-id="${row.memberCouponId}">
                            삭제
                        </button>
                    </td>
                </tr>
            `;
		}).join('');
	}

	async function openCouponSourceModal(memberCouponId) {
		try {
			const detail = await fetchJson(`${baseUrl}/coupons/${memberCouponId}/source`);
			const modalBody = document.getElementById('client-detail-benefitManager-coupon-source-modal-body');

			modalBody.innerHTML = `
                <div class="mb-2"><strong>쿠폰명</strong> : ${escapeHtml(detail.couponName || '-')}</div>
                <div class="mb-2"><strong>발급경로</strong> : ${escapeHtml(detail.issueSourceText || '-')}</div>
                <div class="mb-2"><strong>발급일시</strong> : ${formatDateTime(detail.issueOccurredAt)}</div>
                <div class="mb-2"><strong>발급주문번호</strong> : ${escapeHtml(detail.issueOrderNo || '-')}</div>
                <div class="mb-2"><strong>처리관리자</strong> : ${escapeHtml(detail.issueAdminUsername || '-')}</div>
                <hr>
                <div class="mb-2"><strong>사용주문번호</strong> : ${escapeHtml(detail.usedOrderNo || '-')}</div>
                <div class="mb-0"><strong>사용일시</strong> : ${formatDateTime(detail.usedOccurredAt)}</div>
            `;

			const modalEl = document.getElementById('client-detail-benefitManager-coupon-source-modal');
			const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
			modal.show();
		} catch (e) {
			alert(e.message);
		}
	}

	async function deleteCoupon(memberCouponId) {
		if (!confirm('해당 멤버의 쿠폰을 삭제할까요?')) {
			return;
		}

		try {
			await fetchJson(`${baseUrl}/coupons/${memberCouponId}`, {
				method: 'DELETE'
			});
			await loadCoupons(state.couponPage);
			alert('쿠폰이 삭제되었습니다.');
		} catch (e) {
			alert(e.message);
		}
	}

	function renderPagination(containerId, pageResponse, callback) {
		const container = document.getElementById(containerId);
		if (!container) return;

		container.innerHTML = '';

		const totalPages = pageResponse.totalPages || 0;
		const currentPage = pageResponse.page || 0;

		if (totalPages <= 1) {
			return;
		}

		const fragment = document.createDocumentFragment();

		fragment.appendChild(createPageButton('«', currentPage === 0, function() {
			callback(currentPage - 1);
		}));

		const start = Math.max(0, currentPage - 2);
		const end = Math.min(totalPages - 1, currentPage + 2);

		for (let i = start; i <= end; i++) {
			const active = i === currentPage;
			fragment.appendChild(createPageButton(String(i + 1), false, function() {
				callback(i);
			}, active));
		}

		fragment.appendChild(createPageButton('»', currentPage >= totalPages - 1, function() {
			callback(currentPage + 1);
		}));

		container.appendChild(fragment);
	}

	function createPageButton(text, disabled, onClick, active) {
		const button = document.createElement('button');
		button.type = 'button';
		button.className = active ? 'btn btn-primary' : 'btn btn-light';
		button.textContent = text;

		if (disabled) {
			button.disabled = true;
		} else {
			button.addEventListener('click', onClick);
		}

		return button;
	}

	async function fetchJson(url, options) {
		const config = options || {};
		config.headers = config.headers || {};

		const csrfToken = getCsrfToken();
		const csrfHeader = getCsrfHeader();

		if (csrfToken && csrfHeader) {
			config.headers[csrfHeader] = csrfToken;
		}

		if (config.body && !config.headers['Content-Type']) {
			config.headers['Content-Type'] = 'application/json';
		}

		const response = await fetch(url, config);

		if (!response.ok) {
			let message = '요청 처리 중 오류가 발생했습니다.';
			try {
				const contentType = response.headers.get('content-type') || '';
				if (contentType.includes('application/json')) {
					const errorBody = await response.json();
					message = errorBody.message || message;
				} else {
					const text = await response.text();
					if (text) {
						message = text;
					}
				}
			} catch (e) {
				// ignore
			}
			throw new Error(message);
		}

		const contentType = response.headers.get('content-type') || '';
		if (contentType.includes('application/json')) {
			return response.json();
		}
		return {};
	}

	function getPositiveNumberValue(id) {
		const value = Number(getValue(id));
		if (!Number.isFinite(value) || value <= 0) {
			return null;
		}
		return Math.floor(value);
	}

	function getValue(id) {
		const el = document.getElementById(id);
		return el ? String(el.value || '').trim() : '';
	}

	function resetCouponGrantForm() {
		[
			'client-detail-benefitManager-grant-coupon-code',
			'client-detail-benefitManager-grant-coupon-name',
			'client-detail-benefitManager-grant-min-purchase',
			'client-detail-benefitManager-grant-coupon-amount',
			'client-detail-benefitManager-grant-policy',
			'client-detail-benefitManager-grant-start-date',
			'client-detail-benefitManager-grant-end-date'
		].forEach(function(id) {
			const el = document.getElementById(id);
			if (!el) return;

			if (id === 'client-detail-benefitManager-grant-min-purchase' ||
				id === 'client-detail-benefitManager-grant-coupon-amount') {
				el.value = '0';
			} else {
				el.value = '';
			}
		});
	}

	function formatNumber(value) {
		return Number(value || 0).toLocaleString('ko-KR');
	}

	function formatDate(value) {
		if (!value) return '-';
		return value;
	}

	function formatDateTime(value) {
		if (!value) return '-';

		const date = new Date(value);
		if (Number.isNaN(date.getTime())) {
			return value;
		}

		return new Intl.DateTimeFormat('ko-KR', {
			year: 'numeric',
			month: '2-digit',
			day: '2-digit',
			hour: '2-digit',
			minute: '2-digit',
			second: '2-digit',
			hour12: false
		}).format(date);
	}

	function escapeHtml(value) {
		return String(value)
			.replaceAll('&', '&amp;')
			.replaceAll('<', '&lt;')
			.replaceAll('>', '&gt;')
			.replaceAll('"', '&quot;')
			.replaceAll("'", '&#39;');
	}

	function getCsrfToken() {
		const el = document.querySelector('meta[name="_csrf"]');
		return el ? el.getAttribute('content') : null;
	}

	function getCsrfHeader() {
		const el = document.querySelector('meta[name="_csrf_header"]');
		return el ? el.getAttribute('content') : null;
	}
})();