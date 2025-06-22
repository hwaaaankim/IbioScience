// categoryManager.js

document.addEventListener('DOMContentLoaded', function () {
    // 캐시 변수
    let largeList = [];
    let mediumList = [];
    let smallList = [];

    // ------------------- 대분류 ------------------- //
    const $largeForm = document.getElementById('largeForm');
    const $largeName = document.getElementById('largeName');
    const $largeList = document.getElementById('largeList');
    const $largeSelect = document.getElementById('largeSelect');
    // ------------------- 중분류 ------------------- //
    const $mediumForm = document.getElementById('mediumForm');
    const $mediumName = document.getElementById('mediumName');
    const $mediumList = document.getElementById('mediumList');
    // ------------------- 소분류 ------------------- //
    const $smallForm = document.getElementById('smallForm');
    const $smallName = document.getElementById('smallName');
    const $smallList = document.getElementById('smallList');
    const $smallSelect = document.getElementById('smallSelect');
    // --------- 소분류-중분류 매핑 영역 --------- //
    const $mappingLargeSelect = document.getElementById('mappingLargeSelect');
    const $mappingMediumSelect = document.getElementById('mappingMediumSelect');
    const $mappingBtn = document.getElementById('mappingBtn');
    const $mappingList = document.getElementById('mappingList');

    // API 호출 함수
    async function api(url, method = 'GET', data) {
        const options = { method, headers: {} };
        if (data) {
            options.headers['Content-Type'] = 'application/json';
            options.body = JSON.stringify(data);
        }
        const res = await fetch(url, options);
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.message || '서버 오류');
        }
        return await res.json();
    }

    // ------------------- 초기화 ------------------- //
    async function loadAll() {
        // 대분류
        largeList = await api('/api/category/large');
        renderLargeList();
        renderLargeSelect();

        // 중분류
        mediumList = await api('/api/category/medium');
        renderMediumList();
        renderMappingLargeSelect();

        // 소분류
        smallList = await api('/api/category/small');
        renderSmallList();
        renderSmallSelect();
    }
    loadAll();

    // ------------------- 대분류 CRUD ------------------- //
    $largeForm.onsubmit = async function (e) {
        e.preventDefault();
        const name = $largeName.value.trim();
        if (!name) return alert('대분류명을 입력하세요.');
        try {
            await api('/api/category/large', 'POST', { name });
            $largeName.value = '';
            await loadAll();
        } catch (e) {
            alert(e.message);
        }
    };

    function renderLargeList() {
        $largeList.innerHTML = '';
        largeList.forEach(l => {
            const li = document.createElement('li');
            li.className = 'list-group-item d-flex justify-content-between align-items-center';
            li.innerHTML = `
                <span class="name">${l.name}</span>
                <div>
                    <button class="btn btn-sm btn-outline-secondary me-1 edit">수정</button>
                    <button class="btn btn-sm btn-outline-danger delete">삭제</button>
                </div>
            `;
            // 수정
            li.querySelector('.edit').onclick = function () {
                if (li.querySelector('.edit-input')) return; // 이미 수정중
                const input = document.createElement('input');
                input.type = 'text';
                input.className = 'form-control edit-input me-2';
                input.value = l.name;
                li.querySelector('.name').replaceWith(input);
                this.textContent = '저장';
                this.onclick = async function () {
                    const newName = input.value.trim();
                    if (!newName) return alert('값을 입력하세요.');
                    try {
                        await api(`/api/category/large/${l.id}`, 'PUT', { name: newName });
                        await loadAll();
                    } catch (e) {
                        alert(e.message);
                    }
                }
            };
            // 삭제
            li.querySelector('.delete').onclick = async function () {
                if (!confirm('정말 삭제하시겠습니까?')) return;
                try {
                    await api(`/api/category/large/${l.id}`, 'DELETE');
                    await loadAll();
                } catch (e) {
                    alert(e.message);
                }
            };
            $largeList.appendChild(li);
        });
    }

    function renderLargeSelect() {
        $largeSelect.innerHTML = '<option value="">대분류 선택</option>';
        largeList.forEach(l => {
            $largeSelect.innerHTML += `<option value="${l.id}">${l.name}</option>`;
        });
    }

    // ------------------- 중분류 CRUD ------------------- //
    $mediumForm.onsubmit = async function (e) {
        e.preventDefault();
        const name = $mediumName.value.trim();
        const largeId = $largeSelect.value;
        if (!largeId) return alert('대분류를 선택하세요.');
        if (!name) return alert('중분류명을 입력하세요.');
        try {
            await api('/api/category/medium', 'POST', { name, largeId });
            $mediumName.value = '';
            await loadAll();
        } catch (e) {
            alert(e.message);
        }
    };

    function renderMediumList() {
        $mediumList.innerHTML = '';
        const selLargeId = $largeSelect.value;
        mediumList.filter(m => !selLargeId || m.largeId == selLargeId)
        .forEach(m => {
            const li = document.createElement('li');
            li.className = 'list-group-item d-flex justify-content-between align-items-center';
            const large = largeList.find(l => l.id == m.largeId);
            li.innerHTML = `
                <span class="name">${m.name} <span class="badge bg-light text-dark ms-1">${large ? large.name : ''}</span></span>
                <div>
                    <button class="btn btn-sm btn-outline-secondary me-1 edit">수정</button>
                    <button class="btn btn-sm btn-outline-danger delete">삭제</button>
                </div>
            `;
            // 수정
            li.querySelector('.edit').onclick = function () {
                if (li.querySelector('.edit-input')) return;
                const input = document.createElement('input');
                input.type = 'text';
                input.className = 'form-control edit-input me-2';
                input.value = m.name;
                li.querySelector('.name').replaceWith(input);
                this.textContent = '저장';
                this.onclick = async function () {
                    const newName = input.value.trim();
                    if (!newName) return alert('값을 입력하세요.');
                    try {
                        await api(`/api/category/medium/${m.id}`, 'PUT', { name: newName });
                        await loadAll();
                    } catch (e) {
                        alert(e.message);
                    }
                }
            };
            // 삭제
            li.querySelector('.delete').onclick = async function () {
                if (!confirm('정말 삭제하시겠습니까?')) return;
                try {
                    await api(`/api/category/medium/${m.id}`, 'DELETE');
                    await loadAll();
                } catch (e) {
                    alert(e.message);
                }
            };
            $mediumList.appendChild(li);
        });
    }

    $largeSelect.onchange = renderMediumList;

    // ------------------- 소분류 CRUD ------------------- //
    $smallForm.onsubmit = async function (e) {
        e.preventDefault();
        const name = $smallName.value.trim();
        if (!name) return alert('소분류명을 입력하세요.');
        try {
            await api('/api/category/small', 'POST', { name });
            $smallName.value = '';
            await loadAll();
        } catch (e) {
            alert(e.message);
        }
    };

    function renderSmallList() {
        $smallList.innerHTML = '';
        smallList.forEach(s => {
            const li = document.createElement('li');
            li.className = 'list-group-item d-flex justify-content-between align-items-center';
            li.innerHTML = `
                <span class="name">${s.name}</span>
                <div>
                    <button class="btn btn-sm btn-outline-secondary me-1 edit">수정</button>
                    <button class="btn btn-sm btn-outline-danger delete">삭제</button>
                </div>
            `;
            // 수정
            li.querySelector('.edit').onclick = function () {
                if (li.querySelector('.edit-input')) return;
                const input = document.createElement('input');
                input.type = 'text';
                input.className = 'form-control edit-input me-2';
                input.value = s.name;
                li.querySelector('.name').replaceWith(input);
                this.textContent = '저장';
                this.onclick = async function () {
                    const newName = input.value.trim();
                    if (!newName) return alert('값을 입력하세요.');
                    if (!confirm('소분류명 변경 시 모든 중분류 매핑에 적용됩니다.\n계속하시겠습니까?')) return;
                    try {
                        await api(`/api/category/small/${s.id}`, 'PUT', { name: newName });
                        await loadAll();
                    } catch (e) {
                        alert(e.message);
                    }
                }
            };
            // 삭제
            li.querySelector('.delete').onclick = async function () {
                if (!confirm('정말 삭제하시겠습니까?')) return;
                try {
                    await api(`/api/category/small/${s.id}`, 'DELETE');
                    await loadAll();
                } catch (e) {
                    alert(e.message);
                }
            };
            $smallList.appendChild(li);
        });
    }

    function renderSmallSelect() {
        $smallSelect.innerHTML = '<option value="">소분류 선택</option>';
        smallList.forEach(s => {
            $smallSelect.innerHTML += `<option value="${s.id}">${s.name}</option>`;
        });
    }

    // ------------------- 소분류-중분류 매핑 ------------------- //
    function renderMappingLargeSelect() {
        $mappingLargeSelect.innerHTML = '<option value="">대분류 선택</option>';
        largeList.forEach(l => {
            $mappingLargeSelect.innerHTML += `<option value="${l.id}">${l.name}</option>`;
        });
    }

    $mappingLargeSelect.onchange = function () {
        // 대분류 선택시 해당 하위 중분류만 표시
        const largeId = $mappingLargeSelect.value;
        $mappingMediumSelect.innerHTML = '';
        mediumList.filter(m => m.largeId == largeId)
            .forEach(m => {
                $mappingMediumSelect.innerHTML += `<option value="${m.id}">${m.name}</option>`;
            });
    };

    $smallSelect.onchange = renderMappingList;

    async function renderMappingList() {
        $mappingList.innerHTML = '';
        const smallId = $smallSelect.value;
        if (!smallId) return;
        try {
            const mappings = await api(`/api/category/mapping/small/${smallId}`);
            mappings.forEach(m => {
                const li = document.createElement('li');
                li.className = 'list-group-item d-flex justify-content-between align-items-center';
                const medium = mediumList.find(x => x.id == m.mediumId);
                li.innerHTML = `
                    <span>${medium ? medium.name : ''}</span>
                    <button class="btn btn-sm btn-outline-danger delete">매핑해제</button>
                `;
                li.querySelector('.delete').onclick = async function () {
                    if (!confirm('정말 매핑을 해제하시겠습니까?')) return;
                    try {
                        await api(`/api/category/mapping/${m.id}`, 'DELETE');
                        await renderMappingList();
                    } catch (e) {
                        alert(e.message);
                    }
                }
                $mappingList.appendChild(li);
            });
        } catch (e) {
            alert(e.message);
        }
    }

    $mappingBtn.onclick = async function () {
        const smallId = $smallSelect.value;
        const mediumIds = Array.from($mappingMediumSelect.selectedOptions).map(opt => opt.value);
        if (!smallId) return alert('소분류를 선택하세요.');
        if (!mediumIds.length) return alert('중분류를 선택하세요.');
        try {
            await api('/api/category/mapping', 'POST', {
                smallId, mediumIds
            });
            await renderMappingList();
            alert('매핑이 완료되었습니다.');
        } catch (e) {
            alert(e.message);
        }
    };
});
