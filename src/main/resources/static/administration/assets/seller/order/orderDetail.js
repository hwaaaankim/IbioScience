(function () {
    'use strict';

    const orderId = window.SELLER_ORDER_DETAIL_ORDER_ID;
    if (!orderId) {
        return;
    }

    const API_BASE_URL = '/seller/api/orders/';

    const statusSelect = document.getElementById('seller-order-detail-status-select');
    const saveBtn = document.getElementById('seller-order-detail-save-status-btn');

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
        if (!date) return '-';

        return date.getFullYear() + '-' +
            pad2(date.getMonth() + 1) + '-' +
            pad2(date.getDate()) + ' ' +
            pad2(date.getHours()) + ':' +
            pad2(date.getMinutes());
    }

    function formatNumber(value) {
        return Number(value || 0).toLocaleString('ko-KR');
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

    function joinPhone(...parts) {
        const values = parts.filter(part => part && String(part).trim() !== '-');
        return values.length ? values.join('-') : '-';
    }

    function setValue(id, value) {
        const element = document.getElementById(id);
        if (!element) return;
        element.value = value == null || value === '' ? '-' : value;
    }

    function setText(id, value) {
        const element = document.getElementById(id);
        if (!element) return;
        element.textContent = value == null || value === '' ? '-' : value;
    }

    function updateSaveButtonState() {
        saveBtn.disabled = statusSelect.value === statusSelect.dataset.originalStatus;
    }

    function distinctItems(items) {
        if (!Array.isArray(items) || items.length === 0) {
            return [];
        }

        const itemMap = new Map();
        const fallbackItems = [];

        items.forEach((item, index) => {
            if (!item) {
                return;
            }

            if (item.orderItemId == null) {
                fallbackItems.push({
                    __fallbackIndex: index,
                    ...item
                });
                return;
            }

            if (!itemMap.has(item.orderItemId)) {
                itemMap.set(item.orderItemId, item);
            }
        });

        return Array.from(itemMap.values()).concat(fallbackItems);
    }

    function renderItems(items) {
        const tbody = document.getElementById('seller-order-detail-items-body');

        if (!items || items.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-5">해당 딜러 상품이 없습니다.</td></tr>';
            return;
        }

        tbody.innerHTML = items.map(item => {
            const detailButton = item.productDetailUrl
                ? `<a href="${escapeHtml(item.productDetailUrl)}" class="btn btn-sm btn-light seller-order-detail-link-btn">상세이동</a>`
                : `<button type="button" class="btn btn-sm btn-light seller-order-detail-link-btn" disabled>상세이동</button>`;

            return `
                <tr>
                    <td>${item.dealerProductId != null ? escapeHtml(item.dealerProductId) : '-'}</td>
                    <td class="seller-order-detail-product-name">${escapeHtml(item.productName || '-')}</td>
                    <td>${escapeHtml(item.optionGroupName || '-')}</td>
                    <td>${escapeHtml(item.optionName || '-')}</td>
                    <td>${formatNumber(item.unitPrice)}원</td>
                    <td>${formatNumber(item.quantity)}</td>
                    <td>${formatNumber(item.linePrice)}원</td>
                    <td>${detailButton}</td>
                </tr>
            `;
        }).join('');
    }

    function renderDetail(data) {
        const resolvedItems = distinctItems(data.items || []);

        setText('detail-order-no', data.orderNo || '-');
        setText('detail-ordered-at', formatDateTime(data.orderedAt));
        setText('detail-visible-sum-price', formatNumber(data.visibleItemSumPrice) + '원');

        setValue('detail-orderer-name', data.ordererName);
        setValue('detail-orderer-username', data.ordererUsername);
        setValue('detail-contact', data.contact);
        setValue('detail-email', data.email);
        setValue('detail-dealer-type-label', data.dealerTypeLabel);
        setValue('detail-company-name', data.companyName);
        setValue('detail-shop-name', data.shopName);
        setValue('detail-paid-at', formatDateTime(data.paidAt));

        setValue('detail-receiver-name', data.receiverName);
        setValue('detail-hp', joinPhone(data.hp1, data.hp2, data.hp3));
        setValue('detail-tel', joinPhone(data.tel1, data.tel2, data.tel3));
        setValue('detail-postcode', data.postcode);
        setValue('detail-road-address', data.roadAddress);
        setValue('detail-detail-address', data.detailAddress);
        setValue('detail-payment-method', data.paymentMethodLabel);
        setValue('detail-shipping-method', data.shippingMethodLabel);
        setValue('detail-shipping-pay-type', data.shippingPayTypeLabel);
        setValue('detail-shipping-memo', data.shippingMemo);

        setText('detail-visible-item-count-badge', resolvedItems.length + '건');

        statusSelect.value = data.status || 'ORDER_COMPLETED';
        statusSelect.dataset.originalStatus = data.status || 'ORDER_COMPLETED';

        renderItems(resolvedItems);
        updateSaveButtonState();
    }

    async function loadDetail() {
        try {
            const response = await fetch(API_BASE_URL + orderId, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('주문상세 조회에 실패했습니다.');
            }

            const data = await response.json();
            renderDetail(data);
        } catch (error) {
            console.error(error);
            alert(error.message || '주문상세 조회 중 오류가 발생했습니다.');
        }
    }

    async function saveStatus() {
        if (statusSelect.value === statusSelect.dataset.originalStatus) {
            return;
        }

        saveBtn.disabled = true;

        try {
            const csrf = getCsrfHeader();
            const headers = {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            };

            if (csrf) {
                headers[csrf.headerName] = csrf.token;
            }

            const response = await fetch(API_BASE_URL + orderId + '/status', {
                method: 'PATCH',
                headers,
                body: JSON.stringify({
                    status: statusSelect.value
                })
            });

            if (!response.ok) {
                throw new Error('상태변경 저장에 실패했습니다.');
            }

            await loadDetail();
            alert('주문상태가 변경되었습니다.');
        } catch (error) {
            console.error(error);
            alert(error.message || '상태변경 중 오류가 발생했습니다.');
            updateSaveButtonState();
        }
    }

    statusSelect.addEventListener('change', updateSaveButtonState);
    saveBtn.addEventListener('click', saveStatus);

    loadDetail();
})();