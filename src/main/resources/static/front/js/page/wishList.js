/* eslint-disable */
(function() {
	"use strict";

	const $ = (s, p = document) => p.querySelector(s);
	const $$ = (s, p = document) => Array.from(p.querySelectorAll(s));

	function onReady(fn) {
		if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', fn);
		else fn();
	}

	/* ---------- 공통: 체크박스/수량/검색/하단 버튼 ---------- */
	function commonBindings() {
		const checkAll = $('#wishList-checkAll');
		const tbody = $('#wishList-tbody');
		const btnDelete = $('#wishList-deleteBtn');   // 선택삭제
		const btnCart = $('#wishList-cartBtn');     // (없을 수 있음)
		const btnBuy = $('#wishList-buyBtn');      // (없을 수 있음)

		const checks = () => $$('.wishList-check');

		function syncActionButtons() {
			const any = checks().some(c => c.checked);
			[btnDelete, btnCart, btnBuy].forEach(b => { if (b) b.disabled = !any; });
		}

		// 전체선택
		if (checkAll) {
			checkAll.addEventListener('change', () => {
				checks().forEach(chk => chk.checked = checkAll.checked);
				syncActionButtons();
			});
		}

		// 개별 체크
		if (tbody) {
			tbody.addEventListener('change', (e) => {
				if (!e.target.classList.contains('wishList-check')) return;
				const all = checks();
				const checked = all.filter(c => c.checked).length;
				if (checkAll) {
					checkAll.checked = checked === all.length;
					checkAll.indeterminate = checked > 0 && checked < all.length;
				}
				syncActionButtons();
			});

			// 수량 +/-
			tbody.addEventListener('click', (e) => {
				if (!e.target.classList.contains('wishList-qty-btn')) return;
				const wrap = e.target.closest('.wishList-qty-pc');
				const input = $('.wishList-qty-input', wrap);
				const step = Number(e.target.dataset.step || 0);
				const cur = Math.max(1, Number((input.value || '1').replace(/[^\d]/g, '')) + step);
				input.value = cur;
			});

			// 수량 입력 숫자만
			tbody.addEventListener('input', (e) => {
				if (e.target.classList.contains('wishList-qty-input')) {
					e.target.value = e.target.value.replace(/[^\d]/g, '');
					if (e.target.value === '' || Number(e.target.value) < 1) e.target.value = '1';
				}
				if (e.target.classList.contains('wishList-qty-mobile')) {
					const v = parseInt(e.target.value, 10);
					if (!v || v < 1) e.target.value = 1;
				}
			});

			// 행 버튼(상품바로가기)
			tbody.addEventListener('click', (e) => {
				const btn = e.target.closest('.wishList-btn');
				if (!btn) return;
				if (btn.dataset.action === 'go') {
					alert('상품 상세로 이동합니다(샘플).');
				}
			});
		}

		// 상단 검색(샘플)
		const btnSearch = $('#wishList-searchBtn');
		if (btnSearch) {
			btnSearch.addEventListener('click', () => {
				const payload = {
					status: $('#wishList-status')?.value,
					pageSize: $('#wishList-pageSize')?.value
				};
				console.log('[관심상품 검색]', payload);
				alert('검색 조건이 콘솔에 출력되었습니다.');
			});
		}

		// 하단 선택삭제
		if (btnDelete) {
			btnDelete.addEventListener('click', () => {
				const ids = checks().filter(c => c.checked).map(c => c.dataset.id);
				if (!ids.length) return;
				if (!confirm(`${ids.length}건을 삭제하시겠습니까?`)) return;
				console.log('[선택삭제]', ids);
				checks().forEach(c => { if (c.checked) c.closest('tr')?.remove(); });
				syncActionButtons();
			});
		}

		// (존재 시) 장바구니/바로구매
		if (btnCart) btnCart.addEventListener('click', () => {
			const ids = checks().filter(c => c.checked).map(c => c.dataset.id);
			if (!ids.length) return;
			console.log('[장바구니 담기]', ids);
			alert('장바구니 담기 요청이 콘솔에 출력되었습니다.');
		});
		if (btnBuy) btnBuy.addEventListener('click', () => {
			const ids = checks().filter(c => c.checked).map(c => c.dataset.id);
			if (!ids.length) return;
			console.log('[바로구매]', ids);
			alert('바로구매 요청이 콘솔에 출력되었습니다.');
		});

		// 초기 상태
		syncActionButtons();
	}

	/* ---------- 슬라이드 유틸 (열림 후 max-height 해제 방식) ---------- */
	function slideOpen(panelDiv) {
		// 우선 자동 높이 계산값으로 애니메이션 시작
		panelDiv.style.overflow = 'hidden';
		panelDiv.style.maxHeight = panelDiv.scrollHeight + 'px';
		panelDiv.classList.add('is-open');

		// 트랜지션 종료 후, 높이 제한을 풀어서 어떤 높이도 잘리지 않게 처리
		const onEnd = (e) => {
			if (e.propertyName !== 'max-height') return;
			panelDiv.removeEventListener('transitionend', onEnd);
			panelDiv.style.maxHeight = 'none';   // ★ 제한 해제
			panelDiv.style.overflow = 'visible'; // ★ 열렸을 때는 표시
		};
		panelDiv.addEventListener('transitionend', onEnd);
	}

	function slideClose(panelDiv) {
		// 만약 열림 상태에서 높이 제한이 풀려있다면(=none), 현재 실제 높이를 읽어 다시 설정하여 부드럽게 닫힘
		if (panelDiv.style.maxHeight === 'none') {
			panelDiv.style.overflow = 'hidden'; // 닫힘 애니메이션 동안 숨김
			panelDiv.style.maxHeight = panelDiv.scrollHeight + 'px';
			// 강제 리플로우로 시작점 확정
			// eslint-disable-next-line no-unused-expressions
			panelDiv.offsetHeight;
		}

		// 실제 닫힘
		panelDiv.style.maxHeight = '0px';

		const onEnd = (e) => {
			if (e.propertyName !== 'max-height') return;
			panelDiv.removeEventListener('transitionend', onEnd);
			panelDiv.classList.remove('is-open');
			panelDiv.style.overflow = 'hidden';
			// 닫힌 뒤에는 다시 숫자값 대신 0 상태를 유지 (다음 열림 시 scrollHeight로 계산)
		};
		panelDiv.addEventListener('transitionend', onEnd);
	}

	function isOpen(panelDiv) { return panelDiv.classList.contains('is-open'); }

	/* ---------- 옵션보기 슬라이드(수정) ---------- */
	function optionPanelBindings() {
		let currentOpen = { btn: null, row: null, div: null };

		// 토글 버튼
		$$('.wishList-add-toggle-btn').forEach(btn => {
			btn.addEventListener('click', () => {
				const targetId = btn.getAttribute('data-target');
				const row = document.getElementById('wishList-add-panel-' + targetId);  // tr
				if (!row) return;

				const panelDiv = row.querySelector('.wishList-add-optpanel'); // ★ 실제 슬라이드 대상

				// 이미 열려 있는 다른 패널 닫기
				if (currentOpen.div && currentOpen.div !== panelDiv) {
					currentOpen.btn?.classList.remove('is-open');
					currentOpen.btn?.setAttribute('aria-expanded', 'false');
					currentOpen.row?.setAttribute('aria-hidden', 'true');
					slideClose(currentOpen.div);
					currentOpen = { btn: null, row: null, div: null };
				}

				// 토글
				const willOpen = !isOpen(panelDiv);
				if (willOpen) {
					row.setAttribute('aria-hidden', 'false');
					btn.classList.add('is-open');
					btn.setAttribute('aria-expanded', 'true');
					slideOpen(panelDiv);
					// 열림 후 높이 재보정
					setTimeout(() => { panelDiv.style.maxHeight = panelDiv.scrollHeight + 'px'; }, 10);
					currentOpen = { btn, row, div: panelDiv };
				} else {
					btn.classList.remove('is-open');
					btn.setAttribute('aria-expanded', 'false');
					row.setAttribute('aria-hidden', 'true');
					slideClose(panelDiv);
					currentOpen = { btn: null, row: null, div: null };
				}
			});
		});

		// 패널 내 버튼(견적문의/장바구니/바로구매)
		document.addEventListener('click', (e) => {
			const btn = e.target.closest('.wishList-add-optpanel .wishList-btn');
			if (!btn) return;
			const action = btn.getAttribute('data-action');
			const panel = btn.closest('.wishList-add-optpanel');
			const parentRow = panel?.closest('.wishList-add-optrow');
			const productId = parentRow?.getAttribute('data-parent') || '0';

			// ★ 인덱스 의존 제거: 클래스 기반 안전 추출
			const picks = Array.from(panel.querySelectorAll('tbody tr'))
				.filter(tr => tr.querySelector('.wishList-add-optpick')?.checked)
				.map(tr => {
					return {
						catNo: tr.querySelector('.col-cat')?.textContent?.trim(),
						name: tr.querySelector('.col-name')?.textContent?.trim(),
						price: tr.querySelector('.col-price')?.textContent?.trim(),
						unit: tr.querySelector('.col-unit')?.textContent?.trim(),
						qty: tr.querySelector('.wishList-add-optqty')?.value || '1'
					};
				});

			if (!picks.length) { alert('옵션을 1개 이상 선택해 주세요.'); return; }

			if (action === 'quote') {
				alert('[견적문의] 상품ID=' + productId + '\n선택옵션=' + JSON.stringify(picks, null, 2));
			} else if (action === 'addCart') {
				alert('[장바구니 담기] 상품ID=' + productId + '\n선택옵션=' + JSON.stringify(picks, null, 2));
			} else if (action === 'buyNow') {
				alert('[바로구매] 상품ID=' + productId + '\n선택옵션=' + JSON.stringify(picks, null, 2));
			}
		});
	}

	/* ---------- init ---------- */
	onReady(() => {
		commonBindings();
		optionPanelBindings();
	});
})();
