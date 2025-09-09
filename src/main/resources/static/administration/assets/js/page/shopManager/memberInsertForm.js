// 직원등록 폼 JS
(() => {
	const $ = (sel) => document.querySelector(sel);

	const form = $("#member-insertForm-form");
	if (!form) return;

	// ====== 변경: loginId -> username
	const usernameInput = $("#member-insertForm-username");
	const idCheckBtn = $("#member-insertForm-idCheckBtn");
	const idCheckResult = $("#member-insertForm-idCheckResult");

	const tel1 = $("#member-insertForm-tel1");
	const tel2 = $("#member-insertForm-tel2");
	const tel3 = $("#member-insertForm-tel3");
	const telHidden = $("#member-insertForm-telHidden");
	const telFormatted = $("#member-insertForm-telFormatted");

	const mobile1 = $("#member-insertForm-mobile1");
	const mobile2 = $("#member-insertForm-mobile2");
	const mobile3 = $("#member-insertForm-mobile3");
	const mobileHidden = $("#member-insertForm-mobileHidden");
	const mobileFormatted = $("#member-insertForm-mobileFormatted");

	const emailId = $("#member-insertForm-emailId");
	const emailDomain = $("#member-insertForm-emailDomain");
	const emailDomainDirect = $("#member-insertForm-emailDomainDirect");
	const emailHidden = $("#member-insertForm-emailHidden");

	const roleSelect = $("#member-insertForm-role");
	const submitBtn = $("#member-insertForm-submitBtn");
	const privacyAgree = $("#member-insertForm-privacyAgree");

	let idChecked = false;

	// 숫자만
	const onlyDigits = (e) => { e.target.value = e.target.value.replace(/\D/g, ""); };

	[tel1, tel2, tel3, mobile1, mobile2, mobile3].forEach(inp => {
		if (!inp) return;
		inp.addEventListener("input", onlyDigits);
		inp.addEventListener("keyup", (e) => {
			const max = parseInt(e.target.getAttribute("maxlength") || "4", 10);
			if (e.target.value.length >= max) {
				if (e.target === tel1) tel2.focus();
				else if (e.target === tel2) tel3.focus();
				else if (e.target === mobile1) mobile2.focus();
				else if (e.target === mobile2) mobile3.focus();
			}
		});
	});

	// 이메일 도메인 선택/직접입력 토글
	emailDomain?.addEventListener("change", () => {
		if (emailDomain.value === "_direct") {
			emailDomainDirect.classList.remove("d-none");
			emailDomainDirect.focus();
		} else {
			emailDomainDirect.classList.add("d-none");
			emailDomainDirect.value = "";
		}
	});

	// ===== 아이디 중복 체크 (새 API 경로, username 파라미터)
	idCheckBtn?.addEventListener("click", async () => {
		const username = (usernameInput?.value || "").trim();
		if (!username) {
			idCheckResult.textContent = "아이디를 입력해 주세요.";
			idCheckResult.className = "form-text text-danger";
			return;
		}
		try {
			const res = await fetch(`/api/v1/admin/users/username-exists?username=${encodeURIComponent(username)}`, {
				method: "GET",
				headers: { "Accept": "application/json" }
			});
			if (!res.ok) throw new Error();
			const data = await res.json(); // { exists: boolean }
			if (data.exists) {
				idChecked = false;
				idCheckResult.textContent = "이미 사용 중인 아이디입니다.";
				idCheckResult.className = "form-text text-danger";
			} else {
				idChecked = true;
				idCheckResult.textContent = "사용 가능한 아이디입니다.";
				idCheckResult.className = "form-text text-success";
			}
		} catch (e) {
			idChecked = false;
			idCheckResult.textContent = "중복 확인 중 오류가 발생했습니다.";
			idCheckResult.className = "form-text text-danger";
		}
	});

	// 아이디 변경 시 중복체크 무효화
	usernameInput?.addEventListener("input", () => {
		idChecked = false;
		idCheckResult.textContent = "아이디 중복여부를 확인해 주세요.";
		idCheckResult.className = "form-text";
	});

	// 제출 시 데이터 조립 + 유효성
	form.addEventListener("submit", (e) => {
		// 권한 필수
		if (!roleSelect?.value) {
			e.preventDefault();
			alert("권한을 선택해 주세요.");
			roleSelect?.focus();
			return;
		}

		// 아이디 필수
		const username = (usernameInput?.value || "").trim();
		if (!username) {
			e.preventDefault();
			alert("아이디를 입력해 주세요.");
			usernameInput?.focus();
			return;
		}

		// 아이디 중복체크 필수
		if (!idChecked) {
			e.preventDefault();
			alert("아이디 중복체크를 해주세요.");
			return;
		}

		// 비밀번호
		const pwd = $("#member-insertForm-password")?.value || "";
		if (pwd.length < 5) {
			e.preventDefault();
			alert("비밀번호는 5자 이상 입력해 주세요.");
			return;
		}

		// 이메일 상호 의존 검증
		const idPart = (emailId?.value || "").trim();
		let domainPart = "";
		if (emailDomain?.value === "_direct") {
			domainPart = (emailDomainDirect?.value || "").trim();
		} else {
			domainPart = (emailDomain?.value || "").trim();
		}
		if ((idPart && !domainPart) || (!idPart && domainPart)) {
			e.preventDefault();
			alert("이메일을 올바르게 입력해 주세요. (아이디와 도메인을 모두 입력)");
			return;
		}
		const email = (idPart && domainPart) ? `${idPart}@${domainPart}` : "";
		emailHidden.value = email;

		// 개인정보 동의
		if (!privacyAgree?.checked) {
			e.preventDefault();
			alert("개인정보 취급 관련 주의사항에 동의해 주세요.");
			return;
		}

		// 전화번호 조립
		const tel = [tel1?.value, tel2?.value, tel3?.value].filter(Boolean).join("-");
		const mobile = [mobile1?.value, mobile2?.value, mobile3?.value].filter(Boolean).join("-");
		telHidden.value = tel || "";
		mobileHidden.value = mobile || "";
		telFormatted.value = tel || "";
		mobileFormatted.value = mobile || "";

		// 중복 제출 방지
		submitBtn.disabled = true;
		setTimeout(() => submitBtn.disabled = false, 3000);
	});
})();
