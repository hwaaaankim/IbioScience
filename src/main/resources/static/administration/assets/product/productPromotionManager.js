// /administration/assets/product/productDiscountManager.js
document.addEventListener('DOMContentLoaded', function() {
	const sampleProducts = Array.from({ length: 10 }, (_, i) => ({
		id: i + 1,
		name: `샘플제품 ${i + 1}`,
		category: `대분류 > 중분류 > 소분류${i + 1}`
	}));
	const sampleCoupons = Array.from({ length: 10 }, (_, i) => ({
		id: i + 1,
		name: `할인쿠폰 ${i + 1}`,
		startDate: "2024-07-01",
		endDate: "2024-07-31"
	}));

	// 현재 등록된 증정상품/쿠폰
	let selectedGiftProduct = null;
	let selectedCoupon = null;

	// 타입별 panel show/hide
	document.querySelectorAll('input[name="type"]').forEach(radio => {
		radio.addEventListener('change', function() {
			document.querySelectorAll('.product-promotion-type-panel').forEach(panel => {
				panel.style.display = (panel.getAttribute('data-type') === this.value) ? '' : 'none';
			});
		});
	});

	// === 증정상품 검색 및 등록 ===
	function renderGiftProductList() {
		const listArea = document.getElementById('giftProductList');
		listArea.innerHTML = "";
		sampleProducts.forEach(product => {
			const item = document.createElement('div');
			item.className = "product-promotion-list-item";
			item.innerHTML = `
			<span>
				<strong>${product.name}</strong>
				<small class="text-muted ms-2">${product.category}</small>
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
			listArea.appendChild(item);
		});
	}
	function renderSelectedGiftProduct() {
		const area = document.getElementById('selectedGiftProductList');
		area.innerHTML = "";
		if (selectedGiftProduct) {
			area.innerHTML = `
			<span class="product-promotion-selected-badge">
				${selectedGiftProduct.name}
				<span class="product-promotion-selected-remove text-warning ms-2" style="cursor:pointer;">&times;</span>
			</span>
		`;
			area.querySelector('.product-promotion-selected-remove').onclick = () => {
				selectedGiftProduct = null;
				renderGiftProductList();
				renderSelectedGiftProduct();
			};
		}
	}

	// === 쿠폰 검색 및 등록 ===
	function renderCouponList() {
		const listArea = document.getElementById('couponList');
		listArea.innerHTML = "";
		sampleCoupons.forEach(coupon => {
			const item = document.createElement('div');
			item.className = "product-promotion-list-item";
			item.innerHTML = `
			<span>
				<strong>${coupon.name}</strong>
				<small class="text-muted ms-2">${coupon.startDate} ~ ${coupon.endDate}</small>
			</span>
			<button type="button" class="btn btn-sm btn-outline-primary product-promotion-register-btn"
				${selectedCoupon && selectedCoupon.id === coupon.id ? 'disabled' : ''}
				data-id="${coupon.id}">등록</button>
		`;
			item.querySelector('.product-promotion-register-btn').onclick = () => {
				selectedCoupon = coupon;
				renderCouponList();
				renderSelectedCoupon();
			};
			listArea.appendChild(item);
		});
	}
	function renderSelectedCoupon() {
		const area = document.getElementById('selectedCouponList');
		area.innerHTML = "";
		if (selectedCoupon) {
			area.innerHTML = `
			<span class="product-promotion-selected-badge">
				${selectedCoupon.name}
				<span class="product-promotion-selected-remove text-warning ms-2" style="cursor:pointer;">&times;</span>
			</span>
		`;
			area.querySelector('.product-promotion-selected-remove').onclick = () => {
				selectedCoupon = null;
				renderCouponList();
				renderSelectedCoupon();
			};
		}
	}

	// 최초 로드시 탭별 리스트 렌더링
	window.addEventListener('DOMContentLoaded', () => {
		renderGiftProductList();
		renderSelectedGiftProduct();
		renderCouponList();
		renderSelectedCoupon();
		// 타입 radio 변경시 자동 show/hide 처리 (이미 위에서 바인딩됨)
	});
});
