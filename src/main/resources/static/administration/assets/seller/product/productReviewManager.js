(function () {
    'use strict';
console.log('gd')
    document.addEventListener('DOMContentLoaded', function () {
        const filterForm = document.getElementById('dealer-review-filter-form');
        const pageInput = document.getElementById('dealer-review-page-input');
        const sizeSelect = document.getElementById('dealer-review-size');

        const checkAll = document.getElementById('dealer-review-check-all');
        const checkItems = document.querySelectorAll('.dealer-review-check-item');
        const deleteBtn = document.getElementById('dealer-review-delete-btn');

        function getCsrfInfo() {
            const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
            const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');

            if (!csrfTokenMeta || !csrfHeaderMeta) {
                return null;
            }

            return {
                token: csrfTokenMeta.getAttribute('content'),
                headerName: csrfHeaderMeta.getAttribute('content')
            };
        }

        function getCheckedIds() {
            return Array.from(document.querySelectorAll('.dealer-review-check-item:checked'))
                .map(function (checkbox) {
                    return Number(checkbox.value);
                })
                .filter(function (value) {
                    return !Number.isNaN(value);
                });
        }

        function refreshDeleteButtonState() {
            const checkedIds = getCheckedIds();
            deleteBtn.disabled = checkedIds.length === 0;
        }

        function refreshCheckAllState() {
            if (!checkAll) {
                return;
            }

            const total = checkItems.length;
            const checked = getCheckedIds().length;

            checkAll.checked = total > 0 && checked === total;
            checkAll.indeterminate = checked > 0 && checked < total;
        }

        if (sizeSelect && filterForm && pageInput) {
            sizeSelect.addEventListener('change', function () {
                pageInput.value = '0';
                filterForm.submit();
            });
        }

        if (checkAll) {
            checkAll.addEventListener('change', function () {
                checkItems.forEach(function (item) {
                    item.checked = checkAll.checked;
                });
                refreshCheckAllState();
                refreshDeleteButtonState();
            });
        }

        checkItems.forEach(function (item) {
            item.addEventListener('change', function () {
                refreshCheckAllState();
                refreshDeleteButtonState();
            });
        });

        if (deleteBtn) {
            deleteBtn.addEventListener('click', function () {
                const checkedIds = getCheckedIds();

                if (checkedIds.length === 0) {
                    alert('삭제할 리뷰를 선택해 주세요.');
                    return;
                }

                if (!confirm('선택한 리뷰를 삭제하시겠습니까? 삭제된 리뷰와 리뷰 이미지는 복구할 수 없습니다.')) {
                    return;
                }

                deleteBtn.disabled = true;

                const headers = {
                    'Content-Type': 'application/json'
                };

                const csrfInfo = getCsrfInfo();
                if (csrfInfo) {
                    headers[csrfInfo.headerName] = csrfInfo.token;
                }

                fetch('/seller/productReviewManager/delete', {
                    method: 'POST',
                    headers: headers,
                    body: JSON.stringify({
                        reviewIds: checkedIds
                    })
                })
                .then(function (response) {
                    return response.json().then(function (data) {
                        return {
                            ok: response.ok,
                            status: response.status,
                            data: data
                        };
                    });
                })
                .then(function (result) {
                    if (!result.ok || !result.data.success) {
                        throw new Error(result.data && result.data.message ? result.data.message : '리뷰 삭제에 실패했습니다.');
                    }

                    alert(result.data.message || '리뷰가 삭제되었습니다.');
                    window.location.reload();
                })
                .catch(function (error) {
                    alert(error.message || '리뷰 삭제 중 오류가 발생했습니다.');
                    refreshDeleteButtonState();
                });
            });
        }

        refreshCheckAllState();
        refreshDeleteButtonState();
    });
})();