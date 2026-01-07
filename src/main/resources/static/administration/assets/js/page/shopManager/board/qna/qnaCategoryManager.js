(function() {
	'use strict';

	const $ = (sel, root) => (root || document).querySelector(sel);

	async function requestJson(url, method, bodyObj) {
		const res = await fetch(url, {
			method,
			headers: { 'Content-Type': 'application/json' },
			body: bodyObj ? JSON.stringify(bodyObj) : null
		});
		if (!res.ok) {
			const txt = await res.text();
			throw new Error(txt || `HTTP ${res.status}`);
		}
		return res.json();
	}

	document.addEventListener('DOMContentLoaded', () => {
		const createBtn = $('#qna-category-create-btn');
		createBtn.addEventListener('click', async () => {
			const name = $('#qna-category-create-name').value.trim();
			if (!name) { alert('카테고리명을 입력해 주세요.'); return; }

			try {
				await requestJson('/api/manager/qna-category', 'POST', { name });
				alert('등록되었습니다.');
				location.reload();
			} catch (e) {
				alert(`등록 실패: ${e.message}`);
			}
		});

		document.addEventListener('click', async (e) => {
			const tr = e.target.closest('tr[data-category-id]');
			if (!tr) return;

			const id = tr.getAttribute('data-category-id');

			// 수정/저장 토글
			const editBtn = e.target.closest('.qna-category-btn-edit');
			if (editBtn) {
				const textEl = tr.querySelector('.qna-category-name-text');
				const inputEl = tr.querySelector('.qna-category-name-input');

				const isEditing = !inputEl.classList.contains('d-none') ? true : false;

				if (!isEditing) {
					inputEl.classList.remove('d-none');
					textEl.classList.add('d-none');
					editBtn.textContent = '저장';
					inputEl.focus();
					return;
				}

				// 저장
				const newName = inputEl.value.trim();
				if (!newName) { alert('카테고리명을 입력해 주세요.'); return; }

				try {
					await requestJson(`/api/manager/qna-category/${id}`, 'PUT', { name: newName });
					alert('수정되었습니다.');
					location.reload();
				} catch (err) {
					alert(`수정 실패: ${err.message}`);
				}
				return;
			}

			// 삭제
			const delBtn = e.target.closest('.qna-category-btn-delete');
			if (delBtn) {
				if (!confirm('해당 카테고리를 삭제하시겠습니까?')) return;

				try {
					await requestJson(`/api/manager/qna-category/${id}`, 'DELETE');
					alert('삭제되었습니다.');
					location.reload();
				} catch (err) {
					alert(`삭제 실패: ${err.message}`);
				}
			}
		});
	});
})();
