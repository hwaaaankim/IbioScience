/* /administration/assets/js/page/clientManager/clientSearch.js */
(function() {
	'use strict';

	const form = document.getElementById('client-search-form');
	if (!form) return;

	const pageInput = form.querySelector('input[name="page"]');
	const sortKeyInput = form.querySelector('input[name="sortKey"]');
	const sortDirInput = form.querySelector('input[name="sortDir"]');

	const sizeSelect = document.getElementById('client-size');
	const gradeSelect = document.getElementById('client-grade');
	const memberTypeChecks = document.querySelectorAll('.client-member-type');

	const checkAll = document.getElementById('client-check-all');
	const rowChecks = document.querySelectorAll('.client-row-check');

	function safeSetPageZero() {
		if (pageInput) pageInput.value = '0';
	}

	// ===== 1) 딜러등급 enable/disable =====
	function hasGeneralSelected() {
		const general = document.getElementById('type-general');
		return !!general && general.checked;
	}

	function hasCompanySelected() {
		const buyer = document.getElementById('type-company-buyer');
		const seller = document.getElementById('type-company-seller');
		return (!!buyer && buyer.checked) || (!!seller && seller.checked);
	}

	/**
	 * ✅ 정책:
	 * - 일반회원이 체크되어 있으면 어떤 경우든 딜러등급 disabled
	 * - 일반회원이 체크 안 된 상태에서만 기업회원 체크 시 enabled
	 */
	function isGradeAllowed() {
		if (hasGeneralSelected()) return false;
		return hasCompanySelected();
	}

	function syncGradeEnabled() {
		if (!gradeSelect) return;

		const allowed = isGradeAllowed();
		gradeSelect.disabled = !allowed;

		if (!allowed) {
			// disabled일 때는 서버로 grade가 안 넘어가지만,
			// UI 상태 통일을 위해 ALL로 맞춤
			gradeSelect.value = 'ALL';
		}
	}

	memberTypeChecks.forEach(chk => {
		chk.addEventListener('change', () => {
			safeSetPageZero();
			syncGradeEnabled();
		});
	});

	// 초기 1회
	syncGradeEnabled();

	// ===== 2) 페이지사이즈 변경 =====
	if (sizeSelect) {
		sizeSelect.addEventListener('change', () => {
			safeSetPageZero();
			form.submit();
		});
	}

	// ===== 3) 정렬 헤더 클릭 =====
	const sortableHeaders = document.querySelectorAll('.client-sortable');
	sortableHeaders.forEach(th => {
		th.style.cursor = 'pointer';
		th.addEventListener('click', () => {
			const key = th.getAttribute('data-sort-key');
			if (!key) return;

			const currentKey = (sortKeyInput && sortKeyInput.value) ? sortKeyInput.value : '';
			const currentDir = (sortDirInput && sortDirInput.value) ? sortDirInput.value.toLowerCase() : '';

			let nextDir = 'asc';
			if (currentKey === key) {
				nextDir = (currentDir === 'asc') ? 'desc' : 'asc';
			} else {
				nextDir = 'asc';
			}

			if (sortKeyInput) sortKeyInput.value = key;
			if (sortDirInput) sortDirInput.value = nextDir;
			safeSetPageZero();

			form.submit();
		});
	});

	// ===== 4) 전체 체크 =====
	if (checkAll) {
		checkAll.addEventListener('change', () => {
			rowChecks.forEach(chk => chk.checked = checkAll.checked);
		});
	}

	rowChecks.forEach(chk => {
		chk.addEventListener('change', () => {
			if (!checkAll) return;
			const allChecked = Array.from(rowChecks).every(c => c.checked);
			const anyChecked = Array.from(rowChecks).some(c => c.checked);
			checkAll.indeterminate = !allChecked && anyChecked;
			checkAll.checked = allChecked;
		});
	});

})();