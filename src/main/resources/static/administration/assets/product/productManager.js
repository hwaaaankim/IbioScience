document.addEventListener("DOMContentLoaded", function () {
    // 1. 카테고리 선택 트리(대→중→소)
    const largeList = document.getElementById('category-large-list');
    const mediumList = document.getElementById('category-medium-list');
    const smallList = document.getElementById('category-small-list');
    const selectedList = document.getElementById('selected-category-list');
    const applyCategoryBtn = document.getElementById('apply-category-btn');
    let selectedCategories = [];

    // 대분류 클릭시 중분류 ajax 로딩(예시)
    largeList.addEventListener('click', function(e){
        if(e.target.classList.contains('category-large-item')){
            const largeId = e.target.dataset.id;
            fetch(`/api/category/medium?largeId=${largeId}`)
                .then(res=>res.json())
                .then(list=>{
                    mediumList.innerHTML = '';
                    list.forEach(m=>{
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
    // 중분류 클릭시 소분류 ajax 로딩
    mediumList.addEventListener('click', function(e){
        if(e.target.classList.contains('category-medium-item')){
            const mediumId = e.target.dataset.id;
            fetch(`/api/category/small?mediumId=${mediumId}`)
                .then(res=>res.json())
                .then(list=>{
                    smallList.innerHTML = '';
                    list.forEach(s=>{
                        let li = document.createElement('li');
                        li.textContent = s.name;
                        li.className = 'list-group-item list-group-item-action category-small-item';
                        li.dataset.id = s.id;
                        smallList.appendChild(li);
                    });
                });
        }
    });
    // 소분류 클릭시 선택추가(중복방지)
    smallList.addEventListener('click', function(e){
        if(e.target.classList.contains('category-small-item')){
            const smallId = e.target.dataset.id;
            const text = e.target.textContent;
            if (!selectedCategories.some(sc=>sc.id==smallId)) {
                selectedCategories.push({id:smallId, text:text});
                renderSelectedCategories();
            }
        }
    });
    // 선택분류 렌더링 및 삭제
    function renderSelectedCategories() {
        selectedList.innerHTML = '';
        selectedCategories.forEach((c,idx)=>{
            let div = document.createElement('div');
            div.className = 'badge bg-primary text-white px-2 py-2 me-2 d-flex align-items-center';
            div.textContent = c.text;
            let x = document.createElement('span');
            x.textContent = '×';
            x.style.cursor = 'pointer';
            x.style.marginLeft = '10px';
            x.onclick = ()=>{selectedCategories.splice(idx,1); renderSelectedCategories();}
            div.appendChild(x);
            selectedList.appendChild(div);
        });
    }
    // 적용버튼 클릭: 아래 테이블로 렌더(예시)
    applyCategoryBtn.addEventListener('click',function(){
        // 실제 제품-분류 연결 ajax 또는 렌더링
        alert('분류 적용: '+selectedCategories.map(c=>c.text).join(', '));
        // TODO: 분류 연동 로직 구현
    });

    // 2. 공통표시옵션 중 에디터타입 CKEditor 동적 mount
    document.querySelectorAll('textarea[id^="editor_"]').forEach(t=>{
        CKEDITOR.replace(t.id, {height:100});
    });
    // 상세설명 에디터
    if(document.getElementById('product-manager-detail-html')){
        CKEDITOR.replace('product-manager-detail-html', {height:220});
    }

    // 3. 대표이미지 미리보기/삭제
    const mainInput = document.getElementById('product-manager-main-image');
    const mainPreview = document.getElementById('product-manager-main-image-preview');
    mainInput.addEventListener('change', function(){
        mainPreview.innerHTML = '';
        if(this.files.length>0){
            let file = this.files[0];
            let reader = new FileReader();
            reader.onload = e=>{
                let div = document.createElement('div');
                div.className = 'image-preview-thumb';
                div.style.backgroundImage = `url(${e.target.result})`;
                div.innerHTML = `<img src="${e.target.result}" style="width:100%;height:100%;object-fit:cover;">
                                <button class="btn-close btn-sm" style="position:absolute;top:0;right:0;" aria-label="Remove"></button>`;
                div.querySelector('button').onclick = ()=>{
                    mainInput.value = '';
                    mainPreview.innerHTML = '';
                }
                mainPreview.appendChild(div);
            }
            reader.readAsDataURL(file);
        }
    });

    // 4. 추가이미지 미리보기/삭제/순서변경
    const subInput = document.getElementById('product-manager-sub-image');
    const subPreview = document.getElementById('product-manager-sub-image-preview');
    let subFiles = [];
    subInput.addEventListener('change', function(){
        subFiles = Array.from(this.files);
        renderSubImagePreview();
    });
    function renderSubImagePreview() {
        subPreview.innerHTML = '';
        subFiles.forEach((file,idx)=>{
            let reader = new FileReader();
            reader.onload = e=>{
                let div = document.createElement('div');
                div.className = 'image-preview-thumb position-relative';
                div.innerHTML = `<img src="${e.target.result}" style="width:100%;height:100%;object-fit:cover;">
                                <button class="btn-close btn-sm" style="position:absolute;top:0;right:0;" aria-label="Remove"></button>`;
                div.querySelector('button').onclick = ()=>{
                    subFiles.splice(idx,1);
                    renderSubImagePreview();
                }
                subPreview.appendChild(div);
            }
            reader.readAsDataURL(file);
        });
    }
    // SortableJS 적용(추가이미지 순서변경)
    new Sortable(subPreview, {
        animation: 150,
        onEnd: function(evt) {
            const oldIndex = evt.oldIndex, newIndex = evt.newIndex;
            if (oldIndex!==newIndex) {
                const moved = subFiles.splice(oldIndex,1)[0];
                subFiles.splice(newIndex,0,moved);
                renderSubImagePreview();
            }
        }
    });

    // 5. 제품 등록 버튼 - 모든 값 수집 및 제출
    document.getElementById('submitProductBtn').addEventListener('click', function(e){
        e.preventDefault();
        // TODO: 각 입력값, 에디터 값, 이미지 파일, 선택분류 등 FormData로 만들어서 AJAX 제출 구현
        alert('제품 등록 로직 작성 필요');
    });
});
