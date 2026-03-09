(function () {
    'use strict';

    const orderId = window.ADMIN_ORDER_LIST_DETAIL_ORDER_ID;
    if (!orderId) return;

    const changeFields = document.querySelectorAll('.admin-order-list-detail-change-field');
    const saveBtn = document.getElementById('admin-order-list-detail-save-btn');

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

    function hasAnyChanged() {
        let changed = false;

        changeFields.forEach(field => {
            const originalValue = field.dataset.originalValue;
            if (field.value !== originalValue) {
                changed = true;
            }
        });

        return changed;
    }

    function refreshButtonState() {
        saveBtn.disabled = !hasAnyChanged();
    }

    changeFields.forEach(field => {
        field.addEventListener('change', refreshButtonState);
    });

    saveBtn?.addEventListener('click', async function () {
        if (!hasAnyChanged()) {
            alert('변경된 값이 없습니다.');
            return;
        }

        if (!confirm('주문 상세 상태를 변경하시겠습니까?')) {
            return;
        }

        const payload = {
            status: document.getElementById('admin-order-list-detail-status').value,
            paymentMethod: document.getElementById('admin-order-list-detail-payment-method').value,
            shippingMethod: document.getElementById('admin-order-list-detail-shipping-method').value,
            shippingPayType: document.getElementById('admin-order-list-detail-shipping-pay-type').value
        };

        const headers = {
            'Content-Type': 'application/json'
        };

        const csrf = getCsrfHeader();
        if (csrf) {
            headers[csrf.headerName] = csrf.token;
        }

        try {
            const response = await fetch(`/admin/root/api/orders/${orderId}/status-detail`, {
                method: 'POST',
                headers: headers,
                body: JSON.stringify(payload)
            });

            const result = await response.json();

            if (!response.ok || !result.success) {
                throw new Error(result.message || '상태 변경 중 오류가 발생했습니다.');
            }

            alert(result.message || '저장되었습니다.');
            window.location.reload();
        } catch (e) {
            alert(e.message || '상태 변경 중 오류가 발생했습니다.');
        }
    });

    refreshButtonState();
})();