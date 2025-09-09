/* ===== Personal Sign Up (검증+제출, 중복검사 API 연동완료) ===== */
(function() {
	"use strict";
	const $ = (s, p) => (p || document).querySelector(s);
	const $$ = (s, p) => Array.from((p || document).querySelectorAll(s));
	const digits = (v) => v.replace(/\D/g, '');

	function bindAutoTab(selector) {
		const arr = $$(selector);
		arr.forEach((input, idx) => {
			input.addEventListener('input', () => {
				input.value = digits(input.value);
				if (input.maxLength && input.value.length >= input.maxLength && idx + 1 < arr.length) {
					arr[idx + 1].focus();
				}
			});
		});
	}

	function syncAgreeRowState() {
		$$('.personalSignUp-agree-item').forEach((row) => {
			const chk = row.querySelector('.personalSignUp-agree-check');
			if (chk) row.classList.toggle('is-checked', !!chk.checked);
		});
	}
	function bindAgreements() {
		const all = $('#personalSignUp-agree-all');
		const checks = $$('.personalSignUp-agree-check');
		all.addEventListener('change', () => {
			checks.forEach((c) => (c.checked = all.checked));
			syncAgreeRowState();
		});
		checks.forEach((c) => {
			c.addEventListener('change', () => {
				all.checked = checks.every((i) => i.checked);
				syncAgreeRowState();
			});
			const label = c.closest('.personalSignUp-agree-item');
			if (label) {
				label.addEventListener('click', (e) => {
					if (e.target.tagName !== 'INPUT') {
						c.checked = !c.checked;
						c.dispatchEvent(new Event('change'));
					}
				});
			}
		});
		syncAgreeRowState();
	}

	function bindEmailDomain() {
		const sel = $('#personalSignUp-email-domain-select');
		const wrap = $('#personalSignUp-email-domain-direct-wrap');
		const input = $('#personalSignUp-email-domain-direct');
		sel.addEventListener('change', () => {
			if (sel.value === 'direct') {
				wrap.classList.remove('hidden');
				input.focus();
			} else {
				wrap.classList.add('hidden');
				input.value = '';
			}
		});
	}

	function openDaumPostcode() {
		new daum.Postcode({
			oncomplete: (data) => {
				$('#personalSignUp-zipcode').value = data.zonecode || '';
				$('#personalSignUp-roadAddress').value = (data.roadAddress || data.autoRoadAddress || '');
				$('#personalSignUp-detailAddress').focus();
			}
		}).open();
	}

	function bindPwMatch() {
		const p1 = $('#personalSignUp-password');
		const p2 = $('#personalSignUp-password2');
		const msg = $('#personalSignUp-pwMsg');
		function check() {
			msg.className = 'personalSignUp-pwMsg';
			p2.classList.remove('is-valid', 'is-invalid');
			if (!p1.value || !p2.value) { msg.textContent = ''; return; }
			if (p1.value === p2.value) {
				msg.textContent = '비밀번호가 일치합니다.'; msg.classList.add('ok'); p2.classList.add('is-valid');
			} else {
				msg.textContent = '비밀번호가 일치하지 않습니다.'; msg.classList.add('ng'); p2.classList.add('is-invalid');
			}
		}
		p1.addEventListener('input', check);
		p2.addEventListener('input', check);
		p1.addEventListener('change', check);
		p2.addEventListener('change', check);
	}

	// ===== 아이디 중복검사 (/api/customer/username-exists 사용) =====
	function bindIdCheck() {
		const idInput = $('#personalSignUp-userId');
		const checkedFlag = $('#personalSignUp-idChecked');

		// 아이디가 바뀌면 중복검사 무효화
		function invalidate() {
			checkedFlag.value = 'false';
			idInput.removeAttribute('data-checked-username');
		}
		idInput.addEventListener('input', invalidate);
		idInput.addEventListener('change', invalidate);

		$('#personalSignUp-idCheckBtn').addEventListener('click', async function() {
			const v = idInput.value.trim();
			if (!v) { alert('아이디를 입력하세요.'); idInput.focus(); return; }
			try {
				const resp = await fetch(`/api/customer/username-exists?username=${encodeURIComponent(v)}`, {
					method: 'GET',
					headers: { 'Accept': 'application/json' }
				});
				if (!resp.ok) throw new Error('통신 오류');
				const data = await resp.json(); // { exists: true/false }
				const available = !data.exists;
				if (available) {
					alert('사용 가능한 아이디입니다.');
					checkedFlag.value = 'true';
					idInput.setAttribute('data-checked-username', v);
				} else {
					alert('이미 사용 중인 아이디입니다.');
					checkedFlag.value = 'false';
					idInput.removeAttribute('data-checked-username');
					idInput.focus();
				}
			} catch (e) {
				console.error(e);
				alert('아이디 중복 확인 중 오류가 발생했습니다.');
			}
		});
	}

	function validateAndSubmit() {
		// 1) 필수 약관
		const requiredOk = $$('.personalSignUp-agree-required').every((c) => c.checked);
		if (!requiredOk) { alert('[필수] 약관에 모두 동의해 주세요.'); return; }
		$('#personalSignUp-termsAgreed').value = $('#personalSignUp-agree-terms').checked ? 'true' : 'false';
		$('#personalSignUp-privacyAgreed').value = $('#personalSignUp-agree-privacy').checked ? 'true' : 'false';
		$('#personalSignUp-smsAgreed').value = $('#personalSignUp-agree-sms').checked ? 'true' : 'false';
		$('#personalSignUp-emailAgreed').value = $('#personalSignUp-agree-email').checked ? 'true' : 'false';

		// 2) 아이디/중복검사
		const idInput = $('#personalSignUp-userId');
		const idVal = idInput.value.trim();
		if (!idVal) { alert('아이디를 입력해 주세요.'); idInput.focus(); return; }
		const checked = $('#personalSignUp-idChecked').value === 'true';
		const checkedUsername = idInput.getAttribute('data-checked-username');
		if (!checked || checkedUsername !== idVal) {
			alert('아이디 중복검사를 완료해 주세요.');
			return;
		}

		// 3) 비밀번호
		const p1 = $('#personalSignUp-password').value;
		const p2 = $('#personalSignUp-password2').value;
		if (!p1 || !p2) { alert('비밀번호를 입력해 주세요.'); return; }
		if (p1.length < 5 || p1.length > 16) { alert('비밀번호는 5~16자여야 합니다.'); return; }
		if (p1 !== p2) { alert('비밀번호가 일치하지 않습니다.'); return; }

		// 4) 이름
		if (!$('#personalSignUp-name').value.trim()) { alert('이름을 입력해 주세요.'); $('#personalSignUp-name').focus(); return; }

		// 5) 유선/휴대폰
		const telParts = $$('.personalSignUp-tel').map(i => i.value.trim());
		const mobileParts = $$('.personalSignUp-mobile').map(i => i.value.trim());
		if (telParts.some(v => !v)) { alert('연락처(유선)를 모두 입력해 주세요.'); return; }
		if (mobileParts.some(v => !v)) { alert('핸드폰번호를 모두 입력해 주세요.'); return; }
		$('#personalSignUp-tel-full').value = telParts.join('-');
		$('#personalSignUp-mobile-full').value = mobileParts.join('-');

		// 6) 이메일
		const emailId = $('#personalSignUp-email-id').value.trim();
		const sel = $('#personalSignUp-email-domain-select').value;
		const direct = ($('#personalSignUp-email-domain-direct')?.value || '').trim();
		if (!emailId) { alert('이메일 아이디를 입력해 주세요.'); return; }
		if (!sel) { alert('이메일 도메인을 선택해 주세요.'); return; }
		let domain = sel === 'direct' ? direct : sel;
		if (sel === 'direct' && !domain) { alert('직접입력 도메인을 입력해 주세요.'); return; }
		const emailFull = `${emailId}@${domain}`;
		const emailRe = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
		if (!emailRe.test(emailFull)) { alert('이메일 형식이 올바르지 않습니다.'); return; }
		$('#personalSignUp-email-full').value = emailFull;

		// 7) 주소
		if (!$('#personalSignUp-zipcode').value.trim()) { alert('주소 검색으로 우편번호를 입력해 주세요.'); return; }
		if (!$('#personalSignUp-roadAddress').value.trim()) { alert('주소 검색으로 도로명주소를 입력해 주세요.'); return; }
		if (!$('#personalSignUp-detailAddress').value.trim()) { alert('상세주소를 입력해 주세요.'); return; }

		// 8) 기관/업체명
		if (!$('#personalSignUp-company').value.trim()) { alert('기관/업체명을 입력해 주세요.'); return; }

		// 최종 제출
		$('#personalSignUp-form').submit();
	}

	function bindSubmit() {
		$('#personalSignUp-addrSearchBtn').addEventListener('click', openDaumPostcode);
		$('#personalSignUp-submitBtn').addEventListener('click', validateAndSubmit);
	}

	document.addEventListener('DOMContentLoaded', function() {
		bindAutoTab('.personalSignUp-tel');
		bindAutoTab('.personalSignUp-mobile');
		bindAgreements();
		bindEmailDomain();
		bindPwMatch();
		bindIdCheck();
		bindSubmit();
	});
})();
