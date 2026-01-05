document.addEventListener('DOMContentLoaded', () => {

	// =========================
	// (원본) CKEditor 업로드 어댑터
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

	const editorEl = document.getElementById('event-detail-editor');
	if (editorEl) {
		ClassicEditor
			.create(editorEl, {
				extraPlugins: [EventUploadAdapterPlugin],
			})
			.catch(err => {
				console.error(err);
				alert('에디터 로드 실패');
			});
	}

	// =========================
	// (원본) 삭제 버튼
	// =========================
	const delBtn = document.querySelector('.event-detail-delete-btn');
	if (delBtn) {
		delBtn.addEventListener('click', async () => {
			const id = delBtn.getAttribute('data-id');
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

				// ✅ 원본 유지
				location.href = '/admin/manager/eventManager';
			} catch (e) {
				alert(`삭제 중 오류: ${e.message}`);
			}
		});
	}

	// =========================
	// (추가) 대표이미지 프리뷰 + 초기화(원본 이미지로 복귀)
	// =========================
	const repInput = document.querySelector('.event-detail-rep');
	const previewWrap = document.querySelector('.event-rep-preview-wrap');
	const previewImg = document.querySelector('.event-rep-preview-img');
	const previewEmpty = document.querySelector('.event-rep-preview-empty');
	const resetBtn = document.querySelector('.event-rep-preview-reset');

	let currentObjectUrl = null;

	const originalUrl = previewWrap ? (previewWrap.getAttribute('data-original-url') || '') : '';
	const hasOriginal = !!(originalUrl && originalUrl.trim() !== '');

	function revokeObjectUrlIfAny() {
		if (currentObjectUrl) {
			URL.revokeObjectURL(currentObjectUrl);
			currentObjectUrl = null;
		}
	}

	function showEmpty() {
		if (previewImg) {
			previewImg.removeAttribute('src');
			previewImg.style.display = 'none';
		}
		if (previewEmpty) previewEmpty.style.display = 'flex';
	}

	function showImage(src) {
		if (previewEmpty) previewEmpty.style.display = 'none';
		if (previewImg) {
			previewImg.src = src;
			previewImg.style.display = 'block';
		}
	}

	function setPreviewToOriginal() {
		// input 초기화
		if (repInput) repInput.value = '';

		// objectURL 해제
		revokeObjectUrlIfAny();

		// 원본 이미지가 있으면 원본 표시, 없으면 empty 표시
		if (hasOriginal) {
			showImage(originalUrl);
		} else {
			showEmpty();
		}
	}

	function setPreviewToNewFile(file) {
		if (!file) {
			setPreviewToOriginal();
			return;
		}

		if (!file.type || !file.type.startsWith('image/')) {
			alert('이미지 파일만 선택할 수 있습니다.');
			setPreviewToOriginal();
			return;
		}

		revokeObjectUrlIfAny();
		currentObjectUrl = URL.createObjectURL(file);
		showImage(currentObjectUrl);
	}

	// 최초 로딩: 원본 이미지 or empty
	if (previewWrap) {
		setPreviewToOriginal();
	}

	// 새 이미지 선택 시 프리뷰 교체
	if (repInput) {
		repInput.addEventListener('change', (e) => {
			const file = e.target.files && e.target.files[0] ? e.target.files[0] : null;
			setPreviewToNewFile(file);
		});
	}

	// ‘초기화’ 버튼: 원본으로 복귀
	if (resetBtn) {
		resetBtn.addEventListener('click', () => {
			setPreviewToOriginal();
		});
	}

	// 페이지 이탈 시 objectURL 정리
	window.addEventListener('beforeunload', () => {
		revokeObjectUrlIfAny();
	});

});
