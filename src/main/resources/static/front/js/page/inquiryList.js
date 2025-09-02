// inquiryList.js
(function() {
	const $ = (s, p = document) => p.querySelector(s);
	const $$ = (s, p = document) => Array.from(p.querySelectorAll(s));

	const checkAll = $('#inquiryList-checkAll');
	const checks = () => $$('.inquiryList-check');
	const delBtn = $('#inquiryList-deleteBtn');

	checkAll.addEventListener('change', () => {
		checks().forEach(chk => chk.checked = checkAll.checked);
		updateDeleteState();
	});

	$('#inquiryList-tbody').addEventListener('change', (e) => {
		if (!e.target.classList.contains('inquiryList-check')) return;
		const all = checks();
		const checkedCount = all.filter(c => c.checked).length;
		checkAll.checked = checkedCount === all.length;
		checkAll.indeterminate = checkedCount > 0 && checkedCount < all.length;
		updateDeleteState();
	});

	function updateDeleteState() {
		delBtn.disabled = !checks().some(c => c.checked);
	}

	$('#inquiryList-searchBtn').addEventListener('click', () => {
		const payload = {
			pageSize: $('#inquiryList-pageSize').value,
			sort: $('#inquiryList-sort').value,
			keyword: $('#inquiryList-keyword').value.trim()
		};
		console.log('[1:1문의 검색]', payload);
		alert('검색 조건이 콘솔에 출력되었습니다.');
	});

	delBtn.addEventListener('click', () => {
		const ids = checks().map((c, i) => c.checked ? (c.dataset.id || i + 1) : null).filter(Boolean);
		if (!ids.length) return;
		if (confirm(`${ids.length}건을 삭제하시겠습니까?`)) {
			console.log('[1:1문의 삭제 요청]', ids);
			alert('삭제 요청이 콘솔에 출력되었습니다.');
		}
	});
})();
