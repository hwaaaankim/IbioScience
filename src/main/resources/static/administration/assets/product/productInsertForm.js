// productInsertForm.js
document.addEventListener("DOMContentLoaded", function() {
	// ===== 공용 유틸 =====
	const $$ = (sel, root = document) => root.querySelector(sel);
	const $$$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));
	const logSection = (title) => { console.group(`🧩 ${title}`); };
	const endSection = () => { console.groupEnd(); };

	// ===== 카테고리(외부) =====
	const largeList = document.getElementById('category-large-list');
	const mediumList = document.getElementById('category-medium-list');
	const smallList = document.getElementById('category-small-list');
	const selectedList = document.getElementById('selected-category-list');

	let selectedCategories = [];
	let largeCategoryMap = {};
	let mediumCategoryMap = {};

	// 스크롤 고정
	[largeList, mediumList, smallList].forEach(el => { el.style.maxHeight = '240px'; el.style.overflowY = 'auto'; });

	// 대분류
	fetch('/api/category/list-large')
		.then(res => res.json())
		.then(list => {
			largeList.innerHTML = '';
			list.forEach(large => {
				const li = document.createElement('li');
				li.className = 'list-group-item list-group-item-action category-large-item d-flex justify-content-between align-items-center';
				li.dataset.id = large.id;
				li.innerHTML = `<span>${large.name}</span><span class="badge bg-light text-dark ms-2" data-large-badge="${large.id}">${large.mediumCount ?? 0}</span>`;
				largeCategoryMap[large.id] = large.name;
				largeList.appendChild(li);
			});
		});

	largeList.addEventListener('click', (e) => {
		const li = e.target.closest('.category-large-item');
		if (!li) return;
		const largeId = li.dataset.id;
		fetch(`/api/category/list-medium?largeId=${largeId}`)
			.then(res => res.json())
			.then(list => {
				mediumList.innerHTML = '';
				list.forEach(m => {
					const li2 = document.createElement('li');
					li2.className = 'list-group-item list-group-item-action category-medium-item d-flex justify-content-between align-items-center';
					li2.dataset.id = m.id;
					li2.innerHTML = `<span>${m.name}</span><span class="badge bg-light text-dark ms-2" data-medium-badge="${m.id}">${m.smallCount ?? 0}</span>`;
					mediumCategoryMap[m.id] = { name: m.name, largeId: largeId };
					mediumList.appendChild(li2);
				});
				smallList.innerHTML = '';
			});
	});

	mediumList.addEventListener('click', (e) => {
		const li = e.target.closest('.category-medium-item');
		if (!li) return;
		const mediumId = li.dataset.id;
		fetch(`/api/category/list-small?mediumId=${mediumId}`)
			.then(res => res.json())
			.then(list => {
				smallList.innerHTML = '';
				list.forEach(s => {
					const li3 = document.createElement('li');
					li3.textContent = s.name;
					li3.className = 'list-group-item list-group-item-action category-small-item';
					li3.dataset.id = s.id;
					li3.dataset.mediumId = mediumId;
					smallList.appendChild(li3);
				});
			});
	});

	smallList.addEventListener('click', (e) => {
		const li = e.target.closest('.category-small-item');
		if (!li) return;
		const smallId = li.dataset.id;
		const mediumId = li.dataset.mediumId;
		const mediumInfo = mediumCategoryMap[mediumId] || {};
		const largeId = mediumInfo.largeId;
		const largeName = largeCategoryMap[largeId] || '';
		const mediumName = mediumInfo.name || '';
		const smallName = li.textContent;

		if (!selectedCategories.some(sc => String(sc.id) === String(smallId))) {
			selectedCategories.push({ id: smallId, largeId, largeName, mediumId, mediumName, smallName });
			renderSelectedCategories();
		}
	});

	function debugScanHtml(tag, key, html) {
		try {
			const hasDoubleSlashUpload = /(^|["'=\s])\/\/upload\//i.test(html);
			const tempCount = (html.match(/\/upload\/temp\//g) || []).length;

			if (hasDoubleSlashUpload) {
				console.warn(`[${tag}] ${key} - "//upload/" 흔적 감지 → 치환 필요`);
			}
			if (tempCount > 0) {
				console.warn(`[${tag}] ${key} - "/upload/temp/" 잔여 ${tempCount}건`);
			}
			if (!hasDoubleSlashUpload && tempCount === 0) {
				console.log(`[${tag}] ${key} - 잔여 없음(ok)`);
			}
		} catch (e) {
			console.warn(`[${tag}] ${key} - debugScanHtml 오류`, e);
		}
	}

	function renderSelectedCategories() {
		selectedList.innerHTML = '';
		selectedCategories.forEach((c, idx) => {
			const div = document.createElement('div');
			div.className = 'badge bg-primary text-white px-2 py-2 me-2 d-flex align-items-center';
			div.innerHTML = `${c.largeName} &gt; ${c.mediumName} &gt; <b>${c.smallName}</b>
        <span class="ms-2" style="cursor:pointer;" title="삭제">[삭제]</span>`;
			div.querySelector('span').onclick = () => {
				selectedCategories.splice(idx, 1);
				renderSelectedCategories();
			};
			selectedList.appendChild(div);
		});
	}

	// ===== 내부분류(자체) =====
	const internalLarge = document.getElementById('internal-large-select');
	const internalMedium = document.getElementById('internal-medium-select');
	const internalSmall = document.getElementById('internal-small-select');
	let internalCategorySmallId = '';

	function fetchInternalLarge() {
		fetch('/api/internal-category/list-large')
			.then(r => r.json()).then(list => {
				internalLarge.innerHTML = `<option value="">내부 대분류</option>`;
				list.forEach(x => {
					internalLarge.innerHTML += `<option value="${x.id}">${x.name} (${x.mediumCount ?? 0})</option>`;
				});
				internalMedium.innerHTML = `<option value="">내부 중분류</option>`;
				internalSmall.innerHTML = `<option value="">내부 소분류</option>`;
			})
			.catch(() => {
				console.warn('[내부분류] API가 아직 준비되지 않았습니다. (list-large)');
			});
	}

	function fetchInternalMedium(largeId) {
		internalMedium.innerHTML = `<option value="">내부 중분류</option>`;
		internalSmall.innerHTML = `<option value="">내부 소분류</option>`;
		if (!largeId) return;
		fetch(`/api/internal-category/list-medium?largeId=${largeId}`)
			.then(r => r.json()).then(list => {
				list.forEach(x => {
					internalMedium.innerHTML += `<option value="${x.id}">${x.name} (${x.smallCount ?? 0})</option>`;
				});
			})
			.catch(() => console.warn('[내부분류] API 미구현 (list-medium)'));
	}

	function fetchInternalSmall(mediumId) {
		internalSmall.innerHTML = `<option value="">내부 소분류</option>`;
		if (!mediumId) return;
		fetch(`/api/internal-category/list-small?mediumId=${mediumId}`)
			.then(r => r.json()).then(list => {
				list.forEach(x => {
					internalSmall.innerHTML += `<option value="${x.id}">${x.name} (${x.productCount ?? 0})</option>`;
				});
			})
			.catch(() => console.warn('[내부분류] API 미구현 (list-small)'));
	}

	internalLarge.onchange = () => fetchInternalMedium(internalLarge.value);
	internalMedium.onchange = () => fetchInternalSmall(internalMedium.value);
	internalSmall.onchange = () => { internalCategorySmallId = internalSmall.value || ''; };
	// 초기 로드
	fetchInternalLarge();

	// ===== 키워드 =====
	let keywords = [];
	const keywordInput = document.getElementById('product-keyword-input');
	const addKeywordBtn = document.getElementById('add-keyword-btn');
	const keywordList = document.getElementById('product-keyword-list');

	function renderKeywordList() {
		keywordList.innerHTML = '';
		keywords.forEach((kw, idx) => {
			const badge = document.createElement('div');
			badge.className = 'badge bg-info text-dark px-2 py-2 d-flex align-items-center mt-2';
			badge.style.fontSize = '14px';
			badge.innerHTML = `<span>${kw}</span><span class="ms-2" style="cursor:pointer;" title="삭제">&times;</span>`;
			badge.lastElementChild.onclick = () => { keywords.splice(idx, 1); renderKeywordList(); };
			keywordList.appendChild(badge);
		});
	}
	function addKeyword() {
		const kw = (keywordInput.value || '').trim();
		if (!kw) return;
		if (keywords.includes(kw)) return;
		keywords.push(kw);
		keywordInput.value = '';
		renderKeywordList();
	}
	addKeywordBtn.onclick = addKeyword;
	keywordInput.onkeydown = (e) => { if (e.key === 'Enter') { addKeyword(); e.preventDefault(); } };

	// ===== 할인혜택(프로모션) : 다중 선택으로 통일 =====
	const discountModal = document.getElementById('discountModal');
	const openDiscountBtn = document.getElementById('open-discount-modal-btn');
	const discountOverlayClose = $$$('.discount-modal-close, .discount-modal-overlay, .discount-modal-cancel', discountModal);
	const searchPromotionBtn = document.getElementById('search-promotion-btn');
	const promotionModalList = document.getElementById('promotion-modal-list');
	const selectedDiscountListModal = document.getElementById('selected-discount-list-modal');
	const selectedDiscountListMain = document.getElementById('selected-discount-list');
	const discountApplyBtn = document.getElementById('discount-apply-btn');

	let selectedDiscounts = []; // [{id, name, type, term, active, ...}]
	function openModal(modalEl) { modalEl.style.display = 'block'; modalEl.setAttribute('aria-hidden', 'false'); modalEl.classList.add('active'); }
	function closeModal(modalEl) { modalEl.classList.remove('active'); modalEl.setAttribute('aria-hidden', 'true'); modalEl.style.display = 'none'; }

	openDiscountBtn.onclick = () => {
		openModal(discountModal);
		renderPromotionList([]);
		renderSelectedDiscountsModal();
	};
	discountOverlayClose.forEach(btn => btn.addEventListener('click', () => closeModal(discountModal)));

	function renderPromotionList(list) {
		promotionModalList.innerHTML = '';
		if (!list || list.length === 0) {
			promotionModalList.innerHTML = '<div class="text-muted text-center">프로모션이 없습니다.</div>';
			return;
		}
		list.forEach(d => {
			const row = document.createElement('div');
			row.className = 'd-flex align-items-center border-bottom py-1';
			const already = selectedDiscounts.some(x => String(x.id) === String(d.id));
			row.innerHTML = `
        <span class="me-2">${d.name}</span>
        <span class="badge bg-info text-dark me-2">${d.typeLabel || d.type}</span>
        <span class="badge bg-secondary me-2">${d.termLabel || d.term}</span>
        <span class="badge ${d.active ? 'bg-primary' : 'bg-secondary'}">${d.active ? 'ON' : 'OFF'}</span>
        <button type="button" class="btn btn-outline-primary btn-sm ms-auto" ${already ? 'disabled' : ''}>추가</button>
      `;
			row.querySelector('button').onclick = () => {
				if (!selectedDiscounts.some(x => String(x.id) === String(d.id))) {
					selectedDiscounts.push(d);
					renderSelectedDiscountsModal();
					// 버튼 비활성화
					row.querySelector('button').disabled = true;
				}
			};
			promotionModalList.appendChild(row);
		});
	}

	function renderSelectedDiscountsModal() {
		selectedDiscountListModal.innerHTML = '';
		if (selectedDiscounts.length === 0) {
			selectedDiscountListModal.innerHTML = '<div class="text-muted">선택된 항목이 없습니다.</div>';
			return;
		}
		selectedDiscounts.forEach((d, idx) => {
			const badge = document.createElement('div');
			badge.className = 'badge bg-danger text-white px-2 py-2 d-flex align-items-center';
			badge.innerHTML = `<span>${d.name} (${d.typeLabel || d.type})</span><span class="ms-2" style="cursor:pointer;" title="삭제">&times;</span>`;
			badge.lastElementChild.onclick = () => {
				selectedDiscounts.splice(idx, 1);
				renderSelectedDiscountsModal();
			};
			selectedDiscountListModal.appendChild(badge);
		});
	}

	function renderSelectedDiscountsMain() {
		selectedDiscountListMain.innerHTML = '';
		selectedDiscounts.forEach((d, idx) => {
			const badge = document.createElement('div');
			badge.className = 'badge bg-danger text-white px-2 py-2 d-flex align-items-center';
			badge.innerHTML = `<span>${d.name} (${d.typeLabel || d.type})</span><span class="ms-2" style="cursor:pointer;" title="삭제">&times;</span>`;
			badge.lastElementChild.onclick = () => {
				selectedDiscounts.splice(idx, 1);
				renderSelectedDiscountsMain();
			};
			selectedDiscountListMain.appendChild(badge);
		});
	}

	discountApplyBtn.onclick = () => {
		renderSelectedDiscountsMain();
		closeModal(discountModal);
	};

	// 검색
	searchPromotionBtn.onclick = function() {
		const name = document.getElementById('promotionName').value.trim();
		const type = document.getElementById('promotionType').value;
		const start = document.getElementById('promotionStart').value;
		const end = document.getElementById('promotionEnd').value;
		const active = document.getElementById('promotionActive').value;
		let url = `/api/promotion/search?`;
		url += name ? `name=${encodeURIComponent(name)}&` : '';
		url += type ? `type=${encodeURIComponent(type)}&` : '';
		url += start ? `startDate=${start}&` : '';
		url += end ? `endDate=${end}&` : '';
		url += (active ? `active=${active}&` : '');
		fetch(url).then(res => res.json()).then(list => renderPromotionList(list));
	};

	// ===== 브랜드 =====
	const brandSearchInput = document.getElementById('brand-search-input');
	const brandSearchBtn = document.getElementById('brand-search-btn');
	const brandSearchResult = document.getElementById('brand-search-result');
	const brandSelectedArea = document.getElementById('brand-selected-area');
	let selectedBrand = null;

	brandSearchBtn.onclick = function() {
		const kw = brandSearchInput.value.trim();
		if (!kw) return;
		fetch(`/api/brand/search?keyword=${encodeURIComponent(kw)}`)
			.then(res => res.json())
			.then(list => {
				brandSearchResult.innerHTML = '';
				if (!list || list.length === 0) {
					brandSearchResult.innerHTML = '<div class="text-center text-muted py-2">검색결과가 없습니다.</div>';
					return;
				}
				list.forEach((b) => {
					const div = document.createElement('div');
					div.className = 'brand-search-item d-flex align-items-center gap-2 py-1';
					// imageRoad / imageUrl 둘 다 대응
					const imgUrl = b.imageRoad || b.imageUrl || '/assets/brand-default.png';
					div.innerHTML = `<img src="${imgUrl}" alt="브랜드" style="width:32px;height:32px;object-fit:cover;border-radius:6px;">
            <span>${b.name}</span>
            <button type="button" class="btn btn-outline-primary btn-sm ms-auto">선택</button>`;
					div.querySelector('button').onclick = () => { selectedBrand = b; renderSelectedBrand(); };
					brandSearchResult.appendChild(div);
				});
			});
	};

	function renderSelectedBrand() {
		brandSelectedArea.innerHTML = '';
		if (!selectedBrand) return;
		const imgUrl = selectedBrand.imageRoad || selectedBrand.imageUrl || '/assets/brand-default.png';
		brandSelectedArea.innerHTML = `
      <div class="d-flex align-items-center bg-light p-2 rounded">
        <img src="${imgUrl}" style="width:40px;height:40px;object-fit:cover;border-radius:6px;">
        <span class="ms-2">${selectedBrand.name}</span>
        <button type="button" class="btn btn-outline-danger btn-sm ms-2" id="remove-brand-btn">삭제</button>
      </div>`;
		document.getElementById('remove-brand-btn').onclick = () => { selectedBrand = null; renderSelectedBrand(); };
	}

	// ===== 딜러 등급별 할인 =====
	const dealerDiscounts = {}; // { A: '10', ... }
	const dealerDiscountButtons = document.getElementById('dealer-discount-buttons');
	const dealerDiscountList = document.getElementById('dealer-discount-list');

	dealerDiscountButtons.querySelectorAll('button').forEach(btn => {
		btn.onclick = function() {
			const grade = btn.dataset.grade;
			if (dealerDiscounts[grade]) return;
			dealerDiscounts[grade] = '';
			renderDealerDiscountList();
			btn.disabled = true;
		};
	});
	function renderDealerDiscountList() {
		dealerDiscountList.innerHTML = '';
		Object.keys(dealerDiscounts).forEach(grade => {
			const col = document.createElement('div');
			col.className = 'col-12';
			col.innerHTML = `
        <div class="input-group input-group-sm align-items-center">
          <span class="input-group-text" style="min-width:120px;">${grade} 등급 딜러 추가 할인율</span>
          <input type="number" min="0" max="100" class="form-control" style="max-width:120px;" placeholder="0"
                 value="${dealerDiscounts[grade]}" data-grade="${grade}" />
          <span class="input-group-text">%</span>
          <button type="button" class="btn btn-outline-danger btn-sm" data-grade="${grade}" title="삭제">×</button>
        </div>`;
			col.querySelector('input').oninput = function() { dealerDiscounts[grade] = this.value; };
			col.querySelector('button').onclick = function() {
				delete dealerDiscounts[grade];
				dealerDiscountButtons.querySelector(`button[data-grade="${grade}"]`).disabled = false;
				renderDealerDiscountList();
			};
			dealerDiscountList.appendChild(col);
		});
	}

	// ===== 추가입력필드 =====
	const extraFieldList = document.getElementById('product-manager-extra-field-list');
	const addExtraFieldBtn = document.getElementById('product-manager-add-extra-field');
	let extraFields = [];

	function syncExtraFieldsFromDOM() {
		const rows = extraFieldList.querySelectorAll('.input-group');
		extraFields = [];
		rows.forEach((row) => {
			const label = row.querySelector('input[name$=".label"]')?.value || '';
			const value = row.querySelector('input[name$=".value"]')?.value || '';
			extraFields.push({ label, value });
		});
	}
	function renderExtraFields() {
		extraFieldList.innerHTML = '';
		extraFields.forEach((field, idx) => {
			const row = document.createElement('div');
			row.className = 'input-group mb-2';
			row.innerHTML = `
        <input type="text" class="form-control form-control-sm" name="extraFields[${idx}].label" placeholder="질문명" value="${field.label || ''}" required>
        <input type="text" class="form-control form-control-sm" name="extraFields[${idx}].value" placeholder="답변값" value="${field.value || ''}" required>
        <button type="button" class="btn btn-outline-danger btn-sm" title="삭제">×</button>`;
			row.querySelectorAll('input').forEach(input => input.addEventListener('input', syncExtraFieldsFromDOM));
			row.querySelector('button').onclick = () => { syncExtraFieldsFromDOM(); extraFields.splice(idx, 1); renderExtraFields(); };
			extraFieldList.appendChild(row);
		});
	}
	addExtraFieldBtn.onclick = () => { syncExtraFieldsFromDOM(); extraFields.push({ label: '', value: '' }); renderExtraFields(); };
	renderExtraFields(); // 초기 1개

	// ===== 옵션 그룹 =====
	const optionGroupList = document.getElementById('product-manager-option-group-list');
	const addOptionGroupBtn = document.getElementById('product-manager-add-option-group');
	let optionGroups = [];

	// ===== [교체] syncOptionGroupsFromDOM =====
	function syncOptionGroupsFromDOM() {
		const groupCards = optionGroupList.querySelectorAll('.card');
		optionGroups.forEach((group, groupIdx) => {
			const groupCard = groupCards[groupIdx];
			if (!groupCard) return;
			group.name = groupCard.querySelector(`[name="optionGroups[${groupIdx}].name"]`)?.value || '';
			const optionRows = groupCard.querySelectorAll('.input-group.mb-1');
			group.options.forEach((opt, optIdx) => {
				const row = optionRows[optIdx]; if (!row) return;
				opt.name = row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].name"]`)?.value || '';
				opt.value = row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].value"]`)?.value || '';
				opt.extraPrice = row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].extraPrice"]`)?.value || '';
				opt.sign = row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].sign"]`)?.value || 'PLUS';
			});
		});
	}

	function renderOptionGroups() {
		optionGroupList.innerHTML = '';
		optionGroups.forEach((group, groupIdx) => {
			const groupDiv = document.createElement('div');
			groupDiv.className = 'card mb-2';
			groupDiv.innerHTML = `
        <div class="card-body p-2">
          <div class="input-group mb-2">
            <input type="text" class="form-control form-control-sm" name="optionGroups[${groupIdx}].name" placeholder="옵션 그룹명" value="${group.name || ''}" required>
            <button type="button" class="btn btn-outline-danger btn-sm" title="옵션그룹 삭제">×</button>
          </div>
          <div id="option-group-options-${groupIdx}"></div>
          <button type="button" class="btn btn-outline-primary btn-sm mt-1" data-group-idx="${groupIdx}">+ 옵션 추가</button>
        </div>`;
			// 삭제
			groupDiv.querySelector('.btn-outline-danger').onclick = () => { syncOptionGroupsFromDOM(); optionGroups.splice(groupIdx, 1); renderOptionGroups(); };
			// 옵션 추가
			groupDiv.querySelector('.btn-outline-primary').onclick = () => {
				syncOptionGroupsFromDOM();
				group.options.push({ name: '', value: '', extraPrice: '', sign: 'PLUS', sortOrder: group.options.length + 1 });
				renderOptionGroups();
			};
			groupDiv.querySelector(`[name="optionGroups[${groupIdx}].name"]`).addEventListener('input', syncOptionGroupsFromDOM);

			const optionsContainer = groupDiv.querySelector(`#option-group-options-${groupIdx}`);
			group.options.forEach((opt, optIdx) => {
				const optRow = document.createElement('div');
				optRow.className = 'input-group mb-1';
				optRow.innerHTML = `
					  <input type="text" class="form-control form-control-sm" name="optionGroups[${groupIdx}].options[${optIdx}].name" placeholder="옵션명" value="${opt.name || ''}" required>
					  <input type="text" class="form-control form-control-sm" name="optionGroups[${groupIdx}].options[${optIdx}].value" placeholder="값" value="${opt.value || ''}">
					  <input type="number" class="form-control form-control-sm" name="optionGroups[${groupIdx}].options[${optIdx}].extraPrice" placeholder="추가금액" value="${opt.extraPrice || ''}">
					  <select class="form-select form-select-sm" name="optionGroups[${groupIdx}].options[${optIdx}].sign">
					    <option value="PLUS" ${opt.sign === 'PLUS' ? 'selected' : ''}>+</option>
					    <option value="MINUS" ${opt.sign === 'MINUS' ? 'selected' : ''}>-</option>
					  </select>
					  <button type="button" class="btn btn-outline-danger btn-sm" title="옵션 삭제">×</button>`;
				optRow.querySelector('.btn-outline-danger').onclick = () => { syncOptionGroupsFromDOM(); group.options.splice(optIdx, 1); renderOptionGroups(); };
				optRow.querySelectorAll('input, select').forEach(inp => {
					inp.addEventListener('input', syncOptionGroupsFromDOM);
					inp.addEventListener('change', syncOptionGroupsFromDOM);
				});
				optionsContainer.appendChild(optRow);
			});
			optionGroupList.appendChild(groupDiv);
		});
	}
	addOptionGroupBtn.onclick = () => { syncOptionGroupsFromDOM(); optionGroups.push({ name: '', options: [] }); renderOptionGroups(); };
	renderOptionGroups();

	// ===== CKEditor(공통 표시항목 + 상세설명) =====
	let ckeInstances = {};
	function CustomUploadAdapterPlugin(editor) {
		editor.plugins.get('FileRepository').createUploadAdapter = (loader) => {
			window.currentEditorType = editor.sourceElement.getAttribute('data-type');
			window.currentEditorKey = editor.sourceElement.getAttribute('data-key');
			return new CustomUploadAdapter(loader);
		};
	}
	class CustomUploadAdapter {
		constructor(loader) { this.loader = loader; }
		upload() {
			return this.loader.file.then(file => new Promise(async (resolve, reject) => {
				try {
					const formData = new FormData();
					formData.append('files', file);
					formData.append('type', window.currentEditorType);
					formData.append('key', window.currentEditorKey);
					const res = await fetch('/api/product/editor-images', { method: 'POST', body: formData });
					if (!res.ok) return reject(new Error('이미지 업로드 실패'));
					const data = await res.json();
					if (!data.success || !data.imageUrls || data.imageUrls.length === 0) return reject(new Error('이미지 업로드 실패(서버응답)'));
					resolve({ default: data.imageUrls[0] });
				} catch (e) { reject(e); }
			}));
		}
		abort() { }
	}
	// === [추가] 에디터 임시이미지 식별 접두어
	const TEMP_IMG_PREFIX = '/upload/temp/';

	// === 문자열에서 임시 이미지 URL만 뽑아내기(절대/상대 모두, 중복 제거, 순서 유지)
	function extractTempImageUrls(html) {
		if (!html) return [];
		const urls = [];
		const seen = new Set();
		const re = /<img[^>]*\s+src\s*=\s*(['"]?)([^'">\s]+)\1/ig;
		let m;
		while ((m = re.exec(html)) !== null) {
			let url = m[2];
			const idx = url.indexOf('/upload/');
			if (idx < 0) continue;

			url = url.substring(idx); // '/upload/temp/...'

			// 여기서 상수 활용
			if (!url.startsWith(TEMP_IMG_PREFIX)) continue;

			if (!seen.has(url)) {
				seen.add(url);
				urls.push(url);
			}
		}
		console.log('[TEMP_URLS]', { count: urls.length, urls });
		return urls;
	}

	// === [NEW] 최종 HTML에서 /upload/product/ URL만 뽑기 (중복 제거, 순서 유지)
	function extractProductImageUrls(html) {
		if (!html) return [];
		const out = [];
		const seen = new Set();
		const re = /\/upload\/product\/[^"' >]+/ig;
		let m;
		while ((m = re.exec(html)) !== null) {
			const url = m[0];
			if (!seen.has(url)) { seen.add(url); out.push(url); }
		}
		return out;
	}

	// === [추가] 서버 통신 래퍼: 실패시 상세 메시지 파싱
	async function fetchJson(url, init = {}) {
		const res = await fetch(url, init);
		if (!res.ok) {
			let msg = `HTTP ${res.status}`;
			try {
				const data = await res.json();
				if (data && (data.message || data.error)) msg = data.message || data.error;
			} catch { /* non-json */
				try { msg = await res.text(); } catch { }
			}
			const err = new Error(msg);
			err.status = res.status;
			throw err;
		}
		return res.json();
	}

	// [교체] JSON/TEXT 자동 판별 유틸
	async function fetchJsonOrText(url, init = {}) {
		const res = await fetch(url, init);
		if (!res.ok) {
			let msg = `HTTP ${res.status}`;
			try {
				const data = await res.json();
				msg = data?.message || data?.error || msg;
			} catch {
				try { msg = await res.text(); } catch { /* ignore */ }
			}
			const err = new Error(msg);
			err.status = res.status;
			throw err;
		}
		const ct = (res.headers.get('content-type') || '').toLowerCase();
		if (ct.includes('application/json')) return res.json();
		const text = await res.text();
		try { return JSON.parse(text); } catch { return text; }
	}

	// [교체] moveEditorImagesAPI 에서 위 유틸 사용
	async function moveEditorImagesAPI(productId, payload) {
		return fetchJsonOrText(`/api/product/${productId}/move-editor-images`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify(payload)
		});
	}


	// === [추가] submit 버튼 상태 토글
	function setSubmitting(on) {
		const btn = document.getElementById('submitProductBtn');
		if (!btn) return;
		btn.disabled = !!on;
		btn.dataset._originalText ??= btn.textContent;
		btn.textContent = on ? '저장 중...' : btn.dataset._originalText;
	}

	function makeQuestionInput(option) {
		const requiredAttr = option.required ? 'required' : '';
		const editorId = option.type === 'CKEDITOR' ? `editor-question-${option.id}` : '';
		let inputHtml = '';
		switch (option.type) {
			case 'INPUT':
				inputHtml = `<input type="text" class="form-control form-control-sm" name="question_${option.id}" placeholder="${option.placeholder || ''}" ${requiredAttr}>`; break;
			case 'TEXTAREA':
				inputHtml = `<textarea class="form-control form-control-sm" name="question_${option.id}" rows="2" placeholder="${option.placeholder || ''}" ${requiredAttr}></textarea>`; break;
			case 'SELECT':
				inputHtml = `<select class="form-select form-select-sm" name="question_${option.id}" ${requiredAttr}>${(Array.isArray(option.options) && option.options.length > 0)
					? option.options.map(opt => {
						if (typeof opt === 'object' && opt) {
							if ('value' in opt && 'label' in opt) return `<option value="${opt.value}">${opt.label}</option>`;
							const keys = Object.keys(opt);
							if (keys.length === 1) return `<option value="${keys[0]}">${opt[keys[0]]}</option>`;
							if ('value' in opt) return `<option value="${opt.value}">${opt.value}</option>`;
							if ('label' in opt) return `<option value="${opt.label}">${opt.label}</option>`;
							return `<option disabled>선택지 오류</option>`;
						} else {
							return `<option value="${opt}">${opt}</option>`;
						}
					}).join('')
					: '<option disabled>선택지 없음</option>'
					}</select>`; break;
			case 'FILE':
				inputHtml = `<input type="file" class="form-control form-control-sm" name="question_${option.id}" ${requiredAttr}>`; break;
			case 'CKEDITOR':
				inputHtml = `<textarea class="form-control" name="question_${option.id}" id="${editorId}" rows="3" ${requiredAttr} data-type="question" data-key="question_${option.id}"></textarea>`; break;
			default:
				inputHtml = `<input type="text" class="form-control form-control-sm" disabled placeholder="지원되지 않는 타입">`;
		}
		return inputHtml;
	}

	fetch('/api/display-questions/list-common')
		.then(res => res.json())
		.then(list => {
			const container = document.getElementById('product-manager-display-options');
			container.innerHTML = '';
			list.forEach(option => {
				let colClass = (option.type === 'TEXTAREA' || option.type === 'CKEDITOR') ? 'col-12 mb-2' : 'col-6 mb-2';
				const div = document.createElement('div');
				div.className = colClass + ' d-flex flex-column justify-content-end';
				div.innerHTML = `<label class="form-label mb-1">${option.label ?? option.name}${option.required ? ' <span class="text-danger">*</span>' : ''}</label>${makeQuestionInput(option)}`;
				container.appendChild(div);
			});
			// CKEditor mount (질문용)
			setTimeout(() => {
				list.filter(opt => opt.type === 'CKEDITOR').forEach(option => {
					const tId = `editor-question-${option.id}`;
					const textarea = document.getElementById(tId);
					if (textarea && !ckeInstances[tId] && window.ClassicEditor) {
						window.ClassicEditor.create(textarea, {
							toolbar: { items: ['heading', '|', 'bold', 'italic', 'underline', 'strikethrough', 'highlight', 'fontColor', 'fontBackgroundColor', '|', 'link', 'bulletedList', 'numberedList', 'blockQuote', '|', 'insertTable', 'imageUpload', 'mediaEmbed', '|', 'undo', 'redo', 'alignment', 'outdent', 'indent'] },
							image: { toolbar: ['imageTextAlternative', 'imageStyle:full', 'imageStyle:side', 'linkImage'], styles: ['full', 'side'], resizeUnit: 'px' },
							table: { contentToolbar: ['tableColumn', 'tableRow', 'mergeTableCells', 'tableCellProperties', 'tableProperties'] },
							mediaEmbed: { previewsInData: true },
							fontFamily: { options: ['default', 'Arial, Helvetica, sans-serif', 'Courier New, Courier, monospace', 'Georgia, serif', 'Lucida Sans Unicode, Lucida Grande, sans-serif', 'Tahoma, Geneva, sans-serif', 'Times New Roman, Times, serif', 'Trebuchet MS, Helvetica, sans-serif', 'Verdana, Geneva, sans-serif'] },
							fontSize: { options: ['tiny', 'small', 'default', 'big', 'huge'] },
							language: 'ko',
							extraPlugins: [CustomUploadAdapterPlugin]
						}).then(editor => { ckeInstances[tId] = editor; })
							.catch(err => console.error('CKEditor5 생성 오류:', err));
					}
				});
			}, 50);
		});

	// 상세설명 CKEditor
	let detailEditor = null;
	(function() {
		const desc = document.getElementById('editor-desc');
		if (desc && window.ClassicEditor) {
			desc.setAttribute('data-type', 'detailHtml');
			desc.setAttribute('data-key', 'detailHtml');
			window.ClassicEditor.create(desc, {
				toolbar: { items: ['heading', '|', 'bold', 'italic', 'underline', 'strikethrough', 'highlight', 'fontColor', 'fontBackgroundColor', '|', 'link', 'bulletedList', 'numberedList', 'blockQuote', '|', 'insertTable', 'imageUpload', 'mediaEmbed', '|', 'undo', 'redo', 'alignment', 'outdent', 'indent'] },
				image: { toolbar: ['imageTextAlternative', 'imageStyle:full', 'imageStyle:side', 'linkImage'], styles: ['full', 'side'], resizeUnit: 'px' },
				table: { contentToolbar: ['tableColumn', 'tableRow', 'mergeTableCells', 'tableCellProperties', 'tableProperties'] },
				mediaEmbed: { previewsInData: true },
				fontFamily: { options: ['default', 'Arial, Helvetica, sans-serif', 'Courier New, Courier, monospace', 'Georgia, serif', 'Lucida Sans Unicode, Lucida Grande, sans-serif', 'Tahoma, Geneva, sans-serif', 'Times New Roman, Times, serif', 'Trebuchet MS, Helvetica, sans-serif', 'Verdana, Geneva, sans-serif'] },
				fontSize: { options: ['tiny', 'small', 'default', 'big', 'huge'] },
				language: 'ko',
				extraPlugins: [CustomUploadAdapterPlugin]
			}).then(editor => { detailEditor = editor; })
				.catch(err => console.error('CKEditor5 생성 오류:', err));
		}
	})();

	// ===== 이미지(대표/추가) =====
	const mainInput = document.getElementById('product-manager-main-image');
	const mainPreview = document.getElementById('product-manager-main-image-preview');
	// ===== 대표이미지 미리보기 렌더 =====
	mainInput.addEventListener('change', function() {
		mainPreview.innerHTML = '';
		if (this.files.length > 0) {
			const file = this.files[0];
			const reader = new FileReader();
			reader.onload = e => {
				const wrap = document.createElement('div');
				wrap.className = 'product-insert-repimage-wrapper';
				const div = document.createElement('div');
				div.className = 'product-insert-repimage-thumb';
				div.innerHTML = `
					<img src="${e.target.result}" alt="대표이미지">
					<button class="btn-close btn-sm" aria-label="Remove"></button>`;
				div.querySelector('button').onclick = () => { mainInput.value = ''; mainPreview.innerHTML = ''; };
				wrap.appendChild(div);
				mainPreview.appendChild(wrap);
			};
			reader.readAsDataURL(file);
		}
	});


	const subInput = document.getElementById('product-manager-sub-image');
	const subPreview = document.getElementById('product-manager-sub-image-preview');
	let subFiles = [];
	subInput.addEventListener('change', function() { subFiles = Array.from(this.files); renderSubImagePreview(); });
	function renderSubImagePreview() {
		subPreview.innerHTML = '';
		subFiles.forEach((file, idx) => {
			const reader = new FileReader();
			reader.onload = e => {
				const div = document.createElement('div');
				div.className = 'image-preview-thumb position-relative';
				div.style.width = '100px'; div.style.height = '100px'; div.style.marginRight = '8px';
				div.innerHTML = `<img src="${e.target.result}" style="width:100%;height:100%;object-fit:cover;">
          <button class="btn-close btn-sm" style="position:absolute;top:0;right:0;z-index:2;" aria-label="Remove"></button>`;
				div.querySelector('button').onclick = () => { subFiles.splice(idx, 1); renderSubImagePreview(); };
				subPreview.appendChild(div);
			};
			reader.readAsDataURL(file);
		});
	}
	new Sortable(subPreview, {
		animation: 150,
		onEnd: function(evt) {
			const oldIndex = evt.oldIndex, newIndex = evt.newIndex;
			if (oldIndex !== newIndex) {
				const moved = subFiles.splice(oldIndex, 1)[0];
				subFiles.splice(newIndex, 0, moved);
				renderSubImagePreview();
			}
		}
	});

	// ===== 아이콘 설정 =====
	const iconInput = document.getElementById('product-icon-image');
	const useIconPeriod = document.getElementById('use-icon-period');
	const iconStart = document.getElementById('icon-start');
	const iconEnd = document.getElementById('icon-end');
	useIconPeriod.onchange = () => {
		const enabled = useIconPeriod.checked;
		iconStart.disabled = !enabled;
		iconEnd.disabled = !enabled;
		if (!enabled) { iconStart.value = ''; iconEnd.value = ''; }
	};

	// ===== 관련상품 =====
	const relatedModal = document.getElementById('relatedProductModal');
	const relatedOpenBtn = document.getElementById('open-related-modal-btn');
	const relatedCloseBtns = $$$('.related-modal-close, .related-modal-cancel, .related-modal-overlay', relatedModal);
	const relatedLargeSelect = document.getElementById('related-large-select');
	const relatedMediumSelect = document.getElementById('related-medium-select');
	const relatedSmallSelect = document.getElementById('related-small-select');
	const relatedKeywordInput = document.getElementById('related-product-keyword');
	const relatedProductSearchBtn = document.getElementById('related-product-search-btn');
	const relatedSelectAppendBtn = document.getElementById('related-select-append-btn'); // 신규: 체크 → 임시리스트
	const relatedModalProductList = document.getElementById('related-modal-product-list');
	const relatedRegisterBtn = document.getElementById('related-register-btn');
	const relatedSelectedList = document.getElementById('related-modal-selected-list');
	const relatedListMain = document.getElementById('related-products-list');

	let relatedProductList = []; // 검색 결과
	let relatedCheckedIds = new Set(); // 체크박스 선택
	let relatedProductsTemp = []; // 임시(정렬/삭제)
	let relatedProducts = []; // 최종 등록
	let relatedRegisterType = 'RECIPROCAL'; // 기본값: 상호등록
	const relatedRegisterTypeSelect = document.getElementById('related-register-type');
	if (relatedRegisterTypeSelect) {
		relatedRegisterTypeSelect.onchange = () => {
			relatedRegisterType = relatedRegisterTypeSelect.value || 'RECIPROCAL';
		};
	}

	relatedOpenBtn.onclick = function() {
		openModal(relatedModal);
		relatedRegisterType = 'RECIPROCAL';
		if (relatedRegisterTypeSelect) relatedRegisterTypeSelect.value = 'RECIPROCAL';
		fetchAndRenderLargeOptions(relatedLargeSelect, () => {
			relatedMediumSelect.innerHTML = `<option value="">중분류</option>`;
			relatedSmallSelect.innerHTML = `<option value="">소분류</option>`;
			relatedKeywordInput.value = '';
			relatedProductList = [];
			relatedCheckedIds.clear();
			relatedProductsTemp = []; // 새로 열면 초기화
			renderRelatedModalProductList();
			renderRelatedSelectedList();
		});
	};
	relatedCloseBtns.forEach(btn => btn.onclick = () => closeModal(relatedModal));

	function fetchAndRenderLargeOptions(selectEl, callback) {
		fetch('/api/category/list-large').then(res => res.json()).then(list => {
			selectEl.innerHTML = `<option value="">대분류</option>`;
			list.forEach(cat => { selectEl.innerHTML += `<option value="${cat.id}">${cat.name} (${cat.mediumCount ?? 0})</option>`; });
			if (callback) callback(list);
		});
	}
	function fetchAndRenderMediumOptions(selectEl, largeId, resetSmallSelect) {
		selectEl.innerHTML = `<option value="">중분류</option>`;
		if (!largeId) { if (resetSmallSelect) resetSmallSelect.innerHTML = `<option value="">소분류</option>`; return; }
		fetch(`/api/category/list-medium?largeId=${largeId}`).then(res => res.json()).then(list => {
			list.forEach(m => { selectEl.innerHTML += `<option value="${m.id}">${m.name} (${m.smallCount ?? 0})</option>`; });
			if (resetSmallSelect) resetSmallSelect.innerHTML = `<option value="">소분류</option>`;
		});
	}
	function fetchAndRenderSmallOptions(selectEl, mediumId) {
		selectEl.innerHTML = `<option value="">소분류</option>`;
		if (!mediumId) return;
		fetch(`/api/category/list-small-with-product-count?mediumId=${mediumId}`).then(res => res.json()).then(list => {
			list.forEach(s => { selectEl.innerHTML += `<option value="${s.id}">${s.name} (${s.productCount ?? 0})</option>`; });
		});
	}

	relatedLargeSelect.onchange = function() {
		fetchAndRenderMediumOptions(relatedMediumSelect, this.value, relatedSmallSelect);
		relatedProductList = []; renderRelatedModalProductList();
	};
	relatedMediumSelect.onchange = function() {
		fetchAndRenderSmallOptions(relatedSmallSelect, this.value);
		relatedProductList = []; renderRelatedModalProductList();
	};
	relatedProductSearchBtn.onclick = function() {
		const largeId = relatedLargeSelect.value;
		const mediumId = relatedMediumSelect.value;
		const smallId = relatedSmallSelect.value;
		const keyword = relatedKeywordInput.value.trim();
		let url = `/api/product/list-simple?`;
		if (largeId) url += `largeId=${largeId}&`;
		if (mediumId) url += `mediumId=${mediumId}&`;
		if (smallId) url += `smallId=${smallId}&`;
		if (keyword) url += `keyword=${encodeURIComponent(keyword)}&`;
		fetch(url).then(r => r.json()).then(list => { relatedProductList = list || []; renderRelatedModalProductList(); });
	};

	function renderRelatedModalProductList() {
		relatedModalProductList.innerHTML = '';
		if (!relatedProductList || relatedProductList.length === 0) {
			relatedModalProductList.innerHTML = '<div class="text-muted text-center">제품이 없습니다.</div>';
			return;
		}
		const ul = document.createElement('ul'); ul.className = 'list-group mb-2';
		relatedProductList.forEach(product => {
			const li = document.createElement('li');
			li.className = 'list-group-item d-flex justify-content-between align-items-center';
			const checkbox = document.createElement('input');
			checkbox.type = 'checkbox'; checkbox.className = 'form-check-input me-2';
			checkbox.value = product.id; checkbox.checked = relatedCheckedIds.has(Number(product.id));
			checkbox.onchange = function() { this.checked ? relatedCheckedIds.add(Number(this.value)) : relatedCheckedIds.delete(Number(this.value)); };
			const label = document.createElement('span'); label.textContent = `[${product.code}] ${product.name}`;
			li.appendChild(checkbox); li.appendChild(label); ul.appendChild(li);
		});
		relatedModalProductList.appendChild(ul);
	}

	// 신규: 체크 → 임시리스트 추가
	relatedSelectAppendBtn.onclick = function() {
		const ids = Array.from(relatedCheckedIds);
		ids.forEach(id => {
			const p = relatedProductList.find(x => String(x.id) === String(id));
			if (p && !relatedProductsTemp.some(t => String(t.id) === String(id))) {
				relatedProductsTemp.push({ id: p.id, name: p.name, type: relatedRegisterType });
			}
		});
		renderRelatedSelectedList();
	};

	// 임시 선택 리스트 렌더
	function renderRelatedSelectedList() {
		relatedSelectedList.innerHTML = '';
		if (!relatedProductsTemp || relatedProductsTemp.length === 0) {
			relatedSelectedList.innerHTML = `<div class="text-muted text-center">선택된 관련상품이 없습니다.</div>`;
			return;
		}
		relatedProductsTemp.forEach((p, idx) => {
			const div = document.createElement('div');
			div.className = 'badge bg-info text-white px-2 py-2 me-2 mb-2 d-inline-flex align-items-center';
			const typeLabel = (p.type === 'ONEWAY') ? '일방' : '상호';
			div.innerHTML = `<span>${p.name} <small class="ms-1">(${typeLabel})</small></span>
				<span class="ms-2" style="cursor:pointer;" title="위로">&#8593;</span>
				<span class="ms-1" style="cursor:pointer;" title="아래로">&#8595;</span>
				<span class="ms-1" style="cursor:pointer;" title="삭제">&times;</span>`;
			div.children[1].onclick = function() { if (idx > 0) { [relatedProductsTemp[idx], relatedProductsTemp[idx - 1]] = [relatedProductsTemp[idx - 1], relatedProductsTemp[idx]]; renderRelatedSelectedList(); } };
			div.children[2].onclick = function() { if (idx < relatedProductsTemp.length - 1) { [relatedProductsTemp[idx], relatedProductsTemp[idx + 1]] = [relatedProductsTemp[idx + 1], relatedProductsTemp[idx]]; renderRelatedSelectedList(); } };
			div.children[3].onclick = function() { relatedProductsTemp.splice(idx, 1); renderRelatedSelectedList(); };
			relatedSelectedList.appendChild(div);
		});
	}

	// 최종 등록(임시 → 메인)
	relatedRegisterBtn.onclick = function() {
		relatedProducts = relatedProductsTemp.map((p, i) => ({ id: p.id, name: p.name, sortOrder: i, type: p.type || relatedRegisterType }));
		renderRelatedProductsMain();
		closeModal(relatedModal);
	};

	// 메인 리스트 렌더
	function renderRelatedProductsMain() {
		relatedListMain.innerHTML = '';
		relatedProducts.forEach((p, idx) => {
			const badge = document.createElement('div');
			badge.className = 'badge bg-warning text-dark px-2 py-2 d-flex align-items-center';
			const typeLabel = (p.type === 'ONEWAY') ? '일방' : '상호';
			badge.innerHTML = `${p.name} <span class="ms-2 small">#${idx}</span> <span class="ms-2 small">(${typeLabel})</span><span class="ms-2" style="cursor:pointer;" title="삭제">&times;</span>`;
			badge.lastElementChild.onclick = () => { relatedProducts.splice(idx, 1); renderRelatedProductsMain(); };
			relatedListMain.appendChild(badge);
		});
	}

	// 사용함/안함 토글
	$$$('input[name="useRelatedProducts"]').forEach(r => {
		r.onchange = function() { document.getElementById('open-related-modal-btn').disabled = (this.value === 'false'); };
	});
	$$$('input[name="useBundleItems"]').forEach(r => {
		r.onchange = function() { document.getElementById('open-bundle-modal-btn').disabled = (this.value === 'false'); };
	});

	// ===== 추가구성상품 =====
	const bundleModal = document.getElementById('bundleProductModal');
	const bundleOpenBtn = document.getElementById('open-bundle-modal-btn');
	const bundleCloseBtns = $$$('.bundle-modal-close, .bundle-modal-cancel, .bundle-modal-overlay', bundleModal);
	const bundleLargeSelect = document.getElementById('bundle-large-select');
	const bundleMediumSelect = document.getElementById('bundle-medium-select');
	const bundleSmallSelect = document.getElementById('bundle-small-select');
	const bundleKeywordInput = document.getElementById('bundle-product-keyword');
	const bundleProductSearchBtn = document.getElementById('bundle-product-search-btn');
	const bundleModalProductList = document.getElementById('bundle-modal-product-list');
	const bundleRegisterBtn = document.getElementById('bundle-register-btn');

	let bundleProductList = [];
	let bundleSelectedProductIds = new Set();
	let bundleProducts = [];

	bundleOpenBtn.onclick = function() {
		openModal(bundleModal);
		fetchAndRenderLargeOptions(bundleLargeSelect, () => {
			bundleMediumSelect.innerHTML = `<option value="">중분류</option>`;
			bundleSmallSelect.innerHTML = `<option value="">소분류</option>`;
			bundleKeywordInput.value = '';
			bundleProductList = [];
			bundleSelectedProductIds.clear();
			renderBundleModalProductList();
		});
	};
	bundleCloseBtns.forEach(btn => btn.onclick = () => closeModal(bundleModal));

	bundleLargeSelect.onchange = function() { fetchAndRenderMediumOptions(bundleMediumSelect, this.value, bundleSmallSelect); bundleProductList = []; renderBundleModalProductList(); };
	bundleMediumSelect.onchange = function() { fetchAndRenderSmallOptions(bundleSmallSelect, this.value); bundleProductList = []; renderBundleModalProductList(); };

	bundleProductSearchBtn.onclick = function() {
		const largeId = bundleLargeSelect.value;
		const mediumId = bundleMediumSelect.value;
		const smallId = bundleSmallSelect.value;
		const keyword = bundleKeywordInput.value.trim();
		let url = `/api/product/list-simple?`;
		if (largeId) url += `largeId=${largeId}&`;
		if (mediumId) url += `mediumId=${mediumId}&`;
		if (smallId) url += `smallId=${smallId}&`;
		if (keyword) url += `keyword=${encodeURIComponent(keyword)}&`;
		fetch(url).then(res => res.json()).then(list => { bundleProductList = list || []; renderBundleModalProductList(); });
	};

	function renderBundleModalProductList() {
		bundleModalProductList.innerHTML = '';
		if (!bundleProductList || bundleProductList.length === 0) {
			bundleModalProductList.innerHTML = '<div class="text-muted text-center">제품이 없습니다.</div>';
			return;
		}
		const ul = document.createElement('ul'); ul.className = 'list-group mb-2';
		bundleProductList.forEach(product => {
			const li = document.createElement('li');
			li.className = 'list-group-item d-flex justify-content-between align-items-center';
			const checkbox = document.createElement('input');
			checkbox.type = 'checkbox'; checkbox.className = 'form-check-input me-2';
			checkbox.value = product.id; checkbox.checked = bundleSelectedProductIds.has(Number(product.id));
			checkbox.onchange = function() { this.checked ? bundleSelectedProductIds.add(Number(this.value)) : bundleSelectedProductIds.delete(Number(this.value)); };
			const label = document.createElement('span'); label.textContent = `[${product.code}] ${product.name}`;
			li.appendChild(checkbox); li.appendChild(label); ul.appendChild(li);
		});
		bundleModalProductList.appendChild(ul);
	}

	bundleRegisterBtn.onclick = function() {
		const selectedIds = Array.from(bundleSelectedProductIds);
		selectedIds.forEach(productId => {
			const product = bundleProductList.find(p => String(p.id) === String(productId));
			if (product && !bundleProducts.some(bp => String(bp.id) === String(productId))) {
				bundleProducts.push({ id: product.id, name: product.name });
			}
		});
		renderBundleProductsMain();
		closeModal(bundleModal);
		bundleSelectedProductIds.clear();
	};

	function renderBundleProductsMain() {
		const list = document.getElementById('bundle-products-list');
		list.innerHTML = '';
		bundleProducts.forEach((p, idx) => {
			const badge = document.createElement('div');
			badge.className = 'badge bg-success text-white px-2 py-2 d-flex align-items-center';
			badge.innerHTML = `${p.name} <span class="ms-2 small">#${idx}</span><span class="ms-2" style="cursor:pointer;" title="삭제">&times;</span>`;
			badge.lastElementChild.onclick = () => { bundleProducts.splice(idx, 1); renderBundleProductsMain(); };
			list.appendChild(badge);
		});
	}

	// ===== 가격대체문구 토글 =====
	const usePriceReplacementText = document.getElementById('usePriceReplacementText');
	const priceReplacementArea = document.getElementById('priceReplacementArea');
	usePriceReplacementText.onchange = function() {
		priceReplacementArea.style.display = this.checked ? '' : 'none';
		if (!this.checked) { document.getElementById('priceReplacementText').value = ''; }
	};

	// ===== 제품코드 중복확인 =====
	const checkProductCodeBtn = document.getElementById('checkProductCodeBtn');
	const productCodeInput = document.getElementById('productCode');
	const codeHint = document.getElementById('product-code-check-hint');

	checkProductCodeBtn.onclick = async function() {
		const code = (productCodeInput.value || '').trim();
		if (!code) { alert('제품코드를 입력하세요.'); return; }
		codeHint.textContent = '중복 확인 중...';
		try {
			const res = await fetch(`/api/product/check-code?code=${encodeURIComponent(code)}`);
			if (!res.ok) throw new Error('API 응답 오류');
			const data = await res.json();
			if (data.available) {
				codeHint.textContent = '사용 가능한 코드입니다.';
				codeHint.className = 'form-text text-success';
			} else {
				codeHint.textContent = '이미 사용 중인 코드입니다.';
				codeHint.className = 'form-text text-danger';
			}
		} catch (e) {
			console.warn('[중복확인] API가 준비되지 않았거나 오류가 발생했습니다.', e);
			codeHint.textContent = '중복확인 API 오류/미구현';
			codeHint.className = 'form-text text-warning';
		}
	};

	// == 추가 유틸 ==
	function isNumberOrEmpty(v) { if (v == null || v === '') return true; return !isNaN(v); }
	function inRangeOrEmpty(v, min, max) { if (v == null || v === '') return true; const n = Number(v); return n >= min && n <= max; }

	// == 교체: validateProductForm ==
	async function validateProductForm() {
		// 1) 외부 카테고리: 최소 1개
		if (!selectedCategories || selectedCategories.length === 0) { alert('카테고리를 1개 이상 선택하세요.'); return false; }

		// 2) 기본 필수
		const pName = document.getElementById('productName').value.trim();
		const pCode = document.getElementById('productCode').value.trim();
		const displayStatus = document.querySelector('input[name="displayStatus"]:checked')?.value;
		const saleStatus = document.querySelector('input[name="saleStatus"]:checked')?.value;
		if (!pName) { alert('제품명을 입력하세요.'); return false; }
		if (!pCode) { alert('제품코드를 입력하세요.'); return false; }
		if (!displayStatus) { alert('진열상태를 선택하세요.'); return false; }
		if (!saleStatus) { alert('판매상태를 선택하세요.'); return false; }

		// 3) 대표 이미지 필수
		if (!(mainInput?.files?.length > 0)) { alert('대표 이미지는 필수입니다.'); return false; }

		// 4) 키워드 최소 1개(화면상 * 표시 있음)
		if (!Array.isArray(keywords) || keywords.length === 0) { alert('키워드를 1개 이상 입력하세요.'); return false; }

		// 5) 가격대체문구 체크 시 문구 필수
		if (usePriceReplacementText.checked) {
			const t = (document.getElementById('priceReplacementText').value || '').trim();
			if (!t) { alert('가격대체문구 사용 시 문구를 입력하세요.'); return false; }
		}

		// 6) 공통표시항목(질문/옵션) 필수 항목 확인 (CKEditor 포함)
		let hasQuestionError = false;
		$$$('#product-manager-display-options [name]').forEach(el => {
			const required = el.hasAttribute('required');
			if (el.tagName === 'TEXTAREA' && el.id && el.id.startsWith('editor-question-')) {
				if (required) {
					const editor = ckeInstances[el.id];
					if (!editor || !editor.getData().trim()) { hasQuestionError = true; }
				}
			} else if (el.type === 'file') {
				if (required && !(el.files && el.files.length > 0)) { hasQuestionError = true; }
			} else {
				if (required && !el.value) { hasQuestionError = true; }
			}
		});
		if (hasQuestionError) { alert('필수 공통표시항목(질문/옵션)을 모두 입력하세요.'); return false; }

		// 7) 옵션그룹/옵션
		let hasOptionGroupError = false;
		if (optionGroups.length > 0) {
			optionGroups.forEach((group) => {
				if (!group.name || group.name.trim() === '') { hasOptionGroupError = true; }
				if (!Array.isArray(group.options) || group.options.length === 0) { hasOptionGroupError = true; }
				group.options.forEach((opt) => {
					if (!opt.name || opt.name.trim() === '') { hasOptionGroupError = true; }
					if (!isNumberOrEmpty(opt.extraPrice)) { hasOptionGroupError = true; }
				});
			});
		}
		if (hasOptionGroupError) { alert('옵션그룹을 추가했다면 그룹명과 각 옵션명을 입력하고, 추가금액은 숫자로 입력하세요.'); return false; }

		// 8) 아이콘 기간 체크 시 날짜 필수 + from <= to
		if (useIconPeriod.checked) {
			if (!iconStart.value || !iconEnd.value) { alert('아이콘 기간 사용 시 시작/종료일을 모두 입력하세요.'); return false; }
			if (iconStart.value > iconEnd.value) { alert('아이콘 기간의 시작일이 종료일보다 뒤입니다.'); return false; }
		}

		// 9) 숫자 유효성 (가격/적립률)
		const consumerPrice = document.getElementById('consumerPrice')?.value ?? '';
		const salePrice = document.getElementById('salePrice')?.value ?? '';
		const rewardRate = document.getElementById('rewardRate')?.value ?? '';
		if (!isNumberOrEmpty(consumerPrice) || Number(consumerPrice) < 0) { alert('소비자가는 0 이상 숫자여야 합니다.'); return false; }
		if (!isNumberOrEmpty(salePrice) || Number(salePrice) < 0) { alert('판매가는 0 이상 숫자여야 합니다.'); return false; }
		if (!isNumberOrEmpty(rewardRate) || !inRangeOrEmpty(rewardRate, 0, 100)) { alert('적립률은 0~100 사이 숫자여야 합니다.'); return false; }

		// 10) 유효기간 from<=to
		const validFrom = document.getElementById('validFrom')?.value ?? '';
		const validTo = document.getElementById('validTo')?.value ?? '';
		if (validFrom && validTo && validFrom > validTo) { alert('유효기간 시작일이 종료일보다 뒤입니다.'); return false; }

		// 11) 관련상품/추가구성상품: “사용함”이면 최소 1개
		const useRelated = document.querySelector('input[name="useRelatedProducts"]:checked')?.value === 'true';
		if (useRelated && (!Array.isArray(relatedProducts) || relatedProducts.length === 0)) {
			alert('관련상품을 사용함으로 선택하셨습니다. 최소 1개 이상 등록하세요.'); return false;
		}
		const useBundle = document.querySelector('input[name="useBundleItems"]:checked')?.value === 'true';
		if (useBundle && (!Array.isArray(bundleProducts) || bundleProducts.length === 0)) {
			alert('추가구성상품을 사용함으로 선택하셨습니다. 최소 1개 이상 등록하세요.'); return false;
		}

		// 12) 딜러 추가할인: 입력한 등급은 0~100 숫자
		const dealerKeys = Object.keys(dealerDiscounts || {});
		for (const k of dealerKeys) {
			const v = dealerDiscounts[k];
			if (v === '' || v === null || v === undefined) { alert(`${k} 등급 할인율을 입력하세요.`); return false; }
			if (isNaN(v) || Number(v) < 0 || Number(v) > 100) { alert(`${k} 등급 할인율은 0~100 사이 숫자여야 합니다.`); return false; }
		}

		return true;
	}

	// ===== 디버그 요약(저장 전 콘솔) =====
	function printDebugSummary() {
		console.clear();
		console.group('📦 제품 등록 디버그 요약');

		// 1. 카테고리
		logSection('1) 카테고리(외부)');
		console.log(`선택 개수: ${selectedCategories.length}`);
		selectedCategories.forEach((c, i) => console.log(`#${i} id=${c.id}, path=${c.largeName} > ${c.mediumName} > ${c.smallName}`));
		endSection();

		// 2. 내부 분류
		logSection('2) 자체 분류(내부)');
		console.log(`internalCategorySmallId: ${internalCategorySmallId || '(미선택)'}`);
		endSection();

		// 3. 기본정보
		logSection('3) 기본정보');
		const displayStatus = document.querySelector('input[name="displayStatus"]:checked')?.value;
		const saleStatus = document.querySelector('input[name="saleStatus"]:checked')?.value;
		console.log('제품명:', document.getElementById('productName').value || '(미입력)');
		console.log('제품코드:', document.getElementById('productCode').value || '(미입력)');
		console.log('진열상태:', displayStatus || '(미선택)');
		console.log('판매상태:', saleStatus || '(미선택)');
		endSection();

		// 4. 가격/문구/유효기간/신상 등
		logSection('4) 가격/추가정보');
		console.log('소비자가:', document.getElementById('consumerPrice').value || '(미입력)');
		console.log('판매가:', document.getElementById('salePrice').value || '(미입력)');
		console.log('적립금(%):', document.getElementById('rewardRate').value || '(미입력)');
		console.log('유효기간:', document.getElementById('validFrom').value || '-', '~', document.getElementById('validTo').value || '-');
		console.log('신상상태:', document.getElementById('newState').value);
		console.log('제조사:', document.getElementById('manufacturerText').value || '(없음)');
		console.log('공급사:', document.getElementById('supplierText').value || '(없음)');
		console.log('자체코드:', document.getElementById('internalProductCode').value || '(없음)');
		const priceExposeTarget = document.querySelector('input[name="priceExposeTarget"]:checked')?.value;
		console.log('판매가 노출 대상:', priceExposeTarget);
		const usePriceReplace = document.getElementById('usePriceReplacementText').checked;
		console.log('가격대체문구 사용:', usePriceReplace);
		console.log('가격대체문구:', usePriceReplace ? (document.getElementById('priceReplacementText').value || '(미입력)') : '(미사용)');
		console.log('제조일자:', document.getElementById('manufacturedAt').value || '(없음)');
		console.log('유통기한:', document.getElementById('expiredAt').value || '(없음)');
		endSection();

		// 5. 브랜드
		logSection('5) 브랜드');
		if (selectedBrand) console.log(`선택: id=${selectedBrand.id}, name=${selectedBrand.name}`);
		else console.log('(미선택)');
		endSection();

		// 6. 공통 표시항목
		logSection('6) 공통 표시항목');
		let questionCount = 0;
		$$$('#product-manager-display-options [name]').forEach(el => {
			if (el.type === 'file') {
				if (el.files?.length) { console.log(`${el.name}: 파일 ${el.files.length}개`); questionCount += el.files.length; }
				else console.log(`${el.name}: 파일 없음`);
			} else if (el.tagName === 'TEXTAREA' && el.id.startsWith('editor-question-')) {
				const editor = ckeInstances[el.id]; const filled = !!(editor && editor.getData().trim());
				console.log(`${el.id} (CKEditor): ${filled ? '입력됨' : '미입력'}`);
				questionCount += filled ? 1 : 0;
			} else {
				console.log(`${el.name}: "${el.value}"${el.hasAttribute('required') ? ' (필수)' : ''}`);
				questionCount += el.value ? 1 : 0;
			}
		});
		console.log('총 입력 항목 수:', questionCount);
		endSection();

		// 7. 상세설명
		logSection('7) 상세설명(HTML)');
		if (detailEditor) {
			const html = detailEditor.getData() || '';
			console.log(html.trim() ? `입력됨 (길이: ${html.length})` : '미입력');
		} else {
			console.log('CKEditor 인스턴스 없음');
		}
		endSection();

		// 8. 이미지
		logSection('8) 이미지');
		console.log('대표이미지:', (mainInput.files?.length || 0) ? `${mainInput.files[0].name} (${mainInput.files[0].size}B)` : '(없음)');
		console.log('추가이미지 개수:', subFiles.length);
		subFiles.forEach((f, i) => console.log(`#${i} ${f.name} (${f.size}B)`));
		endSection();

		// 9. 아이콘
		logSection('9) 아이콘');
		console.log('아이콘 파일:', (iconInput.files?.length || 0) ? `${iconInput.files[0].name} (${iconInput.files[0].size}B)` : '(없음)');
		console.log('아이콘 기간 사용:', useIconPeriod.checked);
		console.log('아이콘 기간:', useIconPeriod.checked ? `${iconStart.value || '-'} ~ ${iconEnd.value || '-'}` : '(미사용)');
		endSection();

		// 10. 추가입력필드
		logSection('10) 추가입력필드');
		console.log(`개수: ${extraFields.length}`);
		extraFields.forEach((f, i) => console.log(`#${i} label="${f.label}" value="${f.value}"`));
		endSection();

		// 11. 옵션그룹
		logSection('11) 옵션그룹');
		console.log(`그룹 개수: ${optionGroups.length}`);
		optionGroups.forEach((g, gi) => {
			console.log(`그룹#${gi} name="${g.name}" 옵션수=${g.options.length}`);
			g.options.forEach((o, oi) => console.log(`  - 옵션#${oi} name="${o.name}", value="${o.value}", extraPrice="${o.extraPrice}", sign=${o.sign}, sortOrder=${o.sortOrder}`));
		});
		endSection();

		// 12. 키워드
		logSection('12) 키워드');
		console.log(`개수: ${keywords.length}`);
		keywords.forEach((kw, i) => console.log(`#${i} "${kw}"`));
		endSection();

		// 13. 추가구성상품
		logSection('13) 추가구성상품');
		console.log(`개수: ${bundleProducts.length}`);
		bundleProducts.forEach((p, i) => console.log(`#${i} id=${p.id}, name="${p.name}"`));
		endSection();

		// 14. 관련상품
		logSection('14) 관련상품');
		console.log(`개수: ${relatedProducts.length}`);
		relatedProducts.forEach((p, i) => {
			const tLabel = (p.type === 'ONEWAY') ? '일방' : '상호';
			console.log(`#${i} id=${p.id}, name="${p.name}", sortOrder=${p.sortOrder}, type=${p.type}(${tLabel})`);
		});
		endSection();

		// 15. 할인혜택(다중)
		logSection('15) 할인혜택');
		console.log(`개수: ${selectedDiscounts.length}`);
		selectedDiscounts.forEach((d, i) => console.log(`#${i} id=${d.id}, name="${d.name}", type=${d.type}, term=${d.term}, active=${d.active}`));
		endSection();

		// 16. 딜러 추가할인
		logSection('16) 딜러 추가할인');
		const dealerKeys = Object.keys(dealerDiscounts);
		console.log(`적용 등급 수: ${dealerKeys.length}`);
		dealerKeys.forEach(k => console.log(`${k}: ${dealerDiscounts[k]}%`));
		endSection();

		console.groupEnd();
	}

	// ===== 저장 =====
	document.getElementById('submitProductBtn').addEventListener('click', async function(e) {
		e.preventDefault();

		console.clear();
		printDebugSummary(); // 기존 디버그 요약

		if (!(await validateProductForm())) {
			console.warn('[중단] validateProductForm 실패');
			return;
		}
		if (!confirm('등록하시겠습니까?')) {
			console.log('[중단] 등록 확인 취소');
			return;
		}

		// 제출 중 UI 잠그기
		setSubmitting(true);

		try {
			// === 1) FormData 구성(기존 로직 유지) ===
			const formData = new FormData();

			// 가격 정책
			const priceExposeTarget = document.querySelector('input[name="priceExposeTarget"]:checked')?.value;
			formData.append('priceExposeTarget', priceExposeTarget || 'MEMBER');
			const usePriceReplace = document.getElementById('usePriceReplacementText').checked;
			formData.append('usePriceReplacementText', usePriceReplace);
			const priceReplacementText = document.getElementById('priceReplacementText').value.trim();
			if (usePriceReplace && priceReplacementText) formData.append('priceReplacementText', priceReplacementText);

			// 브랜드
			formData.append('brandId', selectedBrand ? selectedBrand.id : '');

			// 내부 분류
			formData.append('internalCategorySmallId', internalCategorySmallId || '');

			// (외부) 카테고리
			selectedCategories.forEach(cat => formData.append('categorySmallIds[]', cat.id));

			// 공통표시항목(파일/일반값)
			$$('#product-manager-display-options')?.querySelectorAll('[name]')?.forEach(el => {
				if (el.type === 'file') {
					if (el.files?.length) Array.from(el.files).forEach((file) => formData.append(el.name, file));
				} else if (el.tagName === 'TEXTAREA' && el.id.startsWith('editor-question-')) {
					// CKEditor는 아래에서 처리
				} else {
					formData.append(el.name, el.value);
				}
			});

			// === 에디터(질문/상세) HTML 수집 & FormData 포함 ===
			const editorHtmlMap = {}; // { 'question_1': '<p>..</p>', 'detailHtml': '<p>..</p>' }
			for (const [tid, editor] of Object.entries(ckeInstances)) {
				const html = editor.getData() || '';
				const qName = tid.startsWith('editor-') ? tid.replace('editor-', '') : tid; // editor-question-1 -> question-1
				// 서버는 name="question_1" 형식이므로 하이픈 복원
				const normalized = qName.replace('question-', 'question_');
				formData.append(normalized, html);
				editorHtmlMap[normalized] = html;
			}
			if (detailEditor) {
				const html = detailEditor.getData() || '';
				formData.append('detailHtml', html);
				editorHtmlMap['detailHtml'] = html;
			}

			// 기본정보
			formData.append('productName', document.getElementById('productName')?.value ?? '');
			formData.append('productCode', document.getElementById('productCode')?.value ?? '');
			formData.append('displayStatus', document.querySelector('input[name="displayStatus"]:checked')?.value ?? '');
			formData.append('saleStatus', document.querySelector('input[name="saleStatus"]:checked')?.value ?? '');

			// 대표/추가 이미지
			if (mainInput?.files?.length) formData.append('mainImage', mainInput.files[0]);
			if (subFiles?.length) subFiles.forEach(file => formData.append('subImages[]', file));

			// 상세 텍스트들
			formData.append('summaryDescription', document.getElementById('summaryDescription')?.value ?? '');
			formData.append('shortDescription', document.getElementById('shortDescription')?.value ?? '');

			// 수치/날짜
			formData.append('consumerPrice', document.getElementById('consumerPrice')?.value ?? '');
			formData.append('salePrice', document.getElementById('salePrice')?.value ?? '');
			formData.append('rewardRate', document.getElementById('rewardRate')?.value ?? '');
			formData.append('validFrom', document.getElementById('validFrom')?.value ?? '');
			formData.append('validTo', document.getElementById('validTo')?.value ?? '');
			formData.append('newState', document.getElementById('newState')?.value ?? '');
			formData.append('manufacturerText', document.getElementById('manufacturerText')?.value ?? '');
			formData.append('supplierText', document.getElementById('supplierText')?.value ?? '');
			formData.append('internalProductCode', document.getElementById('internalProductCode')?.value ?? '');
			formData.append('manufacturedAt', document.getElementById('manufacturedAt')?.value ?? '');
			formData.append('expiredAt', document.getElementById('expiredAt')?.value ?? '');

			// 아이콘
			if (iconInput?.files?.length) formData.append('iconImage', iconInput.files[0]);
			formData.append('useIconPeriod', useIconPeriod.checked);
			formData.append('iconStartDate', document.getElementById('icon-start')?.value || document.getElementById('iconStart')?.value || iconStart.value || '');
			formData.append('iconEndDate', document.getElementById('icon-end')?.value || document.getElementById('iconEnd')?.value || iconEnd.value || '');

			// 프로모션(다중 선택)
			if (Array.isArray(selectedDiscounts) && selectedDiscounts.length > 0) {
				selectedDiscounts.forEach((d, idx) => {
					formData.append(`discounts[${idx}].id`, d.id ?? '');
					formData.append(`discounts[${idx}].name`, d.name ?? '');
					formData.append(`discounts[${idx}].type`, d.type ?? '');
					formData.append(`discounts[${idx}].term`, d.term ?? '');
					formData.append(`discounts[${idx}].target`, d.target ?? '');
					formData.append(`discounts[${idx}].couponPolicy`, d.couponPolicy ?? '');
					formData.append(`discounts[${idx}].startDate`, d.startDate ?? '');
					formData.append(`discounts[${idx}].endDate`, d.endDate ?? '');
					formData.append(`discounts[${idx}].active`, d.active ?? '');
				});
			} else if (typeof selectedPromotion !== 'undefined' && selectedPromotion && selectedPromotion.id) {
				// 하위호환 (단일 모드)
				formData.append('promotionId', selectedPromotion.id);
			}

			// 키워드
			if (Array.isArray(keywords) && keywords.length > 0) {
				keywords.forEach(k => formData.append('keywords[]', k));
			}

			// 추가입력필드
			if (extraFieldList) {
				const rows = extraFieldList.querySelectorAll('.input-group');
				rows.forEach((row, idx) => {
					const label = row.querySelector(`[name="extraFields[${idx}].label"]`)?.value ?? '';
					const value = row.querySelector(`[name="extraFields[${idx}].value"]`)?.value ?? '';
					formData.append(`extraFields[${idx}].label`, label);
					formData.append(`extraFields[${idx}].value`, value);
				});
			}

			// 옵션그룹/옵션
			if (optionGroupList) {
				const groupCards = optionGroupList.querySelectorAll('.card');
				groupCards.forEach((groupDiv, groupIdx) => {
					const groupName = groupDiv.querySelector(`[name="optionGroups[${groupIdx}].name"]`)?.value || '';
					formData.append(`optionGroups[${groupIdx}].name`, groupName);
					const optionRows = groupDiv.querySelectorAll('.input-group.mb-1');
					optionRows.forEach((row, optIdx) => {
						const name = row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].name"]`)?.value || '';
						const value = row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].value"]`)?.value || '';
						const extraPrice = row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].extraPrice"]`)?.value || '';
						const sign = row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].sign"]`)?.value || '';
						formData.append(`optionGroups[${groupIdx}].options[${optIdx}].name`, name);
						formData.append(`optionGroups[${groupIdx}].options[${optIdx}].value`, value);
						formData.append(`optionGroups[${groupIdx}].options[${optIdx}].extraPrice`, extraPrice);
						formData.append(`optionGroups[${groupIdx}].options[${optIdx}].sign`, sign);
					});
				});
			}

			// 관련상품
			if (Array.isArray(relatedProducts) && relatedProducts.length > 0) {
				relatedProducts.forEach((p, idx) => {
					formData.append(`relatedProducts[${idx}].id`, p.id);
					formData.append(`relatedProducts[${idx}].sortOrder`, String(idx));
					formData.append(`relatedProducts[${idx}].type`, p.type || (document.getElementById('related-register-type')?.value || 'RECIPROCAL'));
				});
			}

			// 추가구성상품
			if (Array.isArray(bundleProducts) && bundleProducts.length > 0) {
				bundleProducts.forEach((p, idx) => {
					formData.append(`bundleProducts[${idx}].id`, p.id);
					formData.append(`bundleProducts[${idx}].sortOrder`, String(idx));
				});
			}

			// 딜러 등급별 추가할인
			if (typeof dealerDiscounts === 'object' && dealerDiscounts) {
				Object.keys(dealerDiscounts).forEach(grade => {
					formData.append(`dealerDiscounts[${grade}]`, dealerDiscounts[grade] ?? '');
				});
			}

			// === 2) 제품 등록 호출 ===
			console.group('🚀 제품 등록 호출');
			const registerRes = await fetch('/api/product/insert', { method: 'POST', body: formData });
			if (!registerRes.ok) {
				let msg = `HTTP ${registerRes.status}`;
				try {
					const data = await registerRes.json();
					msg = data?.message || data?.error || msg;
				} catch { /* ignore */ }
				console.error('등록 실패 응답:', msg);
				alert('등록 실패: ' + msg);
				console.groupEnd();
				setSubmitting(false);
				return;
			}
			const registerJson = await registerRes.json();
			console.log('등록 API 응답:', registerJson);
			console.groupEnd();

			if (!registerJson.success || !registerJson.productId) {
				alert('등록 실패(서버 응답 이상).');
				setSubmitting(false);
				return;
			}

			const productId = registerJson.productId;

			// === 3) 에디터 이미지 이동(상세/질문 CKEditor 전부) ===
			console.group('🖼 에디터 이미지 이동');
			const moveJobs = [];

			// 상세
			if (editorHtmlMap['detailHtml']) {
				const tempList = extractTempImageUrls(editorHtmlMap['detailHtml']);
				if (tempList.length > 0) {
					moveJobs.push({
						type: 'detailHtml',
						key: 'detailHtml',
						html: editorHtmlMap['detailHtml'],
						tempImgList: tempList
					});
				}
			}

			// 질문(CKEditor들)
			Object.keys(editorHtmlMap).forEach(k => {
				if (k === 'detailHtml') return;
				if (!k.startsWith('question_')) return;
				const html = editorHtmlMap[k] || '';
				const tempList = extractTempImageUrls(html);
				if (tempList.length > 0) {
					moveJobs.push({ type: 'question', key: k, html, tempImgList: tempList });
				}
			});

			// [C] 이동 전 로깅/스캔 — 여기 추가!
			console.log('[MOVE_JOBS]', moveJobs);

			if (detailEditor && editorHtmlMap['detailHtml']) {
				debugScanHtml('BEFORE_MOVE', 'detailHtml', editorHtmlMap['detailHtml']);
			}
			Object.keys(editorHtmlMap).forEach(k => {
				if (k !== 'detailHtml' && k.startsWith('question_')) {
					debugScanHtml('BEFORE_MOVE', k, editorHtmlMap[k] || '');
				}
			});

			if (moveJobs.length === 0) {
				console.log('이동할 임시 에디터 이미지가 없습니다. (등록 완료)');
				console.groupEnd();
				alert('제품 등록이 완료되었습니다.');
				setSubmitting(false);
				// 필요 시 이동
				// location.href = `/admin/productDetail/${productId}`;
				return;
			}

			// 순차 실행(치환 순서 보장)
			const moveResults = [];
			for (const job of moveJobs) {
				try {
					// [SEND] 보낼 임시 URL 목록
					console.group(`MOVE → ${job.key}`);
					console.table(job.tempImgList.map((u, i) => ({ idx: i, tempUrl: u })));

					// 서버 호출
					const res = await moveEditorImagesAPI(productId, job);

					// 응답 로깅(문자열/객체 어떤 형태든 미리 확인)
					console.log('[MOVE_RESP_RAW]', job.key, { type: typeof res });
					const newHtml = (typeof res === 'string') ? res : (res?.newHtml ?? job.html);
					console.log('[MOVE_RESP_PREVIEW]', (newHtml || '').slice(0, 200) + '...');

					// [RECV] 받은 HTML에서 /upload/product/ URL만 추출해서 표로 출력
					const productUrls = extractProductImageUrls(newHtml);
					console.table(productUrls.map((u, i) => ({ idx: i, productUrl: u })));

					moveResults.push({ ok: true, key: job.key, res });

					// 이동 후 HTML 내부에 남은 문제 패턴 체크
					debugScanHtml?.('AFTER_MOVE', job.key, newHtml);

					// 에디터 반영
					if (job.key === 'detailHtml' && typeof detailEditor?.setData === 'function') {
						detailEditor.setData(newHtml);
					} else if (job.key.startsWith('question_')) {
						const editorId = 'editor-' + job.key.replace('question_', 'question-'); // editor-question-123
						const inst = ckeInstances[editorId];
						if (inst?.setData) {
							inst.setData(newHtml);
						} else {
							const ta = document.getElementById(editorId);
							if (ta) ta.value = newHtml;
							console.warn('[CKE 폴백] 인스턴스 미발견 → textarea에 값만 주입:', editorId);
						}
					}

					console.groupEnd();
				} catch (err) {
					console.error('이동 실패 ←', job.key, err);
					moveResults.push({ ok: false, key: job.key, error: err });
					console.groupEnd();
				}
			}

			console.groupEnd();

			// === 4) 결과 안내 ===
			const failed = moveResults.filter(x => !x.ok);
			if (failed.length > 0) {
				alert(
					`제품은 저장되었습니다만, 일부 에디터 이미지 이동에 실패했습니다.\n` +
					failed.map(f => `- ${f.key}: ${f.error?.message || '오류'}`).join('\n')
				);
			} else {
				alert('제품 등록 및 에디터 이미지 저장이 모두 완료되었습니다.');
			}

			// 필요 시 이동
			location.href = '/productManager';
		} catch (err) {
			console.error('[등록 중 오류]', err);
			alert('등록 중 오류: ' + (err?.message || err));
		} finally {
			setSubmitting(false);
		}
	});

});
