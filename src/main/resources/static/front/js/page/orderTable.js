(function() {
	var modal = document.getElementById('myPage-main-modal');
	var backdrop = modal.querySelector('.myPage-main-backdrop');
	var dialog = modal.querySelector('.myPage-main-dialog');
	var closeBtns = modal.querySelectorAll('[data-close]');
	var body = modal.querySelector('.myPage-main-mbody');

	function openModal(html) {
		body.innerHTML = html || '';
		modal.setAttribute('aria-hidden', 'false');
		setTimeout(function() { dialog.focus && dialog.focus(); }, 0);
		document.body.style.overflow = 'hidden';
	}
	function closeModal() {
		modal.setAttribute('aria-hidden', 'true');
		document.body.style.overflow = '';
	}

	backdrop.addEventListener('click', closeModal);
	closeBtns.forEach(function(btn) { btn.addEventListener('click', closeModal); });
	document.addEventListener('keydown', function(e) {
		if (e.key === 'Escape' && modal.getAttribute('aria-hidden') === 'false') closeModal();
	});

	// 주문상세: 동일 묶음(tablewrap) 안의 첫 행 데이터를 사용
	document.addEventListener('click', function(e) {
		var btn = e.target.closest('.myPage-main-smbtn[data-role="detail"]');
		if (!btn) return;

		var wrap = btn.closest('.myPage-main-tablewrap');
		var row = wrap.querySelector('tbody tr');
		if (!row) { openModal('<p>표 데이터가 없습니다.</p>'); return; }

		var td = row.querySelectorAll('td');
		var col1 = td[0].innerHTML;            // 1열 주문일자/번호(두 줄)
		var prod = td[1].innerHTML;            // 2열 주문내역/코드
		var price = td[2].textContent.trim();   // 3열 상품금액
		var qty = td[3].textContent.trim();   // 4열 수량
		var total = td[4].textContent.trim();   // 5열 총금액
		var state = td[5].textContent.trim();   // 6열 상태
		var req = td[6].textContent.trim();   // 7열 요청일

		var html = ''
			+ '<div class="myPage-main-mgroup">'
			+ '  <div class="myPage-main-mrow"><div class="myPage-main-mlabel">주문일자/번호</div><div class="myPage-main-mvalue">' + col1 + '</div></div>'
			+ '  <div class="myPage-main-mrow"><div class="myPage-main-mlabel">주문내역/코드</div><div class="myPage-main-mvalue">' + prod + '</div></div>'
			+ '  <div class="myPage-main-mrow"><div class="myPage-main-mlabel">상품금액</div><div class="myPage-main-mvalue">' + price + '</div></div>'
			+ '  <div class="myPage-main-mrow"><div class="myPage-main-mlabel">구매수량</div><div class="myPage-main-mvalue">' + qty + '</div></div>'
			+ '  <div class="myPage-main-mrow"><div class="myPage-main-mlabel">총금액</div><div class="myPage-main-mvalue">' + total + '</div></div>'
			+ '  <div class="myPage-main-mrow"><div class="myPage-main-mlabel">주문상태</div><div class="myPage-main-mvalue">' + state + '</div></div>'
			+ '  <div class="myPage-main-mrow"><div class="myPage-main-mlabel">요청일</div><div class="myPage-main-mvalue">' + req + '</div></div>'
			+ '</div>'
			+ '<div class="myPage-main-mitems"><strong>상세 안내</strong>주문 관련 상세내용을 여기에 노출합니다. (교환/반품, 배송정보 등)</div>';

		openModal(html);
	});
})();