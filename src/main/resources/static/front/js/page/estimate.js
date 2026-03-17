(function () {
    const $ = (selector, parent = document) => parent.querySelector(selector);
    const $$ = (selector, parent = document) => Array.from(parent.querySelectorAll(selector));

    const CATEGORY_API_BASE = '/api/category';
    const ESTIMATE_API_BASE = '/customer/api/estimate';

    const state = {
        initialProductId: window.CUSTOMER_ESTIMATE_INITIAL_PRODUCT_ID || null,
        initialMappingId: window.CUSTOMER_ESTIMATE_INITIAL_MAPPING_ID || null,
        latestSearchItems: [],
        selectedMap: new Map(),
        currentLargeId: '',
        currentMediumId: '',
        currentSmallId: '',
        hasSearched: false,
        brandSuggestRequestSeq: 0,
        productSuggestRequestSeq: 0,
        selectedFiles: []
    };

    const el = {
        large: $('#estimate-page-catL'),
        medium: $('#estimate-page-catM'),
        small: $('#estimate-page-catS'),
        brandKeyword: $('#customer-estimate-brand-keyword'),
        productKeyword: $('#customer-estimate-product-keyword'),
        brandSuggest: $('#customer-estimate-brand-suggest'),
        productSuggest: $('#customer-estimate-product-suggest'),
        tbody: $('#estimate-page-tbody'),
        checkAll: $('#estimate-page-checkAll'),
        detail: $('#estimate-page-detail'),
        title: $('#customer-estimate-title'),
        files: $('#customer-estimate-files'),
        fileList: $('#customer-estimate-file-list'),
        searchBtn: $('#customer-estimate-search-btn'),
        applyBtn: $('#estimate-page-applyBtn')
    };

    function escapeHtml(value) {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function debounce(fn, delay) {
        let timer = null;
        return function (...args) {
            clearTimeout(timer);
            timer = setTimeout(() => fn.apply(this, args), delay);
        };
    }

    async function fetchJson(url, options) {
        const response = await fetch(url, options);
        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || '요청 처리 중 오류가 발생했습니다.');
        }
        return response.json();
    }

    function setOptions(select, items, placeholder, selectedValue) {
        select.innerHTML = '';

        const defaultOption = document.createElement('option');
        defaultOption.value = '';
        defaultOption.textContent = placeholder;
        select.appendChild(defaultOption);

        (items || []).forEach(item => {
            const option = document.createElement('option');
            option.value = String(item.id);
            option.textContent = item.name;
            if (selectedValue != null && String(item.id) === String(selectedValue)) {
                option.selected = true;
            }
            select.appendChild(option);
        });
    }

    async function loadLargeCategories(selectedValue) {
        const items = await fetchJson(`${CATEGORY_API_BASE}/list-large`);
        setOptions(el.large, items, '대분류 전체', selectedValue);
    }

    async function loadMediumCategories(largeId, selectedValue) {
        if (!largeId) {
            setOptions(el.medium, [], '중분류 전체');
            setOptions(el.small, [], '소분류 전체');
            return;
        }

        const items = await fetchJson(`${CATEGORY_API_BASE}/list-medium?largeId=${encodeURIComponent(largeId)}`);
        setOptions(el.medium, items, '중분류 전체', selectedValue);

        if (!selectedValue) {
            setOptions(el.small, [], '소분류 전체');
        }
    }

    async function loadSmallCategories(mediumId, selectedValue) {
        if (!mediumId) {
            setOptions(el.small, [], '소분류 전체');
            return;
        }

        const items = await fetchJson(`${CATEGORY_API_BASE}/list-small?mediumId=${encodeURIComponent(mediumId)}`);
        setOptions(el.small, items, '소분류 전체', selectedValue);
    }

    async function loadInitialSelection() {
        if (!state.initialProductId && !state.initialMappingId) {
            return;
        }

        const params = new URLSearchParams();
        if (state.initialProductId) {
            params.set('productId', state.initialProductId);
        }
        if (state.initialMappingId) {
            params.set('mappingId', state.initialMappingId);
        }

        const data = await fetchJson(`${ESTIMATE_API_BASE}/form/init?${params.toString()}`);
        const selectedItems = data.selectedItems || [];

        selectedItems.forEach(item => {
            state.selectedMap.set(String(item.mappingId), {
                ...item,
                quantity: 1
            });
        });

        if (selectedItems.length > 0) {
            const first = selectedItems[0];

            state.currentLargeId = String(first.largeId || '');
            state.currentMediumId = String(first.mediumId || '');
            state.currentSmallId = String(first.smallId || '');

            await loadLargeCategories(state.currentLargeId);
            await loadMediumCategories(state.currentLargeId, state.currentMediumId);
            await loadSmallCategories(state.currentMediumId, state.currentSmallId);

            el.large.value = state.currentLargeId;
            el.medium.value = state.currentMediumId;
            el.small.value = state.currentSmallId;
        }
    }

    function buildSearchParams() {
        const params = new URLSearchParams();

        const largeId = (el.large.value || '').trim();
        const mediumId = (el.medium.value || '').trim();
        const smallId = (el.small.value || '').trim();
        const productKeyword = (el.productKeyword.value || '').trim();
        const brandKeyword = (el.brandKeyword.value || '').trim();

        if (largeId) params.set('largeId', largeId);
        if (mediumId) params.set('mediumId', mediumId);
        if (smallId) params.set('smallId', smallId);
        if (productKeyword) params.set('productKeyword', productKeyword);
        if (brandKeyword) params.set('brandKeyword', brandKeyword);

        return params;
    }

    function buildBrandSuggestParams() {
        const params = new URLSearchParams();
        const brandKeyword = (el.brandKeyword.value || '').trim();

        if (brandKeyword) {
            params.set('brandKeyword', brandKeyword);
        }

        return params;
    }

    function buildProductSuggestParams() {
        return buildSearchParams();
    }

    async function searchProducts() {
        const params = buildSearchParams();
        const data = await fetchJson(`${ESTIMATE_API_BASE}/products?${params.toString()}`);

        state.latestSearchItems = data.items || [];
        state.hasSearched = true;
        renderRows();
    }

    function buildMergedRows() {
        const latestMap = new Map(
            state.latestSearchItems.map(item => [String(item.mappingId), item])
        );

        const selectedRows = Array.from(state.selectedMap.values()).map(saved => {
            const latest = latestMap.get(String(saved.mappingId)) || saved;
            return {
                ...latest,
                quantity: saved.quantity ?? 1,
                _selectedPinned: true
            };
        });

        if (!state.hasSearched) {
            return selectedRows;
        }

        const restRows = state.latestSearchItems
            .filter(item => !state.selectedMap.has(String(item.mappingId)))
            .map(item => ({
                ...item,
                quantity: 1,
                _selectedPinned: false
            }));

        return [...selectedRows, ...restRows];
    }

    function renderEmptyRow(message) {
        el.tbody.innerHTML = `
            <tr class="estimate-page-empty-row">
                <td colspan="5" style="text-align:center;padding:30px 10px;">${escapeHtml(message)}</td>
            </tr>
        `;
        el.checkAll.checked = false;
        el.checkAll.indeterminate = false;
        updateApplyBtn();
    }

    function renderRows() {
        const rows = buildMergedRows();
        el.tbody.innerHTML = '';

        if (!rows.length) {
            if (state.hasSearched) {
                renderEmptyRow('조회된 상품이 없습니다.');
            } else {
                renderEmptyRow('검색 조건을 입력한 뒤 검색 버튼을 눌러 주세요.');
            }
            return;
        }

        rows.forEach(row => {
            const selected = state.selectedMap.get(String(row.mappingId));
            const quantity = selected?.quantity ?? row.quantity ?? 1;

            const tr = document.createElement('tr');

            if (selected) {
                tr.classList.add('customer-estimate-td-selected');
            }

            tr.innerHTML = `
                <td>
                    <input type="checkbox"
                           class="estimate-page-check"
                           data-mapping-id="${escapeHtml(row.mappingId)}"
                           ${selected ? 'checked' : ''}>
                </td>
                <td>
                    ${escapeHtml(row.largeName)} &gt; ${escapeHtml(row.mediumName)} &gt; ${escapeHtml(row.smallName)}
                </td>
                <td>${escapeHtml(row.brandName || '-')}</td>
                <td class="estimate-page-td-prod">
                    <div class="estimate-page-prod">
                        <img src="${escapeHtml(row.imageUrl || '/front/image/sample/100-100.png')}"
                             alt="${escapeHtml(row.productName)}">
                        <span class="estimate-page-prod-name">
                            ${escapeHtml(row.productName)}
                            ${selected ? '<span class="customer-estimate-selected-pin">선택됨</span>' : ''}
                        </span>
                    </div>
                </td>
                <td>
                    <input type="number"
                           min="1"
                           value="${quantity}"
                           class="estimate-page-qty"
                           data-mapping-id="${escapeHtml(row.mappingId)}">
                </td>
            `;

            tr.dataset.mappingId = String(row.mappingId);
            tr.dataset.row = JSON.stringify(row);
            el.tbody.appendChild(tr);
        });

        syncCheckAllState();
        updateApplyBtn();
    }

    function syncCheckAllState() {
        const checkboxes = $$('.estimate-page-check', el.tbody);
        const checkedCount = checkboxes.filter(node => node.checked).length;

        el.checkAll.checked = checkboxes.length > 0 && checkedCount === checkboxes.length;
        el.checkAll.indeterminate = checkedCount > 0 && checkedCount < checkboxes.length;
    }

    function updateApplyBtn() {
        el.applyBtn.disabled = state.selectedMap.size === 0;
    }

    function clampQuantity(value) {
        const num = Number(value);
        if (!Number.isFinite(num) || num < 1) {
            return 1;
        }
        return Math.floor(num);
    }

    function highlightMatch(text, keyword) {
        const source = String(text ?? '');
        const q = String(keyword ?? '').trim();

        if (!q) {
            return escapeHtml(source);
        }

        const lowerSource = source.toLowerCase();
        const lowerKeyword = q.toLowerCase();
        const index = lowerSource.indexOf(lowerKeyword);

        if (index < 0) {
            return escapeHtml(source);
        }

        const before = source.substring(0, index);
        const match = source.substring(index, index + q.length);
        const after = source.substring(index + q.length);

        return `${escapeHtml(before)}<span class="customer-estimate-suggest-match">${escapeHtml(match)}</span>${escapeHtml(after)}`;
    }

    function showSuggestionBox(box) {
        box.classList.remove('d-none');
    }

    function hideSuggestionBox(box) {
        box.classList.add('d-none');
        box.innerHTML = '';
    }

    function closeBrandSuggest() {
        state.brandSuggestRequestSeq += 1;
        hideSuggestionBox(el.brandSuggest);
    }

    function closeProductSuggest() {
        state.productSuggestRequestSeq += 1;
        hideSuggestionBox(el.productSuggest);
    }

    function closeAllSuggestBoxes() {
        closeBrandSuggest();
        closeProductSuggest();
    }

    function renderSuggestionBox(box, typeLabel, keyword, items, onClick) {
        const trimmedKeyword = (keyword || '').trim();

        if (!trimmedKeyword) {
            hideSuggestionBox(box);
            return;
        }

        let html = `
            <div class="customer-estimate-suggest-head">
                <div class="customer-estimate-suggest-head-label">${escapeHtml(typeLabel)} 입력값</div>
                <div class="customer-estimate-suggest-head-keyword">${escapeHtml(trimmedKeyword)}</div>
            </div>
        `;

        if (!items || items.length === 0) {
            html += `
                <div class="customer-estimate-suggest-empty">
                    포함되는 ${escapeHtml(typeLabel)}이 없습니다.
                </div>
            `;
        } else {
            html += items.map(item => `
                <button type="button"
                        class="customer-estimate-suggest-item"
                        data-value="${escapeHtml(item)}">
                    ${highlightMatch(item, trimmedKeyword)}
                </button>
            `).join('');
        }

        box.innerHTML = html;
        showSuggestionBox(box);

        $$('.customer-estimate-suggest-item', box).forEach(node => {
            node.addEventListener('click', () => {
                const value = node.dataset.value || '';
                onClick(value);
            });
        });
    }

    async function loadBrandSuggestions() {
        const keyword = (el.brandKeyword.value || '').trim();
        const seq = ++state.brandSuggestRequestSeq;

        if (!keyword) {
            hideSuggestionBox(el.brandSuggest);
            return;
        }

        const params = buildBrandSuggestParams();
        const data = await fetchJson(`${ESTIMATE_API_BASE}/brands/suggest?${params.toString()}`);

        if (seq !== state.brandSuggestRequestSeq) {
            return;
        }

        renderSuggestionBox(
            el.brandSuggest,
            '브랜드명',
            keyword,
            data || [],
            (value) => {
                el.brandKeyword.value = value;
                hideSuggestionBox(el.brandSuggest);
            }
        );
    }

    async function loadProductSuggestions() {
        const keyword = (el.productKeyword.value || '').trim();
        const seq = ++state.productSuggestRequestSeq;

        if (!keyword) {
            hideSuggestionBox(el.productSuggest);
            return;
        }

        const params = buildProductSuggestParams();
        const data = await fetchJson(`${ESTIMATE_API_BASE}/products/suggest?${params.toString()}`);

        if (seq !== state.productSuggestRequestSeq) {
            return;
        }

        renderSuggestionBox(
            el.productSuggest,
            '제품명',
            keyword,
            data || [],
            (value) => {
                el.productKeyword.value = value;
                hideSuggestionBox(el.productSuggest);
            }
        );
    }

    function getFileKey(file) {
        return `${file.name}__${file.size}__${file.lastModified}`;
    }

    function formatFileSize(bytes) {
        const size = Number(bytes || 0);

        if (size < 1024) {
            return `${size} B`;
        }
        if (size < 1024 * 1024) {
            return `${(size / 1024).toFixed(1)} KB`;
        }
        return `${(size / (1024 * 1024)).toFixed(1)} MB`;
    }

    function syncSelectedFilesToInput() {
        const dataTransfer = new DataTransfer();
        state.selectedFiles.forEach(file => dataTransfer.items.add(file));
        el.files.files = dataTransfer.files;
    }

    function renderSelectedFiles() {
        if (!state.selectedFiles.length) {
            el.fileList.innerHTML = '';
            return;
        }

        el.fileList.innerHTML = state.selectedFiles.map((file, index) => `
            <div class="customer-estimate-file-item">
                <button type="button"
                        class="customer-estimate-file-remove-btn"
                        data-file-index="${index}"
                        aria-label="${escapeHtml(file.name)} 삭제">
                    ×
                </button>
                <div class="customer-estimate-file-name" title="${escapeHtml(file.name)}">
                    ${escapeHtml(file.name)}
                </div>
                <div class="customer-estimate-file-size">
                    ${escapeHtml(formatFileSize(file.size))}
                </div>
            </div>
        `).join('');
    }

    function mergeSelectedFiles(newFiles) {
        const existingKeys = new Set(state.selectedFiles.map(getFileKey));

        newFiles.forEach(file => {
            const key = getFileKey(file);
            if (existingKeys.has(key)) {
                return;
            }
            state.selectedFiles.push(file);
            existingKeys.add(key);
        });

        syncSelectedFilesToInput();
        renderSelectedFiles();
    }

    function removeSelectedFile(index) {
        if (!Number.isInteger(index)) {
            return;
        }
        if (index < 0 || index >= state.selectedFiles.length) {
            return;
        }

        state.selectedFiles.splice(index, 1);
        syncSelectedFilesToInput();
        renderSelectedFiles();
    }

    function bindEvents() {
        el.large.addEventListener('change', async () => {
            closeAllSuggestBoxes();

            const largeId = (el.large.value || '').trim();
            await loadMediumCategories(largeId, null);
            setOptions(el.small, [], '소분류 전체');
        });

        el.medium.addEventListener('change', async () => {
            closeAllSuggestBoxes();

            const mediumId = (el.medium.value || '').trim();
            await loadSmallCategories(mediumId, null);
        });

        el.small.addEventListener('change', () => {
            closeAllSuggestBoxes();
        });

        el.searchBtn.addEventListener('click', async () => {
            closeAllSuggestBoxes();
            await searchProducts();
        });

        const onEnterSearch = async (event) => {
            if (event.key === 'Enter') {
                event.preventDefault();
                closeAllSuggestBoxes();
                await searchProducts();
            }
        };

        el.brandKeyword.addEventListener('keydown', onEnterSearch);
        el.productKeyword.addEventListener('keydown', onEnterSearch);

        const debouncedBrandSuggest = debounce(() => {
            closeProductSuggest();
            loadBrandSuggestions().catch(err => {
                console.error(err);
                hideSuggestionBox(el.brandSuggest);
            });
        }, 200);

        const debouncedProductSuggest = debounce(() => {
            closeBrandSuggest();
            loadProductSuggestions().catch(err => {
                console.error(err);
                hideSuggestionBox(el.productSuggest);
            });
        }, 200);

        el.brandKeyword.addEventListener('focus', () => {
            closeProductSuggest();

            if ((el.brandKeyword.value || '').trim()) {
                loadBrandSuggestions().catch(err => {
                    console.error(err);
                    hideSuggestionBox(el.brandSuggest);
                });
            } else {
                hideSuggestionBox(el.brandSuggest);
            }
        });

        el.brandKeyword.addEventListener('click', () => {
            closeProductSuggest();

            if ((el.brandKeyword.value || '').trim()) {
                loadBrandSuggestions().catch(err => {
                    console.error(err);
                    hideSuggestionBox(el.brandSuggest);
                });
            } else {
                hideSuggestionBox(el.brandSuggest);
            }
        });

        el.brandKeyword.addEventListener('input', debouncedBrandSuggest);

        el.productKeyword.addEventListener('focus', () => {
            closeBrandSuggest();

            if ((el.productKeyword.value || '').trim()) {
                loadProductSuggestions().catch(err => {
                    console.error(err);
                    hideSuggestionBox(el.productSuggest);
                });
            } else {
                hideSuggestionBox(el.productSuggest);
            }
        });

        el.productKeyword.addEventListener('click', () => {
            closeBrandSuggest();

            if ((el.productKeyword.value || '').trim()) {
                loadProductSuggestions().catch(err => {
                    console.error(err);
                    hideSuggestionBox(el.productSuggest);
                });
            } else {
                hideSuggestionBox(el.productSuggest);
            }
        });

        el.productKeyword.addEventListener('input', debouncedProductSuggest);

        document.addEventListener('focusin', (event) => {
            if (event.target === el.brandKeyword) {
                closeProductSuggest();
                return;
            }

            if (event.target === el.productKeyword) {
                closeBrandSuggest();
                return;
            }

            if (!event.target.closest('.customer-estimate-autocomplete-wrap')) {
                closeAllSuggestBoxes();
            }
        });

        document.addEventListener('click', (event) => {
            if (!event.target.closest('.customer-estimate-autocomplete-wrap')) {
                closeAllSuggestBoxes();
            }
        });

        el.checkAll.addEventListener('change', () => {
            const checked = el.checkAll.checked;
            const rows = $$('tr', el.tbody);

            rows.forEach(tr => {
                const chk = $('.estimate-page-check', tr);
                const qtyInput = $('.estimate-page-qty', tr);

                if (!chk || !qtyInput || !tr.dataset.row) {
                    return;
                }

                const rowData = JSON.parse(tr.dataset.row);
                chk.checked = checked;

                if (checked) {
                    state.selectedMap.set(String(rowData.mappingId), {
                        ...rowData,
                        quantity: clampQuantity(qtyInput.value || 1)
                    });
                } else {
                    state.selectedMap.delete(String(rowData.mappingId));
                }
            });

            renderRows();
        });

        el.tbody.addEventListener('change', (event) => {
            if (event.target.classList.contains('estimate-page-check')) {
                const tr = event.target.closest('tr');
                if (!tr || !tr.dataset.row) {
                    return;
                }

                const qtyInput = $('.estimate-page-qty', tr);
                const rowData = JSON.parse(tr.dataset.row);

                if (event.target.checked) {
                    state.selectedMap.set(String(rowData.mappingId), {
                        ...rowData,
                        quantity: clampQuantity(qtyInput ? qtyInput.value : 1)
                    });
                } else {
                    state.selectedMap.delete(String(rowData.mappingId));
                }

                renderRows();
                return;
            }

            if (event.target.classList.contains('estimate-page-qty')) {
                const mappingId = String(event.target.dataset.mappingId || '');
                const quantity = clampQuantity(event.target.value);
                event.target.value = String(quantity);

                if (state.selectedMap.has(mappingId)) {
                    const saved = state.selectedMap.get(mappingId);
                    saved.quantity = quantity;
                    state.selectedMap.set(mappingId, saved);
                }
            }
        });

        el.tbody.addEventListener('input', (event) => {
            if (!event.target.classList.contains('estimate-page-qty')) {
                return;
            }

            const mappingId = String(event.target.dataset.mappingId || '');
            const quantity = clampQuantity(event.target.value);

            if (state.selectedMap.has(mappingId)) {
                const saved = state.selectedMap.get(mappingId);
                saved.quantity = quantity;
                state.selectedMap.set(mappingId, saved);
            }
        });

        el.files.addEventListener('change', () => {
            const files = Array.from(el.files.files || []);
            mergeSelectedFiles(files);
        });

        el.fileList.addEventListener('click', (event) => {
            const removeBtn = event.target.closest('.customer-estimate-file-remove-btn');
            if (!removeBtn) {
                return;
            }

            const index = Number(removeBtn.dataset.fileIndex);
            removeSelectedFile(index);
        });

        el.applyBtn.addEventListener('click', async () => {
            if (state.selectedMap.size === 0) {
                alert('견적 상품을 선택해 주세요.');
                return;
            }

            if (!confirm('고객님의 이메일을 통해 답변을 드릴 예정입니다.\n견적문의를 등록하시겠습니까?')) {
                return;
            }

            const items = Array.from(state.selectedMap.values()).map(item => ({
                mappingId: Number(item.mappingId),
                quantity: clampQuantity(item.quantity || 1)
            }));

            const formData = new FormData();
            formData.append('title', (el.title.value || '').trim());
            formData.append('detailContent', (el.detail.value || '').trim());
            formData.append('itemsJson', JSON.stringify(items));

            state.selectedFiles.forEach(file => {
                formData.append('files', file);
            });

            const originalButtonText = el.applyBtn.textContent;
            el.applyBtn.disabled = true;
            el.applyBtn.textContent = '등록중...';

            try {
                const response = await fetch(`${ESTIMATE_API_BASE}`, {
                    method: 'POST',
                    body: formData
                });

                if (!response.ok) {
                    const text = await response.text();
                    throw new Error(text || '견적 등록 중 오류가 발생했습니다.');
                }

                const data = await response.json();
                alert(data.message || '견적 문의가 등록되었습니다.');
                location.href = '/customer/estimateList';
            } catch (error) {
                console.error(error);
                alert(error.message || '견적 등록 중 오류가 발생했습니다.');
                updateApplyBtn();
                el.applyBtn.textContent = originalButtonText;
            }
        });
    }

    async function init() {
        bindEvents();
        await loadLargeCategories(null);
        await loadInitialSelection();
        renderRows();
        renderSelectedFiles();
    }

    init().catch(error => {
        console.error(error);
        alert('견적 페이지 초기화 중 오류가 발생했습니다.');
    });
})();