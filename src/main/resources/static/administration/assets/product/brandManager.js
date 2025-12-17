document.addEventListener("DOMContentLoaded", function() {
    let currentPage = 0;
    let currentKeyword = '';

    // ========== [브랜드 등록 미리보기] ==========
    const imageInput = document.getElementById('brand-manager-image');
    const imagePreview = document.getElementById('brand-manager-image-preview');
    if (imageInput && imagePreview) {
        imageInput.addEventListener('change', function() {
            if (imageInput.files && imageInput.files[0]) {
                const file = imageInput.files[0];
                const reader = new FileReader();
                reader.onload = function(e) {
                    imagePreview.src = e.target.result;
                }
                reader.readAsDataURL(file);
            } else {
                imagePreview.src = "/assets/img/no-image.png";
            }
        });
    }

    // 등록 후 폼 리셋 시 미리보기도 초기화
    const registerForm = document.getElementById('brand-manager-register-form');
    if (registerForm && imagePreview) {
        registerForm.addEventListener('reset', function() {
            imagePreview.src = "/assets/img/no-image.png";
        });
    }

    // ========== [브랜드 등록] ==========
    if (registerForm) {
        registerForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            const formData = new FormData(registerForm);
            const res = await fetch('/api/brand/insert', {
                method: 'POST',
                body: formData
            });
            if (res.ok) {
                alert('등록 완료');
                registerForm.reset();
                currentPage = 0;
                loadBrandList();
            } else {
                alert('등록 실패');
            }
        });
    }

    // ========== [검색 필터] ==========
    window.brandManagerSearch = function() {
        currentKeyword = document.getElementById('brand-manager-search').value.trim();
        currentPage = 0;
        loadBrandList();
    };

    // ========== [브랜드 리스트 로딩] ==========
    loadBrandList();

    function loadBrandList() {
        fetch(`/api/brand/list?page=${currentPage}&keyword=${encodeURIComponent(currentKeyword)}`)
            .then(res => res.json())
            .then(data => {
                renderBrandList(data.content);
                renderPagination(data);
            });
    }

    function renderBrandList(brandList) {
        const container = document.getElementById('brand-manager-list');
        container.innerHTML = '';

        if (brandList.length === 0) {
            container.innerHTML = '<div class="text-center text-muted">브랜드가 없습니다.</div>';
            return;
        }

        brandList.forEach(brand => {
            const card = document.createElement('div');
            card.className = 'col-lg-3 col-md-4 col-6 brand-manager-card-item';

            // 기본 이미지 처리
            const brandImg = brand.imageRoad ? brand.imageRoad : '/assets/img/no-image.png';

            card.innerHTML = `
                <div class="card h-100">
                    <div class="card-body d-flex flex-column align-items-center">
                        <img src="${brandImg}" alt="브랜드 이미지" style="width:80px;height:80px;object-fit:contain;" class="mb-2 border rounded brand-manager-card-img" id="brand-manager-img-${brand.id}">
                        <input type="text" class="form-control mb-2 brand-manager-edit-name" value="${brand.name}" data-id="${brand.id}">
                        <input type="file" accept="image/*" class="form-control mb-2 brand-manager-edit-image" data-id="${brand.id}">
                        <div class="d-flex gap-2 mt-2 w-100">
                            <button type="button" class="w-100 btn btn-outline-primary btn-sm brand-manager-save-btn" data-id="${brand.id}" disabled>수정</button>
                            <button type="button" class="w-100 btn btn-outline-warning btn-sm brand-manager-image-delete-btn" data-id="${brand.id}">이미지만 삭제</button>
                            <button type="button" class="w-100 btn btn-outline-danger btn-sm brand-manager-delete-btn" data-id="${brand.id}">브랜드 삭제</button>
                        </div>
                    </div>
                </div>
            `;
            container.appendChild(card);
        });

        // 카드 내 input 변화 감지시 수정버튼 활성화 & 미리보기
        document.querySelectorAll('.brand-manager-card-item').forEach(card => {
            const id = card.querySelector('.brand-manager-save-btn').dataset.id;
            const nameInput = card.querySelector('.brand-manager-edit-name');
            const imageInput = card.querySelector('.brand-manager-edit-image');
            const saveBtn = card.querySelector('.brand-manager-save-btn');
            const imgTag = card.querySelector('.brand-manager-card-img');
            const originImg = imgTag.src;

            let originName = nameInput.value;
            let nameChanged = false;
            let imageChanged = false;

            nameInput.addEventListener('input', function() {
                nameChanged = (nameInput.value !== originName);
                saveBtn.disabled = !(nameChanged || imageChanged);
            });
            imageInput.addEventListener('change', function() {
                if (imageInput.files.length > 0) {
                    const file = imageInput.files[0];
                    const reader = new FileReader();
                    reader.onload = function(e) {
                        imgTag.src = e.target.result;
                    }
                    reader.readAsDataURL(file);
                    imageChanged = true;
                } else {
                    imgTag.src = originImg;
                    imageChanged = false;
                }
                saveBtn.disabled = !(nameChanged || imageChanged);
            });
        });

        // 수정 버튼
        document.querySelectorAll('.brand-manager-save-btn').forEach(btn => {
            btn.addEventListener('click', async function() {
                const id = this.dataset.id;
                const card = this.closest('.brand-manager-card-item');
                const nameInput = card.querySelector('.brand-manager-edit-name');
                const imageInput = card.querySelector('.brand-manager-edit-image');
                const formData = new FormData();
                formData.append('id', id);
                formData.append('name', nameInput.value);
                if (imageInput.files.length > 0) {
                    formData.append('image', imageInput.files[0]);
                }
                const res = await fetch(`/api/brand/update`, {
                    method: 'POST',
                    body: formData
                });
                if (res.ok) {
                    alert('수정 완료');
                    loadBrandList();
                } else {
                    alert('수정 실패');
                }
            });
        });

        // 브랜드 삭제
		document.querySelectorAll('.brand-manager-delete-btn').forEach(btn => {
		    btn.addEventListener('click', async function() {
		        const id = this.dataset.id;
		        if (!confirm('정말 브랜드를 삭제하시겠습니까? (이미지도 삭제됩니다)')) return;
		
		        const res = await fetch(`/api/brand/delete/${id}`, { method: 'DELETE' });
		
		        if (res.ok) {
		            alert('삭제 완료');
		            loadBrandList();
		            return;
		        }
		
		        // 상태별 사용자 메시지
		        if (res.status === 409) {
		            const msg = await res.text();
		            alert(msg || '해당 브랜드는 등록된 제품과 연결되어 있어 삭제할 수 없습니다.');
		        } else if (res.status === 404) {
		            alert('브랜드를 찾을 수 없습니다.');
		        } else {
		            alert('삭제 실패');
		        }
		    });
		});

        // 이미지만 삭제
        document.querySelectorAll('.brand-manager-image-delete-btn').forEach(btn => {
            btn.addEventListener('click', async function() {
                const id = this.dataset.id;
                if (!confirm('브랜드 이미지만 삭제하시겠습니까?')) return;
                const res = await fetch(`/api/brand/image/delete/${id}`, {
                    method: 'POST'
                });
                if (res.ok) {
                    alert('이미지 삭제 완료');
                    loadBrandList();
                } else {
                    alert('이미지 삭제 실패');
                }
            });
        });
    }

    function renderPagination(data) {
        const pagination = document.getElementById('brand-manager-pagination');
        pagination.innerHTML = '';
        const totalPages = data.totalPages;
        const current = data.number;

        // if (totalPages <= 1) return;

        for (let i = 0; i < totalPages; i++) {
            const li = document.createElement('li');
            li.className = `page-item ${i === current ? 'active' : ''}`;
            const btn = document.createElement('button');
            btn.className = 'page-link';
            btn.textContent = i + 1;
            btn.addEventListener('click', () => {
                currentPage = i;
                loadBrandList();
            });
            li.appendChild(btn);
            pagination.appendChild(li);
        }
    }
});
