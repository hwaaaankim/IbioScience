/* settlementManager.js */
(function () {
    'use strict';

    const tbody = document.getElementById('settlement-manager-tbody');
    const pagination = document.getElementById('settlement-manager-pagination');
    const btnSearch = document.getElementById('settlement-manager-btn-search');
    const btnReset = document.getElementById('settlement-manager-btn-reset');
    const btnSaveStatus = document.getElementById('settlement-manager-btn-save-status');

    const orderModalEl = document.getElementById('settlement-manager-order-modal');
    const orderModal = orderModalEl ? new bootstrap.Modal(orderModalEl) : null;
    const orderModalTbody = document.getElementById('settlement-manager-order-modal-tbody');
    const orderModalSummary = document.getElementById('settlement-manager-order-modal-summary');
    const btnSaveOrderAdjustments = document.getElementById('settlement-manager-order-modal-save-btn');

    const changedStatusMap = new Map();
    const changedOrderMap = new Map();
    const originalOrderStateMap = new Map();

    let currentPage = 0;
    let currentOrderSettlementId = null;

    function toJsonSafe(res) {
        return res.json().catch(() => null);
    }

    function formatDateTime(v) {
        if (!v) return '-';
        return String(v).replace('T', ' ');
    }

    function formatDate(v) {
        return v || '-';
    }

    function formatNumber(v) {
        return new Intl.NumberFormat('ko-KR').format(Number(v || 0));
    }

    function formatPercent(v) {
        if (v === null || v === undefined || v === '') return '-';

        const num = Number(v);
        if (Number.isNaN(num)) {
            return `${escapeHtml(String(v))}%`;
        }

        const fixed = Number.isInteger(num) ? num.toFixed(0) : num.toFixed(2);
        return `${fixed.replace(/\.00$/, '')}%`;
    }

    function getCheckedValues(prefix) {
        return Array.from(document.querySelectorAll(`input[id^="${prefix}"]:checked`)).map(x => x.value);
    }

    function buildQuery(page) {
        const params = new URLSearchParams();
        params.set('page', String(page));
        params.set('size', document.getElementById('settlement-manager-size').value);

        const payStatus = document.getElementById('settlement-manager-pay-status').value;
        const keyword = (document.getElementById('settlement-manager-keyword').value || '').trim();

        getCheckedValues('settlement-manager-basis-').forEach(v => params.append('bases', v));
        getCheckedValues('settlement-manager-cycle-').forEach(v => params.append('cycles', v));

        if (payStatus) params.set('payStatus', payStatus);
        if (keyword) params.set('keyword', keyword);

        return params.toString();
    }

    function refreshSaveButton() {
        btnSaveStatus.disabled = changedStatusMap.size === 0;
    }

    function refreshOrderSaveButton() {
        if (!btnSaveOrderAdjustments) return;
        btnSaveOrderAdjustments.disabled = !currentOrderSettlementId || changedOrderMap.size === 0;
    }

    function escapeHtml(str) {
        return String(str ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }

    function renderRows(data) {
        if (!data.content || data.content.length === 0) {
            tbody.innerHTML = `<tr><td colspan="14" class="settlement-manager-empty">조회 결과가 없습니다.</td></tr>`;
            return;
        }

        tbody.innerHTML = data.content.map(row => `
            <tr>
                <td>${row.id}</td>
                <td class="settlement-manager-user-box">
                    <div class="fw-semibold">${escapeHtml(row.memberUsername || '-')}</div>
                    <div class="small text-muted">${escapeHtml(row.memberName || '-')} / ${escapeHtml(row.shopName || '-')}</div>
                    <div class="small text-muted">${escapeHtml(row.companyName || '-')}</div>
                </td>
                <td>${escapeHtml(row.cycle || '-')}</td>
                <td>${escapeHtml(row.basis || '-')}</td>
                <td class="settlement-manager-period">${formatDate(row.periodStartDate)} ~ ${formatDate(row.periodEndDate)}</td>
                <td class="text-center">${formatNumber(row.orderCount)}</td>
                <td class="text-center">${formatNumber(row.itemCount)}</td>
                <td class="text-end">${formatNumber(row.grossAmount)}원</td>
                <td class="text-end text-danger">${formatNumber(row.commissionAmount)}원</td>
                <td class="text-end fw-semibold text-success">${formatNumber(row.settlementAmount)}원</td>
                <td>${formatDateTime(row.executedAt)}</td>
                <td>${formatDateTime(row.paidAt)}</td>
                <td>
                    <button type="button"
                            class="btn btn-outline-primary btn-sm settlement-manager-order-btn"
                            data-settlement-id="${row.id}">
                        주문보기
                    </button>
                </td>
                <td>
                    <select class="form-select form-select-sm settlement-manager-status-select"
                            data-settlement-id="${row.id}"
                            data-original="${row.payStatus}">
                        <option value="UNPAID" ${row.payStatus === 'UNPAID' ? 'selected' : ''}>미지급</option>
                        <option value="PAID" ${row.payStatus === 'PAID' ? 'selected' : ''}>정산완료</option>
                    </select>
                </td>
            </tr>
        `).join('');
    }

    function renderPagination(data) {
        pagination.innerHTML = '';
        if (!data.totalPages || data.totalPages <= 1) {
            return;
        }

        const makeItem = (text, page, disabled, active) => {
            const li = document.createElement('li');
            li.className = `page-item${disabled ? ' disabled' : ''}${active ? ' active' : ''}`;

            const a = document.createElement('a');
            a.className = 'page-link';
            a.href = 'javascript:void(0);';
            a.textContent = text;
            a.addEventListener('click', () => {
                if (disabled) return;
                load(page);
            });

            li.appendChild(a);
            return li;
        };

        pagination.appendChild(makeItem('<<', 0, data.page === 0, false));
        pagination.appendChild(makeItem('<', Math.max(data.page - 1, 0), data.page === 0, false));

        for (let p = 0; p < data.totalPages; p++) {
            pagination.appendChild(makeItem(String(p + 1), p, false, p === data.page));
        }

        pagination.appendChild(makeItem('>', Math.min(data.page + 1, data.totalPages - 1), data.page >= data.totalPages - 1, false));
        pagination.appendChild(makeItem('>>', data.totalPages - 1, data.page >= data.totalPages - 1, false));
    }

    async function load(page) {
        currentPage = page;

        const res = await fetch(`/admin/root/api/settlement-manager/search?${buildQuery(page)}`);
        const body = await toJsonSafe(res);

        if (!res.ok || !body || !body.success) {
            tbody.innerHTML = `<tr><td colspan="14" class="text-center text-danger py-5">${body?.message || '조회 실패'}</td></tr>`;
            pagination.innerHTML = '';
            return;
        }

        renderRows(body.data);
        renderPagination(body.data);
    }

    function getOrderStatusLabel(value) {
        switch (value) {
            case 'NORMAL': return '정상거래';
            case 'CANCELED': return '취소된 거래';
            case 'ABNORMAL': return '비정상거래';
            case 'HOLD': return '보류';
            default: return value || '-';
        }
    }

    function isIncludedStatus(value) {
        return value === 'NORMAL';
    }

    function renderOrderStatusOptions(selected) {
        const values = ['NORMAL', 'CANCELED', 'ABNORMAL', 'HOLD'];
        return values.map(value => `
            <option value="${value}" ${selected === value ? 'selected' : ''}>${getOrderStatusLabel(value)}</option>
        `).join('');
    }

    function renderOrderSummary(list) {
        if (!orderModalSummary) return;

        const reflectedRows = list.filter(row => !!row.included);

        const includedOrderCount = reflectedRows.length;
        const includedItemCount = reflectedRows.reduce((sum, row) => sum + Number(row.dealerItemCount || 0), 0);
        const includedGrossAmount = reflectedRows.reduce((sum, row) => sum + Number(row.dealerItemAmount || 0), 0);
        const includedSettlementAmount = reflectedRows.reduce((sum, row) => sum + Number(row.settlementAmount || 0), 0);

        orderModalSummary.innerHTML = `
            <div class="col-md-3">
                <div class="settlement-manager-order-summary-card">
                    <div class="settlement-manager-order-summary-label">반영 주문수</div>
                    <div class="settlement-manager-order-summary-value">${formatNumber(includedOrderCount)}건</div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="settlement-manager-order-summary-card">
                    <div class="settlement-manager-order-summary-label">반영 아이템수</div>
                    <div class="settlement-manager-order-summary-value">${formatNumber(includedItemCount)}개</div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="settlement-manager-order-summary-card">
                    <div class="settlement-manager-order-summary-label">반영 판매금액</div>
                    <div class="settlement-manager-order-summary-value">${formatNumber(includedGrossAmount)}원</div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="settlement-manager-order-summary-card">
                    <div class="settlement-manager-order-summary-label">반영 정산금액</div>
                    <div class="settlement-manager-order-summary-value">${formatNumber(includedSettlementAmount)}원</div>
                </div>
            </div>
        `;
    }

    function renderOrderRows(list) {
        originalOrderStateMap.clear();
        changedOrderMap.clear();
        refreshOrderSaveButton();

        if (!list || list.length === 0) {
            orderModalTbody.innerHTML = `<tr><td colspan="13" class="text-center text-muted">포함 주문이 없습니다.</td></tr>`;
            renderOrderSummary([]);
            return;
        }

        list.forEach(row => {
            originalOrderStateMap.set(Number(row.settlementOrderId), {
                inclusionStatus: row.inclusionStatus || 'NORMAL',
                memo: row.memo || ''
            });
        });

        orderModalTbody.innerHTML = list.map(row => {
            const excludedClass = row.included ? '' : 'settlement-manager-order-row-excluded';

            return `
                <tr class="${excludedClass}" data-settlement-order-id="${row.settlementOrderId}">
                    <td>${row.settlementOrderId}</td>
                    <td>${row.orderId}</td>
                    <td>${escapeHtml(row.orderNo || '-')}</td>
                    <td>${escapeHtml(row.ordererName || '-')}</td>
                    <td>${formatDateTime(row.basisDate)}</td>
                    <td class="text-center">${formatNumber(row.dealerItemCount)}</td>
                    <td class="text-end">${formatNumber(row.unitAmount)}원</td>
                    <td class="text-end">${formatNumber(row.dealerItemAmount)}원</td>
                    <td class="text-center">${formatPercent(row.commissionRate)}</td>
                    <td class="text-end text-danger">${formatNumber(row.commissionAmount)}원</td>
                    <td class="text-end fw-semibold text-success">${formatNumber(row.settlementAmount)}원</td>
                    <td>
                        <select class="form-select form-select-sm settlement-manager-order-status-select"
                                data-settlement-order-id="${row.settlementOrderId}">
                            ${renderOrderStatusOptions(row.inclusionStatus)}
                        </select>
                    </td>
                    <td>
                        <textarea class="form-control form-control-sm settlement-manager-order-memo"
                                  data-settlement-order-id="${row.settlementOrderId}"
                                  maxlength="1000"
                                  placeholder="관리자 메모 입력">${escapeHtml(row.memo || '')}</textarea>
                    </td>
                </tr>
            `;
        }).join('');

        renderOrderSummary(list);
    }

    async function loadOrders(settlementId, showModal = true) {
        currentOrderSettlementId = Number(settlementId);

        const res = await fetch(`/admin/root/api/settlement-manager/${settlementId}/orders`);
        const body = await toJsonSafe(res);

        if (!res.ok || !body || !body.success) {
            orderModalTbody.innerHTML = `<tr><td colspan="13" class="text-center text-danger">조회 실패</td></tr>`;
            renderOrderSummary([]);
            if (showModal && orderModal) {
                orderModal.show();
            }
            return;
        }

        renderOrderRows(body.data || []);

        if (showModal && orderModal) {
            orderModal.show();
        }
    }

    function updateOrderChangeState(settlementOrderId) {
        const numericId = Number(settlementOrderId);
        const original = originalOrderStateMap.get(numericId);
        if (!original) {
            return;
        }

        const row = orderModalTbody.querySelector(`tr[data-settlement-order-id="${numericId}"]`);
        if (!row) {
            return;
        }

        const statusSelect = row.querySelector('.settlement-manager-order-status-select');
        const memoTextarea = row.querySelector('.settlement-manager-order-memo');

        const currentStatus = statusSelect ? statusSelect.value : 'NORMAL';
        const currentMemo = (memoTextarea ? memoTextarea.value : '').trim();
        const originalMemo = (original.memo || '').trim();

        if (currentStatus === original.inclusionStatus && currentMemo === originalMemo) {
            changedOrderMap.delete(numericId);
        } else {
            changedOrderMap.set(numericId, {
                settlementOrderId: numericId,
                inclusionStatus: currentStatus,
                memo: currentMemo
            });
        }

        row.classList.toggle('settlement-manager-order-row-excluded', !isIncludedStatus(currentStatus));
        refreshOrderSaveButton();
    }

    async function saveOrderAdjustments() {
        if (!currentOrderSettlementId || changedOrderMap.size === 0) {
            return;
        }

        const payload = {
            settlementId: currentOrderSettlementId,
            items: Array.from(changedOrderMap.values()).map(item => ({
                settlementOrderId: Number(item.settlementOrderId),
                inclusionStatus: item.inclusionStatus,
                memo: item.memo
            }))
        };

        const res = await fetch(`/admin/root/api/settlement-manager/${currentOrderSettlementId}/orders`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const body = await toJsonSafe(res);

        if (!res.ok || !body || !body.success) {
            alert(body?.message || '주문 반영 저장 실패');
            return;
        }

        alert(body.message || '주문 반영 및 정산 재계산 완료');

        await loadOrders(currentOrderSettlementId, false);
        await load(currentPage);

        if (orderModal) {
            orderModal.show();
        }
    }

    async function saveStatus() {
        if (changedStatusMap.size === 0) {
            return;
        }

        const payload = {
            items: Array.from(changedStatusMap.entries()).map(([settlementId, payStatus]) => ({
                settlementId: Number(settlementId),
                payStatus: payStatus
            }))
        };

        const res = await fetch('/admin/root/api/settlement-manager/status', {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const body = await toJsonSafe(res);

        if (!res.ok || !body || !body.success) {
            alert(body?.message || '상태 변경 실패');
            return;
        }

        alert(body.message || '상태 변경 완료');
        changedStatusMap.clear();
        refreshSaveButton();
        load(currentPage);
    }

    function resetFilters() {
        document.getElementById('settlement-manager-size').value = '10';
        document.getElementById('settlement-manager-pay-status').value = '';
        document.getElementById('settlement-manager-keyword').value = '';
        document.querySelectorAll('#settlement-manager-basis-wrap input[type="checkbox"], #settlement-manager-cycle-wrap input[type="checkbox"]')
            .forEach(x => x.checked = false);

        changedStatusMap.clear();
        refreshSaveButton();
    }

    btnSearch.addEventListener('click', () => load(0));

    btnReset.addEventListener('click', () => {
        resetFilters();
        load(0);
    });

    btnSaveStatus.addEventListener('click', saveStatus);

    if (btnSaveOrderAdjustments) {
        btnSaveOrderAdjustments.addEventListener('click', saveOrderAdjustments);
    }

    tbody.addEventListener('click', (e) => {
        const btn = e.target.closest('.settlement-manager-order-btn');
        if (!btn) return;
        loadOrders(btn.dataset.settlementId, true);
    });

    tbody.addEventListener('change', (e) => {
        const select = e.target.closest('.settlement-manager-status-select');
        if (!select) return;

        const settlementId = select.dataset.settlementId;
        const original = select.dataset.original;
        const current = select.value;

        if (original === current) {
            changedStatusMap.delete(settlementId);
        } else {
            changedStatusMap.set(settlementId, current);
        }
        refreshSaveButton();
    });

    if (orderModalTbody) {
        orderModalTbody.addEventListener('change', (e) => {
            const select = e.target.closest('.settlement-manager-order-status-select');
            if (!select) return;
            updateOrderChangeState(select.dataset.settlementOrderId);
        });

        orderModalTbody.addEventListener('input', (e) => {
            const textarea = e.target.closest('.settlement-manager-order-memo');
            if (!textarea) return;
            updateOrderChangeState(textarea.dataset.settlementOrderId);
        });
    }

    if (orderModalEl) {
        orderModalEl.addEventListener('hidden.bs.modal', () => {
            currentOrderSettlementId = null;
            changedOrderMap.clear();
            originalOrderStateMap.clear();
            refreshOrderSaveButton();
        });
    }

    load(0);
})();