(function() {
	'use strict';

	const form = document.getElementById('seller-insert-form-form');
	if (!form) return;

	const usernameInput = document.getElementById('seller-insert-form-username');
	const usernameFeedback = document.getElementById('seller-insert-form-username-feedback');
	const passwordInput = document.getElementById('seller-insert-form-password');
	const passwordConfirmInput = document.getElementById('seller-insert-form-password-confirm');

	const buyerGradeSelect = document.getElementById('seller-insert-form-buyer-grade');
	const buyerCustomRateWrap = document.getElementById('seller-insert-form-buyer-custom-rate-wrap');
	const buyerCustomRateInput = document.getElementById('seller-insert-form-buyer-custom-rate');

	const largeSelect = document.getElementById('seller-insert-form-large-select');
	const mediumSelect = document.getElementById('seller-insert-form-medium-select');
	const smallSelect = document.getElementById('seller-insert-form-small-select');
	const addPermissionBtn = document.getElementById('seller-insert-form-add-permission-btn');
	const permissionTbody = document.getElementById('seller-insert-form-permission-tbody');
	const permissionEmptyRow = document.getElementById('seller-insert-form-permission-empty-row');
	const categoryPermissionsJsonInput = document.getElementById('seller-insert-form-category-permissions-json');
	const submitBtn = document.getElementById('seller-insert-form-submit-btn');

	const state = {
		usernameChecked: false,
		usernameAvailable: false,
		lastCheckedUsername: '',
		permissions: []
	};

	document.addEventListener('DOMContentLoaded', init);

	async function init() {
		bindUsernameCheck();
		bindPasswordValidation();
		bindPhoneFormatters();
		bindBuyerGradeToggle();
		bindAddressSearchButtons();
		bindCategoryEvents();
		bindSubmit();
		await loadLargeCategories();
		renderPermissions();
		toggleBuyerCustomRate();
	}

	function bindUsernameCheck() {
		usernameInput.addEventListener('input', function() {
			state.usernameChecked = false;
			state.usernameAvailable = false;
			state.lastCheckedUsername = '';
			usernameFeedback.textContent = '';
			usernameFeedback.className = 'seller-insert-form-feedback';
		});

		usernameInput.addEventListener('blur', async function() {
			const username = (usernameInput.value || '').trim();
			usernameInput.value = username;

			if (!username) {
				usernameFeedback.textContent = '아이디를 입력해 주세요.';
				usernameFeedback.className = 'seller-insert-form-feedback invalid';
				state.usernameChecked = false;
				state.usernameAvailable = false;
				state.lastCheckedUsername = '';
				return;
			}

			try {
				const response = await fetch('/api/v1/admin/users/username-exists?username=' + encodeURIComponent(username), {
					method: 'GET',
					credentials: 'same-origin'
				});

				const body = await response.json();

				state.usernameChecked = true;
				state.lastCheckedUsername = username;
				state.usernameAvailable = !body.exists;

				if (body.exists) {
					usernameFeedback.textContent = '이미 사용 중인 아이디입니다.';
					usernameFeedback.className = 'seller-insert-form-feedback invalid';
				} else {
					usernameFeedback.textContent = '사용 가능한 아이디입니다.';
					usernameFeedback.className = 'seller-insert-form-feedback valid';
				}
			} catch (e) {
				state.usernameChecked = false;
				state.usernameAvailable = false;
				state.lastCheckedUsername = '';
				usernameFeedback.textContent = '아이디 중복 확인 중 오류가 발생했습니다.';
				usernameFeedback.className = 'seller-insert-form-feedback invalid';
			}
		});
	}

	function bindPasswordValidation() {
		const validatePasswordMatch = function() {
			if (!passwordConfirmInput.value) {
				passwordConfirmInput.setCustomValidity('');
				return;
			}

			if (passwordInput.value !== passwordConfirmInput.value) {
				passwordConfirmInput.setCustomValidity('비밀번호가 일치하지 않습니다.');
			} else {
				passwordConfirmInput.setCustomValidity('');
			}
		};

		passwordInput.addEventListener('input', validatePasswordMatch);
		passwordConfirmInput.addEventListener('input', validatePasswordMatch);
	}

	function bindPhoneFormatters() {
		const phoneSelectors = [
			'#seller-insert-form-member-tel',
			'#seller-insert-form-member-mobile',
			'#seller-insert-form-company-representative-tel',
			'#seller-insert-form-company-fax',
			'#seller-insert-form-seller-tel',
			'#seller-insert-form-seller-fax'
		];

		phoneSelectors.forEach(selector => {
			const input = document.querySelector(selector);
			if (!input) return;

			input.addEventListener('input', function() {
				input.value = formatPhoneNumber(input.value);
			});
		});

		const bizNoInput = document.getElementById('seller-insert-form-business-registration-number');
		if (bizNoInput) {
			bizNoInput.addEventListener('input', function() {
				bizNoInput.value = formatBusinessNumber(bizNoInput.value);
			});
		}
	}

	function bindBuyerGradeToggle() {
		buyerGradeSelect.addEventListener('change', toggleBuyerCustomRate);
	}

	function toggleBuyerCustomRate() {
		if (buyerGradeSelect.value === 'CUSTOM') {
			buyerCustomRateWrap.classList.remove('seller-insert-form-hidden');
			buyerCustomRateInput.required = true;
		} else {
			buyerCustomRateWrap.classList.add('seller-insert-form-hidden');
			buyerCustomRateInput.required = false;
			buyerCustomRateInput.value = '';
		}
	}

	function bindAddressSearchButtons() {
		document.querySelectorAll('.seller-insert-form-address-search-btn').forEach(button => {
			button.addEventListener('click', function() {
				openDaumPostcode(button.dataset.addressPrefix);
			});
		});
	}

	function openDaumPostcode(prefix) {
		new daum.Postcode({
			oncomplete: function(data) {
				setAddressValue(prefix, 'postcode', data.zonecode || '');
				setAddressValue(prefix, 'road-address', data.roadAddress || '');
				setAddressValue(prefix, 'jibun-address', data.jibunAddress || '');
				focusAddressDetail(prefix);
			}
		}).open();
	}

	function setAddressValue(prefix, fieldName, value) {
		const target = document.getElementById(`seller-insert-form-${prefix}-${fieldName}`);
		if (target) target.value = value;
	}

	function focusAddressDetail(prefix) {
		const target = document.getElementById(`seller-insert-form-${prefix}-detail-address`);
		if (target) target.focus();
	}

	function bindCategoryEvents() {
		largeSelect.addEventListener('change', async function() {
			const largeId = largeSelect.value;
			resetSelect(mediumSelect, '전체(선택 안 함)');
			resetSelect(smallSelect, '전체(선택 안 함)');

			if (!largeId) return;
			await loadMediumCategories(largeId);
		});

		mediumSelect.addEventListener('change', async function() {
			const mediumId = mediumSelect.value;
			resetSelect(smallSelect, '전체(선택 안 함)');

			if (!mediumId) return;
			await loadSmallCategories(mediumId);
		});

		addPermissionBtn.addEventListener('click', handleAddPermission);

		permissionTbody.addEventListener('click', function(e) {
			const removeBtn = e.target.closest('[data-remove-index]');
			if (!removeBtn) return;

			const index = Number(removeBtn.dataset.removeIndex);
			if (Number.isNaN(index)) return;

			state.permissions.splice(index, 1);
			renderPermissions();
		});
	}

	async function loadLargeCategories() {
		const body = await fetchJson('/api/admin/root/clientTransfer/categories/larges');
		const items = unwrapData(body);
		fillOptions(largeSelect, items, '선택');
	}

	async function loadMediumCategories(largeId) {
		const body = await fetchJson('/api/admin/root/clientTransfer/categories/mediums?largeId=' + encodeURIComponent(largeId));
		const items = unwrapData(body);
		fillOptions(mediumSelect, items, '전체(선택 안 함)');
	}

	async function loadSmallCategories(mediumId) {
		const body = await fetchJson('/api/admin/root/clientTransfer/categories/smalls?mediumId=' + encodeURIComponent(mediumId));
		const items = unwrapData(body);
		fillOptions(smallSelect, items, '전체(선택 안 함)');
	}

	function unwrapData(body) {
		if (body && Object.prototype.hasOwnProperty.call(body, 'data')) {
			return body.data || [];
		}
		return Array.isArray(body) ? body : [];
	}

	function fillOptions(select, items, placeholder) {
		select.innerHTML = '';
		const defaultOption = document.createElement('option');
		defaultOption.value = '';
		defaultOption.textContent = placeholder;
		select.appendChild(defaultOption);

		(items || []).forEach(item => {
			const option = document.createElement('option');
			option.value = item.id;
			option.textContent = item.name;
			select.appendChild(option);
		});
	}

	function resetSelect(select, placeholder) {
		select.innerHTML = '';
		const option = document.createElement('option');
		option.value = '';
		option.textContent = placeholder;
		select.appendChild(option);
	}

	async function fetchJson(url) {
		const response = await fetch(url, {
			method: 'GET',
			credentials: 'same-origin'
		});

		if (!response.ok) {
			throw new Error('카테고리 조회 실패');
		}

		return await response.json();
	}

	function handleAddPermission() {
		const largeId = toLong(largeSelect.value);
		const mediumId = toLong(mediumSelect.value);
		const smallId = toLong(smallSelect.value);

		if (!largeId) {
			alert('대분류를 선택해 주세요.');
			return;
		}

		const candidate = {
			largeId: largeId,
			mediumId: mediumId,
			smallId: smallId,
			largeName: getSelectedText(largeSelect),
			mediumName: mediumId ? getSelectedText(mediumSelect) : null,
			smallName: smallId ? getSelectedText(smallSelect) : null
		};

		// 대분류 전체
		if (!candidate.mediumId) {
			state.permissions = state.permissions.filter(p => p.largeId !== candidate.largeId);
			state.permissions.push(candidate);
			renderPermissions();
			return;
		}

		// 중분류 전체
		if (!candidate.smallId) {
			const hasLargeWildcard = state.permissions.some(p => p.largeId === candidate.largeId && !p.mediumId);
			if (hasLargeWildcard) {
				alert('이미 동일 대분류 전체 권한이 등록되어 있습니다.');
				return;
			}

			state.permissions = state.permissions.filter(p => !(p.largeId === candidate.largeId && p.mediumId === candidate.mediumId));
			state.permissions.push(candidate);
			renderPermissions();
			return;
		}

		// 소분류
		const hasLargeWildcard = state.permissions.some(p => p.largeId === candidate.largeId && !p.mediumId);
		if (hasLargeWildcard) {
			alert('이미 동일 대분류 전체 권한이 등록되어 있어 소분류를 추가할 수 없습니다.');
			return;
		}

		const hasMediumWildcard = state.permissions.some(p =>
			p.largeId === candidate.largeId &&
			p.mediumId === candidate.mediumId &&
			!p.smallId
		);

		if (hasMediumWildcard) {
			alert('이미 동일 중분류 전체 권한이 등록되어 있어 소분류를 추가할 수 없습니다.');
			return;
		}

		const exactExists = state.permissions.some(p =>
			p.largeId === candidate.largeId &&
			p.mediumId === candidate.mediumId &&
			p.smallId === candidate.smallId
		);

		if (exactExists) {
			alert('이미 동일한 카테고리 권한이 등록되어 있습니다.');
			return;
		}

		state.permissions.push(candidate);
		renderPermissions();
	}

	function renderPermissions() {
		const list = state.permissions;

		permissionTbody.querySelectorAll('tr[data-row="permission"]').forEach(tr => tr.remove());

		if (!list.length) {
			permissionEmptyRow.style.display = '';
			categoryPermissionsJsonInput.value = '[]';
			return;
		}

		permissionEmptyRow.style.display = 'none';

		list.forEach((item, index) => {
			const tr = document.createElement('tr');
			tr.setAttribute('data-row', 'permission');

			tr.innerHTML = `
				<td class="text-center">${index + 1}</td>
				<td>${buildPermissionPathLabel(item)}</td>
				<td class="text-center">${buildPermissionScopeLabel(item)}</td>
				<td class="text-center">
					<button type="button" class="btn btn-sm btn-outline-danger" data-remove-index="${index}">삭제</button>
				</td>
			`;

			permissionTbody.appendChild(tr);
		});

		categoryPermissionsJsonInput.value = JSON.stringify(
			list.map(item => ({
				largeId: item.largeId,
				mediumId: item.mediumId || null,
				smallId: item.smallId || null
			}))
		);
	}

	function buildPermissionPathLabel(item) {
		if (!item.mediumId) {
			return `${item.largeName}`;
		}
		if (!item.smallId) {
			return `${item.largeName} > ${item.mediumName}`;
		}
		return `${item.largeName} > ${item.mediumName} > ${item.smallName}`;
	}

	function buildPermissionScopeLabel(item) {
		if (!item.mediumId) return '대분류 전체';
		if (!item.smallId) return '중분류 전체';
		return '소분류 단위';
	}

	function bindSubmit() {
		form.addEventListener('submit', async function(e) {
			e.preventDefault();

			if (!validateBeforeSubmit()) {
				return;
			}

			submitBtn.disabled = true;

			try {
				categoryPermissionsJsonInput.value = JSON.stringify(
					state.permissions.map(item => ({
						largeId: item.largeId,
						mediumId: item.mediumId || null,
						smallId: item.smallId || null
					}))
				);

				const formData = new FormData(form);

				const response = await fetch(form.action, {
					method: 'POST',
					body: formData,
					credentials: 'same-origin'
				});

				const body = await response.json().catch(() => ({}));

				if (!response.ok || body.success === false) {
					alert(body.message || '판매회원 등록에 실패했습니다.');
					return;
				}

				alert(body.message || '판매회원이 등록되었습니다.');
				window.location.reload();
			} catch (e2) {
				alert('등록 처리 중 오류가 발생했습니다.');
			} finally {
				submitBtn.disabled = false;
			}
		});
	}

	function validateBeforeSubmit() {
		const username = (usernameInput.value || '').trim();

		if (!username) {
			alert('아이디를 입력해 주세요.');
			usernameInput.focus();
			return false;
		}

		if (!state.usernameChecked || state.lastCheckedUsername !== username || !state.usernameAvailable) {
			alert('아이디 중복 확인을 완료해 주세요.');
			usernameInput.focus();
			return false;
		}

		if (!passwordInput.value) {
			alert('비밀번호를 입력해 주세요.');
			passwordInput.focus();
			return false;
		}

		if (passwordInput.value !== passwordConfirmInput.value) {
			alert('비밀번호와 비밀번호 확인이 일치하지 않습니다.');
			passwordConfirmInput.focus();
			return false;
		}

		if (!state.permissions.length) {
			alert('판매 가능 카테고리를 1개 이상 등록해 주세요.');
			largeSelect.focus();
			return false;
		}

		if (!form.checkValidity()) {
			form.reportValidity();
			return false;
		}

		return true;
	}

	function formatBusinessNumber(value) {
		const digits = onlyDigits(value).slice(0, 10);

		if (digits.length <= 3) return digits;
		if (digits.length <= 5) return digits.replace(/(\d{3})(\d+)/, '$1-$2');
		return digits.replace(/(\d{3})(\d{2})(\d+)/, '$1-$2-$3');
	}

	function formatPhoneNumber(value) {
		const digits = onlyDigits(value).slice(0, 11);

		if (!digits) return '';

		if (digits.startsWith('02')) {
			if (digits.length <= 2) return digits;
			if (digits.length <= 5) return digits.replace(/(\d{2})(\d+)/, '$1-$2');
			if (digits.length <= 9) return digits.replace(/(\d{2})(\d{3})(\d+)/, '$1-$2-$3');
			return digits.replace(/(\d{2})(\d{4})(\d+)/, '$1-$2-$3');
		}

		if (digits.length <= 3) return digits;
		if (digits.length <= 7) return digits.replace(/(\d{3})(\d+)/, '$1-$2');
		if (digits.length <= 10) return digits.replace(/(\d{3})(\d{3})(\d+)/, '$1-$2-$3');
		return digits.replace(/(\d{3})(\d{4})(\d+)/, '$1-$2-$3');
	}

	function onlyDigits(value) {
		return String(value || '').replace(/\D/g, '');
	}

	function getSelectedText(select) {
		const option = select.options[select.selectedIndex];
		return option ? option.textContent.trim() : '';
	}

	function toLong(value) {
		if (!value) return null;
		const num = Number(value);
		return Number.isNaN(num) ? null : num;
	}
})();