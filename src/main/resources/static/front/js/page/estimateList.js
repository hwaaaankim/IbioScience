(function() {
	const $ = (s, p = document) => p.querySelector(s);
	const $$ = (s, p = document) => Array.from(p.querySelectorAll(s));

	const checkAll = $('#estimateList-checkAll');
	const checks = () => $$('.estimateList-check');
	const delBtn = $('#estimateList-deleteBtn');

	// 전체선택
	checkAll.addEventListener('change', () => {
		checks().forEach(chk => chk.checked = checkAll.checked);
		updateDeleteState();
	});

	// 개별 체크 변경 시 전체선택/indeterminate 상태 동기화
	document.getElementById('estimateList-tbody').addEventListener('change', (e) => {
		if (!e.target.classList.contains('estimateList-check')) return;
		const all = checks();
		const checkedCount = all.filter(c => c.checked).length;
		checkAll.checked = checkedCount === all.length;
		checkAll.indeterminate = checkedCount > 0 && checkedCount < all.length;
		updateDeleteState();
	});

	// 삭제 버튼 활성/비활성
	function updateDeleteState() {
		const any = checks().some(c => c.checked);
		delBtn.disabled = !any;
	}

	// 검색(샘플 동작)
	$('#estimateList-searchBtn').addEventListener('click', () => {
		const payload = {
			pageSize: $('#estimateList-pageSize').value,
			sort: $('#estimateList-sort').value,
			keyword: $('#estimateList-keyword').value.trim()
		};
		console.log('[견적문의 검색]', payload);
		// TODO: 실제 검색 API 연동
		alert('검색 조건이 콘솔에 출력되었습니다.');
	});

	// 삭제(샘플 동작)
	delBtn.addEventListener('click', () => {
		const ids = checks()
			.map((c, idx) => c.checked ? (c.dataset.id || idx + 1) : null)
			.filter(Boolean);
		if (!ids.length) return;
		if (confirm(`${ids.length}건을 삭제하시겠습니까?`)) {
			console.log('[삭제 요청]', ids);
			// TODO: 삭제 API 연동
			alert('삭제 요청이 콘솔에 출력되었습니다.');
		}
	});
})();
