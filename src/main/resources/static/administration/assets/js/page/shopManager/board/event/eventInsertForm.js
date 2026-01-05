document.addEventListener('DOMContentLoaded', () => {

	// =========================
	// 대표이미지 미리보기/삭제
	// =========================
	const repInput = document.querySelector('.event-insert-rep');
	const previewWrap = document.querySelector('.event-rep-preview-wrap');
	const previewImg = document.querySelector('.event-rep-preview-img');
	const previewRemoveBtn = document.querySelector('.event-rep-preview-remove');

	let currentObjectUrl = null;

	function clearPreview() {
		// input 초기화
		if (repInput) repInput.value = '';

		// objectURL 해제
		if (currentObjectUrl) {
			URL.revokeObjectURL(currentObjectUrl);
			currentObjectUrl = null;
		}

		// UI 숨김
		if (previewImg) previewImg.removeAttribute('src');
		if (previewWrap) previewWrap.classList.add('d-none');
	}

	function showPreview(file) {
		if (!file) {
			clearPreview();
			return;
		}

		// 이미지 타입만 허용 (안전)
		if (!file.type || !file.type.startsWith('image/')) {
			alert('이미지 파일만 선택할 수 있습니다.');
			clearPreview();
			return;
		}

		// 이전 objectURL 해제
		if (currentObjectUrl) {
			URL.revokeObjectURL(currentObjectUrl);
			currentObjectUrl = null;
		}

		currentObjectUrl = URL.createObjectURL(file);

		if (previewImg) previewImg.src = currentObjectUrl;
		if (previewWrap) previewWrap.classList.remove('d-none');
	}

	if (repInput) {
		repInput.addEventListener('change', (e) => {
			const file = e.target.files && e.target.files[0] ? e.target.files[0] : null;
			showPreview(file);
		});
	}

	if (previewRemoveBtn) {
		previewRemoveBtn.addEventListener('click', () => {
			clearPreview();
		});
	}

	// =========================
	// CKEditor 업로드 어댑터
	// =========================
	class EventUploadAdapter {
		constructor(loader) {
			this.loader = loader;
		}
		async upload() {
			const file = await this.loader.file;
			const data = new FormData();
			data.append('upload', file);

			const res = await fetch('/api/manager/event/upload-temp', {
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

	function EventUploadAdapterPlugin(editor) {
		editor.plugins.get('FileRepository').createUploadAdapter = (loader) => {
			return new EventUploadAdapter(loader);
		};
	}

	const el = document.getElementById('event-insert-editor');
	if (el) {
		ClassicEditor
			.create(el, {
				extraPlugins: [EventUploadAdapterPlugin],
			})
			.catch(err => {
				console.error(err);
				alert('에디터 로드 실패');
			});
	}

});
