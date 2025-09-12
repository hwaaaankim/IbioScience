/* eslint-disable */
(function() {
	"use strict";

	var $ = function(s, p) { return (p || document).querySelector(s); };
	var $$ = function(s, p) { return Array.prototype.slice.call((p || document).querySelectorAll(s)); };
	var digits = function(v) { return String(v || '').replace(/\D/g, ''); };

	// ===== 공통 유틸 =====
	function joinParts(parts) {
		return parts.map(function(el) { return digits(el.value); })
			.filter(Boolean).join('-');
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

	// ===== 주소 검색(다음) =====
	function openDaum(onDone) { new daum.Postcode({ oncomplete: onDone }).open(); }
	function bindAddressSearch() {
		$('#company-addrSearchBtn').addEventListener('click', function() {
			openDaum(function(data) {
				$('#company-zipcode').value = data.zonecode || '';
				$('#company-roadAddress').value = (data.roadAddress || data.autoRoadAddress || '');
				$('#company-detailAddress').focus();
			});
		});
	}

	// ===== 이메일 도메인 직접입력 토글 =====
	function bindInvoiceEmailDomain() {
		var sel = $('#company-invoice-email-domain-select');
		var wrap = $('#company-invoice-email-domain-direct-wrap');
		var input = $('#company-invoice-email-domain-direct');
		sel.addEventListener('change', function() {
			if (sel.value === 'direct') { wrap.classList.remove('hidden'); input.focus(); }
			else { wrap.classList.add('hidden'); input.value = ''; }
		});
	}

	// ===== 파일 업로드(리스트 미리보기 + X 삭제) =====
	function bindFileList() {
		var input = $('#company-bizRegFiles');
		var btn = $('#company-bizRegBtn');
		var list = $('#company-fileList');

		btn.addEventListener('click', function() { input.click(); });

		function renderList(files) {
			list.innerHTML = '';
			Array.from(files).forEach(function(f, idx) {
				var row = document.createElement('div');
				row.className = 'fileRowItem';

				var name = document.createElement('span');
				name.className = 'fileName';
				name.textContent = f.name;

				var remove = document.createElement('button');
				remove.type = 'button';
				remove.className = 'fileRemove';
				remove.innerHTML = '&times;';
				remove.setAttribute('aria-label', '삭제');

				remove.addEventListener('click', function() {
					// FileList는 불변이므로 DataTransfer로 재구성
					var dt = new DataTransfer();
					Array.from(input.files).forEach(function(file, i) {
						if (i !== idx) dt.items.add(file);
					});
					input.files = dt.files;
					renderList(input.files);
				});

				row.appendChild(name);
				row.appendChild(remove);
				list.appendChild(row);
			});
		}

		input.addEventListener('change', function() {
			renderList(input.files || []);
		});
	}

	// ===== 제출 전 값 조립/검증 =====
	function assembleAndValidate() {
		// 연락처
		var contact = joinParts([
			$('#company-contact-1'), $('#company-contact-2'), $('#company-contact-3')
		]);
		if (!/^[0-9]{2,3}-[0-9]{3,4}-[0-9]{4}$/.test(contact)) {
			alert('담당자 연락처 형식을 확인해 주세요.');
			return false;
		}
		$('#company-managerPhone').value = contact;

		// 대표번호(선택)
		var rep = joinParts([
			$('#company-rep-1'), $('#company-rep-2'), $('#company-rep-3')
		]);
		$('#company-representativeTel').value = rep;

		// 팩스(선택)
		var fax = joinParts([
			$('#company-fax-1'), $('#company-fax-2'), $('#company-fax-3')
		]);
		$('#company-faxHidden').value = fax;

		// 이메일
		var id = $('#company-invoice-email-id').value.trim();
		var sel = $('#company-invoice-email-domain-select').value;
		var dir = $('#company-invoice-email-domain-direct').value.trim();
		var dom = (sel === 'direct') ? dir : sel;
		var full = (id && dom) ? (id + '@' + dom) : '';
		if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(full)) {
			alert('계산서 이메일 형식을 확인해 주세요.');
			return false;
		}
		$('#company-invoiceEmail').value = full;

		// 필수: 주소
		if (!$('#company-zipcode').value.trim() || !$('#company-roadAddress').value.trim()) {
			alert('회사 주소(우편번호, 도로명주소)를 입력해 주세요.');
			return false;
		}

		// 필수: 사업자등록번호
		if (!$('#company-bizNo').value.trim()) {
			alert('사업자등록번호를 입력해 주세요.');
			return false;
		}

		// 필수: 파일 1개 이상
		var files = $('#company-bizRegFiles').files;
		if (!files || !files.length) {
			alert('사업자등록증 파일을 최소 1개 등록해 주세요.');
			return false;
		}

		return true;
	}

	document.addEventListener('DOMContentLoaded', function() {
		bindAddressSearch();
		bindInvoiceEmailDomain();
		bindFileList();
		bindAutoTab('.companySignUp-contact');
		bindAutoTab('.companySignUp-tel');
		bindAutoTab('.companySignUp-fax');

		var form = $('#conversionToCompanyForm');
		form.addEventListener('submit', function(e) {
			// CSRF 미사용이라고 하셨으니 그대로 진행
			if (!assembleAndValidate()) {
				e.preventDefault();
			}
		});
	});
})();
