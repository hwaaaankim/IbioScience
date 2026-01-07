/* global window, document, fetch, alert, ClassicEditor */
(function() {
	'use strict';

	let editorInstance = null;

	function randDraftKey() {
		if (window.crypto && window.crypto.randomUUID) {
			return window.crypto.randomUUID().replace(/-/g, '').slice(0, 32);
		}
		return (Date.now().toString(36) + Math.random().toString(36).substring(2))
			.replace(/[^a-z0-9]/g, '')
			.slice(0, 32);
	}

	class NoticeUploadAdapter {
		constructor(loader, draftKey) {
			this.loader = loader;
			this.draftKey = draftKey;
		}

		async upload() {
			const file = await this.loader.file;

			const data = new FormData();
			data.append('draftKey', this.draftKey);
			data.append('upload', file);

			const res = await fetch('/api/manager/notice/upload-temp', {
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

		abort() {}
	}

	/**
	 * ✅ CKEditor extraPlugins는 내부에서 new Plugin(...) 형태로 생성합니다.
	 * 따라서 arrow function을 넣으면 "not a constructor"가 납니다.
	 * -> 반드시 "일반 function" 또는 "class"로 제공해야 합니다.
	 */
	function makeNoticeUploadAdapterPlugin(draftKey) {
		function NoticeUploadAdapterPlugin(editor) {
			editor.plugins.get('FileRepository').createUploadAdapter = function(loader) {
				return new NoticeUploadAdapter(loader, draftKey);
			};
		}
		return NoticeUploadAdapterPlugin;
	}

	async function createEditor(draftKey) {
		const el = document.getElementById('notice-detail-editor');
		if (!el) {
			console.error('[noticeDetail] #notice-detail-editor 엘리먼트를 찾지 못했습니다.');
			return;
		}
		if (!window.ClassicEditor) {
			console.error('[noticeDetail] ClassicEditor가 로드되지 않았습니다. ckeditor.js 로딩 순서를 확인하세요.');
			return;
		}

		const uploadPlugin = makeNoticeUploadAdapterPlugin(draftKey);

		editorInstance = await ClassicEditor.create(el, {
			extraPlugins: [uploadPlugin]
		});
	}

	async function updateNotice(id, payload) {
		const res = await fetch('/api/manager/notice/' + id, {
			method: 'PUT',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify(payload)
		});

		if (!res.ok) {
			const t = await res.text();
			throw new Error('수정 실패: ' + t);
		}
		return await res.json();
	}

	document.addEventListener('DOMContentLoaded', async function() {
		const idEl = document.getElementById('notice-detail-id');
		if (!idEl || !idEl.value) {
			console.error('[noticeDetail] notice id가 없습니다.');
			return;
		}

		const id = idEl.value;
		const draftKey = randDraftKey(); // 수정 중 새로 업로드되는 temp 이미지 묶음 키

		const dk = document.getElementById('notice-detail-draftKey');
		if (dk) dk.value = draftKey;

		try {
			await createEditor(draftKey);
		} catch (e) {
			console.error('[noticeDetail] editor create failed:', e);
			alert(e.message || '에디터 생성 중 오류가 발생했습니다.');
			return;
		}

		const updateBtn = document.getElementById('notice-detail-update');
		if (!updateBtn) return;

		updateBtn.addEventListener('click', async function() {
			const title = (document.getElementById('notice-detail-title').value || '').trim();
			const contentHtml = editorInstance ? editorInstance.getData() : '';

			if (!title) {
				alert('제목은 필수입니다.');
				return;
			}

			const payload = {
				title: title,
				contentHtml: contentHtml,
				draftKey: draftKey
			};

			try {
				await updateNotice(id, payload);
				alert('수정되었습니다.');
				window.location.reload();
			} catch (e) {
				alert(e.message || '수정 중 오류가 발생했습니다.');
			}
		});
	});
})();
