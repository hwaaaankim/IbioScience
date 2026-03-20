(function () {
    'use strict';

    const state = {
        memberId: null,
        page: 0,
        size: 10,
        totalPages: 0,
        totalElements: 0,
        content: []
    };

    const elements = {};

    document.addEventListener('DOMContentLoaded', init);

    async function init() {
        const pageRoot = document.getElementById('client-detail-wishlist-page');
        if (!pageRoot) {
            return;
        }

        state.memberId = pageRoot.dataset.memberId;

        bindElements();
        bindEvents();

        await loadLargeCategories();
        await fetchWishList(0);
    }

    function bindElements() {
        elements.pageRoot = document.getElementById('client-detail-wishlist-page');
        elements.form = document.getElementById('client-detail-wishlist-search-form');
        elements.size = document.getElementById('client-detail-wishlist-size');
        elements.fromDate = document.getElementById('client-detail-wishlist-from-date');
        elements.toDate = document.getElementById('client-detail-wishlist-to-date');
        elements.largeId = document.getElementById('client-detail-wishlist-large-id');
        elements.mediumId = document.getElementById('client-detail-wishlist-medium-id');
        elements.smallId = document.getElementById('client-detail-wishlist-small-id');
        elements.productName = document.getElementById('client-detail-wishlist-product-name');
        elements.resetBtn = document.getElementById('client-detail-wishlist-reset-btn');
        elements.searchBtn = document.getElementById('client-detail-wishlist-search-btn');
        elements.deleteBtn = document.getElementById('client-detail-wishlist-delete-btn');
        elements.totalCount = document.getElementById('client-detail-wishlist-total-count');
        elements.tbody = document.getElementById('client-detail-wishlist-tbody');
        elements.pagination = document.getElementById('client-detail-wishlist-pagination');
        elements.checkAll = document.getElementById('client-detail-wishlist-check-all');
    }

    function bindEvents() {
        elements.form.addEventListener('submit', async function (e) {
            e.preventDefault();
            await fetchWishList(0);
        });

        elements.resetBtn.addEventListener('click', async function () {
            resetSearchForm();
            await fetchWishList(0);
        });

        elements.size.addEventListener('change', async function () {
            await fetchWishList(0);
        });

        elements.largeId.addEventListener('change', async function () {
            await handleLargeCategoryChange();
        });

        elements.mediumId.addEventListener('change', async function () {
            await handleMediumCategoryChange();
        });

        elements.checkAll.addEventListener('change', function () {
            const rowChecks = getRowChecks();
            rowChecks.forEach(function (checkbox) {
                checkbox.checked = elements.checkAll.checked;
            });
            updateDeleteButtonState();
        });

        elements.deleteBtn.addEventListener('click', async function () {
            await deleteSelectedWishListItems();
        });
    }

    async function handleLargeCategoryChange() {
        const largeId = elements.largeId.value;

        resetSelect(elements.mediumId, '전체');
        resetSelect(elements.smallId, '전체');

        if (largeId) {
            await loadMediumCategories(largeId);
        }
    }

    async function handleMediumCategoryChange() {
        const mediumId = elements.mediumId.value;

        resetSelect(elements.smallId, '전체');

        if (mediumId) {
            await loadSmallCategories(mediumId);
        }
    }

    function resetSearchForm() {
        elements.size.value = '10';
        elements.fromDate.value = '';
        elements.toDate.value = '';
        elements.productName.value = '';

        resetSelect(elements.largeId, '전체');
        resetSelect(elements.mediumId, '전체');
        resetSelect(elements.smallId, '전체');

        loadLargeCategories();
        updateDeleteButtonState();
    }

    async function loadLargeCategories() {
        try {
            const data = await fetchJson('/api/category/list-large');
            setSelectOptions(elements.largeId, data, '전체');
            resetSelect(elements.mediumId, '전체');
            resetSelect(elements.smallId, '전체');
        } catch (e) {
            alert(e.message || '대분류 목록을 불러오지 못했습니다.');
        }
    }

    async function loadMediumCategories(largeId) {
        try {
            const data = await fetchJson('/api/category/list-medium?largeId=' + encodeURIComponent(largeId));
            setSelectOptions(elements.mediumId, data, '전체');
        } catch (e) {
            alert(e.message || '중분류 목록을 불러오지 못했습니다.');
        }
    }

    async function loadSmallCategories(mediumId) {
        try {
            const data = await fetchJson('/api/category/list-small?mediumId=' + encodeURIComponent(mediumId));
            setSelectOptions(elements.smallId, data, '전체');
        } catch (e) {
            alert(e.message || '소분류 목록을 불러오지 못했습니다.');
        }
    }

    async function fetchWishList(page) {
        if (!validateDateRange()) {
            return;
        }

        state.page = page;
        state.size = parseInt(elements.size.value || '10', 10);

        renderLoading();

        try {
            const params = new URLSearchParams();
            params.set('page', String(page));
            params.set('size', String(state.size));

            if (elements.fromDate.value) {
                params.set('fromDate', elements.fromDate.value);
            }

            if (elements.toDate.value) {
                params.set('toDate', elements.toDate.value);
            }

            if (elements.largeId.value) {
                params.set('largeId', elements.largeId.value);
            }

            if (elements.mediumId.value) {
                params.set('mediumId', elements.mediumId.value);
            }

            if (elements.smallId.value) {
                params.set('smallId', elements.smallId.value);
            }

            if (elements.productName.value.trim()) {
                params.set('productName', elements.productName.value.trim());
            }

            const url = '/admin/root/api/clientDetail/' + encodeURIComponent(state.memberId) + '/wishList?' + params.toString();
            const response = await fetchJson(url);

            state.page = response.page;
            state.size = response.size;
            state.totalPages = response.totalPages;
            state.totalElements = response.totalElements;
            state.content = Array.isArray(response.content) ? response.content : [];

            renderTable(state.content);
            renderPagination(state.page, state.totalPages);
            updateTotalCount(state.totalElements);
            resetHeaderCheckbox();
            updateDeleteButtonState();
        } catch (e) {
            renderError(e.message || '관심상품 목록을 조회하지 못했습니다.');
            renderPagination(0, 0);
            updateTotalCount(0);
            resetHeaderCheckbox();
            updateDeleteButtonState();
        }
    }

    async function deleteSelectedWishListItems() {
        const selectedIds = getSelectedWishListItemIds();

        if (selectedIds.length === 0) {
            alert('삭제할 관심상품을 선택해 주세요.');
            return;
        }

        const confirmed = confirm('해당 유저의 위시리스트를 삭제하시겠습니까?');
        if (!confirmed) {
            return;
        }

        try {
            const headers = {
                'Content-Type': 'application/json'
            };
            applyCsrfHeader(headers);

            const response = await fetch('/admin/root/api/clientDetail/' + encodeURIComponent(state.memberId) + '/wishList', {
                method: 'DELETE',
                headers: headers,
                body: JSON.stringify({
                    wishListItemIds: selectedIds
                })
            });

            if (!response.ok) {
                throw new Error(await extractErrorMessage(response));
            }

            const result = await response.json();
            alert((result.deletedCount || 0) + '건이 삭제되었습니다.');

            const isCurrentPageFullyDeleted = state.content.length > 0 && selectedIds.length === state.content.length;
            const nextPage = isCurrentPageFullyDeleted && state.page > 0 ? state.page - 1 : state.page;

            await fetchWishList(nextPage);
        } catch (e) {
            alert(e.message || '관심상품 삭제에 실패했습니다.');
        }
    }

    function renderLoading() {
        elements.tbody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center text-muted py-5">데이터를 불러오는 중입니다.</td>
            </tr>
        `;
    }

    function renderError(message) {
        elements.tbody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center text-danger py-5">${escapeHtml(message)}</td>
            </tr>
        `;
    }

    function renderTable(items) {
        if (!items || items.length === 0) {
            elements.tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="text-center text-muted py-5">조회된 관심상품이 없습니다.</td>
                </tr>
            `;
            return;
        }

        const rowsHtml = items.map(function (item) {
            const imageHtml = hasUsableImage(item.mainImageUrl)
                ? `<img src="${escapeAttribute(item.mainImageUrl)}" alt="대표이미지" class="client-detail-wishlist-thumbnail">`
                : `<div class="client-detail-wishlist-no-image">이미지 없음</div>`;

            const categoryHtml = formatCategoryPath(item.categoryPath);

            return `
                <tr>
                    <td>
                        <input type="checkbox"
                               class="form-check-input client-detail-wishlist-row-check"
                               value="${item.wishListItemId}">
                    </td>
                    <td>${escapeHtml(item.productName || '-')}</td>
                    <td class="client-detail-wishlist-category-text">${categoryHtml}</td>
                    <td>${escapeHtml(item.brandName || '-')}</td>
                    <td>${imageHtml}</td>
                    <td>
                        <a href="/admin/productDetail/${item.productId}"
                           class="btn btn-sm btn-outline-primary">
                            상세보기
                        </a>
                    </td>
                </tr>
            `;
        }).join('');

        elements.tbody.innerHTML = rowsHtml;

        getRowChecks().forEach(function (checkbox) {
            checkbox.addEventListener('change', function () {
                updateHeaderCheckboxState();
                updateDeleteButtonState();
            });
        });
    }

    function renderPagination(currentPage, totalPages) {
        if (!elements.pagination) {
            return;
        }

        if (!totalPages || totalPages <= 0) {
            elements.pagination.innerHTML = '';
            return;
        }

        const startPage = Math.floor(currentPage / 5) * 5 + 1;
        const endPage = Math.min(startPage + 4, totalPages);

        let html = '';
        html += createPageItem('FIRST', 0, currentPage === 0, false);
        html += createPageItem('PREV', currentPage - 1, currentPage === 0, false);

        for (let pageNumber = startPage; pageNumber <= endPage; pageNumber++) {
            html += createPageItem(String(pageNumber), pageNumber - 1, false, currentPage === (pageNumber - 1));
        }

        html += createPageItem('NEXT', currentPage + 1, currentPage >= totalPages - 1, false);
        html += createPageItem('LAST', totalPages - 1, currentPage >= totalPages - 1, false);

        elements.pagination.innerHTML = html;

        const buttons = elements.pagination.querySelectorAll('[data-page]');
        buttons.forEach(function (button) {
            button.addEventListener('click', async function () {
                const targetPage = parseInt(button.dataset.page, 10);
                if (Number.isNaN(targetPage)) {
                    return;
                }
                await fetchWishList(targetPage);
            });
        });
    }

    function createPageItem(label, page, disabled, active) {
        const liClass = [
            'page-item',
            disabled ? 'disabled' : '',
            active ? 'active' : ''
        ].filter(Boolean).join(' ');

        const disabledAttr = disabled ? 'tabindex="-1" aria-disabled="true"' : '';
        const buttonClass = active ? 'page-link fw-semibold' : 'page-link';

        return `
            <li class="${liClass}">
                <button type="button"
                        class="${buttonClass}"
                        data-page="${page}"
                        ${disabledAttr}>
                    ${label}
                </button>
            </li>
        `;
    }

    function updateTotalCount(totalCount) {
        elements.totalCount.textContent = String(totalCount || 0);
    }

    function updateDeleteButtonState() {
        const checkedCount = getSelectedWishListItemIds().length;
        elements.deleteBtn.disabled = checkedCount === 0;
    }

    function updateHeaderCheckboxState() {
        const rowChecks = getRowChecks();

        if (rowChecks.length === 0) {
            elements.checkAll.checked = false;
            elements.checkAll.indeterminate = false;
            elements.checkAll.disabled = true;
            return;
        }

        const checkedCount = rowChecks.filter(function (checkbox) {
            return checkbox.checked;
        }).length;

        elements.checkAll.disabled = false;
        elements.checkAll.checked = checkedCount === rowChecks.length;
        elements.checkAll.indeterminate = checkedCount > 0 && checkedCount < rowChecks.length;
    }

    function resetHeaderCheckbox() {
        elements.checkAll.checked = false;
        elements.checkAll.indeterminate = false;
        elements.checkAll.disabled = state.content.length === 0;
    }

    function getSelectedWishListItemIds() {
        return getRowChecks()
            .filter(function (checkbox) {
                return checkbox.checked;
            })
            .map(function (checkbox) {
                return parseInt(checkbox.value, 10);
            })
            .filter(function (id) {
                return !Number.isNaN(id);
            });
    }

    function getRowChecks() {
        return Array.from(document.querySelectorAll('.client-detail-wishlist-row-check'));
    }

    function setSelectOptions(selectEl, items, defaultLabel) {
        let html = `<option value="">${escapeHtml(defaultLabel)}</option>`;

        (items || []).forEach(function (item) {
            html += `<option value="${escapeAttribute(String(item.id))}">${escapeHtml(item.name)}</option>`;
        });

        selectEl.innerHTML = html;
    }

    function resetSelect(selectEl, defaultLabel) {
        selectEl.innerHTML = `<option value="">${escapeHtml(defaultLabel)}</option>`;
        selectEl.value = '';
    }

    function validateDateRange() {
        if (elements.fromDate.value && elements.toDate.value && elements.fromDate.value > elements.toDate.value) {
            alert('등록일 From 은 To 보다 늦을 수 없습니다.');
            return false;
        }
        return true;
    }

    function hasUsableImage(imageUrl) {
        return !!imageUrl && imageUrl !== '-' && imageUrl.trim() !== '';
    }

    function formatCategoryPath(value) {
        const text = escapeHtml(value || '-');
        return text.replaceAll(' | ', '<br>');
    }

    function applyCsrfHeader(headers) {
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

        if (token && header) {
            headers[header] = token;
        }
    }

    async function fetchJson(url) {
        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(await extractErrorMessage(response));
        }

        return await response.json();
    }

    async function extractErrorMessage(response) {
        try {
            const contentType = response.headers.get('content-type') || '';

            if (contentType.includes('application/json')) {
                const body = await response.json();

                if (body.message) {
                    return body.message;
                }

                if (body.error) {
                    return body.error;
                }

                return '요청 처리 중 오류가 발생했습니다.';
            }

            const text = await response.text();
            return text || '요청 처리 중 오류가 발생했습니다.';
        } catch (e) {
            return '요청 처리 중 오류가 발생했습니다.';
        }
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll('\'', '&#39;');
    }

    function escapeAttribute(value) {
        return escapeHtml(value);
    }
})();