/* eslint-disable */
(function() {
	const $ = (s, p = document) => p.querySelector(s);
	const $$ = (s, p = document) => Array.from(p.querySelectorAll(s));

	// === 탭 전환 ===
	const tabs = $$('.reviewList-tab');
	function applyTab(target) {
		// 탭 버튼 활성화
		tabs.forEach(btn => btn.classList.toggle('active', btn.dataset.target === target));
		// 행 표시 제어: data-type과 일치하는 행만 보이도록, written/writable
		$('#reviewList-tbody').querySelectorAll('tr').forEach(tr => {
			const type = tr.dataset.type || 'written';
			tr.style.display = (target === 'all' || type === target) ? '' : 'none';
		});
	}
	tabs.forEach(btn => btn.addEventListener('click', () => applyTab(btn.dataset.target)));
	applyTab('written'); // 초기값: 내가 작성한 리뷰

	// === 체크 요소 ===
	const checkAll = $('#reviewList-checkAll');
	const checks = () => $$('.reviewList-check');

	// === 하단 액션 버튼 ===
	const btnDelete = $('#reviewList-deleteBtn');
	const btnEdit = $('#reviewList-editBtn');

	// === 버튼 활성화 동기화 ===
	function syncActionButtons() {
		const any = checks().some(c => c.checked && c.closest('tr').style.display !== 'none');
		[btnDelete, btnEdit].forEach(b => b.disabled = !any);
	}
	syncActionButtons();

	// === 전체선택 ===
	checkAll.addEventListener('change', () => {
		checks().forEach(chk => {
			// 현재 보이는 행만 선택
			const tr = chk.closest('tr');
			if (tr.style.display === 'none') return;
			chk.checked = checkAll.checked;
		});
		syncActionButtons();
	});

	// === 개별 체크 변경 시 전체선택/indeterminate 동기화 ===
	$('#reviewList-tbody').addEventListener('change', (e) => {
		if (!e.target.classList.contains('reviewList-check')) return;
		const visible = checks().filter(c => c.closest('tr').style.display !== 'none');
		const checked = visible.filter(c => c.checked).length;
		checkAll.checked = (checked > 0 && checked === visible.length);
		checkAll.indeterminate = (checked > 0 && checked < visible.length);
		syncActionButtons();
	});

	// === 상단 검색(샘플) ===
	$('#reviewList-searchBtn').addEventListener('click', () => {
		const payload = {
			tab: $('.reviewList-tab.active')?.dataset.target || 'written',
			sort: $('#reviewList-sort').value,
			pageSize: $('#reviewList-pageSize').value,
			query: $('#reviewList-query').value.trim()
		};
		console.log('[리뷰 검색]', payload);
		alert('검색 조건이 콘솔에 출력되었습니다.');
		// TODO: 실제 API 연동 시 payload 전달 후 tbody 갱신
	});

	// === 하단 액션 ===
	function selectedIds() {
		return checks()
			.filter(c => c.checked && c.closest('tr').style.display !== 'none')
			.map(c => c.dataset.id);
	}

	btnDelete.addEventListener('click', () => {
		const ids = selectedIds();
		if (!ids.length) return;
		if (!confirm(`${ids.length}건의 리뷰를 삭제하시겠습니까?`)) return;
		console.log('[리뷰 삭제]', ids);
		// TODO: 삭제 API 연동 후 DOM 제거
		checks().forEach(c => {
			if (c.checked) c.closest('tr').remove();
		});
		syncActionButtons();
	});

	btnEdit.addEventListener('click', () => {
		const ids = selectedIds();
		if (ids.length !== 1) {
			alert('수정은 1건만 선택해 주세요.');
			return;
		}
		console.log('[리뷰 수정 이동]', ids[0]);
		// TODO: 수정 페이지로 이동
		alert(`리뷰 수정 페이지로 이동합니다. (id=${ids[0]})`);
	});

	// === 접근성: 엔터로 검색 실행 ===
	$('#reviewList-query').addEventListener('keydown', (e) => {
		if (e.key === 'Enter') $('#reviewList-searchBtn').click();
	});
})();
