/* ===== Personal Sign Up (완성) ===== */
(function() {
	"use strict";

	function qs(s, p) { return (p || document).querySelector(s); }
	function qsa(s, p) { return Array.prototype.slice.call((p || document).querySelectorAll(s)); }
	function digits(v) { return v.replace(/\D/g, ''); }

	// 3-4-4 자동 이동
	function bindAutoTab(selector) {
		var arr = qsa(selector);
		arr.forEach(function(input, idx) {
			input.addEventListener('input', function() {
				input.value = digits(input.value);
				if (input.maxLength && input.value.length >= input.maxLength && idx + 1 < arr.length) {
					arr[idx + 1].focus();
				}
			});
		});
	}

	// 약관 행 상태 동기화
	function syncAgreeRowState() {
		qsa('.personalSignUp-agree-item').forEach(function(row) {
			var chk = row.querySelector('.personalSignUp-agree-check');
			if (chk) { row.classList.toggle('is-checked', !!chk.checked); }
		});
	}
	function bindAgreements() {
		var all = qs('#personalSignUp-agree-all');
		var checks = qsa('.personalSignUp-agree-check');
		all.addEventListener('change', function() {
			checks.forEach(function(c) { c.checked = all.checked; });
			syncAgreeRowState();
		});
		checks.forEach(function(c) {
			c.addEventListener('change', function() {
				all.checked = checks.every(function(i) { return i.checked; });
				syncAgreeRowState();
			});
			var label = c.closest('.personalSignUp-agree-item');
			if (label) {
				label.addEventListener('click', function(e) {
					if (e.target.tagName !== 'INPUT') {
						c.checked = !c.checked;
						c.dispatchEvent(new Event('change'));
					}
				});
			}
		});
		syncAgreeRowState();
	}

	// 이메일 도메인 선택
	function bindEmailDomain() {
		var sel = qs('#personalSignUp-email-domain-select');
		var wrap = qs('#personalSignUp-email-domain-direct-wrap');
		var input = qs('#personalSignUp-email-domain-direct');
		sel.addEventListener('change', function() {
			if (sel.value === 'direct') { wrap.classList.remove('hidden'); input.focus(); }
			else { wrap.classList.add('hidden'); input.value = ''; }
		});
	}

	// 주소검색
	function openDaumPostcode() {
		new daum.Postcode({
			oncomplete: function(data) {
				qs('#personalSignUp-zipcode').value = data.zonecode || '';
				qs('#personalSignUp-roadAddress').value = (data.roadAddress || data.autoRoadAddress || '');
				qs('#personalSignUp-detailAddress').focus();
			}
		}).open();
	}

	// 비밀번호 일치/불일치
	function bindPwMatch() {
		var p1 = qs('#personalSignUp-password');
		var p2 = qs('#personalSignUp-password2');
		var msg = qs('#personalSignUp-pwMsg');
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

	// 아이디 중복검사(연동 포인트)
	function bindIdCheck() {
		qs('#personalSignUp-idCheckBtn').addEventListener('click', function() {
			var v = qs('#personalSignUp-userId').value.trim();
			if (!v) { alert('아이디를 입력하세요.'); return; }
			// TODO: /api/members/id-check?id=... 연동
			alert('사용 가능한 아이디로 가정합니다. (API 연동 예정)');
		});
	}

	// 제출
	function bindSubmit() {
		qs('#personalSignUp-addrSearchBtn').addEventListener('click', openDaumPostcode);
		qs('#personalSignUp-submitBtn').addEventListener('click', function() {
			// 필수 약관
			var ok = qsa('.personalSignUp-agree-required').every(function(c) { return c.checked; });
			if (!ok) { alert('[필수] 약관에 모두 동의해 주세요.'); return; }

			function val(id) { return qs(id).value.trim(); }
			var userId = val('#personalSignUp-userId');
			var p1 = val('#personalSignUp-password');
			var p2 = val('#personalSignUp-password2');
			var name = val('#personalSignUp-name');
			var zip = val('#personalSignUp-zipcode');
			var road = val('#personalSignUp-roadAddress');
			var detail = val('#personalSignUp-detailAddress');
			var company = val('#personalSignUp-company');
			if (!userId || !p1 || !p2 || !name || !zip || !road || !detail || !company) {
				alert('필수 항목을 모두 입력해 주세요.'); return;
			}
			if (p1 !== p2) { alert('비밀번호가 일치하지 않습니다.'); return; }

			var tel = qsa('.personalSignUp-tel').map(function(i) { return i.value.trim(); }).join('-');
			var mobile = qsa('.personalSignUp-mobile').map(function(i) { return i.value.trim(); }).join('-');

			var emailId = val('#personalSignUp-email-id');
			var sel = qs('#personalSignUp-email-domain-select').value;
			var direct = val('#personalSignUp-email-domain-direct');
			var domain = (sel === 'direct') ? direct : sel;
			if (!emailId || !domain) { alert('이메일을 입력해 주세요.'); return; }
			var email = emailId + '@' + domain;

			var payload = {
				userId: userId, pw: p1, name: name,
				tel: tel, mobile: mobile, email: email,
				zipCode: zip, roadAddress: road, detailAddress: detail,
				company: company,
				agree: {
					terms: qs('#personalSignUp-agree-terms').checked,
					privacy: qs('#personalSignUp-agree-privacy').checked,
					sms: qs('#personalSignUp-agree-sms').checked,
					email: qs('#personalSignUp-agree-email').checked
				}
			};
			console.log('submit payload', payload);
			alert('회원가입 데이터가 콘솔에 출력되었습니다. 실제 API와 연동해 주세요.');
		});
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
