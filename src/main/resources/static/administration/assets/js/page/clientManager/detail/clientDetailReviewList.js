(function() {
	'use strict';

	const root = document.getElementById('layout-wrapper');
	if (!root) {
		return;
	}

	const memberId = root.dataset.memberId;
	if (!memberId) {
		console.error('memberId 가 없습니다.');
		return;
	}

	const form = document.getElementById('client-detail-reviewlist-search-form');
	const sizeSelect = document.getElementById('client-detail-reviewlist-size');
	const fromDateInput = document.getElementById('client-detail-reviewlist-from-date');
	const toDateInput = document.getElementById('client-detail-reviewlist-to-date');
	const resetBtn = document.getElementById('client-detail-reviewlist-reset-btn');

	const tbody = document.getElementById('client-detail-reviewlist-tbody');
	const paginationEl = document.getElementById('client-detail-reviewlist-pagination');
	const pageInfoEl = document.getElementById('client-detail-reviewlist-page-info');
	const totalCountEl = document.getElementById('client-detail-reviewlist-total-count');

	const checkAllEl = document.getElementById('client-detail-reviewlist-check-all');
	const deleteBtn = document.getElementById('client-detail-reviewlist-delete-btn');

	const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
	const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || '';

	const state = {
		page: 0,
		size: Number(sizeSelect.value || 10),
		fromDate: '',
		toDate: '',
		loading: false
	};

	function escapeHtml(value) {
		if (value === null || value === undefined) {
			return '';
		}

		return String(value)
			.replace(/&/g, '&amp;')
			.replace(/</g, '&lt;')
			.replace(/>/g, '&gt;')
			.replace(/"/g, '&quot;')
			.replace(/'/g, '&#039;');
	}

	function nl2brEscaped(value) {
		return escapeHtml(value).replace(/\n/g, '<br>');
	}

	function formatRating(rating) {
		const safeRating = Number(rating || 0);
		const filled = '★'.repeat(Math.max(0, Math.min(safeRating, 5)));
		const empty = '☆'.repeat(Math.max(0, 5 - safeRating));
		return `<span class="client-detail-reviewlist-rating-text">${filled}${empty} (${safeRating}점)</span>`;
	}

	function getCheckedReviewIds() {
		return Array.from(document.querySelectorAll('.client-detail-reviewlist-row-check:checked'))
			.map(input => Number(input.value))
			.filter(Number.isFinite);
	}

	function updateDeleteButtonState() {
		const checkedIds = getCheckedReviewIds();
		deleteBtn.disabled = checkedIds.length === 0;
	}

	function updateCheckAllState() {
		const rowChecks = Array.from(document.querySelectorAll('.client-detail-reviewlist-row-check'));
		if (rowChecks.length === 0) {
			checkAllEl.checked = false;
			checkAllEl.indeterminate = false;
			return;
		}

		const checkedCount = rowChecks.filter(input => input.checked).length;
		checkAllEl.checked = checkedCount > 0 && checkedCount === rowChecks.length;
		checkAllEl.indeterminate = checkedCount > 0 && checkedCount < rowChecks.length;
	}

	function setLoadingRow() {
		tbody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center py-5 text-muted">데이터를 불러오는 중입니다.</td>
            </tr>
        `;
	}

	function setEmptyRow(message) {
		tbody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center client-detail-reviewlist-empty">${escapeHtml(message)}</td>
            </tr>
        `;
	}

	function buildImageCell(item) {
		if (item.thumbnailUrl) {
			const countBadge = Number(item.imageCount || 0) > 1
				? `<span class="badge bg-light text-dark">+${Number(item.imageCount) - 1}</span>`
				: '';
			return `
                <div class="client-detail-reviewlist-thumbnail-wrap">
                    <img src="${escapeHtml(item.thumbnailUrl)}"
                         alt="리뷰 이미지"
                         class="client-detail-reviewlist-thumbnail">
                    ${countBadge}
                </div>
            `;
		}

		return `<span class="client-detail-reviewlist-no-image">없음</span>`;
	}

	function createMainRowHtml(item) {
		return `
            <tr class="client-detail-reviewlist-main-row" data-review-id="${item.reviewId}">
                <td class="text-center" data-no-toggle="true">
                    <div class="form-check d-inline-flex justify-content-center mb-0">
                        <input type="checkbox"
                               class="form-check-input client-detail-reviewlist-row-check"
                               value="${item.reviewId}"
                               data-no-toggle="true">
                    </div>
                </td>
                <td>${escapeHtml(item.authorName || '-')}</td>
                <td class="text-center">${buildImageCell(item)}</td>
                <td class="text-center">${formatRating(item.rating)}</td>
                <td class="text-center">${escapeHtml(item.createdAtText || '-')}</td>
                <td class="text-center" data-no-toggle="true">
                    <a href="/productDetail/${item.productId}"
                       target="_blank"
                       class="btn btn-sm btn-outline-primary"
                       data-no-toggle="true">
                        바로가기
                    </a>
                </td>
            </tr>
        `;
	}

	function createDetailRowHtml(item) {
		return `
            <tr class="client-detail-reviewlist-detail-row" data-detail-review-id="${item.reviewId}" style="display:none;">
                <td colspan="6">
                    <div class="client-detail-reviewlist-detail-wrap">
                        <div class="client-detail-reviewlist-detail-inner">
                            <div class="client-detail-reviewlist-detail-label">리뷰 내용</div>
                            <div class="client-detail-reviewlist-detail-content">
                                ${item.content ? nl2brEscaped(item.content) : '-'}
                            </div>
                        </div>
                    </div>
                </td>
            </tr>
        `;
	}

	function renderRows(items) {
		if (!Array.isArray(items) || items.length === 0) {
			setEmptyRow('등록된 리뷰가 없습니다.');
			return;
		}

		const html = items.map(item => createMainRowHtml(item) + createDetailRowHtml(item)).join('');
		tbody.innerHTML = html;
	}

	function buildPageButton(label, page, disabled, active) {
		const li = document.createElement('li');
		li.className = `page-item${disabled ? ' disabled' : ''}${active ? ' active' : ''}`;

		const button = document.createElement('button');
		button.type = 'button';
		button.className = 'page-link';
		button.textContent = label;
		button.disabled = disabled;
		button.dataset.page = String(page);

		li.appendChild(button);
		return li;
	}

	function renderPagination(data) {
		paginationEl.innerHTML = '';

		const totalPages = Number(data.totalPages || 0);
		const currentPage = Number(data.page || 0);

		if (totalPages === 0) {
			return;
		}

		const maxButtons = 5;
		let startPage = Math.max(0, currentPage - Math.floor(maxButtons / 2));
		let endPage = startPage + maxButtons - 1;

		if (endPage >= totalPages) {
			endPage = totalPages - 1;
			startPage = Math.max(0, endPage - maxButtons + 1);
		}

		paginationEl.appendChild(buildPageButton('FIRST', 0, currentPage === 0, false));
		paginationEl.appendChild(buildPageButton('PREV', Math.max(0, currentPage - 1), currentPage === 0, false));

		for (let page = startPage; page <= endPage; page++) {
			paginationEl.appendChild(buildPageButton(String(page + 1), page, false, page === currentPage));
		}

		paginationEl.appendChild(buildPageButton('NEXT', Math.min(totalPages - 1, currentPage + 1), currentPage >= totalPages - 1, false));
		paginationEl.appendChild(buildPageButton('LAST', totalPages - 1, currentPage >= totalPages - 1, false));
	}

	function updatePageInfo(data) {
		const totalElements = Number(data.totalElements || 0);
		const totalPages = Number(data.totalPages || 0);
		const currentPage = Number(data.page || 0);

		totalCountEl.textContent = `총 ${totalElements.toLocaleString()}건`;

		if (totalPages === 0) {
			pageInfoEl.textContent = '조회 결과가 없습니다.';
			return;
		}

		pageInfoEl.textContent = `현재 ${currentPage + 1} / ${totalPages} 페이지`;
	}

	function buildQueryString() {
		const params = new URLSearchParams();
		params.set('page', String(state.page));
		params.set('size', String(state.size));

		if (state.fromDate) {
			params.set('fromDate', state.fromDate);
		}

		if (state.toDate) {
			params.set('toDate', state.toDate);
		}

		return params.toString();
	}

	async function fetchList() {
		if (state.loading) {
			return;
		}

		state.loading = true;
		setLoadingRow();

		try {
			const response = await fetch(`/admin/root/api/clientDetail/${memberId}/reviewList?${buildQueryString()}`, {
				method: 'GET',
				credentials: 'same-origin'
			});

			if (!response.ok) {
				const errorText = await response.text();
				throw new Error(errorText || '리뷰 목록 조회에 실패했습니다.');
			}

			const data = await response.json();

			if (state.page > 0 && data.empty && Number(data.totalElements || 0) > 0) {
				state.page = Math.max(0, Number(data.totalPages || 1) - 1);
				state.loading = false;
				await fetchList();
				return;
			}

			renderRows(data.content || []);
			renderPagination(data);
			updatePageInfo(data);

			checkAllEl.checked = false;
			checkAllEl.indeterminate = false;
			updateDeleteButtonState();
		} catch (error) {
			console.error(error);
			setEmptyRow('리뷰 목록을 불러오지 못했습니다.');
			pageInfoEl.textContent = '오류가 발생했습니다.';
			totalCountEl.textContent = '총 0건';
			paginationEl.innerHTML = '';
			alert(extractErrorMessage(error));
		} finally {
			state.loading = false;
		}
	}

	function extractErrorMessage(error) {
		if (!error) {
			return '처리 중 오류가 발생했습니다.';
		}

		if (typeof error.message === 'string' && error.message.trim()) {
			try {
				const parsed = JSON.parse(error.message);
				if (parsed && parsed.message) {
					return parsed.message;
				}
			} catch (ignored) {
			}
			return error.message;
		}

		return '처리 중 오류가 발생했습니다.';
	}

	function slideUp(element, duration, done) {
		element.style.height = `${element.offsetHeight}px`;
		element.offsetHeight;
		element.style.transitionProperty = 'height, margin, padding';
		element.style.transitionDuration = `${duration}ms`;
		element.style.boxSizing = 'border-box';
		element.style.overflow = 'hidden';
		element.style.height = '0';
		element.style.paddingTop = '0';
		element.style.paddingBottom = '0';
		element.style.marginTop = '0';
		element.style.marginBottom = '0';

		window.setTimeout(() => {
			element.style.display = 'none';
			element.style.removeProperty('height');
			element.style.removeProperty('padding-top');
			element.style.removeProperty('padding-bottom');
			element.style.removeProperty('margin-top');
			element.style.removeProperty('margin-bottom');
			element.style.removeProperty('overflow');
			element.style.removeProperty('transition-duration');
			element.style.removeProperty('transition-property');
			if (typeof done === 'function') {
				done();
			}
		}, duration);
	}

	function slideDown(element, duration, done) {
		element.style.removeProperty('display');
		let display = window.getComputedStyle(element).display;
		if (display === 'none') {
			display = 'block';
		}
		element.style.display = display;

		const height = element.offsetHeight;
		element.style.overflow = 'hidden';
		element.style.height = '0';
		element.style.paddingTop = '0';
		element.style.paddingBottom = '0';
		element.style.marginTop = '0';
		element.style.marginBottom = '0';
		element.offsetHeight;
		element.style.boxSizing = 'border-box';
		element.style.transitionProperty = 'height, margin, padding';
		element.style.transitionDuration = `${duration}ms`;
		element.style.height = `${height}px`;
		element.style.removeProperty('padding-top');
		element.style.removeProperty('padding-bottom');
		element.style.removeProperty('margin-top');
		element.style.removeProperty('margin-bottom');

		window.setTimeout(() => {
			element.style.removeProperty('height');
			element.style.removeProperty('overflow');
			element.style.removeProperty('transition-duration');
			element.style.removeProperty('transition-property');
			if (typeof done === 'function') {
				done();
			}
		}, duration);
	}

	function toggleDetailRow(reviewId) {
		const detailRow = document.querySelector(`.client-detail-reviewlist-detail-row[data-detail-review-id="${reviewId}"]`);
		if (!detailRow) {
			return;
		}

		const wrap = detailRow.querySelector('.client-detail-reviewlist-detail-wrap');
		if (!wrap) {
			return;
		}

		const isOpen = detailRow.style.display !== 'none';

		if (isOpen) {
			slideUp(wrap, 180, () => {
				detailRow.style.display = 'none';
			});
			return;
		}

		detailRow.style.display = 'table-row';
		slideDown(wrap, 180);
	}

	async function deleteSelectedReviews() {
		const reviewIds = getCheckedReviewIds();

		if (reviewIds.length === 0) {
			return;
		}

		const confirmed = window.confirm('해당 유저의 리뷰를 삭제하시겠습니까?');
		if (!confirmed) {
			return;
		}

		try {
			const headers = {
				'Content-Type': 'application/json'
			};

			if (csrfHeader && csrfToken) {
				headers[csrfHeader] = csrfToken;
			}

			const response = await fetch(`/admin/root/api/clientDetail/${memberId}/reviewList`, {
				method: 'DELETE',
				credentials: 'same-origin',
				headers,
				body: JSON.stringify({ reviewIds })
			});

			if (!response.ok) {
				const errorText = await response.text();
				throw new Error(errorText || '리뷰 삭제에 실패했습니다.');
			}

			const result = await response.json();
			alert(result.message || '삭제가 완료되었습니다.');

			checkAllEl.checked = false;
			checkAllEl.indeterminate = false;
			deleteBtn.disabled = true;

			await fetchList();
		} catch (error) {
			console.error(error);
			alert(extractErrorMessage(error));
		}
	}

	form.addEventListener('submit', function(event) {
		event.preventDefault();

		state.page = 0;
		state.size = Number(sizeSelect.value || 10);
		state.fromDate = fromDateInput.value || '';
		state.toDate = toDateInput.value || '';

		fetchList();
	});

	sizeSelect.addEventListener('change', function() {
		state.page = 0;
		state.size = Number(sizeSelect.value || 10);
		state.fromDate = fromDateInput.value || '';
		state.toDate = toDateInput.value || '';
		fetchList();
	});

	resetBtn.addEventListener('click', function() {
		sizeSelect.value = '10';
		fromDateInput.value = '';
		toDateInput.value = '';

		state.page = 0;
		state.size = 10;
		state.fromDate = '';
		state.toDate = '';

		fetchList();
	});

	checkAllEl.addEventListener('change', function() {
		document.querySelectorAll('.client-detail-reviewlist-row-check').forEach(input => {
			input.checked = checkAllEl.checked;
		});
		updateCheckAllState();
		updateDeleteButtonState();
	});

	tbody.addEventListener('change', function(event) {
		const target = event.target;
		if (target.classList.contains('client-detail-reviewlist-row-check')) {
			updateCheckAllState();
			updateDeleteButtonState();
		}
	});

	tbody.addEventListener('click', function(event) {
		const target = event.target;

		if (target.closest('[data-no-toggle="true"]') || target.matches('input, button, a, label')) {
			return;
		}

		const row = target.closest('.client-detail-reviewlist-main-row');
		if (!row) {
			return;
		}

		const reviewId = row.dataset.reviewId;
		if (!reviewId) {
			return;
		}

		toggleDetailRow(reviewId);
	});

	paginationEl.addEventListener('click', function(event) {
		const button = event.target.closest('button[data-page]');
		if (!button || button.disabled) {
			return;
		}

		const page = Number(button.dataset.page);
		if (!Number.isFinite(page) || page < 0 || page === state.page) {
			return;
		}

		state.page = page;
		fetchList();
	});

	deleteBtn.addEventListener('click', deleteSelectedReviews);

	fetchList();
})();