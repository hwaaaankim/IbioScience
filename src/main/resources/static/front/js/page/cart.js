/* eslint-disable */
(function() {
	"use strict";

	// ===== 설정(필요 시 조정) =====
	const VAT_RATE = 0.10;          // 부가세 10%
	const SHIPPING_BASE = 4000;      // 기본 배송비 (요청 시 로직 변경)
	const DISCOUNT_SUM = 0;          // 총 할인금액(데모값)

	// ===== 유틸 =====
	const fmt = n => (Number(n) || 0).toLocaleString('ko-KR');

	function getRows() {
		return Array.from(document.querySelectorAll('#cart-tbody tr'));
	}

	function rowPrice(row, useDealer = false) {
		const selector = useDealer ? '.col-dealer .price' : '.col-price .price';
		const el = row.querySelector(selector);
		return el ? Number(el.dataset.value || '0') : 0;
	}

	function rowQty(row) {
		const el = row.querySelector('.qty-input');
		return el ? Math.max(1, Number(el.value || '1')) : 1;
	}

	function calcTotals() {
		const rows = getRows();
		let sumProducts = 0;
		rows.forEach(row => {
			const price = rowPrice(row, false); // 판매가 기준 합계(요청 시 딜러가 기준 전환 가능)
			const qty = rowQty(row);
			sumProducts += (price * qty);
		});
		const vat = Math.floor(sumProducts * VAT_RATE);
		const shipping = rows.length > 0 ? SHIPPING_BASE : 0;
		const discount = DISCOUNT_SUM;
		const finalPay = sumProducts + vat + shipping - discount;

		// PC/Tablet
		document.getElementById('total-products').textContent = fmt(sumProducts);
		document.getElementById('total-vat').textContent = fmt(vat);
		document.getElementById('total-shipping').textContent = fmt(shipping);
		document.getElementById('total-discount').textContent = fmt(discount);
		document.getElementById('total-final').textContent = fmt(finalPay);
		// Mobile 요약/모달
		document.getElementById('m-total-final').textContent = fmt(finalPay);
		document.getElementById('md-products').textContent = fmt(sumProducts) + ' 원';
		document.getElementById('md-vat').textContent = fmt(vat) + ' 원';
		document.getElementById('md-shipping').textContent = fmt(shipping) + ' 원';
		document.getElementById('md-discount').textContent = fmt(discount) + ' 원';
		document.getElementById('md-final').textContent = fmt(finalPay) + ' 원';
	}

	// ===== 수량 스테퍼 =====
	function bindSteppers() {
		document.querySelectorAll('.qty-stepper').forEach(box => {
			const input = box.querySelector('.qty-input');
			const up = box.querySelector('.btn-step.up');
			const down = box.querySelector('.btn-step.down');
			const min = Number(box.dataset.min || '1');

			const clamp = () => {
				let v = Number(input.value || '1');
				if (isNaN(v) || v < min) v = min;
				input.value = v;
				calcTotals();
			};
			up.addEventListener('click', () => { input.value = Number(input.value || '1') + 1; clamp(); });
			down.addEventListener('click', () => { input.value = Math.max(min, Number(input.value || '1') - 1); clamp(); });
			input.addEventListener('change', clamp);
		});
	}

	// ===== 전체선택 / 해제 =====
	function bindCheckAll() {
		const all = document.getElementById('cart-checkall');
		if (!all) return;
		all.addEventListener('change', () => {
			const checked = all.checked;
			document.querySelectorAll('.row-check').forEach(chk => chk.checked = checked);
		});
	}

	// ===== 액션 버튼 =====
	function bindActions() {
		// 삭제
		document.getElementById('btn-delete')?.addEventListener('click', () => {
			const targets = Array.from(document.querySelectorAll('.row-check:checked'))
				.map(chk => chk.closest('tr'));
			if (targets.length === 0) {
				alert('삭제할 상품을 선택해주세요.');
				return;
			}
			// TODO: 실제 API 연동 (선택 id 리스트)
			targets.forEach(tr => tr.remove());
			calcTotals();
		});

		// 장바구니 비우기
		document.getElementById('btn-empty')?.addEventListener('click', () => {
			if (!confirm('장바구니를 모두 비우시겠습니까?')) return;
			// TODO: 실제 API 연동
			document.getElementById('cart-tbody').innerHTML = '';
			calcTotals();
		});

		// 수량 변경 적용
		document.getElementById('btn-qty-apply')?.addEventListener('click', () => {
			// TODO: 실제 API 연동 (각 tr의 data-id와 qty-input 값을 서버로 전달)
			alert('수량이 적용되었습니다.');
			calcTotals();
		});

		// 구매 버튼들
		document.getElementById('btn-buy-selected')?.addEventListener('click', () => {
			const ids = Array.from(document.querySelectorAll('.row-check:checked'))
				.map(chk => chk.closest('tr')?.dataset.id);
			if (ids.length === 0) {
				alert('구매할 상품을 선택해주세요.');
				return;
			}
			// TODO: 실제 주문서 작성 페이지로 이동(선택 상품만)
			console.log('선택 구매:', ids);
			alert('선택상품 구매로 이동합니다(데모).');
		});
		document.getElementById('btn-buy-all')?.addEventListener('click', () => {
			// TODO: 실제 주문서 작성 페이지로 이동(전체)
			const ids = getRows().map(tr => tr.dataset.id);
			if (ids.length === 0) {
				alert('장바구니가 비어 있습니다.');
				return;
			}
			console.log('전체 구매:', ids);
			alert('전체구매로 이동합니다(데모).');
		});
	}

	// ===== 초기화 =====
	function init() {
		bindSteppers();
		bindCheckAll();
		bindActions();
		calcTotals();
	}

	document.addEventListener('DOMContentLoaded', init);
})();
