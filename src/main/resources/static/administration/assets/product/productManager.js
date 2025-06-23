document.addEventListener("DOMContentLoaded", function () {
    // === 1. 카테고리 선택 트리 ===
    const largeList = document.getElementById('category-large-list');
    const mediumList = document.getElementById('category-medium-list');
    const smallList = document.getElementById('category-small-list');
    const selectedList = document.getElementById('selected-category-list');
    const applyCategoryBtn = document.getElementById('apply-category-btn');
    let selectedCategories = [];

    fetch('/api/category/list-large')
        .then(res => res.json())
        .then(list => {
            largeList.innerHTML = '';
            list.forEach(large => {
                let li = document.createElement('li');
                li.textContent = large.name;
                li.className = 'list-group-item list-group-item-action category-large-item';
                li.dataset.id = large.id;
                largeList.appendChild(li);
            });
        });

    largeList.addEventListener('click', function (e) {
        if (e.target.classList.contains('category-large-item')) {
            const largeId = e.target.dataset.id;
            fetch(`/api/category/list-medium?largeId=${largeId}`)
                .then(res => res.json())
                .then(list => {
                    mediumList.innerHTML = '';
                    list.forEach(m => {
                        let li = document.createElement('li');
                        li.textContent = m.name;
                        li.className = 'list-group-item list-group-item-action category-medium-item';
                        li.dataset.id = m.id;
                        mediumList.appendChild(li);
                    });
                    smallList.innerHTML = '';
                });
        }
    });

    mediumList.addEventListener('click', function (e) {
        if (e.target.classList.contains('category-medium-item')) {
            const mediumId = e.target.dataset.id;
            fetch(`/api/category/list-small?mediumId=${mediumId}`)
                .then(res => res.json())
                .then(list => {
                    smallList.innerHTML = '';
                    list.forEach(s => {
                        let li = document.createElement('li');
                        li.textContent = s.name;
                        li.className = 'list-group-item list-group-item-action category-small-item';
                        li.dataset.id = s.id;
                        smallList.appendChild(li);
                    });
                });
        }
    });

    smallList.addEventListener('click', function (e) {
        if (e.target.classList.contains('category-small-item')) {
            const smallId = e.target.dataset.id;
            const text = e.target.textContent;
            if (!selectedCategories.some(sc => sc.id == smallId)) {
                selectedCategories.push({ id: smallId, text: text });
                renderSelectedCategories();
            }
        }
    });

    function renderSelectedCategories() {
        selectedList.innerHTML = '';
        selectedCategories.forEach((c, idx) => {
            let div = document.createElement('div');
            div.className = 'badge bg-primary text-white px-2 py-2 me-2 d-flex align-items-center';
            div.textContent = c.text;
            let x = document.createElement('span');
            x.textContent = '×';
            x.style.cursor = 'pointer';
            x.style.marginLeft = '10px';
            x.onclick = () => { selectedCategories.splice(idx, 1); renderSelectedCategories(); }
            div.appendChild(x);
            selectedList.appendChild(div);
        });
    }

    applyCategoryBtn.addEventListener('click', function () {
        alert('분류 적용: ' + selectedCategories.map(c => c.text).join(', '));
    });

    // ===== 2. 공통 표시옵션(질문) 동적 랜더링 및 CKEditor mount =====
    let ckeInstances = {};

    function makeQuestionInput(option) {
        const requiredAttr = option.required ? 'required' : '';
        const editorId = option.type === 'CKEDITOR' ? `editor-question-${option.id}` : '';
        let inputHtml = '';
        switch (option.type) {
            case 'INPUT':
                inputHtml = `<input type="text" class="form-control form-control-sm" name="question_${option.id}" placeholder="${option.placeholder || ''}" ${requiredAttr}>`;
                break;
            case 'TEXTAREA':
                inputHtml = `<textarea class="form-control form-control-sm" name="question_${option.id}" rows="2" placeholder="${option.placeholder || ''}" ${requiredAttr}></textarea>`;
                break;
            case 'SELECT':
                inputHtml = `<select class="form-select form-select-sm" name="question_${option.id}" ${requiredAttr}>`
                    + (Array.isArray(option.options) && option.options.length > 0
                        ? option.options.map(opt => {
                            // object 객체면 label/value로 처리
                            if (typeof opt === 'object' && opt !== null) {
                                if (opt.value !== undefined && opt.label !== undefined) {
                                    return `<option value="${opt.value}">${opt.label}</option>`;
                                } else if (Object.keys(opt).length === 1) {
                                    let key = Object.keys(opt)[0];
                                    return `<option value="${key}">${opt[key]}</option>`;
                                } else {
                                    return `<option disabled>선택지 오류</option>`;
                                }
                            } else {
                                return `<option value="${opt}">${opt}</option>`;
                            }
                        }).join('')
                        : '<option disabled>선택지 없음</option>')
                    + `</select>`;
                break;
            case 'FILE':
                inputHtml = `<input type="file" class="form-control form-control-sm" name="question_${option.id}" ${requiredAttr}>`;
                break;
            case 'CKEDITOR':
                inputHtml = `<textarea class="form-control" name="question_${option.id}" id="${editorId}" rows="3" ${requiredAttr}></textarea>`;
                break;
            default:
                inputHtml = `<input type="text" class="form-control form-control-sm" name="question_${option.id}" placeholder="지원되지 않는 타입" disabled>`;
        }
        return inputHtml;
    }

    fetch('/api/display-questions/list-common')
        .then(res => res.json())
        .then(list => {
            const container = document.getElementById('product-manager-display-options');
            container.innerHTML = '';
            list.forEach(option => {
                const div = document.createElement('div');
                div.className = 'row mb-2 align-items-end';
                div.innerHTML = `
                    <div class="col-md-3">
                        <label class="form-label mb-1">${option.label ?? option.name}${option.required ? ' <span class="text-danger">*</span>' : ''}</label>
                        ${makeQuestionInput(option)}
                    </div>
                `;
                container.appendChild(div);
            });
            // CKEDITOR 타입 mount
            setTimeout(() => {
                list.filter(opt => opt.type === 'CKEDITOR').forEach(option => {
                    const tId = `editor-question-${option.id}`;
                    const textarea = document.getElementById(tId);
                    if (textarea && !ckeInstances[tId] && window.ClassicEditor) {
                        window.ClassicEditor.create(textarea)
                            .then(editor => { ckeInstances[tId] = editor; })
                            .catch(err => console.error('CKEditor5 생성 오류:', err));
                    }
                });
            }, 100); // DOM 렌더링 후 mount
        });

    // === 3. 상세설명(이미지/HTML) 에디터 mount (id: editor-desc) ===
    let detailEditor = null;
    (function () {
        const desc = document.getElementById('editor-desc');
        if (desc && window.ClassicEditor) {
            window.ClassicEditor.create(desc)
                .then(editor => { detailEditor = editor; })
                .catch(err => console.error('CKEditor5 생성 오류:', err));
        }
    })();

    // ===== 4. 대표/추가 이미지 미리보기/삭제/순서 =====
    const mainInput = document.getElementById('product-manager-main-image');
    const mainPreview = document.getElementById('product-manager-main-image-preview');
    mainInput.addEventListener('change', function () {
        mainPreview.innerHTML = '';
        if (this.files.length > 0) {
            let file = this.files[0];
            let reader = new FileReader();
            reader.onload = e => {
                let div = document.createElement('div');
                div.className = 'image-preview-thumb position-relative';
                div.innerHTML = `
                    <img src="${e.target.result}" style="width:100%;height:100%;object-fit:cover;">
                    <button class="btn-close btn-sm" style="position:absolute;top:0;right:0;z-index:2;" aria-label="Remove"></button>`;
                div.querySelector('button').onclick = () => {
                    mainInput.value = '';
                    mainPreview.innerHTML = '';
                }
                mainPreview.appendChild(div);
            }
            reader.readAsDataURL(file);
        }
    });

    // 추가 이미지 (여러장, 미리보기, 삭제, Sortable)
    const subInput = document.getElementById('product-manager-sub-image');
    const subPreview = document.getElementById('product-manager-sub-image-preview');
    let subFiles = [];
    subInput.addEventListener('change', function () {
        subFiles = Array.from(this.files);
        renderSubImagePreview();
    });
    function renderSubImagePreview() {
        subPreview.innerHTML = '';
        subFiles.forEach((file, idx) => {
            let reader = new FileReader();
            reader.onload = e => {
                let div = document.createElement('div');
                div.className = 'image-preview-thumb position-relative';
                div.setAttribute('draggable', 'true');
                div.style.width = '100px'; div.style.height = '100px'; div.style.marginRight = '8px';
                div.innerHTML = `
                    <img src="${e.target.result}" style="width:100%;height:100%;object-fit:cover;">
                    <button class="btn-close btn-sm" style="position:absolute;top:0;right:0;z-index:2;" aria-label="Remove"></button>`;
                div.querySelector('button').onclick = () => {
                    subFiles.splice(idx, 1);
                    renderSubImagePreview();
                }
                subPreview.appendChild(div);
            }
            reader.readAsDataURL(file);
        });
    }
    new Sortable(subPreview, {
        animation: 150,
        onEnd: function (evt) {
            const oldIndex = evt.oldIndex, newIndex = evt.newIndex;
            if (oldIndex !== newIndex) {
                const moved = subFiles.splice(oldIndex, 1)[0];
                subFiles.splice(newIndex, 0, moved);
                renderSubImagePreview();
            }
        }
    });

    // ===== 5. 제품 등록 버튼 - FormData로 수집 및 제출 =====
    document.getElementById('submitProductBtn').addEventListener('click', function (e) {
        e.preventDefault();
        const formData = new FormData();
        selectedCategories.forEach(cat => formData.append('categorySmallIds', cat.id));
        document.querySelectorAll('#product-manager-display-options [name]').forEach(el => {
            if (el.type === 'file') {
                if (el.files[0]) formData.append(el.name, el.files[0]);
            } else if (el.type === 'textarea' && el.classList.contains('ck-editor__editable')) {
                // CKEditor는 별도 처리
            } else {
                formData.append(el.name, el.value);
            }
        });
        // 공통질문 에디터
        Object.entries(ckeInstances).forEach(([tid, editor]) => {
            formData.append(tid, editor.getData());
        });
        // 대표이미지
        if (mainInput.files[0]) {
            formData.append('mainImage', mainInput.files[0]);
        }
        // 추가이미지
        subFiles.forEach((f, idx) => formData.append('subImages', f));
        formData.append('productName', document.getElementById('productName').value);
        formData.append('productCode', document.getElementById('productCode').value);
        formData.append('displayStatus', document.querySelector('input[name="displayStatus"]:checked').value);
        formData.append('saleStatus', document.querySelector('input[name="saleStatus"]:checked').value);

        // 상세설명 에디터
        if (detailEditor) {
            formData.append('detailHtml', detailEditor.getData());
        }
        fetch('/api/product', {
            method: 'POST',
            body: formData
        })
            .then(res => {
                if (res.ok) return res.json();
                throw new Error('등록실패');
            })
            .then(json => {
                alert('제품 등록 성공');
            })
            .catch(err => alert('등록 실패: ' + err.message));
    });
});
