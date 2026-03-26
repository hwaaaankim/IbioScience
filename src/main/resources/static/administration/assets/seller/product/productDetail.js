(function() {
	'use strict';

	const dealerProductId = Number(window.SELLER_PRODUCT_DETAIL_ID || 0);
	const isReadOnly = !!window.SELLER_PRODUCT_DETAIL_READ_ONLY;

	const state = {
		meta: null,
		detail: null,
		selectedCategories: [],
		keywords: [],
		representativeImage: {
			existing: null,
			newFile: null,
			removeExisting: false
		},
		iconImage: {
			existing: null,
			newFile: null,
			removeExisting: false
		},
		additionalImages: [],
		editor: null
	};

	document.addEventListener('DOMContentLoaded', async function() {
		try {
			if (!dealerProductId) {
				throw new Error('상품 ID 가 올바르지 않습니다.');
			}

			bindEvents();

			if (!isReadOnly) {
				initSortables();
			}

			await loadMetaAndDetail();
			await initEditor();
			applyDetailToForm();

			if (isReadOnly) {
				applyReadOnlyMode();
			}
		} catch (e) {
			console.error(e);
			alert(e.message || '상품 상세 초기화 중 오류가 발생했습니다.');
		}
	});

	function bindEvents() {
		if (isReadOnly) {
			return;
		}

		document.getElementById('seller-product-detail-largeCategory').addEventListener('change', renderMediumCategories);
		document.getElementById('seller-product-detail-mediumCategory').addEventListener('change', renderSmallCategories);
		document.getElementById('seller-product-detail-addCategoryBtn').addEventListener('click', addSelectedCategory);

		document.getElementById('seller-product-detail-representativeImage').addEventListener('change', onRepresentativeImageChange);
		document.getElementById('seller-product-detail-iconImage').addEventListener('change', onIconImageChange);
		document.getElementById('seller-product-detail-additionalImages').addEventListener('change', onAdditionalImagesChange);

		document.getElementById('seller-product-detail-keywordInput').addEventListener('keydown', onKeywordKeydown);
		document.getElementById('seller-product-detail-addExtraFieldBtn').addEventListener('click', function() {
			addExtraFieldRow();
		});
		document.getElementById('seller-product-detail-addOptionGroupBtn').addEventListener('click', function() {
			addOptionGroupCard();
		});

		document.getElementById('seller-product-detail-usePriceReplacementText').addEventListener('change', updatePriceReplacementEnabledState);
		document.getElementById('seller-product-detail-useIconPeriod').addEventListener('change', updateIconPeriodEnabledState);

		const saveBtn = document.getElementById('seller-product-detail-saveBtn');
		if (saveBtn) {
			saveBtn.addEventListener('click', submitUpdate);
		}
	}

	function initSortables() {
		new Sortable(document.getElementById('seller-product-detail-additionalPreview'), {
			animation: 150,
			onEnd: syncAdditionalImagesByDom
		});
	}

	async function loadMetaAndDetail() {
		if (isReadOnly) {
			const detailResponse = await fetch(`/seller/api/products/${dealerProductId}`, {
				method: 'GET',
				headers: { 'Accept': 'application/json' }
			});

			if (!detailResponse.ok) {
				throw new Error('상품 상세 데이터를 불러오지 못했습니다.');
			}

			state.meta = buildReadOnlyMeta();
			state.detail = await detailResponse.json();

			fillSingleSelectOption('seller-product-detail-displayStatus', state.detail.displayStatus);
			fillSingleSelectOption('seller-product-detail-saleStatus', state.detail.saleStatus);
			fillSingleSelectOption('seller-product-detail-state', state.detail.state);
			fillSingleSelectOption('seller-product-detail-newState', state.detail.newState);
			fillSingleSelectOption('seller-product-detail-priceExposeTarget', state.detail.priceExposeTarget);

			renderLargeCategories();
			return;
		}

		const [metaResponse, detailResponse] = await Promise.all([
			fetch('/seller/api/products/form-meta', {
				method: 'GET',
				headers: { 'Accept': 'application/json' }
			}),
			fetch(`/seller/api/products/${dealerProductId}`, {
				method: 'GET',
				headers: { 'Accept': 'application/json' }
			})
		]);

		if (!metaResponse.ok) {
			throw new Error('상품 메타데이터를 불러오지 못했습니다.');
		}
		if (!detailResponse.ok) {
			throw new Error('상품 상세 데이터를 불러오지 못했습니다.');
		}

		state.meta = await metaResponse.json();
		state.detail = await detailResponse.json();

		fillSelect('seller-product-detail-displayStatus', state.meta.displayStatuses);
		fillSelect('seller-product-detail-saleStatus', state.meta.saleStatuses);
		fillSelect('seller-product-detail-state', state.meta.productStates);
		fillSelect('seller-product-detail-newState', state.meta.newStates);
		fillSelect('seller-product-detail-priceExposeTarget', state.meta.priceExposeTargets);

		renderLargeCategories();
	}

	function buildReadOnlyMeta() {
		return {
			displayStatuses: [],
			saleStatuses: [],
			productStates: [],
			newStates: [],
			priceExposeTargets: [],
			allowedCategories: [],
			priceSigns: []
		};
	}

	function fillSingleSelectOption(selectId, value) {
		const select = document.getElementById(selectId);
		if (!select) {
			return;
		}

		select.innerHTML = '';

		const option = document.createElement('option');
		option.value = value ?? '';
		option.textContent = value ?? '-';
		option.selected = true;
		select.appendChild(option);
	}

	async function initEditor() {
		const config = isReadOnly
			? {}
			: {
				extraPlugins: [sellerProductUploadAdapterPlugin]
			};

		state.editor = await ClassicEditor.create(
			document.querySelector('#seller-product-detail-detailHtml'),
			config
		);

		if (isReadOnly && typeof state.editor.enableReadOnlyMode === 'function') {
			state.editor.enableReadOnlyMode('seller-product-detail-readonly');
		}
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

	function applyDetailToForm() {
		const detail = state.detail;

		setValue('seller-product-detail-id', detail.id);
		setValue('seller-product-detail-name', detail.name);
		setValue('seller-product-detail-code', detail.code);
		setValue('seller-product-detail-internalProductCode', detail.internalProductCode);
		setValue('seller-product-detail-manufacturerText', detail.manufacturerText);
		setValue('seller-product-detail-supplierText', detail.supplierText);
		setValue('seller-product-detail-summaryDescription', detail.summaryDescription);
		setValue('seller-product-detail-shortDescription', detail.shortDescription);
		setValue('seller-product-detail-consumerPrice', detail.consumerPrice);
		setValue('seller-product-detail-salePrice', detail.salePrice);
		setValue('seller-product-detail-rewardRate', detail.rewardRate);
		setValue('seller-product-detail-priceReplacementText', detail.priceReplacementText);

		setDateValue('seller-product-detail-validFrom', detail.validFrom);
		setDateValue('seller-product-detail-validTo', detail.validTo);
		setDateValue('seller-product-detail-manufacturedAt', detail.manufacturedAt);
		setDateValue('seller-product-detail-expiredAt', detail.expiredAt);
		setDateValue('seller-product-detail-iconStartDate', detail.iconStartDate);
		setDateValue('seller-product-detail-iconEndDate', detail.iconEndDate);

		setChecked('seller-product-detail-usePriceReplacementText', detail.usePriceReplacementText);
		setChecked('seller-product-detail-useIconPeriod', detail.useIconPeriod);

		setSelectValue('seller-product-detail-displayStatus', detail.displayStatus);
		setSelectValue('seller-product-detail-saleStatus', detail.saleStatus);
		setSelectValue('seller-product-detail-state', detail.state);
		setSelectValue('seller-product-detail-newState', detail.newState);
		setSelectValue('seller-product-detail-priceExposeTarget', detail.priceExposeTarget);

		state.selectedCategories = (detail.categoryMappings || []).map(item => ({
			largeId: item.largeId,
			largeName: item.largeName,
			mediumId: item.mediumId,
			mediumName: item.mediumName,
			smallId: item.smallId,
			smallName: item.smallName
		}));
		renderSelectedCategories();

		state.keywords = Array.isArray(detail.keywords) ? [...detail.keywords] : [];
		renderKeywords();

		state.representativeImage.existing = detail.representativeImage || null;
		state.representativeImage.newFile = null;
		state.representativeImage.removeExisting = false;
		renderRepresentativeImage();

		state.iconImage.existing = detail.iconImage || null;
		state.iconImage.newFile = null;
		state.iconImage.removeExisting = false;
		renderIconImage();

		state.additionalImages = (detail.additionalImages || []).map(item => ({
			type: 'EXISTING',
			token: `EXISTING:${item.id}`,
			id: item.id,
			url: item.url,
			fileName: item.fileName,
			sortOrder: item.sortOrder,
			deleted: false
		}));
		renderAdditionalImages();

		renderExtraFields(detail.extraFields || []);
		renderOptionGroups(detail.optionGroups || []);

		if (state.editor) {
			state.editor.setData(detail.detailHtml || '');
		}

		updatePriceReplacementEnabledState();
		updateIconPeriodEnabledState();
	}

	function applyReadOnlyMode() {
		document.body.classList.add('seller-product-detail-readonly');

		const form = document.getElementById('seller-product-detail-form');
		if (!form) {
			return;
		}

		form.querySelectorAll('input, select, textarea, button').forEach(el => {
			el.disabled = true;
		});
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
		const largeSelect = document.getElementById('seller-product-detail-largeCategory');
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
		const largeId = document.getElementById('seller-product-detail-largeCategory').value;
		const mediumSelect = document.getElementById('seller-product-detail-mediumCategory');
		const smallSelect = document.getElementById('seller-product-detail-smallCategory');

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
		const largeId = document.getElementById('seller-product-detail-largeCategory').value;
		const mediumId = document.getElementById('seller-product-detail-mediumCategory').value;
		const smallSelect = document.getElementById('seller-product-detail-smallCategory');

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

	function updatePriceReplacementEnabledState() {
		const enabled = document.getElementById('seller-product-detail-usePriceReplacementText').checked;
		const input = document.getElementById('seller-product-detail-priceReplacementText');

		input.disabled = !enabled || isReadOnly;

		if (!enabled) {
			input.value = '';
		}
	}

	function updateIconPeriodEnabledState() {
		const enabled = document.getElementById('seller-product-detail-useIconPeriod').checked;
		const startInput = document.getElementById('seller-product-detail-iconStartDate');
		const endInput = document.getElementById('seller-product-detail-iconEndDate');

		startInput.disabled = !enabled || isReadOnly;
		endInput.disabled = !enabled || isReadOnly;

		if (!enabled) {
			startInput.value = '';
			endInput.value = '';
		}
	}

	function addSelectedCategory() {
		const largeId = document.getElementById('seller-product-detail-largeCategory').value;
		const mediumId = document.getElementById('seller-product-detail-mediumCategory').value;
		const smallId = document.getElementById('seller-product-detail-smallCategory').value;

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
			String(item.mediumId) === String(medium.id) &&
			String(item.smallId) === String(small.id)
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
		const container = document.getElementById('seller-product-detail-selectedCategoryList');
		container.innerHTML = '';

		state.selectedCategories.forEach((item, index) => {
			const chip = document.createElement('div');
			chip.className = 'seller-product-detail-chip';
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
		if (!file) {
			return;
		}
		state.representativeImage.newFile = file;
		state.representativeImage.removeExisting = false;
		renderRepresentativeImage();
	}

	function renderRepresentativeImage() {
		const container = document.getElementById('seller-product-detail-representativePreview');
		container.innerHTML = '';

		if (state.representativeImage.newFile) {
			container.appendChild(buildFileImageCard(
				state.representativeImage.newFile,
				'신규 대표',
				function() {
					state.representativeImage.newFile = null;
					document.getElementById('seller-product-detail-representativeImage').value = '';
					renderRepresentativeImage();
				}
			));
			return;
		}

		if (state.representativeImage.existing && !state.representativeImage.removeExisting) {
			container.appendChild(buildUrlImageCard(
				state.representativeImage.existing.url,
				'기존 대표',
				function() {
					state.representativeImage.removeExisting = true;
					renderRepresentativeImage();
				}
			));
		}
	}

	function onIconImageChange(event) {
		const file = event.target.files[0];
		if (!file) {
			return;
		}
		state.iconImage.newFile = file;
		state.iconImage.removeExisting = false;
		renderIconImage();
	}

	function renderIconImage() {
		const container = document.getElementById('seller-product-detail-iconPreview');
		container.innerHTML = '';

		if (state.iconImage.newFile) {
			container.appendChild(buildFileImageCard(
				state.iconImage.newFile,
				'신규 아이콘',
				function() {
					state.iconImage.newFile = null;
					document.getElementById('seller-product-detail-iconImage').value = '';
					renderIconImage();
				}
			));
			return;
		}

		if (state.iconImage.existing && !state.iconImage.removeExisting) {
			container.appendChild(buildUrlImageCard(
				state.iconImage.existing.url,
				'기존 아이콘',
				function() {
					state.iconImage.removeExisting = true;
					renderIconImage();
				}
			));
		}
	}

	function onAdditionalImagesChange(event) {
		const files = Array.from(event.target.files || []);
		files.forEach(file => {
			const uid = crypto.randomUUID();
			state.additionalImages.push({
				type: 'NEW',
				token: `NEW:${uid}`,
				uploadUid: uid,
				file: file,
				deleted: false
			});
		});

		event.target.value = '';
		renderAdditionalImages();
	}

	function renderAdditionalImages() {
		const container = document.getElementById('seller-product-detail-additionalPreview');
		container.innerHTML = '';

		getVisibleAdditionalImages().forEach(item => {
			let card;

			if (item.type === 'EXISTING') {
				card = buildUrlImageCard(item.url, '기존 추가', function() {
					item.deleted = true;
					renderAdditionalImages();
				});
			} else {
				card = buildFileImageCard(item.file, '신규 추가', function() {
					state.additionalImages = state.additionalImages.filter(image => image.token !== item.token);
					renderAdditionalImages();
				});
			}

			card.dataset.token = item.token;
			container.appendChild(card);
		});
	}

	function syncAdditionalImagesByDom() {
		const container = document.getElementById('seller-product-detail-additionalPreview');
		const orderedTokens = Array.from(container.children).map(node => node.dataset.token);

		const visibleMap = new Map(
			getVisibleAdditionalImages().map(item => [item.token, item])
		);

		const reorderedVisible = orderedTokens
			.map(token => visibleMap.get(token))
			.filter(Boolean);

		const deletedItems = state.additionalImages.filter(item => item.deleted);
		state.additionalImages = [...reorderedVisible, ...deletedItems];
	}

	function getVisibleAdditionalImages() {
		return state.additionalImages.filter(item => !item.deleted);
	}

	function buildFileImageCard(file, badgeText, removeCallback) {
		const card = document.createElement('div');
		card.className = 'seller-product-detail-image-card';

		const img = document.createElement('img');
		img.src = URL.createObjectURL(file);
		img.onload = function() {
			URL.revokeObjectURL(img.src);
		};

		const btn = document.createElement('button');
		btn.type = 'button';
		btn.className = 'seller-product-detail-image-remove';
		btn.textContent = '×';
		btn.addEventListener('click', removeCallback);

		const badge = document.createElement('div');
		badge.className = 'seller-product-detail-image-badge';
		badge.textContent = badgeText;

		card.appendChild(img);
		card.appendChild(btn);
		card.appendChild(badge);
		return card;
	}

	function buildUrlImageCard(url, badgeText, removeCallback) {
		const card = document.createElement('div');
		card.className = 'seller-product-detail-image-card';

		const img = document.createElement('img');
		img.src = url;

		const btn = document.createElement('button');
		btn.type = 'button';
		btn.className = 'seller-product-detail-image-remove';
		btn.textContent = '×';
		btn.addEventListener('click', removeCallback);

		const badge = document.createElement('div');
		badge.className = 'seller-product-detail-image-badge';
		badge.textContent = badgeText;

		card.appendChild(img);
		card.appendChild(btn);
		card.appendChild(badge);
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
		const container = document.getElementById('seller-product-detail-keywordList');
		container.innerHTML = '';

		state.keywords.forEach((keyword, index) => {
			const chip = document.createElement('div');
			chip.className = 'seller-product-detail-chip';
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

	function renderExtraFields(fields) {
		const container = document.getElementById('seller-product-detail-extraFieldList');
		container.innerHTML = '';

		fields.forEach(item => {
			addExtraFieldRow(item.label, item.value);
		});
	}

	function addExtraFieldRow(label = '', value = '') {
		const container = document.getElementById('seller-product-detail-extraFieldList');

		const wrapper = document.createElement('div');
		wrapper.className = 'seller-product-detail-extra-row';
		wrapper.innerHTML = `
			<div class="row g-3">
				<div class="col-lg-5">
					<label class="form-label">질문</label>
					<input type="text" class="form-control seller-product-detail-extra-label" value="${escapeHtml(label)}">
				</div>
				<div class="col-lg-5">
					<label class="form-label">답변</label>
					<input type="text" class="form-control seller-product-detail-extra-value" value="${escapeHtml(value)}">
				</div>
				<div class="col-lg-2 d-flex align-items-end">
					<button type="button" class="btn btn-soft-danger w-100 seller-product-detail-remove-extra-btn">삭제</button>
				</div>
			</div>
		`;

		wrapper.querySelector('.seller-product-detail-remove-extra-btn').addEventListener('click', function() {
			wrapper.remove();
		});

		container.appendChild(wrapper);
	}

	function renderOptionGroups(groups) {
		const container = document.getElementById('seller-product-detail-optionGroupList');
		container.innerHTML = '';

		groups.forEach(group => {
			addOptionGroupCard(group);
		});
	}

	function addOptionGroupCard(groupData = null) {
		const container = document.getElementById('seller-product-detail-optionGroupList');

		const groupCard = document.createElement('div');
		groupCard.className = 'seller-product-detail-option-group-card';
		groupCard.innerHTML = `
		<div class="row g-3 seller-product-detail-option-group-head">
			<div class="col-lg-8">
				<label class="form-label">옵션 그룹명</label>
				<input type="text" class="form-control seller-product-detail-option-group-name" value="${escapeHtml(groupData?.name || '')}">
			</div>
			<div class="col-lg-2 d-flex align-items-end">
				<div class="w-100">
					<label class="form-label invisible">옵션 추가</label>
					<button type="button" class="btn btn-soft-primary w-100 seller-product-detail-add-option-btn">옵션 추가</button>
				</div>
			</div>
			<div class="col-lg-2 d-flex align-items-end">
				<div class="w-100">
					<label class="form-label invisible">그룹 삭제</label>
					<button type="button" class="btn btn-soft-danger w-100 seller-product-detail-remove-group-btn">그룹 삭제</button>
				</div>
			</div>
		</div>
		<div class="seller-product-detail-option-list"></div>
	`;

		groupCard.querySelector('.seller-product-detail-add-option-btn').addEventListener('click', function() {
			addOptionRow(groupCard.querySelector('.seller-product-detail-option-list'));
		});

		groupCard.querySelector('.seller-product-detail-remove-group-btn').addEventListener('click', function() {
			groupCard.remove();
		});

		container.appendChild(groupCard);

		(groupData?.options || []).forEach(option => {
			addOptionRow(groupCard.querySelector('.seller-product-detail-option-list'), option);
		});
	}

	function addOptionRow(optionListEl, optionData = null) {
		const row = document.createElement('div');
		row.className = 'seller-product-detail-option-row';
		row.innerHTML = `
			<div class="row g-3">
				<div class="col-lg-3">
					<label class="form-label">옵션명</label>
					<input type="text" class="form-control seller-product-detail-option-name" value="${escapeHtml(optionData?.name || '')}">
				</div>
				<div class="col-lg-3">
					<label class="form-label">옵션값</label>
					<input type="text" class="form-control seller-product-detail-option-value" value="${escapeHtml(optionData?.value || '')}">
				</div>
				<div class="col-lg-2">
					<label class="form-label">추가금액</label>
					<input type="number" class="form-control seller-product-detail-option-price" value="${optionData?.extraPrice ?? ''}">
				</div>
				<div class="col-lg-2">
					<label class="form-label">부호</label>
					<select class="form-select seller-product-detail-option-sign">
						${buildPriceSignOptions(optionData?.sign)}
					</select>
				</div>
				<div class="col-lg-2 d-flex align-items-end">
					<button type="button" class="btn btn-soft-danger w-100 seller-product-detail-remove-option-btn">삭제</button>
				</div>
			</div>
		`;

		row.querySelector('.seller-product-detail-remove-option-btn').addEventListener('click', function() {
			row.remove();
		});

		optionListEl.appendChild(row);
	}

	function buildPriceSignOptions(selectedValue) {
		const signs = Array.isArray(state.meta?.priceSigns) ? state.meta.priceSigns : [];

		if (!signs.length) {
			const fallbackValue = selectedValue ?? '';
			return `<option value="${escapeHtml(fallbackValue)}" selected>${escapeHtml(fallbackValue || '-')}</option>`;
		}

		return signs.map(item => {
			const selected = String(item.value) === String(selectedValue || '') ? 'selected' : '';
			return `<option value="${escapeHtml(item.value)}" ${selected}>${escapeHtml(item.label)}</option>`;
		}).join('');
	}

	async function submitUpdate() {
		try {
			if (isReadOnly) {
				throw new Error('관리자 조회 모드에서는 수정할 수 없습니다.');
			}

			if (!state.editor) {
				throw new Error('에디터가 아직 준비되지 않았습니다.');
			}

			if (state.selectedCategories.length === 0) {
				throw new Error('최소 1개의 분류를 추가해야 합니다.');
			}

			if (state.keywords.length === 0) {
				throw new Error('키워드는 최소 1개 이상 입력해야 합니다.');
			}

			if (!hasFinalRepresentativeImage()) {
				throw new Error('대표 이미지는 반드시 1장 유지되어야 합니다.');
			}

			const request = buildRequestPayload();

			const formData = new FormData();
			formData.append('request', new Blob([JSON.stringify(request)], { type: 'application/json' }));

			if (state.representativeImage.newFile) {
				formData.append('representativeImage', state.representativeImage.newFile);
			}

			if (state.iconImage.newFile) {
				formData.append('iconImage', state.iconImage.newFile);
			}

			getVisibleAdditionalImages()
				.filter(item => item.type === 'NEW')
				.forEach(item => {
					formData.append('newAdditionalImageUids', item.uploadUid);
					formData.append('newAdditionalImages', item.file);
				});

			const response = await fetch(`/seller/api/products/${dealerProductId}`, {
				method: 'PUT',
				body: formData
			});

			const contentType = response.headers.get('content-type') || '';
			const data = contentType.includes('application/json')
				? await response.json()
				: { message: await response.text() };

			if (!response.ok) {
				throw new Error(data.message || '상품 수정 중 오류가 발생했습니다.');
			}

			alert(data.message || '상품이 수정되었습니다.');
			window.location.reload();

		} catch (e) {
			console.error(e);
			alert(e.message || '상품 수정 중 오류가 발생했습니다.');
		}
	}

	function hasFinalRepresentativeImage() {
		if (state.representativeImage.newFile) {
			return true;
		}
		return !!(state.representativeImage.existing && !state.representativeImage.removeExisting);
	}

	function buildRequestPayload() {
		const usePriceReplacementText = getCheckboxValue('seller-product-detail-usePriceReplacementText');
		const useIconPeriod = getCheckboxValue('seller-product-detail-useIconPeriod');

		return {
			displayStatus: getValue('seller-product-detail-displayStatus'),
			saleStatus: getValue('seller-product-detail-saleStatus'),
			state: getValue('seller-product-detail-state'),
			newState: getValue('seller-product-detail-newState'),

			name: getValue('seller-product-detail-name'),
			code: getValue('seller-product-detail-code'),
			manufacturerText: getValue('seller-product-detail-manufacturerText'),
			supplierText: getValue('seller-product-detail-supplierText'),
			manufacturedAt: getDateValue('seller-product-detail-manufacturedAt'),
			expiredAt: getDateValue('seller-product-detail-expiredAt'),

			detailHtml: state.editor.getData(),
			summaryDescription: getValue('seller-product-detail-summaryDescription'),
			shortDescription: getValue('seller-product-detail-shortDescription'),
			internalProductCode: getValue('seller-product-detail-internalProductCode'),

			consumerPrice: getNumberValue('seller-product-detail-consumerPrice'),
			salePrice: getNumberValue('seller-product-detail-salePrice'),
			priceExposeTarget: getValue('seller-product-detail-priceExposeTarget'),
			usePriceReplacementText: usePriceReplacementText,
			priceReplacementText: usePriceReplacementText
				? getValue('seller-product-detail-priceReplacementText')
				: null,
			rewardRate: getFloatValue('seller-product-detail-rewardRate'),

			validFrom: getDateValue('seller-product-detail-validFrom'),
			validTo: getDateValue('seller-product-detail-validTo'),

			useIconPeriod: useIconPeriod,
			iconStartDate: useIconPeriod
				? getDateValue('seller-product-detail-iconStartDate')
				: null,
			iconEndDate: useIconPeriod
				? getDateValue('seller-product-detail-iconEndDate')
				: null,

			removeRepresentativeImage: state.representativeImage.removeExisting && !state.representativeImage.newFile,
			removeIconImage: state.iconImage.removeExisting && !state.iconImage.newFile,

			categoryMappings: state.selectedCategories.map(item => ({
				mediumId: item.mediumId,
				smallId: item.smallId
			})),

			extraFields: collectExtraFields(),
			keywords: [...state.keywords],
			optionGroups: collectOptionGroups(),
			additionalImageOrders: collectAdditionalImageOrders()
		};
	}

	function collectAdditionalImageOrders() {
		const container = document.getElementById('seller-product-detail-additionalPreview');
		return Array.from(container.children).map((node, index) => {
			const token = node.dataset.token;
			if (token.startsWith('EXISTING:')) {
				return {
					type: 'EXISTING',
					imageId: Number(token.replace('EXISTING:', '')),
					sortOrder: index
				};
			}
			return {
				type: 'NEW',
				uploadUid: token.replace('NEW:', ''),
				sortOrder: index
			};
		});
	}

	function collectExtraFields() {
		return Array.from(document.querySelectorAll('#seller-product-detail-extraFieldList .seller-product-detail-extra-row'))
			.map(row => ({
				label: row.querySelector('.seller-product-detail-extra-label').value.trim(),
				value: row.querySelector('.seller-product-detail-extra-value').value.trim()
			}))
			.filter(item => item.label || item.value);
	}

	function collectOptionGroups() {
		return Array.from(document.querySelectorAll('#seller-product-detail-optionGroupList .seller-product-detail-option-group-card'))
			.map((groupEl, groupIndex) => ({
				name: groupEl.querySelector('.seller-product-detail-option-group-name').value.trim(),
				sortOrder: groupIndex,
				options: Array.from(groupEl.querySelectorAll('.seller-product-detail-option-list .seller-product-detail-option-row'))
					.map((optionEl, optionIndex) => ({
						name: optionEl.querySelector('.seller-product-detail-option-name').value.trim(),
						value: optionEl.querySelector('.seller-product-detail-option-value').value.trim(),
						extraPrice: getInputNumber(optionEl.querySelector('.seller-product-detail-option-price')),
						sign: optionEl.querySelector('.seller-product-detail-option-sign').value,
						sortOrder: optionIndex
					}))
					.filter(option => option.name || option.value || option.extraPrice !== null)
			}))
			.filter(group => group.name || group.options.length > 0);
	}

	function setValue(id, value) {
		document.getElementById(id).value = value ?? '';
	}

	function setDateValue(id, value) {
		document.getElementById(id).value = value ?? '';
	}

	function setChecked(id, checked) {
		document.getElementById(id).checked = !!checked;
	}

	function setSelectValue(id, value) {
		const el = document.getElementById(id);
		if (el) {
			el.value = value ?? '';
		}
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