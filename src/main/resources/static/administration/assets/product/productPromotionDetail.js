// /administration/assets/product/productPromotionDetail.js
document.addEventListener('DOMContentLoaded', function() {
	// 등록 JS 로직을 거의 동일하게 사용하되, 초기 상태와 submit 대상만 다름
	let selectedGiftProduct = null;
	let selectedCoupon = null;

	// 공통 DOM
	const form = document.getElementById('product-promotion-form');
	const submitBtn = document.getElementById('productPromotionSubmitBtn');
	const termSelect = form.elements['term'];
	const startDateInput = form.elements['startDate'];
	const endDateInput = form.elements['endDate'];

	// 증정 DOM
	const largeSelect = document.getElementById('giftCategoryLarge');
	const mediumSelect = document.getElementById('giftCategoryMedium');
	const smallSelect = document.getElementById('giftCategorySmall');
	const productListArea = document.getElementById('giftProductList');
	const selectedGiftArea = document.getElementById('selectedGiftProductList');

	// 쿠폰 DOM
	const couponKeyword = document.getElementById('couponKeyword');
	const couponStartDate = document.getElementById('couponStartDate');
	const couponEndDate = document.getElementById('couponEndDate');
	const couponSearchBtn = document.getElementById('couponSearchBtn');
	const couponListArea = document.getElementById('couponList');
	const selectedCouponArea = document.getElementById('selectedCouponList');

	// 기간정책 동기화
	function syncTerm() {
		const v = termSelect.value;
		if (v === 'ALWAYS') {
			startDateInput.value = '';
			endDateInput.value = '';
			startDateInput.disabled = true;
			endDateInput.disabled = true;
			startDateInput.removeAttribute('required');
			endDateInput.removeAttribute('required');
		} else {
			startDateInput.disabled = false;
			endDateInput.disabled = false;
			startDateInput.setAttribute('required', 'required');
			endDateInput.setAttribute('required', 'required');
		}
	}
	termSelect.addEventListener('change', syncTerm);
	syncTerm();

	// 타입별 패널 show/hide
	function syncTypePanels() {
		const type = form.querySelector('input[name="type"]:checked')?.value;
		document.querySelectorAll('.product-promotion-type-panel').forEach(panel => {
			panel.style.display = (panel.getAttribute('data-type') === type) ? '' : 'none';
		});
	}
	document.querySelectorAll('input[name="type"]').forEach(r => {
		r.addEventListener('change', syncTypePanels);
	});
	syncTypePanels();

	// ========== GIFT 필터/렌더링 ==========
	function setGiftFiltersEnabled(enabled) {
		[largeSelect, mediumSelect, smallSelect].forEach(el => el && (el.disabled = !enabled));
		productListArea?.querySelectorAll('button.product-promotion-register-btn').forEach(btn => {
			btn.disabled = !enabled || (selectedGiftProduct && String(btn.dataset.id) === String(selectedGiftProduct.id));
		});
	}
	function resetGiftFilters() {
		if (!largeSelect) return;
		largeSelect.value = '';
		mediumSelect.innerHTML = '<option value="">중분류</option>';
		smallSelect.innerHTML = '<option value="">소분류</option>';
		productListArea.innerHTML = '';
	}
	function fetchLargeCategories() {
		if (!largeSelect) return;
		fetch('/api/category/list-large').then(r => r.json()).then(list => {
			largeSelect.innerHTML = '<option value="">대분류</option>';
			(list || []).forEach(l => {
				const opt = document.createElement('option');
				opt.value = l.id; opt.textContent = l.name + ' (' + (l.mediumCount ?? 0) + ')';
				largeSelect.appendChild(opt);
			});
			mediumSelect.innerHTML = '<option value="">중분류</option>';
			smallSelect.innerHTML = '<option value="">소분류</option>';
			productListArea.innerHTML = '';
		}).catch(() => { largeSelect.innerHTML = '<option value="">대분류 불러오기 실패</option>'; });
	}
	function fetchMediumCategories(largeId) {
		if (!largeSelect || !largeId) return;
		fetch('/api/category/list-medium?largeId=' + encodeURIComponent(largeId)).then(r => r.json()).then(list => {
			mediumSelect.innerHTML = '<option value="">중분류</option>';
			(list || []).forEach(m => {
				const opt = document.createElement('option');
				opt.value = m.id; opt.textContent = m.name + ' (' + (m.smallCount ?? 0) + ')';
				mediumSelect.appendChild(opt);
			});
			smallSelect.innerHTML = '<option value="">소분류</option>';
			productListArea.innerHTML = '';
		}).catch(() => { mediumSelect.innerHTML = '<option value="">중분류 불러오기 실패</option>'; });
	}
	function fetchSmallCategories(mediumId) {
		if (!smallSelect || !mediumId) return;
		fetch('/api/category/list-small-with-product-count?mediumId=' + encodeURIComponent(mediumId)).then(r => r.json()).then(list => {
			smallSelect.innerHTML = '<option value="">소분류</option>';
			(list || []).forEach(s => {
				const opt = document.createElement('option');
				opt.value = s.id; opt.textContent = s.name + ' (' + (s.productCount ?? 0) + ')';
				smallSelect.appendChild(opt);
			});
			productListArea.innerHTML = '';
		}).catch(() => { smallSelect.innerHTML = '<option value="">소분류 불러오기 실패</option>'; });
	}
	function fetchProductList(smallId) {
		if (!productListArea || !smallId) return;
		fetch('/api/product/list-simple?smallId=' + encodeURIComponent(smallId)).then(r => r.json()).then(list => {
			productListArea.innerHTML = '';
			(list || []).forEach(p => {
				const row = document.createElement('div');
				row.className = "product-promotion-list-item d-flex align-items-center justify-content-between mb-2";
				row.innerHTML = `
                  <span><strong>${p.name}</strong></span>
                  <button type="button" class="btn btn-sm btn-outline-primary product-promotion-register-btn" data-id="${p.id}">등록</button>`;
				row.querySelector('button').onclick = () => {
					selectedGiftProduct = p;
					resetGiftFilters();
					renderSelectedGiftProduct();
					setGiftFiltersEnabled(false);
				};
				productListArea.appendChild(row);
			});
			setGiftFiltersEnabled(!selectedGiftProduct);
		}).catch(() => {
			productListArea.innerHTML = '<div class="text-danger small">제품목록 불러오기 실패</div>';
		});
	}
	function renderSelectedGiftProduct() {
		if (!selectedGiftArea) return;
		selectedGiftArea.innerHTML = "";
		if (selectedGiftProduct) {
			selectedGiftArea.innerHTML = `
              <span class="product-promotion-selected-badge">
                ${selectedGiftProduct.name}
                <span class="product-promotion-selected-remove text-warning ms-2" style="cursor:pointer;">&times;</span>
              </span>`;
			selectedGiftArea.querySelector('.product-promotion-selected-remove').onclick = () => {
				selectedGiftProduct = null;
				setGiftFiltersEnabled(true);
				fetchLargeCategories();
				renderSelectedGiftProduct();
			};
		}
	}
	largeSelect?.addEventListener('change', function() { fetchMediumCategories(this.value || null); });
	mediumSelect?.addEventListener('change', function() { fetchSmallCategories(this.value || null); });
	smallSelect?.addEventListener('change', function() { fetchProductList(this.value || null); });

	// ========== 쿠폰 ==========
	function setCouponFiltersEnabled(enabled) {
		couponKeyword.disabled = !enabled;
		couponStartDate.disabled = !enabled;
		couponEndDate.disabled = !enabled;
		couponSearchBtn.disabled = !enabled;
		couponListArea.querySelectorAll('button.product-promotion-register-btn').forEach(btn => btn.disabled = !enabled);
	}
	function fetchCouponsAndRender() {
		const params = new URLSearchParams();
		params.append('status', 'ISSUED');
		const name = couponKeyword.value.trim();
		const sd = couponStartDate.value;
		const ed = couponEndDate.value;
		if (name) params.append('name', name);
		if (sd) params.append('startDate', sd);
		if (ed) params.append('endDate', ed);

		fetch('/api/coupon/search?' + params.toString())
			.then(res => res.json())
			.then(list => renderCouponList(list))
			.catch(() => { couponListArea.innerHTML = '<div class="text-danger small">쿠폰목록 불러오기 실패</div>'; });
	}
	function renderCouponList(list) {
		couponListArea.innerHTML = "";
		if (!Array.isArray(list) || list.length === 0) {
			couponListArea.innerHTML = '<div class="text-muted small"> # 검색결과가 없습니다.</div>';
			return;
		}
		list.forEach(coupon => {
			const row = document.createElement('div');
			row.className = "product-promotion-list-item d-flex align-items-center justify-content-between mb-2";
			row.innerHTML = `
              <span>
                <strong>${coupon.couponName}</strong>
                <small class="text-muted ms-2">${coupon.startDate} ~ ${coupon.endDate}</small>
                <span class="badge bg-light text-dark ms-2">${coupon.status}</span>
              </span>
              <button type="button" class="btn btn-sm btn-outline-primary product-promotion-register-btn">등록</button>`;
			row.querySelector('button').onclick = () => {
				selectedCoupon = coupon;
				renderSelectedCoupon();
				setCouponFiltersEnabled(false);
				couponListArea.innerHTML = '';
			};
			couponListArea.appendChild(row);
		});
		setCouponFiltersEnabled(!selectedCoupon);
	}
	function renderSelectedCoupon() {
		selectedCouponArea.innerHTML = "";
		if (selectedCoupon) {
			selectedCouponArea.innerHTML = `
              <span class="product-promotion-selected-badge">
                ${selectedCoupon.couponName}
                <span class="product-promotion-selected-remove text-warning ms-2" style="cursor:pointer;">&times;</span>
              </span>`;
			selectedCouponArea.querySelector('.product-promotion-selected-remove').onclick = () => {
				selectedCoupon = null;
				setCouponFiltersEnabled(true);
				fetchCouponsAndRender();
				renderSelectedCoupon();
			};
		}
	}
	couponSearchBtn.onclick = fetchCouponsAndRender;
	fetchCouponsAndRender();
	renderSelectedCoupon();

	// ========== 초기 상태 주입 반영 ==========
	(function initFromServer() {
		const data = window.__promotion__;
		if (!data) return;

		// 기간
		if (data.term === 'ALWAYS') {
			termSelect.value = 'ALWAYS';
		} else {
			termSelect.value = 'PERIOD';
			if (data.startDate) startDateInput.value = data.startDate;
			if (data.endDate) endDateInput.value = data.endDate;
		}
		syncTerm();

		// 타입
		const input = form.querySelector(`input[name="type"][value="${data.type}"]`);
		if (input) { input.checked = true; }
		syncTypePanels();

		// 할인
		if (data.type === 'DISCOUNT' && data.discountPercent != null) {
			form.elements['discountPercent'].value = data.discountPercent;
		}

		// 증정
		if (data.type === 'GIFT' && data.giftProduct) {
			selectedGiftProduct = { id: data.giftProduct.id, name: data.giftProduct.name };
			renderSelectedGiftProduct();
			setGiftFiltersEnabled(false);
		} else {
			// 초기 로딩
			fetchLargeCategories();
			renderSelectedGiftProduct();
		}

		// 쿠폰
		if (data.type === 'COUPON' && data.coupon) {
			selectedCoupon = data.coupon;
			renderSelectedCoupon();
			setCouponFiltersEnabled(false);
			couponListArea.innerHTML = '';
		}
	})();

	// ========== 제출 (UPDATE) ==========
	form.addEventListener('submit', function(e) {
		// 프론트 유효성 — 등록폼과 동일
		const type = form.querySelector('input[name="type"]:checked')?.value;
		const term = termSelect.value;
		if (!type) { e.preventDefault(); alert('프로모션 타입을 선택해주세요.'); return; }

		if (term === 'PERIOD') {
			if (!startDateInput.value || !endDateInput.value) {
				e.preventDefault(); alert('기간한정일 때 시작일/종료일은 필수입니다.'); return;
			}
		} else {
			// ALWAYS — 날짜 초기화
			startDateInput.value = '';
			endDateInput.value = '';
		}

		if (type === 'DISCOUNT') {
			const dp = form.elements['discountPercent'].value;
			if (!dp || isNaN(dp) || dp < 0 || dp > 100) {
				e.preventDefault(); alert('할인율을 0~100 사이로 입력해주세요.'); return;
			}
		} else if (type === 'GIFT') {
			if (!selectedGiftProduct) { e.preventDefault(); alert('증정 상품을 선택해주세요.'); return; }
			// hidden 없이도 서버에서 giftProductId 받도록 formdata에 append 하려면 submit 전에 숨은 input 생성
			ensureHidden('giftProductId', selectedGiftProduct.id);
		} else if (type === 'COUPON') {
			if (!selectedCoupon) { e.preventDefault(); alert('쿠폰을 선택해주세요.'); return; }
			ensureHidden('couponId', selectedCoupon.id);
		}
		// 안내 메시지는 서버에서 flash로 내려줍니다.
	});

	function ensureHidden(name, value) {
		let input = form.querySelector(`input[name="${name}"]`);
		if (!input) {
			input = document.createElement('input');
			input.type = 'hidden'; input.name = name;
			form.appendChild(input);
		}
		input.value = value;
	}
});
