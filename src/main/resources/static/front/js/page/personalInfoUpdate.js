/* eslint-disable */
(function() {
	const $ = (s, p = document) => p.querySelector(s);
	const $$ = (s, p = document) => Array.from(p.querySelectorAll(s));

	// ====== 비밀번호 일치 메시지 ======
	const pw1 = $('#personalInfoUpdate-password');
	const pw2 = $('#personalInfoUpdate-password2');
	const pwMsg = $('#personalInfoUpdate-pwMessage');

	function showPwState() {
		if (!pw1.value || !pw2.value) { pwMsg.textContent = ''; pwMsg.className = 'personalInfoUpdate-msg'; return; }
		if (pw1.value === pw2.value) {
			pwMsg.textContent = '비밀번호가 일치합니다.'; pwMsg.className = 'personalInfoUpdate-msg ok';
		} else {
			pwMsg.textContent = '비밀번호가 일치하지 않습니다.'; pwMsg.className = 'personalInfoUpdate-msg err';
		}
	}
	pw1.addEventListener('input', showPwState);
	pw2.addEventListener('input', showPwState);
	pw2.addEventListener('change', showPwState);

	// ====== 숫자만 허용 & 자동 이동 (휴대폰/유선) ======
	function numericOnlyAutoNext(inputs) {
		inputs.forEach((el, idx) => {
			el.addEventListener('input', (e) => {
				// 숫자만
				e.target.value = e.target.value.replace(/\D/g, '');
				// 길이 채워지면 다음으로
				const max = Number(e.target.getAttribute('maxlength') || 0);
				if (max && e.target.value.length >= max) {
					const next = inputs[idx + 1];
					if (next) next.focus();
				}
			});
			// 백스페이스 시 이전으로 이동
			el.addEventListener('keydown', (e) => {
				if (e.key === 'Backspace' && el.selectionStart === 0 && el.selectionEnd === 0) {
					const prev = inputs[idx - 1];
					if (prev) prev.focus();
				}
			});
		});
	}
	numericOnlyAutoNext($$('.personalInfoUpdate-phone'));
	numericOnlyAutoNext($$('.personalInfoUpdate-tel'));

	// ====== 이메일 검증 ======
	const email = $('#personalInfoUpdate-email');
	const emailMsg = $('#personalInfoUpdate-emailMsg');
	const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

	function showEmailState() {
		if (!email.value) { emailMsg.textContent = ''; emailMsg.className = 'personalInfoUpdate-msg'; return; }
		if (emailPattern.test(email.value)) {
			emailMsg.textContent = '올바른 이메일 형식입니다.'; emailMsg.className = 'personalInfoUpdate-msg ok';
		} else {
			emailMsg.textContent = '이메일 형식이 올바르지 않습니다. 예) name@example.com'; emailMsg.className = 'personalInfoUpdate-msg err';
		}
	}
	email.addEventListener('input', showEmailState);
	email.addEventListener('change', showEmailState);

	// ====== Daum 우편번호 검색 ======
	const zipBtn = $('#personalInfoUpdate-addrSearchBtn');
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
				// 기본값
				const roadAddr = data.roadAddress || '';
				const jibunAddr = data.jibunAddress || data.autoJibunAddress || '';

				$('#personalInfoUpdate-zip').value = data.zonecode || '';
				$('#personalInfoUpdate-road').value = roadAddr;
				$('#personalInfoUpdate-jibun').value = jibunAddr;

				// 상세주소 포커스
				$('#personalInfoUpdate-detail').focus();
			}
		}).open();
	}
	zipBtn.addEventListener('click', () => ensureDaumScriptLoaded(openPostcode));

	// ====== 아이디 중복검색 (샘플 동작) ======
	$('#personalInfoUpdate-dupCheckBtn').addEventListener('click', () => {
		const id = $('#personalInfoUpdate-username').value.trim();
		if (!id) { alert('아이디를 입력하세요.'); return; }
		// TODO: 실제 중복검사 API 호출
		console.log('[중복검사 요청]', id);
		alert('중복검사 API 연동 예정입니다.');
	});

	// ====== 제출 검증 ======
	$('#personalInfoUpdate-form').addEventListener('submit', (e) => {
		// 기본 HTML5 검증
		if (!e.target.checkValidity()) {
			e.preventDefault();
			e.stopPropagation();
			alert('필수 항목을 확인해 주세요.');
			return;
		}
		// 비밀번호 일치
		if (pw1.value !== pw2.value) {
			e.preventDefault(); e.stopPropagation();
			alert('비밀번호가 일치하지 않습니다.');
			pw2.focus();
			return;
		}
		// 이메일 형식
		if (!emailPattern.test(email.value)) {
			e.preventDefault(); e.stopPropagation();
			alert('이메일 형식을 확인해 주세요.');
			email.focus();
			return;
		}

		// 휴대폰 3-4-4 길이 체크
		const m1 = $('#personalInfoUpdate-m1').value.trim();
		const m2 = $('#personalInfoUpdate-m2').value.trim();
		const m3 = $('#personalInfoUpdate-m3').value.trim();
		if (!(m1.length === 3 && m2.length === 4 && m3.length === 4)) {
			e.preventDefault(); e.stopPropagation();
			alert('휴대폰 번호를 올바르게 입력해 주세요.');
			$('#personalInfoUpdate-m1').focus();
			return;
		}

		// 주소 필수(우편/도로/지번 중 하나라도 비었으면 막기)
		if (!$('#personalInfoUpdate-zip').value || !$('#personalInfoUpdate-road').value || !$('#personalInfoUpdate-jibun').value) {
			e.preventDefault(); e.stopPropagation();
			alert('주소를 검색하여 입력해 주세요.');
			$('#personalInfoUpdate-addrSearchBtn').focus();
			return;
		}

		// 수신동의 선택 확인
		if (!$('input[name="agree"]:checked')) {
			e.preventDefault(); e.stopPropagation();
			alert('수신동의를 선택해 주세요.');
			return;
		}

		// TODO: 실제 제출 로직(ajax 또는 기본 submit) 적용
		console.log('[제출]', {
			username: $('#personalInfoUpdate-username').value,
			name: $('#personalInfoUpdate-name').value,
			email: $('#personalInfoUpdate-email').value
		});
	});
})();
