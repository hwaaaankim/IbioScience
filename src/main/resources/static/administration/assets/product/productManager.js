document.addEventListener("DOMContentLoaded", function() {
	// === 기존 변수/상태 선언 ===
	const largeList = document.getElementById('category-large-list');
	const mediumList = document.getElementById('category-medium-list');
	const smallList = document.getElementById('category-small-list');
	const selectedList = document.getElementById('selected-category-list');
	let selectedCategories = [];

	let largeCategoryMap = {};
	let mediumCategoryMap = {};

	let keywords = [];
	const keywordInput = document.getElementById('product-keyword-input');
	const addKeywordBtn = document.getElementById('add-keyword-btn');
	const keywordList = document.getElementById('product-keyword-list');

	const relatedModal = document.getElementById('relatedProductModal');
	const relatedOpenBtn = document.getElementById('open-related-modal-btn');
	const relatedCloseBtns = relatedModal.querySelectorAll('.related-modal-close, .related-modal-cancel, .related-modal-overlay');
	const relatedLargeSelect = document.getElementById('related-large-select');
	const relatedMediumSelect = document.getElementById('related-medium-select');
	const relatedSmallSelect = document.getElementById('related-small-select');
	const relatedKeywordInput = document.getElementById('related-product-keyword');
	const relatedModalProductList = document.getElementById('related-modal-product-list');
	const addRelatedProductBtn = document.getElementById('add-related-product-btn');
	const relatedTypeSelect = document.getElementById('related-modal-type');
	let relatedProductList = [], relatedSelectedProductIds = new Set();

	const bundleModal = document.getElementById('bundleProductModal');
	const bundleOpenBtn = document.getElementById('open-bundle-modal-btn');
	const bundleCloseBtns = bundleModal.querySelectorAll('.bundle-modal-close, .bundle-modal-cancel, .bundle-modal-overlay');
	const bundleLargeSelect = document.getElementById('bundle-large-select');
	const bundleMediumSelect = document.getElementById('bundle-medium-select');
	const bundleSmallSelect = document.getElementById('bundle-small-select');
	const bundleKeywordInput = document.getElementById('bundle-product-keyword');
	const bundleModalProductList = document.getElementById('bundle-modal-product-list');
	const addBundleProductBtn = document.getElementById('add-bundle-product-btn');
	let bundleProductList = [], bundleSelectedProductIds = new Set();

	let relatedProducts = [];
	let bundleProducts = [];

	// ----------- 모달 관련 (active 클래스 시 display block 보장)
	// (CSS에 아래 예시 추가 필요)
	// .related-modal-root.active, .bundle-modal-root.active { display: block !important; }
	// .related-modal-root, .bundle-modal-root { display: none; }
	// -----------

	// --- 모달 대분류 옵션 그리기 (중분류 수 표시)
	function fetchAndRenderLargeOptions(selectEl, callback) {
		fetch('/api/category/list-large')
			.then(res => res.json())
			.then(list => {
				selectEl.innerHTML = `<option value="">대분류</option>`;
				list.forEach(cat => {
					selectEl.innerHTML += `<option value="${cat.id}">${cat.name} (${cat.mediumCount ?? 0})</option>`;
				});
				if (callback) callback(list);
			});
	}

	// --- 모달 중분류 옵션 그리기 (소분류 수 표시)
	function fetchAndRenderMediumOptions(selectEl, largeId, resetSmallSelect) {
		selectEl.innerHTML = `<option value="">중분류</option>`;
		if (!largeId) {
			if (resetSmallSelect) resetSmallSelect.innerHTML = `<option value="">소분류</option>`;
			return;
		}
		fetch(`/api/category/list-medium?largeId=${largeId}`)
			.then(res => res.json())
			.then(list => {
				list.forEach(m => {
					selectEl.innerHTML += `<option value="${m.id}">${m.name} (${m.smallCount ?? 0})</option>`;
				});
				if (resetSmallSelect) resetSmallSelect.innerHTML = `<option value="">소분류</option>`;
			});
	}

	function fetchAndRenderSmallOptions(selectEl, mediumId) {
		selectEl.innerHTML = `<option value="">소분류</option>`;
		if (!mediumId) return;
		// 변경: 기존 list-small → list-small-with-product-count
		fetch(`/api/category/list-small-with-product-count?mediumId=${mediumId}`)
			.then(res => res.json())
			.then(list => {
				list.forEach(s => {
					// 제품수가 없는 경우 0, 제품수는 productCount
					selectEl.innerHTML += `<option value="${s.id}">${s.name} (${s.productCount ?? 0})</option>`;
				});
			});
	}

	function fetchProductListBySmall(smallId, callback) {
		if (!smallId) {
			callback([]);
			return;
		}
		fetch(`/api/product/list-simple?smallId=${smallId}`)
			.then(res => res.json())
			.then(list => callback(list));
	}

	// ---- 관련상품 모달 컨트롤러 ----
	relatedOpenBtn.onclick = function() {
		relatedModal.classList.add('active');
		fetchAndRenderLargeOptions(relatedLargeSelect, () => {
			relatedMediumSelect.innerHTML = `<option value="">중분류</option>`;
			relatedSmallSelect.innerHTML = `<option value="">소분류</option>`;
			relatedKeywordInput.value = '';
			relatedProductList = [];
			relatedSelectedProductIds = new Set();
			renderRelatedModalProductList();
		});
	};
	relatedLargeSelect.onchange = function() {
		fetchAndRenderMediumOptions(relatedMediumSelect, this.value, relatedSmallSelect);
		relatedProductList = [];
		renderRelatedModalProductList();
	};
	relatedMediumSelect.onchange = function() {
		fetchAndRenderSmallOptions(relatedSmallSelect, this.value);
		relatedProductList = [];
		renderRelatedModalProductList();
	};
	relatedSmallSelect.onchange = function() {
		const smallId = relatedSmallSelect.value;
		// API 호출 및 select 옵션 렌더
		fetchProductListBySmall(smallId, function(list) {
			relatedProductList = list || [];
			renderRelatedModalProductList();
		});
	};
	// 제품리스트를 select로 표시
	function renderRelatedModalProductList() {
		relatedModalProductList.innerHTML = '';
		if (!relatedProductList || relatedProductList.length === 0) {
			relatedModalProductList.innerHTML = '<div class="text-muted text-center">제품이 없습니다.</div>';
			return;
		}
		// 리스트(ul)로 표시 + 삭제(x)
		const ul = document.createElement('ul');
		ul.className = 'list-group mb-2';
		relatedProductList.forEach(product => {
			const li = document.createElement('li');
			li.className = 'list-group-item d-flex justify-content-between align-items-center';
			// 체크박스 (다중 선택 가능)
			const checkbox = document.createElement('input');
			checkbox.type = 'checkbox';
			checkbox.className = 'form-check-input me-2';
			checkbox.value = product.id;
			checkbox.checked = relatedSelectedProductIds.has(Number(product.id));
			checkbox.onchange = function() {
				if (this.checked) relatedSelectedProductIds.add(Number(this.value));
				else relatedSelectedProductIds.delete(Number(this.value));
			};

			// 제품명 및 코드
			const label = document.createElement('span');
			label.textContent = `[${product.code}] ${product.name}`;

			// x(삭제)
			const delBtn = document.createElement('button');
			delBtn.className = 'btn btn-outline-danger btn-sm ms-2';
			delBtn.type = 'button';
			delBtn.innerHTML = '&times;';
			delBtn.onclick = function() {
				// 제품 리스트에서 제거
				relatedProductList = relatedProductList.filter(p => p.id !== product.id);
				// 선택에서도 제거
				relatedSelectedProductIds.delete(product.id);
				renderRelatedModalProductList();
			};

			li.appendChild(checkbox);
			li.appendChild(label);
			li.appendChild(delBtn);
			ul.appendChild(li);
		});
		relatedModalProductList.appendChild(ul);
	}

	// 제품 추가 버튼 클릭
	addRelatedProductBtn.onclick = function() {
		// 선택된 항목만 바깥 리스트에 추가
		const type = relatedTypeSelect.value;
		const selectedIds = Array.from(relatedSelectedProductIds);

		selectedIds.forEach(productId => {
			const product = relatedProductList.find(p => p.id === productId);
			if (product && !relatedProducts.some(rp => rp.id === productId)) {
				relatedProducts.push({ id: product.id, name: product.name, type });
			}
		});
		renderRelatedProducts();
		relatedModal.classList.remove('active');
		// 선택 초기화
		relatedSelectedProductIds.clear();
	};

	relatedCloseBtns.forEach(btn => btn.onclick = () => relatedModal.classList.remove('active'));

	function renderRelatedProducts() {
		const list = document.getElementById('related-products-list');
		list.innerHTML = '';
		relatedProducts.forEach((p, idx) => {
			const badge = document.createElement('div');
			badge.className = 'badge bg-warning text-dark px-2 py-2 d-flex align-items-center';
			badge.innerHTML = `${p.name} (${p.type === 'RECIPROCAL' ? '상호' : '일방'})<span class="ms-2" style="cursor:pointer;" title="삭제">&times;</span>`;
			badge.querySelector('span').onclick = () => {
				relatedProducts.splice(idx, 1);
				renderRelatedProducts();
			};
			list.appendChild(badge);
		});
	}

	// ---- 추가구성상품 모달 컨트롤러 ----
	bundleOpenBtn.onclick = function() {
		bundleModal.classList.add('active');
		fetchAndRenderLargeOptions(bundleLargeSelect, () => {
			bundleMediumSelect.innerHTML = `<option value="">중분류</option>`;
			bundleSmallSelect.innerHTML = `<option value="">소분류</option>`;
			bundleKeywordInput.value = '';
			bundleProductList = [];
			bundleSelectedProductIds = new Set();
			renderBundleModalProductList();
		});
	};
	bundleLargeSelect.onchange = function() {
		fetchAndRenderMediumOptions(bundleMediumSelect, this.value, bundleSmallSelect);
		bundleProductList = [];
		renderBundleModalProductList();
	};
	bundleMediumSelect.onchange = function() {
		fetchAndRenderSmallOptions(bundleSmallSelect, this.value);
		bundleProductList = [];
		renderBundleModalProductList();
	};
	bundleSmallSelect.onchange = function() {
		const smallId = bundleSmallSelect.value;
		fetchProductListBySmall(smallId, function(list) {
			bundleProductList = list || [];
			renderBundleModalProductList();
		});
	};

	function renderBundleModalProductList() {
		bundleModalProductList.innerHTML = '';
		if (!bundleProductList || bundleProductList.length === 0) {
			bundleModalProductList.innerHTML = '<div class="text-muted text-center">제품이 없습니다.</div>';
			return;
		}
		// 체크박스 + x 버튼 리스트 렌더
		const ul = document.createElement('ul');
		ul.className = 'list-group mb-2';
		bundleProductList.forEach(product => {
			const li = document.createElement('li');
			li.className = 'list-group-item d-flex justify-content-between align-items-center';

			const checkbox = document.createElement('input');
			checkbox.type = 'checkbox';
			checkbox.className = 'form-check-input me-2';
			checkbox.value = product.id;
			checkbox.checked = bundleSelectedProductIds.has(Number(product.id));
			checkbox.onchange = function() {
				if (this.checked) bundleSelectedProductIds.add(Number(this.value));
				else bundleSelectedProductIds.delete(Number(this.value));
			};

			const label = document.createElement('span');
			label.textContent = `[${product.code}] ${product.name}`;

			const delBtn = document.createElement('button');
			delBtn.className = 'btn btn-outline-danger btn-sm ms-2';
			delBtn.type = 'button';
			delBtn.innerHTML = '&times;';
			delBtn.onclick = function() {
				bundleProductList = bundleProductList.filter(p => p.id !== product.id);
				bundleSelectedProductIds.delete(product.id);
				renderBundleModalProductList();
			};

			li.appendChild(checkbox);
			li.appendChild(label);
			li.appendChild(delBtn);
			ul.appendChild(li);
		});
		bundleModalProductList.appendChild(ul);
	}

	addBundleProductBtn.onclick = function() {
		const selectedIds = Array.from(bundleSelectedProductIds);
		selectedIds.forEach(productId => {
			const product = bundleProductList.find(p => p.id === productId);
			if (product && !bundleProducts.some(bp => bp.id === productId)) {
				bundleProducts.push({ id: product.id, name: product.name });
			}
		});
		renderBundleProducts();
		bundleModal.classList.remove('active');
		bundleSelectedProductIds.clear();
	};

	bundleCloseBtns.forEach(btn => btn.onclick = () => bundleModal.classList.remove('active'));

	function renderBundleProducts() {
		const list = document.getElementById('bundle-products-list');
		list.innerHTML = '';
		bundleProducts.forEach((p, idx) => {
			const badge = document.createElement('div');
			badge.className = 'badge bg-success text-white px-2 py-2 d-flex align-items-center';
			badge.innerHTML = `${p.name}<span class="ms-2" style="cursor:pointer;" title="삭제">&times;</span>`;
			badge.querySelector('span').onclick = () => {
				bundleProducts.splice(idx, 1);
				renderBundleProducts();
			};
			list.appendChild(badge);
		});
	}
	document.getElementById('bundle-add-product-btn').onclick = function() {
		const input = document.getElementById('bundle-add-product-name');
		const name = input.value.trim();
		if (!name) {
			alert('제품명을 입력하세요.');
			return;
		}
		// 중복 방지
		if (bundleProductList.some(p => p.name === name)) {
			alert('이미 등록된 제품입니다.');
			return;
		}
		let newId = bundleProductList.reduce((max, cur) => Math.max(max, Number(cur.id)), 0) + 1;
		let newCode = 'TMP' + newId;

		bundleProductList.push({
			id: newId,
			code: newCode,
			name: name
		});
		bundleSelectedProductIds.add(newId); // **자동 선택 반영**

		input.value = '';
		renderBundleModalProductList();
	};


	document.getElementById('related-add-product-btn').onclick = function() {
		const input = document.getElementById('related-add-product-name');
		const name = input.value.trim();
		if (!name) {
			alert('제품명을 입력하세요.');
			return;
		}
		// 중복 방지: 이름 중복 등록 안 함
		if (relatedProductList.some(p => p.name === name)) {
			alert('이미 등록된 제품입니다.');
			return;
		}
		// 임의 id/code 생성 (중복방지)
		let newId = relatedProductList.reduce((max, cur) => Math.max(max, Number(cur.id)), 0) + 1;
		let newCode = 'TMP' + newId;
		// 데이터에 추가
		relatedProductList.push({
			id: newId,
			code: newCode,
			name: name
		});
		// **추가: 새로 등록한 제품을 바로 선택상태로**
		relatedSelectedProductIds.add(newId);

		// 렌더링
		renderRelatedModalProductList();
		input.value = '';
	};

	// 스크롤 스타일 적용(최대 5개) - CSS로도 적용 가능하지만 JS로 보장
	[largeList, mediumList, smallList].forEach(listEl => {
		listEl.style.maxHeight = '240px';
		listEl.style.overflowY = 'auto';
	});

	// 대분류 로딩 및 중분류 개수 badge 표시
	fetch('/api/category/list-large')
		.then(res => res.json())
		.then(list => {
			largeList.innerHTML = '';
			list.forEach(large => {
				let li = document.createElement('li');
				li.className = 'list-group-item list-group-item-action category-large-item d-flex justify-content-between align-items-center';
				li.dataset.id = large.id;
				// mediumCount 바로 사용!
				li.innerHTML = `<span>${large.name}</span>
                <span class="badge bg-light text-dark ms-2" data-large-badge="${large.id}">${large.mediumCount ?? 0}</span>`;
				largeCategoryMap[large.id] = large.name;
				largeList.appendChild(li);
			});
		});

	largeList.addEventListener('click', function(e) {
		const li = e.target.closest('.category-large-item');
		if (li) {
			const largeId = li.dataset.id;
			fetch(`/api/category/list-medium?largeId=${largeId}`)
				.then(res => res.json())
				.then(list => {
					mediumList.innerHTML = '';
					list.forEach(m => {
						let li = document.createElement('li');
						li.className = 'list-group-item list-group-item-action category-medium-item d-flex justify-content-between align-items-center';
						li.dataset.id = m.id;
						// smallCount 바로 사용!
						li.innerHTML = `<span>${m.name}</span>
                        <span class="badge bg-light text-dark ms-2" data-medium-badge="${m.id}">${m.smallCount ?? 0}</span>`;
						mediumCategoryMap[m.id] = { name: m.name, largeId: largeId };
						mediumList.appendChild(li);
					});
					smallList.innerHTML = '';
				});
		}
	});

	mediumList.addEventListener('click', function(e) {
		const li = e.target.closest('.category-medium-item');
		if (li) {
			const mediumId = li.dataset.id;
			fetch(`/api/category/list-small?mediumId=${mediumId}`)
				.then(res => res.json())
				.then(list => {
					smallList.innerHTML = '';
					list.forEach(s => {
						let li = document.createElement('li');
						li.textContent = s.name;
						li.className = 'list-group-item list-group-item-action category-small-item';
						li.dataset.id = s.id;
						li.dataset.mediumId = mediumId;
						smallList.appendChild(li);
					});
				});
		}
	});

	smallList.addEventListener('click', function(e) {
		if (e.target.classList.contains('category-small-item')) {
			const smallId = e.target.dataset.id;
			const mediumId = e.target.dataset.mediumId;
			const mediumInfo = mediumCategoryMap[mediumId] || {};
			const largeId = mediumInfo.largeId;
			const largeName = largeCategoryMap[largeId] || '';
			const mediumName = mediumInfo.name || '';
			const smallName = e.target.textContent;
			if (!selectedCategories.some(sc => sc.id == smallId)) {
				selectedCategories.push({
					id: smallId,
					largeId,
					largeName,
					mediumId,
					mediumName,
					smallName
				});
				renderSelectedCategories();
			}
		}
	});

	function renderSelectedCategories() {
		selectedList.innerHTML = '';
		selectedCategories.forEach((c, idx) => {
			let div = document.createElement('div');
			div.className = 'badge bg-primary text-white px-2 py-2 me-2 d-flex align-items-center';
			div.innerHTML =
				`${c.largeName} &gt; ${c.mediumName} &gt; <b>${c.smallName}</b>
				<span class="ms-2" style="cursor:pointer;" title="삭제">[삭제]</span>`;
			div.querySelector('span').onclick = () => {
				selectedCategories.splice(idx, 1);
				renderSelectedCategories();
			}
			selectedList.appendChild(div);
		});
	}

	// ===== 2. 공통 표시옵션(질문) 동적 랜더링 및 CKEditor mount =====
	let ckeInstances = {};

	function makeQuestionInput(option) {
		const requiredAttr = option.required ? 'required' : '';
		const editorId = option.type === 'CKEDITOR' ? `editor-question-${option.id}` : '';
		let inputHtml = '';
		switch (option.type) {
			case 'INPUT':
				inputHtml = `<input type="text" class="form-control form-control-sm" name="question_${option.id}" placeholder="${option.placeholder || ''}" ${requiredAttr}>`;
				break;
			case 'TEXTAREA':
				inputHtml = `<textarea class="form-control form-control-sm" name="question_${option.id}" rows="2" placeholder="${option.placeholder || ''}" ${requiredAttr}></textarea>`;
				break;
			case 'SELECT':
				inputHtml = `<select class="form-select form-select-sm" name="question_${option.id}" ${requiredAttr}>`
					+ (Array.isArray(option.options) && option.options.length > 0
						? option.options.map(opt => {
							if (typeof opt === 'object' && opt !== null) {
								if (opt.value !== undefined && opt.label !== undefined) {
									return `<option value="${opt.value}">${opt.label}</option>`;
								} else if (Object.keys(opt).length === 1) {
									let key = Object.keys(opt)[0];
									return `<option value="${key}">${opt[key]}</option>`;
								} else {
									return `<option disabled>선택지 오류</option>`;
								}
							} else {
								return `<option value="${opt}">${opt}</option>`;
							}
						}).join('')
						: '<option disabled>선택지 없음</option>')
					+ `</select>`;
				break;
			case 'FILE':
				inputHtml = `<input type="file" class="form-control form-control-sm" name="question_${option.id}" ${requiredAttr}>`;
				break;
			case 'CKEDITOR':
				inputHtml = `<textarea class="form-control" name="question_${option.id}" id="${editorId}" rows="3" ${requiredAttr}></textarea>`;
				break;
			default:
				inputHtml = `<input type="text" class="form-control form-control-sm" name="question_${option.id}" placeholder="지원되지 않는 타입" disabled>`;
		}
		return inputHtml;
	}

	fetch('/api/display-questions/list-common')
		.then(res => res.json())
		.then(list => {
			const container = document.getElementById('product-manager-display-options');
			container.innerHTML = '';
			list.forEach(option => {
				let colClass = 'col-6 mb-2';
				if (option.type === 'TEXTAREA' || option.type === 'CKEDITOR') {
					colClass = 'col-12 mb-2';
				}
				const div = document.createElement('div');
				div.className = colClass + ' d-flex flex-column justify-content-end';
				div.innerHTML = `
                    <label class="form-label mb-1">${option.label ?? option.name}${option.required ? ' <span class="text-danger">*</span>' : ''}</label>
                    ${makeQuestionInput(option)}
                `;
				container.appendChild(div);
			});
			setTimeout(() => {
				list.filter(opt => opt.type === 'CKEDITOR').forEach(option => {
					const tId = `editor-question-${option.id}`;
					const textarea = document.getElementById(tId);
					if (textarea && !ckeInstances[tId] && window.ClassicEditor) {
						window.ClassicEditor.create(textarea)
							.then(editor => { ckeInstances[tId] = editor; })
							.catch(err => console.error('CKEditor5 생성 오류:', err));
					}
				});
			}, 100);
		});

	// === 3. 상세설명(이미지/HTML) 에디터 mount (id: editor-desc) ===
	let detailEditor = null;
	(function() {
		const desc = document.getElementById('editor-desc');
		if (desc && window.ClassicEditor) {
			window.ClassicEditor.create(desc)
				.then(editor => { detailEditor = editor; })
				.catch(err => console.error('CKEditor5 생성 오류:', err));
		}
	})();

	// ===== 4. 대표/추가 이미지 미리보기/삭제/순서 =====
	const mainInput = document.getElementById('product-manager-main-image');
	const mainPreview = document.getElementById('product-manager-main-image-preview');
	mainInput.addEventListener('change', function() {
		mainPreview.innerHTML = '';
		if (this.files.length > 0) {
			let file = this.files[0];
			let reader = new FileReader();
			reader.onload = e => {
				let div = document.createElement('div');
				div.className = 'image-preview-thumb position-relative';
				div.innerHTML = `
                    <img src="${e.target.result}" style="width:100%;height:100%;object-fit:cover;">
                    <button class="btn-close btn-sm" style="position:absolute;top:0;right:0;z-index:2;" aria-label="Remove"></button>`;
				div.querySelector('button').onclick = () => {
					mainInput.value = '';
					mainPreview.innerHTML = '';
				}
				mainPreview.appendChild(div);
			}
			reader.readAsDataURL(file);
		}
	});

	// 추가 이미지 (여러장, 미리보기, 삭제, Sortable)
	const subInput = document.getElementById('product-manager-sub-image');
	const subPreview = document.getElementById('product-manager-sub-image-preview');
	let subFiles = [];
	subInput.addEventListener('change', function() {
		subFiles = Array.from(this.files);
		renderSubImagePreview();
	});
	function renderSubImagePreview() {
		subPreview.innerHTML = '';
		subFiles.forEach((file, idx) => {
			let reader = new FileReader();
			reader.onload = e => {
				let div = document.createElement('div');
				div.className = 'image-preview-thumb position-relative';
				div.setAttribute('draggable', 'true');
				div.style.width = '100px'; div.style.height = '100px'; div.style.marginRight = '8px';
				div.innerHTML = `
                    <img src="${e.target.result}" style="width:100%;height:100%;object-fit:cover;">
                    <button class="btn-close btn-sm" style="position:absolute;top:0;right:0;z-index:2;" aria-label="Remove"></button>`;
				div.querySelector('button').onclick = () => {
					subFiles.splice(idx, 1);
					renderSubImagePreview();
				}
				subPreview.appendChild(div);
			}
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

	// ===== 5. 추가 입력필드 동적 추가/삭제 =====
	const extraFieldList = document.getElementById('product-manager-extra-field-list');
	const addExtraFieldBtn = document.getElementById('product-manager-add-extra-field');
	let extraFields = [];
	function renderExtraFields() {
		extraFieldList.innerHTML = '';
		extraFields.forEach((field, idx) => {
			let row = document.createElement('div');
			row.className = 'input-group mb-2';
			row.innerHTML = `
                <input type="text" class="form-control form-control-sm" name="extraFields[${idx}].label" placeholder="질문명" value="${field.label || ''}" required>
                <input type="text" class="form-control form-control-sm" name="extraFields[${idx}].value" placeholder="답변값" value="${field.value || ''}" required>
                <button type="button" class="btn btn-outline-danger btn-sm" title="삭제">×</button>
            `;
			row.querySelector('button').onclick = () => { extraFields.splice(idx, 1); renderExtraFields(); }
			extraFieldList.appendChild(row);
		});
	}
	addExtraFieldBtn.addEventListener('click', function() {
		extraFields.push({ label: '', value: '' });
		renderExtraFields();
	});
	// 최초 1개 표시
	renderExtraFields();

	// ===== 6. 옵션그룹/옵션 동적 추가/삭제 =====
	const optionGroupList = document.getElementById('product-manager-option-group-list');
	const addOptionGroupBtn = document.getElementById('product-manager-add-option-group');
	let optionGroups = [];
	function renderOptionGroups() {
		optionGroupList.innerHTML = '';
		optionGroups.forEach((group, groupIdx) => {
			let groupDiv = document.createElement('div');
			groupDiv.className = 'card mb-2';
			groupDiv.innerHTML = `
                <div class="card-body p-2">
                    <div class="input-group mb-2">
                        <input type="text" class="form-control form-control-sm" name="optionGroups[${groupIdx}].name" placeholder="옵션 그룹명" value="${group.name || ''}" required>
                        <button type="button" class="btn btn-outline-danger btn-sm" title="옵션그룹 삭제">×</button>
                    </div>
                    <div id="option-group-options-${groupIdx}"></div>
                    <button type="button" class="btn btn-outline-primary btn-sm mt-1" data-group-idx="${groupIdx}">+ 옵션 추가</button>
                </div>
            `;
			// 옵션그룹 삭제
			groupDiv.querySelector('.btn-outline-danger').onclick = () => { optionGroups.splice(groupIdx, 1); renderOptionGroups(); }
			// 옵션 추가
			groupDiv.querySelector('.btn-outline-primary').onclick = (e) => {
				group.options.push({
					name: '', value: '', extraPrice: '', sign: 'PLUS', sortOrder: group.options.length + 1
				});
				renderOptionGroups();
			}
			// 옵션 리스트
			const optionsContainer = groupDiv.querySelector(`#option-group-options-${groupIdx}`);
			group.options.forEach((opt, optIdx) => {
				let optRow = document.createElement('div');
				optRow.className = 'input-group mb-1';
				optRow.innerHTML = `
                    <input type="text" class="form-control form-control-sm" name="optionGroups[${groupIdx}].options[${optIdx}].name" placeholder="옵션명" value="${opt.name || ''}" required>
                    <input type="text" class="form-control form-control-sm" name="optionGroups[${groupIdx}].options[${optIdx}].value" placeholder="값" value="${opt.value || ''}">
                    <input type="number" class="form-control form-control-sm" name="optionGroups[${groupIdx}].options[${optIdx}].extraPrice" placeholder="추가금액" value="${opt.extraPrice || ''}">
                    <select class="form-select form-select-sm" name="optionGroups[${groupIdx}].options[${optIdx}].sign">
                        <option value="PLUS" ${opt.sign === 'PLUS' ? 'selected' : ''}>+</option>
                        <option value="MINUS" ${opt.sign === 'MINUS' ? 'selected' : ''}>-</option>
                    </select>
                    <input type="number" class="form-control form-control-sm" name="optionGroups[${groupIdx}].options[${optIdx}].sortOrder" placeholder="정렬" value="${opt.sortOrder || optIdx + 1}">
                    <button type="button" class="btn btn-outline-danger btn-sm" title="옵션 삭제">×</button>
                `;
				// 옵션 삭제
				optRow.querySelector('.btn-outline-danger').onclick = () => {
					group.options.splice(optIdx, 1);
					renderOptionGroups();
				};
				optionsContainer.appendChild(optRow);
			});
			optionGroupList.appendChild(groupDiv);
		});
	}
	addOptionGroupBtn.addEventListener('click', function() {
		optionGroups.push({
			name: '',
			options: []
		});
		renderOptionGroups();
	});
	// 최초 1개 표시
	renderOptionGroups();
	// 키워드 등록
	function renderKeywordList() {
		keywordList.innerHTML = '';
		keywords.forEach((kw, idx) => {
			const badge = document.createElement('div');
			badge.className = 'badge bg-info text-dark px-2 py-2 d-flex align-items-center mt-2';
			badge.style.fontSize = '14px';
			badge.innerHTML = `
            <span>${kw}</span>
            <span class="ms-2" style="cursor:pointer;" title="삭제">&times;</span>
        `;
			badge.querySelector('span:last-child').onclick = () => {
				keywords.splice(idx, 1);
				renderKeywordList();
			};
			keywordList.appendChild(badge);
		});
	}
	addKeywordBtn.onclick = addKeyword;
	keywordInput.onkeydown = function(e) {
		if (e.key === 'Enter') {
			addKeyword();
			e.preventDefault();
		}
	};
	function addKeyword() {
		const kw = keywordInput.value.trim();
		if (!kw) return;
		if (keywords.includes(kw)) return; // 중복제거
		keywords.push(kw);
		keywordInput.value = '';
		renderKeywordList();
	}

	// === 사용함/사용안함 이벤트 ===
	document.querySelectorAll('input[name="useRelatedProducts"]').forEach(radio => {
		radio.onchange = function() {
			document.getElementById('open-related-modal-btn').disabled = (this.value === "false");
		}
	});
	document.querySelectorAll('input[name="useBundleItems"]').forEach(radio => {
		radio.onchange = function() {
			document.getElementById('open-bundle-modal-btn').disabled = (this.value === "false");
		}
	});

	function renderRelatedProducts() {
		const list = document.getElementById('related-products-list');
		list.innerHTML = '';
		relatedProducts.forEach((p, idx) => {
			const badge = document.createElement('div');
			badge.className = 'badge bg-warning text-dark px-2 py-2 d-flex align-items-center';
			badge.innerHTML = `${p.name} (${p.type === 'RECIPROCAL' ? '상호' : '일방'})<span class="ms-2" style="cursor:pointer;" title="삭제">&times;</span>`;
			badge.querySelector('span').onclick = () => {
				relatedProducts.splice(idx, 1);
				renderRelatedProducts();
			}
			list.appendChild(badge);
		});
	}

	function renderBundleProducts() {
		const list = document.getElementById('bundle-products-list');
		list.innerHTML = '';
		bundleProducts.forEach((p, idx) => {
			const badge = document.createElement('div');
			badge.className = 'badge bg-success text-white px-2 py-2 d-flex align-items-center';
			badge.innerHTML = `${p.name}<span class="ms-2" style="cursor:pointer;" title="삭제">&times;</span>`;
			badge.querySelector('span').onclick = () => {
				bundleProducts.splice(idx, 1);
				renderBundleProducts();
			}
			list.appendChild(badge);
		});
	}

	document.getElementById('submitProductBtn').addEventListener('click', function(e) {
		e.preventDefault();

		const formData = new FormData();

		// =================== [1. 소분류(카테고리)] ===================
		console.log('========== [1. 소분류(카테고리) 선택] ==========');
		if (!selectedCategories || selectedCategories.length === 0) {
			console.log('선택된 소분류 없음');
		} else {
			console.log(`총 ${selectedCategories.length}개 선택`);
		}
		selectedCategories.forEach(cat => {
			formData.append('categorySmallIds', cat.id);
			console.log(`- id=${cat.id} (${cat.largeName} > ${cat.mediumName} > ${cat.smallName})`);
		});

		// =================== [2. 공통표시항목(질문/옵션)] ===================
		console.log('\n========== [2. 공통표시항목(질문/옵션)] ==========');
		let questionCnt = 0;
		document.querySelectorAll('#product-manager-display-options [name]').forEach(el => {
			if (el.type === 'file') {
				if (el.files && el.files.length > 0) {
					Array.from(el.files).forEach((file, fidx) => {
						formData.append(el.name, file);
						console.log(`- ${el.name}[${fidx}] 파일: ${file.name}, ${file.size}byte`);
						questionCnt++;
					});
				} else {
					console.log(`- ${el.name}: 파일 없음`);
				}
			} else if (el.type === 'textarea' && el.classList.contains('ck-editor__editable')) {
				// CKEditor는 별도 처리
			} else {
				formData.append(el.name, el.value);
				console.log(`- ${el.name}: "${el.value}"`);
				questionCnt++;
			}
		});
		Object.entries(ckeInstances).forEach(([tid, editor]) => {
			const val = editor.getData();
			formData.append(tid, val);
			console.log(`- CKEditor(${tid}): [${val && val.trim().length > 0 ? '입력됨' : '미입력'}]`);
			questionCnt++;
		});
		if (questionCnt === 0) console.log('질문/공통표시항목 없음');

		// =================== [3. 제품 기본정보] ===================
		console.log('\n========== [3. 제품 기본정보] ==========');
		const pName = document.getElementById('productName').value;
		const pCode = document.getElementById('productCode').value;
		const displayStatus = document.querySelector('input[name="displayStatus"]:checked')?.value;
		const saleStatus = document.querySelector('input[name="saleStatus"]:checked')?.value;
		formData.append('productName', pName);
		formData.append('productCode', pCode);
		formData.append('displayStatus', displayStatus ?? '');
		formData.append('saleStatus', saleStatus ?? '');
		console.log('- 제품명:', pName ? `"${pName}"` : '(미입력)');
		console.log('- 제품코드:', pCode ? `"${pCode}"` : '(미입력)');
		console.log('- 진열상태:', displayStatus ?? '(미선택)');
		console.log('- 판매상태:', saleStatus ?? '(미선택)');

		// =================== [4. 대표이미지] ===================
		console.log('\n========== [4. 대표이미지] ==========');
		if (mainInput.files && mainInput.files.length > 0) {
			const file = mainInput.files[0];
			formData.append('mainImage', file);
			console.log(`대표이미지: ${file.name} (${file.size}byte)`);
		} else {
			console.log('대표이미지 없음');
		}

		// =================== [5. 추가이미지] ===================
		console.log('\n========== [5. 추가이미지] ==========');
		if (!subFiles || subFiles.length === 0) {
			console.log('추가이미지 없음');
		} else {
			console.log(`총 ${subFiles.length}개`);
			subFiles.forEach((file, idx) => {
				formData.append('subImages', file); // 서버는 MultipartFile[] 등으로 받기
				console.log(`- [${idx + 1}] ${file.name} (${file.size}byte)`);
			});
		}

		// =================== [6. 상세설명(HTML)] ===================
		console.log('\n========== [6. 상세설명(HTML)] ==========');
		if (detailEditor) {
			const html = detailEditor.getData();
			formData.append('detailHtml', html);
			console.log(html && html.trim().length > 0 ? `입력됨 (HTML 길이: ${html.length})` : '미입력');
		} else {
			console.log('CKEditor 인스턴스 없음');
		}

		// =================== [7. 추가입력필드] ===================
		console.log('\n========== [7. 추가입력필드] ==========');
		const extraFieldRows = extraFieldList.querySelectorAll('.input-group');
		if (!extraFieldRows || extraFieldRows.length === 0) {
			console.log('추가입력필드 없음');
		} else {
			console.log(`총 ${extraFieldRows.length}개`);
			extraFieldRows.forEach((row, idx) => {
				const label = row.querySelector(`[name="extraFields[${idx}].label"]`)?.value ?? '';
				const value = row.querySelector(`[name="extraFields[${idx}].value"]`)?.value ?? '';
				formData.append(`extraFields[${idx}].label`, label);
				formData.append(`extraFields[${idx}].value`, value);
				console.log(`- [${idx + 1}] 질문명: "${label}" / 답변값: "${value}"`);
			});
		}

		// =================== [8. 옵션그룹/옵션] ===================
		console.log('\n========== [8. 옵션그룹/옵션] ==========');
		const groupCards = optionGroupList.querySelectorAll('.card');
		if (!groupCards || groupCards.length === 0) {
			console.log('옵션그룹 없음');
		} else {
			console.log(`총 ${groupCards.length}개 그룹`);
			groupCards.forEach((groupDiv, groupIdx) => {
				const groupName = groupDiv.querySelector(`[name="optionGroups[${groupIdx}].name"]`)?.value || '';
				formData.append(`optionGroups[${groupIdx}].name`, groupName);
				console.log(`- 그룹[${groupIdx + 1}] 그룹명: "${groupName}"`);
				const optionRows = groupDiv.querySelectorAll('.input-group.mb-1');
				if (!optionRows || optionRows.length === 0) {
					console.log('  옵션 없음');
				} else {
					console.log(`  옵션 ${optionRows.length}개`);
					optionRows.forEach((row, optIdx) => {
						const name = row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].name"]`)?.value || '';
						const value = row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].value"]`)?.value || '';
						const extraPrice = row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].extraPrice"]`)?.value || '';
						const sign = row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].sign"]`)?.value || '';
						const sortOrder = row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].sortOrder"]`)?.value || '';
						formData.append(`optionGroups[${groupIdx}].options[${optIdx}].name`, name);
						formData.append(`optionGroups[${groupIdx}].options[${optIdx}].value`, value);
						formData.append(`optionGroups[${groupIdx}].options[${optIdx}].extraPrice`, extraPrice);
						formData.append(`optionGroups[${groupIdx}].options[${optIdx}].sign`, sign);
						formData.append(`optionGroups[${groupIdx}].options[${optIdx}].sortOrder`, sortOrder);
						console.log(`    - 옵션[${optIdx + 1}] 옵션명: "${name}" / 값: "${value}" / 추가금액: "${extraPrice}" / 부호: "${sign}" / 정렬: "${sortOrder}"`);
					});
				}
			});
		}

		// =================== [9. 키워드] ===================
		console.log('\n========== [9. 키워드] ==========');
		if (!keywords || keywords.length === 0) {
			console.log('키워드 없음');
		} else {
			console.log(`총 ${keywords.length}개`);
			keywords.forEach((kw, idx) => {
				formData.append('keywords', kw);
				console.log(`- [${idx + 1}] "${kw}"`);
			});
		}

		// =================== [10. 관련상품] ===================
		console.log('\n========== [10. 관련상품] ==========');
		if (!relatedProducts || relatedProducts.length === 0) {
			console.log('관련상품 없음');
		} else {
			console.log(`총 ${relatedProducts.length}개`);
			relatedProducts.forEach((p, idx) => {
				formData.append(`relatedProducts[${idx}].id`, p.id);
				formData.append(`relatedProducts[${idx}].type`, p.type);
				console.log(`- [${idx + 1}] id=${p.id} / name="${p.name}" / type=${p.type}`);
			});
		}

		// =================== [11. 추가구성상품] ===================
		console.log('\n========== [11. 추가구성상품] ==========');
		if (!bundleProducts || bundleProducts.length === 0) {
			console.log('추가구성상품 없음');
		} else {
			console.log(`총 ${bundleProducts.length}개`);
			bundleProducts.forEach((p, idx) => {
				formData.append(`bundleProducts[${idx}].id`, p.id);
				console.log(`- [${idx + 1}] id=${p.id} / name="${p.name}"`);
			});
		}

		console.log('\n[= 전체 데이터 수집/콘솔 출력 완료 =]');

		// === 실제 전송 ===
		/*
		fetch('/api/product', {
			method: 'POST',
			body: formData
		})
		.then(res => {
			if (res.ok) return res.json();
			throw new Error('등록실패');
		})
		.then(json => {
			alert('제품 등록 성공');
		})
		.catch(err => alert('등록 실패: ' + err.message));
		*/
	});

});
