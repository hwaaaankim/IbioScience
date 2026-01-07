(function() {
	'use strict';

	let editor;

	class QnaUploadAdapter {
		constructor(loader) { this.loader = loader; }
		async upload() {
			const file = await this.loader.file;
			const data = new FormData();
			data.append('upload', file);

			const res = await fetch('/api/manager/qna/upload-temp', { method: 'POST', body: data });
			if (!res.ok) {
				const txt = await res.text();
				throw new Error(`이미지 업로드 실패 (${res.status}): ${txt}`);
			}
			const json = await res.json();
			if (!json.url) throw new Error('업로드 응답에 url이 없습니다.');
			return { default: json.url };
		}
		abort() { }
	}

	function QnaUploadAdapterPlugin(editorInstance) {
		editorInstance.plugins.get('FileRepository').createUploadAdapter = (loader) => new QnaUploadAdapter(loader);
	}

	document.addEventListener('DOMContentLoaded', async () => {
		const root = document.getElementById('qna-detail-root');
		if (!root) return;

		const qnaId = root.getAttribute('data-qna-id');
		if (!qnaId) return;

		const editorEl = document.getElementById('qna-detail-editor');
		editor = await ClassicEditor.create(editorEl, { extraPlugins: [QnaUploadAdapterPlugin] });

		document.getElementById('qna-detail-btn-update').addEventListener('click', async () => {
			const categoryId = document.getElementById('qna-detail-category').value;
			const title = document.getElementById('qna-detail-title').value.trim();
			const contentHtml = editor.getData();

			if (!categoryId) { alert('카테고리를 선택해 주세요.'); return; }
			if (!title) { alert('제목을 입력해 주세요.'); return; }

			const payload = { categoryId: Number(categoryId), title, contentHtml };

			try {
				const res = await fetch(`/api/manager/qna/${qnaId}`, {
					method: 'PUT',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify(payload)
				});

				if (!res.ok) {
					const txt = await res.text();
					alert(`수정 실패: ${txt}`);
					return;
				}

				// 변경이 없어도 서버는 그대로 유지(이미지 diff=0)
				alert('수정되었습니다.');
				location.reload();
			} catch (err) {
				alert(`수정 중 오류: ${err.message}`);
			}
		});

		document.getElementById('qna-detail-btn-delete').addEventListener('click', async () => {
			if (!confirm('해당 QNA를 삭제하시겠습니까?')) return;

			try {
				const res = await fetch(`/api/manager/qna/${qnaId}`, { method: 'DELETE' });
				if (!res.ok) {
					const txt = await res.text();
					alert(`삭제 실패: ${txt}`);
					return;
				}
				alert('삭제되었습니다.');
				location.href = '/admin/manager/qnaManager';
			} catch (err) {
				alert(`삭제 중 오류: ${err.message}`);
			}
		});
	});
})();
