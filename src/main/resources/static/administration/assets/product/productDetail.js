// /administration/assets/product/productDetail.js
document.addEventListener('DOMContentLoaded', async function() {
	// ========= 공통 유틸 =========
	const $$ = (sel, root = document) => root.querySelector(sel);
	const $$$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));
	const byId = (id) => document.getElementById(id);
	const setVal = (sel, v) => { const el = document.querySelector(sel); if (el) el.value = v ?? ''; };
	const logSection = (title) => { console.group(`🧩 ${title}`); };
	const endSection = () => { console.groupEnd(); };
	const deepClone = (obj) => JSON.parse(JSON.stringify(obj));

	// URL에서 productId 추출 (예: /administration/product/product/productDetail/123)
	const path = window.location.pathname;
	const idMatch = path.match(/\/productDetail\/(\d+)/);
	const productId = idMatch ? idMatch[1] : null;
	if (!productId) { alert('잘못된 접근입니다. (제품 ID 없음)'); return; }
	const DETAIL_API = `/api/product/${productId}/detail`;

	// ===== Insert 화면과 동일한 상태 변수들 =====
	let selectedCategories = [];   // 외부 카테고리
	let internalCategorySmallId = ''; // 내부 소분류 id
	let selectedBrand = null;     // 브랜드
	let keywords = [];            // 키워드
	let optionGroups = [];        // 옵션 그룹
	let relatedProducts = [];     // 관련상품
	let bundleProducts = [];      // 추가구성상품
	let selectedDiscounts = [];   // 프로모션
	let dealerDiscounts = {};     // 딜러 등급 할인
	let extraFields = [];         // 추가입력필드

	// ===== 이미지(대표/추가) =====
	const mainInput = byId('product-manager-main-image');
	const mainPreview = byId('product-manager-main-image-preview');
	const subInput = byId('product-manager-sub-image');
	const subPreview = byId('product-manager-sub-image-preview');
	let subFiles = []; // 새로 추가한 추가이미지(미리보기/정렬용)

	// 아이콘 이미지 
	const iconInput = byId('product-icon-image');
	const iconPreview = byId('product-icon-image-preview'); // 없으면 자동 생성합니다.
	let iconImageAction = 'KEEP';      // KEEP | DELETE | REPLACE
	let iconServerUrl = '';            // 서버에 있던 아이콘 URL

	// ===== CKEditor 인스턴스 관리 =====
	const ckeInstances = {}; // 질문용
	let detailEditor = null; // 상세설명

	// ===== 에디터 업로드 어댑터 (insert와 동일) =====
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

	// ===== 상태: 원본 데이터/원본 정규화/변경 추적 =====
	let __originalDTO__ = null;         // DETAIL_API 응답 원본
	let __originalState__ = null;       // 비교용 정규화 상태
	let __currentState__ = null;        // 현재 DOM에서 수집한 정규화 상태

	// === 이미지/파일 변경 플래그 ===
	// 대표이미지: KEEP | DELETE | REPLACE
	let mainImageAction = 'KEEP';
	// 서버에 존재하던 추가이미지 URL들
	let serverSubImageUrls = [];
	// 유저가 삭제한 추가이미지 URL 모음
	const deletedSubImageUrls = new Set(); // 삭제 요청된 서버 URL
	// 파일형 공통표시항목: 질문ID -> { action: 'KEEP'|'DELETE'|'REPLACE', serverFiles: string[] }
	const fileQuestionActions = {};

	// ====== 공통 표시항목 영역 ======
	const displayContainer = byId('product-manager-display-options');

	// ====== 키워드 ======
	const keywordInput = byId('product-keyword-input');
	const addKeywordBtn = byId('add-keyword-btn');
	const keywordList = byId('product-keyword-list');
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
	if (addKeywordBtn) addKeywordBtn.onclick = addKeyword;
	if (keywordInput) keywordInput.onkeydown = (e) => { if (e.key === 'Enter') { addKeyword(); e.preventDefault(); } };

	// ====== (외부) 카테고리 리스트(대/중/소) + 선택 배지 ======
	const largeList = byId('category-large-list');
	const mediumList = byId('category-medium-list');
	const smallList = byId('category-small-list');
	const selectedList = byId('selected-category-list');
	let largeCategoryMap = {};
	let mediumCategoryMap = {};
	[largeList, mediumList, smallList].forEach(el => { if (el) { el.style.maxHeight = '240px'; el.style.overflowY = 'auto'; } });

	function renderSelectedCategories() {
		selectedList.innerHTML = '';
		selectedCategories.forEach((c, idx) => {
			const div = document.createElement('div');
			div.className = 'badge bg-primary text-white px-2 py-2 me-2 d-flex align-items-center';
			div.innerHTML = `${c.largeName} &gt; ${c.mediumName} &gt; <b>${c.smallName}</b>
        <span class="ms-2" style="cursor:pointer;" title="삭제">[삭제]</span>`;
			div.querySelector('span').onclick = () => { selectedCategories.splice(idx, 1); renderSelectedCategories(); };
			selectedList.appendChild(div);
		});
	}

	async function loadLargeCategories() {
		if (!largeList) return;
		const res = await fetch('/api/category/list-large');
		const list = await res.json();
		largeList.innerHTML = '';
		list.forEach(large => {
			const li = document.createElement('li');
			li.className = 'list-group-item list-group-item-action category-large-item d-flex justify-content-between align-items-center';
			li.dataset.id = large.id;
			li.innerHTML = `<span>${large.name}</span><span class="badge bg-light text-dark ms-2" data-large-badge="${large.id}">${large.mediumCount ?? 0}</span>`;
			largeCategoryMap[large.id] = large.name;
			largeList.appendChild(li);
		});
	}
	if (largeList) {
		await loadLargeCategories();
		largeList.addEventListener('click', async (e) => {
			const li = e.target.closest('.category-large-item'); if (!li) return;
			const largeId = li.dataset.id;
			const res = await fetch(`/api/category/list-medium?largeId=${largeId}`);
			const list = await res.json();
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
	}
	if (mediumList) {
		mediumList.addEventListener('click', async (e) => {
			const li = e.target.closest('.category-medium-item'); if (!li) return;
			const mediumId = li.dataset.id;
			const res = await fetch(`/api/category/list-small?mediumId=${mediumId}`);
			const list = await res.json();
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
	}
	if (smallList) {
		smallList.addEventListener('click', (e) => {
			const li = e.target.closest('.category-small-item'); if (!li) return;
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
	}

	// ========= 차이 출력 유틸 =========
	function __isPlainObject__(v) {
		return v !== null && typeof v === 'object' && !Array.isArray(v);
	}
	function __previewValue__(val, limit = 140) {
		if (val === undefined) return '␀ undefined';
		if (val === null) return '␀ null';
		if (typeof val === 'string') {
			const txt = val.replace(/\s+/g, ' ');
			return txt.length > limit ? `${txt.slice(0, limit)}… [len=${txt.length}]` : txt;
		}
		if (typeof val === 'number' || typeof val === 'boolean') return String(val);
		try {
			const json = JSON.stringify(val);
			return json.length > limit ? `${json.slice(0, limit)}… [len=${json.length}]` : json;
		} catch {
			return Object.prototype.toString.call(val);
		}
	}

	/**
	 * 깊은 비교로 변경 항목만 수집
	 * @returns Array<{ path: string, change: 'added'|'removed'|'changed', before: any, after: any }>
	 */
	function __deepDiff__(a, b, basePath = '') {
		// 둘 다 같은(===) 값이면 변경 없음
		if (a === b) return [];

		// 한쪽만 null/undefined
		if (a == null && b != null) return [{ path: basePath || '(root)', change: 'added', before: a, after: b }];
		if (a != null && b == null) return [{ path: basePath || '(root)', change: 'removed', before: a, after: b }];

		// 배열
		if (Array.isArray(a) && Array.isArray(b)) {
			const max = Math.max(a.length, b.length);
			const out = [];
			for (let i = 0; i < max; i++) {
				const p = `${basePath}[${i}]`;
				if (i >= a.length) out.push({ path: p, change: 'added', before: undefined, after: b[i] });
				else if (i >= b.length) out.push({ path: p, change: 'removed', before: a[i], after: undefined });
				else out.push(...__deepDiff__(a[i], b[i], p));
			}
			return out;
		}

		// 객체
		if (__isPlainObject__(a) && __isPlainObject__(b)) {
			const out = [];
			const keys = new Set([...Object.keys(a), ...Object.keys(b)]);
			keys.forEach((k) => {
				const p = basePath ? `${basePath}.${k}` : k;
				if (!(k in b)) out.push({ path: p, change: 'removed', before: a[k], after: undefined });
				else if (!(k in a)) out.push({ path: p, change: 'added', before: undefined, after: b[k] });
				else out.push(...__deepDiff__(a[k], b[k], p));
			});
			return out;
		}

		// 원시/타입이 다르거나 값이 다른 경우
		return [{ path: basePath || '(root)', change: 'changed', before: a, after: b }];
	}

	/** 콘솔에 표로 출력 */
	function __printDiffTable__(title, orig, now, opts = {}) {
		const { collapse = false } = opts;
		const diffs = __deepDiff__(orig, now, '');
		const groupFn = collapse ? console.groupCollapsed : console.group;
		groupFn(`🔎 ${title}${diffs.length ? ` — ${diffs.length} change(s)` : ' — 변경 없음'}`);
		if (!diffs.length) {
			console.log('변경 없음');
			console.groupEnd();
			return;
		}
		const rows = diffs.map(d => ({
			경로: d.path,
			변경: d.change,
			이전: __previewValue__(d.before),
			이후: __previewValue__(d.after),
		}));
		console.table(rows);
		console.groupEnd();
	}

	/** 파일형 질문에 대한 변경 표 */
	function __printFileQuestionDiff__(title, fileQuestionNow, originalAnswers) {
		const rows = Object.entries(fileQuestionNow || {}).map(([qid, v]) => {
			const origCnt = (originalAnswers?.[qid]?.fileUrls || []).length;
			return {
				질문ID: qid,
				액션: v.action,
				'기존파일수(keep)': v.keepCount ?? origCnt,
				'신규선택수(new)': v.newFileCount ?? 0,
				해석:
					v.action === 'DELETE' && (v.newFileCount ?? 0) === 0 ? '기존 삭제'
						: v.action === 'REPLACE' && (v.newFileCount ?? 0) > 0 ? '교체'
							: v.action === 'KEEP' ? '유지'
								: '—'
			};
		});
		const groupFn = console.group;
		groupFn(`📎 ${title}${rows.length ? ` — ${rows.length} item(s)` : ''}`);
		if (!rows.length) console.log('파일형 질문 변경 없음');
		else console.table(rows);
		console.groupEnd();
	}


	// ====== 내부 카테고리(자체) 셀렉트 ======
	const internalLarge = byId('internal-large-select');
	const internalMedium = byId('internal-medium-select');
	const internalSmall = byId('internal-small-select');

	// insert 화면과 동일한 상태 변수 사용 (상단에 이미 선언되어 있음)
	// let internalCategorySmallId = ''; // ← 상단 공용 상태 변수 사용

	/** 내부 대분류 로드 */
	function fetchInternalLarge() {
		if (!internalLarge) return Promise.resolve();
		return fetch('/api/internal-category/list-large')
			.then(r => r.json())
			.then(list => {
				internalLarge.innerHTML = `<option value="">내부 대분류</option>`;
				list.forEach(x => {
					internalLarge.innerHTML += `<option value="${x.id}">${x.name} (${x.mediumCount ?? 0})</option>`;
				});
				if (internalMedium) internalMedium.innerHTML = `<option value="">내부 중분류</option>`;
				if (internalSmall) internalSmall.innerHTML = `<option value="">내부 소분류</option>`;
			})
			.catch(() => console.warn('[내부분류] API 미구현 (list-large)'));
	}

	/** 내부 중분류 로드 */
	function fetchInternalMedium(largeId) {
		if (!internalMedium || !internalSmall) return Promise.resolve();
		internalMedium.innerHTML = `<option value="">내부 중분류</option>`;
		internalSmall.innerHTML = `<option value="">내부 소분류</option>`;
		if (!largeId) return Promise.resolve();
		return fetch(`/api/internal-category/list-medium?largeId=${largeId}`)
			.then(r => r.json())
			.then(list => {
				list.forEach(x => {
					internalMedium.innerHTML += `<option value="${x.id}">${x.name} (${x.smallCount ?? 0})</option>`;
				});
			})
			.catch(() => console.warn('[내부분류] API 미구현 (list-medium)'));
	}

	/** 내부 소분류 로드 */
	function fetchInternalSmall(mediumId) {
		if (!internalSmall) return Promise.resolve();
		internalSmall.innerHTML = `<option value="">내부 소분류</option>`;
		if (!mediumId) return Promise.resolve();
		return fetch(`/api/internal-category/list-small?mediumId=${mediumId}`)
			.then(r => r.json())
			.then(list => {
				list.forEach(x => {
					internalSmall.innerHTML += `<option value="${x.id}">${x.name} (${x.productCount ?? 0})</option>`;
				});
			})
			.catch(() => console.warn('[내부분류] API 미구현 (list-small)'));
	}

	/**
	 * 내부 카테고리 경로(대/중/소)를 순차 로딩 후 선택
	 * - largeId, mediumId, smallId는 문자열/숫자 모두 허용
	 */
	async function selectInternalCategoryPath(largeId, mediumId, smallId) {
		if (!internalLarge || !internalMedium || !internalSmall) return;

		// 1) 대분류 옵션 보장 후 선택
		await fetchInternalLarge();
		if (largeId) {
			internalLarge.value = String(largeId);
			await fetchInternalMedium(internalLarge.value);
		} else {
			internalLarge.value = '';
			internalMedium.innerHTML = `<option value="">내부 중분류</option>`;
			internalSmall.innerHTML = `<option value="">내부 소분류</option>`;
			internalCategorySmallId = '';
			return;
		}

		// 2) 중분류 선택 후 소분류 로드
		if (mediumId) {
			internalMedium.value = String(mediumId);
			await fetchInternalSmall(internalMedium.value);
		} else {
			internalMedium.value = '';
			internalSmall.innerHTML = `<option value="">내부 소분류</option>`;
			internalCategorySmallId = '';
			return;
		}

		// 3) 소분류 선택
		if (smallId) {
			internalSmall.value = String(smallId);
			internalCategorySmallId = String(smallId);
		} else {
			internalSmall.value = '';
			internalCategorySmallId = '';
		}
	}

	/**
	 * 상세 DTO를 받아 내부 카테고리 자동 선택 수행
	 * - 상세 로딩 직후(d를 받은 바로 다음)에 호출하세요.
	 *   예) await applyInternalCategoryFromDTO(d);
	 */
	async function applyInternalCategoryFromDTO(d) {
		const L = d?.internalCategoryLargeId ? String(d.internalCategoryLargeId) : '';
		const M = d?.internalCategoryMediumId ? String(d.internalCategoryMediumId) : '';
		const S = d?.internalCategorySmallId ? String(d.internalCategorySmallId) : '';
		await selectInternalCategoryPath(L, M, S);

		// 선택 안내 텍스트(선택 사항)
		if (S) {
			const helper = document.createElement('div');
			helper.className = 'form-text';
			helper.textContent = `현재 내부 카테고리: L=${L || '-'} / M=${M || '-'} / S=${S || '-'}`;
			byId('internal-small-select')?.insertAdjacentElement('afterend', helper);
		}
	}

	// 변경 시 연쇄 로딩(기존 UX 유지)
	if (internalLarge) internalLarge.onchange = () => fetchInternalMedium(internalLarge.value);
	if (internalMedium) internalMedium.onchange = () => fetchInternalSmall(internalMedium.value);
	if (internalSmall) internalSmall.onchange = () => { internalCategorySmallId = internalSmall.value || ''; };

	// 초기 로드(빈 화면 진입 시 옵션만 채움; 실제 선택은 DTO 적용 시점에서 처리)
	fetchInternalLarge();
	// ====== 브랜드 검색 ======
	const brandSearchInput = byId('brand-search-input');
	const brandSearchBtn = byId('brand-search-btn');
	const brandSearchResult = byId('brand-search-result');
	const brandSelectedArea = byId('brand-selected-area');

	function renderSelectedBrand() {
		brandSelectedArea.innerHTML = '';
		if (!selectedBrand) return;
		const imgUrl = selectedBrand.imageUrl || selectedBrand.imageRoad || '/assets/brand-default.png';
		brandSelectedArea.innerHTML = `
      <div class="d-flex align-items-center bg-light p-2 rounded">
        <img src="${imgUrl}" style="width:40px;height:40px;object-fit:cover;border-radius:6px;">
        <span class="ms-2">${selectedBrand.name}</span>
        <button type="button" class="btn btn-outline-danger btn-sm ms-2" id="remove-brand-btn">삭제</button>
      </div>`;
		const rm = byId('remove-brand-btn');
		if (rm) rm.onclick = () => { selectedBrand = null; renderSelectedBrand(); };
	}
	if (brandSearchBtn) {
		brandSearchBtn.onclick = function() {
			const kw = (brandSearchInput?.value || '').trim();
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
						const imgUrl = b.imageRoad || b.imageUrl || '/assets/brand-default.png';
						div.innerHTML = `<img src="${imgUrl}" alt="브랜드" style="width:32px;height:32px;object-fit:cover;border-radius:6px;">
              <span>${b.name}</span>
              <button type="button" class="btn btn-outline-primary btn-sm ms-auto">선택</button>`;
						div.querySelector('button').onclick = () => { selectedBrand = b; renderSelectedBrand(); };
						brandSearchResult.appendChild(div);
					});
				});
		};
	}

	// ====== 옵션 그룹 렌더 ======
	const optionGroupList = byId('product-manager-option-group-list');
	function renderOptionGroups() {
		optionGroupList.innerHTML = '';
		optionGroups.forEach((group, groupIdx) => {
			const groupDiv = document.createElement('div');
			groupDiv.className = 'card mb-2';
			groupDiv.innerHTML = `
        <div class="card-body p-2">
          <div class="input-group mb-2">
            <input type="text" class="form-control form-control-sm" name="optionGroups[${groupIdx}].name"
                   placeholder="옵션 그룹명" value="${group.name || ''}" required>
            <button type="button" class="btn btn-outline-danger btn-sm" title="옵션그룹 삭제">×</button>
          </div>
          <div id="option-group-options-${groupIdx}"></div>
          <button type="button" class="btn btn-outline-primary btn-sm mt-1" data-group-idx="${groupIdx}">+ 옵션 추가</button>
        </div>`;
			// 삭제
			groupDiv.querySelector('.btn-outline-danger').onclick = () => {
				optionGroups.splice(groupIdx, 1);
				renderOptionGroups();
			};
			// 옵션 추가
			groupDiv.querySelector('.btn-outline-primary').onclick = () => {
				group.options = group.options || [];
				group.options.push({ name: '', value: '', extraPrice: '', sign: 'PLUS', sortOrder: (group.options.length + 1) });
				renderOptionGroups();
			};

			const optionsContainer = groupDiv.querySelector(`#option-group-options-${groupIdx}`);
			(group.options || []).forEach((opt, optIdx) => {
				const optRow = document.createElement('div');
				optRow.className = 'input-group mb-1';
				optRow.innerHTML = `
          <input type="text" class="form-control form-control-sm" name="optionGroups[${groupIdx}].options[${optIdx}].name"
                 placeholder="옵션명" value="${opt.name || ''}" required>
          <input type="text" class="form-control form-control-sm" name="optionGroups[${groupIdx}].options[${optIdx}].value"
                 placeholder="값" value="${opt.value || ''}">
          <input type="number" class="form-control form-control-sm" name="optionGroups[${groupIdx}].options[${optIdx}].extraPrice"
                 placeholder="추가금액" value="${opt.extraPrice ?? ''}">
          <select class="form-select form-select-sm" name="optionGroups[${groupIdx}].options[${optIdx}].sign">
            <option value="PLUS"  ${opt.sign === 'PLUS' ? 'selected' : ''}>+</option>
            <option value="MINUS" ${opt.sign === 'MINUS' ? 'selected' : ''}>-</option>
          </select>
          <button type="button" class="btn btn-outline-danger btn-sm" title="옵션 삭제">×</button>`;
				optRow.querySelector('.btn-outline-danger').onclick = () => {
					group.options.splice(optIdx, 1);
					renderOptionGroups();
				};
				optionsContainer.appendChild(optRow);
			});

			optionGroupList.appendChild(groupDiv);
		});
	}
	const addOptionGroupBtn = byId('product-manager-add-option-group');
	if (addOptionGroupBtn) {
		addOptionGroupBtn.onclick = () => {
			optionGroups.push({ name: '', options: [] });
			renderOptionGroups();
		};
	}

	// ====== 관련/번들/프로모션/딜러할인/추가필드 메인 렌더 ======
	function renderRelatedProductsMain() {
		const list = byId('related-products-list');
		list.innerHTML = '';
		relatedProducts.forEach((p, idx) => {
			const badge = document.createElement('div');
			badge.className = 'badge bg-warning text-dark px-2 py-2 d-flex align-items-center';
			const typeLabel = p.type === 'ONEWAY' ? '일방' : '상호';
			badge.innerHTML = `${p.name} <span class="ms-2 small">#${idx}</span> <span class="ms-2 small">(${typeLabel})</span>
                         <span class="ms-2" style="cursor:pointer;" title="삭제">&times;</span>`;
			badge.lastElementChild.onclick = () => { relatedProducts.splice(idx, 1); renderRelatedProductsMain(); };
			list.appendChild(badge);
		});
	}
	function renderBundleProductsMain() {
		const list = byId('bundle-products-list');
		list.innerHTML = '';
		bundleProducts.forEach((p, idx) => {
			const badge = document.createElement('div');
			badge.className = 'badge bg-success text-white px-2 py-2 d-flex align-items-center';
			badge.innerHTML = `${p.name} <span class="ms-2 small">#${idx}</span>
                         <span class="ms-2" style="cursor:pointer;" title="삭제">&times;</span>`;
			badge.lastElementChild.onclick = () => { bundleProducts.splice(idx, 1); renderBundleProductsMain(); };
			list.appendChild(badge);
		});
	}
	function renderSelectedDiscountsMain() {
		const wrap = byId('selected-discount-list');
		wrap.innerHTML = '';
		selectedDiscounts.forEach((d, idx) => {
			const badge = document.createElement('div');
			badge.className = 'badge bg-danger text-white px-2 py-2 d-flex align-items-center';
			badge.innerHTML = `<span>${d.name} (${d.typeLabel || d.type})</span><span class="ms-2" style="cursor:pointer;" title="삭제">&times;</span>`;
			badge.lastElementChild.onclick = () => { selectedDiscounts.splice(idx, 1); renderSelectedDiscountsMain(); };
			wrap.appendChild(badge);
		});
	}

	function renderDealerDiscounts() {
		const container = byId('dealer-discount-list');
		const buttons = byId('dealer-discount-buttons');
		if (!container || !buttons) return;

		container.innerHTML = '';
		$$$('#dealer-discount-buttons button').forEach((b) => (b.disabled = false));
		Object.keys(dealerDiscounts).forEach((grade) => {
			const col = document.createElement('div');
			col.className = 'col-12';
			col.innerHTML = `
        <div class="input-group input-group-sm align-items-center">
          <span class="input-group-text" style="min-width:120px;">${grade} 등급 딜러 추가 할인율</span>
          <input type="number" min="0" max="100" class="form-control" style="max-width:120px;" placeholder="0"
                 value="${dealerDiscounts[grade] ?? ''}" data-grade="${grade}" />
          <span class="input-group-text">%</span>
          <button type="button" class="btn btn-outline-danger btn-sm" data-grade="${grade}" title="삭제">×</button>
        </div>`;
			col.querySelector('input').oninput = function() { dealerDiscounts[grade] = this.value; };

			// CSS.escape로 안전한 셀렉터
			const safeGrade = (typeof CSS !== 'undefined' && CSS.escape) ? CSS.escape(String(grade)) : String(grade).replace(/"/g, '\\"');

			col.querySelector('button').onclick = function() {
				delete dealerDiscounts[grade];
				const btn = buttons.querySelector('button[data-grade="' + safeGrade + '"]');
				if (btn) btn.disabled = false;
				renderDealerDiscounts();
			};
			container.appendChild(col);

			const btn = buttons.querySelector('button[data-grade="' + safeGrade + '"]');
			if (btn) btn.disabled = true;
		});
	}
	const dealerDiscountButtons = byId('dealer-discount-buttons');
	if (dealerDiscountButtons) {
		dealerDiscountButtons.querySelectorAll('button').forEach(btn => {
			btn.onclick = function() {
				const grade = btn.dataset.grade;
				if (dealerDiscounts[grade]) return;
				dealerDiscounts[grade] = '';
				renderDealerDiscounts();
				btn.disabled = true;
			};
		});
	}

	// ====== 아이콘 이미지(서버 저장본) 미리보기 + 변경추적 ======
	function ensureIconPreviewContainer() {
		if (!iconPreview) {
			// 없으면 product-icon-image 인풋 뒤에 프리뷰 컨테이너 생성
			const input = iconInput || byId('product-icon-image');
			if (input) {
				const wrap = document.createElement('div');
				wrap.id = 'product-icon-image-preview';
				wrap.className = 'mt-2 d-flex align-items-center gap-2';
				input.insertAdjacentElement('afterend', wrap);
				return wrap;
			}
		}
		return iconPreview;
	}

	/**
	 * 서버에 저장되어 있던 아이콘을 미리보기로 그립니다.
	 * - 삭제 클릭 시: action=DELETE, input 비우기
	 * - 새 파일 선택 시: action=REPLACE 로 전환
	 */
	function renderServerIconImage(iconDTO) {
		const preview = ensureIconPreviewContainer();
		if (!preview) return;

		preview.innerHTML = '';
		iconServerUrl = iconDTO?.imageUrl || '';

		if (iconServerUrl) {
			const div = document.createElement('div');
			div.className = 'position-relative';
			div.style.width = '64px';
			div.style.height = '64px';
			div.innerHTML = `
      <img src="${iconServerUrl}" style="width:64px;height:64px;object-fit:cover;border-radius:6px;border:1px solid #eee;">
      <button class="btn-close btn-sm" style="position:absolute;top:-8px;right:-8px;" aria-label="Remove"></button>
    `;
			// 삭제 버튼 → DELETE (새 파일 없으면 실제 삭제)
			div.querySelector('button').onclick = () => {
				iconImageAction = 'DELETE';
				if (iconInput) iconInput.value = '';
				preview.innerHTML = `<div class="text-muted small">아이콘: (삭제 예정)</div>`;
			};
			preview.appendChild(div);
		} else {
			preview.innerHTML = `<div class="text-muted small">아이콘: (없음)</div>`;
		}

		// 파일 선택 이벤트 (REPLACE/KEEP 토글)
		if (iconInput) {
			iconInput.addEventListener('change', function() {
				const hasNew = this.files && this.files.length > 0;
				if (hasNew) {
					iconImageAction = 'REPLACE';
					// 새 파일 미리보기
					const reader = new FileReader();
					reader.onload = e => {
						const p = ensureIconPreviewContainer();
						if (!p) return;
						p.innerHTML = `
            <div class="position-relative" title="새 아이콘(교체)">
              <img src="${e.target.result}" style="width:64px;height:64px;object-fit:cover;border-radius:6px;border:1px solid #eee;">
              <button class="btn-close btn-sm" style="position:absolute;top:-8px;right:-8px;" aria-label="Remove"></button>
            </div>
            <div class="form-text">기존 아이콘은 교체됩니다.</div>
          `;
						p.querySelector('button').onclick = () => {
							iconInput.value = '';
							// 새 선택 취소 → 기존이 있으면 KEEP, 기존도 삭제 버튼 눌렀다면 DELETE 유지
							iconImageAction = iconServerUrl ? 'KEEP' : 'KEEP';
							renderServerIconImage({ imageUrl: iconServerUrl });
						};
					};
					reader.readAsDataURL(this.files[0]);
				} else {
					// 파일 선택 취소 → 기존 상태로 회귀 (DELETE가 이미 눌린 상태라면 유지)
					if (iconImageAction === 'REPLACE') {
						iconImageAction = iconServerUrl ? 'KEEP' : 'KEEP';
					}
					renderServerIconImage({ imageUrl: iconServerUrl });
				}
			}, { once: false });
		}
	}

	// ====== 이미지 미리보기(서버 저장본) 초기 렌더 + 변경추적 ======
	function renderServerImages(imagesDTO) {
		// 대표이미지
		mainPreview.innerHTML = '';
		if (imagesDTO?.mainImageUrl) {
			const wrap = document.createElement('div');
			wrap.className = 'product-insert-repimage-wrapper';
			const div = document.createElement('div');
			div.className = 'product-insert-repimage-thumb';
			div.innerHTML = `
        <img src="${imagesDTO.mainImageUrl}" alt="대표이미지">
        <button class="btn-close btn-sm" aria-label="Remove"></button>`;
			// 삭제 클릭 → 대표이미지 삭제 의도 표시
			div.querySelector('button').onclick = () => {
				mainPreview.innerHTML = '';
				mainImageAction = 'DELETE';
				if (mainInput) mainInput.value = '';
			};
			wrap.appendChild(div);
			mainPreview.appendChild(wrap);
		}
		// 추가이미지
		subPreview.innerHTML = '';
		serverSubImageUrls = Array.isArray(imagesDTO?.subImageUrls) ? imagesDTO.subImageUrls.slice() : [];
		deletedSubImageUrls.clear();
		serverSubImageUrls.forEach((url) => {
			const div = document.createElement('div');
			div.className = 'image-preview-thumb position-relative';
			div.style.width = '100px'; div.style.height = '100px'; div.style.marginRight = '8px';
			div.innerHTML = `<img src="${url}" style="width:100%;height:100%;object-fit:cover;">
                       <button class="btn-close btn-sm" style="position:absolute;top:0;right:0;z-index:2;" aria-label="Remove"></button>`;
			div.querySelector('button').onclick = () => {
				deletedSubImageUrls.add(url);
				div.remove();
			};
			subPreview.appendChild(div);
		});
	}
	// 신규 업로드시 미리보기(대표/추가)
	if (mainInput) {
		mainInput.addEventListener('change', function() {
			if (this.files.length > 0) { mainImageAction = 'REPLACE'; }
			else { mainImageAction = 'KEEP'; }
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
					div.querySelector('button').onclick = () => { mainInput.value = ''; mainPreview.innerHTML = ''; mainImageAction = 'KEEP'; };
					wrap.appendChild(div);
					mainPreview.appendChild(wrap);
				};
				reader.readAsDataURL(file);
			}
		});
	}
	if (subInput) {
		subInput.addEventListener('change', function() {
			subFiles = Array.from(this.files);
			renderSubImagePreview();
		});
	}
	function renderSubImagePreview() {
		// 새로 추가한 파일들에 대한 미리보기(서버것과 분리)
		const addedWrapId = 'sub-added-preview-wrap';
		let wrap = byId(addedWrapId);
		if (!wrap) {
			wrap = document.createElement('div');
			wrap.id = addedWrapId;
			subPreview.appendChild(wrap);
		}
		wrap.innerHTML = '';
		subFiles.forEach((file, idx) => {
			const reader = new FileReader();
			reader.onload = e => {
				const div = document.createElement('div');
				div.className = 'image-preview-thumb position-relative';
				div.style.width = '100px'; div.style.height = '100px'; div.style.marginRight = '8px';
				div.innerHTML = `<img src="${e.target.result}" style="width:100%;height:100%;object-fit:cover;">
          <button class="btn-close btn-sm" style="position:absolute;top:0;right:0;z-index:2;" aria-label="Remove"></button>`;
				div.querySelector('button').onclick = () => { subFiles.splice(idx, 1); renderSubImagePreview(); };
				wrap.appendChild(div);
			};
			reader.readAsDataURL(file);
		});
	}
	if (window.Sortable && subPreview) {
		new Sortable(subPreview, {
			animation: 150,
			onEnd: function(evt) {
				const oldIndex = evt.oldIndex, newIndex = evt.newIndex;
				if (oldIndex !== newIndex) {
					// 서버 미리보기와 섞여 있을 수 있으므로, 새로 추가한 Wrap 내부에서만 순서 이동
					// (필요시 여기서 추가 구현 가능)
				}
			}
		});
	}

	// ====== 가격대체문구 토글 ======
	const usePriceReplacementText = byId('usePriceReplacementText');
	const priceReplacementArea = byId('priceReplacementArea');
	if (usePriceReplacementText && priceReplacementArea) {
		usePriceReplacementText.onchange = function() {
			priceReplacementArea.style.display = this.checked ? '' : 'none';
			if (!this.checked) { const t = byId('priceReplacementText'); if (t) t.value = ''; }
		};
	}

	// ====== 아이콘 표시기간 토글 ======
	const useIconPeriod = byId('use-icon-period');
	const iconStart = byId('icon-start');
	const iconEnd = byId('icon-end');
	if (useIconPeriod && iconStart && iconEnd) {
		useIconPeriod.onchange = () => {
			const enabled = useIconPeriod.checked;
			iconStart.disabled = !enabled;
			iconEnd.disabled = !enabled;
			if (!enabled) { iconStart.value = ''; iconEnd.value = ''; }
		};
	}

	// ====== 할인혜택 모달 (검색/선택/적용) ======
	const discountModal = byId('discountModal');
	const openDiscountBtn = byId('open-discount-modal-btn');
	const discountOverlayClose = $$$('.discount-modal-close, .discount-modal-overlay, .discount-modal-cancel', discountModal);
	const searchPromotionBtn = byId('search-promotion-btn');
	const promotionModalList = byId('promotion-modal-list');
	const selectedDiscountListModal = byId('selected-discount-list-modal');
	const discountApplyBtn = byId('discount-apply-btn');
	function openModal(modalEl) { modalEl.style.display = 'block'; modalEl.setAttribute('aria-hidden', 'false'); modalEl.classList.add('active'); }
	function closeModal(modalEl) { modalEl.classList.remove('active'); modalEl.setAttribute('aria-hidden', 'true'); modalEl.style.display = 'none'; }
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
        <button type="button" class="btn btn-outline-primary btn-sm ms-auto" ${already ? 'disabled' : ''}>추가</button>`;
			row.querySelector('button').onclick = () => {
				if (!selectedDiscounts.some(x => String(x.id) === String(d.id))) {
					selectedDiscounts.push(d);
					renderSelectedDiscountsModal();
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
			badge.lastElementChild.onclick = () => { selectedDiscounts.splice(idx, 1); renderSelectedDiscountsModal(); };
			selectedDiscountListModal.appendChild(badge);
		});
	}
	if (openDiscountBtn) openDiscountBtn.onclick = () => { openModal(discountModal); renderPromotionList([]); renderSelectedDiscountsModal(); };
	discountOverlayClose.forEach(btn => btn.addEventListener('click', () => closeModal(discountModal)));
	if (searchPromotionBtn) {
		searchPromotionBtn.onclick = function() {
			const name = byId('promotionName').value.trim();
			const type = byId('promotionType').value;
			const start = byId('promotionStart').value;
			const end = byId('promotionEnd').value;
			const active = byId('promotionActive').value;
			let url = `/api/promotion/search?`;
			url += name ? `name=${encodeURIComponent(name)}&` : '';
			url += type ? `type=${encodeURIComponent(type)}&` : '';
			url += start ? `startDate=${start}&` : '';
			url += end ? `endDate=${end}&` : '';
			url += (active ? `active=${active}&` : '');
			fetch(url).then(res => res.json()).then(list => renderPromotionList(list));
		};
	}
	if (discountApplyBtn) discountApplyBtn.onclick = () => { renderSelectedDiscountsMain(); closeModal(discountModal); };

	// ====== 관련상품 모달 ======
	const relatedModal = byId('relatedProductModal');
	const relatedOpenBtn = byId('open-related-modal-btn');
	const relatedCloseBtns = $$$('.related-modal-close, .related-modal-cancel, .related-modal-overlay', relatedModal);
	const relatedLargeSelect = byId('related-large-select');
	const relatedMediumSelect = byId('related-medium-select');
	const relatedSmallSelect = byId('related-small-select');
	const relatedKeywordInput = byId('related-product-keyword');
	const relatedProductSearchBtn = byId('related-product-search-btn');
	const relatedSelectAppendBtn = byId('related-select-append-btn');
	const relatedModalProductList = byId('related-modal-product-list');
	const relatedRegisterBtn = byId('related-register-btn');
	const relatedSelectedList = byId('related-modal-selected-list');

	let relatedProductList = [];
	let relatedCheckedIds = new Set();
	let relatedProductsTemp = [];
	let relatedRegisterType = 'RECIPROCAL';
	const relatedRegisterTypeSelect = byId('related-register-type');
	if (relatedRegisterTypeSelect) relatedRegisterTypeSelect.onchange = () => { relatedRegisterType = relatedRegisterTypeSelect.value || 'RECIPROCAL'; };

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
	if (relatedOpenBtn) {
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
				relatedProductsTemp = [];
				renderRelatedModalProductList();
				renderRelatedSelectedList();
			});
		};
	}
	relatedCloseBtns.forEach(btn => btn.onclick = () => closeModal(relatedModal));
	if (relatedLargeSelect) relatedLargeSelect.onchange = function() {
		fetchAndRenderMediumOptions(relatedMediumSelect, this.value, relatedSmallSelect);
		relatedProductList = []; renderRelatedModalProductList();
	};
	if (relatedMediumSelect) relatedMediumSelect.onchange = function() {
		fetchAndRenderSmallOptions(relatedSmallSelect, this.value);
		relatedProductList = []; renderRelatedModalProductList();
	};
	if (relatedProductSearchBtn) {
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
	}
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
	const relatedSelectAppendBtn2 = byId('related-select-append-btn');
	if (relatedSelectAppendBtn2) {
		relatedSelectAppendBtn2.onclick = function() {
			const ids = Array.from(relatedCheckedIds);
			ids.forEach(id => {
				const p = relatedProductList.find(x => String(x.id) === String(id));
				if (p && !relatedProductsTemp.some(t => String(t.id) === String(id))) {
					relatedProductsTemp.push({ id: p.id, name: p.name, type: relatedRegisterType });
				}
			});
			renderRelatedSelectedList();
		};
	}
	function renderRelatedSelectedList() {
		const relatedSelectedListEl = byId('related-modal-selected-list');
		relatedSelectedListEl.innerHTML = '';
		if (!relatedProductsTemp || relatedProductsTemp.length === 0) {
			relatedSelectedListEl.innerHTML = `<div class="text-muted text-center">선택된 관련상품이 없습니다.</div>`;
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
			relatedSelectedListEl.appendChild(div);
		});
	}
	if (relatedRegisterBtn) {
		relatedRegisterBtn.onclick = function() {
			relatedProducts = relatedProductsTemp.map((p, i) => ({ id: p.id, name: p.name, sortOrder: i, type: p.type || relatedRegisterType }));
			renderRelatedProductsMain();
			closeModal(relatedModal);
		};
	}

	// ====== 추가구성상품 모달 ======
	const bundleModal = byId('bundleProductModal');
	const bundleOpenBtn = byId('open-bundle-modal-btn');
	const bundleCloseBtns = $$$('.bundle-modal-close, .bundle-modal-cancel, .bundle-modal-overlay', bundleModal);
	const bundleLargeSelect = byId('bundle-large-select');
	const bundleMediumSelect = byId('bundle-medium-select');
	const bundleSmallSelect = byId('bundle-small-select');
	const bundleKeywordInput = byId('bundle-product-keyword');
	const bundleProductSearchBtn = byId('bundle-product-search-btn');
	const bundleModalProductList = byId('bundle-modal-product-list');
	const bundleRegisterBtn = byId('bundle-register-btn');

	let bundleProductList = [];
	let bundleSelectedProductIds = new Set();

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
	if (bundleOpenBtn) {
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
	}
	bundleCloseBtns.forEach(btn => btn.onclick = () => closeModal(bundleModal));
	if (bundleLargeSelect) bundleLargeSelect.onchange = function() { fetchAndRenderMediumOptions(bundleMediumSelect, this.value, bundleSmallSelect); bundleProductList = []; renderBundleModalProductList(); };
	if (bundleMediumSelect) bundleMediumSelect.onchange = function() { fetchAndRenderSmallOptions(bundleSmallSelect, this.value); bundleProductList = []; renderBundleModalProductList(); };
	if (bundleProductSearchBtn) {
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
	}
	if (bundleRegisterBtn) {
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
	}

	// ====== 추가 입력필드 렌더/추가 ======
	const extraList = byId('product-manager-extra-field-list');
	const addExtraFieldBtn = byId('product-manager-add-extra-field');
	function renderExtraFields() {
		extraList.innerHTML = '';
		extraFields.forEach((field, idx) => {
			const row = document.createElement('div');
			row.className = 'input-group mb-2';
			row.innerHTML = `
        <input type="text" class="form-control form-control-sm" name="extraFields[${idx}].label" placeholder="질문명" value="${field.label || ''}" required>
        <input type="text" class="form-control form-control-sm" name="extraFields[${idx}].value" placeholder="답변값" value="${field.value || ''}" required>
        <button type="button" class="btn btn-outline-danger btn-sm" title="삭제">×</button>`;
			row.querySelector('button').onclick = () => { extraFields.splice(idx, 1); renderExtraFields(); };
			extraList.appendChild(row);
		});
	}
	if (addExtraFieldBtn) addExtraFieldBtn.onclick = () => { extraFields.push({ label: '', value: '' }); renderExtraFields(); };

	// ====== 공통표시항목 렌더 (질문정의 + 기존답변 바인딩) ======
	function makeQuestionInput(q) {
		const requiredAttr = q.required ? 'required' : '';
		const editorId = q.type === 'CKEDITOR' ? `editor-question-${q.id}` : '';
		switch (q.type) {
			case 'INPUT':
				return `<input type="text" class="form-control form-control-sm" name="question_${q.id}" placeholder="${q.placeholder || ''}" ${requiredAttr}>`;
			case 'TEXTAREA':
				return `<textarea class="form-control form-control-sm" name="question_${q.id}" rows="2" placeholder="${q.placeholder || ''}" ${requiredAttr}></textarea>`;
			case 'SELECT':
				return `<select class="form-select form-select-sm" name="question_${q.id}" ${requiredAttr}>
          ${(q.options || []).map((o) => {
					if (typeof o === 'object' && o) {
						if ('value' in o && 'label' in o) return `<option value="${o.value}">${o.label}</option>`;
						const keys = Object.keys(o);
						if (keys.length === 1) return `<option value="${keys[0]}">${o[keys[0]]}</option>`;
						if ('value' in o) return `<option value="${o.value}">${o.value}</option>`;
						if ('label' in o) return `<option value="${o.label}">${o.label}</option>`;
						return `<option disabled>선택지 오류</option>`;
					} else {
						return `<option value="${o}">${o}</option>`;
					}
				}).join('')}
        </select>`;
			case 'FILE':
				return `<div class="d-flex flex-column gap-1">
					<input type="file" class="form-control form-control-sm" name="question_${q.id}" ${requiredAttr}>
					<div class="form-text" id="q-file-helper-${q.id}"></div>
				</div>`;
			case 'CKEDITOR':
				return `<textarea class="form-control" name="question_${q.id}" id="${editorId}" rows="3"
                 ${requiredAttr} data-type="question" data-key="question_${q.id}"></textarea>`;
			default:
				return `<input type="text" class="form-control form-control-sm" disabled placeholder="지원되지 않는 타입">`;
		}
	}
	function bindAnswers(questions, answers) {
		const answerMap = {};
		(answers || []).forEach((a) => { answerMap[a.questionId] = a; });

		questions.forEach((q) => {
			const name = `question_${q.id}`;
			const ans = answerMap[q.id];
			if (!ans) return;

			if (q.type === 'CKEDITOR') {
				const editorId = `editor-question-${q.id}`;
				const editor = ckeInstances[editorId];
				if (editor) editor.setData(ans.value || '');
			} else if (q.type === 'FILE') {
				// 파일형: 유지/삭제/교체 상태관리 초기화
				const helper = byId(`q-file-helper-${q.id}`);
				const input = $$(`[name="${name}"]`);
				const urls = Array.isArray(ans.fileUrls) ? ans.fileUrls.slice() : [];
				fileQuestionActions[q.id] = { action: 'KEEP', serverFiles: urls };
				if (helper) {
					const links = urls.length ? urls.map(u => `<a href="${u}" target="_blank">파일</a>`).join(', ') : '없음';
					helper.innerHTML = `기존 파일: ${links} | <button type="button" class="btn btn-sm btn-outline-danger" data-qid="${q.id}">기존파일 삭제</button>`;
					helper.querySelector('button')?.addEventListener('click', () => {
						fileQuestionActions[q.id].action = 'DELETE';
						if (input) input.value = ''; // 교체 방지
						helper.innerHTML = `기존 파일: (삭제 예정) | 필요시 새 파일을 업로드하면 '교체'로 처리됩니다.`;
					});
				}
				// 새 파일 선택 시 → REPLACE 로 전환
				if (input) {
					input.addEventListener('change', function() {
						if (this.files && this.files.length > 0) {
							fileQuestionActions[q.id].action = 'REPLACE';
							if (helper) helper.innerHTML = `기존 파일: (교체 예정) 새 파일 ${this.files.length}개 선택됨`;
						} else {
							// 파일 선택을 취소하면 기존 상태 유지 (삭제 버튼을 누른 상태였다면 DELETE 유지)
							if (fileQuestionActions[q.id].action === 'REPLACE') {
								fileQuestionActions[q.id].action = 'KEEP';
								if (helper) helper.innerHTML = `기존 파일: ${(fileQuestionActions[q.id].serverFiles || []).length ? '유지' : '없음'}`;
							}
						}
					});
				}
			} else {
				const el = $$(`[name="${name}"]`);
				if (!el) return;
				el.value = ans.value || '';
			}
		});
	}
	/** FILE 타입 질문 공통 초기화: 기존 답변 유무와 상관없이 액션/리스너 세팅 */
	function setupFileQuestions(questions) {
		(questions || []).forEach((q) => {
			if (q.type !== 'FILE') return;
			const qid = String(q.id);
			const name = `question_${qid}`;
			const input = $$(`[name="${name}"]`);
			const helper = byId(`q-file-helper-${qid}`);

			// 기존 초기값 없으면 기본값 주입
			if (!fileQuestionActions[qid]) {
				fileQuestionActions[qid] = { action: 'KEEP', serverFiles: [] };
			}
			// helper가 비어있다면 기본 텍스트
			if (helper && helper.innerHTML.trim() === '') {
				helper.innerHTML = `기존 파일: 없음`;
			}

			// 변경 리스너(중복 바인딩 방지)
			if (input && !input.__listenerBound) {
				input.addEventListener('change', function() {
					const cnt = this.files?.length || 0;
					if (cnt > 0) {
						fileQuestionActions[qid].action = 'REPLACE';
						if (helper) helper.innerHTML = `새 파일 ${cnt}개 선택됨 (교체 예정)`;
					} else {
						// 선택을 취소 → REPLACE였던 걸 되돌림 (DELETE는 유지)
						if (fileQuestionActions[qid].action === 'REPLACE') {
							fileQuestionActions[qid].action = 'KEEP';
							const keep = (fileQuestionActions[qid].serverFiles || []).length > 0;
							if (helper) helper.innerHTML = keep ? `기존 파일: 유지` : `기존 파일: 없음`;
						}
					}
				});
				input.__listenerBound = true;
			}
		});
	}

	// ====== 상세설명 CKEditor Mount ======
	async function mountDetailEditor(html) {
		const desc = byId('editor-desc');
		if (desc && window.ClassicEditor) {
			desc.setAttribute('data-type', 'detailHtml');
			desc.setAttribute('data-key', 'detailHtml');
			detailEditor = await window.ClassicEditor.create(desc, {
				toolbar: { items: ['heading', '|', 'bold', 'italic', 'underline', 'strikethrough', 'highlight', 'fontColor', 'fontBackgroundColor', '|', 'link', 'bulletedList', 'numberedList', 'blockQuote', '|', 'insertTable', 'imageUpload', 'mediaEmbed', '|', 'undo', 'redo', 'alignment', 'outdent', 'indent'] },
				image: { toolbar: ['imageTextAlternative', 'imageStyle:full', 'imageStyle:side', 'linkImage'], styles: ['full', 'side'], resizeUnit: 'px' },
				table: { contentToolbar: ['tableColumn', 'tableRow', 'mergeTableCells', 'tableCellProperties', 'tableProperties'] },
				mediaEmbed: { previewsInData: true },
				language: 'ko',
				extraPlugins: [CustomUploadAdapterPlugin]
			});
			detailEditor.setData(html || '');
		}
	}
	// ====== 질문 CKEditor Mount ======
	async function mountQuestionEditors(questions) {
		const tasks = [];
		(questions || []).forEach((q) => {
			if (q.type !== 'CKEDITOR') return;
			const tId = `editor-question-${q.id}`;
			const textarea = byId(tId);
			if (!textarea) return;
			tasks.push(
				window.ClassicEditor.create(textarea, {
					toolbar: { items: ['heading', '|', 'bold', 'italic', 'underline', 'strikethrough', 'highlight', 'fontColor', 'fontBackgroundColor', '|', 'link', 'bulletedList', 'numberedList', 'blockQuote', '|', 'insertTable', 'imageUpload', 'mediaEmbed', '|', 'undo', 'redo', 'alignment', 'outdent', 'indent'] },
					image: { toolbar: ['imageTextAlternative', 'imageStyle:full', 'imageStyle:side', 'linkImage'], styles: ['full', 'side'], resizeUnit: 'px' },
					table: { contentToolbar: ['tableColumn', 'tableRow', 'mergeTableCells', 'tableCellProperties', 'tableProperties'] },
					mediaEmbed: { previewsInData: true },
					language: 'ko',
					extraPlugins: [CustomUploadAdapterPlugin]
				}).then((editor) => { ckeInstances[tId] = editor; })
			);
		});
		await Promise.all(tasks);
	}

	// ====== 상세 데이터 로딩 & 바인딩 ======
	try {
		const res = await fetch(DETAIL_API);
		if (!res.ok) throw new Error(`상세 API 오류: HTTP ${res.status}`);
		const d = await res.json(); // ProductDetailReadResponseDTO
		__originalDTO__ = deepClone(d);

		// 1) 기본/상태
		if (d.displayStatus) $$(`input[name="displayStatus"][value="${d.displayStatus}"]`)?.click();
		if (d.saleStatus) $$(`input[name="saleStatus"][value="${d.saleStatus}"]`)?.click();
		if (d.name) setVal('#productName', d.name);
		if (d.code) setVal('#productCode', d.code);
		setVal('#summaryDescription', d.summaryDescription || '');
		setVal('#shortDescription', d.shortDescription || '');
		if (d.consumerPrice != null) setVal('#consumerPrice', d.consumerPrice);
		if (d.salePrice != null) setVal('#salePrice', d.salePrice);
		if (d.rewardRate != null) setVal('#rewardRate', d.rewardRate);
		if (d.validFrom) setVal('#validFrom', d.validFrom);
		if (d.validTo) setVal('#validTo', d.validTo);
		if (d.newState) setVal('#newState', d.newState);
		setVal('#manufacturerText', d.manufacturerText || '');
		setVal('#supplierText', d.supplierText || '');
		setVal('#internalProductCode', d.internalProductCode || '');
		setVal('#manufacturedAt', d.manufacturedAt || '');
		setVal('#expiredAt', d.expiredAt || '');

		// 2) 가격정책
		if (d.pricePolicy) {
			if (d.pricePolicy.priceExposeTarget) $$(`input[name="priceExposeTarget"][value="${d.pricePolicy.priceExposeTarget}"]`)?.click();
			if (d.pricePolicy.usePriceReplacementText) {
				const chk = byId('usePriceReplacementText');
				if (chk) {
					chk.checked = true;
					if (priceReplacementArea) priceReplacementArea.style.display = '';
					setVal('#priceReplacementText', d.pricePolicy.priceReplacementText || '');
				}
			}
		}

		// 3) 외부 카테고리
		selectedCategories = (d.externalCategories || []).map((c) => ({
			id: c.smallId,
			largeId: c.largeId,
			largeName: c.largeName,
			mediumId: c.mediumId,
			mediumName: c.mediumName,
			smallName: c.smallName
		}));
		renderSelectedCategories();

		// 4) 내부 카테고리
		await applyInternalCategoryFromDTO(d);

		// 5) 브랜드
		if (d.brand) {
			selectedBrand = { id: d.brand.id, name: d.brand.name, imageUrl: d.brand.imageUrl };
			renderSelectedBrand();
		}

		// 6) 키워드
		keywords = Array.isArray(d.keywords) ? d.keywords.slice() : [];
		renderKeywordList();

		// 7) 공통표시항목 & 답변
		if (Array.isArray(d.displayQuestions)) {
			displayContainer.innerHTML = '';
			d.displayQuestions.forEach((q) => {
				const colClass = q.type === 'TEXTAREA' || q.type === 'CKEDITOR' ? 'col-12 mb-2' : 'col-6 mb-2';
				const div = document.createElement('div');
				div.className = colClass + ' d-flex flex-column justify-content-end';
				const requiredMark = q.required ? ' <span class="text-danger">*</span>' : '';
				div.innerHTML = `<label class="form-label mb-1">${q.label}${requiredMark}</label>${makeQuestionInput(q)}`;
				displayContainer.appendChild(div);
			});

			// ① DOM이 생겼으니, 기존답변 유무와 상관없이 FILE 리스너/액션 선 세팅
			setupFileQuestions(d.displayQuestions);

			await mountQuestionEditors(d.displayQuestions);
			bindAnswers(d.displayQuestions, d.answers || []);

			// ② bindAnswers가 helper 텍스트를 업데이트할 수 있으므로, 마무리로 한 번 더 보정
			setupFileQuestions(d.displayQuestions);
		}

		// 8) 상세설명
		await mountDetailEditor(d.detailHtml || '');

		// 9) 이미지 (서버 저장본 미리보기)
		renderServerImages(d.images || null);

		// 10) 아이콘
		if (d.icon) {
			// 기간 스위치/날짜
			if (useIconPeriod) useIconPeriod.checked = !!d.icon.usePeriod;
			if (iconStart) iconStart.disabled = !useIconPeriod?.checked;
			if (iconEnd) iconEnd.disabled = !useIconPeriod?.checked;
			if (d.icon.startDate && iconStart) iconStart.value = d.icon.startDate;
			if (d.icon.endDate && iconEnd) iconEnd.value = d.icon.endDate;

			// 아이콘 이미지 미리보기 & 상태관리(대표/추가와 동일 액션 모델)
			renderServerIconImage({ imageUrl: d.icon.imageUrl || '' });
		}

		// 11) 옵션
		optionGroups = (d.optionGroups || []).map((g) => ({
			name: g.name,
			options: (g.options || []).map((o) => ({
				name: o.name, value: o.value, extraPrice: o.extraPrice, sign: o.sign || 'PLUS', sortOrder: o.sortOrder
			}))
		}));
		renderOptionGroups();

		// 12) 관련/번들
		relatedProducts = (d.relatedProducts || []).map((r, i) => ({ id: r.id, name: r.name, sortOrder: r.sortOrder ?? i, type: r.type || 'RECIPROCAL' }));
		bundleProducts = (d.bundleProducts || []).map((b, i) => ({ id: b.id, name: b.name, sortOrder: b.sortOrder ?? i }));

		// ✅ 라디오 상태를 데이터로 강제
		applyUseRelatedByData();
		applyUseBundleByData();

		// ✅ 리스트 렌더 (라디오와 무관하게 일단 그려둠)
		renderRelatedProductsMain();
		renderBundleProductsMain();


		// 13) 딜러 등급별 할인
		dealerDiscounts = d.dealerDiscounts || {};
		renderDealerDiscounts();

		// 14) 프로모션
		selectedDiscounts = (d.discounts || []).map((x) => ({
			id: x.id, name: x.name, type: x.type, term: x.term, active: x.active,
			startDate: x.startDate, endDate: x.endDate, target: x.target, couponPolicy: x.couponPolicy,
			typeLabel: x.typeLabel, termLabel: x.termLabel
		}));
		renderSelectedDiscountsMain();

		// 15) 추가 입력필드
		extraFields = (d.extraFields || []).map((e) => ({ label: e.label, value: e.value }));
		renderExtraFields();

		// === 원본 정규화 상태 생성 (diff/validation용) ===
		__originalState__ = buildNormalizedStateFromDTO(d);

	} catch (e) {
		console.error(e);
		alert(e.message || '상세 로딩 중 오류');
	}

	// ====== 사용함/안함 토글 버튼 상태(관련/번들) ======
	$$$('input[name="useRelatedProducts"]').forEach(r => {
		r.onchange = function() { byId('open-related-modal-btn').disabled = (this.value === 'false'); };
	});
	$$$('input[name="useBundleItems"]').forEach(r => {
		r.onchange = function() { byId('open-bundle-modal-btn').disabled = (this.value === 'false'); };
	});

	// ====== 제품코드 중복확인 ======
	const checkProductCodeBtn = byId('checkProductCodeBtn');
	const productCodeInput = byId('productCode');
	const codeHint = byId('product-code-check-hint');
	if (checkProductCodeBtn && productCodeInput && codeHint) {
		checkProductCodeBtn.onclick = async function() {
			const code = (productCodeInput.value || '').trim();
			if (!code) { alert('제품코드를 입력하세요.'); return; }
			codeHint.textContent = '중복 확인 중...';
			try {
				const res = await fetch(`/api/product/check-code?code=${encodeURIComponent(code)}`);
				if (!res.ok) throw new Error('API 응답 오류');
				const data = await res.json();
				if (data.available) {
					codeHint.textContent = '사용 가능한 코드입니다.'; codeHint.className = 'form-text text-success';
				} else {
					codeHint.textContent = '이미 사용 중인 코드입니다.'; codeHint.className = 'form-text text-danger';
				}
			} catch (e) {
				console.warn('[중복확인] API 오류/미구현', e);
				codeHint.textContent = '중복확인 API 오류/미구현'; codeHint.className = 'form-text text-warning';
			}
		};
	}

	// ===========================================================
	// ============== [추가] 정규화/검증/변경요약 ================
	// ===========================================================

	function buildNormalizedStateFromDTO(d) {
		return {
			displayStatus: d.displayStatus || null,
			saleStatus: d.saleStatus || null,
			name: d.name || '',
			code: d.code || '',
			summaryDescription: d.summaryDescription || '',
			shortDescription: d.shortDescription || '',
			consumerPrice: d.consumerPrice ?? '',
			salePrice: d.salePrice ?? '',
			rewardRate: d.rewardRate ?? '',
			validFrom: d.validFrom || '',
			validTo: d.validTo || '',
			newState: d.newState || '',
			manufacturerText: d.manufacturerText || '',
			supplierText: d.supplierText || '',
			internalProductCode: d.internalProductCode || '',
			manufacturedAt: d.manufacturedAt || '',
			expiredAt: d.expiredAt || '',
			pricePolicy: {
				priceExposeTarget: d.pricePolicy?.priceExposeTarget || 'MEMBER',
				usePriceReplacementText: !!d.pricePolicy?.usePriceReplacementText,
				priceReplacementText: d.pricePolicy?.priceReplacementText || ''
			},
			internalCategorySmallId: d.internalCategorySmallId ? String(d.internalCategorySmallId) : '',
			externalCategories: (d.externalCategories || []).map(c => ({ smallId: String(c.smallId), path: [c.largeName, c.mediumName, c.smallName].join(' > ') })),
			brand: d.brand ? { id: String(d.brand.id), name: d.brand.name } : null,
			keywords: Array.isArray(d.keywords) ? d.keywords.slice() : [],
			displayQuestions: (d.displayQuestions || []).map(q => ({ id: String(q.id), type: q.type, required: !!q.required })),
			answers: (d.answers || []).reduce((acc, a) => { acc[String(a.questionId)] = { value: a.value || '', fileUrls: a.fileUrls || [] }; return acc; }, {}),
			detailHtml: d.detailHtml || '',
			images: {
				mainImageUrl: d.images?.mainImageUrl || '',
				subImageUrls: Array.isArray(d.images?.subImageUrls) ? d.images.subImageUrls.slice() : []
			},
			icon: {
				usePeriod: !!d.icon?.usePeriod,
				startDate: d.icon?.startDate || '',
				endDate: d.icon?.endDate || '',
				imageUrl: d.icon?.imageUrl || ''
			},
			optionGroups: (d.optionGroups || []).map(g => ({
				name: g.name || '',
				options: (g.options || []).map(o => ({
					name: o.name || '', value: o.value || '', extraPrice: o.extraPrice ?? '', sign: o.sign || 'PLUS'
				}))
			})),
			relatedProducts: (d.relatedProducts || []).map(r => ({ id: String(r.id), name: r.name, type: r.type || 'RECIPROCAL' })),
			bundleProducts: (d.bundleProducts || []).map(b => ({ id: String(b.id), name: b.name })),
			dealerDiscounts: d.dealerDiscounts || {},
			discounts: (d.discounts || []).map(x => ({ id: String(x.id), name: x.name, type: x.type, active: !!x.active })),
			extraFields: (d.extraFields || []).map(e => ({ label: e.label || '', value: e.value || '' })),
			internalCategoryLargeId: d.internalCategoryLargeId ? String(d.internalCategoryLargeId) : '',
			internalCategoryMediumId: d.internalCategoryMediumId ? String(d.internalCategoryMediumId) : '',
		};
	}

	function collectCurrentStateFromDOM() {
		// 공통표시항목 현재값 수집
		const questionValues = {};
		const fileQuestionNow = {}; // 파일형 현재 상태 { action, newFileCount, keepCount }
		(__originalState__?.displayQuestions || []).forEach(q => {
			const name = `question_${q.id}`;
			if (q.type === 'CKEDITOR') {
				const editor = ckeInstances[`editor-question-${q.id}`];
				questionValues[q.id] = { value: editor ? editor.getData() : ($$(`[name="${name}"]`)?.value || '') };
			} else if (q.type === 'FILE') {
				const input = $$(`[name="${name}"]`);
				const act = fileQuestionActions[q.id]?.action || 'KEEP';
				const keepCount = (fileQuestionActions[q.id]?.serverFiles || []).length;
				const newCount = input?.files?.length || 0;
				fileQuestionNow[q.id] = { action: act, newFileCount: newCount, keepCount: keepCount };
				questionValues[q.id] = { value: __originalState__.answers[q.id]?.value || '' };
			} else {
				const el = $$(`[name="${name}"]`);
				questionValues[q.id] = { value: el?.value || '' };
			}
		});

		// 아이콘 기간 스위치/날짜
		const iconUse = !!byId('use-icon-period')?.checked;
		const iconStartVal = byId('icon-start')?.value || '';
		const iconEndVal = byId('icon-end')?.value || '';

		// 대표/추가 이미지
		const hasNewMain = !!(mainInput?.files?.length);
		const mainAct = hasNewMain ? 'REPLACE' : mainImageAction; // change 이벤트에서 이미 세팅됨
		const addSubCount = subFiles.length;
		const delSubCount = deletedSubImageUrls.size;

		// 아이콘 이미지(대표/추가와 동일한 액션 모델)
		const hasNewIcon = !!(iconInput?.files?.length);
		const iconAct = hasNewIcon ? 'REPLACE' : iconImageAction;

		// 옵션 그룹
		const og = [];
		if (optionGroupList) {
			const groupCards = optionGroupList.querySelectorAll('.card');
			groupCards.forEach((groupDiv, groupIdx) => {
				const groupName = groupDiv.querySelector(`[name="optionGroups[${groupIdx}].name"]`)?.value || '';
				const opts = [];
				groupDiv.querySelectorAll('.input-group.mb-1').forEach((row, optIdx) => {
					opts.push({
						name: row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].name"]`)?.value || '',
						value: row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].value"]`)?.value || '',
						extraPrice: row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].extraPrice"]`)?.value || '',
						sign: row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].sign"]`)?.value || 'PLUS'
					});
				});
				og.push({ name: groupName, options: opts });
			});
		}

		return {
			displayStatus: $$('input[name="displayStatus"]:checked')?.value || null,
			saleStatus: $$('input[name="saleStatus"]:checked')?.value || null,
			name: byId('productName')?.value || '',
			code: byId('productCode')?.value || '',
			summaryDescription: byId('summaryDescription')?.value || '',
			shortDescription: byId('shortDescription')?.value || '',
			consumerPrice: byId('consumerPrice')?.value ?? '',
			salePrice: byId('salePrice')?.value ?? '',
			rewardRate: byId('rewardRate')?.value ?? '',
			validFrom: byId('validFrom')?.value || '',
			validTo: byId('validTo')?.value || '',
			newState: byId('newState')?.value || '',
			manufacturerText: byId('manufacturerText')?.value || '',
			supplierText: byId('supplierText')?.value || '',
			internalProductCode: byId('internalProductCode')?.value || '',
			manufacturedAt: byId('manufacturedAt')?.value || '',
			expiredAt: byId('expiredAt')?.value || '',
			pricePolicy: {
				priceExposeTarget: $$('input[name="priceExposeTarget"]:checked')?.value || 'MEMBER',
				usePriceReplacementText: !!byId('usePriceReplacementText')?.checked,
				priceReplacementText: byId('priceReplacementText')?.value || ''
			},
			internalCategorySmallId: internalCategorySmallId || '',
			externalCategories: selectedCategories.map(c => ({ smallId: String(c.id), path: [c.largeName, c.mediumName, c.smallName].join(' > ') })),
			brand: selectedBrand ? { id: String(selectedBrand.id), name: selectedBrand.name } : null,
			keywords: keywords.slice(),
			displayQuestions: __originalState__?.displayQuestions || [],
			answers: questionValues, // { qid: { value } } (FILE은 value 대신 fileQuestionNow가 추적)
			fileQuestionNow, // FILE형 세부 상태
			detailHtml: detailEditor ? detailEditor.getData() : (byId('editor-desc')?.value || ''),

			// 이미지는 하나의 블록에서 대표/추가/아이콘을 모두 동일 포맷으로
			images: {
				// 대표
				mainAction: mainAct,
				newMainSelected: hasNewMain,
				serverMainExists: !!__originalState__?.images?.mainImageUrl,
				// 추가
				serverSubUrls: serverSubImageUrls.slice(),
				deletedSubUrls: Array.from(deletedSubImageUrls),
				newSubCount: addSubCount,
				// 아이콘
				icon: {
					action: iconAct,               // KEEP | DELETE | REPLACE
					serverExists: !!iconServerUrl, // 원본 존재 여부
					serverUrl: iconServerUrl || '',
					newSelected: hasNewIcon        // 새 아이콘 파일 선택 여부
				}
			},

			// 아이콘 기간 설정(표시기간)은 기존대로 별도 보존
			icon: {
				usePeriod: iconUse,
				startDate: iconStartVal,
				endDate: iconEndVal
			},

			optionGroups: og,
			relatedProducts: relatedProducts.map(r => ({ id: String(r.id), name: r.name, type: r.type || 'RECIPROCAL' })),
			bundleProducts: bundleProducts.map(b => ({ id: String(b.id), name: b.name })),
			dealerDiscounts: Object.assign({}, dealerDiscounts),
			discounts: selectedDiscounts.map(x => ({ id: String(x.id), name: x.name, type: x.type, active: !!x.active })),
			extraFields: extraFields.map(e => ({ label: e.label || '', value: e.value || '' })),
			internalCategoryLargeId: internalLarge?.value || '',
			internalCategoryMediumId: internalMedium?.value || ''
		};
	}

	// ===== Validation (업데이트 전) =====
	async function validateProductFormDetail() {
		// 1) 외부 카테고리 1개 이상
		if (!selectedCategories || selectedCategories.length === 0) { alert('카테고리를 1개 이상 선택하세요.'); return false; }

		// 2) 기본 필수
		const pName = byId('productName')?.value.trim();
		const pCode = byId('productCode')?.value.trim();
		const displayStatus = $$('input[name="displayStatus"]:checked')?.value;
		const saleStatus = $$('input[name="saleStatus"]:checked')?.value;
		if (!pName) { alert('제품명을 입력하세요.'); return false; }
		if (!pCode) { alert('제품코드를 입력하세요.'); return false; }
		if (!displayStatus) { alert('진열상태를 선택하세요.'); return false; }
		if (!saleStatus) { alert('판매상태를 선택하세요.'); return false; }

		// 3) 대표이미지 필수(업데이트 특수 규칙)
		const serverHasMain = !!__originalState__?.images?.mainImageUrl;
		const hasNewMain = !!(mainInput?.files?.length);
		if (serverHasMain) {
			if (mainImageAction === 'DELETE' && !hasNewMain) { alert('대표이미지를 삭제하려면 새 대표이미지를 업로드하세요.'); return false; }
		} else {
			if (!hasNewMain) { alert('대표 이미지는 필수입니다. 새 대표이미지를 업로드하세요.'); return false; }
		}

		// 4) 공통표시항목(필수) — FILE 타입 포함
		let hasQuestionError = false;
		(__originalState__?.displayQuestions || []).forEach(q => {
			if (!q.required) return;
			const name = `question_${q.id}`;
			if (q.type === 'CKEDITOR') {
				const editor = ckeInstances[`editor-question-${q.id}`];
				const v = editor ? editor.getData().trim() : ($$(`[name="${name}"]`)?.value || '').trim();
				if (!v) hasQuestionError = true;
			} else if (q.type === 'FILE') {
				const act = fileQuestionActions[q.id]?.action || 'KEEP';
				const keepCnt = (fileQuestionActions[q.id]?.serverFiles || []).length;
				const newCnt = $$(`[name="${name}"]`)?.files?.length || 0;
				if (act === 'DELETE' && newCnt === 0) hasQuestionError = true;
				if (act === 'KEEP' && keepCnt === 0) hasQuestionError = true;
				if (act === 'REPLACE' && newCnt === 0) hasQuestionError = true;
			} else {
				const el = $$(`[name="${name}"]`); if (!el || !el.value) hasQuestionError = true;
			}
		});
		if (hasQuestionError) { alert('필수 공통표시항목(질문/옵션)을 모두 입력하세요.'); return false; }

		// 5) 숫자 유효성 (가격/적립률)
		const isNumberOrEmpty = (v) => (v == null || v === '' || !isNaN(v));
		const inRangeOrEmpty = (v, min, max) => { if (v == null || v === '') return true; const n = Number(v); return n >= min && n <= max; };
		const consumerPrice = byId('consumerPrice')?.value ?? '';
		const salePrice = byId('salePrice')?.value ?? '';
		const rewardRate = byId('rewardRate')?.value ?? '';
		if (!isNumberOrEmpty(consumerPrice) || Number(consumerPrice) < 0) { alert('소비자가는 0 이상 숫자여야 합니다.'); return false; }
		if (!isNumberOrEmpty(salePrice) || Number(salePrice) < 0) { alert('판매가는 0 이상 숫자여야 합니다.'); return false; }
		if (!isNumberOrEmpty(rewardRate) || !inRangeOrEmpty(rewardRate, 0, 100)) { alert('적립률은 0~100 사이 숫자여야 합니다.'); return false; }

		// 6) 유효기간 from<=to
		const validFrom = byId('validFrom')?.value ?? '';
		const validTo = byId('validTo')?.value ?? '';
		if (validFrom && validTo && validFrom > validTo) { alert('유효기간 시작일이 종료일보다 뒤입니다.'); return false; }

		// 7) 아이콘 기간 체크시 날짜 필수 + from<=to
		if (useIconPeriod?.checked) {
			if (!iconStart?.value || !iconEnd?.value) { alert('아이콘 기간 사용 시 시작/종료일을 모두 입력하세요.'); return false; }
			if (iconStart.value > iconEnd.value) { alert('아이콘 기간의 시작일이 종료일보다 뒤입니다.'); return false; }
		}

		// 8) 관련/번들 “사용함”이면 최소 1개
		const useRelated = $$('input[name="useRelatedProducts"]:checked')?.value === 'true';
		if (useRelated && (!Array.isArray(relatedProducts) || relatedProducts.length === 0)) {
			alert('관련상품을 사용함으로 선택하셨습니다. 최소 1개 이상 등록하세요.'); return false;
		}
		const useBundle = $$('input[name="useBundleItems"]:checked')?.value === 'true';
		if (useBundle && (!Array.isArray(bundleProducts) || bundleProducts.length === 0)) {
			alert('추가구성상품을 사용함으로 선택하셨습니다. 최소 1개 이상 등록하세요.'); return false;
		}

		// 9) 딜러 추가할인 0~100 유효성
		for (const [grade, val] of Object.entries(dealerDiscounts || {})) {
			if (val === '' || val == null) { alert(`${grade} 등급 할인율을 입력하세요.`); return false; }
			if (isNaN(val) || Number(val) < 0 || Number(val) > 100) { alert(`${grade} 등급 할인율은 0~100 사이 숫자여야 합니다.`); return false; }
		}

		return true;
	}

	// ===== 변경 요약 콘솔 출력 =====
	function printChangeSummary() {
		__currentState__ = collectCurrentStateFromDOM();
		const A = __originalState__;
		const B = __currentState__;

		console.clear();
		console.group('🧾 제품 수정 변경 요약 (deep diff)');

		// 1) 기본/상태
		__printDiffTable__('1) 기본/상태', {
			displayStatus: A.displayStatus, saleStatus: A.saleStatus, name: A.name, code: A.code
		}, {
			displayStatus: B.displayStatus, saleStatus: B.saleStatus, name: B.name, code: B.code
		});

		// 2) 요약/설명/가격/기간/기타
		__printDiffTable__('2) 요약/설명/가격/기간/기타', {
			summaryDescription: A.summaryDescription, shortDescription: A.shortDescription,
			consumerPrice: A.consumerPrice, salePrice: A.salePrice, rewardRate: A.rewardRate,
			validFrom: A.validFrom, validTo: A.validTo, newState: A.newState,
			manufacturerText: A.manufacturerText, supplierText: A.supplierText, internalProductCode: A.internalProductCode,
			manufacturedAt: A.manufacturedAt, expiredAt: A.expiredAt
		}, {
			summaryDescription: B.summaryDescription, shortDescription: B.shortDescription,
			consumerPrice: B.consumerPrice, salePrice: B.salePrice, rewardRate: B.rewardRate,
			validFrom: B.validFrom, validTo: B.validTo, newState: B.newState,
			manufacturerText: B.manufacturerText, supplierText: B.supplierText, internalProductCode: B.internalProductCode,
			manufacturedAt: B.manufacturedAt, expiredAt: B.expiredAt
		});

		// 3) 가격정책
		__printDiffTable__('3) 가격정책', A.pricePolicy, B.pricePolicy);

		// 4) 내부/외부 카테고리
		__printDiffTable__('4) 내부 소분류', A.internalCategorySmallId, B.internalCategorySmallId);
		__printDiffTable__('4) 외부 카테고리', A.externalCategories, B.externalCategories, { collapse: true });

		// 5) 브랜드/키워드
		__printDiffTable__('5) 브랜드', A.brand, B.brand);
		__printDiffTable__('5) 키워드', A.keywords, B.keywords);

		// 6) 공통표시항목 값 비교 + 파일형 상세표
		console.group('6) 공통표시항목');
		__printDiffTable__('6-1) 답변 값', A.answers, B.answers, { collapse: true });
		__printFileQuestionDiff__('6-2) 파일형 질문 상태', B.fileQuestionNow, A.answers);
		console.groupEnd();

		// 7) 상세설명 – 내용이 길 수 있어 길이만 비교 + 변경 시 앞부분 미리보기
		const aLen = (A.detailHtml || '').length;
		const bLen = (B.detailHtml || '').length;
		const changed = aLen !== bLen || (A.detailHtml || '') !== (B.detailHtml || '');
		console.group(`7) 상세설명(detailHtml) — ${changed ? '변경됨' : '변경 없음'}`);
		console.log('이전 길이:', aLen, '현재 길이:', bLen);
		if (changed) {
			console.log('이전(미리보기):', __previewValue__(A.detailHtml || '', 200));
			console.log('현재(미리보기):', __previewValue__(B.detailHtml || '', 200));
		}
		console.groupEnd();

		// 8) 이미지
		console.group('8) 이미지');

		// 대표
		console.table([{
			항목: '대표이미지',
			'원본 존재': !!A.images.mainImageUrl,
			'원본 URL': __previewValue__(A.images.mainImageUrl || '', 120),
			'현재 액션': B.images.mainAction,
			'새 대표 선택 여부': B.images.newMainSelected
		}]);

		// 추가 (상태 해석)
		const subOrig = A.images.subImageUrls || [];
		const subDeleted = B.images.deletedSubUrls || [];
		const subNew = B.images.newSubCount || 0;
		let subStatus = '유지';
		if (subOrig.length === 0 && subNew > 0) subStatus = '신규 추가';
		else if (subOrig.length > 0 && subDeleted.length === subOrig.length && subNew === 0) subStatus = '전체 삭제';
		else if (subDeleted.length > 0 && subDeleted.length < subOrig.length && subNew === 0) subStatus = '일부 삭제';
		else if (subNew > 0 && subDeleted.length === 0 && subOrig.length > 0) subStatus = '추가 업로드';
		else if (subNew > 0 && subDeleted.length > 0) subStatus = '추가 + 일부 삭제';

		console.table([{
			항목: '추가이미지',
			'원본 개수': subOrig.length,
			'삭제 예정 개수': subDeleted.length,
			'신규 추가 개수': subNew,
			'상태 해석': subStatus
		}]);

		console.groupEnd();

		// 9) 아이콘
		console.group('9) 아이콘');
		console.table([{
			항목: '아이콘',
			'원본 존재': !!A.icon?.imageUrl,
			'원본 URL': __previewValue__(A.icon?.imageUrl || '', 120),
			'기간 사용': !!B.icon?.usePeriod,
			'기간(시작)': B.icon?.startDate || '',
			'기간(종료)': B.icon?.endDate || '',
			'현재 액션': B.images?.icon?.action || 'KEEP',
			'새 파일 선택': !!B.images?.icon?.newSelected
		}]);
		console.groupEnd();

		// 10) 옵션그룹
		__printDiffTable__('10) 옵션그룹', A.optionGroups, B.optionGroups, { collapse: true });

		// 11) 관련/번들
		__printDiffTable__('11) 관련상품', A.relatedProducts, B.relatedProducts, { collapse: true });
		__printDiffTable__('11) 추가구성상품', A.bundleProducts, B.bundleProducts, { collapse: true });

		// 12) 딜러할인/프로모션/추가필드
		__printDiffTable__('12) 딜러할인', A.dealerDiscounts, B.dealerDiscounts);
		__printDiffTable__('12) 프로모션', A.discounts, B.discounts, { collapse: true });
		__printDiffTable__('12) 추가입력필드', A.extraFields, B.extraFields, { collapse: true });

		console.groupEnd();
	}

	// =============================
	// ===== [UPDATE FLOW v1] ======
	// =============================

	/** HTML에서 /upload/temp/... 형태의 임시 업로드 URL들만 추출 */
	function extractTempUploadUrlsFromHtml(html) {
		if (!html) return [];
		const set = new Set();
		// 절대/상대/도메인 포함 모두 허용. 쿼리/해시 포함 허용.
		const re = /(?:https?:)?(?:\/\/[^"'>\s]+)?\/upload\/temp\/[0-9]{8}\/[^\s"'<>]+/gi;
		let m;
		while ((m = re.exec(html)) !== null) set.add(m[0]);
		return Array.from(set);
	}
	/** ''이면 그대로 '', 숫자형이면 Number로, 그 외는 원본 유지 */
	function __toNumberOrBlank__(v) {
		if (v === '' || v == null) return '';
		const n = Number(v);
		return isNaN(n) ? v : n;
	}

	/** 현재 DOM 스냅샷을 __originalState__와 같은 모양으로 투영해서 반환 */
	function buildComparableStateForDiff() {
		// 최신 DOM 수집
		const S = collectCurrentStateFromDOM();
		const A = __originalState__ || {};

		// 1) 숫자/문자 일치화
		const consumerPrice = __toNumberOrBlank__(S.consumerPrice);
		const salePrice = __toNumberOrBlank__(S.salePrice);
		const rewardRate = __toNumberOrBlank__(S.rewardRate);

		// 2) 외부카테고리: 원본과 동일 포맷 (smallId, path)
		const externalCategories = (S.externalCategories || []).map(c => ({
			smallId: String(c.smallId),
			path: c.path
		}));

		// 3) 브랜드: 원본과 동일 포맷
		const brand = S.brand ? { id: String(S.brand.id), name: S.brand.name } : null;

		// 4) 공통표시항목 답변: FILE 타입은 값 비교 대신 원본과 동일한 형태 유지
		//    - 값은 텍스트/에디터만 반영, FILE은 원본 answers의 value만 사용(파일 변경은 이미지/파일 섹션에서 판단)
		const answers = {};
		(A.displayQuestions || []).forEach(q => {
			const qid = String(q.id);
			if (q.type === 'FILE') {
				// 파일형은 값 비교를 하지 않음(파일 변경은 별도로 처리)
				answers[qid] = { value: A.answers?.[qid]?.value || '', fileUrls: A.answers?.[qid]?.fileUrls || [] };
			} else {
				answers[qid] = { value: S.answers?.[qid]?.value || '', fileUrls: [] };
			}
		});

		// 5) 상세설명
		const detailHtml = S.detailHtml || '';

		// 6) 이미지 비교: 원본 모양으로 복원
		// 대표이미지: REPLACE면 “있다”로, DELETE면 “없다”로, KEEP이면 원본대로
		let mainImageUrl = A.images?.mainImageUrl || '';
		if (S.images?.mainAction === 'REPLACE') {
			// 새 대표 업로드 예정 → 결과적으로 대표가 존재하는 상태로 간주
			mainImageUrl = '(will be replaced)';
		} else if (S.images?.mainAction === 'DELETE') {
			// 삭제 예정(새 대표 없음) → 결과적으로 없음
			const hasNewMain = !!(S.images?.newMainSelected);
			mainImageUrl = hasNewMain ? '(will be replaced)' : '';
		}

		// 추가이미지: 서버 목록 - 삭제 + (신규 추가 존재시 표시) 로 “결과 상태”를 근사
		const origSubs = Array.isArray(A.images?.subImageUrls) ? A.images.subImageUrls.slice() : [];
		const delSubs = new Set(S.images?.deletedSubUrls || []);
		const kept = origSubs.filter(u => !delSubs.has(u));
		// 새로 추가될 이미지가 있다면, 결과적으로 개수 증가로만 표현(실제 URL은 아직 없음)
		const finalSubImageUrls =
			(S.images?.newSubCount > 0) ? kept.concat(Array(S.images.newSubCount).fill('(new)')) : kept;

		// 아이콘: 기간/이미지
		const iconUsePeriod = !!S.icon?.usePeriod;
		const iconStartDate = S.icon?.startDate || '';
		const iconEndDate = S.icon?.endDate || '';
		let iconImageUrl = A.icon?.imageUrl || '';
		if (S.images?.icon?.action === 'REPLACE') {
			iconImageUrl = '(will be replaced)';
		} else if (S.images?.icon?.action === 'DELETE') {
			iconImageUrl = '';
		} // KEEP이면 원본 유지

		// 7) 옵션/관련/번들/프로모션/추가필드/딜러할인: 원본과 동일 포맷
		const optionGroups = (S.optionGroups || []).map(g => ({
			name: g.name || '',
			options: (g.options || []).map(o => ({
				name: o.name || '',
				value: o.value || '',
				extraPrice: o.extraPrice === '' ? '' : __toNumberOrBlank__(o.extraPrice),
				sign: o.sign || 'PLUS'
			}))
		}));

		const relatedProducts = (S.relatedProducts || []).map(r => ({
			id: String(r.id),
			name: r.name,
			type: r.type || 'RECIPROCAL'
		}));

		const bundleProducts = (S.bundleProducts || []).map(b => ({
			id: String(b.id),
			name: b.name
		}));

		const discounts = (S.discounts || []).map(d => ({
			id: String(d.id),
			name: d.name,
			type: d.type,
			active: !!d.active
		}));

		const extraFields = (S.extraFields || []).map(e => ({
			label: e.label || '',
			value: e.value || ''
		}));

		// 8) 최종적으로 __originalState__와 같은 구조로 반환
		return {
			displayStatus: S.displayStatus || null,
			saleStatus: S.saleStatus || null,
			name: S.name || '',
			code: S.code || '',
			summaryDescription: S.summaryDescription || '',
			shortDescription: S.shortDescription || '',
			consumerPrice,
			salePrice,
			rewardRate,
			validFrom: S.validFrom || '',
			validTo: S.validTo || '',
			newState: S.newState || '',
			manufacturerText: S.manufacturerText || '',
			supplierText: S.supplierText || '',
			internalProductCode: S.internalProductCode || '',
			manufacturedAt: S.manufacturedAt || '',
			expiredAt: S.expiredAt || '',
			pricePolicy: {
				priceExposeTarget: S.pricePolicy?.priceExposeTarget || 'MEMBER',
				usePriceReplacementText: !!S.pricePolicy?.usePriceReplacementText,
				priceReplacementText: S.pricePolicy?.priceReplacementText || ''
			},
			internalCategorySmallId: S.internalCategorySmallId ? String(S.internalCategorySmallId) : '',
			externalCategories,
			brand,
			keywords: Array.isArray(S.keywords) ? S.keywords.slice() : [],
			displayQuestions: (A.displayQuestions || []).map(q => ({ id: String(q.id), type: q.type, required: !!q.required })),
			answers,
			detailHtml,
			images: {
				mainImageUrl,
				subImageUrls: finalSubImageUrls
			},
			icon: {
				usePeriod: iconUsePeriod,
				startDate: iconStartDate,
				endDate: iconEndDate,
				imageUrl: iconImageUrl
			},
			optionGroups,
			relatedProducts,
			bundleProducts,
			dealerDiscounts: Object.assign({}, S.dealerDiscounts || {}),
			discounts,
			extraFields
		};
	}

	function applyUseRelatedByData() {
		const has = Array.isArray(relatedProducts) && relatedProducts.length > 0;
		const yes = $$('input[name="useRelatedProducts"][value="true"]');
		const no = $$('input[name="useRelatedProducts"][value="false"]');
		(has ? yes : no)?.click(); // 기존 onchange 로직을 활용(버튼 disable 포함)
	}

	function applyUseBundleByData() {
		const has = Array.isArray(bundleProducts) && bundleProducts.length > 0;
		const yes = $$('input[name="useBundleItems"][value="true"]');
		const no = $$('input[name="useBundleItems"][value="false"]');
		(has ? yes : no)?.click();
	}

	/** 현재 DOM 값을 기반으로 update용 FormData 생성 */
	function buildUpdateFormData() {
		// 최신 상태 스냅샷 수집
		__currentState__ = collectCurrentStateFromDOM();
		const S = __currentState__;
		const fd = new FormData();

		// 1) 기본 정보
		fd.append('productName', S.name || '');
		fd.append('productCode', S.code || '');
		if (S.displayStatus) fd.append('displayStatus', S.displayStatus);
		if (S.saleStatus) fd.append('saleStatus', S.saleStatus);
		fd.append('detailHtml', S.detailHtml || '');
		fd.append('manufacturerText', S.manufacturerText || '');
		fd.append('supplierText', S.supplierText || '');
		if (S.brand?.id) fd.append('brandId', S.brand.id);
		if (S.manufacturedAt) fd.append('manufacturedAt', S.manufacturedAt);
		if (S.expiredAt) fd.append('expiredAt', S.expiredAt);
		fd.append('summaryDescription', S.summaryDescription || '');
		fd.append('shortDescription', S.shortDescription || '');
		fd.append('internalProductCode', S.internalProductCode || '');

		if (S.consumerPrice !== '') fd.append('consumerPrice', S.consumerPrice);
		if (S.salePrice !== '') fd.append('salePrice', S.salePrice);

		if (S.pricePolicy?.priceExposeTarget) fd.append('priceExposeTarget', S.pricePolicy.priceExposeTarget);
		fd.append('usePriceReplacementText', S.pricePolicy?.usePriceReplacementText ? 'true' : 'false');
		if (S.pricePolicy?.usePriceReplacementText && S.pricePolicy.priceReplacementText) {
			fd.append('priceReplacementText', S.pricePolicy.priceReplacementText);
		}

		if (S.rewardRate !== '') fd.append('rewardRate', S.rewardRate);
		if (S.validFrom) fd.append('validFrom', S.validFrom);
		if (S.validTo) fd.append('validTo', S.validTo);

		// 사용함/안함
		const useRelated = $$('input[name="useRelatedProducts"]:checked')?.value === 'true';
		const useBundle = $$('input[name="useBundleItems"]:checked')?.value === 'true';
		fd.append('useRelatedProducts', useRelated ? 'true' : 'false');
		fd.append('useBundleItems', useBundle ? 'true' : 'false');

		if (S.internalCategorySmallId) fd.append('internalCategorySmallId', S.internalCategorySmallId);
		if (S.newState) fd.append('newState', S.newState);

		// 2) 카테고리(외부)
		(selectedCategories || []).forEach((c) => fd.append('categorySmallIds[]', String(c.id)));

		// 3) 대표/추가 이미지 & 액션
		// 대표
		fd.append('mainImageAction', S.images.mainAction || 'KEEP'); // KEEP | DELETE | REPLACE
		if (S.images.newMainSelected && mainInput?.files?.length) {
			fd.append('mainImage', mainInput.files[0]);
		}
		// 추가
		(S.images.deletedSubUrls || []).forEach((u) => fd.append('subImageDeleteUrls[]', u));
		if (subFiles && subFiles.length) {
			subFiles.forEach((f) => fd.append('subImages[]', f));
		}

		// 4) 아이콘(기간 + 액션/파일)
		fd.append('useIconPeriod', S.icon?.usePeriod ? 'true' : 'false');
		if (S.icon?.usePeriod && S.icon.startDate) fd.append('iconStartDate', S.icon.startDate);
		if (S.icon?.usePeriod && S.icon.endDate) fd.append('iconEndDate', S.icon.endDate);
		fd.append('iconImageAction', S.images?.icon?.action || 'KEEP'); // KEEP | DELETE | REPLACE
		if (S.images?.icon?.action === 'REPLACE' && iconInput?.files?.length) {
			fd.append('iconImage', iconInput.files[0]);
		}

		// 5) 추가입력필드
		(extraFields || []).forEach((ef, i) => {
			fd.append(`extraFields[${i}].label`, ef.label || '');
			fd.append(`extraFields[${i}].value`, ef.value || '');
		});

		// 6) 옵션그룹
		(S.optionGroups || []).forEach((g, gi) => {
			fd.append(`optionGroups[${gi}].name`, g.name || '');
			if (Array.isArray(g.options)) {
				g.options.forEach((o, oi) => {
					fd.append(`optionGroups[${gi}].options[${oi}].name`, o.name || '');
					fd.append(`optionGroups[${gi}].options[${oi}].value`, o.value || '');
					if (o.extraPrice !== '') fd.append(`optionGroups[${gi}].options[${oi}].extraPrice`, String(o.extraPrice));
					fd.append(`optionGroups[${gi}].options[${oi}].sign`, o.sign || 'PLUS');
				});
			}
		});

		// 7) 키워드
		(S.keywords || []).forEach((kw, i) => fd.append(`keywords[${i}]`, kw));

		// 8) 관련/번들
		if (useRelated) {
			(S.relatedProducts || []).forEach((r, i) => {
				fd.append(`relatedProducts[${i}].id`, r.id);
				fd.append(`relatedProducts[${i}].type`, r.type || 'RECIPROCAL');
				fd.append(`relatedProducts[${i}].sortOrder`, String(i));
			});
		}
		if (useBundle) {
			(S.bundleProducts || []).forEach((b, i) => {
				fd.append(`bundleProducts[${i}].id`, b.id);
				fd.append(`bundleProducts[${i}].sortOrder`, String(i));
			});
		}

		// 9) 프로모션(할인) – id만 보내도 충분
		(S.discounts || []).forEach((d, i) => fd.append(`discounts[${i}].id`, d.id));

		// 10) 딜러 등급 할인
		Object.entries(S.dealerDiscounts || {}).forEach(([grade, v]) => {
			fd.append(`dealerDiscounts[${grade}]`, String(v ?? ''));
		});

		// 11) 공통표시항목 (값/파일 + 파일형 액션)
		(S.displayQuestions || []).forEach((q) => {
			const qid = q.id;
			const name = `question_${qid}`;
			if (q.type === 'FILE') {
				// 파일형 액션
				const act = (S.fileQuestionNow?.[qid]?.action) || 'KEEP';
				fd.append(`${name}_fileAction`, act); // KEEP | DELETE | REPLACE
				// 새 파일(교체) 선택 시
				const input = $$(`[name="${name}"]`);
				if (input?.files?.length) {
					Array.from(input.files).forEach((f) => fd.append(name, f));
				}
				// 값 필드는 서버에서 무시하나 키 유지 차원에서 같이 보냄
				fd.append(name, '');
			} else {
				// 텍스트/셀렉트/에디터(HTML)
				const val = S.answers?.[qid]?.value || '';
				fd.append(name, val);
			}
		});

		return fd;
	}

	/** 상세와 질문 CKEditor 내 임시이미지 목록을 모아서 이동 API를 호출 */
	async function moveAllEditorImages(productId) {
		// 1) 상세
		const detailHtmlNow = detailEditor ? detailEditor.getData() : (byId('editor-desc')?.value || '');
		const detailTemps = extractTempUploadUrlsFromHtml(detailHtmlNow);
		console.log("detailTemps : ",detailTemps);
		{
			const body = { type: 'detailHtml', key: 'detailHtml', html: detailHtmlNow, tempImgList: detailTemps };
			const res = await fetch(`/api/product/${productId}/move-editor-images`, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify(body)
			});
			if (!res.ok) throw new Error('상세설명 이미지 이동 실패');
			const data = await res.json();
			if (!data?.success) throw new Error('상세설명 이미지 이동 실패(서버응답)');
			if (detailEditor) detailEditor.setData(data.newHtml || detailHtmlNow);
		}

		// 2) 공통표시사항 CKEditor들 — temp 유무와 무관하게 모두 호출
		const ckQuestions = (__originalState__?.displayQuestions || []).filter(q => q.type === 'CKEDITOR');
		for (const q of ckQuestions) {
			const editor = ckeInstances[`editor-question-${q.id}`];
			const html = editor ? editor.getData() : '';
			const temps = extractTempUploadUrlsFromHtml(html);
			console.log("temps : ",temps);
			const body = { type: 'question', key: `question_${q.id}`, html, tempImgList: temps };
			const res = await fetch(`/api/product/${productId}/move-editor-images`, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify(body)
			});
			if (!res.ok) throw new Error(`질문 #${q.id} 이미지 이동 실패`);
			const data = await res.json();
			if (!data?.success) throw new Error(`질문 #${q.id} 이미지 이동 실패(서버응답)`);
			if (editor) editor.setData(data.newHtml || html);
		}
	}


	/** 최종 업데이트 실행 */
	async function doUpdateFlow(e) {
		e?.preventDefault?.();

		printChangeSummary();
		const valid = await validateProductFormDetail();
		if (!valid) return;

		const diffs = __deepDiff__(__originalState__, buildComparableStateForDiff(), '');
		const proceed = confirm(`변경 사항이 ${diffs.length}개 있습니다.\n저장하시겠습니까?`);
		if (!proceed) return;

		// (중요) 에디터 임시이미지 선승격
		await moveAllEditorImages(productId);

		// 승격된 HTML 기준으로 FormData 생성
		const fd = buildUpdateFormData();

		try {
			const upd = await fetch(`/api/product/${productId}/update`, { method: 'POST', body: fd });
			if (!upd.ok) throw new Error(`업데이트 실패 (HTTP ${upd.status})`);
			const updResp = await upd.json();
			if (!updResp?.success) throw new Error('업데이트 실패(서버응답)');

			alert('저장 완료되었습니다.');
			// location.reload();
		} catch (err) {
			console.error(err);
			alert(err.message || '업데이트 중 오류가 발생했습니다.');
		}
	}
	// ===== 저장(업데이트) 버튼 연결 교체 =====
	(function attachUpdateButton() {
		const updateBtn = byId('updateProductBtn') || byId('submitProductBtn');
		if (updateBtn) {
			updateBtn.addEventListener('click', doUpdateFlow);
		} else {
			window.__productDetail__ = window.__productDetail__ || {};
			window.__productDetail__.doUpdate = doUpdateFlow;
			console.warn('업데이트 버튼을 찾지 못했습니다. 필요 시 window.__productDetail__.doUpdate() 호출.');
		}
	})();
});
