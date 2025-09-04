/* eslint-disable */
(function() {
	const $ = (s, p = document) => p.querySelector(s);
	const $$ = (s, p = document) => Array.from(p.querySelectorAll(s));

	// =========================
	// 1) 비밀번호 일치 메시지
	// =========================
	const pw1 = $('#companyInfoUpdate-password');
	const pw2 = $('#companyInfoUpdate-password2');
	const pwMsg = $('#companyInfoUpdate-pwMessage');

	function showPwState() {
		if (!pw1 || !pw2 || !pwMsg) return;
		if (!pw1.value || !pw2.value) {
			pwMsg.textContent = '';
			pwMsg.className = 'companyInfoUpdate-msg';
			return;
		}
		if (pw1.value === pw2.value) {
			pwMsg.textContent = '비밀번호가 일치합니다.';
			pwMsg.className = 'companyInfoUpdate-msg ok';
		} else {
			pwMsg.textContent = '비밀번호가 일치하지 않습니다.';
			pwMsg.className = 'companyInfoUpdate-msg err';
		}
	}
	if (pw1 && pw2) {
		pw1.addEventListener('input', showPwState);
		pw2.addEventListener('input', showPwState);
		pw2.addEventListener('change', showPwState);
	}

	// =========================
	// 2) 숫자만 허용 + 자동 이동
	//    (휴대폰/전화/팩스/사업자번호)
	// =========================
	function numericOnlyAutoNext(inputs) {
		inputs.forEach((el, idx) => {
			if (!el) return;
			el.addEventListener('input', (e) => {
				// 숫자만
				e.target.value = e.target.value.replace(/\D/g, '');
				// maxlength 도달 시 다음칸
				const max = Number(e.target.getAttribute('maxlength') || 0);
				if (max && e.target.value.length >= max) {
					const next = inputs[idx + 1];
					if (next) next.focus();
				}
			});
			// 백스페이스로 이전칸 이동
			el.addEventListener('keydown', (e) => {
				if (e.key === 'Backspace' && el.selectionStart === 0 && el.selectionEnd === 0) {
					const prev = inputs[idx - 1];
					if (prev) prev.focus();
				}
			});
		});
	}
	// 휴대폰 (필수)
	numericOnlyAutoNext($$('.companyInfoUpdate-phone'));
	// 회사 전화 (선택, 동일 클래스 companyInfoUpdate-phone 을 HTML에서 재사용했다면 위에서 이미 처리됨)
	// 팩스
	numericOnlyAutoNext($$('.companyInfoUpdate-fax'));
	// 사업자등록번호(3-2-5)
	numericOnlyAutoNext($$('.companyInfoUpdate-biznum'));

	// =========================
	// 3) 이메일 형식 검증
	// =========================
	const email = $('#companyInfoUpdate-email');
	const emailMsg = $('#companyInfoUpdate-emailMsg');
	const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

	function showEmailState() {
		if (!email || !emailMsg) return;
		if (!email.value) {
			emailMsg.textContent = '';
			emailMsg.className = 'companyInfoUpdate-msg';
			return;
		}
		if (emailPattern.test(email.value)) {
			emailMsg.textContent = '올바른 이메일 형식입니다.';
			emailMsg.className = 'companyInfoUpdate-msg ok';
		} else {
			emailMsg.textContent = '이메일 형식이 올바르지 않습니다. 예) name@example.com';
			emailMsg.className = 'companyInfoUpdate-msg err';
		}
	}
	if (email) {
		email.addEventListener('input', showEmailState);
		email.addEventListener('change', showEmailState);
	}

	// =========================
	// 4) Daum 우편번호 검색
	// =========================
	const zipBtn = $('#companyInfoUpdate-addrSearchBtn');

	function ensureDaumScriptLoaded(cb) {
		if (window.daum && window.daum.Postcode) { cb(); return; }
		const s = document.createElement('script');
		s.src = '//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js';
		s.onload = cb;
		document.head.appendChild(s);
	}

	function openPostcode() {
		/* global daum */
		new daum.Postcode({
			oncomplete: function(data) {
				const roadAddr = data.roadAddress || '';
				const jibunAddr = data.jibunAddress || data.autoJibunAddress || '';

				$('#companyInfoUpdate-zip').value = data.zonecode || '';
				$('#companyInfoUpdate-road').value = roadAddr;
				$('#companyInfoUpdate-jibun').value = jibunAddr;

				const detail = $('#companyInfoUpdate-detail');
				if (detail) detail.focus();
			}
		}).open();
	}

	if (zipBtn) {
		zipBtn.addEventListener('click', () => ensureDaumScriptLoaded(openPostcode));
	}

	// =========================
	// 5) 아이디 중복검색 (샘플)
	// =========================
	const dupBtn = $('#companyInfoUpdate-dupCheckBtn');
	if (dupBtn) {
		dupBtn.addEventListener('click', () => {
			const id = $('#companyInfoUpdate-username')?.value?.trim();
			if (!id) { alert('아이디를 입력하세요.'); return; }
			// TODO: 실제 중복검사 API 호출
			console.log('[기업회원 아이디 중복검사 요청]', id);
			alert('중복검사 API 연동 예정입니다.');
		});
	}

	// =========================
	// 6) 사업자등록번호 유효성 검사
	//    - 형식: 3-2-5 = 총 10자리
	//    - 알고리즘: 국세청 가중치 체크
	// =========================
	function isValidKRBizNumber(digits10) {
		// digits10: 숫자만 10자리 문자열
		if (!/^\d{10}$/.test(digits10)) return false;

		const w = [1, 3, 7, 1, 3, 7, 1, 3, 5]; // 마지막 전 자리까지 가중치
		let sum = 0;
		for (let i = 0; i < 9; i++) {
			sum += (parseInt(digits10[i], 10) * w[i]);
		}
		sum += Math.floor((parseInt(digits10[8], 10) * 5) / 10); // 9번째 자리 ×5의 십의 자리 더함
		const check = (10 - (sum % 10)) % 10;
		return check === parseInt(digits10[9], 10);
	}

	function getBizNumberDigits() {
		const parts = $$('.companyInfoUpdate-biznum').map(el => el.value.replace(/\D/g, ''));
		return parts.join('');
	}

	// 마지막 칸에서 blur 시 즉시 안내 (알림으로 처리)
	const biznumInputs = $$('.companyInfoUpdate-biznum');
	if (biznumInputs.length) {
		const last = biznumInputs[biznumInputs.length - 1];
		last.addEventListener('blur', () => {
			const d = getBizNumberDigits();
			if (d.length === 10) {
				if (!isValidKRBizNumber(d)) {
					alert('사업자등록번호가 올바르지 않습니다. 다시 확인해 주세요.');
					biznumInputs[0].focus();
				}
			}
		});
	}

	// =========================
	// 7) 파일 업로드 (1개 제한)
	//    - 이미지: 150x150 미리보기
	//    - 비이미지: 파일명 + 확장자
	//    - X 버튼으로 제거
	// =========================
	const fileInput = $('#companyInfoUpdate-bizFile');
	const previewWrap = $('#companyInfoUpdate-filePreview');

	function clearPreview() {
		if (previewWrap) previewWrap.innerHTML = '';
	}

	function makeRemoveBtn() {
		const btn = document.createElement('button');
		btn.type = 'button';
		btn.className = 'companyInfoUpdate-remove remove-btn';
		btn.textContent = '×';
		btn.addEventListener('click', () => {
			clearPreview();
			if (fileInput) fileInput.value = '';
		});
		return btn;
	}

	function renderPreview(file) {
		clearPreview();
		if (!file || !previewWrap) return;

		const isImage = /^image\//.test(file.type);

		const item = document.createElement('div');
		item.className = 'preview-item';

		if (isImage) {
			const img = document.createElement('img');
			img.alt = '사업자등록증 미리보기';
			item.appendChild(img);

			const reader = new FileReader();
			reader.onload = (e) => { img.src = e.target.result; };
			reader.readAsDataURL(file);
		} else {
			const box = document.createElement('div');
			box.className = 'file-box';
			const name = file.name || '첨부파일';
			const dot = name.lastIndexOf('.');
			const ext = dot !== -1 ? name.slice(dot + 1) : '';
			box.textContent = `${name}${ext ? ` (${ext.toUpperCase()})` : ''}`;
			item.appendChild(box);
		}

		item.appendChild(makeRemoveBtn());
		previewWrap.appendChild(item);
	}

	if (fileInput) {
		fileInput.addEventListener('change', () => {
			const f = fileInput.files && fileInput.files[0];
			if (!f) { clearPreview(); return; }
			// 하나만 유지
			if (fileInput.files.length > 1) {
				alert('파일은 1개만 업로드할 수 있습니다.');
				fileInput.value = '';
				clearPreview();
				return;
			}
			renderPreview(f);
		});
	}

	// =========================
	// 8) 제출 검증
	// =========================
	const form = $('#companyInfoUpdate-form');
	if (form) {
		form.addEventListener('submit', (e) => {
			// HTML5 기본 검증
			if (!form.checkValidity()) {
				e.preventDefault(); e.stopPropagation();
				alert('필수 항목을 확인해 주세요.');
				return;
			}

			// 비밀번호 일치
			if (pw1 && pw2 && pw1.value !== pw2.value) {
				e.preventDefault(); e.stopPropagation();
				alert('비밀번호가 일치하지 않습니다.');
				pw2.focus();
				return;
			}

			// 이메일 형식
			if (email && !emailPattern.test(email.value)) {
				e.preventDefault(); e.stopPropagation();
				alert('이메일 형식을 확인해 주세요.');
				email.focus();
				return;
			}

			// 휴대폰 3-4-4
			const m = $$('.companyInfoUpdate-phone').map(el => (el.value || '').trim());
			if (m.length >= 3) {
				if (!(m[0].length === 3 && m[1].length === 4 && m[2].length === 4)) {
					e.preventDefault(); e.stopPropagation();
					alert('휴대폰 번호를 올바르게 입력해 주세요. (예: 010-1234-5678)');
					$$('.companyInfoUpdate-phone')[0].focus();
					return;
				}
			}

			// 주소 필수(우편/도로/지번)
			const zip = $('#companyInfoUpdate-zip')?.value?.trim();
			const road = $('#companyInfoUpdate-road')?.value?.trim();
			const jibun = $('#companyInfoUpdate-jibun')?.value?.trim();
			if (!zip || !road || !jibun) {
				e.preventDefault(); e.stopPropagation();
				alert('주소를 검색하여 입력해 주세요.');
				$('#companyInfoUpdate-addrSearchBtn')?.focus();
				return;
			}

			// 수신동의
			if (!$('input[name="agree"]:checked')) {
				e.preventDefault(); e.stopPropagation();
				alert('수신동의를 선택해 주세요.');
				return;
			}

			// 사업자등록번호 10자리 + 검증
			const bizDigits = getBizNumberDigits();
			if (bizDigits.length !== 10 || !isValidKRBizNumber(bizDigits)) {
				e.preventDefault(); e.stopPropagation();
				alert('사업자등록번호가 올바르지 않습니다.');
				$$('.companyInfoUpdate-biznum')[0]?.focus();
				return;
			}

			// 파일 1개 제한 확인 (필수라면 이 조건 활성화)
			const bizFile = $('#companyInfoUpdate-bizFile');
			if (bizFile && bizFile.hasAttribute('required')) {
				if (!bizFile.files || bizFile.files.length !== 1) {
					e.preventDefault(); e.stopPropagation();
					alert('사업자등록증 파일을 1개 업로드해 주세요.');
					bizFile.focus();
					return;
				}
			}

			// 제출 로그 (연동 시 제거)
			console.log('[기업회원 수정 제출]', {
				username: $('#companyInfoUpdate-username')?.value,
				name: $('#companyInfoUpdate-name')?.value,
				email: $('#companyInfoUpdate-email')?.value,
				bizNumber: bizDigits
			});
		});
	}
})();
