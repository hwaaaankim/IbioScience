(function() {
	const $ = (s, p = document) => p.querySelector(s);
	const $$ = (s, p = document) => Array.from(p.querySelectorAll(s));

	// 오늘 날짜 & 임시 회원정보
	const dateInput = $('#inquiry-page-date');
	const memberId = $('#inquiry-page-memberId');
	const memberGrade = $('#inquiry-page-memberGrade');

	dateInput.value = fmt(new Date());
	if (!memberId.value) memberId.value = 'guest@example.com';
	if (!memberGrade.value) memberGrade.value = '일반회원';

	function fmt(d) {
		const y = d.getFullYear(), m = String(d.getMonth() + 1).padStart(2, '0'), da = String(d.getDate()).padStart(2, '0');
		return `${y}-${m}-${da}`;
	}

	// ===== 파일 업로드 누적 관리 =====
	const inputFile = $('#inquiry-page-file');  // 숨김 input[type=file]
	const clearBtn = $('#inquiry-page-clearAll');
	const listWrap = $('#inquiry-page-fileList');
	const form = $('#inquiry-page-form');

	/** @type {File[]} */
	let files = [];

	// 파일 선택 시 -> 누적
	inputFile.addEventListener('change', (e) => {
		const sel = Array.from(e.target.files || []);
		if (!sel.length) return;
		files.push(...sel);
		inputFile.value = '';   // 같은 파일 재선택 가능하도록 초기화
		renderFileList();
	});

	// 전체삭제
	clearBtn.addEventListener('click', () => {
		if (!files.length) return;
		if (!confirm('등록된 모든 파일을 삭제하시겠습니까?')) return;
		files = [];
		renderFileList();
	});

	// 렌더링
	function renderFileList() {
		listWrap.innerHTML = '';
		files.forEach((file, idx) => {
			const isImage = /^image\//.test(file.type);
			if (isImage) {
				const card = document.createElement('div');
				card.className = 'inquiry-page-fileItem';

				const img = document.createElement('img');
				const reader = new FileReader();
				reader.onload = ev => img.src = ev.target.result;
				reader.readAsDataURL(file);

				const del = document.createElement('button');
				del.type = 'button'; del.className = 'inquiry-page-fileRemove'; del.textContent = '×';
				del.addEventListener('click', () => removeAt(idx));

				card.appendChild(img); card.appendChild(del);
				listWrap.appendChild(card);
			} else {
				const chip = document.createElement('div');
				chip.className = 'inquiry-page-fileChip';

				const name = document.createElement('div');
				name.className = 'inquiry-page-fileChip-name';
				name.textContent = file.name;

				const del = document.createElement('button');
				del.type = 'button'; del.className = 'inquiry-page-fileRemove'; del.textContent = '×';
				del.addEventListener('click', () => removeAt(idx));

				chip.appendChild(name); chip.appendChild(del);
				listWrap.appendChild(chip);
			}
		});
	}

	function removeAt(i) {
		files.splice(i, 1);
		renderFileList();
	}

	// 제출(데모)
	form.addEventListener('submit', (e) => {
		e.preventDefault();
		const title = $('#inquiry-page-title').value.trim();
		const topic = $('#inquiry-page-topic').value;
		const content = $('#inquiry-page-content').value.trim();
		if (!title || !topic || !content) {
			alert('제목 / 주제 / 내용을 입력해주세요.');
			return;
		}

		const fd = new FormData();
		fd.append('title', title);
		fd.append('topic', topic);
		fd.append('date', dateInput.value);
		fd.append('memberId', memberId.value);
		fd.append('memberGrade', memberGrade.value);
		fd.append('content', content);
		files.forEach(f => fd.append('files', f, f.name));

		// TODO: fetch('/api/inquiry', { method:'POST', body: fd })
		console.log('[문의 제출] 파일개수:', files.length);
		alert('문의가 제출되었습니다. (데모)');
	});
})();
