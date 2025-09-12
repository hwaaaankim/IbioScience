/* eslint-disable */
(function() {
	const $ = (s, p = document) => p.querySelector(s);
	const $$ = (s, p = document) => Array.from(p.querySelectorAll(s));
	const digits = v => String(v || '').replace(/\D/g, '');

	// ====== 초기 데이터 ======
	const INIT = window.__companyInfoInit || {};

	// ====== 공용 유틸 ======
	const setIf = (el, val) => { if (el) el.value = (val ?? ''); };
	const on = (el, ev, fn) => { if (el) el.addEventListener(ev, fn); };

	// ====== 수정 버튼 관리 ======
	const form = $('#companyInfoUpdate-form');
	const submitBtn = form?.querySelector('button[type="submit"]');
	let dirty = false;

	function setDirty() {
		if (!dirty) {
			dirty = true;
			if (submitBtn) submitBtn.disabled = false;
		}
	}
	function initDirty() {
		dirty = false;
		if (submitBtn) submitBtn.disabled = true;
	}

	// ====== 수신동의 라디오: 동의함 고정 및 비활성 ======
	(function lockAgree() {
		const radios = $$('input[name="agree"]');
		radios.forEach(r => {
			if (r.value === 'Y') { r.checked = true; }
			r.disabled = true;
		});
	})();

	// ====== 변경 감지 ======
	function onAnyChangeEnable() {
		$$('.companyInfoUpdate-input, input, select, textarea').forEach(el => {
			el.addEventListener('input', setDirty);
			el.addEventListener('change', setDirty);
		});
	}

	// ====== 아이디 중복검사 ======
	let usernameDupChecked = false;
	let lastCheckedUsername = '';
	const usernameInput = $('#companyInfoUpdate-username');
	const dupBtn = $('#companyInfoUpdate-dupCheckBtn');

	function markDupUnchecked() {
		usernameDupChecked = false;
		lastCheckedUsername = '';
	}

	if (usernameInput) {
		usernameInput.addEventListener('input', () => {
			if (usernameInput.value.trim() !== lastCheckedUsername) markDupUnchecked();
		});
	}

	if (dupBtn) {
		dupBtn.addEventListener('click', async () => {
			const id = (usernameInput?.value || '').trim();
			if (!id) { alert('아이디를 입력해 주세요.'); return; }
			try {
				const res = await fetch(`/api/customer/username-exists?username=${encodeURIComponent(id)}`);
				const data = await res.json();
				if (data.exists && id !== (INIT.username || '')) {
					alert('이미 사용 중인 아이디입니다.');
					usernameDupChecked = false;
				} else {
					alert('사용 가능한 아이디입니다.');
					usernameDupChecked = true;
					lastCheckedUsername = id;
				}
			} catch (e) {
				alert('중복검사 중 오류가 발생했습니다.');
			}
		});
	}

	// ====== 비밀번호 일치 메시지 ======
	const pw1 = $('#companyInfoUpdate-password');
	const pw2 = $('#companyInfoUpdate-password2');
	const pwMsg = $('#companyInfoUpdate-pwMessage');

	function showPwState() {
		if (!pw1 || !pw2 || !pwMsg) return;
		if (!pw1.value && !pw2.value) {
			pwMsg.textContent = ''; pwMsg.className = 'companyInfoUpdate-msg'; return;
		}
		if (pw1.value === pw2.value) {
			pwMsg.textContent = '비밀번호가 일치합니다.'; pwMsg.className = 'companyInfoUpdate-msg ok';
		} else {
			pwMsg.textContent = '비밀번호가 일치하지 않습니다.'; pwMsg.className = 'companyInfoUpdate-msg err';
		}
	}
	on(pw1, 'input', showPwState);
	on(pw2, 'input', showPwState);

	// ====== 숫자 + 자동 이동 ======
	function numericOnlyAutoNext(inputs) {
		inputs.forEach((el, idx) => {
			el.addEventListener('input', e => {
				e.target.value = e.target.value.replace(/\D/g, '');
				const max = Number(e.target.getAttribute('maxlength') || 0);
				if (max && e.target.value.length >= max) {
					const n = inputs[idx + 1]; if (n) n.focus();
				}
			});
			el.addEventListener('keydown', e => {
				if (e.key === 'Backspace' && el.selectionStart === 0 && el.selectionEnd === 0) {
					const p = inputs[idx - 1]; if (p) p.focus();
				}
			});
		});
	}
	numericOnlyAutoNext($$('.companyInfoUpdate-phone')); // 모바일/전화 파트칸
	numericOnlyAutoNext($$('.companyInfoUpdate-fax'));
	numericOnlyAutoNext($$('.companyInfoUpdate-biznum')); // 3-2-5

	// ====== 이메일 형식 검증 ======
	const email = $('#companyInfoUpdate-email');
	const emailMsg = $('#companyInfoUpdate-emailMsg');
	const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
	function showEmailState() {
		if (!email || !emailMsg) return;
		if (!email.value) { emailMsg.textContent = ''; emailMsg.className = 'companyInfoUpdate-msg'; return; }
		if (emailPattern.test(email.value)) {
			emailMsg.textContent = '올바른 이메일 형식입니다.'; emailMsg.className = 'companyInfoUpdate-msg ok';
		} else {
			emailMsg.textContent = '이메일 형식이 올바르지 않습니다.'; emailMsg.className = 'companyInfoUpdate-msg err';
		}
	}
	on(email, 'input', showEmailState);
	on(email, 'change', showEmailState);

	// ====== 다음 우편번호 (기본/사업장 공용) ======
	function ensureDaum(cb) {
		if (window.daum && window.daum.Postcode) { cb(); return; }
		const s = document.createElement('script');
		s.src = '//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js';
		s.onload = cb; document.head.appendChild(s);
	}

	// (1) 기본주소 버튼
	const baseZipBtn = $('#companyInfoUpdate-addrSearchBtn');
	on(baseZipBtn, 'click', () => ensureDaum(() => {
		/* global daum */
		new daum.Postcode({
			oncomplete: function(data) {
				setIf($('#companyInfoUpdate-zip'), data.zonecode || '');
				setIf($('#companyInfoUpdate-road'), data.roadAddress || '');
				setIf($('#companyInfoUpdate-jibun'), data.jibunAddress || data.autoJibunAddress || '');
				$('#companyInfoUpdate-detail')?.focus();
			}
		}).open();
	}));

	// (2) 사업장 주소 버튼
	const wBtn = document.getElementById('companyInfoUpdate-w-addrSearchBtn');
	on(wBtn, 'click', () => ensureDaum(() => {
		/* global daum */
		new daum.Postcode({
			oncomplete: function(data) {
				setIf($('#companyInfoUpdate-w-zip'), data.zonecode || '');
				setIf($('#companyInfoUpdate-w-road'), data.roadAddress || '');
				setIf($('#companyInfoUpdate-w-jibun'), data.jibunAddress || data.autoJibunAddress || '');
				$('#companyInfoUpdate-w-detail')?.focus();
			}
		}).open();
	}));

	// ====== 사업자등록증 프리뷰 (기존 1개를 리스트처럼 렌더링) ======
	const previewWrap = $('#companyInfoUpdate-filePreview');
	const fileInput = $('#companyInfoUpdate-bizFile');
	let deleteExistingBizReg = false; // 서버로 전송할 플래그

	function clearPreview() { if (previewWrap) previewWrap.innerHTML = ''; }

	function renderExistingBizReg() {
		clearPreview();
		if (!INIT.bizRegPublicUrl) return;
		const item = document.createElement('div');
		item.className = 'preview-item';
		const isImage = (INIT.bizRegContentType || '').startsWith('image/');
		if (isImage) {
			const img = document.createElement('img');
			img.src = INIT.bizRegPublicUrl;
			img.alt = INIT.bizRegOriginalName || '사업자등록증';
			item.appendChild(img);
		} else {
			const box = document.createElement('div');
			box.className = 'file-box';
			box.textContent = (INIT.bizRegOriginalName || '첨부파일');
			item.appendChild(box);
		}
		const x = document.createElement('button');
		x.type = 'button'; x.className = 'companyInfoUpdate-remove remove-btn'; x.textContent = '×';
		x.addEventListener('click', () => {
			// 기존파일 삭제표시
			deleteExistingBizReg = true;
			item.remove();
			setDirty();
		});
		item.appendChild(x);
		previewWrap.appendChild(item);
	}

	function renderNewPreview(file) {
		clearPreview();
		const item = document.createElement('div');
		item.className = 'preview-item';
		if (file.type && file.type.startsWith('image/')) {
			const img = document.createElement('img');
			item.appendChild(img);
			const fr = new FileReader();
			fr.onload = e => img.src = e.target.result;
			fr.readAsDataURL(file);
		} else {
			const box = document.createElement('div');
			box.className = 'file-box';
			box.textContent = file.name;
			item.appendChild(box);
		}
		const x = document.createElement('button');
		x.type = 'button'; x.className = 'companyInfoUpdate-remove remove-btn'; x.textContent = '×';
		x.addEventListener('click', () => {
			fileInput.value = '';
			// 신규 업로드를 취소하면 기존 파일이 있었다면 다시 보여줌(삭제표시가 아니었다면)
			clearPreview();
			if (!deleteExistingBizReg) renderExistingBizReg();
			setDirty();
		});
		item.appendChild(x);
		previewWrap.appendChild(item);
	}

	on(fileInput, 'change', () => {
		const f = fileInput.files && fileInput.files[0];
		if (!f) {
			clearPreview();
			if (!deleteExistingBizReg) renderExistingBizReg();
			return;
		}
		renderNewPreview(f);
		setDirty();
	});

	// ====== 제출 검증 ======
	function isValidKRBizNumber(d) {
		if (!/^\d{10}$/.test(d)) return false;
		const w = [1, 3, 7, 1, 3, 7, 1, 3, 5];
		let sum = 0;
		for (let i = 0; i < 9; i++) sum += (parseInt(d[i], 10) * w[i]);
		sum += Math.floor((parseInt(d[8], 10) * 5) / 10);
		const check = (10 - (sum % 10)) % 10;
		return check === parseInt(d[9], 10);
	}

	function requireFilled(id, msg) {
		const el = $(id);
		if (!el || !el.value.trim()) { alert(msg); el?.focus(); return false; }
		return true;
	}

	on(form, 'submit', (e) => {
		// HTML5 기본 검증
		if (!form.checkValidity()) {
			e.preventDefault(); e.stopPropagation();
			alert('필수 항목을 확인해 주세요.');
			return;
		}
		// 이메일
		if (email && !emailPattern.test(email.value)) {
			e.preventDefault(); e.stopPropagation();
			alert('이메일 형식을 확인해 주세요.');
			email.focus(); return;
		}
		// 휴대폰 3-4-4
		const m1 = $('#companyInfoUpdate-m1')?.value.trim() || '';
		const m2 = $('#companyInfoUpdate-m2')?.value.trim() || '';
		const m3 = $('#companyInfoUpdate-m3')?.value.trim() || '';
		if (!(m1.length === 3 && m2.length === 4 && m3.length === 4)) {
			e.preventDefault(); e.stopPropagation();
			alert('휴대폰 번호를 올바르게 입력해 주세요. (예: 010-1234-5678)');
			$('#companyInfoUpdate-m1')?.focus(); return;
		}
		// 기본주소 필수 (지번 선택)
		if (!requireFilled('#companyInfoUpdate-zip', '주소를 검색해 주세요.')) { e.preventDefault(); return; }
		if (!requireFilled('#companyInfoUpdate-road', '도로명 주소를 입력해 주세요.')) { e.preventDefault(); return; }
		if (!requireFilled('#companyInfoUpdate-detail', '상세 주소를 입력해 주세요.')) { e.preventDefault(); return; }

		// 사업장주소 필수(지번 선택)
		if (!requireFilled('#companyInfoUpdate-w-zip', '사업장 주소를 검색해 주세요.')) { e.preventDefault(); return; }
		if (!requireFilled('#companyInfoUpdate-w-road', '사업장 도로명 주소를 입력해 주세요.')) { e.preventDefault(); return; }
		if (!requireFilled('#companyInfoUpdate-w-detail', '사업장 상세 주소를 입력해 주세요.')) { e.preventDefault(); return; }

		// 비밀번호 규칙
		if ((pw1?.value || '') || (pw2?.value || '')) {
			if (!pw1?.value || !pw2?.value) {
				e.preventDefault(); e.stopPropagation();
				alert('비밀번호와 비밀번호 확인을 모두 입력해 주세요.'); (pw1?.value ? '' : pw1)?.focus(); return;
			}
			if (pw1.value !== pw2.value) {
				e.preventDefault(); e.stopPropagation();
				alert('비밀번호가 일치하지 않습니다.'); pw2.focus(); return;
			}
		}

		// 아이디 변경 시 중복확인 필수
		const cur = (usernameInput?.value || '').trim();
		const changed = (cur !== (INIT.username || ''));
		if (changed && !usernameDupChecked) {
			e.preventDefault(); e.stopPropagation();
			alert('아이디 중복확인을 완료해 주세요.'); dupBtn?.focus(); return;
		}

		// 사업자번호 10자리 가중치
		const bizNo = [
			$('.companyInfoUpdate-biznum[name="bizNo1"]')?.value || '',
			$('.companyInfoUpdate-biznum[name="bizNo2"]')?.value || '',
			$('.companyInfoUpdate-biznum[name="bizNo3"]')?.value || ''
		].map(v => v.replace(/\D/g, '')).join('');
		if (!(bizNo.length === 10 && isValidKRBizNumber(bizNo))) {
			e.preventDefault(); e.stopPropagation();
			alert('사업자등록번호가 올바르지 않습니다.');
			$('.companyInfoUpdate-biznum[name="bizNo1"]')?.focus(); return;
		}

		// 사업자등록증: 기존 삭제표시 + 신규 업로드 없음 => 금지
		const hasNew = !!(fileInput && fileInput.files && fileInput.files.length === 1);
		const hadExisting = !!INIT.bizRegPublicUrl;
		if (deleteExistingBizReg && !hasNew) {
			e.preventDefault(); e.stopPropagation();
			alert('사업자등록증은 최소 1개 이상 등록되어야 합니다.');
			fileInput?.focus(); return;
		}
		if (!hadExisting && !hasNew) {
			e.preventDefault(); e.stopPropagation();
			alert('사업자등록증 파일을 업로드해 주세요.');
			fileInput?.focus(); return;
		}

		// hidden 필드로 서버에 "삭제표시" 전송
		let hidden = $('#__deleteExistingBizReg');
		if (!hidden) {
			hidden = document.createElement('input');
			hidden.type = 'hidden'; hidden.name = 'deleteExistingBizReg'; hidden.id = '__deleteExistingBizReg';
			form.appendChild(hidden);
		}
		hidden.value = deleteExistingBizReg ? 'true' : 'false';

		// hidden 필드: usernameDupChecked
		let h2 = $('#__usernameDupChecked');
		if (!h2) {
			h2 = document.createElement('input');
			h2.type = 'hidden'; h2.name = 'usernameDupChecked'; h2.id = '__usernameDupChecked';
			form.appendChild(h2);
		}
		h2.value = usernameDupChecked ? 'true' : 'false';
	});

	// ====== 초기 렌더링 ======
	function init() {
		// 수정 버튼 초기 비활성
		initDirty();
		onAnyChangeEnable();

		// 기존 사업자등록증 표시
		renderExistingBizReg();

		// ===== 서버 분할값으로 정확 바인딩 =====
		// 휴대폰
		setIf($('#companyInfoUpdate-m1'), INIT.mobile1);
		setIf($('#companyInfoUpdate-m2'), INIT.mobile2);
		setIf($('#companyInfoUpdate-m3'), INIT.mobile3);

		// 대표전화
		setIf($('input[name="bizTel1"]'), INIT.bizTel1);
		setIf($('input[name="bizTel2"]'), INIT.bizTel2);
		setIf($('input[name="bizTel3"]'), INIT.bizTel3);

		// 팩스
		setIf($('input[name="fax1"]'), INIT.fax1);
		setIf($('input[name="fax2"]'), INIT.fax2);
		setIf($('input[name="fax3"]'), INIT.fax3);

		// 사업자등록번호 3-2-5
		setIf($('input[name="bizNo1"]'), INIT.bizNo1);
		setIf($('input[name="bizNo2"]'), INIT.bizNo2);
		setIf($('input[name="bizNo3"]'), INIT.bizNo3);
	}
	init();
})();
