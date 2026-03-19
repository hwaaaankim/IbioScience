(function() {
	'use strict';

	const memberId = window.CLIENT_DETAIL_HOME_MEMBER_ID;

	const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
	const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

	function headersJson() {
		const h = { 'Content-Type': 'application/json' };
		if (csrfToken && csrfHeader) h[csrfHeader] = csrfToken;
		return h;
	}

	function headersOnlyCsrf() {
		const h = {};
		if (csrfToken && csrfHeader) h[csrfHeader] = csrfToken;
		return h;
	}

	function apiUrl(path) {
		return `/admin/root/api/clientDetail/${memberId}${path}`;
	}

	function showAlert(msg) {
		window.alert(msg);
	}

	function toJsonSafe(res) {
		return res.json().catch(() => null);
	}

	function escapeHtml(str) {
		return String(str)
			.replaceAll('&', '&amp;')
			.replaceAll('<', '&lt;')
			.replaceAll('>', '&gt;')
			.replaceAll('"', '&quot;')
			.replaceAll("'", '&#039;');
	}

	function parseNullableNum(v) {
		if (v === null || v === undefined) return null;
		const s = String(v).trim();
		if (s === '' || s.toLowerCase() === 'null' || s.toLowerCase() === 'undefined') return null;
		const n = Number(s);
		if (!Number.isFinite(n) || n <= 0) return null;
		return n;
	}

	function nvText(id) {
		return document.getElementById(id)?.innerText?.trim() || '';
	}

	function nvValue(id) {
		return document.getElementById(id)?.value?.trim() || '';
	}

	// =========================
	// Dirty flags
	// =========================
	const btnSaveMember = document.getElementById('client-detail-home-btn-save-member');
	const btnSaveCompany = document.getElementById('client-detail-home-btn-save-company');
	const btnSaveBuyer = document.getElementById('client-detail-home-btn-save-buyer');
	const btnSaveSeller = document.getElementById('client-detail-home-btn-save-seller');
	const btnSaveMemos = document.getElementById('client-detail-home-btn-save-memos');

	const dirty = {
		memberAddress: false,
		companyAddress: false,
		buyer: false,
		seller: false,
		memos: false
	};

	function setDirty(key, val) {
		dirty[key] = !!val;
		if (key === 'memberAddress' && btnSaveMember) btnSaveMember.disabled = !dirty.memberAddress;
		if (key === 'companyAddress' && btnSaveCompany) btnSaveCompany.disabled = !dirty.companyAddress;
		if (key === 'buyer' && btnSaveBuyer) btnSaveBuyer.disabled = !dirty.buyer;
		if (key === 'seller' && btnSaveSeller) btnSaveSeller.disabled = !dirty.seller;
		if (key === 'memos' && btnSaveMemos) btnSaveMemos.disabled = !dirty.memos;
	}

	// =========================
	// 1) Password Reset
	// =========================
	const btnResetPw = document.getElementById('client-detail-home-btn-reset-password');
	if (btnResetPw) {
		btnResetPw.addEventListener('click', async () => {
			if (!confirm('비밀번호를 임의의 16자리로 초기화하고, 회원 휴대폰으로 SMS 발송합니다. 진행하시겠습니까?')) return;

			const res = await fetch(apiUrl('/resetPassword'), {
				method: 'POST',
				headers: headersJson()
			});

			const body = await toJsonSafe(res);
			if (!res.ok || !body?.success) {
				showAlert(body?.message || '비밀번호 초기화 실패');
				return;
			}
			showAlert(body.message || '완료');
		});
	}

	// =========================
	// 2) Address Modal
	// =========================
	const addressModalEl = document.getElementById('client-detail-home-address-modal');
	const addressModal = addressModalEl ? new bootstrap.Modal(addressModalEl) : null;

	const addrTarget = document.getElementById('client-detail-home-address-modal-target');
	const addrPostcode = document.getElementById('client-detail-home-address-postcode');
	const addrRoad = document.getElementById('client-detail-home-address-road');
	const addrJibun = document.getElementById('client-detail-home-address-jibun');
	const addrDetail = document.getElementById('client-detail-home-address-detail');

	const btnDaumPost = document.getElementById('client-detail-home-btn-daum-post');
	if (btnDaumPost) {
		btnDaumPost.addEventListener('click', () => {
			new daum.Postcode({
				oncomplete: function(data) {
					addrPostcode.value = data.zonecode || '';
					addrRoad.value = data.roadAddress || '';
					addrJibun.value = data.jibunAddress || '';
					addrDetail.focus();
				}
			}).open();
		});
	}

	function openAddressModal(target, current) {
		addrTarget.value = target;
		addrPostcode.value = current.postcode || '';
		addrRoad.value = current.roadAddress || '';
		addrJibun.value = current.jibunAddress || '';
		addrDetail.value = current.detailAddress || '';
		addressModal.show();
	}

	const btnEditMemberAddr = document.getElementById('client-detail-home-btn-edit-member-address');
	if (btnEditMemberAddr) {
		btnEditMemberAddr.addEventListener('click', () => {
			openAddressModal('MEMBER', {
				postcode: nvText('client-detail-home-member-postcode'),
				roadAddress: nvText('client-detail-home-member-road'),
				jibunAddress: nvText('client-detail-home-member-jibun'),
				detailAddress: nvText('client-detail-home-member-detail')
			});
		});
	}

	const btnEditCompanyAddr = document.getElementById('client-detail-home-btn-edit-company-address');
	if (btnEditCompanyAddr) {
		btnEditCompanyAddr.addEventListener('click', () => {
			openAddressModal('COMPANY', {
				postcode: nvText('client-detail-home-company-postcode'),
				roadAddress: nvText('client-detail-home-company-road'),
				jibunAddress: nvText('client-detail-home-company-jibun'),
				detailAddress: nvText('client-detail-home-company-detail')
			});
		});
	}

	const btnEditSellerBizAddr = document.getElementById('client-detail-home-btn-edit-seller-biz-address');
	if (btnEditSellerBizAddr) {
		btnEditSellerBizAddr.addEventListener('click', () => {
			openAddressModal('SELLER_BIZ', {
				postcode: nvText('client-detail-home-seller-biz-postcode'),
				roadAddress: nvText('client-detail-home-seller-biz-road'),
				jibunAddress: nvText('client-detail-home-seller-biz-jibun'),
				detailAddress: nvText('client-detail-home-seller-biz-detail')
			});
		});
	}

	const btnEditSellerReturnAddr = document.getElementById('client-detail-home-btn-edit-seller-return-address');
	if (btnEditSellerReturnAddr) {
		btnEditSellerReturnAddr.addEventListener('click', () => {
			openAddressModal('SELLER_RETURN', {
				postcode: nvText('client-detail-home-seller-return-postcode'),
				roadAddress: nvText('client-detail-home-seller-return-road'),
				jibunAddress: nvText('client-detail-home-seller-return-jibun'),
				detailAddress: nvText('client-detail-home-seller-return-detail')
			});
		});
	}

	const btnApplyAddress = document.getElementById('client-detail-home-btn-apply-address');
	if (btnApplyAddress) {
		btnApplyAddress.addEventListener('click', () => {
			const target = addrTarget.value;
			const p = addrPostcode.value.trim();
			const r = addrRoad.value.trim();
			const j = addrJibun.value.trim();
			const d = addrDetail.value.trim();

			if (target === 'MEMBER') {
				document.getElementById('client-detail-home-member-postcode').innerText = p;
				document.getElementById('client-detail-home-member-road').innerText = r;
				document.getElementById('client-detail-home-member-jibun').innerText = j;
				document.getElementById('client-detail-home-member-detail').innerText = d;
				setDirty('memberAddress', true);
			}

			if (target === 'COMPANY') {
				document.getElementById('client-detail-home-company-postcode').innerText = p;
				document.getElementById('client-detail-home-company-road').innerText = r;
				document.getElementById('client-detail-home-company-jibun').innerText = j;
				document.getElementById('client-detail-home-company-detail').innerText = d;
				setDirty('companyAddress', true);
			}

			if (target === 'SELLER_BIZ') {
				document.getElementById('client-detail-home-seller-biz-postcode').innerText = p;
				document.getElementById('client-detail-home-seller-biz-road').innerText = r;
				document.getElementById('client-detail-home-seller-biz-jibun').innerText = j;
				document.getElementById('client-detail-home-seller-biz-detail').innerText = d;
				refreshSellerDirty();
			}

			if (target === 'SELLER_RETURN') {
				document.getElementById('client-detail-home-seller-return-postcode').innerText = p;
				document.getElementById('client-detail-home-seller-return-road').innerText = r;
				document.getElementById('client-detail-home-seller-return-jibun').innerText = j;
				document.getElementById('client-detail-home-seller-return-detail').innerText = d;
				refreshSellerDirty();
			}

			addressModal.hide();
		});
	}

	// =========================
	// 3) Memo add/delete
	// =========================
	const memoTop5List = document.getElementById('client-detail-home-memo-top5-list');
	const memoNewContent = document.getElementById('client-detail-home-memo-new-content');
	const btnAddMemo = document.getElementById('client-detail-home-btn-add-memo');

	const pendingMemoAdds = [];
	const pendingMemoDeletes = new Set();

	function renderPendingNewMemo(content) {
		const li = document.createElement('li');
		li.className = 'list-group-item d-flex justify-content-between align-items-start';
		li.setAttribute('data-memo-new', '1');

		const left = document.createElement('div');
		left.className = 'me-3';
		left.innerHTML = `
			<div class="fw-semibold text-primary">신규 메모</div>
			<div class="text-muted small">저장 대기</div>
			<div class="mt-2"></div>
		`;
		left.querySelector('.mt-2').innerText = content;

		const btn = document.createElement('button');
		btn.type = 'button';
		btn.className = 'btn btn-outline-danger btn-sm';
		btn.innerText = '취소';
		btn.addEventListener('click', () => {
			const idx = pendingMemoAdds.indexOf(content);
			if (idx >= 0) pendingMemoAdds.splice(idx, 1);
			li.remove();
			setDirty('memos', pendingMemoAdds.length > 0 || pendingMemoDeletes.size > 0);
		});

		li.appendChild(left);
		li.appendChild(btn);
		memoTop5List.insertBefore(li, memoTop5List.firstChild);
	}

	if (btnAddMemo) {
		btnAddMemo.addEventListener('click', () => {
			const c = (memoNewContent.value || '').trim();
			if (!c) {
				showAlert('메모 내용을 입력해 주세요.');
				return;
			}
			pendingMemoAdds.push(c);
			memoNewContent.value = '';
			renderPendingNewMemo(c);
			setDirty('memos', true);
		});
	}

	if (memoTop5List) {
		memoTop5List.addEventListener('click', (e) => {
			const btn = e.target.closest('.client-detail-home-btn-delete-memo');
			if (!btn) return;

			const li = btn.closest('li[data-memo-id]');
			if (!li) return;

			const memoId = li.getAttribute('data-memo-id');
			if (!memoId) return;

			if (!confirm('해당 메모를 삭제 처리(저장 대기) 하시겠습니까?')) return;

			pendingMemoDeletes.add(Number(memoId));
			li.style.opacity = '0.5';
			btn.disabled = true;
			btn.innerText = '삭제대기';
			setDirty('memos', true);
		});
	}

	if (btnSaveMemos) {
		btnSaveMemos.addEventListener('click', async () => {
			if (!confirm('메모 변경사항을 저장하시겠습니까?')) return;

			const payload = {
				addContents: pendingMemoAdds.slice(),
				deleteIds: Array.from(pendingMemoDeletes)
			};

			const res = await fetch(apiUrl('/memos/save'), {
				method: 'POST',
				headers: headersJson(),
				body: JSON.stringify(payload)
			});

			const body = await toJsonSafe(res);
			if (!res.ok || !body?.success) {
				showAlert(body?.message || '메모 저장 실패');
				return;
			}

			showAlert(body.message || '저장 완료');
			window.location.reload();
		});
	}

	// =========================
	// 4) Memo modal
	// =========================
	const memoModalEl = document.getElementById('client-detail-home-memo-modal');
	const memoModal = memoModalEl ? new bootstrap.Modal(memoModalEl) : null;

	const btnOpenMemoModal = document.getElementById('client-detail-home-btn-open-memo-modal');
	const memoTbody = document.getElementById('client-detail-home-memo-modal-tbody');
	const memoPagination = document.getElementById('client-detail-home-memo-pagination');

	const filterFrom = document.getElementById('client-detail-home-memo-filter-from');
	const filterTo = document.getElementById('client-detail-home-memo-filter-to');
	const btnFilterSearch = document.getElementById('client-detail-home-btn-memo-filter-search');
	const btnFilterReset = document.getElementById('client-detail-home-btn-memo-filter-reset');

	let memoPageState = { page: 0, size: 10, from: '', to: '' };

	async function loadMemoPage(page) {
		memoPageState.page = page;

		const params = new URLSearchParams();
		params.set('page', String(memoPageState.page));
		params.set('size', String(memoPageState.size));
		if (memoPageState.from) params.set('from', memoPageState.from);
		if (memoPageState.to) params.set('to', memoPageState.to);

		const res = await fetch(apiUrl('/memos?' + params.toString()), { headers: headersJson() });
		const body = await toJsonSafe(res);

		if (!res.ok || !body?.success) {
			memoTbody.innerHTML = `<tr><td colspan="4" class="text-center text-danger">${body?.message || '조회 실패'}</td></tr>`;
			memoPagination.innerHTML = '';
			return;
		}

		const data = body.data;

		if (!data.content || data.content.length === 0) {
			memoTbody.innerHTML = `<tr><td colspan="4" class="text-center text-muted">데이터가 없습니다.</td></tr>`;
		} else {
			memoTbody.innerHTML = data.content.map(x => {
				const dt = x.createdAt ? String(x.createdAt).replace('T', ' ') : '';
				return `
					<tr>
						<td>${x.id}</td>
						<td>${escapeHtml(x.writerName || '')}</td>
						<td>${escapeHtml(dt)}</td>
						<td style="white-space: pre-wrap;">${escapeHtml(x.content || '')}</td>
					</tr>
				`;
			}).join('');
		}

		renderPagination(data.page, data.totalPages);
	}

	function renderPagination(page, totalPages) {
		memoPagination.innerHTML = '';
		if (totalPages <= 0) return;

		const blockSize = 5;
		const blockStart = Math.floor(page / blockSize) * blockSize;
		const blockEnd = Math.min(blockStart + blockSize - 1, totalPages - 1);

		memoPagination.appendChild(pageItem('FIRST', 0, page === 0));
		memoPagination.appendChild(pageItem('PREV', Math.max(page - 1, 0), page === 0));

		for (let p = blockStart; p <= blockEnd; p++) {
			memoPagination.appendChild(pageItem(String(p + 1), p, false, p === page));
		}

		memoPagination.appendChild(pageItem('NEXT', Math.min(page + 1, totalPages - 1), page >= totalPages - 1));
		memoPagination.appendChild(pageItem('LAST', totalPages - 1, page >= totalPages - 1));
	}

	function pageItem(text, targetPage, disabled, active) {
		const li = document.createElement('li');
		li.className = 'page-item' + (disabled ? ' disabled' : '') + (active ? ' active' : '');
		const a = document.createElement('a');
		a.className = 'page-link';
		a.href = 'javascript:void(0);';
		a.innerText = text;
		a.addEventListener('click', () => {
			if (disabled) return;
			loadMemoPage(targetPage);
		});
		li.appendChild(a);
		return li;
	}

	if (btnOpenMemoModal) {
		btnOpenMemoModal.addEventListener('click', async () => {
			memoPageState = { page: 0, size: 10, from: '', to: '' };
			filterFrom.value = '';
			filterTo.value = '';
			memoModal.show();
			await loadMemoPage(0);
		});
	}

	if (btnFilterSearch) {
		btnFilterSearch.addEventListener('click', async () => {
			memoPageState.from = (filterFrom.value || '').trim();
			memoPageState.to = (filterTo.value || '').trim();
			await loadMemoPage(0);
		});
	}

	if (btnFilterReset) {
		btnFilterReset.addEventListener('click', async () => {
			filterFrom.value = '';
			filterTo.value = '';
			memoPageState.from = '';
			memoPageState.to = '';
			await loadMemoPage(0);
		});
	}

	// =========================
	// 5) Save Member Address
	// =========================
	if (btnSaveMember) {
		btnSaveMember.addEventListener('click', async () => {
			if (!dirty.memberAddress) return;

			const payload = {
				postcode: nvText('client-detail-home-member-postcode'),
				roadAddress: nvText('client-detail-home-member-road'),
				jibunAddress: nvText('client-detail-home-member-jibun'),
				detailAddress: nvText('client-detail-home-member-detail')
			};

			const res = await fetch(apiUrl('/member/address'), {
				method: 'POST',
				headers: headersJson(),
				body: JSON.stringify(payload)
			});

			const body = await toJsonSafe(res);
			if (!res.ok || !body?.success) {
				showAlert(body?.message || '회원 주소 저장 실패');
				return;
			}
			showAlert(body.message || '저장 완료');
			setDirty('memberAddress', false);
		});
	}

	// =========================
	// 6) Save Company Address
	// =========================
	if (btnSaveCompany) {
		btnSaveCompany.addEventListener('click', async () => {
			if (!dirty.companyAddress) return;

			const payload = {
				postcode: nvText('client-detail-home-company-postcode'),
				roadAddress: nvText('client-detail-home-company-road'),
				jibunAddress: nvText('client-detail-home-company-jibun'),
				detailAddress: nvText('client-detail-home-company-detail')
			};

			const res = await fetch(apiUrl('/company/address'), {
				method: 'POST',
				headers: headersJson(),
				body: JSON.stringify(payload)
			});

			const body = await toJsonSafe(res);
			if (!res.ok || !body?.success) {
				showAlert(body?.message || '회사 주소 저장 실패');
				return;
			}
			showAlert(body.message || '저장 완료');
			setDirty('companyAddress', false);
		});
	}

	// =========================
	// 7) Buyer grade
	// =========================
	const buyerGradeSelect = document.getElementById('client-detail-home-buyer-grade');
	if (buyerGradeSelect) {
		buyerGradeSelect.addEventListener('change', () => {
			setDirty('buyer', true);
		});
	}

	if (btnSaveBuyer) {
		btnSaveBuyer.addEventListener('click', async () => {
			if (!dirty.buyer) return;

			const payload = { grade: buyerGradeSelect.value };

			const res = await fetch(apiUrl('/buyer/grade'), {
				method: 'POST',
				headers: headersJson(),
				body: JSON.stringify(payload)
			});

			const body = await toJsonSafe(res);
			if (!res.ok || !body?.success) {
				showAlert(body?.message || '바이어 등급 저장 실패');
				return;
			}
			showAlert(body.message || '저장 완료');
			setDirty('buyer', false);
		});
	}

	// =========================
	// 8) Seller - logo / contacts / dirty detection
	// =========================
	const sellerLogoPreview = document.getElementById('client-detail-home-seller-logo-preview');
	const sellerLogoEmpty = document.getElementById('client-detail-home-seller-logo-empty');
	const sellerLogoFile = document.getElementById('client-detail-home-seller-logo-file');
	const sellerLogoAction = document.getElementById('client-detail-home-seller-logo-action');
	const sellerLogoOriginalRoad = document.getElementById('client-detail-home-seller-logo-original-road');
	const btnDeleteSellerLogo = document.getElementById('client-detail-home-btn-delete-seller-logo');
	const btnKeepSellerLogo = document.getElementById('client-detail-home-btn-keep-seller-logo');

	const sellerContactTbody = document.getElementById('client-detail-home-seller-contact-tbody');
	const btnAddSellerContact = document.getElementById('client-detail-home-btn-add-seller-contact');

	let tempLogoObjectUrl = null;
	let initialSellerSnapshot = null;
	let initialPermState = [];

	function revokeTempLogoObjectUrl() {
		if (tempLogoObjectUrl) {
			URL.revokeObjectURL(tempLogoObjectUrl);
			tempLogoObjectUrl = null;
		}
	}

	function setSellerLogoPreview(src) {
		if (sellerLogoPreview) {
			if (src) {
				sellerLogoPreview.src = src;
				sellerLogoPreview.classList.remove('d-none');
			} else {
				sellerLogoPreview.removeAttribute('src');
				sellerLogoPreview.classList.add('d-none');
			}
		}
		if (sellerLogoEmpty) {
			if (src) sellerLogoEmpty.classList.add('d-none');
			else sellerLogoEmpty.classList.remove('d-none');
		}
	}

	function getOriginalSellerLogoRoad() {
		return sellerLogoOriginalRoad?.value?.trim() || '';
	}

	function resetSellerLogoToOriginal() {
		revokeTempLogoObjectUrl();
		if (sellerLogoFile) sellerLogoFile.value = '';
		if (sellerLogoAction) sellerLogoAction.value = 'KEEP';

		const original = getOriginalSellerLogoRoad();
		setSellerLogoPreview(original || '');
		refreshSellerDirty();
	}

	if (sellerLogoFile) {
		sellerLogoFile.addEventListener('change', () => {
			revokeTempLogoObjectUrl();

			const file = sellerLogoFile.files && sellerLogoFile.files[0] ? sellerLogoFile.files[0] : null;
			if (!file) {
				resetSellerLogoToOriginal();
				return;
			}

			if (!file.type || !file.type.startsWith('image/')) {
				showAlert('로고는 이미지 파일만 선택할 수 있습니다.');
				sellerLogoFile.value = '';
				resetSellerLogoToOriginal();
				return;
			}

			tempLogoObjectUrl = URL.createObjectURL(file);
			setSellerLogoPreview(tempLogoObjectUrl);
			if (sellerLogoAction) sellerLogoAction.value = 'REPLACE';
			refreshSellerDirty();
		});
	}

	if (btnDeleteSellerLogo) {
		btnDeleteSellerLogo.addEventListener('click', () => {
			revokeTempLogoObjectUrl();
			if (sellerLogoFile) sellerLogoFile.value = '';

			const hasOriginal = !!getOriginalSellerLogoRoad();
			if (!hasOriginal) {
				setSellerLogoPreview('');
				if (sellerLogoAction) sellerLogoAction.value = 'KEEP';
				refreshSellerDirty();
				return;
			}

			setSellerLogoPreview('');
			if (sellerLogoAction) sellerLogoAction.value = 'DELETE';
			refreshSellerDirty();
		});
	}

	if (btnKeepSellerLogo) {
		btnKeepSellerLogo.addEventListener('click', () => {
			resetSellerLogoToOriginal();
		});
	}

	function removeEmptyContactRow() {
		const emptyRow = sellerContactTbody?.querySelector('.client-detail-home-seller-contact-empty-row');
		if (emptyRow) emptyRow.remove();
	}

	function ensureEmptyContactRow() {
		if (!sellerContactTbody) return;

		const rows = sellerContactTbody.querySelectorAll('tr[data-contact-row="1"]');
		const emptyRow = sellerContactTbody.querySelector('.client-detail-home-seller-contact-empty-row');

		if (rows.length === 0 && !emptyRow) {
			const tr = document.createElement('tr');
			tr.className = 'client-detail-home-seller-contact-empty-row';
			tr.innerHTML = `<td colspan="5" class="text-muted text-center">등록된 담당자가 없습니다.</td>`;
			sellerContactTbody.appendChild(tr);
		}

		if (rows.length > 0 && emptyRow) {
			emptyRow.remove();
		}
	}

	function createSellerContactRow(contact) {
		const tr = document.createElement('tr');
		tr.setAttribute('data-contact-row', '1');
		tr.setAttribute('data-contact-id', contact && contact.id ? String(contact.id) : '');

		const idText = contact && contact.id ? String(contact.id) : 'NEW';

		tr.innerHTML = `
			<td class="text-center">${escapeHtml(idText)}</td>
			<td>
				<input type="text" class="form-control form-control-sm client-detail-home-seller-contact-name"
					value="${escapeHtml(contact?.name || '')}">
			</td>
			<td>
				<input type="text" class="form-control form-control-sm client-detail-home-seller-contact-phone"
					value="${escapeHtml(contact?.phone || '')}">
			</td>
			<td>
				<input type="text" class="form-control form-control-sm client-detail-home-seller-contact-email"
					value="${escapeHtml(contact?.email || '')}">
			</td>
			<td class="text-center">
				<button type="button" class="btn btn-outline-danger btn-sm client-detail-home-btn-delete-seller-contact">X</button>
			</td>
		`;

		return tr;
	}

	if (btnAddSellerContact && sellerContactTbody) {
		btnAddSellerContact.addEventListener('click', () => {
			removeEmptyContactRow();
			sellerContactTbody.appendChild(createSellerContactRow({
				id: null,
				name: '',
				phone: '',
				email: ''
			}));
			refreshSellerDirty();
		});
	}

	if (sellerContactTbody) {
		sellerContactTbody.addEventListener('click', (e) => {
			const btn = e.target.closest('.client-detail-home-btn-delete-seller-contact');
			if (!btn) return;

			const row = btn.closest('tr[data-contact-row="1"]');
			if (!row) return;

			row.remove();
			ensureEmptyContactRow();
			refreshSellerDirty();
		});

		sellerContactTbody.addEventListener('input', (e) => {
			if (
				e.target.classList.contains('client-detail-home-seller-contact-name') ||
				e.target.classList.contains('client-detail-home-seller-contact-phone') ||
				e.target.classList.contains('client-detail-home-seller-contact-email')
			) {
				refreshSellerDirty();
			}
		});

		sellerContactTbody.addEventListener('change', (e) => {
			if (
				e.target.classList.contains('client-detail-home-seller-contact-name') ||
				e.target.classList.contains('client-detail-home-seller-contact-phone') ||
				e.target.classList.contains('client-detail-home-seller-contact-email')
			) {
				refreshSellerDirty();
			}
		});
	}

	function collectSellerContactsForCompare() {
		if (!sellerContactTbody) return [];
		return Array.from(sellerContactTbody.querySelectorAll('tr[data-contact-row="1"]')).map(row => ({
			id: parseNullableNum(row.getAttribute('data-contact-id')),
			name: row.querySelector('.client-detail-home-seller-contact-name')?.value?.trim() || '',
			phone: row.querySelector('.client-detail-home-seller-contact-phone')?.value?.trim() || '',
			email: row.querySelector('.client-detail-home-seller-contact-email')?.value?.trim() || ''
		}));
	}

	function collectSellerContactsForSave() {
		if (!sellerContactTbody) return [];

		const rows = Array.from(sellerContactTbody.querySelectorAll('tr[data-contact-row="1"]'));
		const contacts = [];

		for (const row of rows) {
			const id = parseNullableNum(row.getAttribute('data-contact-id'));
			const name = row.querySelector('.client-detail-home-seller-contact-name')?.value?.trim() || '';
			const phone = row.querySelector('.client-detail-home-seller-contact-phone')?.value?.trim() || '';
			const email = row.querySelector('.client-detail-home-seller-contact-email')?.value?.trim() || '';

			const hasAny = !!(name || phone || email);
			if (!hasAny) {
				throw new Error('담당자 빈 행이 있습니다. 빈 행은 삭제하거나 내용을 입력해 주세요.');
			}
			if (!name) {
				throw new Error('담당자명은 필수입니다.');
			}

			contacts.push({
				id: id,
				name: name,
				phone: phone,
				email: email
			});
		}

		return contacts;
	}

	function normalizeObject(obj) {
		return JSON.stringify(obj);
	}

	function captureCurrentSellerSnapshot() {
		if (!btnSaveSeller) return null;

		return {
			shopName: nvValue('client-detail-home-seller-shopName'),
			supplierCode: nvValue('client-detail-home-seller-supplierCode'),
			homepageUrl: nvValue('client-detail-home-seller-homepageUrl'),
			productTypeText: nvValue('client-detail-home-seller-productTypeText'),
			tel: nvValue('client-detail-home-seller-tel'),
			fax: nvValue('client-detail-home-seller-fax'),
			tradingStatus: nvValue('client-detail-home-seller-tradingStatus'),
			supplyType: nvValue('client-detail-home-seller-supplyType'),
			supplyStructure: nvValue('client-detail-home-seller-supplyStructure'),
			dealStartDate: nvValue('client-detail-home-seller-dealStartDate'),
			dealStopDate: nvValue('client-detail-home-seller-dealStopDate'),
			bizPostcode: nvText('client-detail-home-seller-biz-postcode'),
			bizRoad: nvText('client-detail-home-seller-biz-road'),
			bizJibun: nvText('client-detail-home-seller-biz-jibun'),
			bizDetail: nvText('client-detail-home-seller-biz-detail'),
			returnPostcode: nvText('client-detail-home-seller-return-postcode'),
			returnRoad: nvText('client-detail-home-seller-return-road'),
			returnJibun: nvText('client-detail-home-seller-return-jibun'),
			returnDetail: nvText('client-detail-home-seller-return-detail'),
			commissionRate: nvValue('client-detail-home-settle-commissionRate'),
			cycle: nvValue('client-detail-home-settle-cycle'),
			basis: nvValue('client-detail-home-settle-basis'),
			nextSettlementDate: nvValue('client-detail-home-settle-nextDate'),
			logoAction: sellerLogoAction?.value || 'KEEP',
			logoFileName: sellerLogoFile?.files?.[0]?.name || '',
			contacts: collectSellerContactsForCompare(),
			permissions: normalizePermList((permState || []).map(x => ({
				largeId: x.largeId,
				mediumId: x.mediumId || null,
				smallId: x.smallId || null
			})))
		};
	}

	function refreshSellerDirty() {
		if (!btnSaveSeller) return;
		if (!initialSellerSnapshot) return;

		const current = captureCurrentSellerSnapshot();
		setDirty('seller', normalizeObject(current) !== normalizeObject(initialSellerSnapshot));
	}

	const sellerInputs = [
		'client-detail-home-seller-shopName',
		'client-detail-home-seller-supplierCode',
		'client-detail-home-seller-homepageUrl',
		'client-detail-home-seller-productTypeText',
		'client-detail-home-seller-tel',
		'client-detail-home-seller-fax',
		'client-detail-home-seller-tradingStatus',
		'client-detail-home-seller-supplyType',
		'client-detail-home-seller-supplyStructure',
		'client-detail-home-seller-dealStartDate',
		'client-detail-home-seller-dealStopDate',
		'client-detail-home-settle-commissionRate',
		'client-detail-home-settle-cycle',
		'client-detail-home-settle-basis',
		'client-detail-home-settle-nextDate'
	];

	sellerInputs.forEach(id => {
		const el = document.getElementById(id);
		if (!el) return;
		el.addEventListener('change', refreshSellerDirty);
		el.addEventListener('input', refreshSellerDirty);
	});

	// =========================
	// 9) Category permission add/delete
	// =========================
	const permList = document.getElementById('client-detail-home-permission-list');

	const selLarge = document.getElementById('client-detail-home-cat-large');
	const selMedium = document.getElementById('client-detail-home-cat-medium');
	const selSmall = document.getElementById('client-detail-home-cat-small');
	const btnAddPerm = document.getElementById('client-detail-home-btn-add-permission');

	const existingPermIds = [];
	let permState = [];

	function keyOf(p) {
		const m = p.mediumId ? String(p.mediumId) : '';
		const s = p.smallId ? String(p.smallId) : '';
		return `${p.largeId}:${m}:${s}`;
	}

	function normalizePermList(list) {
		const largeIds = new Set();
		const largeAllSet = new Set();
		const mediumAllSet = new Map();
		const smallSet = new Map();

		(list || []).forEach(p => {
			if (!p || !p.largeId) return;

			const largeId = Number(p.largeId);
			const mediumId = p.mediumId ? Number(p.mediumId) : null;
			const smallId = p.smallId ? Number(p.smallId) : null;

			largeIds.add(largeId);

			if (smallId && !mediumId) return;

			if (!mediumId && !smallId) {
				largeAllSet.add(largeId);
				return;
			}

			if (mediumId && !smallId) {
				if (!mediumAllSet.has(largeId)) mediumAllSet.set(largeId, new Set());
				mediumAllSet.get(largeId).add(mediumId);
				return;
			}

			if (mediumId && smallId) {
				if (!smallSet.has(largeId)) smallSet.set(largeId, new Map());
				const mm = smallSet.get(largeId);
				if (!mm.has(mediumId)) mm.set(mediumId, new Set());
				mm.get(mediumId).add(smallId);
			}
		});

		const out = [];
		const sortedLarge = Array.from(largeIds).sort((a, b) => a - b);

		sortedLarge.forEach(largeId => {
			if (largeAllSet.has(largeId)) {
				out.push({ largeId, mediumId: null, smallId: null });
				return;
			}

			const mAll = mediumAllSet.get(largeId) ? Array.from(mediumAllSet.get(largeId)).sort((a, b) => a - b) : [];
			mAll.forEach(mid => {
				out.push({ largeId, mediumId: mid, smallId: null });
			});

			const smallMap = smallSet.get(largeId);
			if (!smallMap) return;

			const sortedMedium = Array.from(smallMap.keys()).sort((a, b) => a - b);
			sortedMedium.forEach(mid => {
				if (mediumAllSet.get(largeId)?.has(mid)) return;
				const sIds = Array.from(smallMap.get(mid)).sort((a, b) => a - b);
				sIds.forEach(sid => {
					out.push({ largeId, mediumId: mid, smallId: sid });
				});
			});
		});

		const seen = new Set();
		const dedup = [];
		out.forEach(p => {
			const k = keyOf(p);
			if (seen.has(k)) return;
			seen.add(k);
			dedup.push(p);
		});

		return dedup;
	}

	function existsLargeAll(largeId) {
		return (permState || []).some(p => p.largeId === largeId && !p.mediumId && !p.smallId);
	}

	function existsMediumAll(largeId, mediumId) {
		return (permState || []).some(p => p.largeId === largeId && p.mediumId === mediumId && !p.smallId);
	}

	function removeByLarge(largeId) {
		permState = (permState || []).filter(p => p.largeId !== largeId);
	}

	function removeByMedium(largeId, mediumId) {
		permState = (permState || []).filter(p => !(p.largeId === largeId && p.mediumId === mediumId));
	}

	function renderPermissionList() {
		if (!permList) return;

		permList.innerHTML = '';

		if (!permState || permState.length === 0) {
			const li = document.createElement('li');
			li.className = 'list-group-item text-muted';
			li.innerText = '등록된 카테고리 권한이 없습니다.';
			permList.appendChild(li);
			return;
		}

		permState.forEach(p => {
			const li = document.createElement('li');
			li.className = 'list-group-item d-flex justify-content-between align-items-center';
			li.setAttribute('data-large-id', String(p.largeId));
			li.setAttribute('data-medium-id', p.mediumId ? String(p.mediumId) : '');
			li.setAttribute('data-small-id', p.smallId ? String(p.smallId) : '');
			li.setAttribute('data-perm-key', keyOf(p));

			const mediumText = p.mediumId ? `ID ${p.mediumId}` : '-';
			const smallText = p.smallId ? `ID ${p.smallId}` : '-';

			li.innerHTML = `
				<div class="client-detail-home-perm-text">
					<span class="text-muted">대:</span> <span class="client-detail-home-perm-large">ID ${p.largeId}</span>
					<span class="ms-2 text-muted">중:</span> <span class="client-detail-home-perm-medium">${mediumText}</span>
					<span class="ms-2 text-muted">소:</span> <span class="client-detail-home-perm-small">${smallText}</span>
				</div>
				<button type="button" class="btn btn-outline-danger btn-sm client-detail-home-btn-delete-permission">X</button>
			`;

			permList.appendChild(li);
		});
	}

	function initPermissionStateFromDom() {
		if (!permList) return;

		const lis = permList.querySelectorAll('li[data-perm-id]');
		lis.forEach(li => {
			const pid = parseNullableNum(li.getAttribute('data-perm-id'));
			if (pid) existingPermIds.push(pid);

			const largeId = parseNullableNum(li.getAttribute('data-large-id'));
			const mediumId = parseNullableNum(li.getAttribute('data-medium-id'));
			const smallId = parseNullableNum(li.getAttribute('data-small-id'));
			if (!largeId) return;
			if (smallId && !mediumId) return;

			permState.push({
				largeId: Number(largeId),
				mediumId: mediumId ? Number(mediumId) : null,
				smallId: smallId ? Number(smallId) : null
			});
		});

		permState = normalizePermList(permState);
		initialPermState = normalizePermList((permState || []).map(x => ({
			largeId: x.largeId,
			mediumId: x.mediumId || null,
			smallId: x.smallId || null
		})));
		renderPermissionList();
	}

	async function loadLarges() {
		const res = await fetch('/api/admin/root/clientTransfer/categories/larges', { headers: headersJson() });
		const body = await toJsonSafe(res);
		const arr = body?.data || [];
		selLarge.innerHTML = `<option value="">선택</option>` + arr.map(x => `<option value="${x.id}">${escapeHtml(x.name)}</option>`).join('');
	}

	async function loadMediums(largeId) {
		if (!largeId) {
			selMedium.innerHTML = `<option value="">선택</option>`;
			selSmall.innerHTML = `<option value="">선택</option>`;
			return;
		}
		const res = await fetch(`/api/admin/root/clientTransfer/categories/mediums?largeId=${largeId}`, { headers: headersJson() });
		const body = await toJsonSafe(res);
		const arr = body?.data || [];
		selMedium.innerHTML = `<option value="">선택</option>` + arr.map(x => `<option value="${x.id}">${escapeHtml(x.name)}</option>`).join('');
		selSmall.innerHTML = `<option value="">선택</option>`;
	}

	async function loadSmalls(mediumId) {
		if (!mediumId) {
			selSmall.innerHTML = `<option value="">선택</option>`;
			return;
		}
		const res = await fetch(`/api/admin/root/clientTransfer/categories/smalls?mediumId=${mediumId}`, { headers: headersJson() });
		const body = await toJsonSafe(res);
		const arr = body?.data || [];
		selSmall.innerHTML = `<option value="">선택</option>` + arr.map(x => `<option value="${x.id}">${escapeHtml(x.name)}</option>`).join('');
	}

	if (selLarge) {
		selLarge.addEventListener('change', async () => {
			await loadMediums(selLarge.value);
		});
	}
	if (selMedium) {
		selMedium.addEventListener('change', async () => {
			await loadSmalls(selMedium.value);
		});
	}

	async function initCategorySelectors() {
		if (!selLarge) return;
		await loadLarges();
		await loadMediums('');
		await loadSmalls('');
	}

	function addCategoryPermissionWithRule() {
		const largeId = selLarge.value ? Number(selLarge.value) : null;
		const mediumId = selMedium.value ? Number(selMedium.value) : null;
		const smallId = selSmall.value ? Number(selSmall.value) : null;

		if (!largeId) {
			showAlert('대분류는 필수입니다.');
			return;
		}
		if (smallId && !mediumId) {
			showAlert('소분류를 선택한 경우 중분류는 필수입니다.');
			return;
		}

		const level = smallId ? 'SMALL' : (mediumId ? 'MEDIUM' : 'LARGE');

		if (level === 'LARGE') {
			if (existsLargeAll(largeId)) {
				showAlert('이미 해당 대분류 전체 권한이 등록되어 있습니다.');
				return;
			}
			removeByLarge(largeId);
			permState.push({ largeId, mediumId: null, smallId: null });
		}

		if (level === 'MEDIUM') {
			if (existsLargeAll(largeId)) {
				showAlert('이미 해당 대분류 전체 권한이 등록되어 있어, 중분류를 추가 등록할 필요가 없습니다.');
				return;
			}
			if (!mediumId) {
				showAlert('중분류 선택이 올바르지 않습니다.');
				return;
			}
			removeByMedium(largeId, mediumId);
			permState.push({ largeId, mediumId, smallId: null });
		}

		if (level === 'SMALL') {
			if (existsLargeAll(largeId)) {
				showAlert('이미 해당 대분류 전체 권한이 등록되어 있어, 소분류를 추가 등록할 필요가 없습니다.');
				return;
			}
			if (!mediumId) {
				showAlert('소분류 등록은 중분류 선택이 필요합니다.');
				return;
			}
			if (existsMediumAll(largeId, mediumId)) {
				showAlert('이미 해당 중분류 전체 권한이 등록되어 있어, 소분류를 추가 등록할 필요가 없습니다.');
				return;
			}
			if (!smallId) {
				showAlert('소분류 선택이 올바르지 않습니다.');
				return;
			}

			const existsExact = (permState || []).some(p =>
				p.largeId === largeId && p.mediumId === mediumId && p.smallId === smallId
			);
			if (existsExact) {
				showAlert('이미 등록된 권한입니다.');
				return;
			}
			permState.push({ largeId, mediumId, smallId });
		}

		permState = normalizePermList(permState);
		renderPermissionList();
		refreshSellerDirty();

		selLarge.value = '';
		selMedium.innerHTML = `<option value="">선택</option>`;
		selSmall.innerHTML = `<option value="">선택</option>`;
	}

	if (btnAddPerm) {
		btnAddPerm.addEventListener('click', () => {
			addCategoryPermissionWithRule();
		});
	}

	if (permList) {
		permList.addEventListener('click', (e) => {
			const btn = e.target.closest('.client-detail-home-btn-delete-permission');
			if (!btn) return;

			const li = btn.closest('li[data-perm-key]');
			if (!li) return;

			if (!confirm('해당 카테고리 권한을 삭제하시겠습니까? (저장 버튼을 눌러야 반영됩니다)')) return;

			const k = li.getAttribute('data-perm-key');
			permState = (permState || []).filter(p => keyOf(p) !== k);
			permState = normalizePermList(permState);
			renderPermissionList();
			refreshSellerDirty();
		});
	}

	function isPermissionChanged() {
		return normalizeObject(normalizePermList((permState || []).map(x => ({
			largeId: x.largeId,
			mediumId: x.mediumId || null,
			smallId: x.smallId || null
		})))) !== normalizeObject(initialPermState);
	}

	// =========================
	// 10) Seller saveAll
	// =========================
	if (btnSaveSeller) {
		btnSaveSeller.addEventListener('click', async () => {
			if (!dirty.seller) return;
			if (!confirm('셀러 변경사항(프로필/로고/주소/정산/담당자/카테고리)을 저장하시겠습니까?')) return;

			let contacts;
			try {
				contacts = collectSellerContactsForSave();
			} catch (e) {
				showAlert(e.message || '담당자 입력값을 확인해 주세요.');
				return;
			}

			const payload = {
				shopName: nvValue('client-detail-home-seller-shopName'),
				supplierCode: nvValue('client-detail-home-seller-supplierCode'),
				tel: nvValue('client-detail-home-seller-tel'),
				fax: nvValue('client-detail-home-seller-fax'),
				homepageUrl: nvValue('client-detail-home-seller-homepageUrl'),
				productTypeText: nvValue('client-detail-home-seller-productTypeText'),

				tradingStatus: nvValue('client-detail-home-seller-tradingStatus'),
				supplyType: nvValue('client-detail-home-seller-supplyType'),
				supplyStructure: nvValue('client-detail-home-seller-supplyStructure'),

				dealStartDate: document.getElementById('client-detail-home-seller-dealStartDate')?.value || null,
				dealStopDate: document.getElementById('client-detail-home-seller-dealStopDate')?.value || null,

				logoAction: sellerLogoAction?.value || 'KEEP',

				businessAddress: {
					postcode: nvText('client-detail-home-seller-biz-postcode'),
					roadAddress: nvText('client-detail-home-seller-biz-road'),
					jibunAddress: nvText('client-detail-home-seller-biz-jibun'),
					detailAddress: nvText('client-detail-home-seller-biz-detail')
				},
				returnAddress: {
					postcode: nvText('client-detail-home-seller-return-postcode'),
					roadAddress: nvText('client-detail-home-seller-return-road'),
					jibunAddress: nvText('client-detail-home-seller-return-jibun'),
					detailAddress: nvText('client-detail-home-seller-return-detail')
				},

				settlement: {
					commissionRate: document.getElementById('client-detail-home-settle-commissionRate')?.value || null,
					cycle: document.getElementById('client-detail-home-settle-cycle')?.value || null,
					basis: document.getElementById('client-detail-home-settle-basis')?.value || null,
					nextSettlementDate: document.getElementById('client-detail-home-settle-nextDate')?.value || null
				},

				contacts: contacts
			};

			if (isPermissionChanged()) {
				payload.deletePermissionIds = existingPermIds.slice();
				payload.addPermissions = (permState || []).map(p => ({
					largeId: p.largeId,
					mediumId: p.mediumId || null,
					smallId: p.smallId || null
				}));
			}

			const formData = new FormData();
			formData.append('request', new Blob([JSON.stringify(payload)], { type: 'application/json' }));

			const logoFile = sellerLogoFile?.files?.[0];
			if ((sellerLogoAction?.value || 'KEEP') === 'REPLACE') {
				if (!logoFile) {
					showAlert('로고 변경으로 선택되었지만 업로드 파일이 없습니다.');
					return;
				}
				formData.append('logoFile', logoFile);
			}

			const res = await fetch(apiUrl('/seller/saveAll'), {
				method: 'POST',
				headers: headersOnlyCsrf(),
				body: formData
			});

			const body = await toJsonSafe(res);
			if (!res.ok || !body?.success) {
				showAlert(body?.message || '셀러 저장 실패');
				return;
			}

			showAlert(body.message || '저장 완료');
			window.location.reload();
		});
	}

	// =========================
	// init
	// =========================
	initPermissionStateFromDom();
	initCategorySelectors();
	ensureEmptyContactRow();
	initialSellerSnapshot = captureCurrentSellerSnapshot();
	refreshSellerDirty();

})();