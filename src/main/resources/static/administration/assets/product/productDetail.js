// /administration/assets/product/productDetail.js
document.addEventListener('DOMContentLoaded', async function() {
	// ========= 공통 유틸 =========
	const $$ = (sel, root = document) => root.querySelector(sel);
	const $$$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));
	const byId = (id) => document.getElementById(id);
	const setVal = (sel, v) => {
		const el = document.querySelector(sel);
		if (el) el.value = v ?? '';
	};

	// URL에서 productId 추출 (예: /administration/product/product/productDetail/123)
	const path = window.location.pathname;
	const idMatch = path.match(/\/productDetail\/(\d+)/);
	const productId = idMatch ? idMatch[1] : null;
	if (!productId) {
		alert('잘못된 접근입니다. (제품 ID 없음)');
		return;
	}

	// API 엔드포인트
	const DETAIL_API = `/api/product/${productId}/detail`;

	// ===== Insert 화면과 동일한 상태 변수들 =====
	let selectedCategories = []; // 외부 카테고리
	let internalCategorySmallId = ''; // 내부 소분류 id
	let selectedBrand = null; // 브랜드
	let keywords = []; // 키워드
	let optionGroups = []; // 옵션 그룹
	let relatedProducts = []; // 관련상품
	let bundleProducts = []; // 추가구성상품
	let selectedDiscounts = []; // 프로모션
	let dealerDiscounts = {}; // 딜러 등급 할인
	let extraFields = []; // 추가입력필드

	// ====== CKEditor 인스턴스 관리 ======
	const ckeInstances = {}; // 질문용
	let detailEditor = null; // 상세설명

	// ====== 공통 표시항목 영역 ======
	const displayContainer = byId('product-manager-display-options');

	// ====== 키워드 렌더 ======
	const keywordList = byId('product-keyword-list');
	function renderKeywordList() {
		keywordList.innerHTML = '';
		keywords.forEach((kw, idx) => {
			const badge = document.createElement('div');
			badge.className =
				'badge bg-info text-dark px-2 py-2 d-flex align-items-center mt-2';
			badge.style.fontSize = '14px';
			badge.innerHTML = `<span>${kw}</span><span class="ms-2" style="cursor:pointer;" title="삭제">&times;</span>`;
			badge.lastElementChild.onclick = () => {
				keywords.splice(idx, 1);
				renderKeywordList();
			};
			keywordList.appendChild(badge);
		});
	}

	// ====== (외부) 카테고리 렌더 ======
	const selectedList = byId('selected-category-list');
	function renderSelectedCategories() {
		selectedList.innerHTML = '';
		selectedCategories.forEach((c, idx) => {
			const div = document.createElement('div');
			div.className =
				'badge bg-primary text-white px-2 py-2 me-2 d-flex align-items-center';
			div.innerHTML = `${c.largeName} &gt; ${c.mediumName} &gt; <b>${c.smallName}</b>
        <span class="ms-2" style="cursor:pointer;" title="삭제">[삭제]</span>`;
			div.querySelector('span').onclick = () => {
				selectedCategories.splice(idx, 1);
				renderSelectedCategories();
			};
			selectedList.appendChild(div);
		});
	}

	// ====== 브랜드 렌더 ======
	const brandSelectedArea = byId('brand-selected-area');
	function renderSelectedBrand() {
		brandSelectedArea.innerHTML = '';
		if (!selectedBrand) return;
		const imgUrl =
			selectedBrand.imageUrl ||
			selectedBrand.imageRoad ||
			'/assets/brand-default.png';
		brandSelectedArea.innerHTML = `
      <div class="d-flex align-items-center bg-light p-2 rounded">
        <img src="${imgUrl}" style="width:40px;height:40px;object-fit:cover;border-radius:6px;">
        <span class="ms-2">${selectedBrand.name}</span>
        <button type="button" class="btn btn-outline-danger btn-sm ms-2" id="remove-brand-btn">삭제</button>
      </div>`;
		const rm = byId('remove-brand-btn');
		if (rm) rm.onclick = () => {
			selectedBrand = null;
			renderSelectedBrand();
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
				group.options.push({
					name: '',
					value: '',
					extraPrice: '',
					sign: 'PLUS',
					sortOrder: group.options.length + 1
				});
				renderOptionGroups();
			};

			const optionsContainer = groupDiv.querySelector(
				`#option-group-options-${groupIdx}`
			);
			group.options.forEach((opt, optIdx) => {
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

	// ====== 관련/번들/프로모션/딜러할인/추가필드 메인 렌더 ======
	function renderRelatedProductsMain() {
		const list = byId('related-products-list');
		list.innerHTML = '';
		relatedProducts.forEach((p, idx) => {
			const badge = document.createElement('div');
			badge.className =
				'badge bg-warning text-dark px-2 py-2 d-flex align-items-center';
			const typeLabel = p.type === 'ONEWAY' ? '일방' : '상호';
			badge.innerHTML = `${p.name} <span class="ms-2 small">#${idx}</span> <span class="ms-2 small">(${typeLabel})</span>
                         <span class="ms-2" style="cursor:pointer;" title="삭제">&times;</span>`;
			badge.lastElementChild.onclick = () => {
				relatedProducts.splice(idx, 1);
				renderRelatedProductsMain();
			};
			list.appendChild(badge);
		});
	}
	function renderBundleProductsMain() {
		const list = byId('bundle-products-list');
		list.innerHTML = '';
		bundleProducts.forEach((p, idx) => {
			const badge = document.createElement('div');
			badge.className =
				'badge bg-success text-white px-2 py-2 d-flex align-items-center';
			badge.innerHTML = `${p.name} <span class="ms-2 small">#${idx}</span>
                         <span class="ms-2" style="cursor:pointer;" title="삭제">&times;</span>`;
			badge.lastElementChild.onclick = () => {
				bundleProducts.splice(idx, 1);
				renderBundleProductsMain();
			};
			list.appendChild(badge);
		});
	}
	function renderSelectedDiscountsMain() {
		const wrap = byId('selected-discount-list');
		wrap.innerHTML = '';
		selectedDiscounts.forEach((d, idx) => {
			const badge = document.createElement('div');
			badge.className =
				'badge bg-danger text-white px-2 py-2 d-flex align-items-center';
			badge.innerHTML = `<span>${d.name} (${d.typeLabel || d.type})</span><span class="ms-2" style="cursor:pointer;" title="삭제">&times;</span>`;
			badge.lastElementChild.onclick = () => {
				selectedDiscounts.splice(idx, 1);
				renderSelectedDiscountsMain();
			};
			wrap.appendChild(badge);
		});
	}
	function renderDealerDiscounts() {
		const container = byId('dealer-discount-list');
		const buttons = byId('dealer-discount-buttons');
		container.innerHTML = '';
		// 버튼 초기화
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
			col.querySelector('input').oninput = function() {
				dealerDiscounts[grade] = this.value;
			};
			col.querySelector('button').onclick = function() {
				delete dealerDiscounts[grade];
				const btn = buttons.querySelector(`button[data-grade="${grade}"]`);
				if (btn) btn.disabled = false;
				renderDealerDiscounts();
			};
			container.appendChild(col);

			// 버튼 잠그기
			const btn = buttons.querySelector(`button[data-grade="${grade}"]`);
			if (btn) btn.disabled = true;
		});
	}

	// ====== 이미지 미리보기(서버 저장본) 초기 렌더 ======
	const mainPreview = byId('product-manager-main-image-preview');
	const subPreview = byId('product-manager-sub-image-preview');
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
			div.querySelector('button').onclick = () => {
				mainPreview.innerHTML = '';
			};
			wrap.appendChild(div);
			mainPreview.appendChild(wrap);
		}
		// 추가이미지
		subPreview.innerHTML = '';
		(imagesDTO?.subImageUrls || []).forEach((url) => {
			const div = document.createElement('div');
			div.className = 'image-preview-thumb position-relative';
			div.style.width = '100px';
			div.style.height = '100px';
			div.style.marginRight = '8px';
			div.innerHTML = `<img src="${url}" style="width:100%;height:100%;object-fit:cover;">
                       <button class="btn-close btn-sm" style="position:absolute;top:0;right:0;z-index:2;" aria-label="Remove"></button>`;
			div.querySelector('button').onclick = () => {
				div.remove();
			};
			subPreview.appendChild(div);
		});
	}

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
          ${(q.options || []).map((o) => `<option value="${o.value}">${o.value}</option>`).join('')}
        </select>`;
			case 'FILE':
				return `<input type="file" class="form-control form-control-sm" name="question_${q.id}" ${requiredAttr}>`;
			case 'CKEDITOR':
				return `<textarea class="form-control" name="question_${q.id}" id="${editorId}" rows="3"
                 ${requiredAttr} data-type="question" data-key="question_${q.id}"></textarea>`;
			default:
				return `<input type="text" class="form-control form-control-sm" disabled placeholder="지원되지 않는 타입">`;
		}
	}

	function bindAnswers(questions, answers) {
		// questionId → 답변 DTO
		const answerMap = {};
		(answers || []).forEach((a) => {
			answerMap[a.questionId] = a;
		});

		// 입력값 주입
		questions.forEach((q) => {
			const name = `question_${q.id}`;
			const ans = answerMap[q.id];
			if (!ans) return;
			if (q.type === 'CKEDITOR') {
				const editorId = `editor-question-${q.id}`;
				const editor = ckeInstances[editorId];
				if (editor) editor.setData(ans.value || '');
			} else if (q.type === 'FILE') {
				// 파일은 value를 직접 세팅할 수 없으니, 기존 파일 URL들을 안내로 표시
				if (Array.isArray(ans.fileUrls) && ans.fileUrls.length) {
					const input = $$(`[name="${name}"]`);
					if (input) {
						const helper = document.createElement('div');
						helper.className = 'form-text';
						helper.innerHTML = `기존 파일: ${ans.fileUrls
							.map((u) => `<a href="${u}" target="_blank">링크</a>`)
							.join(', ')}`;
						input.insertAdjacentElement('afterend', helper);
					}
				}
			} else {
				const el = $$(`[name="${name}"]`);
				if (!el) return;
				el.value = ans.value || '';
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
				toolbar: {
					items: [
						'heading',
						'|',
						'bold',
						'italic',
						'underline',
						'strikethrough',
						'highlight',
						'fontColor',
						'fontBackgroundColor',
						'|',
						'link',
						'bulletedList',
						'numberedList',
						'blockQuote',
						'|',
						'insertTable',
						'imageUpload',
						'mediaEmbed',
						'|',
						'undo',
						'redo',
						'alignment',
						'outdent',
						'indent'
					]
				},
				image: {
					toolbar: [
						'imageTextAlternative',
						'imageStyle:full',
						'imageStyle:side',
						'linkImage'
					],
					styles: ['full', 'side'],
					resizeUnit: 'px'
				},
				table: {
					contentToolbar: [
						'tableColumn',
						'tableRow',
						'mergeTableCells',
						'tableCellProperties',
						'tableProperties'
					]
				},
				mediaEmbed: { previewsInData: true },
				language: 'ko'
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
					toolbar: {
						items: [
							'heading',
							'|',
							'bold',
							'italic',
							'underline',
							'strikethrough',
							'highlight',
							'fontColor',
							'fontBackgroundColor',
							'|',
							'link',
							'bulletedList',
							'numberedList',
							'blockQuote',
							'|',
							'insertTable',
							'imageUpload',
							'mediaEmbed',
							'|',
							'undo',
							'redo',
							'alignment',
							'outdent',
							'indent'
						]
					},
					image: {
						toolbar: [
							'imageTextAlternative',
							'imageStyle:full',
							'imageStyle:side',
							'linkImage'
						],
						styles: ['full', 'side'],
						resizeUnit: 'px'
					},
					table: {
						contentToolbar: [
							'tableColumn',
							'tableRow',
							'mergeTableCells',
							'tableCellProperties',
							'tableProperties'
						]
					},
					mediaEmbed: { previewsInData: true },
					language: 'ko'
				}).then((editor) => {
					ckeInstances[tId] = editor;
				})
			);
		});
		await Promise.all(tasks);
	}

	// ====== 상세 데이터 로딩 & 바인딩 ======
	try {
		const res = await fetch(DETAIL_API);
		if (!res.ok) throw new Error(`상세 API 오류: HTTP ${res.status}`);
		const d = await res.json(); // ProductDetailReadResponseDTO

		// 1) 기본/상태
		if (d.displayStatus) {
			const ds = $$(`input[name="displayStatus"][value="${d.displayStatus}"]`);
			if (ds) ds.checked = true;
		}
		if (d.saleStatus) {
			const ss = $$(`input[name="saleStatus"][value="${d.saleStatus}"]`);
			if (ss) ss.checked = true;
		}
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
			if (d.pricePolicy.priceExposeTarget) {
				const pe = $$(
					`input[name="priceExposeTarget"][value="${d.pricePolicy.priceExposeTarget}"]`
				);
				if (pe) pe.checked = true;
			}
			if (d.pricePolicy.usePriceReplacementText) {
				const chk = byId('usePriceReplacementText');
				if (chk) {
					chk.checked = true;
					const area = byId('priceReplacementArea');
					if (area) area.style.display = '';
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

		// 4) 내부 카테고리 (smallId만 저장)
		internalCategorySmallId = d.internalCategorySmallId
			? String(d.internalCategorySmallId)
			: '';

		// 5) 브랜드
		if (d.brand) {
			selectedBrand = {
				id: d.brand.id,
				name: d.brand.name,
				imageUrl: d.brand.imageUrl
			};
			renderSelectedBrand();
		}

		// 6) 키워드
		keywords = Array.isArray(d.keywords) ? d.keywords.slice() : [];
		renderKeywordList();

		// 7) 공통표시항목 & 답변
		if (Array.isArray(d.displayQuestions)) {
			// 렌더(질문 정의)
			displayContainer.innerHTML = '';
			d.displayQuestions.forEach((q) => {
				const colClass =
					q.type === 'TEXTAREA' || q.type === 'CKEDITOR'
						? 'col-12 mb-2'
						: 'col-6 mb-2';
				const div = document.createElement('div');
				div.className = colClass + ' d-flex flex-column justify-content-end';
				const requiredMark = q.required ? ' <span class="text-danger">*</span>' : '';
				div.innerHTML = `<label class="form-label mb-1">${q.label}${requiredMark}</label>${makeQuestionInput(q)}`;
				displayContainer.appendChild(div);
			});
			// CKEditor mount → 답변 주입
			await mountQuestionEditors(d.displayQuestions);
			bindAnswers(d.displayQuestions, d.answers || []);
		}

		// 8) 상세설명
		await mountDetailEditor(d.detailHtml || '');

		// 9) 이미지 (서버 저장본을 미리보기로 표시; 새로 선택하면 insert와 동일하게 교체)
		renderServerImages(d.images || null);

		// 10) 아이콘
		if (d.icon) {
			const useIconPeriod = byId('use-icon-period');
			const iconStart = byId('icon-start');
			const iconEnd = byId('icon-end');
			if (useIconPeriod) useIconPeriod.checked = !!d.icon.usePeriod;
			if (iconStart) iconStart.disabled = !useIconPeriod?.checked;
			if (iconEnd) iconEnd.disabled = !useIconPeriod?.checked;
			if (d.icon.startDate && iconStart) iconStart.value = d.icon.startDate;
			if (d.icon.endDate && iconEnd) iconEnd.value = d.icon.endDate;
			// 기존 아이콘 이미지는 파일 input에 세팅 불가 → 필요 시 별도 안내만
			if (d.icon.imageUrl) {
				const hint = document.createElement('div');
				hint.className = 'form-text';
				hint.innerHTML = `기존 아이콘: <a href="${d.icon.imageUrl}" target="_blank">${d.icon.imageUrl}</a>`;
				const icoInp = byId('product-icon-image');
				if (icoInp) icoInp.insertAdjacentElement('afterend', hint);
			}
		}

		// 11) 옵션
		optionGroups = (d.optionGroups || []).map((g) => ({
			name: g.name,
			options: (g.options || []).map((o) => ({
				name: o.name,
				value: o.value,
				extraPrice: o.extraPrice,
				sign: o.sign || 'PLUS',
				sortOrder: o.sortOrder
			}))
		}));
		renderOptionGroups();

		// 12) 관련/번들
		relatedProducts = (d.relatedProducts || []).map((r, i) => ({
			id: r.id,
			name: r.name,
			sortOrder: r.sortOrder ?? i,
			type: r.type || 'RECIPROCAL'
		}));
		bundleProducts = (d.bundleProducts || []).map((b, i) => ({
			id: b.id,
			name: b.name,
			sortOrder: b.sortOrder ?? i
		}));
		renderRelatedProductsMain();
		renderBundleProductsMain();

		// 13) 딜러 등급별 할인
		dealerDiscounts = d.dealerDiscounts || {};
		renderDealerDiscounts();

		// 14) 프로모션
		selectedDiscounts = (d.discounts || []).map((x) => ({
			id: x.id,
			name: x.name,
			type: x.type,
			term: x.term,
			active: x.active,
			startDate: x.startDate,
			endDate: x.endDate,
			target: x.target,
			couponPolicy: x.couponPolicy,
			typeLabel: x.typeLabel,
			termLabel: x.termLabel
		}));
		renderSelectedDiscountsMain();

		// 15) 추가 입력필드
		extraFields = (d.extraFields || []).map((e) => ({
			label: e.label,
			value: e.value
		}));
		// 화면에 반영 (insert 렌더 방식 준용)
		const extraList = byId('product-manager-extra-field-list');
		extraList.innerHTML = '';
		extraFields.forEach((field, idx) => {
			const row = document.createElement('div');
			row.className = 'input-group mb-2';
			row.innerHTML = `
        <input type="text" class="form-control form-control-sm" name="extraFields[${idx}].label" placeholder="질문명" value="${field.label || ''}" required>
        <input type="text" class="form-control form-control-sm" name="extraFields[${idx}].value" placeholder="답변값" value="${field.value || ''}" required>
        <button type="button" class="btn btn-outline-danger btn-sm" title="삭제">×</button>`;
			row.querySelector('button').onclick = () => {
				extraFields.splice(idx, 1);
				row.remove();
			};
			extraList.appendChild(row);
		});

		// 16) 내부 카테고리 Select 미리 선택 (smallId만 있으므로 체인은 사용자 선택 시 로드)
		if (internalCategorySmallId) {
			const helper = document.createElement('div');
			helper.className = 'form-text';
			helper.textContent = `현재 내부 소분류 ID: ${internalCategorySmallId} (필요시 상단 셀렉터에서 다시 선택하세요)`;
			const smallSel = byId('internal-small-select');
			if (smallSel) smallSel.insertAdjacentElement('afterend', helper);
		}

		// 저장/수정 동작은 필요 시 별도 구현(현재 파일은 상세 로딩/바인딩 전용)
	} catch (e) {
		console.error(e);
		alert(e.message || '상세 로딩 중 오류');
	}
});
