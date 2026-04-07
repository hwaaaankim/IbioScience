(function () {
    'use strict';

    const state = {
        page: 1,
        pageSize: 10,
        fromDate: '',
        toDate: '',
        totalPages: 0
    };

    const elements = {
        pageSize: document.getElementById('seller-settlement-manager-page-size'),
        fromDate: document.getElementById('seller-settlement-manager-from-date'),
        toDate: document.getElementById('seller-settlement-manager-to-date'),
        searchBtn: document.getElementById('seller-settlement-manager-search-btn'),
        resetBtn: document.getElementById('seller-settlement-manager-reset-btn'),
        tbody: document.getElementById('seller-settlement-manager-tbody'),
        pagination: document.getElementById('seller-settlement-manager-pagination'),
        pageInfo: document.getElementById('seller-settlement-manager-page-info'),
        totalCountBadge: document.getElementById('seller-settlement-manager-total-count-badge'),
        resultPeriodText: document.getElementById('seller-settlement-manager-result-period-text'),
        loading: document.getElementById('seller-settlement-manager-loading'),
        orderModal: document.getElementById('seller-settlement-manager-order-modal'),
        orderTbody: document.getElementById('seller-settlement-manager-order-tbody'),
        orderSummaryLeft: document.getElementById('seller-settlement-manager-order-summary-left'),
        orderSummaryRight: document.getElementById('seller-settlement-manager-order-summary-right')
    };

    let orderModalInstance = null;

    document.addEventListener('DOMContentLoaded', function () {
        if (!elements.pageSize) {
            return;
        }

        orderModalInstance = new bootstrap.Modal(elements.orderModal);

        bindEvents();
        fetchSettlementList();
    });

    function bindEvents() {
        elements.searchBtn.addEventListener('click', function () {
            state.page = 1;
            applyFiltersFromForm();
            if (!validateDateRange()) {
                return;
            }
            fetchSettlementList();
        });

        elements.resetBtn.addEventListener('click', function () {
            elements.pageSize.value = '10';
            elements.fromDate.value = '';
            elements.toDate.value = '';
            state.page = 1;
            state.pageSize = 10;
            state.fromDate = '';
            state.toDate = '';
            fetchSettlementList();
        });

        elements.pageSize.addEventListener('change', function () {
            state.page = 1;
            applyFiltersFromForm();
            fetchSettlementList();
        });

        elements.pagination.addEventListener('click', function (event) {
            const button = event.target.closest('button[data-page]');
            if (!button) {
                return;
            }

            const targetPage = Number(button.dataset.page);
            if (!targetPage || targetPage < 1 || targetPage === state.page) {
                return;
            }

            state.page = targetPage;
            fetchSettlementList();
        });

        elements.tbody.addEventListener('click', function (event) {
            const detailButton = event.target.closest('.seller-settlement-manager-detail-btn');
            if (!detailButton) {
                return;
            }

            const settlementId = detailButton.dataset.settlementId;
            if (!settlementId) {
                alert('정산 ID를 확인할 수 없습니다.');
                return;
            }

            fetchSettlementOrders(settlementId);
        });
    }

    function applyFiltersFromForm() {
        state.pageSize = Number(elements.pageSize.value || 10);
        state.fromDate = elements.fromDate.value || '';
        state.toDate = elements.toDate.value || '';
    }

    function validateDateRange() {
        const fromDate = elements.fromDate.value;
        const toDate = elements.toDate.value;

        if (fromDate && toDate && fromDate > toDate) {
            alert('기간 시작일은 기간 종료일보다 클 수 없습니다.');
            return false;
        }
        return true;
    }

    async function fetchSettlementList() {
        applyFiltersFromForm();

        try {
            setLoading(true);

            const headers = {
                'Content-Type': 'application/json'
            };

            applyCsrfHeader(headers);

            const response = await fetch('/seller/api/settlement/manager/list', {
                method: 'POST',
                headers: headers,
                body: JSON.stringify({
                    page: state.page,
                    pageSize: state.pageSize,
                    fromDate: state.fromDate || null,
                    toDate: state.toDate || null
                })
            });

            const payload = await parseJson(response);
            const data = unwrapPayload(payload);

            renderSettlementTable(data);
            renderPagination(data.currentPage, data.totalPages);
            renderPageInfo(data.currentPage, data.totalPages, data.totalElements);
            renderResultPeriodText();
        } catch (error) {
            console.error(error);
            alert(error.message || '정산 내역 조회 중 오류가 발생했습니다.');
            renderEmptyTable('정산 내역을 불러오지 못했습니다.');
            renderPagination(1, 0);
            renderPageInfo(1, 0, 0);
        } finally {
            setLoading(false);
        }
    }

    async function fetchSettlementOrders(settlementId) {
        try {
            setLoading(true);
            elements.orderTbody.innerHTML = `
                <tr>
                    <td colspan="10" class="py-5 text-muted">불러오는 중입니다.</td>
                </tr>
            `;
            elements.orderSummaryLeft.innerHTML = '';
            elements.orderSummaryRight.innerHTML = '';

            const response = await fetch(`/seller/api/settlement/manager/${settlementId}/orders`, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                }
            });

            const payload = await parseJson(response);
            const data = unwrapPayload(payload);

            renderOrderSummary(data);
            renderOrderTable(data.orders || []);
            orderModalInstance.show();
        } catch (error) {
            console.error(error);
            alert(error.message || '상세주문 조회 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    }

    function renderSettlementTable(data) {
        const rows = data.content || [];
        elements.totalCountBadge.textContent = `총 ${formatNumber(data.totalElements || 0)}건`;

        if (!rows.length) {
            renderEmptyTable('조회된 정산 내역이 없습니다.');
            return;
        }

        elements.tbody.innerHTML = rows.map(function (row) {
            return `
                <tr>
                    <td class="fw-semibold">${escapeHtml(row.id)}</td>
                    <td>${formatDate(row.periodStartDate)} ~ ${formatDate(row.periodEndDate)}</td>
                    <td>${formatSettlementBasis(row.settlementBasis)}</td>
                    <td>${formatSettlementCycle(row.settlementCycle)}</td>
                    <td>${formatNumber(row.orderCount)}</td>
                    <td>${formatNumber(row.itemCount)}</td>
                    <td class="text-end fw-semibold">${formatMoney(row.grossAmount)}</td>
                    <td>${formatCommissionRate(row.commissionRate)}</td>
                    <td class="text-end">${formatMoney(row.commissionAmount)}</td>
                    <td class="text-end text-primary fw-bold">${formatMoney(row.settlementAmount)}</td>
                    <td>${renderPayStatusBadge(row.payStatus)}</td>
                    <td>${formatDateTime(row.executedAt)}</td>
                    <td>${formatDateTime(row.paidAt)}</td>
                    <td>
                        <button type="button"
                                class="btn btn-sm btn-outline-primary seller-settlement-manager-detail-btn"
                                data-settlement-id="${escapeHtml(row.id)}">
                            상세주문보기
                        </button>
                    </td>
                </tr>
            `;
        }).join('');
    }

    function renderEmptyTable(message) {
        elements.tbody.innerHTML = `
            <tr>
                <td colspan="14" class="py-5 text-muted">${escapeHtml(message)}</td>
            </tr>
        `;
    }

    function renderOrderSummary(data) {
        elements.orderSummaryLeft.innerHTML = `
            <div class="mb-1"><span class="fw-semibold text-dark">정산ID</span> : ${escapeHtml(data.settlementId)}</div>
            <div class="mb-1"><span class="fw-semibold text-dark">정산기간</span> : ${formatDate(data.periodStartDate)} ~ ${formatDate(data.periodEndDate)}</div>
            <div class="mb-1"><span class="fw-semibold text-dark">정산기준</span> : ${formatSettlementBasis(data.settlementBasis)}</div>
            <div class="mb-1"><span class="fw-semibold text-dark">정산주기</span> : ${formatSettlementCycle(data.settlementCycle)}</div>
            <div class="mb-1"><span class="fw-semibold text-dark">상태</span> : ${formatSettlementPayStatusText(data.payStatus)}</div>
            <div><span class="fw-semibold text-dark">정산생성일시</span> : ${formatDateTime(data.executedAt)}</div>
        `;

        elements.orderSummaryRight.innerHTML = `
            <div class="mb-1"><span class="fw-semibold text-dark">주문건수</span> : ${formatNumber(data.orderCount)}건</div>
            <div class="mb-1"><span class="fw-semibold text-dark">상품수량</span> : ${formatNumber(data.itemCount)}개</div>
            <div class="mb-1"><span class="fw-semibold text-dark">총주문금액</span> : ${formatMoney(data.grossAmount)}</div>
            <div class="mb-1"><span class="fw-semibold text-dark">수수료율</span> : ${formatCommissionRate(data.commissionRate)}</div>
            <div class="mb-1"><span class="fw-semibold text-dark">수수료</span> : ${formatMoney(data.commissionAmount)}</div>
            <div><span class="fw-semibold text-dark">실정산액</span> : ${formatMoney(data.settlementAmount)}</div>
        `;
    }

    function renderOrderTable(orders) {
        if (!orders.length) {
            elements.orderTbody.innerHTML = `
                <tr>
                    <td colspan="10" class="py-5 text-muted">해당 정산에 포함된 주문이 없습니다.</td>
                </tr>
            `;
            return;
        }

        elements.orderTbody.innerHTML = orders.map(function (order) {
            return `
                <tr>
                    <td>${escapeHtml(order.orderIdSnapshot)}</td>
                    <td class="fw-semibold">${escapeHtml(order.orderNoSnapshot)}</td>
                    <td>${escapeHtml(order.ordererNameSnapshot || '-')}</td>
                    <td>${formatDateTime(order.basisDateSnapshot)}</td>
                    <td>${renderOrderInclusionStatusBadge(order.inclusionStatus)}</td>
                    <td>${formatNumber(order.dealerItemCount)}</td>
                    <td class="text-end">${formatMoney(order.dealerItemAmount)}</td>
                    <td class="text-end">${formatMoney(order.commissionAmount)}</td>
                    <td class="text-end fw-semibold text-primary">${formatMoney(order.settlementAmount)}</td>
                    <td class="seller-settlement-manager-order-memo-cell">${formatMemoHtml(order.memo)}</td>
                </tr>
            `;
        }).join('');
    }

    function renderPagination(currentPage, totalPages) {
        state.totalPages = totalPages || 0;
        elements.pagination.innerHTML = '';

        if (!totalPages || totalPages < 1) {
            return;
        }

        const blockSize = 5;
        const currentBlock = Math.ceil(currentPage / blockSize);
        const startPage = (currentBlock - 1) * blockSize + 1;
        const endPage = Math.min(startPage + blockSize - 1, totalPages);

        const items = [];

        items.push(createPaginationItem('FIRST', 1, currentPage === 1));
        items.push(createPaginationItem('PREV', Math.max(1, currentPage - 1), currentPage === 1));

        for (let page = startPage; page <= endPage; page++) {
            items.push(createPaginationItem(String(page), page, false, page === currentPage));
        }

        items.push(createPaginationItem('NEXT', Math.min(totalPages, currentPage + 1), currentPage === totalPages));
        items.push(createPaginationItem('LAST', totalPages, currentPage === totalPages));

        elements.pagination.innerHTML = items.join('');
    }

    function createPaginationItem(label, page, disabled, active) {
        return `
            <li class="page-item ${disabled ? 'disabled' : ''} ${active ? 'active' : ''}">
                <button type="button" class="page-link px-3" data-page="${page}">
                    ${label}
                </button>
            </li>
        `;
    }

    function renderPageInfo(currentPage, totalPages, totalElements) {
        elements.pageInfo.textContent = `${formatNumber(totalElements || 0)}건 / ${currentPage || 0} / ${totalPages || 0} 페이지`;
    }

    function renderResultPeriodText() {
        const fromText = state.fromDate ? state.fromDate : '전체';
        const toText = state.toDate ? state.toDate : '전체';
        elements.resultPeriodText.textContent = `조회 기간 : ${fromText} ~ ${toText}`;
    }

    function renderPayStatusBadge(payStatus) {
        const text = formatSettlementPayStatusText(payStatus);
        const cls = payStatus === 'PAID'
            ? 'bg-success-subtle text-success-emphasis'
            : 'bg-warning-subtle text-warning-emphasis';

        return `<span class="badge ${cls} rounded-pill px-3 py-2">${escapeHtml(text)}</span>`;
    }

    function renderOrderInclusionStatusBadge(inclusionStatus) {
        const text = formatOrderInclusionStatusText(inclusionStatus);
        const cls = inclusionStatus === 'NORMAL'
            ? 'bg-success-subtle text-success-emphasis'
            : 'bg-secondary-subtle text-secondary-emphasis';

        return `<span class="badge ${cls} rounded-pill px-3 py-2">${escapeHtml(text)}</span>`;
    }

    function formatSettlementBasis(value) {
        const map = {
            PAYMENT_COMPLETED: '결제완료',
            DELIVERY_COMPLETED: '배송완료',
            PURCHASE_CONFIRMED: '구매확정완료'
        };
        return map[value] || value || '-';
    }

    function formatSettlementCycle(value) {
        const map = {
            DAY_1: '매월 1일',
            DAY_5: '매월 5일',
            DAY_10: '매월 10일',
            DAY_15: '매월 15일',
            DAY_20: '매월 20일',
            DAY_25: '매월 25일',
            MONTH_END: '말일'
        };
        return map[value] || value || '-';
    }

    function formatSettlementPayStatusText(value) {
        const map = {
            UNPAID: '미지급',
            PAID: '지급완료'
        };
        return map[value] || value || '-';
    }

    function formatOrderInclusionStatusText(value) {
        const map = {
            NORMAL: '정상'
        };
        return map[value] || value || '-';
    }

    function formatDate(value) {
        if (!value) {
            return '-';
        }
        return value;
    }

    function formatDateTime(value) {
        if (!value) {
            return '-';
        }

        const normalized = String(value).replace('T', ' ');
        if (normalized.length >= 19) {
            return normalized.substring(0, 19);
        }
        return normalized;
    }

    function formatNumber(value) {
        const number = Number(value || 0);
        return number.toLocaleString('ko-KR');
    }

    function formatMoney(value) {
        return `${formatNumber(value)}원`;
    }

    function formatCommissionRate(value) {
        if (value === null || value === undefined || value === '') {
            return '-';
        }
        return `${value}%`;
    }

    function formatMemoHtml(value) {
        if (value === null || value === undefined || value === '') {
            return '-';
        }

        return escapeHtml(String(value)).replaceAll('\n', '<br>');
    }

    function setLoading(show) {
        if (show) {
            elements.loading.classList.remove('d-none');
        } else {
            elements.loading.classList.add('d-none');
        }
    }

    function applyCsrfHeader(headers) {
        const tokenMeta = document.querySelector('meta[name="_csrf"]');
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');

        if (!tokenMeta || !headerMeta) {
            return;
        }

        headers[headerMeta.content] = tokenMeta.content;
    }

    async function parseJson(response) {
        const text = await response.text();
        if (!text) {
            return {};
        }

        try {
            return JSON.parse(text);
        } catch (error) {
            if (!response.ok) {
                throw new Error('서버 응답을 해석할 수 없습니다.');
            }
            return {};
        }
    }

    function unwrapPayload(payload) {
        if (!payload) {
            return {};
        }

        if (payload.success === false || payload.result === false) {
            throw new Error(payload.message || '요청 처리 중 오류가 발생했습니다.');
        }

        if (payload.data !== undefined) {
            return payload.data;
        }

        return payload;
    }

    function escapeHtml(value) {
        if (value === null || value === undefined) {
            return '';
        }

        return String(value)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }
})();