(function () {
    'use strict';

    const API_URL = '/seller/api/orders';
    const DETAIL_BASE_URL = '/seller/orderDetail/';

    const ORDER_STATUS_LABELS = {
        ORDER_COMPLETED: '주문완료',
        PRODUCT_PREPARING: '상품준비중',
        DELIVERING: '배송중',
        CANCEL_FINISHED: '취소완료',
        PAYMENT_ERROR: '결제에러'
    };

    const SORTABLE_FIELDS = ['ordererName', 'contact', 'email', 'orderedAt', 'status'];
    const MANAGEABLE_STATUSES = ['ORDER_COMPLETED', 'PRODUCT_PREPARING', 'DELIVERING', 'CANCEL_FINISHED'];

    const form = document.getElementById('seller-order-search-form');
    const pageInput = document.getElementById('seller-order-page');
    const sizeSelect = document.getElementById('seller-order-page-size');
    const sortFieldInput = document.getElementById('seller-order-sort-field');
    const sortDirInput = document.getElementById('seller-order-sort-dir');
    const fromDateInput = document.getElementById('seller-order-from-date');
    const toDateInput = document.getElementById('seller-order-to-date');
    const keywordTypeInput = document.getElementById('seller-order-keyword-type');
    const keywordInput = document.getElementById('seller-order-keyword');
    const tableBody = document.getElementById('seller-order-table-body');
    const pagination = document.getElementById('seller-order-pagination');
    const totalText = document.getElementById('seller-order-total-text');
    const saveStatusBtn = document.getElementById('seller-order-save-status-btn');
    const resetBtn = document.getElementById('seller-order-reset-btn');

    function getCsrfHeader() {
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');
        const tokenMeta = document.querySelector('meta[name="_csrf"]');

        if (!headerMeta || !tokenMeta) {
            return null;
        }

        return {
            headerName: headerMeta.getAttribute('content'),
            token: tokenMeta.getAttribute('content')
        };
    }

    function pad2(number) {
        return String(number).padStart(2, '0');
    }

    function formatDateInput(date) {
        return date.getFullYear() + '-' + pad2(date.getMonth() + 1) + '-' + pad2(date.getDate());
    }

    function toDate(value) {
        if (value == null || value === '') {
            return null;
        }

        if (value instanceof Date) {
            return Number.isNaN(value.getTime()) ? null : value;
        }

        if (Array.isArray(value)) {
            if (value.length < 3) {
                return null;
            }

            const year = Number(value[0]);
            const month = Number(value[1]);
            const day = Number(value[2]);
            const hour = Number(value[3] || 0);
            const minute = Number(value[4] || 0);
            const second = Number(value[5] || 0);

            const date = new Date(year, month - 1, day, hour, minute, second);
            return Number.isNaN(date.getTime()) ? null : date;
        }

        if (typeof value === 'object') {
            const year = Number(value.year);
            const month = Number(value.monthValue != null ? value.monthValue : value.month);
            const day = Number(value.dayOfMonth != null ? value.dayOfMonth : value.day);
            const hour = Number(value.hour || 0);
            const minute = Number(value.minute || 0);
            const second = Number(value.second || 0);

            if (!Number.isNaN(year) && !Number.isNaN(month) && !Number.isNaN(day)) {
                const date = new Date(year, month - 1, day, hour, minute, second);
                return Number.isNaN(date.getTime()) ? null : date;
            }

            return null;
        }

        if (typeof value === 'number') {
            const date = new Date(value);
            return Number.isNaN(date.getTime()) ? null : date;
        }

        if (typeof value === 'string') {
            const trimmed = value.trim();
            if (!trimmed) {
                return null;
            }

            const normalized = trimmed.includes('T') ? trimmed : trimmed.replace(' ', 'T');
            let date = new Date(normalized);

            if (!Number.isNaN(date.getTime())) {
                return date;
            }

            const match = trimmed.match(
                /^(\d{4})-(\d{2})-(\d{2})(?:[ T](\d{2}):(\d{2})(?::(\d{2}))?)?$/
            );

            if (!match) {
                return null;
            }

            date = new Date(
                Number(match[1]),
                Number(match[2]) - 1,
                Number(match[3]),
                Number(match[4] || 0),
                Number(match[5] || 0),
                Number(match[6] || 0)
            );

            return Number.isNaN(date.getTime()) ? null : date;
        }

        return null;
    }

    function formatDateTime(value) {
        const date = toDate(value);
        if (!date) {
            return '-';
        }

        return date.getFullYear() + '-' +
            pad2(date.getMonth() + 1) + '-' +
            pad2(date.getDate()) + ' ' +
            pad2(date.getHours()) + ':' +
            pad2(date.getMinutes());
    }

    function formatNumber(value) {
        if (value == null) return '0';
        return Number(value).toLocaleString('ko-KR');
    }

    function escapeHtml(value) {
        if (value == null) return '';
        return String(value)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }

    function getCheckedValues(name) {
        return Array.from(document.querySelectorAll('input[name="' + name + '"]:checked'))
            .map(input => input.value);
    }

    function setCheckedValues(name, values) {
        const valueSet = new Set(values || []);
        document.querySelectorAll('input[name="' + name + '"]').forEach(input => {
            input.checked = valueSet.has(input.value);
        });
    }

    function collectParams(pageOverride) {
        const params = new URLSearchParams();

        const page = typeof pageOverride === 'number' ? pageOverride : Number(pageInput.value || 0);
        const size = sizeSelect.value || '10';

        params.set('page', String(page));
        params.set('size', size);
        params.set('sortField', sortFieldInput.value || 'orderedAt');
        params.set('sortDir', sortDirInput.value || 'desc');

        if (fromDateInput.value) params.set('fromDate', fromDateInput.value);
        if (toDateInput.value) params.set('toDate', toDateInput.value);
        if (keywordTypeInput.value) params.set('keywordType', keywordTypeInput.value);
        if (keywordInput.value.trim()) params.set('keyword', keywordInput.value.trim());

        getCheckedValues('dealerTypes').forEach(value => params.append('dealerTypes', value));
        getCheckedValues('statuses').forEach(value => params.append('statuses', value));
        getCheckedValues('paymentMethods').forEach(value => params.append('paymentMethods', value));
        getCheckedValues('shippingMethods').forEach(value => params.append('shippingMethods', value));
        getCheckedValues('shippingPayTypes').forEach(value => params.append('shippingPayTypes', value));

        return params;
    }

    function syncFormFromUrl() {
        const url = new URL(window.location.href);
        const params = url.searchParams;

        sizeSelect.value = params.get('size') || '10';
        pageInput.value = params.get('page') || '0';
        fromDateInput.value = params.get('fromDate') || '';
        toDateInput.value = params.get('toDate') || '';
        keywordTypeInput.value = params.get('keywordType') || '';
        keywordInput.value = params.get('keyword') || '';
        sortFieldInput.value = params.get('sortField') || 'orderedAt';
        sortDirInput.value = params.get('sortDir') || 'desc';

        setCheckedValues('dealerTypes', params.getAll('dealerTypes'));
        setCheckedValues('statuses', params.getAll('statuses'));
        setCheckedValues('paymentMethods', params.getAll('paymentMethods'));
        setCheckedValues('shippingMethods', params.getAll('shippingMethods'));
        setCheckedValues('shippingPayTypes', params.getAll('shippingPayTypes'));

        updateSortButtonState();
    }

    function replaceUrl(params) {
        const newUrl = window.location.pathname + '?' + params.toString();
        window.history.replaceState({}, '', newUrl);
    }

    function updateSortButtonState() {
        const currentField = sortFieldInput.value || 'orderedAt';
        const currentDir = sortDirInput.value || 'desc';

        document.querySelectorAll('.seller-order-sort-btn').forEach(button => {
            const field = button.dataset.sortField;
            const icon = button.querySelector('.sort-icon');

            button.classList.remove('active');
            if (icon) icon.textContent = '↕';

            if (field === currentField) {
                button.classList.add('active');
                if (icon) {
                    icon.textContent = currentDir === 'asc' ? '↑' : '↓';
                }
            }
        });
    }

    function setLoadingRow(message) {
        tableBody.innerHTML = '<tr><td colspan="10" class="seller-order-empty">' + escapeHtml(message) + '</td></tr>';
    }

    function renderTable(response) {
        if (!response || !response.content || response.content.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="10" class="seller-order-empty">조회된 주문이 없습니다.</td></tr>';
            saveStatusBtn.disabled = true;
            return;
        }

        const rowsHtml = response.content.map(item => {
            const selectedStatus = item.status || '';
            const statusOptions = MANAGEABLE_STATUSES.map(status => {
                const selected = selectedStatus === status ? ' selected' : '';
                return '<option value="' + status + '"' + selected + '>' + escapeHtml(ORDER_STATUS_LABELS[status] || status) + '</option>';
            }).join('');

            return `
                <tr class="seller-order-clickable-row" data-order-id="${item.id}">
                    <td>${item.id ?? '-'}</td>
                    <td>${escapeHtml(item.orderNo || '-')}</td>
                    <td>${escapeHtml(item.ordererName || '-')}</td>
                    <td>${escapeHtml(item.contact || '-')}</td>
                    <td>${escapeHtml(item.email || '-')}</td>
                    <td>${escapeHtml(item.companyName || '-')}</td>
                    <td>${escapeHtml(item.shopName || '-')}</td>
                    <td>${escapeHtml(formatDateTime(item.orderedAt))}</td>
                    <td>${escapeHtml(item.statusLabel || '-')}</td>
                    <td>
                        <select class="form-select form-select-sm seller-order-status-select seller-order-row-status"
                                data-order-id="${item.id}"
                                data-original-status="${escapeHtml(selectedStatus)}">
                            ${statusOptions}
                        </select>
                    </td>
                </tr>
            `;
        }).join('');

        tableBody.innerHTML = rowsHtml;
        bindRowEvents();
        updateSaveButtonState();
    }

    function createPageItem(label, page, disabled, active) {
        const liClass = ['page-item'];
        if (disabled) liClass.push('disabled');
        if (active) liClass.push('active');

        const href = disabled ? 'javascript:void(0);' : '#';

        return `
            <li class="${liClass.join(' ')}">
                <a class="page-link seller-order-page-link" href="${href}" data-page="${page}">${label}</a>
            </li>
        `;
    }

    function renderPagination(response) {
        pagination.innerHTML = '';

        if (!response || response.totalPages <= 0) {
            return;
        }

        const currentPage = response.page;
        const totalPages = response.totalPages;

        const startPage = Math.floor(currentPage / 5) * 5;
        const endPage = Math.min(startPage + 5, totalPages);

        let html = '';
        html += createPageItem('FIRST', 0, response.first, false);
        html += createPageItem('PREV', Math.max(0, currentPage - 1), response.first, false);

        for (let i = startPage; i < endPage; i += 1) {
            html += createPageItem(String(i + 1), i, false, i === currentPage);
        }

        html += createPageItem('NEXT', Math.min(totalPages - 1, currentPage + 1), response.last, false);
        html += createPageItem('LAST', totalPages - 1, response.last, false);

        pagination.innerHTML = html;

        pagination.querySelectorAll('.seller-order-page-link').forEach(link => {
            link.addEventListener('click', function (event) {
                event.preventDefault();

                const parent = link.closest('.page-item');
                if (!parent || parent.classList.contains('disabled') || parent.classList.contains('active')) {
                    return;
                }

                const page = Number(link.dataset.page);
                pageInput.value = String(page);
                fetchOrders(page);
            });
        });
    }

    function updateSaveButtonState() {
        const changed = Array.from(document.querySelectorAll('.seller-order-row-status'))
            .some(select => select.value !== select.dataset.originalStatus);

        saveStatusBtn.disabled = !changed;
    }

    function bindRowEvents() {
        document.querySelectorAll('.seller-order-clickable-row').forEach(row => {
            row.addEventListener('click', function (event) {
                const target = event.target;
                if (target.closest('.seller-order-row-status')) {
                    return;
                }

                const orderId = row.dataset.orderId;
                if (!orderId) return;

                window.location.href = DETAIL_BASE_URL + orderId;
            });
        });

        document.querySelectorAll('.seller-order-row-status').forEach(select => {
            select.addEventListener('click', function (event) {
                event.stopPropagation();
            });

            select.addEventListener('change', function (event) {
                event.stopPropagation();
                updateSaveButtonState();
            });
        });
    }

    async function fetchOrders(pageOverride) {
        const params = collectParams(pageOverride);
        replaceUrl(params);
        updateSortButtonState();
        setLoadingRow('조회 중입니다.');

        try {
            const response = await fetch(API_URL + '?' + params.toString(), {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('주문 목록 조회에 실패했습니다.');
            }

            const data = await response.json();
            totalText.textContent = '총 ' + formatNumber(data.totalElements || 0) + '건';
            pageInput.value = String(data.page || 0);
            renderTable(data);
            renderPagination(data);
        } catch (error) {
            console.error(error);
            totalText.textContent = '총 0건';
            pagination.innerHTML = '';
            setLoadingRow(error.message || '조회 중 오류가 발생했습니다.');
            saveStatusBtn.disabled = true;
        }
    }

    async function saveChangedStatuses() {
        const changedItems = Array.from(document.querySelectorAll('.seller-order-row-status'))
            .filter(select => select.value !== select.dataset.originalStatus)
            .map(select => ({
                orderId: Number(select.dataset.orderId),
                status: select.value
            }));

        if (changedItems.length === 0) {
            return;
        }

        saveStatusBtn.disabled = true;

        try {
            const csrf = getCsrfHeader();
            const headers = {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            };

            if (csrf) {
                headers[csrf.headerName] = csrf.token;
            }

            const response = await fetch(API_URL + '/status', {
                method: 'PATCH',
                headers,
                body: JSON.stringify({ items: changedItems })
            });

            if (!response.ok) {
                throw new Error('주문상태 저장에 실패했습니다.');
            }

            await fetchOrders(Number(pageInput.value || 0));
            alert('주문상태가 저장되었습니다.');
        } catch (error) {
            console.error(error);
            alert(error.message || '상태 저장 중 오류가 발생했습니다.');
            updateSaveButtonState();
        }
    }

    function resetForm() {
        form.reset();
        pageInput.value = '0';
        sortFieldInput.value = 'orderedAt';
        sortDirInput.value = 'desc';
        updateSortButtonState();
        fetchOrders(0);
    }

    function applyRange(rangeType) {
        const today = new Date();
        let fromDate = null;
        let toDate = formatDateInput(today);

        if (rangeType === 'today') {
            fromDate = formatDateInput(today);
        } else if (rangeType === '7days') {
            const date = new Date(today);
            date.setDate(today.getDate() - 6);
            fromDate = formatDateInput(date);
        } else if (rangeType === '1month') {
            const date = new Date(today);
            date.setMonth(today.getMonth() - 1);
            fromDate = formatDateInput(date);
        } else if (rangeType === 'all') {
            fromDate = '';
            toDate = '';
        }

        fromDateInput.value = fromDate || '';
        toDateInput.value = toDate || '';
    }

    function bindEvents() {
        form.addEventListener('submit', function (event) {
            event.preventDefault();
            pageInput.value = '0';
            fetchOrders(0);
        });

        sizeSelect.addEventListener('change', function () {
            pageInput.value = '0';
            fetchOrders(0);
        });

        resetBtn.addEventListener('click', function () {
            resetForm();
        });

        document.querySelectorAll('.seller-order-range-btn').forEach(button => {
            button.addEventListener('click', function () {
                applyRange(button.dataset.range);
            });
        });

        document.querySelectorAll('.seller-order-sort-btn').forEach(button => {
            button.addEventListener('click', function () {
                const field = button.dataset.sortField;
                if (!SORTABLE_FIELDS.includes(field)) return;

                if (sortFieldInput.value === field) {
                    sortDirInput.value = sortDirInput.value === 'asc' ? 'desc' : 'asc';
                } else {
                    sortFieldInput.value = field;
                    sortDirInput.value = 'asc';
                }

                pageInput.value = '0';
                updateSortButtonState();
                fetchOrders(0);
            });
        });

        saveStatusBtn.addEventListener('click', function () {
            saveChangedStatuses();
        });
    }

    syncFormFromUrl();
    bindEvents();
    fetchOrders(Number(pageInput.value || 0));
})();