(function() {
	'use strict';

	let editor;

	class QnaUploadAdapter {
		constructor(loader) { this.loader = loader; }
		async upload() {
			const file = await this.loader.file;
			const data = new FormData();
			data.append('upload', file);

			const res = await fetch('/api/manager/qna/upload-temp', {
				method: 'POST',
				body: data
			});

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
		const editorEl = document.getElementById('qna-insert-editor');
		if (!editorEl) return;

		editor = await ClassicEditor.create(editorEl, {
			extraPlugins: [QnaUploadAdapterPlugin]
		});

		const btnSave = document.getElementById('qna-insert-btn-save');
		btnSave.addEventListener('click', async () => {
			const categoryId = document.getElementById('qna-insert-category').value;
			const title = document.getElementById('qna-insert-title').value.trim();
			const contentHtml = editor.getData();

			if (!categoryId) { alert('카테고리를 선택해 주세요.'); return; }
			if (!title) { alert('제목을 입력해 주세요.'); return; }

			// writerMemberId는 프로젝트 로그인 정보에서 넣고 싶으면 여기서 주입
			const payload = { categoryId: Number(categoryId), title, contentHtml, writerMemberId: null };

			try {
				const res = await fetch('/api/manager/qna', {
					method: 'POST',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify(payload)
				});

				if (!res.ok) {
					const txt = await res.text();
					alert(`등록 실패: ${txt}`);
					return;
				}

				const json = await res.json();
				alert('등록되었습니다.');
				location.href = `/admin/manager/qnaDetail/${json.id}`;
			} catch (err) {
				alert(`등록 중 오류: ${err.message}`);
			}
		});
	});
})();
