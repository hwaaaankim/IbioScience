document.addEventListener('DOMContentLoaded', function() {
	// === 카테고리/제품 조회용 전역상태 ===
	let selectedLargeId = null;
	let selectedMediumId = null;
	let selectedSmallId = null;
	let giftProducts = []; // 현재 소분류의 제품목록 (최근 검색결과)
	let selectedGiftProduct = null; // 실제 등록된 증정상품(1개 제한)

	let selectedCoupon = null; // 실제 선택된 쿠폰(1개 제한)

	// === DOM 캐시 ===
	const form = document.getElementById('product-promotion-form');
	const submitBtn = document.getElementById('productPromotionSubmitBtn');
	const termSelect = form.elements['term'];
	const startDateInput = form.elements['startDate'];
	const endDateInput = form.elements['endDate'];

	// 증정탭
	const largeSelect = document.getElementById('giftCategoryLarge');
	const mediumSelect = document.getElementById('giftCategoryMedium');
	const smallSelect = document.getElementById('giftCategorySmall');
	const productListArea = document.getElementById('giftProductList');
	const selectedGiftArea = document.getElementById('selectedGiftProductList');

	// 쿠폰탭
	const couponStatusSel = document.getElementById('couponStatus'); // disabled + ISSUED selected
	const couponKeyword = document.getElementById('couponKeyword');
	const couponStartDate = document.getElementById('couponStartDate');
	const couponEndDate = document.getElementById('couponEndDate');
	const couponSearchBtn = document.getElementById('couponSearchBtn');
	const couponListArea = document.getElementById('couponList');
	const selectedCouponArea = document.getElementById('selectedCouponList');

	// ========== 공통: 기간정책에 따른 날짜 on/off ==========
	function updateDateInputsByTerm() {
		const term = termSelect.value; // PERIOD | ALWAYS
		if (term === 'ALWAYS') {
			// 의미 없으므로 disabled + 값 초기화
			startDateInput.value = '';
			endDateInput.value = '';
			startDateInput.disabled = true;
			endDateInput.disabled = true;
			startDateInput.removeAttribute('required');
			endDateInput.removeAttribute('required');
		} else {
			// 기간한정
			startDateInput.disabled = false;
			endDateInput.disabled = false;
			startDateInput.setAttribute('required', 'required');
			endDateInput.setAttribute('required', 'required');
		}
	}
	termSelect.addEventListener('change', updateDateInputsByTerm);
	updateDateInputsByTerm(); // 최초 호출

	// ========== 폼 제출 ==========
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
		let requestData = { name, status, term, type };
		let isValid = true;
		let errorMsg = "";

		// term=PERIOD 일 때만 날짜 필수 검증
		if (term === 'PERIOD') {
			if (!startDate || !endDate) {
				isValid = false;
				errorMsg = "기간한정일 때 시작일/종료일은 필수입니다.";
			} else {
				requestData.startDate = startDate;
				requestData.endDate = endDate;
			}
		}

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
			if (!selectedGiftProduct) {
				isValid = false;
				errorMsg = "증정 상품을 선택해주세요.";
			} else {
				requestData.giftProductId = selectedGiftProduct.id;
			}
		} else if (type === "COUPON") {
			if (!selectedCoupon) {
				isValid = false;
				errorMsg = "쿠폰을 선택해주세요.";
			} else {
				requestData.couponId = selectedCoupon.id;
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
		Object.entries(requestData).forEach(([k, v]) => {
			if (v !== undefined && v !== null) fd.append(k, v);
		});
		if (iconFile) fd.append("iconFile", iconFile);

		try {
			const res = await fetch('/api/promotion', { method: 'POST', body: fd });
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
				panel.style.display = (panel.getAttribute('data-type') === radio.value) ? '' : 'none';
			});
		});
	});

	// =========================
	// [GIFT] 증정 상품 선택 로직
	// =========================
	function setGiftFiltersEnabled(enabled) {
		[largeSelect, mediumSelect, smallSelect].forEach(el => el.disabled = !enabled);
		// 제품 리스트 영역 버튼도 전체 disable
		productListArea.querySelectorAll('button.product-promotion-register-btn').forEach(btn => {
			btn.disabled = !enabled || (selectedGiftProduct && String(btn.dataset.id) === String(selectedGiftProduct.id));
		});
	}

	function resetGiftFilters() {
		selectedLargeId = null;
		selectedMediumId = null;
		selectedSmallId = null;
		largeSelect.value = '';
		mediumSelect.innerHTML = '<option value="">중분류</option>';
		smallSelect.innerHTML = '<option value="">소분류</option>';
		productListArea.innerHTML = '';
	}

	// 1) 대분류 불러오기
	function fetchLargeCategories() {
		fetch('/api/category/list-large')
			.then(res => {
				if (!res.ok) throw new Error("대분류 API 오류");
				return res.json();
			})
			.then(list => {
				if (!Array.isArray(list)) list = [];
				largeSelect.innerHTML = '<option value="">대분류</option>';
				list.forEach(l => {
					const opt = document.createElement('option');
					opt.value = l.id;
					opt.textContent = l.name + ' (' + (l.mediumCount ?? 0) + ')';
					largeSelect.appendChild(opt);
				});
				mediumSelect.innerHTML = '<option value="">중분류</option>';
				smallSelect.innerHTML = '<option value="">소분류</option>';
				productListArea.innerHTML = '';
			})
			.catch(err => {
				console.error('대분류 API 오류', err);
				largeSelect.innerHTML = '<option value="">대분류 불러오기 실패</option>';
			});
	}

	// 2) 중분류 불러오기
	function fetchMediumCategories(largeId) {
		if (!largeId) {
			mediumSelect.innerHTML = '<option value="">중분류</option>';
			smallSelect.innerHTML = '<option value="">소분류</option>';
			productListArea.innerHTML = '';
			return;
		}
		fetch('/api/category/list-medium?largeId=' + encodeURIComponent(largeId))
			.then(res => {
				if (!res.ok) throw new Error("중분류 API 오류");
				return res.json();
			})
			.then(list => {
				if (!Array.isArray(list)) list = [];
				mediumSelect.innerHTML = '<option value="">중분류</option>';
				list.forEach(m => {
					const opt = document.createElement('option');
					opt.value = m.id;
					opt.textContent = m.name + ' (' + (m.smallCount ?? 0) + ')';
					mediumSelect.appendChild(opt);
				});
				smallSelect.innerHTML = '<option value="">소분류</option>';
				productListArea.innerHTML = '';
			})
			.catch(err => {
				console.error('중분류 API 오류', err);
				mediumSelect.innerHTML = '<option value="">중분류 불러오기 실패</option>';
			});
	}

	// 3) 소분류 불러오기
	function fetchSmallCategories(mediumId) {
		if (!mediumId) {
			smallSelect.innerHTML = '<option value="">소분류</option>';
			productListArea.innerHTML = '';
			return;
		}
		fetch('/api/category/list-small-with-product-count?mediumId=' + encodeURIComponent(mediumId))
			.then(res => {
				if (!res.ok) throw new Error("소분류 API 오류");
				return res.json();
			})
			.then(list => {
				if (!Array.isArray(list)) list = [];
				smallSelect.innerHTML = '<option value="">소분류</option>';
				list.forEach(s => {
					const opt = document.createElement('option');
					opt.value = s.id;
					opt.textContent = s.name + ' (' + (s.productCount ?? 0) + ')';
					smallSelect.appendChild(opt);
				});
				productListArea.innerHTML = '';
			})
			.catch(err => {
				console.error('소분류 API 오류', err);
				smallSelect.innerHTML = '<option value="">소분류 불러오기 실패</option>';
			});
	}

	// 4) 제품 목록 불러오기
	function fetchProductList(smallId) {
		if (!smallId) {
			productListArea.innerHTML = '';
			giftProducts = [];
			return;
		}
		fetch('/api/product/list-simple?smallId=' + encodeURIComponent(smallId))
			.then(res => {
				if (!res.ok) throw new Error("제품 API 오류");
				return res.json();
			})
			.then(list => {
				if (!Array.isArray(list)) list = [];
				giftProducts = list; // [{id, code, name}]
				renderGiftProductList();
			})
			.catch(err => {
				console.error('제품목록 API 오류', err);
				productListArea.innerHTML = '<div class="text-danger small">제품목록 불러오기 실패</div>';
				giftProducts = [];
			});
	}

	// 5) 제품 리스트 렌더링
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
					${selectedGiftProduct && String(selectedGiftProduct.id) === String(product.id) ? 'disabled' : ''}
					data-id="${product.id}">등록</button>
			`;
			item.querySelector('.product-promotion-register-btn').onclick = () => {
				// 1개만 등록
				selectedGiftProduct = product;
				// 분류검색 초기화 + 비활성화
				resetGiftFilters();
				renderSelectedGiftProduct();
				// 리스트는 비워도 OK
				productListArea.innerHTML = '';
				setGiftFiltersEnabled(false);
			};
			productListArea.appendChild(item);
		});

		// 선택 후에는 전체 disable 처리
		setGiftFiltersEnabled(!selectedGiftProduct);
	}

	// 6) 선택된 증정상품 렌더링 + 삭제
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
				// 필터 다시 활성화 + 대분류부터 로딩 재개
				setGiftFiltersEnabled(true);
				fetchLargeCategories();
				renderSelectedGiftProduct();
			};
		}
	}

	// 7) 카테고리 선택 이벤트 바인딩
	largeSelect.addEventListener('change', function() {
		selectedLargeId = this.value || null;
		selectedMediumId = null;
		selectedSmallId = null;
		selectedGiftProduct = null;
		fetchMediumCategories(selectedLargeId);
		renderSelectedGiftProduct();
	});

	mediumSelect.addEventListener('change', function() {
		selectedMediumId = this.value || null;
		selectedSmallId = null;
		selectedGiftProduct = null;
		fetchSmallCategories(selectedMediumId);
		renderSelectedGiftProduct();
	});

	smallSelect.addEventListener('change', function() {
		selectedSmallId = this.value || null;
		selectedGiftProduct = null;
		fetchProductList(selectedSmallId);
		renderSelectedGiftProduct();
	});

	// 최초 로드시 대분류부터
	fetchLargeCategories();
	renderSelectedGiftProduct();

	// =========================
	// [COUPON] 쿠폰 검색/선택 (1개 제한)
	// =========================
	function setCouponFiltersEnabled(enabled) {
		// 상태는 어차피 ISSUED 고정(disabled 유지)
		couponKeyword.disabled = !enabled;
		couponStartDate.disabled = !enabled;
		couponEndDate.disabled = !enabled;
		couponSearchBtn.disabled = !enabled;
		// 검색 리스트 버튼
		couponListArea.querySelectorAll('button.product-promotion-register-btn').forEach(btn => btn.disabled = !enabled);
	}

	function fetchCouponsAndRender() {
		// 항상 ISSUED로 고정
		const params = new URLSearchParams();
		params.append('status', 'ISSUED');

		const name = couponKeyword.value.trim();
		const startDate = couponStartDate.value;
		const endDate = couponEndDate.value;
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
				console.error('쿠폰목록 API 오류', err);
				couponListArea.innerHTML = '<div class="text-danger small">쿠폰목록 불러오기 실패</div>';
			});
	}

	function renderCouponList(couponList) {
		couponListArea.innerHTML = "";
		if (!Array.isArray(couponList) || couponList.length === 0) {
			couponListArea.innerHTML = '<div class="text-muted small"> # 검색결과가 없습니다.</div>';
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
					${selectedCoupon && String(selectedCoupon.id) === String(coupon.id) ? 'disabled' : ''}
					data-id="${coupon.id}">등록</button>
			`;
			item.querySelector('.product-promotion-register-btn').onclick = () => {
				selectedCoupon = coupon;
				renderSelectedCoupon();
				// 선택되면 검색 잠금
				setCouponFiltersEnabled(false);
				// 리스트는 비워도 OK
				couponListArea.innerHTML = '';
			};
			couponListArea.appendChild(item);
		});
		// 선택 상태면 잠금
		setCouponFiltersEnabled(!selectedCoupon);
	}

	function renderSelectedCoupon() {
		selectedCouponArea.innerHTML = "";
		if (selectedCoupon) {
			selectedCouponArea.innerHTML = `
				<span class="product-promotion-selected-badge">
					${selectedCoupon.couponName}
					<span class="product-promotion-selected-remove text-warning ms-2" style="cursor:pointer;">&times;</span>
				</span>
			`;
			selectedCouponArea.querySelector('.product-promotion-selected-remove').onclick = () => {
				selectedCoupon = null;
				// 다시 검색 가능
				setCouponFiltersEnabled(true);
				fetchCouponsAndRender();
				renderSelectedCoupon();
			};
		}
	}

	// 쿠폰 검색버튼/초기 검색
	couponSearchBtn.onclick = fetchCouponsAndRender;
	fetchCouponsAndRender();
	renderSelectedCoupon();
});
