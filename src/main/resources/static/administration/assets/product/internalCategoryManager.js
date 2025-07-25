// 내부카테고리 관리자 JS (internalCategoryManager.js)
// 모든 id/class internal-category- 접두사 사용

document.addEventListener("DOMContentLoaded", function () {

    // =========== [ 공통함수 ] ===========
    function showAlert(msg) {
        window.alert(msg); // 추후 모달 등 교체 가능
    }

    // =========== [ 대분류 ] ===========
    const largeNameInput = document.getElementById('internal-category-large-name');
    const largeAddBtn = document.getElementById('internal-category-large-add-btn');
    const largeList = document.getElementById('internal-category-large-list');

    // =========== [ 중분류 ] ===========
    const mediumLargeSelect = document.getElementById('internal-category-medium-large-select');
    const mediumNameInput = document.getElementById('internal-category-medium-name');
    const mediumAddBtn = document.getElementById('internal-category-medium-add-btn');
    const mediumList = document.getElementById('internal-category-medium-list');

    // =========== [ 소분류 ] ===========
    const smallLargeSelect = document.getElementById('internal-category-small-large-select');
    const smallMediumSelect = document.getElementById('internal-category-small-medium-select');
    const smallNameInput = document.getElementById('internal-category-small-name');
    const smallAddBtn = document.getElementById('internal-category-small-add-btn');
    const smallList = document.getElementById('internal-category-small-list');

    // =========== [ 대분류 CRUD ] ===========
    function loadLargeList() {
        fetch('/api/internal-category/large')
            .then(res => res.json())
            .then(data => {
                renderLargeList(data);
                renderLargeSelects(data); // 중분류/소분류용 select도 갱신
            });
    }

    function renderLargeList(list) {
        largeList.innerHTML = '';
        list.forEach(item => {
            const div = document.createElement('div');
            div.className = 'd-flex align-items-center mb-2 justify-content-between';
            div.innerHTML = `
                <div>
                    <span class="fw-bold">${item.name}</span>
                    <span class="badge bg-secondary ms-2">${item.mediumCount || 0}개 중분류</span>
                </div>
                <div>
                    <button class="btn btn-sm btn-outline-primary internal-category-large-edit-btn" data-id="${item.id}" data-name="${item.name}">수정</button>
                    <button class="btn btn-sm btn-outline-danger internal-category-large-del-btn ms-1" data-id="${item.id}">삭제</button>
                </div>
            `;
            largeList.appendChild(div);
        });
        setLargeListEvents();
    }

    function setLargeListEvents() {
        // 수정
        document.querySelectorAll('.internal-category-large-edit-btn').forEach(btn => {
            btn.onclick = function () {
                const id = this.dataset.id;
                const oldName = this.dataset.name;
                const newName = prompt('대분류명을 수정하세요.', oldName);
                if (newName && newName.trim() && newName !== oldName) {
                    fetch(`/api/internal-category/large/${id}`, {
                        method: 'PUT',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ name: newName.trim() })
                    }).then(r => {
                        if (!r.ok) return r.text().then(showAlert);
                        loadLargeList();
                        loadMediumList(); // 연관 업데이트
                        loadSmallList();
                    });
                }
            };
        });
        // 삭제
        document.querySelectorAll('.internal-category-large-del-btn').forEach(btn => {
            btn.onclick = function () {
                const id = this.dataset.id;
                if (confirm('해당 대분류와 하위 모든 분류가 삭제됩니다. 진행할까요?')) {
                    fetch(`/api/internal-category/large/${id}`, { method: 'DELETE' })
                        .then(r => {
                            if (!r.ok) return r.text().then(showAlert);
                            loadLargeList();
                            loadMediumList();
                            loadSmallList();
                        });
                }
            };
        });
    }

    largeAddBtn.onclick = function () {
        const name = largeNameInput.value.trim();
        if (!name) return showAlert('대분류명을 입력하세요.');
        fetch('/api/internal-category/large', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name })
        }).then(r => {
            if (!r.ok) return r.text().then(showAlert);
            largeNameInput.value = '';
            loadLargeList();
        });
    };

    // =========== [ 중분류 CRUD ] ===========
    function loadMediumList() {
        const largeId = mediumLargeSelect.value;
        if (!largeId) {
            mediumList.innerHTML = '<div class="text-secondary">대분류 선택 필요</div>';
            return;
        }
        fetch(`/api/internal-category/medium?largeId=${largeId}`)
            .then(res => res.json())
            .then(data => renderMediumList(data));
    }

    function renderMediumList(list) {
        mediumList.innerHTML = '';
        list.forEach(item => {
            const div = document.createElement('div');
            div.className = 'd-flex align-items-center mb-2 justify-content-between';
            div.innerHTML = `
                <div>
                    <span class="fw-bold">${item.name}</span>
                    <span class="badge bg-success ms-2">${item.smallCount || 0}개 소분류</span>
                </div>
                <div>
                    <button class="btn btn-sm btn-outline-primary internal-category-medium-edit-btn" data-id="${item.id}" data-name="${item.name}">수정</button>
                    <button class="btn btn-sm btn-outline-danger internal-category-medium-del-btn ms-1" data-id="${item.id}">삭제</button>
                </div>
            `;
            mediumList.appendChild(div);
        });
        setMediumListEvents();
    }

    function setMediumListEvents() {
        // 수정
        document.querySelectorAll('.internal-category-medium-edit-btn').forEach(btn => {
            btn.onclick = function () {
                const id = this.dataset.id;
                const oldName = this.dataset.name;
                const newName = prompt('중분류명을 수정하세요.', oldName);
                if (newName && newName.trim() && newName !== oldName) {
                    fetch(`/api/internal-category/medium/${id}`, {
                        method: 'PUT',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ name: newName.trim() })
                    }).then(r => {
                        if (!r.ok) return r.text().then(showAlert);
                        loadMediumList();
                        loadSmallList();
                    });
                }
            };
        });
        // 삭제
        document.querySelectorAll('.internal-category-medium-del-btn').forEach(btn => {
            btn.onclick = function () {
                const id = this.dataset.id;
                if (confirm('해당 중분류와 하위 모든 소분류가 삭제됩니다. 진행할까요?')) {
                    fetch(`/api/internal-category/medium/${id}`, { method: 'DELETE' })
                        .then(r => {
                            if (!r.ok) return r.text().then(showAlert);
                            loadMediumList();
                            loadSmallList();
                        });
                }
            };
        });
    }

    mediumAddBtn.onclick = function () {
        const name = mediumNameInput.value.trim();
        const largeId = mediumLargeSelect.value;
        if (!largeId) return showAlert('대분류를 먼저 선택하세요.');
        if (!name) return showAlert('중분류명을 입력하세요.');
        fetch('/api/internal-category/medium', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, largeId })
        }).then(r => {
            if (!r.ok) return r.text().then(showAlert);
            mediumNameInput.value = '';
            loadMediumList();
        });
    };

    mediumLargeSelect.onchange = loadMediumList;

    // =========== [ 소분류 CRUD ] ===========
    function loadSmallList() {
        const largeId = smallLargeSelect.value;
        const mediumId = smallMediumSelect.value;
        if (!largeId || !mediumId) {
            smallList.innerHTML = '<div class="text-secondary">대분류와 중분류 선택 필요</div>';
            return;
        }
        fetch(`/api/internal-category/small?mediumId=${mediumId}`)
            .then(res => res.json())
            .then(data => renderSmallList(data));
    }

    function renderSmallList(list) {
        smallList.innerHTML = '';
        list.forEach(item => {
            const div = document.createElement('div');
            div.className = 'd-flex align-items-center mb-2 justify-content-between';
            div.innerHTML = `
                <div>
                    <span class="fw-bold">${item.name}</span>
                </div>
                <div>
                    <button class="btn btn-sm btn-outline-primary internal-category-small-edit-btn" data-id="${item.id}" data-name="${item.name}">수정</button>
                    <button class="btn btn-sm btn-outline-danger internal-category-small-del-btn ms-1" data-id="${item.id}">삭제</button>
                </div>
            `;
            smallList.appendChild(div);
        });
        setSmallListEvents();
    }

    function setSmallListEvents() {
        // 수정
        document.querySelectorAll('.internal-category-small-edit-btn').forEach(btn => {
            btn.onclick = function () {
                const id = this.dataset.id;
                const oldName = this.dataset.name;
                const newName = prompt('소분류명을 수정하세요.', oldName);
                if (newName && newName.trim() && newName !== oldName) {
                    fetch(`/api/internal-category/small/${id}`, {
                        method: 'PUT',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ name: newName.trim() })
                    }).then(r => {
                        if (!r.ok) return r.text().then(showAlert);
                        loadSmallList();
                    });
                }
            };
        });
        // 삭제
        document.querySelectorAll('.internal-category-small-del-btn').forEach(btn => {
            btn.onclick = function () {
                const id = this.dataset.id;
                if (confirm('해당 소분류가 삭제됩니다. 진행할까요?')) {
                    fetch(`/api/internal-category/small/${id}`, { method: 'DELETE' })
                        .then(r => {
                            if (!r.ok) return r.text().then(showAlert);
                            loadSmallList();
                        });
                }
            };
        });
    }

    smallAddBtn.onclick = function () {
        const name = smallNameInput.value.trim();
        const mediumId = smallMediumSelect.value;
        if (!smallLargeSelect.value) return showAlert('대분류를 선택하세요.');
        if (!mediumId) return showAlert('중분류를 선택하세요.');
        if (!name) return showAlert('소분류명을 입력하세요.');
        fetch('/api/internal-category/small', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, mediumId })
        }).then(r => {
            if (!r.ok) return r.text().then(showAlert);
            smallNameInput.value = '';
            loadSmallList();
        });
    };

    // =========== [ 대분류/중분류 select 옵션 갱신 ] ===========
    function renderLargeSelects(list) {
        // 중분류용 select
        mediumLargeSelect.innerHTML = '<option value="">대분류 선택</option>';
        // 소분류용 대분류 select
        smallLargeSelect.innerHTML = '<option value="">대분류 선택</option>';
        list.forEach(item => {
            mediumLargeSelect.innerHTML += `<option value="${item.id}">${item.name}</option>`;
            smallLargeSelect.innerHTML += `<option value="${item.id}">${item.name}</option>`;
        });
    }

    function loadMediumOptionsForSmall() {
        // 소분류용 중분류 select
        const largeId = smallLargeSelect.value;
        smallMediumSelect.innerHTML = '<option value="">중분류 선택</option>';
        if (!largeId) return;
        fetch(`/api/internal-category/medium?largeId=${largeId}`)
            .then(res => res.json())
            .then(list => {
                list.forEach(item => {
                    smallMediumSelect.innerHTML += `<option value="${item.id}">${item.name}</option>`;
                });
            });
    }

    smallLargeSelect.onchange = function () {
        loadMediumOptionsForSmall();
        smallList.innerHTML = '<div class="text-secondary">중분류 선택 필요</div>';
    };
    smallMediumSelect.onchange = loadSmallList;

    // =========== [ 초기 로딩 ] ===========
    loadLargeList();
    // 중분류/소분류 셀렉트, 리스트 동기화 필요시
    mediumLargeSelect.onchange = loadMediumList;

});
