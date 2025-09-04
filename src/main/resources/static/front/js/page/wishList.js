/* eslint-disable */
(function() {
	const $ = (s, p = document) => p.querySelector(s);
	const $$ = (s, p = document) => Array.from(p.querySelectorAll(s));

	// 체크 요소
	const checkAll = $('#wishList-checkAll');
	const checks = () => $$('.wishList-check');

	// 하단 액션 버튼
	const btnDelete = $('#wishList-deleteBtn');
	const btnCart = $('#wishList-cartBtn');
	const btnBuy = $('#wishList-buyBtn');

	// === 선택 상태에 따라 하단 버튼 활성화 ===
	function syncActionButtons() {
		const any = checks().some(c => c.checked);
		[btnDelete, btnCart, btnBuy].forEach(b => b.disabled = !any);
	}

	// === 전체선택 ===
	checkAll.addEventListener('change', () => {
		checks().forEach(chk => chk.checked = checkAll.checked);
		syncActionButtons();
	});

	// === 개별 체크 변경 시 전체선택/indeterminate 동기화 ===
	$('#wishList-tbody').addEventListener('change', (e) => {
		if (!e.target.classList.contains('wishList-check')) return;
		const all = checks();
		const checked = all.filter(c => c.checked).length;
		checkAll.checked = checked === all.length;
		checkAll.indeterminate = checked > 0 && checked < all.length;
		syncActionButtons();
	});

	// === 수량 증가/감소 (PC/Tablet UI) ===
	$('#wishList-tbody').addEventListener('click', (e) => {
		if (!e.target.classList.contains('wishList-qty-btn')) return;
		const wrap = e.target.closest('.wishList-qty-pc');
		const input = $('.wishList-qty-input', wrap);
		const step = Number(e.target.dataset.step || 0);
		const cur = Math.max(1, Number((input.value || '1').replace(/[^\d]/g, '')) + step);
		input.value = cur;
	});

	// === 수량 입력 숫자만 허용 (PC/Tablet UI) ===
	$('#wishList-tbody').addEventListener('input', (e) => {
		if (e.target.classList.contains('wishList-qty-input')) {
			e.target.value = e.target.value.replace(/[^\d]/g, '');
			if (e.target.value === '' || Number(e.target.value) < 1) e.target.value = '1';
		}
		if (e.target.classList.contains('wishList-qty-mobile')) {
			const v = parseInt(e.target.value, 10);
			if (!v || v < 1) e.target.value = 1;
		}
	});

	// === 행 내 액션 버튼(상품바로가기 / 삭제) ===
	$('#wishList-tbody').addEventListener('click', (e) => {
		const btn = e.target.closest('.wishList-btn');
		if (!btn) return;
		const tr = e.target.closest('tr');
		const id = $('.wishList-check', tr)?.dataset.id;

		if (btn.dataset.action === 'go') {
			// TODO: 실제 상품 상세 URL로 이동
			console.log('[상품바로가기]', id);
			alert('상품 상세로 이동합니다(샘플).');
		}
		if (btn.dataset.action === 'remove') {
			// TODO: 실제 삭제 API 연동
			console.log('[행 삭제]', id);
			tr.remove();
			syncActionButtons();
		}
	});

	// === 상단 검색(샘플) ===
	$('#wishList-searchBtn').addEventListener('click', () => {
		const payload = {
			status: $('#wishList-status').value,
			pageSize: $('#wishList-pageSize').value
		};
		console.log('[관심상품 검색]', payload);
		alert('검색 조건이 콘솔에 출력되었습니다.');
	});

	// === 하단 일괄 액션(샘플) ===
	function selectedIds() {
		return checks().filter(c => c.checked).map(c => c.dataset.id);
	}

	btnDelete.addEventListener('click', () => {
		const ids = selectedIds();
		if (!ids.length) return;
		if (!confirm(`${ids.length}건을 삭제하시겠습니까?`)) return;
		console.log('[선택삭제]', ids);
		// TODO: 삭제 API 연동 후 DOM 제거
		checks().forEach(c => { if (c.checked) c.closest('tr').remove(); });
		syncActionButtons();
	});

	btnCart.addEventListener('click', () => {
		const ids = selectedIds();
		if (!ids.length) return;
		console.log('[장바구니 담기]', ids);
		alert('장바구니 담기 요청이 콘솔에 출력되었습니다.');
	});

	btnBuy.addEventListener('click', () => {
		const ids = selectedIds();
		if (!ids.length) return;
		console.log('[바로구매]', ids);
		alert('바로구매 요청이 콘솔에 출력되었습니다.');
	});

	// 초기 상태 동기화
	syncActionButtons();
})();
