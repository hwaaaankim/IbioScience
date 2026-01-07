(function() {
	'use strict';

	document.addEventListener('click', async (e) => {
		const btn = e.target.closest('.qna-manager-btn-delete');
		if (!btn) return;

		const id = btn.getAttribute('data-qna-id');
		if (!id) return;

		if (!confirm('해당 QNA를 삭제하시겠습니까?')) return;

		try {
			const res = await fetch(`/api/manager/qna/${id}`, { method: 'DELETE' });
			if (!res.ok) {
				const txt = await res.text();
				alert(`삭제 실패: ${txt}`);
				return;
			}
			alert('삭제되었습니다.');
			location.reload();
		} catch (err) {
			alert(`삭제 중 오류: ${err.message}`);
		}
	});
})();
