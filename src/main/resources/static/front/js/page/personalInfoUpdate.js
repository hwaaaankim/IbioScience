/* eslint-disable */
(function() {
	const $ = (s, p = document) => p.querySelector(s);
	const $$ = (s, p = document) => Array.from(p.querySelectorAll(s));

	const form = $('#personalInfoUpdate-form');
	const submitBtn = $('#personalInfoUpdate-submitBtn');
	const originalUsername = form.getAttribute('data-original-username') || '';

	// ==== 공통 유틸 ====
	const trim = (v) => (v == null ? '' : String(v).trim());
	function addInvalid(el) {
		el.classList.add('is-invalid');
		el.style.outline = '2px solid #dc3545';
		el.style.outlineOffset = '2px';
	}
	function clearInvalid(el) {
		el.classList.remove('is-invalid');
		el.style.outline = '';
		el.style.outlineOffset = '';
		el.setCustomValidity('');
	}
	function invalidate(el, message, errors) {
		if (!el) return;
		el.setCustomValidity(message || '잘못된 값입니다.');
		addInvalid(el);
		if (message) errors.push(message);
	}
	function focusFirstInvalid() {
		form.reportValidity();
		const el = form.querySelector('.is-invalid') || form.querySelector(':invalid');
		if (el) {
			el.scrollIntoView({ behavior: 'smooth', block: 'center' });
			el.focus({ preventScroll: true });
		}
	}
	function setDirty() {
		if (submitBtn.disabled) submitBtn.disabled = false;
	}

	// ==== 비밀번호 일치 표시 ====
	const pw1 = $('#personalInfoUpdate-password');
	const pw2 = $('#personalInfoUpdate-password2');
	const pwMsg = $('#personalInfoUpdate-pwMessage');

	function showPwState() {
		if (!pw1.value && !pw2.value) {
			pwMsg.textContent = '';
			pwMsg.className = 'personalInfoUpdate-msg';
			return;
		}
		if (pw1.value && !pw2.value) {
			pwMsg.textContent = '비밀번호 확인을 입력해 주세요.';
			pwMsg.className = 'personalInfoUpdate-msg err';
			return;
		}
		if (pw1.value === pw2.value) {
			pwMsg.textContent = '비밀번호가 일치합니다.';
			pwMsg.className = 'personalInfoUpdate-msg ok';
		} else {
			pwMsg.textContent = '비밀번호가 일치하지 않습니다.';
			pwMsg.className = 'personalInfoUpdate-msg err';
		}
	}
	pw1.addEventListener('input', () => { showPwState(); setDirty(); clearInvalid(pw1); });
	pw2.addEventListener('input', () => { showPwState(); setDirty(); clearInvalid(pw2); });
	pw2.addEventListener('change', showPwState);

	// ==== 숫자만 & 자동 이동 ====
	function numericOnlyAutoNext(inputs) {
		inputs.forEach((el, idx) => {
			el.addEventListener('input', (e) => {
				e.target.value = e.target.value.replace(/\D/g, '');
				const max = Number(e.target.getAttribute('maxlength') || 0);
				if (max && e.target.value.length >= max) {
					const next = inputs[idx + 1];
					if (next) next.focus();
				}
				setDirty();
				clearInvalid(el);
			});
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

	// ==== 이메일 검증 ====
	const email = $('#personalInfoUpdate-email');
	const emailMsg = $('#personalInfoUpdate-emailMsg');
	const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

	function showEmailState() {
		if (!email.value) {
			emailMsg.textContent = '';
			emailMsg.className = 'personalInfoUpdate-msg';
			return;
		}
		if (emailPattern.test(email.value)) {
			emailMsg.textContent = '올바른 이메일 형식입니다.';
			emailMsg.className = 'personalInfoUpdate-msg ok';
		} else {
			emailMsg.textContent = '이메일 형식이 올바르지 않습니다. 예) name@example.com';
			emailMsg.className = 'personalInfoUpdate-msg err';
		}
	}
	email.addEventListener('input', () => { showEmailState(); setDirty(); clearInvalid(email); });
	email.addEventListener('change', showEmailState);

	// ==== Daum 우편번호 ====
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
				const roadAddr = data.roadAddress || '';
				const jibunAddr = data.jibunAddress || data.autoJibunAddress || '';
				$('#personalInfoUpdate-zip').value = data.zonecode || '';
				$('#personalInfoUpdate-road').value = roadAddr;
				$('#personalInfoUpdate-jibun').value = jibunAddr; // 선택항목
				$('#personalInfoUpdate-detail').focus();
				setDirty();
				clearInvalid($('#personalInfoUpdate-zip'));
				clearInvalid($('#personalInfoUpdate-road'));
			}
		}).open();
	}
	zipBtn.addEventListener('click', () => ensureDaumScriptLoaded(openPostcode));

	// ==== 아이디 중복 체크 ====
	const usernameEl = $('#personalInfoUpdate-username');
	let idChecked = false;
	let lastCheckedId = '';

	async function checkUsernameAvailability(username) {
		const url = `/api/customer/username-exists?username=${encodeURIComponent(username)}`;
		const res = await fetch(url, { method: 'GET', headers: { 'Accept': 'application/json' } });
		if (!res.ok) throw new Error('중복검사 호출 실패');
		const data = await res.json(); // { exists: boolean }
		return !data.exists; // 사용 가능이면 true
	}

	$('#personalInfoUpdate-dupCheckBtn').addEventListener('click', async () => {
		const id = trim(usernameEl.value);
		if (!id) {
			alert('아이디를 입력하세요.');
			invalidate(usernameEl, '아이디는 필수입니다.', []);
			focusFirstInvalid();
			return;
		}
		if (id === originalUsername) {
			idChecked = true;
			lastCheckedId = id;
			clearInvalid(usernameEl);
			alert('현재 사용 중인 아이디입니다. 사용 가능합니다.');
			return;
		}
		try {
			const available = await checkUsernameAvailability(id);
			if (available) {
				idChecked = true;
				lastCheckedId = id;
				clearInvalid(usernameEl);
				alert('사용 가능한 아이디입니다.');
			} else {
				idChecked = false;
				lastCheckedId = '';
				alert('이미 사용 중인 아이디입니다.');
				invalidate(usernameEl, '이미 사용 중인 아이디입니다.', []);
				focusFirstInvalid();
			}
		} catch (e) {
			console.error(e);
			alert('중복검사 중 오류가 발생했습니다. 다시 시도해 주세요.');
		}
	});

	usernameEl.addEventListener('input', (e) => {
		const current = trim(e.target.value);
		if (current === originalUsername) {
			idChecked = true;
			lastCheckedId = current;
			clearInvalid(usernameEl);
		} else {
			idChecked = false;
			lastCheckedId = '';
		}
		setDirty();
	});

	// ==== 폼 변경 → 버튼 활성화 & invalid 해제 ====
	$$('input, select, textarea', form).forEach((el) => {
		el.addEventListener('input', () => clearInvalid(el));
		el.addEventListener('change', () => clearInvalid(el));
		if (![
			'personalInfoUpdate-password', 'personalInfoUpdate-password2', 'personalInfoUpdate-email',
			'personalInfoUpdate-m1', 'personalInfoUpdate-m2', 'personalInfoUpdate-m3',
			'personalInfoUpdate-t1', 'personalInfoUpdate-t2', 'personalInfoUpdate-t3',
			'personalInfoUpdate-username'
		].includes(el.id)) {
			el.addEventListener('input', setDirty);
			el.addEventListener('change', setDirty);
		}
	});

	// ==== 제출 검증(항목별로 구체 메시지) ====
	form.addEventListener('submit', (e) => {
		// 기존 invalid 제거
		$$('input, select, textarea', form).forEach(clearInvalid);
		const errors = [];

		// 1) 필수값(브라우저 required와 동일하게, 메시지를 사람말로)
		const nameEl = $('#personalInfoUpdate-name');
		if (!trim(nameEl.value)) invalidate(nameEl, '이름을 입력해 주세요.', errors);

		if (!trim(usernameEl.value)) invalidate(usernameEl, '아이디를 입력해 주세요.', errors);

		if (!trim(email.value)) invalidate(email, '이메일을 입력해 주세요.', errors);

		const m1 = $('#personalInfoUpdate-m1');
		const m2 = $('#personalInfoUpdate-m2');
		const m3 = $('#personalInfoUpdate-m3');
		if (!trim(m1.value) || !trim(m2.value) || !trim(m3.value)) {
			invalidate(m1, '휴대폰 번호(3-4-4)를 모두 입력해 주세요.', errors);
			addInvalid(m2); addInvalid(m3);
		}

		const zip = $('#personalInfoUpdate-zip');
		const road = $('#personalInfoUpdate-road');
		if (!trim(zip.value)) invalidate(zip, '우편번호가 비어 있습니다.', errors);
		if (!trim(road.value)) invalidate(road, '도로명주소가 비어 있습니다.', errors);
		// ✅ 지번주소는 선택값이므로 검증하지 않음

		// 2) 아이디 중복검사 (아이디 변경시에만)
		const currentId = trim(usernameEl.value);
		if (currentId !== originalUsername) {
			if (!idChecked || lastCheckedId !== currentId) {
				invalidate(usernameEl, '아이디 중복검사를 완료해 주세요.', errors);
			}
		}

		// 3) 이메일 형식
		if (trim(email.value) && !emailPattern.test(email.value)) {
			invalidate(email, '이메일 형식이 올바르지 않습니다. 예) name@example.com', errors);
		}

		// 4) 휴대폰 형식(3-4-4)
		const hpOk = trim(m1.value).length === 3 && trim(m2.value).length === 4 && trim(m3.value).length === 4;
		if (trim(m1.value) || trim(m2.value) || trim(m3.value)) {
			if (!hpOk) {
				invalidate(m1, '휴대폰 번호는 3-4-4 자리로 입력해 주세요.', errors);
				addInvalid(m2); addInvalid(m3);
			}
		}

		// 5) 비밀번호 규칙
		const p1 = pw1.value;
		const p2 = pw2.value;
		if (!p1 && !p2) {
			// 미변경 OK
		} else {
			if (!p1 || !p2) {
				invalidate(pw1, '비밀번호와 비밀번호 확인을 모두 입력해 주세요.', errors);
				addInvalid(pw2);
			} else if (p1 !== p2) {
				invalidate(pw2, '비밀번호가 일치하지 않습니다.', errors);
			} else if (p1.length < 8 || p1.length > 32) {
				invalidate(pw1, '비밀번호는 8~32자여야 합니다.', errors);
			}
		}

		if (errors.length > 0) {
			e.preventDefault();
			e.stopPropagation();
			const list = Array.from(new Set(errors.filter(Boolean)));
			alert('다음 항목을 확인해 주세요:\n\n- ' + list.join('\n- '));
			focusFirstInvalid();
			return;
		}

		// 마지막 방어: 브라우저 기본 검증
		if (!form.checkValidity()) {
			e.preventDefault();
			e.stopPropagation();
			focusFirstInvalid();
		}
	});
})();
