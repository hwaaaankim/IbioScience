document.addEventListener('DOMContentLoaded', function() {
	// === 카테고리/제품 조회용 전역상태 ===
	let selectedLargeId = null;
	let selectedMediumId = null;
	let selectedSmallId = null;
	let giftProducts = []; // 현재 소분류의 제품목록 (최근 검색결과)
	let selectedGiftProduct = null; // 실제 등록된 증정상품

	let selectedCoupon = null; // 실제 선택된 쿠폰 1개

	// === 셀렉트 DOM ===
	const largeSelect = document.getElementById('giftCategoryLarge');
	const mediumSelect = document.getElementById('giftCategoryMedium');
	const smallSelect = document.getElementById('giftCategorySmall');
	const productListArea = document.getElementById('giftProductList');
	const selectedGiftArea = document.getElementById('selectedGiftProductList');

	const form = document.getElementById('product-promotion-form');
    const submitBtn = document.getElementById('productPromotionSubmitBtn');

    form.onsubmit = async function(e) {
        e.preventDefault();
        submitBtn.disabled = true;

        // 타입 결정
        const type = form.querySelector('input[name="type"]:checked')?.value;
        if (!type) {
            alert('프로모션 타입을 선택해주세요.');
            submitBtn.disabled = false;
            return;
        }

        // 공통값
        const name = form.elements['name'].value.trim();
        const status = form.elements['status'].value;
        const term = form.elements['term'].value;
        const startDate = form.elements['startDate'].value;
        const endDate = form.elements['endDate'].value;
        const iconFile = form.elements['iconFile'].files[0] || null;

        // 타입별 데이터 분기
        let requestData = {
            name,
            status,
            term,
            type,
            startDate,
            endDate
        };
        let isValid = true;
        let errorMsg = "";

        // 타입별 값 수집 및 검증
        if (type === "DISCOUNT") {
            const discountPercent = form.elements['discountPercent'].value;
            if (!discountPercent || isNaN(discountPercent) || discountPercent < 0 || discountPercent > 100) {
                isValid = false;
                errorMsg = "할인율을 0~100 사이로 입력해주세요.";
            } else {
                requestData.discountPercent = discountPercent;
            }
        } else if (type === "GIFT") {
            // selectedGiftProduct는 이미 전역에 있음
            if (!window.selectedGiftProduct) {
                isValid = false;
                errorMsg = "증정 상품을 선택해주세요.";
            } else {
                requestData.giftProductId = window.selectedGiftProduct.id;
            }
        } else if (type === "COUPON") {
            if (!window.selectedCoupon) {
                isValid = false;
                errorMsg = "쿠폰을 선택해주세요.";
            } else {
                requestData.couponId = window.selectedCoupon.id;
            }
        } else if (type === "ONE_PLUS_ONE") {
            // 별도 데이터 없음
        } else {
            isValid = false;
            errorMsg = "잘못된 프로모션 타입입니다.";
        }

        if (!isValid) {
            alert(errorMsg);
            submitBtn.disabled = false;
            return;
        }

        // 폼데이터 생성 (파일 포함)
        const fd = new FormData();
        Object.entries(requestData).forEach(([k, v]) => fd.append(k, v));
        if (iconFile) fd.append("iconFile", iconFile);

        try {
            const res = await fetch('/api/promotion', {
                method: 'POST',
                body: fd
            });
            if (!res.ok) throw new Error('저장 실패');
            alert('저장되었습니다.');
            location.reload();
        } catch (err) {
            alert('저장 중 오류 발생: ' + err.message);
            submitBtn.disabled = false;
        }
    };

	// ========== 타입별 패널 show/hide ==========
	document.querySelectorAll('input[name="type"]').forEach(radio => {
		radio.addEventListener('change', function() {
			document.querySelectorAll('.product-promotion-type-panel').forEach(panel => {
				panel.style.display = (panel.getAttribute('data-type') === this.value) ? '' : 'none';
			});
		});
	});

	// ========== 1. 대분류 불러오기 ==========
	function fetchLargeCategories() {
		fetch('/api/category/list-large')
			.then(res => {
				if (!res.ok) throw new Error("대분류 API 오류");
				return res.json();
			})
			.then(list => {
				if (!Array.isArray(list)) {
					console.warn('대분류 응답값이 배열이 아님', list);
					list = [];
				}
				largeSelect.innerHTML = '<option value="">대분류</option>';
				list.forEach(l => {
					const opt = document.createElement('option');
					opt.value = l.id;
					opt.textContent = l.name + ' (' + l.mediumCount + ')';
					largeSelect.appendChild(opt);
				});
				// 선택초기화
				mediumSelect.innerHTML = '<option value="">중분류</option>';
				smallSelect.innerHTML = '<option value="">소분류</option>';
				productListArea.innerHTML = '';
			})
			.catch(err => {
				largeSelect.innerHTML = '<option value="">대분류 불러오기 실패</option>';
				console.error('대분류 API 오류', err);
			});
	}

	// ========== 2. 중분류 불러오기 ==========
	function fetchMediumCategories(largeId) {
		if (!largeId) {
			mediumSelect.innerHTML = '<option value="">중분류</option>';
			smallSelect.innerHTML = '<option value="">소분류</option>';
			productListArea.innerHTML = '';
			return;
		}
		fetch('/api/category/list-medium?largeId=' + largeId)
			.then(res => {
				if (!res.ok) throw new Error("중분류 API 오류");
				return res.json();
			})
			.then(list => {
				if (!Array.isArray(list)) {
					console.warn('중분류 응답값이 배열이 아님', list);
					list = [];
				}
				mediumSelect.innerHTML = '<option value="">중분류</option>';
				list.forEach(m => {
					const opt = document.createElement('option');
					opt.value = m.id;
					opt.textContent = m.name + ' (' + m.smallCount + ')';
					mediumSelect.appendChild(opt);
				});
				// 선택초기화
				smallSelect.innerHTML = '<option value="">소분류</option>';
				productListArea.innerHTML = '';
			})
			.catch(err => {
				mediumSelect.innerHTML = '<option value="">중분류 불러오기 실패</option>';
				console.error('중분류 API 오류', err);
			});
	}

	// ========== 3. 소분류 불러오기 ==========
	function fetchSmallCategories(mediumId) {
		if (!mediumId) {
			smallSelect.innerHTML = '<option value="">소분류</option>';
			productListArea.innerHTML = '';
			return;
		}
		fetch('/api/category/list-small-with-product-count?mediumId=' + mediumId)
			.then(res => {
				if (!res.ok) throw new Error("소분류 API 오류");
				return res.json();
			})
			.then(list => {
				if (!Array.isArray(list)) {
					console.warn('소분류 응답값이 배열이 아님', list);
					list = [];
				}
				smallSelect.innerHTML = '<option value="">소분류</option>';
				list.forEach(s => {
					const opt = document.createElement('option');
					opt.value = s.id;
					opt.textContent = s.name + ' (' + (s.productCount || 0) + ')';
					smallSelect.appendChild(opt);
				});
				productListArea.innerHTML = '';
			})
			.catch(err => {
				smallSelect.innerHTML = '<option value="">소분류 불러오기 실패</option>';
				console.error('소분류 API 오류', err);
			});
	}

	// ========== 4. 소분류 선택 → 제품 목록 불러오기 ==========
	function fetchProductList(smallId) {
		if (!smallId) {
			productListArea.innerHTML = '';
			giftProducts = [];
			return;
		}
		fetch('/api/product/list-simple?smallId=' + smallId)
			.then(res => {
				if (!res.ok) throw new Error("제품 API 오류");
				return res.json();
			})
			.then(list => {
				if (!Array.isArray(list)) {
					console.warn('제품 응답값이 배열이 아님', list);
					list = [];
				}
				giftProducts = list; // [{id, code, name}]
				renderGiftProductList();
			})
			.catch(err => {
				productListArea.innerHTML = '<div class="text-danger small">제품목록 불러오기 실패</div>';
				giftProducts = [];
				console.error('제품목록 API 오류', err);
			});
	}

	// ========== 5. 제품 리스트 렌더링 ==========
	function renderGiftProductList() {
		productListArea.innerHTML = '';
		if (!giftProducts || giftProducts.length === 0) {
			productListArea.innerHTML = '<div class="text-muted small px-2 py-1">등록된 제품이 없습니다.</div>';
			return;
		}

		// 선택된 카테고리명 가져오기 (괄호 제거)
		const largeCategoryName = largeSelect.options[largeSelect.selectedIndex]?.textContent?.split(' (')[0] || '';
		const mediumCategoryName = mediumSelect.options[mediumSelect.selectedIndex]?.textContent?.split(' (')[0] || '';
		const smallCategoryName = smallSelect.options[smallSelect.selectedIndex]?.textContent?.split(' (')[0] || '';

		giftProducts.forEach(product => {
			const item = document.createElement('div');
			item.className = "product-promotion-list-item d-flex align-items-center justify-content-between mb-2";
			item.innerHTML = `
				<span>
					<strong>${product.name}</strong>
					<small class="text-muted ms-2">
						${largeCategoryName} > ${mediumCategoryName} > ${smallCategoryName}
					</small>
				</span>
				<button type="button" class="btn btn-sm btn-outline-primary product-promotion-register-btn"
					${selectedGiftProduct && selectedGiftProduct.id === product.id ? 'disabled' : ''}
					data-id="${product.id}">등록</button>
			`;
			item.querySelector('.product-promotion-register-btn').onclick = () => {
				selectedGiftProduct = product;
				renderGiftProductList();
				renderSelectedGiftProduct();
			};
			productListArea.appendChild(item);
		});
	}

	// ========== 6. 선택된 증정상품 렌더링 ==========
	function renderSelectedGiftProduct() {
		selectedGiftArea.innerHTML = "";
		if (selectedGiftProduct) {
			selectedGiftArea.innerHTML = `
				<span class="product-promotion-selected-badge">
					${selectedGiftProduct.name}
					<span class="product-promotion-selected-remove text-warning ms-2" style="cursor:pointer;">&times;</span>
				</span>
			`;
			selectedGiftArea.querySelector('.product-promotion-selected-remove').onclick = () => {
				selectedGiftProduct = null;
				renderGiftProductList();
				renderSelectedGiftProduct();
			};
		}
	}

	// ========== 7. 카테고리 선택 이벤트 바인딩 ==========
	largeSelect.addEventListener('change', function() {
		selectedLargeId = this.value;
		selectedMediumId = null;
		selectedSmallId = null;
		selectedGiftProduct = null;
		fetchMediumCategories(selectedLargeId);
		renderSelectedGiftProduct();
	});

	mediumSelect.addEventListener('change', function() {
		selectedMediumId = this.value;
		selectedSmallId = null;
		selectedGiftProduct = null;
		fetchSmallCategories(selectedMediumId);
		renderSelectedGiftProduct();
	});

	smallSelect.addEventListener('change', function() {
		selectedSmallId = this.value;
		selectedGiftProduct = null;
		fetchProductList(selectedSmallId);
		renderSelectedGiftProduct();
	});

	// ========== 최초 로드시 대분류부터 셋업 ==========
	fetchLargeCategories();
	renderSelectedGiftProduct();

	// [쿠폰 검색 및 그리기]
	function fetchCouponsAndRender() {
		const status = document.getElementById('couponStatus').value;
		const name = document.getElementById('couponKeyword').value.trim();
		const startDate = document.getElementById('couponStartDate').value;
		const endDate = document.getElementById('couponEndDate').value;
		const params = new URLSearchParams();
		if (status) params.append('status', status);
		if (name) params.append('name', name);
		if (startDate) params.append('startDate', startDate);
		if (endDate) params.append('endDate', endDate);

		fetch('/api/coupon/search?' + params.toString())
			.then(res => {
				if (!res.ok) throw new Error("쿠폰목록 조회 실패");
				return res.json();
			})
			.then(list => {
				renderCouponList(list);
			})
			.catch(err => {
				document.getElementById('couponList').innerHTML = '<div class="text-danger small">쿠폰목록 불러오기 실패</div>';
				console.error('쿠폰목록 API 오류', err);
			});
	}

	// [쿠폰 리스트 렌더링]
	function renderCouponList(couponList) {
		const listArea = document.getElementById('couponList');
		listArea.innerHTML = "";
		if (!Array.isArray(couponList) || couponList.length === 0) {
			listArea.innerHTML = '<div class="text-muted small"> # 검색결과가 없습니다.</div>';
			return;
		}
		couponList.forEach(coupon => {
			const item = document.createElement('div');
			item.className = "product-promotion-list-item d-flex align-items-center justify-content-between mb-2";
			item.innerHTML = `
				<span>
					<strong>${coupon.couponName}</strong>
					<small class="text-muted ms-2">${coupon.startDate} ~ ${coupon.endDate}</small>
					<span class="badge bg-light text-dark ms-2">${coupon.status}</span>
				</span>
				<button type="button" class="btn btn-sm btn-outline-primary product-promotion-register-btn"
					${selectedCoupon && selectedCoupon.id === coupon.id ? 'disabled' : ''}
					data-id="${coupon.id}">등록</button>
			`;
			item.querySelector('.product-promotion-register-btn').onclick = () => {
				selectedCoupon = coupon;
				renderCouponList(couponList);
				renderSelectedCoupon();
			};
			listArea.appendChild(item);
		});
	}

	// [선택된 쿠폰 렌더링 + 삭제]
	function renderSelectedCoupon() {
		const area = document.getElementById('selectedCouponList');
		area.innerHTML = "";
		if (selectedCoupon) {
			area.innerHTML = `
				<span class="product-promotion-selected-badge">
					${selectedCoupon.couponName}
					<span class="product-promotion-selected-remove text-warning ms-2" style="cursor:pointer;">&times;</span>
				</span>
			`;
			area.querySelector('.product-promotion-selected-remove').onclick = () => {
				selectedCoupon = null;
				fetchCouponsAndRender(); // 다시 검색결과(등록버튼) 활성화
				renderSelectedCoupon();
			};
		}
	}

	// [쿠폰 검색버튼 이벤트]
	document.getElementById('couponSearchBtn').onclick = fetchCouponsAndRender;

	// [쿠폰 탭 최초 진입시 바로 검색]
	fetchCouponsAndRender();
	renderSelectedCoupon();
});
