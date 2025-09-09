(function() {
	"use strict";

	var $ = function(s, p) { return (p || document).querySelector(s); };
	var $$ = function(s, p) { return Array.prototype.slice.call((p || document).querySelectorAll(s)); };
	var digits = function(v) { return String(v || '').replace(/\D/g, ''); };

	// ✅ 아이디 중복검사 강제용 플래그
	var idChecked = false;
	var lastCheckedId = '';

	// ✅ 요소/문자열 모두 안전 처리하는 join
	var join = function(arr) {
		return arr
			.map(function(v) {
				var val = (typeof v === 'string') ? v : (v && 'value' in v ? v.value : '');
				return String(val).trim();
			})
			.filter(Boolean)
			.join('-');
	};

	function showStep(n) {
		$('#companySignUp-step1').style.display = (n === 1 ? '' : 'none');
		$('#companySignUp-step2').style.display = (n === 2 ? '' : 'none');
		$$('#companySignUp-stepper .companySignUp-step').forEach(function(el) {
			el.classList.toggle('companySignUp-active', parseInt(el.getAttribute('data-step'), 10) === n);
		});
		window.scrollTo({ top: 0, behavior: 'smooth' });
	}

	// 약관 시각 상태
	function syncAgreeRowState() {
		$$('.companySignUp-agree-item').forEach(function(row) {
			var chk = row.querySelector('.companySignUp-agree-check');
			if (chk) { row.classList.toggle('is-checked', !!chk.checked); }
		});
	}
	function bindAgreements() {
		var all = $('#companySignUp-agree-all');
		var checks = $$('.companySignUp-agree-check').filter(function(c) {
			return c.id !== 'companySignUp-agree-all';
		});

		all.addEventListener('change', function() {
			checks.forEach(function(c) { c.checked = all.checked; });
			syncAgreeRowState();
		});

		checks.forEach(function(c) {
			c.addEventListener('change', function() {
				all.checked = checks.every(function(i) { return i.checked; });
				syncAgreeRowState();
			});
			var label = c.closest('.companySignUp-agree-item');
			if (label) {
				label.addEventListener('click', function(e) {
					if (e.target.tagName !== 'INPUT') {
						c.checked = !c.checked;
						c.dispatchEvent(new Event('change'));
					}
				});
			}
		});
		var allLabel = all.closest('.companySignUp-agree-item');
		if (allLabel) {
			allLabel.addEventListener('click', function(e) {
				if (e.target.tagName !== 'INPUT') {
					all.checked = !all.checked;
					all.dispatchEvent(new Event('change'));
				}
			});
		}
		syncAgreeRowState();
	}

	function bindEmailDomain(selId, wrapId, inputId) {
		var sel = $(selId), wrap = $(wrapId), input = $(inputId);
		if (!sel) return;
		sel.addEventListener('change', function() {
			if (sel.value === 'direct') { wrap.classList.remove('hidden'); input.focus(); }
			else { wrap.classList.add('hidden'); input.value = ''; }
		});
	}

	function bindAutoTab(selector) {
		var arr = $$(selector);
		arr.forEach(function(input, idx) {
			input.addEventListener('input', function() {
				input.value = digits(input.value);
				if (input.maxLength && input.value.length >= input.maxLength && idx + 1 < arr.length) {
					arr[idx + 1].focus();
				}
			});
		});
	}

	function bindPwMatch() {
		var p1 = $('#companySignUp-password'), p2 = $('#companySignUp-password2'), msg = $('#companySignUp-pwMsg');
		function check() {
			msg.className = 'companySignUp-pwMsg';
			p2.classList.remove('is-valid', 'is-invalid');
			if (!p1.value || !p2.value) { msg.textContent = ''; return; }
			if (p1.value === p2.value) { msg.textContent = '비밀번호가 일치합니다.'; msg.classList.add('ok'); p2.classList.add('is-valid'); }
			else { msg.textContent = '비밀번호가 일치하지 않습니다.'; msg.classList.add('ng'); p2.classList.add('is-invalid'); }
		}
		p1.addEventListener('input', check); p2.addEventListener('input', check);
		p1.addEventListener('change', check); p2.addEventListener('change', check);
	}

	function openDaum(onDone) { new daum.Postcode({ oncomplete: onDone }).open(); }
	function bindAddressButtons() {
		$('#companySignUp-a-addrSearchBtn').addEventListener('click', function() {
			openDaum(function(data) {
				$('#companySignUp-a-zipcode').value = data.zonecode || '';
				$('#companySignUp-a-roadAddress').value = (data.roadAddress || data.autoRoadAddress || '');
				$('#companySignUp-a-detailAddress').focus();
			});
		});
		$('#companySignUp-addrSearchBtn').addEventListener('click', function() {
			openDaum(function(data) {
				$('#companySignUp-zipcode').value = data.zonecode || '';
				$('#companySignUp-roadAddress').value = (data.roadAddress || data.autoRoadAddress || '');
				$('#companySignUp-detailAddress').focus();
			});
		});
	}

	// 파일 업로드(미리보기)
	function bindFileUpload() {
		var fileInput = $('#companySignUp-bizRegFile');
		var btn = $('#companySignUp-bizRegBtn');
		var preview = $('#companySignUp-filePreview');

		function clearPreview() { preview.innerHTML = ''; fileInput.value = ''; }

		btn.addEventListener('click', function() { fileInput.click(); });
		fileInput.addEventListener('change', function() {
			preview.innerHTML = '';
			if (!fileInput.files || !fileInput.files.length) return;
			var f = fileInput.files[0];

			var removeBtn = document.createElement('span');
			removeBtn.className = 'remove';
			removeBtn.innerHTML = '&times;';
			removeBtn.addEventListener('click', clearPreview);

			if (/^image\//.test(f.type)) {
				var img = document.createElement('img'); img.className = 'thumb'; img.alt = '미리보기';
				var reader = new FileReader();
				reader.onload = function(e) { img.src = e.target.result; };
				reader.readAsDataURL(f);
				preview.appendChild(img); preview.appendChild(removeBtn);
			} else {
				var tag = document.createElement('span'); tag.className = 'fileTag'; tag.textContent = f.name;
				preview.appendChild(tag); preview.appendChild(removeBtn);
			}
		});
	}

	// 아이디 중복조회 API
	async function apiUsernameExists(username) {
		const res = await fetch('/api/customer/username-exists?username=' + encodeURIComponent(username));
		if (!res.ok) throw new Error('중복조회 실패');
		const data = await res.json();
		return !!(data && data.exists);
	}

	// ✅ 아이디 중복검사(버튼 누른 경우에만 통과로 기록)
	function bindIdCheck() {
		const $userId = $('#companySignUp-userId');

		// 아이디가 바뀌면 플래그 초기화
		['input', 'change', 'blur'].forEach(function(evt) {
			$userId.addEventListener(evt, function() {
				if ($userId.value.trim() !== lastCheckedId) {
					idChecked = false;
				}
			});
		});

		$('#companySignUp-idCheckBtn').addEventListener('click', async function() {
			var v = $userId.value.trim();
			if (!v) { alert('아이디를 입력하세요.'); $userId.focus(); return; }
			try {
				const exists = await apiUsernameExists(v);
				if (exists) {
					idChecked = false;
					lastCheckedId = '';
					alert('이미 사용 중인 아이디입니다.');
					$userId.focus();
				} else {
					idChecked = true;
					lastCheckedId = v;
					alert('사용 가능한 아이디입니다.');
				}
			} catch (e) {
				idChecked = false;
				lastCheckedId = '';
				alert('아이디 중복조회 중 오류가 발생했습니다.');
			}
		});
	}

	// Step1 검증
	function validateStep1() {
		var ok = true, msg = '';

		if (!$('#companySignUp-agree-terms').checked || !$('#companySignUp-agree-privacy').checked) {
			ok = false; msg = '필수 약관에 동의해 주세요.';
		}

		var id = $('#companySignUp-userId').value.trim();
		var pw1 = $('#companySignUp-password').value;
		var pw2 = $('#companySignUp-password2').value;
		var name = $('#companySignUp-name').value.trim();

		if (ok && (!id || id.length < 4)) { ok = false; msg = '아이디를 4자 이상 입력해 주세요.'; }
		// ✅ 8자 기준 고정
		if (ok && (!pw1 || pw1.length < 5)) { ok = false; msg = '비밀번호는 8자 이상이어야 합니다.'; }
		if (ok && pw1 !== pw2) { ok = false; msg = '비밀번호가 서로 일치하지 않습니다.'; }
		if (ok && !name) { ok = false; msg = '이름을 입력해 주세요.'; }

		var mobile = join($$('.companySignUp-mobileStep1').map(function(el) { return digits(el.value); }));
		if (ok && !/^01[0-9]-[0-9]{3,4}-[0-9]{4}$/.test(mobile)) { ok = false; msg = '휴대폰 번호 형식을 확인해 주세요.'; }

		var emailId = $('#companySignUp-email-id').value.trim();
		var emailSel = $('#companySignUp-email-domain-select').value;
		var emailDir = $('#companySignUp-email-domain-direct').value.trim();
		var emailDomain = (emailSel === 'direct') ? emailDir : emailSel;
		var email = (emailId && emailDomain) ? (emailId + '@' + emailDomain) : '';
		if (ok && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) { ok = false; msg = '이메일 형식을 확인해 주세요.'; }

		var zip = $('#companySignUp-a-zipcode').value.trim();
		var road = $('#companySignUp-a-roadAddress').value.trim();
		if (ok && (!zip || !road)) { ok = false; msg = '개인 주소(우편번호, 도로명주소)를 입력해 주세요.'; }

		if (!ok) alert(msg);
		return ok;
	}

	// Step2 검증
	function validateStep2() {
		var ok = true, msg = '';
		function v(id) { return $(id).value.trim(); }

		if (!v('#companySignUp-companyName')) { ok = false; msg = '회사명을 입력해 주세요.'; }
		if (ok && !v('#companySignUp-ceo')) { ok = false; msg = '대표자를 입력해 주세요.'; }
		if (ok && !v('#companySignUp-bizType')) { ok = false; msg = '업태를 입력해 주세요.'; }
		if (ok && !v('#companySignUp-bizItem')) { ok = false; msg = '업종을 입력해 주세요.'; }
		if (ok && (!v('#companySignUp-zipcode') || !v('#companySignUp-roadAddress'))) { ok = false; msg = '회사 주소(우편번호, 도로명주소)를 입력해 주세요.'; }

		if (ok && !v('#companySignUp-managerName')) { ok = false; msg = '담당자명을 입력해 주세요.'; }
		var contact = join($$('.companySignUp-contact').map(function(el) { return digits(el.value); }));
		if (ok && !/^[0-9]{2,3}-[0-9]{3,4}-[0-9]{4}$/.test(contact)) { ok = false; msg = '담당자 연락처 형식을 확인해 주세요.'; }

		if (ok && !$('#companySignUp-orgType').value) { ok = false; msg = '기관분류를 선택해 주세요.'; }
		if (ok && !v('#companySignUp-bizNo')) { ok = false; msg = '사업자등록번호를 입력해 주세요.'; }

		var invId = v('#companySignUp-invoice-email-id');
		var invSel = $('#companySignUp-invoice-email-domain-select').value;
		var invDir = v('#companySignUp-invoice-email-domain-direct');
		var invDomain = (invSel === 'direct') ? invDir : invSel;
		var invEmail = (invId && invDomain) ? (invId + '@' + invDomain) : '';
		if (ok && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(invEmail)) { ok = false; msg = '계산서 이메일 형식을 확인해 주세요.'; }

		if (!ok) alert(msg);
		return ok;
	}

	// 숨은 필드 매핑 (제출 전 최종)
	async function mapToHidden() {
		// 조합값 만들기
		var tel = join($$('.companySignUp-telStep1').map(function(el) { return digits(el.value); }));
		var mobile = join($$('.companySignUp-mobileStep1').map(function(el) { return digits(el.value); }));
		var repTel = join($$('.companySignUp-tel').map(function(el) { return digits(el.value); }));
		var fax = join($$('.companySignUp-fax').map(function(el) { return digits(el.value); }));
		var contact = join($$('.companySignUp-contact').map(function(el) { return digits(el.value); }));

		var emailId = $('#companySignUp-email-id').value.trim();
		var emailSel = $('#companySignUp-email-domain-select').value;
		var emailDir = $('#companySignUp-email-domain-direct').value.trim();
		var emailDom = (emailSel === 'direct') ? emailDir : emailSel;
		var email = (emailId && emailDom) ? (emailId + '@' + emailDom) : '';

		var invId = $('#companySignUp-invoice-email-id').value.trim();
		var invSel = $('#companySignUp-invoice-email-domain-select').value;
		var invDir = $('#companySignUp-invoice-email-domain-direct').value.trim();
		var invDom = (invSel === 'direct') ? invDir : invSel;
		var invoiceEmail = (invId && invDom) ? (invId + '@' + invDom) : '';

		// hidden 매핑
		$('#f-username').value = $('#companySignUp-userId').value.trim();
		$('#f-password').value = $('#companySignUp-password').value;
		$('#f-name').value = $('#companySignUp-name').value.trim();
		$('#f-tel').value = tel;
		$('#f-mobile').value = mobile;
		$('#f-email').value = email;
		$('#f-aPostcode').value = $('#companySignUp-a-zipcode').value.trim();
		$('#f-aRoadAddress').value = $('#companySignUp-a-roadAddress').value.trim();
		$('#f-aDetailAddress').value = $('#companySignUp-a-detailAddress').value.trim();

		$('#f-companyName').value = $('#companySignUp-companyName').value.trim();
		$('#f-department').value = $('#companySignUp-dept').value.trim();
		$('#f-ceoName').value = $('#companySignUp-ceo').value.trim();
		$('#f-businessType').value = $('#companySignUp-bizType').value.trim();
		$('#f-businessItem').value = $('#companySignUp-bizItem').value.trim();
		$('#f-cPostcode').value = $('#companySignUp-zipcode').value.trim();
		$('#f-cRoadAddress').value = $('#companySignUp-roadAddress').value.trim();
		$('#f-cDetailAddress').value = $('#companySignUp-detailAddress').value.trim();
		$('#f-managerName').value = $('#companySignUp-managerName').value.trim();
		$('#f-managerPhone').value = contact;
		$('#f-organizationCategory').value = $('#companySignUp-orgType').value;

		$('#f-businessRegistrationNumber').value = $('#companySignUp-bizNo').value.trim();
		$('#f-representativeTel').value = repTel;
		$('#f-fax').value = fax;
		$('#f-invoiceEmail').value = invoiceEmail;

		$('#f-agreeTerms').value = $('#companySignUp-agree-terms').checked;
		$('#f-agreePrivacy').value = $('#companySignUp-agree-privacy').checked;
		$('#f-agreeSms').value = $('#companySignUp-agree-sms').checked;
		$('#f-agreeEmail').value = $('#companySignUp-agree-email').checked;
	}

	document.addEventListener('DOMContentLoaded', function() {
		bindAgreements();
		bindEmailDomain('#companySignUp-email-domain-select', '#companySignUp-email-domain-direct-wrap', '#companySignUp-email-domain-direct');
		bindEmailDomain('#companySignUp-invoice-email-domain-select', '#companySignUp-invoice-email-domain-direct-wrap', '#companySignUp-invoice-email-domain-direct');
		bindPwMatch();
		bindAutoTab('.companySignUp-telStep1');
		bindAutoTab('.companySignUp-mobileStep1');
		bindAutoTab('.companySignUp-contact');
		bindAutoTab('.companySignUp-tel');
		bindAutoTab('.companySignUp-fax');
		bindIdCheck();
		bindFileUpload();
		bindAddressButtons();

		// ✅ 다음 단계: (1) Step1 검증 통과 (2) 반드시 버튼 중복검사 통과했는지 확인
		$('#companySignUp-nextBtn').addEventListener('click', async function() {
			if (!validateStep1()) return;

			// 수동 중복검사 강제: 아이디가 변경되었거나 검사 안했으면 차단
			const currentId = $('#companySignUp-userId').value.trim();
			if (!idChecked || currentId !== lastCheckedId) {
				alert('아이디 중복검사를 진행해 주세요.');
				$('#companySignUp-userId').focus();
				return;
			}

			// 안전망: 서버 재확인
			try {
				const exists = await apiUsernameExists(currentId);
				if (exists) { alert('이미 사용 중인 아이디입니다. 다른 아이디를 입력해 주세요.'); return; }
			} catch (e) { alert('아이디 중복조회 중 오류가 발생했습니다.'); return; }

			showStep(2);
		});

		$('#companySignUp-prevBtn').addEventListener('click', function() { showStep(1); });

		// ✅ 폼 제출: 반드시 동기적으로 preventDefault → 검증/매핑 → 수동 submit()
		const form = $('#companySignUpForm');
		form.addEventListener('submit', async function(e) {
			e.preventDefault(); // 가장 먼저 막아야 함 (중요)

			// Step2 검증
			if (!validateStep2()) return;

			// 안전망: 서버 중복확인
			const username = $('#companySignUp-userId').value.trim();
			try {
				const exists = await apiUsernameExists(username);
				if (exists) {
					alert('이미 사용 중인 아이디입니다. 다른 아이디를 입력해 주세요.');
					return;
				}
			} catch (err) {
				alert('아이디 중복조회 중 오류가 발생했습니다.');
				return;
			}

			// 숨은 필드 매핑 후 수동 제출
			await mapToHidden();
			form.submit();
		});

		showStep(1);
	});
})();
