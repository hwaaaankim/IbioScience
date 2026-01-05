document.addEventListener('DOMContentLoaded', () => {
	const deleteBtns = document.querySelectorAll('.event-manager-delete-btn');

	deleteBtns.forEach(btn => {
		btn.addEventListener('click', async () => {
			const id = btn.getAttribute('data-id');
			if (!id) return;

			const ok = confirm(`이벤트(ID: ${id})를 삭제하시겠습니까?\n삭제 후 복구할 수 없습니다.`);
			if (!ok) return;

			try {
				const res = await fetch(`/api/manager/event/${id}`, {
					method: 'DELETE',
					headers: { 'Accept': 'application/json' }
				});

				if (!res.ok) {
					const txt = await res.text();
					alert(`삭제 실패: ${res.status}\n${txt}`);
					return;
				}

				location.reload();
			} catch (e) {
				alert(`삭제 중 오류: ${e.message}`);
			}
		});
	});
});
