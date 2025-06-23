const typeMap = {
    'INPUT': '일반 입력란',
    'TEXTAREA': '여러줄 입력란',
    'SELECT': '선택(드롭다운)',
    'FILE': '파일 업로드',
    'CKEDITOR': '에디터(HTML)'
};
let rowData = []; // 서버와 동기화될 데이터

const tbody = document.getElementById('questionTbody');
const saveBtn = document.getElementById('saveBtn');
const typeSelect = document.getElementById('typeSelect');

function setDirty() { saveBtn.disabled = false; }
function setClean() { saveBtn.disabled = true; }

// row 랜더링
function renderRows() {
    tbody.innerHTML = '';
    rowData.forEach((row, i) => {
        tbody.insertAdjacentHTML('beforeend', makeRowHtml(row, i));
    });
}

// placeholder input 노출여부 (INPUT, TEXTAREA만 노출)
function showPlaceholderInput(type) {
    return type === 'INPUT' || type === 'TEXTAREA';
}

// row HTML 생성 (placeholder, 옵션 등 조건 분기)
function makeRowHtml(row, idx) {
    const isSelect = row.type === 'SELECT';
    let optionInput = '-';
    if (isSelect) {
        optionInput = `
            <div class="d-flex align-items-center mb-1 gap-1 display-manager-option-row">
                <input type="text" class="form-control form-control-sm display-manager-option-input option-input" placeholder="옵션값 입력" style="max-width:80px;" data-idx="${idx}">
                <button type="button" class="btn btn-outline-secondary btn-sm ms-1 display-manager-add-option-btn add-option-btn" data-idx="${idx}">등록</button>
            </div>
            <div class="display-manager-option-list option-list" id="optionList_${idx}">
                ${(row.options || []).map((opt, j) =>
                    `<span class="display-manager-option-chip option-chip">
                        <span class="display-manager-option-label">${opt}</span>
                        <button type="button" class="display-manager-del-option del-option" title="삭제" data-idx="${idx}" data-opt="${j}">&times;</button>
                    </span>`
                ).join('')}
            </div>
        `;
    }
    return `
    <tr class="sortable-row" data-idx="${idx}">
        <td class="text-center">
            <button type="button" class="btn btn-outline-secondary btn-sm action-btn move-up" title="위로" ${idx === 0 ? 'disabled' : ''}>▲</button>
            <button type="button" class="btn btn-outline-secondary btn-sm action-btn move-down" title="아래로" ${idx === rowData.length - 1 ? 'disabled' : ''}>▼</button>
        </td>
        <td>
            <select class="form-select form-select-sm display-select" data-idx="${idx}">
                <option value="Y"${row.display === 'Y' ? ' selected' : ''}>표시함</option>
                <option value="N"${row.display === 'N' ? ' selected' : ''}>숨김</option>
            </select>
        </td>
        <td>
            <input type="text" class="form-control form-control-sm label-input" data-idx="${idx}" value="${row.label || ''}" required placeholder="항목명">
        </td>
        <td>
            ${
                showPlaceholderInput(row.type)
                ? `<input type="text" class="form-control form-control-sm placeholder-sm placeholder-input" data-idx="${idx}" value="${row.placeholder || ''}" placeholder="표시 텍스트">`
                : `<input type="text" class="form-control form-control-sm placeholder-sm" disabled style="background:#f5f5f5;" value="" placeholder="표시 텍스트">`
            }
        </td>
        <td>
            <input type="hidden" value="${row.type}">
            <span>${typeMap[row.type]}</span>
        </td>
        <td>
            ${optionInput}
        </td>
        <td class="text-center">
            <button type="button" class="btn btn-danger btn-sm del-row" data-idx="${idx}">삭제</button>
        </td>
    </tr>
    `;
}

// 신규 행 추가
document.getElementById('addRowBtn').addEventListener('click', function() {
    const type = typeSelect.value;
    rowData.push({
        display: 'Y', label: '', placeholder: '', type, options: []
    });
    setDirty();
    renderRows();
});

// 행 삭제
tbody.addEventListener('click', function(e) {
    if (e.target.classList.contains('del-row')) {
        const idx = parseInt(e.target.dataset.idx);
        rowData.splice(idx, 1);
        setDirty();
        renderRows();
    }
});

// 순서 이동
tbody.addEventListener('click', function(e) {
    const row = e.target.closest('tr');
    if (!row) return;
    const idx = parseInt(row.dataset.idx);
    if (e.target.classList.contains('move-up') && idx > 0) {
        [rowData[idx - 1], rowData[idx]] = [rowData[idx], rowData[idx - 1]];
        setDirty();
        renderRows();
    }
    if (e.target.classList.contains('move-down') && idx < rowData.length - 1) {
        [rowData[idx], rowData[idx + 1]] = [rowData[idx + 1], rowData[idx]];
        setDirty();
        renderRows();
    }
});

// 입력값 변경시 dirty
tbody.addEventListener('input', function(e) {
    const idx = parseInt(e.target.dataset.idx);
    if (e.target.classList.contains('label-input')) {
        rowData[idx].label = e.target.value;
        setDirty();
    }
    if (e.target.classList.contains('placeholder-input')) {
        rowData[idx].placeholder = e.target.value;
        setDirty();
    }
    if (e.target.classList.contains('display-select')) {
        rowData[idx].display = e.target.value;
        rowData[idx].required = (e.target.value === 'Y');
        setDirty();
    }
});

// 옵션 등록 (등록 버튼/엔터키)
tbody.addEventListener('click', function(e) {
    if (e.target.classList.contains('add-option-btn') || e.target.classList.contains('display-manager-add-option-btn')) {
        const idx = parseInt(e.target.dataset.idx);
        const input = tbody.querySelector(`input.option-input[data-idx="${idx}"]`);
        const val = input.value.trim();
        if (val) { // 중복 허용: 중복 체크 제거
            rowData[idx].options.push(val);
            setDirty();
            renderRows();
            setTimeout(() => {
                const newInput = tbody.querySelector(`input.option-input[data-idx="${idx}"]`);
                if (newInput) newInput.focus();
            }, 50);
        }
    }
    if (e.target.classList.contains('del-option') || e.target.classList.contains('display-manager-del-option')) {
        const idx = parseInt(e.target.dataset.idx);
        const optIdx = parseInt(e.target.dataset.opt);
        rowData[idx].options.splice(optIdx, 1);
        setDirty();
        renderRows();
    }
});
tbody.addEventListener('keydown', function(e) {
    if (e.target.classList.contains('option-input') && e.key === 'Enter') {
        e.preventDefault();
        const idx = parseInt(e.target.dataset.idx);
        const val = e.target.value.trim();
        if (val) { // 중복 허용: 중복 체크 제거
            rowData[idx].options.push(val);
            setDirty();
            renderRows();
            setTimeout(() => {
                const newInput = tbody.querySelector(`input.option-input[data-idx="${idx}"]`);
                if (newInput) newInput.focus();
            }, 50);
        }
    }
});

// 저장 버튼 - 서버로 rowData 저장
saveBtn.addEventListener('click', function() {
    // 유효성 검증
    for (let i = 0; i < rowData.length; i++) {
        if (!rowData[i].label || rowData[i].label.trim() === '') {
            alert('항목명은 필수 입력입니다.');
            return;
        }
        if (rowData[i].type === 'SELECT' && (!rowData[i].options || rowData[i].options.length === 0)) {
            alert('선택(드롭다운) 타입은 옵션값이 1개 이상 있어야 합니다.');
            return;
        }
    }
    // display/required 변환
    rowData.forEach(row => {
        row.required = (row.display === 'Y');
    });

    fetch('/api/display-questions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(rowData)
    })
    .then(res => {
        if (!res.ok) throw new Error('저장에 실패했습니다.');
        return res;
    })
    .then(() => {
        alert('저장되었습니다!');
        setClean();
        loadQuestions(); // 저장 후 재조회
    })
    .catch(e => {
        alert(e.message || '오류가 발생했습니다.');
    });
});

// 서버에서 기존 데이터 불러오기
function loadQuestions() {
    fetch('/api/display-questions')
        .then(res => res.json())
        .then(data => {
            rowData = data.map(q => ({
                id: q.id,
                label: q.label,
                placeholder: q.placeholder,
                type: q.type,
                required: q.required,
                display: q.required ? 'Y' : 'N',
                options: (q.options || []).map(opt => opt.value)
            }));
            setClean();
            renderRows();
        });
}
window.addEventListener('DOMContentLoaded', loadQuestions);
