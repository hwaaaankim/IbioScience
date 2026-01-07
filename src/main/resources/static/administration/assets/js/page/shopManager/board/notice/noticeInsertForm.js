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

		abort() { }
	}

	/**
	 * ✅ CKEditor extraPlugins는 "생성자"로 new 호출됩니다.
	 * 그래서 arrow function 금지. (new 불가)
	 * 반드시 일반 function 또는 class 로 제공해야 합니다.
	 */
	function makeNoticeUploadAdapterPlugin(draftKey) {
		// 여기서 반환하는 "일반 function"이 plugin constructor 역할
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
			console.error('[noticeInsertForm] #notice-detail-editor 엘리먼트를 찾지 못했습니다.');
			return;
		}
		if (!window.ClassicEditor) {
			console.error('[noticeInsertForm] ClassicEditor가 로드되지 않았습니다. ckeditor.js 로딩 순서를 확인하세요.');
			return;
		}

		const uploadPlugin = makeNoticeUploadAdapterPlugin(draftKey);

		editorInstance = await ClassicEditor.create(el, {
			extraPlugins: [uploadPlugin]
		});
	}

	async function createNotice(payload) {
		const res = await fetch('/api/manager/notice', {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify(payload)
		});

		if (!res.ok) {
			const t = await res.text();
			throw new Error('등록 실패: ' + t);
		}
		return await res.json();
	}

	document.addEventListener('DOMContentLoaded', async function() {
		const draftKey = randDraftKey();

		const dk = document.getElementById('notice-detail-draftKey');
		if (dk) dk.value = draftKey;

		try {
			await createEditor(draftKey);
		} catch (e) {
			console.error('[noticeInsertForm] editor create failed:', e);
			alert(e.message || '에디터 생성 중 오류가 발생했습니다.');
			return;
		}

		const saveBtn = document.getElementById('notice-detail-save');
		if (!saveBtn) return;

		saveBtn.addEventListener('click', async function() {
			const title = (document.getElementById('notice-detail-title').value || '').trim();
			const contentHtml = editorInstance ? editorInstance.getData() : '';

			if (!title) {
				alert('제목은 필수입니다.');
				return;
			}

			const payload = {
				title: title,
				contentHtml: contentHtml,
				draftKey: draftKey,
				writerMemberId: null,
				writerName: null
			};

			try {
				const json = await createNotice(payload);
				alert('등록되었습니다.');
				window.location.href = '/admin/manager/noticeDetail/' + json.id;
			} catch (e) {
				alert(e.message || '등록 중 오류가 발생했습니다.');
			}
		});
	});
})();
