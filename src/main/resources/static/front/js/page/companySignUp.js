/* ===== Company Sign Up (2-step) ===== */
(function() {
	"use strict";

	// Utils
	var $ = function(s, p) { return (p || document).querySelector(s); };
	var $$ = function(s, p) { return Array.prototype.slice.call((p || document).querySelectorAll(s)); };
	var digits = function(v) { return v.replace(/\D/g, ''); };

	/* ---------- Stepper ---------- */
	function showStep(n) {
		$('#companySignUp-step1').style.display = (n === 1 ? '' : 'none');
		$('#companySignUp-step2').style.display = (n === 2 ? '' : 'none');
		$$('#companySignUp-stepper .companySignUp-step').forEach(function(el) {
			var idx = parseInt(el.getAttribute('data-step'), 10);
			el.classList.toggle('companySignUp-active', idx === n);
		});
		window.scrollTo(0, 0);
	}

	/* ---------- Terms visual ---------- */
	function syncAgreeRowState() {
		$$('.companySignUp-agree-item').forEach(function(row) {
			var chk = row.querySelector('.companySignUp-agree-check');
			if (chk) { row.classList.toggle('is-checked', !!chk.checked); }
		});
	}
	function bindAgreements() {
		var all = $('#companySignUp-agree-all');
		var checks = $$('.companySignUp-agree-check');
		all.addEventListener('change', function() {
			checks.forEach(function(c) { c.checked = all.checked; }); syncAgreeRowState();
		});
		checks.forEach(function(c) {
			c.addEventListener('change', function() {
				all.checked = checks.every(function(i) { return i.checked; });
				syncAgreeRowState();
			});
			var label = c.closest('.companySignUp-agree-item');
			if (label) {
				label.addEventListener('click', function(e) {
					if (e.target.tagName !== 'INPUT') { c.checked = !c.checked; c.dispatchEvent(new Event('change')); }
				});
			}
		});
		syncAgreeRowState();
	}

	/* ---------- Email domain select ---------- */
	function bindEmailDomain(selId, wrapId, inputId) {
		var sel = $(selId), wrap = $(wrapId), input = $(inputId);
		if (!sel) return;
		sel.addEventListener('change', function() {
			if (sel.value === 'direct') { wrap.classList.remove('hidden'); input.focus(); }
			else { wrap.classList.add('hidden'); input.value = ''; }
		});
	}

	/* ---------- AutoTab groups ---------- */
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

	/* ---------- Password match ---------- */
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

	/* ---------- Daum Postcode (step1 & step2) ---------- */
	function openDaum(onDone) {
		new daum.Postcode({ oncomplete: onDone }).open();
	}
	function bindAddressButtons() {
		// STEP1: 개인 주소
		$('#companySignUp-a-addrSearchBtn').addEventListener('click', function() {
			openDaum(function(data) {
				$('#companySignUp-a-zipcode').value = data.zonecode || '';
				$('#companySignUp-a-roadAddress').value = (data.roadAddress || data.autoRoadAddress || '');
				$('#companySignUp-a-detailAddress').focus();
			});
		});
		// STEP2: 회사 주소
		$('#companySignUp-addrSearchBtn').addEventListener('click', function() {
			openDaum(function(data) {
				$('#companySignUp-zipcode').value = data.zonecode || '';
				$('#companySignUp-roadAddress').value = (data.roadAddress || data.autoRoadAddress || '');
				$('#companySignUp-detailAddress').focus();
			});
		});
	}

	/* ---------- File upload (preview/remove) ---------- */
	function bindFileUpload() {
		var fileInput = $('#companySignUp-bizRegFile');
		var btn = $('#companySignUp-bizRegBtn');
		var preview = $('#companySignUp-filePreview');

		btn.addEventListener('click', function() { fileInput.click(); });

		function clearPreview() { preview.innerHTML = ''; fileInput.value = ''; }

		fileInput.addEventListener('change', function() {
			preview.innerHTML = '';
			if (!fileInput.files || fileInput.files.length === 0) return;
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

	/* ---------- ID check (mock) ---------- */
	function bindIdCheck() {
		$('#companySignUp-idCheckBtn').addEventListener('click', function() {
			var v = $('#companySignUp-userId').value.trim();
			if (!v) { alert('아이디를 입력하세요.'); return; }
			// TODO: 실제 API 연동
			alert('사용 가능한 아이디로 가정합니다. (API 연동 예정)');
		});
	}

	/* ---------- Step2 validation & submit ---------- */
	function validateStep2() {
		function val(id) { return $(id).value.trim(); }
		var comp = val('#companySignUp-companyName');
		var ceo = val('#companySignUp-ceo');
		var bizType = val('#companySignUp-bizType');
		var bizItem = val('#companySignUp-bizItem');
		var zip = val('#companySignUp-zipcode');
		var road = val('#companySignUp-roadAddress');
		var detail = val('#companySignUp-detailAddress');
		var mgr = val('#companySignUp-managerName');
		var orgType = $('#companySignUp-orgType').value;

		var contact = $$('.companySignUp-contact').map(function(i) { return i.value.trim(); });
		var invId = val('#companySignUp-invoice-email-id');
		var invSel = $('#companySignUp-invoice-email-domain-select').value;
		var invDirect = val('#companySignUp-invoice-email-domain-direct');
		var invDomain = (invSel === 'direct') ? invDirect : invSel;

		if (!comp || !ceo || !bizType || !bizItem) { alert('회사 기본정보 검증이 필요합니다.'); return false; }
		if (!zip || !road || !detail) { alert('회사 주소 검증이 필요합니다.'); return false; }
		if (!mgr) { alert('담당자명 검증이 필요합니다.'); return false; }
		if (contact.some(function(v) { return !v; })) { alert('담당자 연락처 검증이 필요합니다.'); return false; }
		if (!orgType) { alert('기관분류 검증이 필요합니다.'); return false; }
		if (!$('#companySignUp-bizNo').value.trim()) { alert('사업자등록번호 검증이 필요합니다.'); return false; }
		if (!invId || !invDomain) { alert('계산서 이메일 검증이 필요합니다.'); return false; }
		return true;
	}

	function collectAndSubmit() {
		// step1 account payload
		var account = {
			userId: $('#companySignUp-userId').value.trim(),
			password: $('#companySignUp-password').value,
			name: $('#companySignUp-name').value.trim(),
			tel: $$('.companySignUp-telStep1').map(function(i) { return i.value.trim(); }).join('-'),
			mobile: $$('.companySignUp-mobileStep1').map(function(i) { return i.value.trim(); }).join('-'),
			email: (function() {
				var id = $('#companySignUp-email-id').value.trim();
				var sel = $('#companySignUp-email-domain-select').value;
				var dir = $('#companySignUp-email-domain-direct').value.trim();
				var domain = (sel === 'direct') ? dir : sel;
				return id && domain ? id + '@' + domain : '';
			})(),
			address: {
				zip: $('#companySignUp-a-zipcode').value.trim(),
				road: $('#companySignUp-a-roadAddress').value.trim(),
				detail: $('#companySignUp-a-detailAddress').value.trim()
			}
		};

		// step2 company/tax payload
		var payload = {
			account: account,
			company: {
				name: $('#companySignUp-companyName').value.trim(),
				dept: $('#companySignUp-dept').value.trim(),
				ceo: $('#companySignUp-ceo').value.trim(),
				bizType: $('#companySignUp-bizType').value.trim(),
				bizItem: $('#companySignUp-bizItem').value.trim(),
				address: {
					zip: $('#companySignUp-zipcode').value.trim(),
					road: $('#companySignUp-roadAddress').value.trim(),
					detail: $('#companySignUp-detailAddress').value.trim()
				},
				manager: {
					name: $('#companySignUp-managerName').value.trim(),
					phone: $$('.companySignUp-contact').map(function(i) { return i.value.trim(); }).join('-')
				},
				orgType: $('#companySignUp-orgType').value
			},
			tax: {
				bizNo: $('#companySignUp-bizNo').value.trim(),
				mainTel: $$('.companySignUp-tel').map(function(i) { return i.value.trim(); }).join('-'),
				fax: $$('.companySignUp-fax').map(function(i) { return i.value.trim(); }).join('-'),
				invoiceEmail: (function() {
					var id = $('#companySignUp-invoice-email-id').value.trim();
					var sel = $('#companySignUp-invoice-email-domain-select').value;
					var dir = $('#companySignUp-invoice-email-domain-direct').value.trim();
					var domain = (sel === 'direct') ? dir : sel;
					return id && domain ? id + '@' + domain : '';
				})(),
				bizRegFileName: (function() {
					var f = $('#companySignUp-bizRegFile').files;
					return (f && f.length) ? f[0].name : '';
				})()
			},
			agree: {
				terms: $('#companySignUp-agree-terms').checked,
				privacy: $('#companySignUp-agree-privacy').checked,
				sms: $('#companySignUp-agree-sms').checked,
				email: $('#companySignUp-agree-email').checked
			}
		};

		// TODO: 실제 API 연동
		console.log('companySignUp submit payload', payload);
		alert('기업회원 가입 데이터가 콘솔에 출력되었습니다. 실제 API와 연동해 주세요.');
	}

	/* ---------- Init ---------- */
	document.addEventListener('DOMContentLoaded', function() {
		bindAgreements();
		bindEmailDomain('#companySignUp-email-domain-select', '#companySignUp-email-domain-direct-wrap', '#companySignUp-email-domain-direct');
		bindEmailDomain('#companySignUp-invoice-email-domain-select', '#companySignUp-invoice-email-domain-direct-wrap', '#companySignUp-invoice-email-domain-direct');
		bindPwMatch();
		// AutoTab: 1단계 유선/휴대폰, 2단계 담당자연락처
		bindAutoTab('.companySignUp-telStep1');
		bindAutoTab('.companySignUp-mobileStep1');
		bindAutoTab('.companySignUp-contact');
		bindIdCheck();
		bindFileUpload();
		bindAddressButtons();

		// 다음 단계: 지금은 알럿만 보여주고 진행
		$('#companySignUp-nextBtn').addEventListener('click', function() {
			alert('검증이 필요합니다. (임시로 다음 단계로 진행합니다)');
			showStep(2);
		});

		$('#companySignUp-prevBtn').addEventListener('click', function() { showStep(1); });

		$('#companySignUp-submitBtn').addEventListener('click', function() {
			if (!validateStep2()) return;
			collectAndSubmit();
		});

		showStep(1);
	});
})();
