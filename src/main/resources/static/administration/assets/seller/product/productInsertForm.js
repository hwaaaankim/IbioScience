(function() {
	'use strict';

	const state = {
		meta: null,
		selectedCategories: [],
		keywords: [],
		representativeImage: null,
		iconImage: null,
		additionalImages: [],
		editor: null
	};

	document.addEventListener('DOMContentLoaded', async function() {
		try {
			await loadFormMeta();
			bindEvents();
			syncToggleControlledFields();
			initSortables();
			await initEditor();
		} catch (e) {
			console.error(e);
			alert(e.message || '초기화 중 오류가 발생했습니다.');
		}
	});

	async function loadFormMeta() {
		const response = await fetch('/seller/api/products/form-meta', {
			method: 'GET',
			headers: {
				'Accept': 'application/json'
			}
		});

		if (!response.ok) {
			throw new Error('상품 등록 메타데이터를 불러오지 못했습니다.');
		}

		state.meta = await response.json();

		fillSelect('seller-product-insertForm-displayStatus', state.meta.displayStatuses);
		fillSelect('seller-product-insertForm-saleStatus', state.meta.saleStatuses);
		fillSelect('seller-product-insertForm-state', state.meta.productStates);
		fillSelect('seller-product-insertForm-newState', state.meta.newStates);
		fillSelect('seller-product-insertForm-priceExposeTarget', state.meta.priceExposeTargets);

		renderLargeCategories();
	}

	function bindEvents() {
		document.getElementById('seller-product-insertForm-largeCategory').addEventListener('change', renderMediumCategories);
		document.getElementById('seller-product-insertForm-mediumCategory').addEventListener('change', renderSmallCategories);

		document.getElementById('seller-product-insertForm-addCategoryBtn').addEventListener('click', addSelectedCategory);

		document.getElementById('seller-product-insertForm-representativeImage').addEventListener('change', onRepresentativeImageChange);
		document.getElementById('seller-product-insertForm-additionalImages').addEventListener('change', onAdditionalImagesChange);
		document.getElementById('seller-product-insertForm-iconImage').addEventListener('change', onIconImageChange);

		document.getElementById('seller-product-insertForm-keywordInput').addEventListener('keydown', onKeywordKeydown);

		document.getElementById('seller-product-insertForm-addExtraFieldBtn').addEventListener('click', addExtraFieldRow);
		document.getElementById('seller-product-insertForm-addOptionGroupBtn').addEventListener('click', addOptionGroupCard);

		document.getElementById('seller-product-insertForm-usePriceReplacementText').addEventListener('change', syncToggleControlledFields);
		document.getElementById('seller-product-insertForm-useIconPeriod').addEventListener('change', syncToggleControlledFields);

		document.getElementById('seller-product-insertForm-saveBtn').addEventListener('click', submitForm);
	}

	function initSortables() {
		new Sortable(document.getElementById('seller-product-insertForm-additionalPreview'), {
			animation: 150,
			onEnd: syncAdditionalImagesByDom
		});
	}

	async function initEditor() {
		state.editor = await ClassicEditor.create(
			document.querySelector('#seller-product-insertForm-detailHtml'),
			{
				extraPlugins: [sellerProductUploadAdapterPlugin]
			}
		);
	}

	function sellerProductUploadAdapterPlugin(editor) {
		editor.plugins.get('FileRepository').createUploadAdapter = loader => {
			return new SellerProductUploadAdapter(loader);
		};
	}

	class SellerProductUploadAdapter {
		constructor(loader) {
			this.loader = loader;
		}

		upload() {
			return this.loader.file.then(file => {
				const formData = new FormData();
				formData.append('upload', file);

				return fetch('/seller/api/products/editor/temp-image', {
					method: 'POST',
					body: formData
				})
					.then(response => {
						if (!response.ok) {
							throw new Error('에디터 이미지 업로드에 실패했습니다.');
						}
						return response.json();
					})
					.then(data => {
						return { default: data.url };
					});
			});
		}

		abort() { }
	}

	function fillSelect(selectId, options) {
		const select = document.getElementById(selectId);
		select.innerHTML = '';

		(options || []).forEach(option => {
			const el = document.createElement('option');
			el.value = option.value;
			el.textContent = option.label;
			select.appendChild(el);
		});
	}

	function renderLargeCategories() {
		const largeSelect = document.getElementById('seller-product-insertForm-largeCategory');
		largeSelect.innerHTML = '<option value="">대분류 선택</option>';

		(state.meta.allowedCategories || []).forEach(large => {
			const option = document.createElement('option');
			option.value = String(large.id);
			option.textContent = large.name;
			largeSelect.appendChild(option);
		});

		renderMediumCategories();
	}

	function renderMediumCategories() {
		const largeId = document.getElementById('seller-product-insertForm-largeCategory').value;
		const mediumSelect = document.getElementById('seller-product-insertForm-mediumCategory');
		const smallSelect = document.getElementById('seller-product-insertForm-smallCategory');

		mediumSelect.innerHTML = '<option value="">중분류 선택</option>';
		smallSelect.innerHTML = '<option value="">소분류 선택</option>';

		const large = (state.meta.allowedCategories || []).find(item => String(item.id) === String(largeId));
		if (!large) {
			return;
		}

		(large.mediums || []).forEach(medium => {
			const option = document.createElement('option');
			option.value = String(medium.id);
			option.textContent = medium.name;
			mediumSelect.appendChild(option);
		});

		renderSmallCategories();
	}

	function renderSmallCategories() {
		const largeId = document.getElementById('seller-product-insertForm-largeCategory').value;
		const mediumId = document.getElementById('seller-product-insertForm-mediumCategory').value;
		const smallSelect = document.getElementById('seller-product-insertForm-smallCategory');

		smallSelect.innerHTML = '<option value="">소분류 선택</option>';

		const large = (state.meta.allowedCategories || []).find(item => String(item.id) === String(largeId));
		if (!large) {
			return;
		}

		const medium = (large.mediums || []).find(item => String(item.id) === String(mediumId));
		if (!medium) {
			return;
		}

		(medium.smalls || []).forEach(small => {
			const option = document.createElement('option');
			option.value = String(small.id);
			option.textContent = small.name;
			smallSelect.appendChild(option);
		});
	}

	function addSelectedCategory() {
		const largeId = document.getElementById('seller-product-insertForm-largeCategory').value;
		const mediumId = document.getElementById('seller-product-insertForm-mediumCategory').value;
		const smallId = document.getElementById('seller-product-insertForm-smallCategory').value;

		const large = (state.meta.allowedCategories || []).find(item => String(item.id) === String(largeId));
		if (!large) {
			alert('대분류를 선택해주세요.');
			return;
		}

		const medium = (large.mediums || []).find(item => String(item.id) === String(mediumId));
		if (!medium) {
			alert('중분류를 선택해주세요.');
			return;
		}

		const small = (medium.smalls || []).find(item => String(item.id) === String(smallId));
		if (!small) {
			alert('소분류를 선택해주세요.');
			return;
		}

		const exists = state.selectedCategories.some(item =>
			String(item.mediumId) === String(medium.id) && String(item.smallId) === String(small.id)
		);

		if (exists) {
			alert('이미 추가된 분류입니다.');
			return;
		}

		state.selectedCategories.push({
			largeId: large.id,
			largeName: large.name,
			mediumId: medium.id,
			mediumName: medium.name,
			smallId: small.id,
			smallName: small.name
		});

		renderSelectedCategories();
	}

	function renderSelectedCategories() {
		const container = document.getElementById('seller-product-insertForm-selectedCategoryList');
		container.innerHTML = '';

		state.selectedCategories.forEach((item, index) => {
			const chip = document.createElement('div');
			chip.className = 'seller-product-insertForm-chip';
			chip.innerHTML = `
                <span>${escapeHtml(item.largeName)} &gt; ${escapeHtml(item.mediumName)} &gt; ${escapeHtml(item.smallName)}</span>
                <button type="button" data-index="${index}">×</button>
            `;
			chip.querySelector('button').addEventListener('click', function() {
				state.selectedCategories.splice(index, 1);
				renderSelectedCategories();
			});
			container.appendChild(chip);
		});
	}

	function onRepresentativeImageChange(event) {
		const file = event.target.files[0];
		state.representativeImage = file || null;
		renderSingleImagePreview('seller-product-insertForm-representativePreview', file, function() {
			state.representativeImage = null;
			document.getElementById('seller-product-insertForm-representativeImage').value = '';
			document.getElementById('seller-product-insertForm-representativePreview').innerHTML = '';
		});
	}

	function onIconImageChange(event) {
		const file = event.target.files[0];
		state.iconImage = file || null;
		renderSingleImagePreview('seller-product-insertForm-iconPreview', file, function() {
			state.iconImage = null;
			document.getElementById('seller-product-insertForm-iconImage').value = '';
			document.getElementById('seller-product-insertForm-iconPreview').innerHTML = '';
		});
	}

	function renderSingleImagePreview(containerId, file, removeCallback) {
		const container = document.getElementById(containerId);
		container.innerHTML = '';

		if (!file) {
			return;
		}

		const card = buildImageCard(file, 0, removeCallback);
		container.appendChild(card);
	}

	function onAdditionalImagesChange(event) {
		const files = Array.from(event.target.files || []);
		files.forEach(file => {
			state.additionalImages.push({
				uid: crypto.randomUUID(),
				file: file
			});
		});

		event.target.value = '';
		renderAdditionalImages();
	}

	function renderAdditionalImages() {
		const container = document.getElementById('seller-product-insertForm-additionalPreview');
		container.innerHTML = '';

		state.additionalImages.forEach((item, index) => {
			const card = buildImageCard(item.file, item.uid, function() {
				state.additionalImages = state.additionalImages.filter(image => image.uid !== item.uid);
				renderAdditionalImages();
			});
			card.dataset.uid = item.uid;
			card.dataset.index = String(index);
			container.appendChild(card);
		});
	}

	function syncAdditionalImagesByDom() {
		const container = document.getElementById('seller-product-insertForm-additionalPreview');
		const orderedUids = Array.from(container.children).map(node => node.dataset.uid);
		state.additionalImages.sort((a, b) => orderedUids.indexOf(a.uid) - orderedUids.indexOf(b.uid));
	}

	function buildImageCard(file, uid, removeCallback) {
		const card = document.createElement('div');
		card.className = 'seller-product-insertForm-image-card';

		const img = document.createElement('img');
		img.src = URL.createObjectURL(file);
		img.onload = function() {
			URL.revokeObjectURL(img.src);
		};

		const btn = document.createElement('button');
		btn.type = 'button';
		btn.className = 'seller-product-insertForm-image-remove';
		btn.textContent = '×';
		btn.addEventListener('click', removeCallback);

		card.appendChild(img);
		card.appendChild(btn);
		if (uid !== undefined && uid !== null) {
			card.dataset.uid = uid;
		}
		return card;
	}

	function onKeywordKeydown(event) {
		if (event.key !== 'Enter') {
			return;
		}

		event.preventDefault();

		const input = event.target;
		const value = input.value.trim();

		if (!value) {
			return;
		}

		if (state.keywords.includes(value)) {
			input.value = '';
			return;
		}

		state.keywords.push(value);
		input.value = '';
		renderKeywords();
	}

	function renderKeywords() {
		const container = document.getElementById('seller-product-insertForm-keywordList');
		container.innerHTML = '';

		state.keywords.forEach((keyword, index) => {
			const chip = document.createElement('div');
			chip.className = 'seller-product-insertForm-chip';
			chip.innerHTML = `
                <span>${escapeHtml(keyword)}</span>
                <button type="button" data-index="${index}">×</button>
            `;
			chip.querySelector('button').addEventListener('click', function() {
				state.keywords.splice(index, 1);
				renderKeywords();
			});
			container.appendChild(chip);
		});
	}

	function addExtraFieldRow() {
		const container = document.getElementById('seller-product-insertForm-extraFieldList');

		const wrapper = document.createElement('div');
		wrapper.className = 'seller-product-insertForm-extra-row';
		wrapper.innerHTML = `
            <div class="row g-3">
                <div class="col-lg-5">
                    <label class="form-label">질문</label>
                    <input type="text" class="form-control seller-product-insertForm-extra-label">
                </div>
                <div class="col-lg-5">
                    <label class="form-label">답변</label>
                    <input type="text" class="form-control seller-product-insertForm-extra-value">
                </div>
                <div class="col-lg-2 d-flex align-items-end">
                    <button type="button" class="btn btn-soft-danger w-100 seller-product-insertForm-remove-extra-btn">삭제</button>
                </div>
            </div>
        `;

		wrapper.querySelector('.seller-product-insertForm-remove-extra-btn').addEventListener('click', function() {
			wrapper.remove();
		});

		container.appendChild(wrapper);
	}

	function addOptionGroupCard() {
		const container = document.getElementById('seller-product-insertForm-optionGroupList');

		const groupCard = document.createElement('div');
		groupCard.className = 'seller-product-insertForm-option-group-card';
		groupCard.innerHTML = `
		<div class="row g-3 seller-product-insertForm-option-group-head">
			<div class="col-lg-8">
				<label class="form-label">옵션 그룹명</label>
				<input type="text" class="form-control seller-product-insertForm-option-group-name">
			</div>
			<div class="col-lg-2 d-flex align-items-end">
				<div class="w-100">
					<label class="form-label invisible">옵션 추가</label>
					<button type="button" class="btn btn-soft-primary w-100 seller-product-insertForm-add-option-btn">옵션 추가</button>
				</div>
			</div>
			<div class="col-lg-2 d-flex align-items-end">
				<div class="w-100">
					<label class="form-label invisible">그룹 삭제</label>
					<button type="button" class="btn btn-soft-danger w-100 seller-product-insertForm-remove-group-btn">그룹 삭제</button>
				</div>
			</div>
		</div>
		<div class="seller-product-insertForm-option-list"></div>
	`;

		groupCard.querySelector('.seller-product-insertForm-add-option-btn').addEventListener('click', function() {
			addOptionRow(groupCard.querySelector('.seller-product-insertForm-option-list'));
		});

		groupCard.querySelector('.seller-product-insertForm-remove-group-btn').addEventListener('click', function() {
			groupCard.remove();
		});

		container.appendChild(groupCard);
	}

	function addOptionRow(optionListEl) {
		const row = document.createElement('div');
		row.className = 'seller-product-insertForm-option-row';
		row.innerHTML = `
            <div class="row g-3">
                <div class="col-lg-3">
                    <label class="form-label">옵션명</label>
                    <input type="text" class="form-control seller-product-insertForm-option-name">
                </div>
                <div class="col-lg-3">
                    <label class="form-label">옵션값</label>
                    <input type="text" class="form-control seller-product-insertForm-option-value">
                </div>
                <div class="col-lg-2">
                    <label class="form-label">추가금액</label>
                    <input type="number" class="form-control seller-product-insertForm-option-price">
                </div>
                <div class="col-lg-2">
                    <label class="form-label">부호</label>
                    <select class="form-select seller-product-insertForm-option-sign">
                        ${buildPriceSignOptions()}
                    </select>
                </div>
                <div class="col-lg-2 d-flex align-items-end">
                    <button type="button" class="btn btn-soft-danger w-100 seller-product-insertForm-remove-option-btn">삭제</button>
                </div>
            </div>
        `;

		row.querySelector('.seller-product-insertForm-remove-option-btn').addEventListener('click', function() {
			row.remove();
		});

		optionListEl.appendChild(row);
	}

	function buildPriceSignOptions() {
		return (state.meta.priceSigns || []).map(item => {
			return `<option value="${escapeHtml(item.value)}">${escapeHtml(item.label)}</option>`;
		}).join('');
	}

	async function submitForm() {
		try {
			if (!state.editor) {
				throw new Error('에디터가 아직 준비되지 않았습니다.');
			}

			if (!state.representativeImage) {
				throw new Error('대표 이미지는 필수입니다.');
			}

			if (state.selectedCategories.length === 0) {
				throw new Error('최소 1개의 분류를 추가해야 합니다.');
			}

			if (state.keywords.length === 0) {
				throw new Error('키워드는 최소 1개 이상 입력해야 합니다.');
			}

			const request = buildRequestPayload();

			const formData = new FormData();
			formData.append('request', new Blob([JSON.stringify(request)], { type: 'application/json' }));
			formData.append('representativeImage', state.representativeImage);

			if (state.iconImage) {
				formData.append('iconImage', state.iconImage);
			}

			state.additionalImages.forEach(item => {
				formData.append('additionalImages', item.file);
			});

			const response = await fetch('/seller/api/products', {
				method: 'POST',
				body: formData
			});

			const data = await response.json();

			if (!response.ok) {
				throw new Error(data.message || '상품 저장 중 오류가 발생했습니다.');
			}

			alert(data.message || '상품이 등록되었습니다.');
			window.location.reload();

		} catch (e) {
			console.error(e);
			alert(e.message || '상품 저장 중 오류가 발생했습니다.');
		}
	}

	function buildRequestPayload() {
		return {
			displayStatus: getValue('seller-product-insertForm-displayStatus'),
			saleStatus: getValue('seller-product-insertForm-saleStatus'),
			state: getValue('seller-product-insertForm-state'),
			newState: getValue('seller-product-insertForm-newState'),

			name: getValue('seller-product-insertForm-name'),
			code: getValue('seller-product-insertForm-code'),
			manufacturerText: getValue('seller-product-insertForm-manufacturerText'),
			supplierText: getValue('seller-product-insertForm-supplierText'),
			manufacturedAt: getDateValue('seller-product-insertForm-manufacturedAt'),
			expiredAt: getDateValue('seller-product-insertForm-expiredAt'),

			detailHtml: state.editor.getData(),
			summaryDescription: getValue('seller-product-insertForm-summaryDescription'),
			shortDescription: getValue('seller-product-insertForm-shortDescription'),
			internalProductCode: getValue('seller-product-insertForm-internalProductCode'),

			consumerPrice: getNumberValue('seller-product-insertForm-consumerPrice'),
			salePrice: getNumberValue('seller-product-insertForm-salePrice'),
			priceExposeTarget: getValue('seller-product-insertForm-priceExposeTarget'),
			usePriceReplacementText: getCheckboxValue('seller-product-insertForm-usePriceReplacementText'),
			priceReplacementText: getCheckboxValue('seller-product-insertForm-usePriceReplacementText')
				? getValue('seller-product-insertForm-priceReplacementText')
				: null,
			rewardRate: getFloatValue('seller-product-insertForm-rewardRate'),

			validFrom: getDateValue('seller-product-insertForm-validFrom'),
			validTo: getDateValue('seller-product-insertForm-validTo'),

			useIconPeriod: getCheckboxValue('seller-product-insertForm-useIconPeriod'),
			iconStartDate: getCheckboxValue('seller-product-insertForm-useIconPeriod')
				? getDateValue('seller-product-insertForm-iconStartDate')
				: null,
			iconEndDate: getCheckboxValue('seller-product-insertForm-useIconPeriod')
				? getDateValue('seller-product-insertForm-iconEndDate')
				: null,

			categoryMappings: state.selectedCategories.map(item => ({
				mediumId: item.mediumId,
				smallId: item.smallId
			})),

			extraFields: collectExtraFields(),
			keywords: [...state.keywords],
			optionGroups: collectOptionGroups()
		};
	}
	function syncToggleControlledFields() {
		syncControlledInputs(
			'seller-product-insertForm-usePriceReplacementText',
			['seller-product-insertForm-priceReplacementText']
		);

		syncControlledInputs(
			'seller-product-insertForm-useIconPeriod',
			[
				'seller-product-insertForm-iconStartDate',
				'seller-product-insertForm-iconEndDate'
			]
		);
	}

	function syncControlledInputs(toggleId, targetIds) {
		const enabled = document.getElementById(toggleId).checked;

		targetIds.forEach(function(targetId) {
			const target = document.getElementById(targetId);
			target.disabled = !enabled;

			if (!enabled) {
				target.value = '';
			}
		});
	}
	function collectExtraFields() {
		return Array.from(document.querySelectorAll('#seller-product-insertForm-extraFieldList .seller-product-insertForm-extra-row'))
			.map(row => ({
				label: row.querySelector('.seller-product-insertForm-extra-label').value.trim(),
				value: row.querySelector('.seller-product-insertForm-extra-value').value.trim()
			}));
	}

	function collectOptionGroups() {
		return Array.from(document.querySelectorAll('#seller-product-insertForm-optionGroupList .seller-product-insertForm-option-group-card'))
			.map((groupEl, groupIndex) => ({
				name: groupEl.querySelector('.seller-product-insertForm-option-group-name').value.trim(),
				sortOrder: groupIndex,
				options: Array.from(groupEl.querySelectorAll('.seller-product-insertForm-option-list .seller-product-insertForm-option-row'))
					.map((optionEl, optionIndex) => ({
						name: optionEl.querySelector('.seller-product-insertForm-option-name').value.trim(),
						value: optionEl.querySelector('.seller-product-insertForm-option-value').value.trim(),
						extraPrice: getInputNumber(optionEl.querySelector('.seller-product-insertForm-option-price')),
						sign: optionEl.querySelector('.seller-product-insertForm-option-sign').value,
						sortOrder: optionIndex
					}))
			}));
	}

	function getValue(id) {
		return document.getElementById(id).value.trim();
	}

	function getDateValue(id) {
		const value = document.getElementById(id).value;
		return value ? value : null;
	}

	function getCheckboxValue(id) {
		return document.getElementById(id).checked;
	}

	function getNumberValue(id) {
		const value = document.getElementById(id).value;
		return value === '' ? null : Number(value);
	}

	function getFloatValue(id) {
		const value = document.getElementById(id).value;
		return value === '' ? null : parseFloat(value);
	}

	function getInputNumber(input) {
		const value = input.value;
		return value === '' ? null : Number(value);
	}

	function escapeHtml(value) {
		return String(value ?? '')
			.replaceAll('&', '&amp;')
			.replaceAll('<', '&lt;')
			.replaceAll('>', '&gt;')
			.replaceAll('"', '&quot;')
			.replaceAll("'", '&#39;');
	}
})();