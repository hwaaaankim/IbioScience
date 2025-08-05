document.addEventListener("DOMContentLoaded", function() {
	// === 기존 변수/상태 선언 ===
	const largeList = document.getElementById('category-large-list');
	const mediumList = document.getElementById('category-medium-list');
	const smallList = document.getElementById('category-small-list');
	const selectedList = document.getElementById('selected-category-list');
	let selectedCategories = [];

	let largeCategoryMap = {};
	let mediumCategoryMap = {};

	let keywords = [];
	const keywordInput = document.getElementById('product-keyword-input');
	const addKeywordBtn = document.getElementById('add-keyword-btn');
	const keywordList = document.getElementById('product-keyword-list');

	/* 할인 혜택 */
	const promotionModal = document.getElementById('discountModal');
	const searchPromotionBtn = document.getElementById('search-promotion-btn');
	const promotionModalList = document.getElementById('promotion-modal-list');
	let selectedPromotion = null;

	document.getElementById('open-discount-modal-btn').onclick = function() {
		const promotionModal = document.getElementById('discountModal');
		promotionModal.style.display = 'block';
		promotionModal.classList.add('active');
		renderPromotionList([]);
	};
	document.querySelectorAll('.discount-modal-close, .discount-modal-overlay').forEach(btn => btn.onclick = () => {
		const promotionModal = document.getElementById('discountModal');
		promotionModal.style.display = 'none';
		promotionModal.classList.remove('active');
	});


	// 검색 버튼 클릭 시
	searchPromotionBtn.onclick = function() {
		const name = document.getElementById('promotionName').value.trim();
		const type = document.getElementById('promotionType').value;
		const start = document.getElementById('promotionStart').value;
		const end = document.getElementById('promotionEnd').value;
		const active = document.getElementById('promotionActive').value;
		let url = `/api/promotion/search?`;
		url += name ? `name=${encodeURIComponent(name)}&` : '';
		url += type ? `type=${encodeURIComponent(type)}&` : '';
		url += start ? `startDate=${start}&` : '';
		url += end ? `endDate=${end}&` : '';
		url += (active ? `active=${active}&` : '');
		fetch(url)
			.then(res => res.json())
			.then(list => renderPromotionList(list));
	};

	function renderPromotionList(list) {
		promotionModalList.innerHTML = '';
		if (!list || list.length === 0) {
			promotionModalList.innerHTML = '<div class="text-muted text-center">프로모션이 없습니다.</div>';
			return;
		}
		list.forEach(d => {
			const div = document.createElement('div');
			div.className = 'd-flex align-items-center border-bottom py-1';
			div.innerHTML = `
      <span class="me-2">${d.name}</span>
      <span class="badge bg-info text-dark me-2">${d.typeLabel || d.type}</span>
      <span class="badge bg-secondary me-2">${d.termLabel || d.term}</span>
      <span class="badge ${d.active ? 'bg-primary' : 'bg-secondary'}">${d.active ? "ON" : "OFF"}</span>
      <button type="button" class="btn btn-outline-primary btn-sm ms-auto" data-id="${d.id}">등록</button>
    `;
			div.querySelector('button').onclick = function() {
				selectedPromotion = d;
				renderSelectedPromotion();
				promotionModal.style.display = 'none';
			};
			promotionModalList.appendChild(div);
		});
	}

	function renderSelectedPromotion() {
		const area = document.getElementById('selected-discount-list');
		area.innerHTML = '';
		if (!selectedPromotion) return;
		const badge = document.createElement('div');
		badge.className = 'badge bg-danger text-white px-2 py-2 d-flex align-items-center';
		badge.innerHTML = `
    <span>${selectedPromotion.name} (${selectedPromotion.typeLabel || selectedPromotion.type})</span>
    <span class="ms-2" style="cursor:pointer;" title="삭제">&times;</span>
  `;
		badge.querySelector('span:last-child').onclick = function() {
			selectedPromotion = null;
			renderSelectedPromotion();
		};
		area.appendChild(badge);
	}
	/* 할인혜택 */

	/* 브랜드 등록 */
	const brandSearchInput = document.getElementById('brand-search-input');
	const brandSearchBtn = document.getElementById('brand-search-btn');
	const brandSearchResult = document.getElementById('brand-search-result');
	const brandSelectedArea = document.getElementById('brand-selected-area');
	let selectedBrand = null;

	brandSearchBtn.onclick = function() {
		const kw = brandSearchInput.value.trim();
		if (!kw) return;

		// 모든 검색결과 가져오기 (제한 없음)
		fetch(`/api/brand/search?keyword=${encodeURIComponent(kw)}`)
			.then(res => res.json())
			.then(list => {
				brandSearchResult.innerHTML = '';
				if (!list || list.length === 0) {
					brandSearchResult.innerHTML = '<div class="text-center text-muted py-2">검색결과가 없습니다.</div>';
					return;
				}
				list.forEach((b, idx) => {
					const div = document.createElement('div');
					div.className = 'brand-search-item';
					div.innerHTML = `
          <img src="${b.imageRoad || '/assets/brand-default.png'}" alt="브랜드">
          <span>${b.name}</span>
          <button type="button" class="btn btn-outline-primary btn-sm" data-id="${b.id}">선택</button>
        `;
					div.querySelector('button').onclick = function() {
						selectedBrand = b;
						renderSelectedBrand();
					};
					brandSearchResult.appendChild(div);
				});
				// 결과가 5개 이하여도 스크롤 영역이 항상 유지됨 (max-height 고정)
			});
	};


	function renderSelectedBrand() {
		brandSelectedArea.innerHTML = '';
		if (!selectedBrand) return;
		brandSelectedArea.innerHTML = `
    <div class="d-flex align-items-center bg-light p-2 rounded">
      <img src="${selectedBrand.imageRoad || '/assets/brand-default.png'}" style="width:40px;height:40px;">
      <span class="ms-2">${selectedBrand.name}</span>
      <button type="button" class="btn btn-outline-danger btn-sm ms-2" id="remove-brand-btn">삭제</button>
    </div>
  `;
		document.getElementById('remove-brand-btn').onclick = function() {
			selectedBrand = null;
			renderSelectedBrand();
		};
	}
	/* 브랜드 등록 */

	/* 딜러별 할인율 */
	const dealerDiscounts = {}; // { A: 할인율, ... }
	const dealerDiscountButtons = document.getElementById('dealer-discount-buttons');
	const dealerDiscountList = document.getElementById('dealer-discount-list');

	// 버튼 클릭 이벤트
	dealerDiscountButtons.querySelectorAll('button').forEach(btn => {
		btn.onclick = function() {
			const grade = btn.dataset.grade;
			if (dealerDiscounts[grade]) return; // 이미 추가된 경우 중복방지

			dealerDiscounts[grade] = '';
			renderDealerDiscountList();
			btn.disabled = true;
		};
	});

	function renderDealerDiscountList() {
		dealerDiscountList.innerHTML = '';
		Object.keys(dealerDiscounts).forEach(grade => {
			// col-6, 입력칸 넓게
			const col = document.createElement('div');
			col.className = 'col-12';
			col.innerHTML = `
            <div class="input-group input-group-sm align-items-center">
                <span class="input-group-text" style="min-width:120px;">${grade} 등급 딜러에 대한 추가 할인율 입력</span>
                <input type="number" min="0" max="100" class="form-control" style="max-width:120px;" 
                    placeholder="0" value="${dealerDiscounts[grade]}" data-grade="${grade}"/>
                <span class="input-group-text">%</span>
                <button type="button" class="btn btn-outline-danger btn-sm" data-grade="${grade}" title="삭제">×</button>
            </div>
        `;
			// 할인율 입력 이벤트
			col.querySelector('input').oninput = function() {
				dealerDiscounts[grade] = this.value;
			};
			// 삭제 이벤트
			col.querySelector('button').onclick = function() {
				delete dealerDiscounts[grade];
				dealerDiscountButtons.querySelector(`button[data-grade="${grade}"]`).disabled = false;
				renderDealerDiscountList();
			};
			dealerDiscountList.appendChild(col);
		});
	}
	/* 딜러별 할인율 */
	function fetchAndRenderLargeOptions(selectEl, callback) {
		fetch('/api/category/list-large')
			.then(res => res.json())
			.then(list => {
				selectEl.innerHTML = `<option value="">대분류</option>`;
				list.forEach(cat => {
					selectEl.innerHTML += `<option value="${cat.id}">${cat.name} (${cat.mediumCount ?? 0})</option>`;
				});
				if (callback) callback(list);
			});
	}

	// --- 모달 중분류 옵션 그리기 (소분류 수 표시)
	function fetchAndRenderMediumOptions(selectEl, largeId, resetSmallSelect) {
		selectEl.innerHTML = `<option value="">중분류</option>`;
		if (!largeId) {
			if (resetSmallSelect) resetSmallSelect.innerHTML = `<option value="">소분류</option>`;
			return;
		}
		fetch(`/api/category/list-medium?largeId=${largeId}`)
			.then(res => res.json())
			.then(list => {
				list.forEach(m => {
					selectEl.innerHTML += `<option value="${m.id}">${m.name} (${m.smallCount ?? 0})</option>`;
				});
				if (resetSmallSelect) resetSmallSelect.innerHTML = `<option value="">소분류</option>`;
			});
	}

	function fetchAndRenderSmallOptions(selectEl, mediumId) {
		selectEl.innerHTML = `<option value="">소분류</option>`;
		if (!mediumId) return;
		fetch(`/api/category/list-small-with-product-count?mediumId=${mediumId}`)
			.then(res => res.json())
			.then(list => {
				list.forEach(s => {
					selectEl.innerHTML += `<option value="${s.id}">${s.name} (${s.productCount ?? 0})</option>`;
				});
			});
	}

	function fetchProductListBySmall(smallId, callback) {
		if (!smallId) {
			callback([]);
			return;
		}
		fetch(`/api/product/list-simple?smallId=${smallId}`)
			.then(res => res.json())
			.then(list => callback(list));
	}


	/* 추가구성상품모달 */
	// 추가구성상품 모달 변수 (필수 포함)
	const bundleModal = document.getElementById('bundleProductModal');
	const bundleOpenBtn = document.getElementById('open-bundle-modal-btn');
	const bundleCloseBtns = bundleModal.querySelectorAll('.bundle-modal-close, .bundle-modal-cancel, .bundle-modal-overlay');
	const bundleLargeSelect = document.getElementById('bundle-large-select');
	const bundleMediumSelect = document.getElementById('bundle-medium-select');
	const bundleSmallSelect = document.getElementById('bundle-small-select');
	const bundleKeywordInput = document.getElementById('bundle-product-keyword');
	const bundleProductSearchBtn = document.getElementById('bundle-product-search-btn');
	const bundleModalProductList = document.getElementById('bundle-modal-product-list');
	const bundleRegisterBtn = document.getElementById('bundle-register-btn');

	// 모달 내부 상태
	let bundleProductList = []; // 현재 검색결과
	let bundleSelectedProductIds = new Set(); // 체크된 id만
	let bundleProducts = []; // 실제 등록될 최종 제품리스트

	// 모달 오픈시 필터 select/input 초기화
	bundleOpenBtn.onclick = function() {
		bundleModal.classList.add('active');
		fetchAndRenderLargeOptions(bundleLargeSelect, () => {
			bundleMediumSelect.innerHTML = `<option value="">중분류</option>`;
			bundleSmallSelect.innerHTML = `<option value="">소분류</option>`;
			bundleKeywordInput.value = '';
			bundleProductList = [];
			bundleSelectedProductIds.clear();
			renderBundleModalProductList();
		});
	};

	// 셀렉트 change시 하위 필터링
	bundleLargeSelect.onchange = function() {
		fetchAndRenderMediumOptions(bundleMediumSelect, this.value, bundleSmallSelect);
		bundleProductList = [];
		renderBundleModalProductList();
	};
	bundleMediumSelect.onchange = function() {
		fetchAndRenderSmallOptions(bundleSmallSelect, this.value);
		bundleProductList = [];
		renderBundleModalProductList();
	};

	// 검색버튼 클릭시 실제 제품 조회
	bundleProductSearchBtn.onclick = function() {
		const largeId = bundleLargeSelect.value;
		const mediumId = bundleMediumSelect.value;
		const smallId = bundleSmallSelect.value;
		const keyword = bundleKeywordInput.value.trim();

		// 필터 쿼리
		let url = `/api/product/list-simple?`;
		if (largeId) url += `largeId=${largeId}&`;
		if (mediumId) url += `mediumId=${mediumId}&`;
		if (smallId) url += `smallId=${smallId}&`;
		if (keyword) url += `keyword=${encodeURIComponent(keyword)}&`;

		fetch(url)
			.then(res => res.json())
			.then(list => {
				bundleProductList = list || [];
				renderBundleModalProductList();
			});
	};

	// 실제 리스트 렌더 (검색된 제품들)
	function renderBundleModalProductList() {
		bundleModalProductList.innerHTML = '';
		if (!bundleProductList || bundleProductList.length === 0) {
			bundleModalProductList.innerHTML = '<div class="text-muted text-center">제품이 없습니다.</div>';
			return;
		}
		const ul = document.createElement('ul');
		ul.className = 'list-group mb-2';
		bundleProductList.forEach(product => {
			const li = document.createElement('li');
			li.className = 'list-group-item d-flex justify-content-between align-items-center';
			// 체크박스
			const checkbox = document.createElement('input');
			checkbox.type = 'checkbox';
			checkbox.className = 'form-check-input me-2';
			checkbox.value = product.id;
			checkbox.checked = bundleSelectedProductIds.has(Number(product.id));
			checkbox.onchange = function() {
				if (this.checked) bundleSelectedProductIds.add(Number(this.value));
				else bundleSelectedProductIds.delete(Number(this.value));
			};

			const label = document.createElement('span');
			label.textContent = `[${product.code}] ${product.name}`;

			li.appendChild(checkbox);
			li.appendChild(label);
			ul.appendChild(li);
		});
		bundleModalProductList.appendChild(ul);
	}

	// 등록버튼 클릭시 체크된 제품만 추가구성상품에 반영
	bundleRegisterBtn.onclick = function() {
		const selectedIds = Array.from(bundleSelectedProductIds);
		// 기존에 이미 등록된 건 중복 제외
		selectedIds.forEach(productId => {
			const product = bundleProductList.find(p => p.id === productId);
			if (product && !bundleProducts.some(bp => bp.id === productId)) {
				bundleProducts.push({ id: product.id, name: product.name });
			}
		});
		renderBundleProducts();
		bundleModal.classList.remove('active');
		bundleSelectedProductIds.clear();
	};

	bundleCloseBtns.forEach(btn => btn.onclick = () => bundleModal.classList.remove('active'));

	// 메인에 반영(기존 renderBundleProducts 그대로)
	function renderBundleProducts() {
		const list = document.getElementById('bundle-products-list');
		list.innerHTML = '';
		bundleProducts.forEach((p, idx) => {
			const badge = document.createElement('div');
			badge.className = 'badge bg-success text-white px-2 py-2 d-flex align-items-center';
			badge.innerHTML = `${p.name}<span class="ms-2" style="cursor:pointer;" title="삭제">&times;</span>`;
			badge.querySelector('span').onclick = () => {
				bundleProducts.splice(idx, 1);
				renderBundleProducts();
			}
			list.appendChild(badge);
		});
	}
	/* 추가구성상품 모달 */

	/* 관련상품 모달 */
	const relatedModal = document.getElementById('relatedProductModal');
	const relatedOpenBtn = document.getElementById('open-related-modal-btn');
	const relatedCloseBtns = relatedModal.querySelectorAll('.related-modal-close, .related-modal-cancel, .related-modal-overlay');
	const relatedLargeSelect = document.getElementById('related-large-select');
	const relatedMediumSelect = document.getElementById('related-medium-select');
	const relatedSmallSelect = document.getElementById('related-small-select');
	const relatedKeywordInput = document.getElementById('related-product-keyword');
	const relatedProductSearchBtn = document.getElementById('related-product-search-btn');
	const relatedModalProductList = document.getElementById('related-modal-product-list');
	const relatedRegisterBtn = document.getElementById('related-register-btn');
	const relatedSelectedList = document.getElementById('related-modal-selected-list');

	let relatedProductList = []; // 검색 결과
	let relatedCheckedIds = new Set(); // 체크박스 선택
	let relatedProductsTemp = []; // 모달 임시리스트 (순서O, 삭제O)
	let relatedProducts = []; // 실제 등록될 메인리스트

	relatedOpenBtn.onclick = function() {
		relatedModal.classList.add('active');
		fetchAndRenderLargeOptions(relatedLargeSelect, () => {
			relatedMediumSelect.innerHTML = `<option value="">중분류</option>`;
			relatedSmallSelect.innerHTML = `<option value="">소분류</option>`;
			relatedKeywordInput.value = '';
			relatedProductList = [];
			relatedCheckedIds.clear();
			relatedProductsTemp = [];
			renderRelatedModalProductList();
			renderRelatedSelectedList();
		});
	};

	relatedLargeSelect.onchange = function() {
		fetchAndRenderMediumOptions(relatedMediumSelect, this.value, relatedSmallSelect);
		relatedProductList = [];
		renderRelatedModalProductList();
	};
	relatedMediumSelect.onchange = function() {
		fetchAndRenderSmallOptions(relatedSmallSelect, this.value);
		relatedProductList = [];
		renderRelatedModalProductList();
	};
	relatedProductSearchBtn.onclick = function() {
		const largeId = relatedLargeSelect.value;
		const mediumId = relatedMediumSelect.value;
		const smallId = relatedSmallSelect.value;
		const keyword = relatedKeywordInput.value.trim();

		let url = `/api/product/list-simple?`;
		if (largeId) url += `largeId=${largeId}&`;
		if (mediumId) url += `mediumId=${mediumId}&`;
		if (smallId) url += `smallId=${smallId}&`;
		if (keyword) url += `keyword=${encodeURIComponent(keyword)}&`;

		fetch(url)
			.then(res => res.json())
			.then(list => {
				relatedProductList = list || [];
				renderRelatedModalProductList();
			});
	};

	function renderRelatedModalProductList() {
		relatedModalProductList.innerHTML = '';
		if (!relatedProductList || relatedProductList.length === 0) {
			relatedModalProductList.innerHTML = '<div class="text-muted text-center">제품이 없습니다.</div>';
			return;
		}
		const ul = document.createElement('ul');
		ul.className = 'list-group mb-2';
		relatedProductList.forEach(product => {
			const li = document.createElement('li');
			li.className = 'list-group-item d-flex justify-content-between align-items-center';
			// 체크박스
			const checkbox = document.createElement('input');
			checkbox.type = 'checkbox';
			checkbox.className = 'form-check-input me-2';
			checkbox.value = product.id;
			checkbox.checked = relatedCheckedIds.has(Number(product.id));
			checkbox.onchange = function() {
				if (this.checked) {
					relatedCheckedIds.add(Number(this.value));
				} else {
					relatedCheckedIds.delete(Number(this.value));
				}
			};
			const label = document.createElement('span');
			label.textContent = `[${product.code}] ${product.name}`;
			li.appendChild(checkbox);
			li.appendChild(label);
			ul.appendChild(li);
		});
		relatedModalProductList.appendChild(ul);
	}

	// 등록버튼(1차) 클릭: 체크된 제품만 temp로 하단에 보여주고 순서/삭제/타입조정
	relatedRegisterBtn.onclick = function() {
		relatedProductsTemp = Array.from(relatedCheckedIds).map(productId => {
			const product = relatedProductList.find(p => p.id === productId);
			return product ? { id: product.id, name: product.name } : null;
		}).filter(Boolean);
		renderRelatedSelectedList();
	};

	// 하단 임시리스트에서 순서이동/삭제 지원
	function renderRelatedSelectedList() {
		relatedSelectedList.innerHTML = '';
		if (!relatedProductsTemp || relatedProductsTemp.length === 0) {
			relatedSelectedList.innerHTML = `<div class="text-muted text-center">선택된 관련상품이 없습니다.</div>`;
			return;
		}
		relatedProductsTemp.forEach((p, idx) => {
			const div = document.createElement('div');
			div.className = 'badge bg-info text-white px-2 py-2 me-2 mb-2 d-inline-flex align-items-center';
			div.innerHTML = `
            <span>${p.name}</span>
            <span class="ms-2" style="cursor:pointer;" title="위로">&#8593;</span>
            <span class="ms-1" style="cursor:pointer;" title="아래로">&#8595;</span>
            <span class="ms-1" style="cursor:pointer;" title="삭제">&times;</span>
        `;
			// 위로
			div.children[1].onclick = function() {
				if (idx > 0) {
					[relatedProductsTemp[idx], relatedProductsTemp[idx - 1]] = [relatedProductsTemp[idx - 1], relatedProductsTemp[idx]];
					renderRelatedSelectedList();
				}
			};
			// 아래로
			div.children[2].onclick = function() {
				if (idx < relatedProductsTemp.length - 1) {
					[relatedProductsTemp[idx], relatedProductsTemp[idx + 1]] = [relatedProductsTemp[idx + 1], relatedProductsTemp[idx]];
					renderRelatedSelectedList();
				}
			};
			// 삭제
			div.children[3].onclick = function() {
				relatedProductsTemp.splice(idx, 1);
				renderRelatedSelectedList();
			}
			relatedSelectedList.appendChild(div);
		});
	}

	// 최종 관련상품 저장/반영
	document.getElementById('related-register-btn').onclick = function() {
		relatedProducts = relatedProductsTemp.map(p => ({ id: p.id, name: p.name }));
		renderRelatedProducts();
		relatedModal.classList.remove('active');
	};

	relatedCloseBtns.forEach(btn => btn.onclick = () => relatedModal.classList.remove('active'));

	// 메인에 반영
	function renderRelatedProducts() {
		const list = document.getElementById('related-products-list');
		list.innerHTML = '';
		relatedProducts.forEach((p, idx) => {
			const badge = document.createElement('div');
			badge.className = 'badge bg-info text-white px-2 py-2 d-flex align-items-center';
			badge.innerHTML = `${p.name}<span class="ms-2" style="cursor:pointer;" title="삭제">&times;</span>`;
			badge.querySelector('span').onclick = () => {
				relatedProducts.splice(idx, 1);
				renderRelatedProducts();
			}
			list.appendChild(badge);
		});
	}
	/* 관련상품 모달 */

	// 스크롤 스타일 적용(최대 5개) - CSS로도 적용 가능하지만 JS로 보장
	[largeList, mediumList, smallList].forEach(listEl => {
		listEl.style.maxHeight = '240px';
		listEl.style.overflowY = 'auto';
	});

	// 대분류 로딩 및 중분류 개수 badge 표시
	fetch('/api/category/list-large')
		.then(res => res.json())
		.then(list => {
			largeList.innerHTML = '';
			list.forEach(large => {
				let li = document.createElement('li');
				li.className = 'list-group-item list-group-item-action category-large-item d-flex justify-content-between align-items-center';
				li.dataset.id = large.id;
				// mediumCount 바로 사용!
				li.innerHTML = `<span>${large.name}</span>
                <span class="badge bg-light text-dark ms-2" data-large-badge="${large.id}">${large.mediumCount ?? 0}</span>`;
				largeCategoryMap[large.id] = large.name;
				largeList.appendChild(li);
			});
		});

	largeList.addEventListener('click', function(e) {
		const li = e.target.closest('.category-large-item');
		if (li) {
			const largeId = li.dataset.id;
			fetch(`/api/category/list-medium?largeId=${largeId}`)
				.then(res => res.json())
				.then(list => {
					mediumList.innerHTML = '';
					list.forEach(m => {
						let li = document.createElement('li');
						li.className = 'list-group-item list-group-item-action category-medium-item d-flex justify-content-between align-items-center';
						li.dataset.id = m.id;
						// smallCount 바로 사용!
						li.innerHTML = `<span>${m.name}</span>
                        <span class="badge bg-light text-dark ms-2" data-medium-badge="${m.id}">${m.smallCount ?? 0}</span>`;
						mediumCategoryMap[m.id] = { name: m.name, largeId: largeId };
						mediumList.appendChild(li);
					});
					smallList.innerHTML = '';
				});
		}
	});

	mediumList.addEventListener('click', function(e) {
		const li = e.target.closest('.category-medium-item');
		if (li) {
			const mediumId = li.dataset.id;
			fetch(`/api/category/list-small?mediumId=${mediumId}`)
				.then(res => res.json())
				.then(list => {
					smallList.innerHTML = '';
					list.forEach(s => {
						let li = document.createElement('li');
						li.textContent = s.name;
						li.className = 'list-group-item list-group-item-action category-small-item';
						li.dataset.id = s.id;
						li.dataset.mediumId = mediumId;
						smallList.appendChild(li);
					});
				});
		}
	});

	smallList.addEventListener('click', function(e) {
		if (e.target.classList.contains('category-small-item')) {
			const smallId = e.target.dataset.id;
			const mediumId = e.target.dataset.mediumId;
			const mediumInfo = mediumCategoryMap[mediumId] || {};
			const largeId = mediumInfo.largeId;
			const largeName = largeCategoryMap[largeId] || '';
			const mediumName = mediumInfo.name || '';
			const smallName = e.target.textContent;
			if (!selectedCategories.some(sc => sc.id == smallId)) {
				selectedCategories.push({
					id: smallId,
					largeId,
					largeName,
					mediumId,
					mediumName,
					smallName
				});
				renderSelectedCategories();
			}
		}
	});

	function renderSelectedCategories() {
		selectedList.innerHTML = '';
		selectedCategories.forEach((c, idx) => {
			let div = document.createElement('div');
			div.className = 'badge bg-primary text-white px-2 py-2 me-2 d-flex align-items-center';
			div.innerHTML =
				`${c.largeName} &gt; ${c.mediumName} &gt; <b>${c.smallName}</b>
				<span class="ms-2" style="cursor:pointer;" title="삭제">[삭제]</span>`;
			div.querySelector('span').onclick = () => {
				selectedCategories.splice(idx, 1);
				renderSelectedCategories();
			}
			selectedList.appendChild(div);
		});
	}

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
							if (typeof opt === 'object' && opt !== null) {
								// 1. value/label 쌍이 있는 경우
								if ('value' in opt && 'label' in opt) {
									return `<option value="${opt.value}">${opt.label}</option>`;
								}
								// 2. 키가 하나뿐인 객체 ({red:'빨강'})
								const keys = Object.keys(opt);
								if (keys.length === 1) {
									return `<option value="${keys[0]}">${opt[keys[0]]}</option>`;
								}
								// 3. value만 있으면 그 값을 표시
								if ('value' in opt) {
									return `<option value="${opt.value}">${opt.value}</option>`;
								}
								// 4. label만 있으면 그 값을 표시
								if ('label' in opt) {
									return `<option value="${opt.label}">${opt.label}</option>`;
								}
								// 5. 빈 객체거나 형태가 맞지 않는 경우
								return `<option disabled>선택지 오류</option>`;
							} else {
								// 문자열(숫자) 등
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
				let colClass = 'col-6 mb-2';
				if (option.type === 'TEXTAREA' || option.type === 'CKEDITOR') {
					colClass = 'col-12 mb-2';
				}
				const div = document.createElement('div');
				div.className = colClass + ' d-flex flex-column justify-content-end';
				div.innerHTML = `
                    <label class="form-label mb-1">${option.label ?? option.name}${option.required ? ' <span class="text-danger">*</span>' : ''}</label>
                    ${makeQuestionInput(option)}
                `;
				container.appendChild(div);
			});
			setTimeout(() => {
				list.filter(opt => opt.type === 'CKEDITOR').forEach(option => {
					const tId = `editor-question-${option.id}`;
					const textarea = document.getElementById(tId);
					if (textarea && !ckeInstances[tId] && window.ClassicEditor) {
						window.ClassicEditor.create(textarea, {
							toolbar: {
								items: [
									'heading', '|', 'bold', 'italic', 'underline', 'strikethrough', 'highlight', 'fontColor', 'fontBackgroundColor',
									'|', 'link', 'bulletedList', 'numberedList', 'blockQuote',
									'|', 'insertTable', 'imageUpload', 'mediaEmbed',
									'|', 'undo', 'redo', 'alignment', 'outdent', 'indent'
								]
							},
							image: {
								toolbar: [
									'imageTextAlternative', 'imageStyle:full', 'imageStyle:side', 'linkImage'
								],
								styles: ['full', 'side'],
								resizeUnit: 'px'
							},
							table: {
								contentToolbar: [
									'tableColumn', 'tableRow', 'mergeTableCells', 'tableCellProperties', 'tableProperties'
								]
							},
							mediaEmbed: {
								previewsInData: true
							},
							fontFamily: {
								options: [
									'default', 'Arial, Helvetica, sans-serif', 'Courier New, Courier, monospace', 'Georgia, serif',
									'Lucida Sans Unicode, Lucida Grande, sans-serif', 'Tahoma, Geneva, sans-serif', 'Times New Roman, Times, serif',
									'Trebuchet MS, Helvetica, sans-serif', 'Verdana, Geneva, sans-serif'
								]
							},
							fontSize: {
								options: ['tiny', 'small', 'default', 'big', 'huge']
							},
							language: 'ko',
							extraPlugins: [CustomUploadAdapterPlugin]
						}).then(editor => { ckeInstances[tId] = editor; })
							.catch(err => console.error('CKEditor5 생성 오류:', err));

					}
				});
			}, 100);
		});

	// === 3. 상세설명(이미지/HTML) 에디터 mount (id: editor-desc) ===
	let detailEditor = null;
	(function() {
		const desc = document.getElementById('editor-desc');
		if (desc && window.ClassicEditor) {
			window.ClassicEditor.create(desc, {
				toolbar: {
					items: [
						'heading', '|', 'bold', 'italic', 'underline', 'strikethrough', 'highlight', 'fontColor', 'fontBackgroundColor',
						'|', 'link', 'bulletedList', 'numberedList', 'blockQuote',
						'|', 'insertTable', 'imageUpload', 'mediaEmbed',
						'|', 'undo', 'redo', 'alignment', 'outdent', 'indent'
					]
				},
				image: {
					toolbar: [
						'imageTextAlternative', 'imageStyle:full', 'imageStyle:side', 'linkImage'
					],
					styles: ['full', 'side'],
					resizeUnit: 'px'
				},
				table: {
					contentToolbar: [
						'tableColumn', 'tableRow', 'mergeTableCells', 'tableCellProperties', 'tableProperties'
					]
				},
				mediaEmbed: {
					previewsInData: true
				},
				fontFamily: {
					options: [
						'default', 'Arial, Helvetica, sans-serif', 'Courier New, Courier, monospace', 'Georgia, serif',
						'Lucida Sans Unicode, Lucida Grande, sans-serif', 'Tahoma, Geneva, sans-serif', 'Times New Roman, Times, serif',
						'Trebuchet MS, Helvetica, sans-serif', 'Verdana, Geneva, sans-serif'
					]
				},
				fontSize: {
					options: ['tiny', 'small', 'default', 'big', 'huge']
				},
				language: 'ko',
				extraPlugins: [CustomUploadAdapterPlugin]
			})
				.then(editor => { detailEditor = editor; })
				.catch(err => console.error('CKEditor5 생성 오류:', err));
		}
	})();

	// ===== 4. 대표/추가 이미지 미리보기/삭제/순서 =====
	const mainInput = document.getElementById('product-manager-main-image');
	const mainPreview = document.getElementById('product-manager-main-image-preview');
	mainInput.addEventListener('change', function() {
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
	subInput.addEventListener('change', function() {
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
		onEnd: function(evt) {
			const oldIndex = evt.oldIndex, newIndex = evt.newIndex;
			if (oldIndex !== newIndex) {
				const moved = subFiles.splice(oldIndex, 1)[0];
				subFiles.splice(newIndex, 0, moved);
				renderSubImagePreview();
			}
		}
	});

	// ===== 5. 추가 입력필드 동적 추가/삭제 =====
	const extraFieldList = document.getElementById('product-manager-extra-field-list');
	const addExtraFieldBtn = document.getElementById('product-manager-add-extra-field');
	let extraFields = [];
	function syncExtraFieldsFromDOM() {
		const rows = extraFieldList.querySelectorAll('.input-group');
		extraFields = [];
		rows.forEach((row) => {
			const label = row.querySelector('input[name$=".label"]')?.value || '';
			const value = row.querySelector('input[name$=".value"]')?.value || '';
			extraFields.push({ label, value });
		});
	}
	function renderExtraFields() {
		extraFieldList.innerHTML = '';
		extraFields.forEach((field, idx) => {
			let row = document.createElement('div');
			row.className = 'input-group mb-2';
			row.innerHTML = `
	            <input type="text" class="form-control form-control-sm" name="extraFields[${idx}].label" placeholder="질문명" value="${field.label || ''}" required>
	            <input type="text" class="form-control form-control-sm" name="extraFields[${idx}].value" placeholder="답변값" value="${field.value || ''}" required>
	            <button type="button" class="btn btn-outline-danger btn-sm" title="삭제">×</button>
	        `;
			// 입력시 배열 동기화
			row.querySelectorAll('input').forEach(input => {
				input.addEventListener('input', syncExtraFieldsFromDOM);
			});
			// 삭제
			row.querySelector('button').onclick = () => {
				syncExtraFieldsFromDOM();
				extraFields.splice(idx, 1);
				renderExtraFields();
			}
			extraFieldList.appendChild(row);
		});
	}
	addExtraFieldBtn.addEventListener('click', function() {
		syncExtraFieldsFromDOM();
		extraFields.push({ label: '', value: '' });
		renderExtraFields();
	});
	// 최초 1개 표시
	renderExtraFields();

	function syncOptionGroupsFromDOM() {
		const groupCards = optionGroupList.querySelectorAll('.card');
		optionGroups.forEach((group, groupIdx) => {
			const groupCard = groupCards[groupIdx];
			if (!groupCard) return;
			group.name = groupCard.querySelector(`[name="optionGroups[${groupIdx}].name"]`)?.value || '';
			const optionRows = groupCard.querySelectorAll('.input-group.mb-1');
			group.options.forEach((opt, optIdx) => {
				const row = optionRows[optIdx];
				if (!row) return;
				opt.name = row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].name"]`)?.value || '';
				opt.value = row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].value"]`)?.value || '';
				opt.extraPrice = row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].extraPrice"]`)?.value || '';
				opt.sign = row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].sign"]`)?.value || 'PLUS';
				opt.sortOrder = row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].sortOrder"]`)?.value || (optIdx + 1);
			});
		});
	}


	// ===== 6. 옵션그룹/옵션 동적 추가/삭제 =====
	const optionGroupList = document.getElementById('product-manager-option-group-list');
	const addOptionGroupBtn = document.getElementById('product-manager-add-option-group');
	let optionGroups = [];
	function renderOptionGroups() {
		optionGroupList.innerHTML = '';
		optionGroups.forEach((group, groupIdx) => {
			let groupDiv = document.createElement('div');
			groupDiv.className = 'card mb-2';
			groupDiv.innerHTML = `
			<div class="card-body p-2">
				<div class="input-group mb-2">
					<input type="text" class="form-control form-control-sm" name="optionGroups[${groupIdx}].name" placeholder="옵션 그룹명" value="${group.name || ''}" required>
					<button type="button" class="btn btn-outline-danger btn-sm" title="옵션그룹 삭제">×</button>
				</div>
				<div id="option-group-options-${groupIdx}"></div>
				<button type="button" class="btn btn-outline-primary btn-sm mt-1" data-group-idx="${groupIdx}">+ 옵션 추가</button>
			</div>
		`;
			// 옵션그룹 삭제
			groupDiv.querySelector('.btn-outline-danger').onclick = () => {
				syncOptionGroupsFromDOM();
				optionGroups.splice(groupIdx, 1);
				renderOptionGroups();
			};
			// 옵션 추가
			groupDiv.querySelector('.btn-outline-primary').onclick = (e) => {
				syncOptionGroupsFromDOM();
				group.options.push({
					name: '', value: '', extraPrice: '', sign: 'PLUS', sortOrder: group.options.length + 1
				});
				renderOptionGroups();
			};
			// 그룹명 input 값이 변경될 때 동기화
			groupDiv.querySelector(`[name="optionGroups[${groupIdx}].name"]`)
				.addEventListener('input', syncOptionGroupsFromDOM);

			// 옵션 리스트
			const optionsContainer = groupDiv.querySelector(`#option-group-options-${groupIdx}`);
			group.options.forEach((opt, optIdx) => {
				let optRow = document.createElement('div');
				optRow.className = 'input-group mb-1';
				optRow.innerHTML = `
				<input type="text" class="form-control form-control-sm" name="optionGroups[${groupIdx}].options[${optIdx}].name" placeholder="옵션명" value="${opt.name || ''}" required>
				<input type="text" class="form-control form-control-sm" name="optionGroups[${groupIdx}].options[${optIdx}].value" placeholder="값" value="${opt.value || ''}">
				<input type="number" class="form-control form-control-sm" name="optionGroups[${groupIdx}].options[${optIdx}].extraPrice" placeholder="추가금액" value="${opt.extraPrice || ''}">
				<select class="form-select form-select-sm" name="optionGroups[${groupIdx}].options[${optIdx}].sign">
					<option value="PLUS" ${opt.sign === 'PLUS' ? 'selected' : ''}>+</option>
					<option value="MINUS" ${opt.sign === 'MINUS' ? 'selected' : ''}>-</option>
				</select>
				<input type="number" class="form-control form-control-sm" name="optionGroups[${groupIdx}].options[${optIdx}].sortOrder" placeholder="정렬" value="${opt.sortOrder || optIdx + 1}">
				<button type="button" class="btn btn-outline-danger btn-sm" title="옵션 삭제">×</button>
			`;
				// 옵션 삭제
				optRow.querySelector('.btn-outline-danger').onclick = () => {
					syncOptionGroupsFromDOM();
					group.options.splice(optIdx, 1);
					renderOptionGroups();
				};
				// 각 입력란 값 변경시 동기화 이벤트 바인딩
				optRow.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].name"]`)
					.addEventListener('input', syncOptionGroupsFromDOM);
				optRow.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].value"]`)
					.addEventListener('input', syncOptionGroupsFromDOM);
				optRow.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].extraPrice"]`)
					.addEventListener('input', syncOptionGroupsFromDOM);
				optRow.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].sign"]`)
					.addEventListener('change', syncOptionGroupsFromDOM);
				optRow.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].sortOrder"]`)
					.addEventListener('input', syncOptionGroupsFromDOM);

				optionsContainer.appendChild(optRow);
			});
			optionGroupList.appendChild(groupDiv);
		});
	}

	addOptionGroupBtn.addEventListener('click', function() {
		syncOptionGroupsFromDOM();
		optionGroups.push({
			name: '',
			options: []
		});
		renderOptionGroups();
	});
	// 최초 1개 표시
	renderOptionGroups();
	// 키워드 등록
	function renderKeywordList() {
		keywordList.innerHTML = '';
		keywords.forEach((kw, idx) => {
			const badge = document.createElement('div');
			badge.className = 'badge bg-info text-dark px-2 py-2 d-flex align-items-center mt-2';
			badge.style.fontSize = '14px';
			badge.innerHTML = `
            <span>${kw}</span>
            <span class="ms-2" style="cursor:pointer;" title="삭제">&times;</span>
        `;
			badge.querySelector('span:last-child').onclick = () => {
				keywords.splice(idx, 1);
				renderKeywordList();
			};
			keywordList.appendChild(badge);
		});
	}
	addKeywordBtn.onclick = addKeyword;
	keywordInput.onkeydown = function(e) {
		if (e.key === 'Enter') {
			addKeyword();
			e.preventDefault();
		}
	};
	function addKeyword() {
		const kw = keywordInput.value.trim();
		if (!kw) return;
		if (keywords.includes(kw)) return; // 중복제거
		keywords.push(kw);
		keywordInput.value = '';
		renderKeywordList();
	}

	// === 사용함/사용안함 이벤트 ===
	document.querySelectorAll('input[name="useRelatedProducts"]').forEach(radio => {
		radio.onchange = function() {
			document.getElementById('open-related-modal-btn').disabled = (this.value === "false");
		}
	});
	document.querySelectorAll('input[name="useBundleItems"]').forEach(radio => {
		radio.onchange = function() {
			document.getElementById('open-bundle-modal-btn').disabled = (this.value === "false");
		}
	});

	function renderRelatedProducts() {
		const list = document.getElementById('related-products-list');
		list.innerHTML = '';
		relatedProducts.forEach((p, idx) => {
			const badge = document.createElement('div');
			badge.className = 'badge bg-warning text-dark px-2 py-2 d-flex align-items-center';
			badge.innerHTML = `${p.name} (${p.type === 'RECIPROCAL' ? '상호' : '일방'})<span class="ms-2" style="cursor:pointer;" title="삭제">&times;</span>`;
			badge.querySelector('span').onclick = () => {
				relatedProducts.splice(idx, 1);
				renderRelatedProducts();
			}
			list.appendChild(badge);
		});
	}
	/* 가격 대체문구 사용 Start */
	// 가격대체문구 사용여부 체크시 입력란 show/hide
	const usePriceReplacementText = document.getElementById('usePriceReplacementText');
	const priceReplacementArea = document.getElementById('priceReplacementArea');
	usePriceReplacementText.onchange = function() {
		priceReplacementArea.style.display = this.checked ? '' : 'none';
		// 대체문구 입력값 클리어/disabled 처리도 필요하면 추가
		if (!this.checked) {
			document.getElementById('priceReplacementText').value = '';
		}
	};

	/* 가격 대체문구 사용 End */
	function renderBundleProducts() {
		const list = document.getElementById('bundle-products-list');
		list.innerHTML = '';
		bundleProducts.forEach((p, idx) => {
			const badge = document.createElement('div');
			badge.className = 'badge bg-success text-white px-2 py-2 d-flex align-items-center';
			badge.innerHTML = `${p.name}<span class="ms-2" style="cursor:pointer;" title="삭제">&times;</span>`;
			badge.querySelector('span').onclick = () => {
				bundleProducts.splice(idx, 1);
				renderBundleProducts();
			}
			list.appendChild(badge);
		});
	}

	// === 검증 함수 추가 ===
	async function validateProductForm() {
		// 1. 소분류
		if (!selectedCategories || selectedCategories.length === 0) {
			alert('카테고리를 1개 이상 선택하세요.');
			return false;
		}

		// 2. 기본정보
		const pName = document.getElementById('productName').value.trim();
		const pCode = document.getElementById('productCode').value.trim();
		const displayStatus = document.querySelector('input[name="displayStatus"]:checked')?.value;
		const saleStatus = document.querySelector('input[name="saleStatus"]:checked')?.value;
		if (!pName) {
			alert('제품명을 입력하세요.');
			return false;
		}
		if (!pCode) {
			alert('제품코드를 입력하세요.');
			return false;
		}
		if (!displayStatus) {
			alert('진열상태를 선택하세요.');
			return false;
		}
		if (!saleStatus) {
			alert('판매상태를 선택하세요.');
			return false;
		}

		// 3. 공통표시항목(질문) + CKEditor (editor-question-*)
		let hasQuestionError = false;
		document.querySelectorAll('#product-manager-display-options [name]').forEach(el => {
			const required = el.hasAttribute('required');
			if (el.tagName === 'TEXTAREA' && el.id && el.id.startsWith('editor-question-')) {
				if (required) {
					const editor = ckeInstances[el.id];
					if (!editor || !editor.getData().trim()) {
						hasQuestionError = true;
					}
				}
			} else {
				if (required && !el.value) {
					hasQuestionError = true;
				}
			}
		});
		if (hasQuestionError) {
			alert('필수 공통표시항목(질문/옵션)을 모두 입력하세요.');
			return false;
		}

		// 4. 상세설명(에디터) 필수여부 체크 (editor-desc)
		const descEl = document.getElementById('editor-desc');
		if (descEl && descEl.hasAttribute('required')) {
			let isEmpty = true;
			if (window.ClassicEditor && typeof detailEditor?.getData === 'function') {
				if (detailEditor.getData().trim()) isEmpty = false;
			}
			if (isEmpty) {
				alert('상세설명을 입력하세요.');
				return false;
			}
		}

		// 5. 옵션그룹
		let hasOptionGroupError = false;
		optionGroups.forEach((group, groupIdx) => {
			// 그룹명 콘솔
			console.log(`[옵션그룹${groupIdx + 1}] 그룹명: "${group.name}"`);
			if (!group.name || group.name.trim() === '') {
				console.warn(`[옵션그룹${groupIdx + 1}] 그룹명이 비어 있습니다.`);
				hasOptionGroupError = true;
			}
			group.options.forEach((opt, optIdx) => {
				// 옵션명 콘솔
				console.log(`  [옵션${optIdx + 1}] 옵션명: "${opt.name}"`);
				if (!opt.name || opt.name.trim() === '') {
					console.warn(`[옵션그룹${groupIdx + 1}] 옵션${optIdx + 1}의 옵션명이 비어 있습니다.`);
					hasOptionGroupError = true;
				}
			});
		});
		if (hasOptionGroupError) {
			alert('옵션그룹/옵션명을 모두 입력하세요.');
			return false;
		}
		// 6. 추가입력필드
		let hasExtraFieldError = false;
		extraFields.forEach(f => {
			if (!f.label || !f.value) {
				hasExtraFieldError = true;
			}
		});
		if (hasExtraFieldError) {
			alert('추가입력필드의 질문명/답변값을 모두 입력하세요.');
			return false;
		}
		return true;
	}

	// [임시 이미지 src 추출] - /upload/temp/ 경로만 추출
	function extractTempImageUrls(html) {
		const imgRegex = /<img[^>]+src="([^">]+)"/g;
		const urls = [];
		let match;
		while ((match = imgRegex.exec(html)) !== null) {
			const src = match[1];
			if (src && src.startsWith('/upload/temp/')) {
				urls.push(src);
			}
		}
		return urls;
	}

	// [base64 이미지 src 추출] - data:image로 시작하는 src만 추출
	function extractBase64ImagesFromHtml(html) {
		const imgRegex = /<img[^>]+src="([^">]+)"/g;
		const base64List = [];
		let match;
		while ((match = imgRegex.exec(html)) !== null) {
			const src = match[1];
			if (src && src.startsWith('data:image')) {
				base64List.push(src);
			}
		}
		return base64List;
	}

	// base64 → Blob 변환 (fetch 업로드 용)
	function base64ToBlob(base64) {
		const arr = base64.split(',');
		const mime = arr[0].match(/:(.*?);/)[1];
		const bstr = atob(arr[1]);
		let n = bstr.length;
		const u8arr = new Uint8Array(n);
		while (n--) u8arr[n] = bstr.charCodeAt(n);
		return new Blob([u8arr], { type: mime });
	}

	// (에디터 최초 등록용) base64 이미지를 임시 폴더로 업로드 후, src 치환
	async function uploadEditorImages(base64List, type, key) {
		const formData = new FormData();
		base64List.forEach((base64, idx) => {
			formData.append('files', base64ToBlob(base64), `${type}_${key}_editorImg${idx}.png`);
		});
		formData.append('type', type);
		formData.append('key', key);
		const res = await fetch('/api/product/editor-images', {
			method: 'POST',
			body: formData
		});
		if (!res.ok) throw new Error('이미지 업로드 실패');
		const data = await res.json();
		if (!data.success || !data.imageUrls) throw new Error('이미지 업로드 실패(서버응답)');
		return data.imageUrls;
	}

	// base64 → 실제 업로드 url로 변환
	function replaceBase64WithUrls(html, base64List, urlList) {
		let newHtml = html;
		base64List.forEach((base64, idx) => {
			newHtml = newHtml.replace(base64, urlList[idx]);
		});
		return newHtml;
	}

	// ========== [CKEditor Custom Upload Adapter: 서버 직행, 현재 에디터 type/key에 따라 폴더구분] ==========
	class CustomUploadAdapter {
		constructor(loader) {
			this.loader = loader;
		}
		upload() {
			return this.loader.file.then(file => {
				return new Promise(async (resolve, reject) => {
					const formData = new FormData();
					formData.append('files', file);
					formData.append('type', window.currentEditorType); // "detailHtml" 또는 "question"
					formData.append('key', window.currentEditorKey);   // "detailHtml" 또는 "question_1"
					try {
						const res = await fetch('/api/product/editor-images', {
							method: 'POST',
							body: formData
						});
						if (!res.ok) return reject(new Error('이미지 업로드 실패'));
						const data = await res.json();
						if (!data.success || !data.imageUrls || data.imageUrls.length === 0)
							return reject(new Error('이미지 업로드 실패(서버응답)'));
						resolve({ default: data.imageUrls[0] }); // 1개 파일만 등록하므로
					} catch (e) {
						reject(e);
					}
				});
			});
		}
		abort() { }
	}

	// [CKEditor 플러그인 등록: 반드시 인스턴스마다 type/key 셋팅]
	function CustomUploadAdapterPlugin(editor) {
		editor.plugins.get('FileRepository').createUploadAdapter = (loader) => {
			// editor.sourceElement는 항상 CKEditor가 붙은 원본 DOM 엘리먼트
			window.currentEditorType = editor.sourceElement.getAttribute('data-type');
			window.currentEditorKey = editor.sourceElement.getAttribute('data-key');
			return new CustomUploadAdapter(loader);
		};
	}


	// ========== [저장버튼 이벤트 핸들러: 기존 콘솔 및 변수, 기능 그대로] ==========
	document.getElementById('submitProductBtn').addEventListener('click', async function(e) {
		e.preventDefault();
		if (!(await validateProductForm())) {
			console.log('[중단] validateProductForm 실패');
			return;
		}
		if (!confirm('등록하시겠습니까?')) {
			console.log('[중단] 등록 확인 취소');
			return;
		}

		const formData = new FormData();
		// ========== [가격정책] ==============
		// 가격정책 값 수집 및 FormData 추가
		const priceExposeTarget = document.querySelector('input[name="priceExposeTarget"]:checked').value;
		formData.append('priceExposeTarget', priceExposeTarget);

		const usePriceReplace = usePriceReplacementText.checked;
		formData.append('usePriceReplacementText', usePriceReplace);

		const priceReplacementText = document.getElementById('priceReplacementText').value.trim();
		if (usePriceReplace && priceReplacementText) {
			formData.append('priceReplacementText', priceReplacementText);
		}
		/* 브랜드 등록 */
		formData.append('brandId', selectedBrand ? selectedBrand.id : '');
		/* 브랜드 등록 */

		/* 할인혜택 */
		formData.append('promotionId', selectedPromotion ? selectedPromotion.id : '');
		/* 할인혜택 */

		// ========== [1. 소분류(카테고리)] ==========
		console.log('========== [1. 소분류(카테고리) 선택] ==========');
		if (!selectedCategories || selectedCategories.length === 0) {
			console.log('선택된 소분류 없음');
		} else {
			console.log(`총 ${selectedCategories.length}개 선택`);
			selectedCategories.forEach(cat => {
				formData.append('categorySmallIds[]', cat.id);
				console.log(`- id=${cat.id} (${cat.largeName} > ${cat.mediumName} > ${cat.smallName})`);
			});
		}

		// ========== [2. 공통표시항목(질문/옵션)] ==========
		console.log('\n========== [2. 공통표시항목(질문/옵션)] ==========');

		let questionCnt = 0;
		document.querySelectorAll('#product-manager-display-options [name]').forEach(el => {
			if (el.type === 'file') {
				if (el.files && el.files.length > 0) {
					Array.from(el.files).forEach((file, fidx) => {
						formData.append(el.name, file);
						console.log(`- ${el.name}[${fidx}] 파일: ${file.name}, ${file.size}byte`);
						questionCnt++;
					});
				} else {
					console.log(`- ${el.name}: 파일 없음`);
				}
			} else if (el.tagName === 'TEXTAREA' && el.id.startsWith('editor-question-')) {
				// CKEditor textarea는 아래에서 처리
			} else {
				formData.append(el.name, el.value);
				console.log(`- ${el.name}: "${el.value}"`);
				questionCnt++;
			}
		});

		// ========== [3. CKEditor 인스턴스별 HTML/이미지 변환 처리] ==========
		let ckeCnt = 0;
		const editorHtmlMap = {};
		for (const [tid, editor] of Object.entries(ckeInstances)) {
			let html = editor.getData();
			const base64List = extractBase64ImagesFromHtml(html);
			if (base64List.length > 0) {
				try {
					let questionId = tid.replace(/^editor-question-/, '');
					const type = tid.startsWith('editor-question-') ? 'question' : 'detailHtml';
					const key = tid.startsWith('editor-question-') ? ('question_' + questionId) : 'detailHtml';
					const urlList = await uploadEditorImages(base64List, type, key);
					html = replaceBase64WithUrls(html, base64List, urlList);
					console.log(`- CKEditor(${tid}): 이미지 ${base64List.length}개 업로드 및 src 교체`);
				} catch (err) {
					alert(`[CKEditor] 이미지 업로드 실패: ${err.message}`);
					console.error(err);
					return;
				}
			}
			const qName = tid.startsWith('editor-') ? tid.replace('editor-', '') : tid;
			formData.append(qName, html);
			editorHtmlMap[qName] = html;
			if (html && html.trim().length > 0) {
				console.log(`- CKEditor(${tid}): 입력됨 (HTML 길이: ${html.length})`);
			} else {
				console.log(`- CKEditor(${tid}): 미입력`);
			}
			ckeCnt++;
		}
		if (questionCnt === 0 && ckeCnt === 0) console.log('질문/공통표시항목 없음');

		// ========== [3. 제품 기본정보] ==========
		console.log('\n========== [3. 제품 기본정보] ==========');
		const pName = document.getElementById('productName')?.value ?? '';
		const pCode = document.getElementById('productCode')?.value ?? '';
		const displayStatus = document.querySelector('input[name="displayStatus"]:checked')?.value ?? '';
		const saleStatus = document.querySelector('input[name="saleStatus"]:checked')?.value ?? '';
		formData.append('productName', pName);
		formData.append('productCode', pCode);
		formData.append('displayStatus', displayStatus);
		formData.append('saleStatus', saleStatus);
		console.log('- 제품명:', pName ? `"${pName}"` : '(미입력)');
		console.log('- 제품코드:', pCode ? `"${pCode}"` : '(미입력)');
		console.log('- 진열상태:', displayStatus || '(미선택)');
		console.log('- 판매상태:', saleStatus || '(미선택)');

		// ========== [4. 대표이미지] ==========
		console.log('\n========== [4. 대표이미지] ==========');
		if (mainInput && mainInput.files && mainInput.files.length > 0) {
			const file = mainInput.files[0];
			formData.append('mainImage', file);
			console.log(`대표이미지: ${file.name} (${file.size}byte)`);
		} else {
			console.log('대표이미지 없음');
		}

		// ========== [5. 추가이미지] ==========
		console.log('\n========== [5. 추가이미지] ==========');
		if (subFiles && subFiles.length > 0) {
			console.log(`총 ${subFiles.length}개`);
			subFiles.forEach((file, idx) => {
				formData.append('subImages[]', file);
				console.log(`- [${idx + 1}] ${file.name} (${file.size}byte)`);
			});
		} else {
			console.log('추가이미지 없음');
		}

		// ========== [6. 상세설명(HTML)] ==========
		console.log('\n========== [6. 상세설명(HTML)] ==========');
		if (detailEditor) {
			let html = detailEditor.getData();
			formData.append('detailHtml', html);
			editorHtmlMap['detailHtml'] = html;
			console.log(html && html.trim().length > 0 ? `입력됨 (HTML 길이: ${html.length})` : '미입력');
		} else {
			console.log('CKEditor 인스턴스 없음');
		}

		// ========== [7~13. 기타 입력필드: 생략 없이 기존 코드 유지] ==========
		// (코드 생략 없이 기존 질문 내용 전체 참고)

		const extraFieldRows = extraFieldList?.querySelectorAll('.input-group');
		if (extraFieldRows && extraFieldRows.length > 0) {
			console.log(`총 ${extraFieldRows.length}개`);
			extraFieldRows.forEach((row, idx) => {
				const label = row.querySelector(`[name="extraFields[${idx}].label"]`)?.value ?? '';
				const value = row.querySelector(`[name="extraFields[${idx}].value"]`)?.value ?? '';
				formData.append(`extraFields[${idx}].label`, label);
				formData.append(`extraFields[${idx}].value`, value);
				console.log(`- [${idx + 1}] 질문명: "${label}" / 답변값: "${value}"`);
			});
		} else {
			console.log('추가입력필드 없음');
		}

		const groupCards = optionGroupList?.querySelectorAll('.card');
		if (groupCards && groupCards.length > 0) {
			console.log(`총 ${groupCards.length}개 그룹`);
			groupCards.forEach((groupDiv, groupIdx) => {
				const groupName = groupDiv.querySelector(`[name="optionGroups[${groupIdx}].name"]`)?.value || '';
				formData.append(`optionGroups[${groupIdx}].name`, groupName);
				console.log(`- 그룹[${groupIdx + 1}] 그룹명: "${groupName}"`);
				const optionRows = groupDiv.querySelectorAll('.input-group.mb-1');
				if (optionRows && optionRows.length > 0) {
					console.log(`  옵션 ${optionRows.length}개`);
					optionRows.forEach((row, optIdx) => {
						const name = row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].name"]`)?.value || '';
						const value = row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].value"]`)?.value || '';
						const extraPrice = row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].extraPrice"]`)?.value || '';
						const sign = row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].sign"]`)?.value || '';
						const sortOrder = row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].sortOrder"]`)?.value || '';
						formData.append(`optionGroups[${groupIdx}].options[${optIdx}].name`, name);
						formData.append(`optionGroups[${groupIdx}].options[${optIdx}].value`, value);
						formData.append(`optionGroups[${groupIdx}].options[${optIdx}].extraPrice`, extraPrice);
						formData.append(`optionGroups[${groupIdx}].options[${optIdx}].sign`, sign);
						formData.append(`optionGroups[${groupIdx}].options[${optIdx}].sortOrder`, sortOrder);
						console.log(`    - 옵션[${optIdx + 1}] 옵션명: "${name}" / 값: "${value}" / 추가금액: "${extraPrice}" / 부호: "${sign}" / 정렬: "${sortOrder}"`);
					});
				} else {
					console.log('  옵션 없음');
				}
			});
		} else {
			console.log('옵션그룹 없음');
		}

		if (keywords && keywords.length > 0) {
			console.log(`총 ${keywords.length}개`);
			keywords.forEach((kw, idx) => {
				formData.append('keywords[]', kw);
				console.log(`- [${idx + 1}] "${kw}"`);
			});
		} else {
			console.log('키워드 없음');
		}

		if (relatedProducts && relatedProducts.length > 0) {
			console.log(`총 ${relatedProducts.length}개`);
			relatedProducts.forEach((p, idx) => {
				formData.append(`relatedProducts[${idx}].id`, p.id);
				formData.append(`relatedProducts[${idx}].type`, p.type);
				console.log(`- [${idx + 1}] id=${p.id} / name="${p.name}" / type=${p.type}`);
			});
		} else {
			console.log('관련상품 없음');
		}

		if (selectedDiscounts && selectedDiscounts.length > 0) {
			console.log(`총 ${selectedDiscounts.length}개`);
			selectedDiscounts.forEach((d, idx) => {
				formData.append(`discounts[${idx}].id`, d.id);
				formData.append(`discounts[${idx}].name`, d.name);
				formData.append(`discounts[${idx}].type`, d.type);
				formData.append(`discounts[${idx}].term`, d.term);
				formData.append(`discounts[${idx}].target`, d.target);
				formData.append(`discounts[${idx}].couponPolicy`, d.couponPolicy);
				formData.append(`discounts[${idx}].startDate`, d.startDate);
				formData.append(`discounts[${idx}].endDate`, d.endDate);
				formData.append(`discounts[${idx}].active`, d.active);
				console.log(`- [${idx + 1}] id=${d.id} / name="${d.name}" / type=${d.type} / term=${d.term} / target=${d.target} / couponPolicy=${d.couponPolicy} / start=${d.startDate} / end=${d.endDate} / active=${d.active}`);
			});
		} else {
			console.log('할인혜택 없음');
		}

		if (bundleProducts && bundleProducts.length > 0) {
			console.log(`총 ${bundleProducts.length}개`);
			bundleProducts.forEach((p, idx) => {
				formData.append('bundleProductIds[]', p.id);
				console.log(`- [${idx + 1}] id=${p.id} / name="${p.name}"`);
			});
		} else {
			console.log('추가구성상품 없음');
		}

		const dealerKeys = dealerDiscounts ? Object.keys(dealerDiscounts) : [];
		if (dealerKeys.length > 0) {
			dealerKeys.forEach(grade => {
				const value = dealerDiscounts[grade];
				formData.append(`dealerDiscounts[${grade}]`, value);
				console.log(`- ${grade} 등급: ${value}%`);
			});
		} else {
			console.log('딜러 등급별 추가할인 없음');
		}

		console.log('\n[= 전체 데이터 수집/콘솔 출력 완료 =]');

		// ========== [1차 상품등록] ==========
		try {
			const res = await fetch('/api/product/insert', {
				method: 'POST',
				body: formData
			});
			if (!res.ok) throw new Error('등록실패');
			const json = await res.json();
			if (!json.success) throw new Error(json.message || '등록 실패');
			alert('제품 등록 성공');
			console.log(json);

			const productId = json.productId;
			if (!productId) {
				alert('상품ID 반환값 없음(서버 응답 확인 필요)');
				console.error('상품ID 반환값 없음(서버 응답 확인 필요)');
				return;
			}

			// ========== [2차 - 에디터 임시이미지 상품폴더로 이동/치환] ==========
			const tempImageMap = {};
			Object.entries(editorHtmlMap).forEach(([key, html]) => {
				const tempImgList = extractTempImageUrls(html);
				if (tempImgList.length > 0) {
					tempImageMap[key] = { html, tempImgList };
				}
			});

			const moveImagePromises = Object.entries(tempImageMap).map(async ([key, val]) => {
				let type, reqKey;
				if (key === "detailHtml") {
					type = "detailHtml";
					reqKey = "detailHtml";
				} else if (key.startsWith("question_") || key.startsWith("question-")) {
					type = "question";
					reqKey = key.replace("question-", "question_");
				} else {
					throw new Error(`지원하지 않는 key: ${key}`);
				}


				const res2 = await fetch(`/api/product/${productId}/move-editor-images`, {
					method: 'POST',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify({
						type: type,
						key: reqKey,
						html: val.html,
						tempImgList: val.tempImgList
					})
				});
				if (!res2.ok) throw new Error(`[${key}] 에디터 이미지 최종저장 실패`);
				const data2 = await res2.json();
				if (data2.newHtml) {
					console.log(`[최종 ${key} HTML]`, data2.newHtml);
				}
				return data2;
			});

			if (moveImagePromises.length > 0) {
				await Promise.all(moveImagePromises);
				console.log('[2차] 에디터 이미지 모두 상품 폴더로 이동 및 HTML src 치환 완료');
			}

		} catch (err) {
			alert('등록 실패: ' + err.message);
			console.error(err);
		}
	});

	renderSelectedDiscounts();
});
