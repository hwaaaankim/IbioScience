/* eslint-disable */
(function() {
	"use strict";

	// =========================
	// 공통 유틸
	// =========================
	const fmt = n => Number(n || 0).toLocaleString();
	const num = s => (typeof s === 'number' ? s : Number(String(s || '').replace(/[^\d]/g, '')) || 0);

	/**
	 * ✅ 개선된 DOM 헬퍼
	 * - $(selector)              : document.querySelector(selector)
	 * - $(elementOrDocument)     : 그대로 반환 (document 포함)
	 * - $(selector, rootElement) : rootElement.querySelector(selector)
	 */
	const $ = (sel, root) => {
		if (!sel) return null;
		if (sel === document || sel === window) return sel;
		if (sel instanceof Element) return sel;
		if (typeof sel === "string") return (root || document).querySelector(sel);
		return null;
	};

	const $$ = (sel, root) => {
		if (!sel || typeof sel !== "string") return [];
		return Array.from((root || document).querySelectorAll(sel));
	};

	// =========================
	// ✅ 배송비 정책 (요구사항 반영)
	// =========================
	const SHIPPING_FEES = {
		"택배배송": 3000,
		"우편배송": 5000,
		"퀵/당일": 20000
	};

	function getLoginMemberId() {
		const v = window.__loginMemberId;
		const n = Number(v);
		if (!isFinite(n) || n <= 0) return null;
		return n;
	}

	function getPaymentSessionKey(memberId) {
		return 'ibio_payment_payload_v1_u' + String(memberId || '');
	}

	function getPaymentInProgressKey(memberId) {
		return 'ibio_payment_in_progress_v1_u' + String(memberId || '');
	}

	function setPaymentInProgress(memberId, enabled) {
		const key = getPaymentInProgressKey(memberId);
		try {
			if (enabled) sessionStorage.setItem(key, '1');
			else sessionStorage.removeItem(key);
		} catch (e) { }
	}

	function isPaymentInProgress(memberId) {
		const key = getPaymentInProgressKey(memberId);
		try { return sessionStorage.getItem(key) === '1'; } catch (e) { }
		return false;
	}

	function confirmAbandonPaymentAndRedirect() {
		const memberId = getLoginMemberId();
		// 로그인 없으면 기존대로
		if (!memberId) {
			location.href = '/';
			return;
		}

		const msg = "현재 결제 진행중 정보가 삭제됩니다.\n계속 진행하시겠습니까?";
		const ok = confirm(msg);
		if (!ok) return; // ✅ 취소하면 그대로 현재 페이지 유지

		// ✅ 확인이면 진행중 플래그 + payload 삭제 후 장바구니 이동
		setPaymentInProgress(memberId, false);
		clearPaymentPayloadSession(memberId);
		redirectToCart();
	}

	function clearPaymentPayloadSession(memberId) {
		const key = getPaymentSessionKey(memberId);
		try { sessionStorage.removeItem(key); } catch (e) { }
	}

	function redirectToCart() {
		const memberId = getLoginMemberId();
		if (!memberId) {
			location.href = '/';
			return;
		}
		location.href = '/customer/cart/' + memberId;
	}

	// =========================
	// state
	// =========================
	const st = {
		memberId: null,
		payload: null,
		memberProfile: null,   // API로 로드
		coupon: null,          // 선택된 쿠폰
		pointAvail: 0,         // member.point
		pointUse: 0,
		shippingMethod: "택배배송",
		shipPay: "선불",
		expectPointRate: 0.05,
		shipFee: 0,            // ✅ 선택된 배송방법에 따라 업데이트
		itemDiscountTotal: 0   // 현재는 0 (추후 프로모션 연동)
	};

	function updateShipFeeFromMethod() {
		st.shipFee = num(SHIPPING_FEES[st.shippingMethod] || 0);
	}

	// =========================
	// payload 로드 & 검증
	// =========================
	function loadPayloadOrRedirect() {
		const memberId = getLoginMemberId();
		if (!memberId) {
			alert('로그인이 필요합니다.');
			location.href = '/';
			return;
		}
		st.memberId = memberId;

		const key = getPaymentSessionKey(memberId);
		let raw = null;
		try {
			raw = sessionStorage.getItem(key);
		} catch (e) { }

		if (!raw) {
			// ✅ 결제 진행중 플래그가 있으면, 새로고침/이탈로 인한 상황일 가능성
			if (isPaymentInProgress(memberId)) {
				confirmAbandonPaymentAndRedirect();
				return;
			}

			// 진행중도 아닌데 payload가 없으면 기존대로 장바구니
			redirectToCart();
			return;
		}

		let payload = null;
		try {
			payload = JSON.parse(raw);
		} catch (e) {
			try { sessionStorage.removeItem(key); } catch (_) { }
			redirectToCart();
			return;
		}

		// 기본 검증
		if (!payload || Number(payload.userId) !== Number(memberId) || !Array.isArray(payload.items) || payload.items.length === 0) {
			try { sessionStorage.removeItem(key); } catch (_) { }
			redirectToCart();
			return;
		}

		// =========================
		// ✅ 가격/수량 정규화 (중요)
		// - 문제 원인: payload의 unitPrice가 "단가"가 아니라 "라인합계(단가*수량)"로 들어오는 경우가 있음.
		// - 해결: linePrice가 존재하면 unitPrice를 linePrice/quantity로 보정하여 항상 단가로 통일.
		// - 주의: linePrice 없이 unitPrice만 있는 경우는 '합계인지 단가인지' 확정 불가 → 추측 보정 금지.
		// =========================
		payload.items.forEach(it => {
			const qty = Math.max(1, num(it.quantity));
			const rawUnit = num(it.unitPrice);
			const rawLine = num(it.linePrice);

			let unitPrice = rawUnit;

			// linePrice가 있으면 그 값을 기준으로 단가를 확정 가능
			if (rawLine > 0) {
				const derivedUnit = Math.round(rawLine / qty);

				// 1) unitPrice가 비어있거나
				// 2) unitPrice*qty 와 linePrice가 다르거나
				// 3) unitPrice가 linePrice와 같고 qty>1(= unitPrice에 합계가 들어온 전형적인 케이스)
				// -> 단가를 derivedUnit로 보정
				const mismatch = Math.abs(rawLine - (rawUnit * qty)) > 0;
				const looksLikeLineInUnit = (qty > 1 && rawUnit === rawLine);

				if (!rawUnit || mismatch || looksLikeLineInUnit) {
					unitPrice = derivedUnit;
				}
			}

			it.quantity = qty;
			it.unitPrice = num(unitPrice);
			it.linePrice = it.unitPrice * it.quantity;
		});

		st.payload = payload;
	}

	// =========================
	// 서버 API
	// =========================
	async function apiGetJson(url) {
		const res = await fetch(url, { method: 'GET', headers: { 'Accept': 'application/json' } });
		if (!res.ok) {
			throw new Error('API 실패: ' + res.status);
		}
		return await res.json();
	}

	async function loadMemberProfile() {
		// ✅ 회원정보 동일 체크 시 채우기 위해 미리 로드
		const data = await apiGetJson('/api/customer/auth/member/me');
		st.memberProfile = data;
		st.pointAvail = num(data.point || 0);

		const pointAvailEl = $("#paymentSuccess-pointAvail");
		if (pointAvailEl) pointAvailEl.textContent = fmt(st.pointAvail);
	}

	// =========================
	// 렌더링: 주문 상품 테이블
	// =========================
	function escapeHtml(str) {
		str = (str === undefined || str === null) ? '' : String(str);
		return str
			.replace(/&/g, '&amp;')
			.replace(/</g, '&lt;')
			.replace(/>/g, '&gt;')
			.replace(/"/g, '&quot;')
			.replace(/'/g, '&#39;');
	}

	function buildOrderRowHtml(item) {
		const img = item.productImageUrl || '/front/image/sample/100-100.png';
		const name = item.productName || '-';
		const productId = (item.productId != null ? String(item.productId) : '-');

		const optParts = [];
		if (item.optionGroupName) optParts.push(item.optionGroupName);
		if (item.optionName) optParts.push(item.optionName);
		if (item.optionCode) optParts.push(item.optionCode);
		const optLabel = optParts.length ? optParts.join(' / ') : '-';

		const unitText = item.unit || '-';
		const unitPrice = num(item.unitPrice); // ✅ 반드시 "단가"로 통일된 값
		const qty = Math.max(1, num(item.quantity));

		// ✅ 배송비는 "주문 단위"로 별도 적용 (요약/최종금액에 포함)
		// 테이블 행 배송비는 0으로 유지 (혼동 방지)
		const ship = 0;

		const point = 0;      // 적립금 표기는 현재 0
		const discount = 0;   // 현재 0
		const subtotal = unitPrice * qty + ship - discount;

		return `
		<tr data-key="${item.cartEntryId}::${item.optIndex}">
			<td data-label="상품정보">
				<div class="paymentSuccess-prod">
					<img src="${img}" alt="상품" class="paymentSuccess-prod-thumb">
					<div class="paymentSuccess-prod-info">
						<div class="paymentSuccess-prod-name">${escapeHtml(name)}</div>
						<div class="paymentSuccess-prod-sku text-muted">Product ID ${escapeHtml(productId)} / 옵션 ${escapeHtml(optLabel)} / 단위 ${escapeHtml(unitText)}</div>
					</div>
				</div>
			</td>
			<td class="text-end" data-label="판매가">
				<div class="paymentSuccess-cellVal">
					<span class="ps-unitPrice" data-value="${unitPrice}">${fmt(unitPrice)}</span><span>원</span>
				</div>
			</td>
			<td class="text-end" data-label="배송비">
			  	<div class="paymentSuccess-cellVal"><span class="ps-shipText">(공통)</span></div>
			</td>
			<td class="text-end" data-label="적립금">
				<div class="paymentSuccess-cellVal"><span class="ps-point" data-value="${point}">${fmt(point)}</span><span>원</span></div>
			</td>
			<td class="text-end" data-label="수량">
				<div class="paymentSuccess-qty">
					<button type="button" class="btn btn-sm btn-light paymentSuccess-qty-minus" aria-label="수량 감소">-</button>
					<input type="text" class="form-control paymentSuccess-qty-input" value="${qty}" inputmode="numeric" pattern="[0-9]*">
					<button type="button" class="btn btn-sm btn-light paymentSuccess-qty-plus" aria-label="수량 증가">+</button>
				</div>
			</td>
			<td class="text-end" data-label="총 할인금액">
				<div class="paymentSuccess-cellVal paymentSuccess-cellGroup">
					<span class="ps-discount" data-value="${discount}">${fmt(discount)}</span><span>원</span>
					<button type="button" class="btn btn-xs btn-outline-primary paymentSuccess-discountDetailBtn">내역보기</button>
				</div>
			</td>
			<td class="text-end" data-label="합계금액">
				<div class="paymentSuccess-cellVal">
					<span class="ps-subtotal" data-value="${subtotal}">${fmt(subtotal)}</span><span>원</span>
				</div>
			</td>
		</tr>`;
	}

	function mapPayMethodToEnum(v) {
		if (v === '계좌이체') return 'ACCOUNT_TRANSFER';
		if (v === '신용카드') return 'CREDIT_CARD';
		if (v === 'PSYS(연구비카드)') return 'PSYS';
		return 'ACCOUNT_TRANSFER';
	}

	function mapShippingMethodToEnum(v) {
		if (v === '택배배송') return 'PARCEL';
		if (v === '우편배송') return 'POST';
		if (v === '퀵/당일') return 'QUICK';
		return 'PARCEL';
	}

	function mapShipPayToEnum(v) {
		if (v === '선불') return 'PREPAID';
		if (v === '착불') return 'COLLECT';
		return 'PREPAID';
	}

	function buildCreateOrderRequest() {
		// 주문자
		const ordererName = $("#paymentSuccess-ordererName")?.value || "";
		const ordererPhone = $("#paymentSuccess-ordererPhone")?.value || "";
		const orderSmsAgree = $("#paymentSuccess-orderSmsAgree")?.checked === true;

		// 배송지
		const receiverName = $("#paymentSuccess-receiverName")?.value || "";
		const hp1 = $("#paymentSuccess-hp1")?.value || "";
		const hp2 = $("#paymentSuccess-hp2")?.value || "";
		const hp3 = $("#paymentSuccess-hp3")?.value || "";

		const tel1 = $("#paymentSuccess-tel1")?.value || "";
		const tel2 = $("#paymentSuccess-tel2")?.value || "";
		const tel3 = $("#paymentSuccess-tel3")?.value || "";

		const postcode = $("#paymentSuccess-zip")?.value || "";
		const roadAddress = $("#paymentSuccess-road")?.value || "";
		const detailAddress = $("#paymentSuccess-detail")?.value || "";
		const shippingMemo = $("#paymentSuccess-memo")?.value || "";

		// 배송/결제
		const shippingMethodText = $(".paymentSuccess-shippingMethod")?.value || "택배배송";
		const shipPayText = (document.querySelector(".paymentSuccess-shipPay:checked")?.value) || "선불";
		const payMethodText = (document.querySelector(".paymentSuccess-payMethod:checked")?.value) || "계좌이체";

		const paymentMethod = mapPayMethodToEnum(payMethodText);
		const shippingMethod = mapShippingMethodToEnum(shippingMethodText);
		const shippingPayType = mapShipPayToEnum(shipPayText);

		// 포인트/쿠폰
		const pointUse = Number(st.pointUse || 0);
		const memberCouponId = (st.coupon && st.coupon.memberCouponId) ? Number(st.coupon.memberCouponId) : null;

		// 아이템
		const items = [];
		$$(".paymentSuccess-orderTable tbody tr").forEach((tr, idx) => {
			const origin = st.payload.items[idx];

			const qty = Math.max(1, num(tr.querySelector(".paymentSuccess-qty-input")?.value));
			const unitPrice = num(tr.querySelector(".ps-unitPrice")?.dataset?.value);

			const itemProductType = normalizeItemProductType(origin.productType);
			const isDealer = itemProductType === 'DEALER';

			const rawProductId = origin.productId != null ? Number(origin.productId) : null;
			const rawOptionGroupId = origin.optionGroupId != null ? Number(origin.optionGroupId) : null;
			const rawOptionId = origin.optionId != null ? Number(origin.optionId) : null;

			items.push({
				itemProductType: itemProductType,

				// 명시 필드
				productId: isDealer ? null : rawProductId,
				dealerProductId: isDealer ? rawProductId : null,

				companyOptionGroupId: isDealer ? null : rawOptionGroupId,
				companyOptionId: isDealer ? null : rawOptionId,

				dealerOptionGroupId: isDealer ? rawOptionGroupId : null,
				dealerOptionId: isDealer ? rawOptionId : null,

				// 하위 호환용 공통 필드도 같이 전송
				optionGroupId: rawOptionGroupId,
				optionId: rawOptionId,

				productName: origin.productName || "",
				productImageUrl: origin.productImageUrl || "",
				optionGroupName: origin.optionGroupName || "",
				optionName: origin.optionName || "",
				optionCode: origin.optionCode || "",
				unit: origin.unit || "-",

				unitPrice: unitPrice,
				quantity: qty,
				linePrice: unitPrice * qty
			});
		});

		return {
			userId: Number(st.memberId),
			paymentMethod,
			shippingMethod,
			shippingPayType,

			ordererName,
			ordererPhone,
			orderSmsAgree,

			receiverName,
			hp1, hp2, hp3,
			tel1, tel2, tel3,
			postcode,
			roadAddress,
			detailAddress,
			shippingMemo,

			pointUse,
			memberCouponId,
			items
		};
	}

	async function apiPostJson(url, body) {
		const res = await fetch(url, {
			method: 'POST',
			headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' },
			body: JSON.stringify(body)
		});
		if (!res.ok) {
			const txt = await res.text().catch(() => "");
			throw new Error('API 실패: ' + res.status + ' ' + txt);
		}
		return await res.json();
	}

	async function apiPatch(url) {
		const res = await fetch(url, { method: 'PATCH', headers: { 'Accept': 'application/json' } });
		if (!res.ok) {
			const txt = await res.text().catch(() => "");
			throw new Error('API 실패: ' + res.status + ' ' + txt);
		}
		return await res.json();
	}

	function setTestModalContent(payMethodEnum, orderNo, grand) {
		const payTextEl = $("#paymentTest-payMethodText");
		const bodyEl = $("#paymentTest-bodyText");
		const orderNoEl = $("#paymentTest-orderNo");
		const grandEl = $("#paymentTest-grand");

		let text = "";
		if (payMethodEnum === "ACCOUNT_TRANSFER") text = "계좌이체 결제(테스트) 안내 문구가 표시됩니다.";
		if (payMethodEnum === "CREDIT_CARD") text = "신용카드 결제(테스트) 안내 문구가 표시됩니다.";
		if (payMethodEnum === "PSYS") text = "PSYS(연구비카드) 결제(테스트) 안내 문구가 표시됩니다.";

		if (payTextEl) payTextEl.textContent = payMethodEnum;
		if (bodyEl) bodyEl.textContent = text;
		if (orderNoEl) orderNoEl.textContent = orderNo || "-";
		if (grandEl) grandEl.textContent = fmt(grand || 0);
	}

	function openPaymentTestModal(orderNo, payMethodEnum) {
		const modal = $("#paymentTestModal");
		if (!modal) return;

		// 현재 우측 요약 grand 값
		const grand = num(document.querySelector('.paymentSuccess-summary [data-bind="grand"]')?.textContent);

		setTestModalContent(payMethodEnum, orderNo, grand);

		// 버튼 바인딩(중복 방지 위해 onclick 대체)
		const btnError = $("#paymentTest-btnError");
		const btnSuccess = $("#paymentTest-btnSuccess");

		if (btnError) {
			btnError.onclick = async () => {
				try {
					await apiPatch(`/customer/api/orders/${encodeURIComponent(orderNo)}/payment-error`);
					closeModal(modal);
					// 진행중 플래그/세션 제거
					setPaymentInProgress(st.memberId, false);
					clearPaymentPayloadSession(st.memberId);
					location.href = `/customer/paymentEnd/${encodeURIComponent(orderNo)}`;
				} catch (e) {
					alert("결제에러 처리에 실패했습니다.");
					console.error(e);
				}
			};
		}

		if (btnSuccess) {
			btnSuccess.onclick = async () => {
				try {
					await apiPatch(`/customer/api/orders/${encodeURIComponent(orderNo)}/payment-complete`);
					closeModal(modal);
					setPaymentInProgress(st.memberId, false);
					clearPaymentPayloadSession(st.memberId);
					location.href = `/customer/paymentEnd/${encodeURIComponent(orderNo)}`;
				} catch (e) {
					alert("결제완료 처리에 실패했습니다.");
					console.error(e);
				}
			};
		}

		wireModal(modal);
		openModal(modal);
	}

	function renderOrderTable() {
		const tbody = document.querySelector(".paymentSuccess-orderTable tbody");
		if (!tbody) return;

		tbody.innerHTML = "";
		st.payload.items.forEach(it => {
			tbody.insertAdjacentHTML("beforeend", buildOrderRowHtml(it));
		});
	}

	// =========================
	// 계산(쿠폰/포인트/배송 포함)
	// =========================
	function calcSumPrice() {
		let sum = 0;
		$$(".paymentSuccess-orderTable tbody tr").forEach(tr => {
			const unitPrice = num(tr.querySelector(".ps-unitPrice")?.dataset?.value);
			const qty = Math.max(1, num(tr.querySelector(".paymentSuccess-qty-input")?.value));
			sum += unitPrice * qty;
		});
		return sum;
	}

	function couponDiscount(sumPrice) {
		if (!st.coupon) return 0;

		const min = num(st.coupon.minPurchaseAmount);
		if (sumPrice < min) return 0;

		// 현재 엔티티 기준: couponAmount(금액형)
		return num(st.coupon.couponAmount);
	}

	function recalc() {
		updateShipFeeFromMethod();

		const sumPrice = calcSumPrice();
		const baseDiscount = 0;
		const couponDc = couponDiscount(sumPrice);

		const pointDc = Math.min(num(st.pointUse), num(st.pointAvail));

		// ✅ 배송비는 항상 '정책상 존재'하지만, 결제금액(grand)에 포함 여부는 선불/착불로 결정
		const shipFee = num(st.shipFee);
		const includeShip = (st.shipPay === '선불');
		const shipToPayNow = includeShip ? shipFee : 0;

		const grand = Math.max(0, sumPrice - baseDiscount - couponDc - pointDc + shipToPayNow);

		// ✅ 우측 요약의 "할인금액"은 적립금 포함(기존 요구사항 유지)
		const totalDiscountForSummary = Math.max(0, baseDiscount + pointDc);

		// 상단 Total
		const grandEl = document.querySelector('.paymentSuccess-orderTable-total [data-bind="grand"]');
		if (grandEl) grandEl.textContent = fmt(grand);

		// 우측 요약
		const sumPriceEl = document.querySelector('[data-bind="sumPrice"]');
		if (sumPriceEl) sumPriceEl.textContent = fmt(sumPrice);

		const sumShipEl = document.querySelector('[data-bind="sumShip"]');
		if (sumShipEl) sumShipEl.textContent = fmt(shipFee); // ✅ 배송비 자체는 표시(공통)

		const sumDiscountEl = document.querySelector('[data-bind="sumDiscount"]');
		if (sumDiscountEl) sumDiscountEl.textContent = fmt(totalDiscountForSummary);

		const couponDiscountEl = document.querySelector('[data-bind="couponDiscount"]');
		if (couponDiscountEl) couponDiscountEl.textContent = fmt(couponDc);

		// 총 n건(라인 수)
		const totalCountEl = document.querySelector('[data-bind="totalCount"]');
		if (totalCountEl) {
			const rowCount = $$(".paymentSuccess-orderTable tbody tr").length;
			totalCountEl.textContent = String(rowCount);
		}

		// 우측 grand
		const rightGrandEl = document.querySelector('.paymentSuccess-summary [data-bind="grand"]');
		if (rightGrandEl) rightGrandEl.textContent = fmt(grand);

		// ✅ 예상 적립 포인트(표시용): 제품별(라인별) 합산 = sumPrice * 10% (간단 계산)
		// 서버 저장은 OrderItem 단위로 저장되므로, 여기 표시는 참고값으로 동일하게 맞춥니다.
		const expectPointEl = document.querySelector('[data-bind="expectPoint"]');
		if (expectPointEl) expectPointEl.textContent = fmt(Math.floor(sumPrice * st.expectPointRate));

		// 할인내역 모달
		const list = $("#paymentSuccess-discountList");
		if (list) {
			const shipText = includeShip ? `+ ${fmt(shipFee)} 원` : `+ ${fmt(shipFee)} 원 (착불)`;
			list.innerHTML = `
      <li class="d-flex justify-content-between"><span>기본 프로모션</span><span>- ${fmt(baseDiscount)} 원</span></li>
      <li class="d-flex justify-content-between"><span>쿠폰 할인</span><span>- ${fmt(couponDc)} 원</span></li>
      <li class="d-flex justify-content-between"><span>적립금 사용</span><span>- ${fmt(pointDc)} 원</span></li>
      <li class="d-flex justify-content-between"><span>배송비</span><span>${shipText}</span></li>
      <li class="paymentSuccess-divider-soft" style="list-style:none; margin:10px 0;"></li>
      <li class="d-flex justify-content-between fw-bold"><span>최종 결제금액</span><span>${fmt(grand)} 원</span></li>
    `;
		}
	}

	// =========================
	// 수량 이벤트: 행 단위로 동작 + recalc
	// =========================
	function attachQtyEvents() {
		$(document).addEventListener('click', (e) => {
			const minusBtn = e.target.closest('.paymentSuccess-qty-minus');
			const plusBtn = e.target.closest('.paymentSuccess-qty-plus');

			if (!minusBtn && !plusBtn) return;

			const tr = e.target.closest('tr');
			if (!tr) return;

			const input = tr.querySelector('.paymentSuccess-qty-input');
			if (!input) return;

			let v = Math.max(1, num(input.value));
			if (minusBtn) v = Math.max(1, v - 1);
			if (plusBtn) v = Math.max(1, v + 1);

			input.value = String(v);

			// subtotal 갱신 (행 단위)
			const unitPrice = num(tr.querySelector('.ps-unitPrice')?.dataset?.value);
			const ship = num(tr.querySelector('.ps-ship')?.dataset?.value);
			const discount = num(tr.querySelector('.ps-discount')?.dataset?.value);
			const subtotal = unitPrice * v + ship - discount;

			const subEl = tr.querySelector('.ps-subtotal');
			if (subEl) {
				subEl.dataset.value = String(subtotal);
				subEl.textContent = fmt(subtotal);
			}

			recalc();
		});

		// 직접 입력(테이블 단위 바인딩)
		$$('.paymentSuccess-orderTable').forEach(tbl => {
			tbl.addEventListener('input', (e) => {
				const input = e.target.closest('.paymentSuccess-qty-input');
				if (!input) return;

				const tr = e.target.closest('tr');
				if (!tr) return;

				let v = Math.max(1, num(input.value));
				input.value = String(v);

				const unitPrice = num(tr.querySelector('.ps-unitPrice')?.dataset?.value);
				const ship = num(tr.querySelector('.ps-ship')?.dataset?.value);
				const discount = num(tr.querySelector('.ps-discount')?.dataset?.value);
				const subtotal = unitPrice * v + ship - discount;

				const subEl = tr.querySelector('.ps-subtotal');
				if (subEl) {
					subEl.dataset.value = String(subtotal);
					subEl.textContent = fmt(subtotal);
				}

				recalc();
			});
		});
	}

	// =========================
	// 전화번호 자동 이동
	// =========================
	function attachPhoneAutoTab() {
		$$(".paymentSuccess-phone").forEach((el, idx, arr) => {
			el.addEventListener("input", (e) => {
				e.target.value = e.target.value.replace(/[^\d]/g, "");
				const max = Number(e.target.getAttribute("maxlength")) || 4;
				if (e.target.value.length >= max && arr[idx + 1]) arr[idx + 1].focus();
			});
			el.addEventListener("keydown", (e) => {
				if (e.key === "Backspace" && e.target.value.length === 0 && arr[idx - 1]) arr[idx - 1].focus();
			});
		});
	}

	// =========================
	// 적립금 제한
	// =========================
	function attachPointLimit() {
		const input = $("#paymentSuccess-pointUse");
		if (!input) return;

		input.addEventListener("input", (e) => {
			let v = num(e.target.value);
			if (v > st.pointAvail) v = st.pointAvail;

			// 화면엔 천단위 콤마, 내부 st.pointUse는 숫자 보관
			e.target.value = v ? fmt(v) : "";
			st.pointUse = v;

			recalc();
		});
	}

	// =========================
	// 주소 검색(daum.Postcode)
	// =========================
	function attachFindAddress() {
		const btn = $("#paymentSuccess-findAddrBtn");
		if (!btn) return;

		btn.addEventListener("click", () => {
			if (typeof daum === "undefined" || !daum.Postcode) {
				alert("주소 검색 스크립트(daum.Postcode)가 로드되지 않았습니다.");
				return;
			}

			new daum.Postcode({
				oncomplete: function(data) {
					const roadAddr = data.roadAddress || "";
					const zonecode = data.zonecode || "";
					const zipEl = $("#paymentSuccess-zip");
					const roadEl = $("#paymentSuccess-road");
					const detailEl = $("#paymentSuccess-detail");

					if (zipEl) zipEl.value = zonecode;
					if (roadEl) roadEl.value = roadAddr;
					if (detailEl) detailEl.focus();
				}
			}).open();
		});
	}

	// =========================
	// ✅ 회원정보 동일 체크 (mobile: 010-1234-1234 형태도 정확 분리)
	// =========================
	function splitPhone(mobile) {
		// ✅ 하이픈 포함 저장되어도 숫자만 추출해서 분리
		const p = String(mobile || '').replace(/[^\d]/g, '');
		// 01012341234 (11자리) 기준
		if (p.length < 10) return { p1: '', p2: '', p3: '' };

		// 010 + 4 + 4 우선
		if (p.length >= 11) {
			return { p1: p.slice(0, 3), p2: p.slice(3, 7), p3: p.slice(7, 11) };
		}
		// 예외: 10자리 등 (지역번호 등)
		return { p1: p.slice(0, 3), p2: p.slice(3, 6), p3: p.slice(6, 10) };
	}

	function fillShippingFromMember() {
		const m = st.memberProfile;
		if (!m) return;

		const receiverName = $("#paymentSuccess-receiverName");
		if (receiverName) receiverName.value = m.name || "";

		// ✅ mobile "010-1234-1234" 도 정상 분리
		const ph = splitPhone(m.mobile);
		const hp1 = $("#paymentSuccess-hp1");
		const hp2 = $("#paymentSuccess-hp2");
		const hp3 = $("#paymentSuccess-hp3");
		if (hp1) hp1.value = ph.p1;
		if (hp2) hp2.value = ph.p2;
		if (hp3) hp3.value = ph.p3;

		// ✅ Address 매핑
		const zip = $("#paymentSuccess-zip");
		const road = $("#paymentSuccess-road");
		const detail = $("#paymentSuccess-detail");
		if (zip) zip.value = (m.address && m.address.postcode) ? m.address.postcode : "";
		if (road) road.value = (m.address && m.address.roadAddress) ? m.address.roadAddress : "";
		if (detail) detail.value = (m.address && m.address.detailAddress) ? m.address.detailAddress : "";

		// 유선번호(선택)
		if (m.tel) {
			const t = String(m.tel).replace(/[^\d]/g, '');
			if (t.length >= 9) {
				const tel1 = $("#paymentSuccess-tel1");
				const tel2 = $("#paymentSuccess-tel2");
				const tel3 = $("#paymentSuccess-tel3");
				if (tel1) tel1.value = t.slice(0, 3);
				if (tel2) tel2.value = t.slice(3, 7);
				if (tel3) tel3.value = t.slice(7, 11);
			}
		}
	}

	function clearShippingFields() {
		const ids = [
			"paymentSuccess-receiverName",
			"paymentSuccess-hp1",
			"paymentSuccess-hp2",
			"paymentSuccess-hp3",
			"paymentSuccess-zip",
			"paymentSuccess-road",
			"paymentSuccess-detail",
			"paymentSuccess-tel1",
			"paymentSuccess-tel2",
			"paymentSuccess-tel3"
		];

		ids.forEach(id => {
			const el = $("#" + id);
			if (el) el.value = "";
		});
	}

	function attachSameAsOrderer() {
		const chk = $("#paymentSuccess-sameAsOrderer");
		if (!chk) return;

		chk.addEventListener("change", (e) => {
			if (e.target.checked) {
				fillShippingFromMember();
			} else {
				clearShippingFields();
			}
		});
	}

	// =========================
	// 약관/결제 (결제 연동은 아직 없음)
	// =========================
	function attachTerms() {
		const btn = $("#paymentSuccess-payBtn");
		const terms = $("#paymentSuccess-terms");
		const termsLink = $(".paymentSuccess-termsLink");

		if (btn && terms) {
			terms.addEventListener("change", (e) => { btn.disabled = !e.target.checked; });

			btn.addEventListener("click", async () => {

				// 1) confirm -> 주문 생성(PAYMENT_PENDING 저장)
				const ok = confirm("결제를 진행하시겠습니까?\n확인 시 주문이 생성되며 결제대기 상태로 저장됩니다.");
				if (!ok) return;

				try {
					// 최신 계산 반영
					recalc();

					const req = buildCreateOrderRequest();

					// 서버 저장
					const created = await apiPostJson('/customer/api/orders', req);
					const orderNo = created.orderNo;
					const status = created.status;

					if (!orderNo) {
						alert("주문 생성에 실패했습니다.(orderNo 누락)");
						return;
					}

					// 2) 테스트 결제 팝업 오픈(결제수단별)
					openPaymentTestModal(orderNo, req.paymentMethod);

				} catch (e) {
					console.error(e);
					alert("주문 생성에 실패했습니다.\n입력값을 확인해주세요.");
				}
			});

		}

		if (termsLink) {
			termsLink.addEventListener("click", () => alert("약관 상세 페이지 또는 모달로 연결"));
		}
	}

	// =========================
	// 바닐라 모달 공통
	// =========================
	function openModal(el) {
		if (!el) return;
		el.classList.add("is-open");
		el.setAttribute("aria-hidden", "false");
	}
	function closeModal(el) {
		if (!el) return;
		el.classList.remove("is-open");
		el.setAttribute("aria-hidden", "true");
	}
	function wireModal(el) {
		if (!el) return;
		el.addEventListener("click", (e) => {
			if (e.target === el || e.target.hasAttribute("data-close")) closeModal(el);
		});
	}

	// 할인내역 모달
	function attachDiscountDetail() {
		const modal = $("#paymentSuccess-discountModal");
		if (!modal) return;

		wireModal(modal);

		$(document).addEventListener("click", (e) => {
			if (e.target.closest(".paymentSuccess-discountDetailBtn")) {
				// ✅ 열기 직전에 recalc 한번 더(모바일 포함 항상 최신)
				recalc();
				openModal(modal);
			}
		});
	}

	// =========================
	// ✅ 쿠폰 모달
	// =========================
	function couponTemplate(c) {
		return `
		<li class="list-group-item">
			<input class="form-check-input paymentSuccess-cpnCheck" type="checkbox" value="${c.memberCouponId}" />
			<div class="paymentSuccess-cpnItem">
				<div class="paymentSuccess-cpnName">${escapeHtml(c.couponName)} <span class="text-muted">(${escapeHtml(c.couponCode)})</span></div>
				<div class="paymentSuccess-cpnAmt">${fmt(num(c.couponAmount))}원</div>
				<div class="paymentSuccess-cpnPeriod">사용가능 기한: ${escapeHtml(c.startDate)} ~ ${escapeHtml(c.endDate)}</div>
				<div class="paymentSuccess-cpnPeriod text-muted">발급일: ${escapeHtml(c.issuedDate)}</div>
				<div class="paymentSuccess-cpnPeriod text-muted">최소구매: ${fmt(num(c.minPurchaseAmount))}원</div>
			</div>
		</li>`;
	}

	function renderCouponList(list, modal) {
		const ul = $("#paymentSuccess-cpnList");
		const applyBtn = $("#paymentSuccess-cpnApply");
		if (!ul) return;

		ul.innerHTML = "";

		// ✅ 마지막 조회 목록 캐시(선택 적용 시 사용)
		if (modal) modal.__lastList = list || [];

		if (!list || list.length === 0) {
			ul.innerHTML = `<li class="list-group-item text-muted">사용 가능한 쿠폰이 없습니다.</li>`;
			if (applyBtn) applyBtn.disabled = true;
			return;
		}

		list.forEach(c => ul.insertAdjacentHTML("beforeend", couponTemplate(c)));

		ul.querySelectorAll(".paymentSuccess-cpnCheck").forEach(chk => {
			chk.addEventListener("change", (e) => {
				if (e.target.checked) {
					ul.querySelectorAll(".paymentSuccess-cpnCheck").forEach(other => { if (other !== e.target) other.checked = false; });
					if (applyBtn) applyBtn.disabled = false;
				} else {
					const any = ul.querySelector(".paymentSuccess-cpnCheck:checked");
					if (applyBtn) applyBtn.disabled = !any;
				}
			});
		});
	}

	async function fetchCoupons(issuedStart, issuedEnd) {
		const qs = new URLSearchParams();
		if (issuedStart) qs.append('issuedStart', issuedStart);
		if (issuedEnd) qs.append('issuedEnd', issuedEnd);

		const url = '/api/customer/auth/coupons' + (qs.toString() ? ('?' + qs.toString()) : '');
		return await apiGetJson(url);
	}

	function attachCouponModal() {
		const modal = $("#paymentSuccess-couponModal");
		if (!modal) return;

		wireModal(modal);

		const openBtn = $("#paymentSuccess-openCouponModal");
		const searchBtn = $("#paymentSuccess-cpnSearch");
		const applyBtn = $("#paymentSuccess-cpnApply");
		const removeBtn = $(".paymentSuccess-removeCoupon");

		if (openBtn) {
			openBtn.addEventListener("click", async () => {
				const sEl = $("#paymentSuccess-cpnStart");
				const eEl = $("#paymentSuccess-cpnEnd");
				if (sEl) sEl.value = "";
				if (eEl) eEl.value = "";
				if (applyBtn) applyBtn.disabled = true;

				try {
					const list = await fetchCoupons("", "");
					renderCouponList(list, modal);
					openModal(modal);
				} catch (e) {
					alert("쿠폰 정보를 불러오지 못했습니다.");
				}
			});
		}

		if (searchBtn) {
			searchBtn.addEventListener("click", async () => {
				const s = $("#paymentSuccess-cpnStart")?.value || "";
				const e = $("#paymentSuccess-cpnEnd")?.value || "";
				if (applyBtn) applyBtn.disabled = true;

				try {
					const list = await fetchCoupons(s, e);
					renderCouponList(list, modal);
				} catch (ex) {
					alert("쿠폰 검색에 실패했습니다.");
				}
			});
		}

		if (applyBtn) {
			applyBtn.addEventListener("click", () => {
				const checked = modal.querySelector(".paymentSuccess-cpnCheck:checked");
				if (!checked) return;

				const list = modal.__lastList || [];
				const mcId = String(checked.value);
				const c = list.find(x => String(x.memberCouponId) === mcId);
				if (!c) {
					alert("선택한 쿠폰 정보를 찾을 수 없습니다.");
					return;
				}

				st.coupon = c;

				const input = $("#paymentSuccess-couponInput");
				if (input) input.value = c.couponName;

				const badge = document.querySelector(".paymentSuccess-selectedCoupon");
				if (badge) {
					const textEl = badge.querySelector(".text");
					if (textEl) textEl.textContent = c.couponName;
					badge.classList.remove("d-none");
				}

				closeModal(modal);
				recalc();
			});
		}

		// 배지 X 클릭: 완전 초기화
		if (removeBtn) {
			removeBtn.addEventListener("click", () => {
				st.coupon = null;

				const input = $("#paymentSuccess-couponInput");
				if (input) input.value = "";

				const badge = document.querySelector(".paymentSuccess-selectedCoupon");
				if (badge) {
					const textEl = badge.querySelector(".text");
					if (textEl) textContent = "";
					badge.classList.add("d-none");
				}
				recalc();
			});
		}
	}

	function attachRefreshConfirm() {
		// ✅ F5 / Ctrl+R / Cmd+R 감지해서 "원하는 문구" confirm
		window.addEventListener('keydown', (e) => {
			const isF5 = (e.key === 'F5' || e.keyCode === 116);
			const isCtrlR = (e.ctrlKey && (e.key === 'r' || e.key === 'R'));
			const isCmdR = (e.metaKey && (e.key === 'r' || e.key === 'R'));
			if (!isF5 && !isCtrlR && !isCmdR) return;

			// 결제 진행중일 때만 confirm
			const memberId = getLoginMemberId();
			if (!memberId || !isPaymentInProgress(memberId)) return;

			e.preventDefault();
			e.stopPropagation();

			const ok = confirm("현재 결제 진행중 정보가 삭제됩니다.\n계속 진행하시겠습니까?");
			if (ok) {
				setPaymentInProgress(memberId, false);
				clearPaymentPayloadSession(memberId);
				// 새로고침 진행
				location.reload();
			}
		}, { capture: true });
	}

	function attachBeforeUnloadGuard() {
		window.addEventListener('beforeunload', (e) => {
			const memberId = getLoginMemberId();
			if (!memberId || !isPaymentInProgress(memberId)) return;

			// 대부분 브라우저에서 문구는 무시되고 기본 confirm으로 대체됩니다.
			e.preventDefault();
			e.returnValue = '';
		});
	}

	function normalizeItemProductType(v) {
		const s = String(v || '').trim().toUpperCase();
		return s === 'DEALER' ? 'DEALER' : 'COMPANY';
	}

	// =========================
	// 기타 셀렉트/라디오
	// =========================
	function attachOthers() {
		const shipMethod = $(".paymentSuccess-shippingMethod");
		if (shipMethod) {
			// 초기값 반영
			st.shippingMethod = shipMethod.value || "택배배송";
			updateShipFeeFromMethod();

			shipMethod.addEventListener("change", (e) => {
				st.shippingMethod = e.target.value;
				updateShipFeeFromMethod();
				recalc();
			});
		} else {
			// 방어
			updateShipFeeFromMethod();
		}

		$$(".paymentSuccess-shipPay").forEach(r => r.addEventListener("change", (e) => {
			if (e.target.checked) st.shipPay = e.target.value;
			// ✅ 요구사항: 선불/착불 상관없이 포함이므로 recalc만
			recalc();
		}));

		$$(".paymentSuccess-payMethod").forEach(r => r.addEventListener("change", () => recalc()));
	}

	// =========================
	// init
	// =========================
	async function init() {
		loadPayloadOrRedirect();
		if (!st.payload) return;

		// 주문 상품 렌더
		renderOrderTable();
		attachBeforeUnloadGuard();
		// ✅ 결제 진행중 플래그 ON
		setPaymentInProgress(st.memberId, true);
		attachRefreshConfirm();
		// 회원 정보 로드
		try {
			await loadMemberProfile();

			const ordererName = $("#paymentSuccess-ordererName");
			const ordererPhone = $("#paymentSuccess-ordererPhone");
			if (ordererName) ordererName.value = st.memberProfile?.name || "";
			if (ordererPhone) ordererPhone.value = st.memberProfile?.mobile || "";
		} catch (e) {
			console.error(e);
		}

		attachQtyEvents();
		attachPhoneAutoTab();
		attachPointLimit();
		attachFindAddress();
		attachSameAsOrderer();
		attachTerms();
		attachDiscountDetail();
		attachCouponModal();
		attachOthers();

		recalc();
	}

	document.addEventListener("DOMContentLoaded", init);

})();
