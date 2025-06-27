// /administration/assets/product/productDiscountManager.js
document.addEventListener('DOMContentLoaded', function () {
    // ======== 변수 선언 ========
    const discountTermSelect = document.getElementById('discountTermSelect');
    const startDateInput = document.querySelector('input[name="startDate"]');
    const endDateInput = document.querySelector('input[name="endDate"]');
    const periodOnRadio = document.getElementById('periodOn');
    const periodOffRadio = document.getElementById('periodOff');

    const iconFileInput = document.getElementById('iconFile');
    const iconPreview = document.getElementById('iconPreview');
    const removeIconBtn = document.getElementById('removeIcon');
    let currentIconFile = null;

    const form = document.getElementById('product-discount-form');
    const submitBtn = document.getElementById('submitDiscountBtn');

    // ======== 1. 기간설정 사용함/안함 처리 ========
    function updatePeriodFieldStatus() {
        const isEnabled = periodOnRadio.checked;
        startDateInput.disabled = !isEnabled;
        endDateInput.disabled = !isEnabled;
        if (!isEnabled) {
            startDateInput.value = '';
            endDateInput.value = '';
        }
    }
    if (periodOnRadio && periodOffRadio && startDateInput && endDateInput) {
        periodOnRadio.addEventListener('change', updatePeriodFieldStatus);
        periodOffRadio.addEventListener('change', updatePeriodFieldStatus);
        updatePeriodFieldStatus();
    }

    // ======== 2. 아이콘 업로드/미리보기/x버튼 ========
    function resetIconPreview() {
        iconPreview.src = '#';
        iconPreview.style.display = 'none';
        removeIconBtn.style.display = 'none';
        iconFileInput.value = '';
        currentIconFile = null;
    }
    if (iconFileInput && iconPreview && removeIconBtn) {
        iconFileInput.addEventListener('change', function (event) {
            const file = event.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = function (e) {
                    iconPreview.src = e.target.result;
                    iconPreview.style.display = 'inline-block';
                    removeIconBtn.style.display = 'inline-block';
                };
                reader.readAsDataURL(file);
                currentIconFile = file;
            } else {
                resetIconPreview();
            }
        });
        removeIconBtn.addEventListener('click', function () {
            resetIconPreview();
        });
    }

    // ======== 3. 저장 전 입력값 검증/상세 콘솔 출력/확인 ========
    if (form) {
        form.addEventListener('submit', function (e) {
            e.preventDefault();

            // 1. 데이터 수집
            const data = {};
            // 기본 설정
            data.active = form.querySelector('input[name="active"]:checked')?.value;
            data.type = form.querySelector('select[name="type"]')?.value;
            data.term = form.querySelector('select[name="term"]')?.value;
            data.name = form.querySelector('input[name="name"]')?.value?.trim();

            // 조건설정
            data.periodEnabled = form.querySelector('input[name="periodEnabled"]:checked')?.value === 'true';
            data.startDate = startDateInput.value ? startDateInput.value : null;
            data.endDate = endDateInput.value ? endDateInput.value : null;

            // 적용대상
            data.applyToAll = form.querySelector('input[name="applyToAll"]')?.checked || false;
            data.applyToDealer = form.querySelector('input[name="applyToDealer"]')?.checked || false;
            data.applyToRegular = form.querySelector('input[name="applyToRegular"]')?.checked || false;

            // 혜택설정
            data.discountPercent = form.querySelector('input[name="discountPercent"]')?.value;
            data.couponPolicy = form.querySelector('select[name="couponPolicy"]')?.value;

            // 아이콘 파일
            data.iconFile = iconFileInput.files[0] || null;

            // 2. 검증
            const errors = [];
            // 필수값 검증
            if (!data.name) errors.push('혜택명은 필수입니다.');
            if (!data.type) errors.push('구분은 필수입니다.');
            if (!data.term) errors.push('혜택유형은 필수입니다.');
            if (!data.discountPercent) errors.push('할인율은 필수입니다.');
            // 기간설정 검증
            if (data.periodEnabled) {
                if (!data.startDate && !data.endDate) {
                    errors.push('기간설정이 사용됨일 때 시작일, 종료일 중 하나 이상은 필수입니다.');
                }
            }
            // 할인율 검증
            if (data.discountPercent) {
                const percent = Number(data.discountPercent);
                if (isNaN(percent) || percent < 0 || percent > 100) {
                    errors.push('할인율은 0~100 사이의 숫자여야 합니다.');
                }
            }
            if (!data.couponPolicy) errors.push('쿠폰 정책은 필수입니다.');

            // 3. 상세 콘솔 출력
            if (errors.length > 0) {
                alert(errors.join('\n'));
                return;
            }

            let msg = "==== 할인혜택 입력값 상세 ====\n";
            msg += `진행여부: ${data.active}\n`;
            msg += `구분(type): ${data.type}\n`;
            msg += `혜택유형(term): ${data.term}\n`;
            msg += `혜택명(name): ${data.name}\n`;
            msg += `기간설정사용: ${data.periodEnabled}\n`;
            msg += `시작일: ${data.startDate}\n`;
            msg += `종료일: ${data.endDate}\n`;
            msg += `적용대상-전체: ${data.applyToAll}\n`;
            msg += `적용대상-딜러: ${data.applyToDealer}\n`;
            msg += `적용대상-일반: ${data.applyToRegular}\n`;
            msg += `할인율: ${data.discountPercent}\n`;
            msg += `쿠폰정책: ${data.couponPolicy}\n`;
            msg += `아이콘첨부: ${data.iconFile ? data.iconFile.name : "없음"}\n`;

            // 상세 콘솔
            console.log('===== 할인혜택 상세 제출 데이터 =====');
            console.table(data);

            // 4. confirm으로 사용자 최종확인
            if (!confirm(msg + "\n\n이대로 저장할까요?")) {
                alert('저장 취소됨');
                return;
            }

            // 5. 저장 로직
            const formData = new FormData(form);
            fetch('/api/product-discount', {
                method: 'POST',
                body: formData
            })
            .then(response => {
                if (!response.ok) throw new Error("저장실패");
                return response.json();
            })
            .then(res => {
                alert('저장 성공!');
                window.location.reload();
            })
            .catch(err => {
                alert('저장 실패');
                console.error(err);
            });
        });
    }
});
