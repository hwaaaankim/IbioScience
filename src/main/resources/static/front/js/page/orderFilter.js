// ===== 스크립트 =====
(function() {
	const $ = (s) => document.querySelector(s);
	const start = $('#startDate');
	const end = $('#endDate');

	// 초기값: 최근 1개월
	setRange('1m');

	// 라디오 변경 시 자동 반영
	document.querySelectorAll('input[name="rangePreset"]').forEach(r => {
		r.addEventListener('change', e => setRange(e.target.value));
	});

	// 검색(데모)
	$('#orderFilterSearchBtn').addEventListener('click', () => {
		const payload = {
			rangePreset: document.querySelector('input[name="rangePreset"]:checked')?.value ?? '',
			startDate: start.value,
			endDate: end.value,
			orderStatus: $('#orderStatus')?.value ?? '',
			sku: $('#sku')?.value?.trim() ?? '',
			productName: $('#productName')?.value?.trim() ?? '',
			brand: $('#brand')?.value ?? ''
		};
		console.log('[검색 요청]', payload);
		alert('검색 조건이 콘솔에 출력되었습니다.');
	});

	function setRange(preset) {
		const today = new Date();
		end.value = fmt(today);
		const from = new Date(today);
		switch (preset) {
			case '1m': from.setMonth(from.getMonth() - 1); break;
			case '3m': from.setMonth(from.getMonth() - 3); break;
			case '6m': from.setMonth(from.getMonth() - 6); break;
			case '1y': from.setFullYear(from.getFullYear() - 1); break;
			default: from.setMonth(from.getMonth() - 1);
		}
		start.value = fmt(from);
	}
	function fmt(d) {
		const y = d.getFullYear(), m = String(d.getMonth() + 1).padStart(2, '0'), da = String(d.getDate()).padStart(2, '0');
		return `${y}-${m}-${da}`;
	}
})();
