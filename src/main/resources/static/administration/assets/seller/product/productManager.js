(function () {
    'use strict';

    const state = {
        categoryTree: [],
        currentPage: 0,
        pageSize: 10,
        totalPages: 0
    };

    const el = {
        dateType: document.getElementById('seller-product-manager-date-type'),
        startDate: document.getElementById('seller-product-manager-start-date'),
        endDate: document.getElementById('seller-product-manager-end-date'),
        quickDateButtons: document.querySelectorAll('.seller-product-manager-quick-date-btn'),

        displayStatusCheckboxes: document.querySelectorAll('.seller-product-manager-display-status'),
        saleStatusCheckboxes: document.querySelectorAll('.seller-product-manager-sale-status'),

        searchType: document.getElementById('seller-product-manager-search-type'),
        keyword: document.getElementById('seller-product-manager-keyword'),

        largeId: document.getElementById('seller-product-manager-large-id'),
        mediumId: document.getElementById('seller-product-manager-medium-id'),
        smallId: document.getElementById('seller-product-manager-small-id'),

        pageSize: document.getElementById('seller-product-manager-page-size'),
        searchBtn: document.getElementById('seller-product-manager-search-btn'),
        resetBtn: document.getElementById('seller-product-manager-reset-btn'),
        deleteBtn: document.getElementById('seller-product-manager-delete-btn'),

        totalCount: document.getElementById('seller-product-manager-total-count'),
        tbody: document.getElementById('seller-product-manager-tbody'),
        pagination: document.getElementById('seller-product-manager-pagination'),
        checkAll: document.getElementById('seller-product-manager-check-all')
    };

    document.addEventListener('DOMContentLoaded', async function () {
        bindEvents();
        await loadFilterMeta();
        await loadList(0);
    });

    function bindEvents() {
        el.quickDateButtons.forEach(button => {
            button.addEventListener('click', function () {
                applyQuickDateRange(button.dataset.range);
            });
        });

        el.largeId.addEventListener('change', function () {
            updateMediumOptions();
            updateSmallOptions();
        });

        el.mediumId.addEventListener('change', function () {
            updateSmallOptions();
        });

        el.searchBtn.addEventListener('click', function () {
            loadList(0);
        });

        el.resetBtn.addEventListener('click', function () {
            resetFilters();
            loadList(0);
        });

        el.pageSize.addEventListener('change', function () {
            state.pageSize = parseInt(el.pageSize.value, 10) || 10;
            loadList(0);
        });

        el.keyword.addEventListener('keydown', function (event) {
            if (event.key === 'Enter') {
                event.preventDefault();
                loadList(0);
            }
        });

        el.checkAll.addEventListener('change', function () {
            document.querySelectorAll('.seller-product-manager-row-check').forEach(checkbox => {
                checkbox.checked = el.checkAll.checked;
            });
            syncDeleteButton();
        });

        el.deleteBtn.addEventListener('click', handleDeleteClick);
    }

    async function loadFilterMeta() {
        const response = await fetch('/seller/api/products/manager/filter-meta', {
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            alert('분류 정보를 불러오지 못했습니다.');
            return;
        }

        const data = await response.json();
        state.categoryTree = Array.isArray(data.largeCategories) ? data.largeCategories : [];
        renderLargeOptions();
        updateMediumOptions();
        updateSmallOptions();
    }

    async function loadList(page) {
        if (!validateDateRange()) {
            return;
        }

        showLoadingRow();

        const params = buildQueryParams(page);
        const response = await fetch('/seller/api/products/manager/list?' + params.toString(), {
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            renderErrorRow('상품 목록을 불러오지 못했습니다.');
            return;
        }

        const data = await response.json();

        if (data.totalPages > 0 && page > data.totalPages - 1) {
            await loadList(data.totalPages - 1);
            return;
        }

        state.currentPage = data.page || 0;
        state.totalPages = data.totalPages || 0;

        renderTable(data);
        renderPagination(data);
        syncDeleteButton();
    }

    function buildQueryParams(page) {
        const params = new URLSearchParams();

        params.append('dateType', el.dateType.value);
        params.append('page', String(page));
        params.append('size', el.pageSize.value);

        if (el.startDate.value) {
            params.append('startDate', el.startDate.value);
        }

        if (el.endDate.value) {
            params.append('endDate', el.endDate.value);
        }

        getCheckedValues(el.displayStatusCheckboxes).forEach(value => {
            params.append('displayStatuses', value);
        });

        getCheckedValues(el.saleStatusCheckboxes).forEach(value => {
            params.append('saleStatuses', value);
        });

        if (el.searchType.value) {
            params.append('searchType', el.searchType.value);
        }

        const keyword = el.keyword.value.trim();
        if (keyword) {
            params.append('keyword', keyword);
        }

        if (el.largeId.value) {
            params.append('largeId', el.largeId.value);
        }

        if (el.mediumId.value) {
            params.append('mediumId', el.mediumId.value);
        }

        if (el.smallId.value) {
            params.append('smallId', el.smallId.value);
        }

        return params;
    }

    function getCheckedValues(nodeList) {
        return Array.from(nodeList)
            .filter(checkbox => checkbox.checked)
            .map(checkbox => checkbox.value);
    }

    function renderLargeOptions() {
        const currentValue = el.largeId.value;
        el.largeId.innerHTML = '<option value="">전체</option>';

        state.categoryTree.forEach(large => {
            const option = document.createElement('option');
            option.value = String(large.id);
            option.textContent = large.name;
            el.largeId.appendChild(option);
        });

        if (currentValue) {
            el.largeId.value = currentValue;
        }
    }

    function updateMediumOptions() {
        const selectedLargeId = parseLong(el.largeId.value);
        const currentMediumId = el.mediumId.value;

        el.mediumId.innerHTML = '<option value="">전체</option>';

        if (!selectedLargeId) {
            el.mediumId.value = '';
            return;
        }

        const largeNode = state.categoryTree.find(item => item.id === selectedLargeId);
        const mediums = largeNode && Array.isArray(largeNode.mediums) ? largeNode.mediums : [];

        mediums.forEach(medium => {
            const option = document.createElement('option');
            option.value = String(medium.id);
            option.textContent = medium.name;
            el.mediumId.appendChild(option);
        });

        if (currentMediumId && mediums.some(medium => String(medium.id) === currentMediumId)) {
            el.mediumId.value = currentMediumId;
        } else {
            el.mediumId.value = '';
        }
    }

    function updateSmallOptions() {
        const selectedLargeId = parseLong(el.largeId.value);
        const selectedMediumId = parseLong(el.mediumId.value);
        const currentSmallId = el.smallId.value;

        el.smallId.innerHTML = '<option value="">전체</option>';

        if (!selectedLargeId || !selectedMediumId) {
            el.smallId.value = '';
            return;
        }

        const largeNode = state.categoryTree.find(item => item.id === selectedLargeId);
        const mediumNode = largeNode && Array.isArray(largeNode.mediums)
            ? largeNode.mediums.find(item => item.id === selectedMediumId)
            : null;

        const smalls = mediumNode && Array.isArray(mediumNode.smalls) ? mediumNode.smalls : [];

        smalls.forEach(small => {
            const option = document.createElement('option');
            option.value = String(small.id);
            option.textContent = small.name;
            el.smallId.appendChild(option);
        });

        if (currentSmallId && smalls.some(small => String(small.id) === currentSmallId)) {
            el.smallId.value = currentSmallId;
        } else {
            el.smallId.value = '';
        }
    }

    function renderTable(data) {
        el.totalCount.textContent = formatNumber(data.totalElements || 0);
        el.checkAll.checked = false;

        const content = Array.isArray(data.content) ? data.content : [];

        if (content.length === 0) {
            el.tbody.innerHTML = `
                <tr>
                    <td colspan="9" class="seller-product-manager-empty-row">
                        조회된 상품이 없습니다.
                    </td>
                </tr>
            `;
            return;
        }

        const rowsHtml = content.map(item => {
            const categoryHtml = renderCategoryPaths(item.categoryPaths);
            const imageHtml = renderImage(item.mainImageUrl);
            const consumerPriceHtml = item.consumerPrice != null ? `${formatNumber(item.consumerPrice)}원` : '-';
            const salePriceHtml = item.salePrice != null ? `${formatNumber(item.salePrice)}원` : '-';

            return `
                <tr>
                    <td class="seller-product-manager-check-cell">
                        <div class="seller-product-manager-row-check-wrap">
                            <input type="checkbox"
                                   class="form-check-input seller-product-manager-row-check"
                                   value="${item.dealerProductId}">
                        </div>
                    </td>
                    <td class="text-center">${escapeHtml(item.code || '-')}</td>
                    <td>${categoryHtml}</td>
                    <td class="text-center">${imageHtml}</td>
                    <td>
                        <a href="${escapeHtml(item.detailUrl || '#')}"
                           class="seller-product-manager-name-link">
                            ${escapeHtml(item.name || '-')}
                        </a>
                    </td>
                    <td class="text-end seller-product-manager-price">${consumerPriceHtml}</td>
                    <td class="text-end seller-product-manager-price">${salePriceHtml}</td>
                    <td class="seller-product-manager-status-cell">
                        ${renderStatusBadge(item.saleStatus, item.saleStatusLabel)}
                    </td>
                    <td class="seller-product-manager-status-cell">
                        ${renderStatusBadge(item.displayStatus, item.displayStatusLabel)}
                    </td>
                </tr>
            `;
        }).join('');

        el.tbody.innerHTML = rowsHtml;

        document.querySelectorAll('.seller-product-manager-row-check').forEach(checkbox => {
            checkbox.addEventListener('change', function () {
                syncHeaderCheckbox();
                syncDeleteButton();
            });
        });
    }

    function renderPagination(data) {
        el.pagination.innerHTML = '';

        const totalPages = data.totalPages || 0;
        const currentPage = data.page || 0;

        if (totalPages <= 1) {
            return;
        }

        const fragment = document.createDocumentFragment();

        fragment.appendChild(createPageItem('«', currentPage === 0, () => loadList(0)));
        fragment.appendChild(createPageItem('‹', currentPage === 0, () => loadList(currentPage - 1)));

        const startPage = Math.max(0, currentPage - 2);
        const endPage = Math.min(totalPages - 1, currentPage + 2);

        for (let i = startPage; i <= endPage; i++) {
            fragment.appendChild(createPageItem(String(i + 1), false, () => loadList(i), i === currentPage));
        }

        fragment.appendChild(createPageItem('›', currentPage >= totalPages - 1, () => loadList(currentPage + 1)));
        fragment.appendChild(createPageItem('»', currentPage >= totalPages - 1, () => loadList(totalPages - 1)));

        el.pagination.appendChild(fragment);
    }

    function createPageItem(text, disabled, onClick, active) {
        const li = document.createElement('li');
        li.className = 'page-item' + (disabled ? ' disabled' : '') + (active ? ' active' : '');

        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'page-link';
        button.textContent = text;

        if (!disabled) {
            button.addEventListener('click', onClick);
        }

        li.appendChild(button);
        return li;
    }

    function renderCategoryPaths(categoryPaths) {
        if (!Array.isArray(categoryPaths) || categoryPaths.length === 0) {
            return '<span class="text-muted">-</span>';
        }

        return `
            <div class="seller-product-manager-category-list">
                ${categoryPaths.map(path => `
                    <div class="seller-product-manager-category-item">${escapeHtml(path)}</div>
                `).join('')}
            </div>
        `;
    }

    function renderImage(imageUrl) {
        if (!imageUrl) {
            return `
                <div class="seller-product-manager-thumb">
                    <div class="seller-product-manager-thumb-empty">NO<br>IMAGE</div>
                </div>
            `;
        }

        return `
            <div class="seller-product-manager-thumb">
                <img src="${escapeHtml(imageUrl)}" alt="대표이미지">
            </div>
        `;
    }

    function renderStatusBadge(status, label) {
        let badgeClass = 'bg-secondary-subtle text-secondary';

        if (status === 'ON') {
            badgeClass = 'bg-success-subtle text-success';
        } else if (status === 'OFF') {
            badgeClass = 'bg-danger-subtle text-danger';
        }

        return `<span class="badge ${badgeClass}">${escapeHtml(label || '-')}</span>`;
    }

    function showLoadingRow() {
        el.tbody.innerHTML = `
            <tr>
                <td colspan="9" class="seller-product-manager-loading">
                    데이터를 불러오는 중입니다.
                </td>
            </tr>
        `;
    }

    function renderErrorRow(message) {
        el.tbody.innerHTML = `
            <tr>
                <td colspan="9" class="seller-product-manager-empty-row">
                    ${escapeHtml(message)}
                </td>
            </tr>
        `;
        el.totalCount.textContent = '0';
        el.pagination.innerHTML = '';
        el.checkAll.checked = false;
        syncDeleteButton();
    }

    function syncHeaderCheckbox() {
        const rowChecks = Array.from(document.querySelectorAll('.seller-product-manager-row-check'));

        if (rowChecks.length === 0) {
            el.checkAll.checked = false;
            return;
        }

        el.checkAll.checked = rowChecks.every(checkbox => checkbox.checked);
    }

    function syncDeleteButton() {
        const checkedCount = document.querySelectorAll('.seller-product-manager-row-check:checked').length;
        el.deleteBtn.disabled = checkedCount === 0;
    }

    async function handleDeleteClick() {
        const checkedIds = Array.from(document.querySelectorAll('.seller-product-manager-row-check:checked'))
            .map(checkbox => parseLong(checkbox.value))
            .filter(value => value !== null);

        if (checkedIds.length === 0) {
            alert('삭제할 상품을 선택해 주세요.');
            return;
        }

        if (!confirm('삭제 하시겠습니까?')) {
            return;
        }

        const response = await fetch('/seller/api/products/manager/delete', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({
                dealerProductIds: checkedIds
            })
        });

        if (!response.ok) {
            alert('삭제 처리에 실패했습니다.');
            return;
        }

        const data = await response.json();
        alert(data.message || '처리가 완료되었습니다.');
        await loadList(state.currentPage);
    }

    function resetFilters() {
        el.dateType.value = 'CREATED_AT';
        el.startDate.value = '';
        el.endDate.value = '';

        el.displayStatusCheckboxes.forEach(checkbox => {
            checkbox.checked = false;
        });

        el.saleStatusCheckboxes.forEach(checkbox => {
            checkbox.checked = false;
        });

        el.searchType.value = 'PRODUCT_NAME';
        el.keyword.value = '';

        el.largeId.value = '';
        updateMediumOptions();
        updateSmallOptions();

        el.pageSize.value = '10';
        state.pageSize = 10;

        el.checkAll.checked = false;
        syncDeleteButton();
    }

    function applyQuickDateRange(range) {
        const today = new Date();
        let start = new Date(today);

        if (range === 'today') {
            start = new Date(today);
        } else if (range === '7days') {
            start.setDate(today.getDate() - 6);
        } else if (range === '1month') {
            start.setMonth(today.getMonth() - 1);
        }

        el.startDate.value = formatDate(start);
        el.endDate.value = formatDate(today);
    }

    function validateDateRange() {
        if (el.startDate.value && el.endDate.value && el.startDate.value > el.endDate.value) {
            alert('시작일은 종료일보다 클 수 없습니다.');
            return false;
        }
        return true;
    }

    function formatDate(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    function formatNumber(value) {
        return new Intl.NumberFormat('ko-KR').format(value);
    }

    function parseLong(value) {
        if (value === null || value === undefined || value === '') {
            return null;
        }

        const parsed = Number(value);
        return Number.isNaN(parsed) ? null : parsed;
    }

    function escapeHtml(value) {
        return String(value)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }
})();