(function() {
	'use strict';

	const form = document.getElementById('admin-order-list-manager-search-form');
	if (!form) return;

	const pageInput = form.querySelector('input[name="page"]');
	const sortByInput = form.querySelector('input[name="sortBy"]');
	const sortDirInput = form.querySelector('input[name="sortDir"]');
	const sizeSelect = document.getElementById('admin-order-list-manager-size');
	const fromDateInput = document.getElementById('admin-order-list-manager-from-date');
	const toDateInput = document.getElementById('admin-order-list-manager-to-date');
	const saveStatusBtn = document.getElementById('admin-order-list-manager-save-status-btn');
	const statusSelects = document.querySelectorAll('.admin-order-list-manager-status-select');
	const clickableCells = document.querySelectorAll('.admin-order-list-manager-click-cell');
	const sortButtons = document.querySelectorAll('.admin-order-list-manager-sort-btn');
	const paginationButtons = document.querySelectorAll('.admin-order-list-manager-pagination-wrap button[data-page]');
	const rangeButtons = document.querySelectorAll('[data-range]');

	const changedMap = new Map();

	function getCsrfHeader() {
		const headerMeta = document.querySelector('meta[name="_csrf_header"]');
		const tokenMeta = document.querySelector('meta[name="_csrf"]');

		if (!headerMeta || !tokenMeta) {
			return null;
		}

		return {
			headerName: headerMeta.getAttribute('content'),
			token: tokenMeta.getAttribute('content')
		};
	}

	function submitWithPage(page) {
		if (pageInput) {
			pageInput.value = page;
		}
		form.submit();
	}

	function formatDate(date) {
		const year = date.getFullYear();
		const month = String(date.getMonth() + 1).padStart(2, '0');
		const day = String(date.getDate()).padStart(2, '0');
		return `${year}-${month}-${day}`;
	}

	function applyRange(type) {
		const today = new Date();
		const end = new Date(today);
		const start = new Date(today);

		if (type === 'today') {
			fromDateInput.value = formatDate(today);
			toDateInput.value = formatDate(today);
			return;
		}

		if (type === '7days') {
			start.setDate(today.getDate() - 6);
			fromDateInput.value = formatDate(start);
			toDateInput.value = formatDate(end);
			return;
		}

		if (type === '1month') {
			start.setMonth(today.getMonth() - 1);
			fromDateInput.value = formatDate(start);
			toDateInput.value = formatDate(end);
			return;
		}

		if (type === 'all') {
			fromDateInput.value = '';
			toDateInput.value = '';
		}
	}

	function refreshSaveButton() {
		saveStatusBtn.disabled = changedMap.size === 0;
	}

	sizeSelect?.addEventListener('change', function() {
		submitWithPage(0);
	});

	rangeButtons.forEach(btn => {
		btn.addEventListener('click', function() {
			applyRange(this.dataset.range);
		});
	});

	clickableCells.forEach(cell => {
		cell.addEventListener('click', function() {
			const href = this.dataset.href;
			if (href) {
				window.location.href = href;
			}
		});
	});

	sortButtons.forEach(btn => {
		btn.addEventListener('click', function() {
			const clickedSortBy = this.dataset.sortBy;
			const currentSortBy = window.ADMIN_ORDER_LIST_MANAGER_CURRENT_SORT_BY || 'createdAt';
			const currentSortDir = window.ADMIN_ORDER_LIST_MANAGER_CURRENT_SORT_DIR || 'desc';

			let nextDir = 'asc';
			if (clickedSortBy === currentSortBy) {
				nextDir = currentSortDir === 'asc' ? 'desc' : 'asc';
			}

			sortByInput.value = clickedSortBy;
			sortDirInput.value = nextDir;

			submitWithPage(0);
		});
	});

	paginationButtons.forEach(btn => {
		btn.addEventListener('click', function() {
			if (this.disabled) return;
			const page = this.dataset.page;
			if (page === undefined || page === null || page === '') return;
			submitWithPage(page);
		});
	});

	statusSelects.forEach(select => {
		select.addEventListener('click', function(e) {
			e.stopPropagation();
		});

		select.addEventListener('change', function() {
			const orderId = this.dataset.orderId;
			const originalValue = this.dataset.originalValue;
			const currentValue = this.value;

			if (!orderId) return;

			if (originalValue === currentValue) {
				changedMap.delete(orderId);
			} else {
				changedMap.set(orderId, currentValue);
			}

			refreshSaveButton();
		});
	});

	saveStatusBtn?.addEventListener('click', async function() {
		if (changedMap.size === 0) {
			alert('변경된 상태가 없습니다.');
			return;
		}

		if (!confirm('변경된 주문 상태를 저장하시겠습니까?')) {
			return;
		}

		const payload = {
			items: Array.from(changedMap.entries()).map(([orderId, status]) => ({
				orderId: Number(orderId),
				status: status
			}))
		};

		const headers = {
			'Content-Type': 'application/json'
		};

		const csrf = getCsrfHeader();
		if (csrf) {
			headers[csrf.headerName] = csrf.token;
		}

		try {
			const response = await fetch('/admin/root/api/orders/status/bulk', {
				method: 'POST',
				headers: headers,
				body: JSON.stringify(payload)
			});

			const result = await response.json();

			if (!response.ok || !result.success) {
				throw new Error(result.message || '상태 저장 중 오류가 발생했습니다.');
			}

			alert(result.message || '저장되었습니다.');
			window.location.reload();
		} catch (e) {
			alert(e.message || '상태 저장 중 오류가 발생했습니다.');
		}
	});
})();