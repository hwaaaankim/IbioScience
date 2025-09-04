/* eslint-disable */
(function() {
	"use strict";

	// ===== 상태 =====
	const st = {
		item: {
			id: "12345678", name: "Orion Star A211 pH Benchtop Meter, pH 번호판 미터",
			price: 59400, shipFee: 3000, tableShipFee: 4000, point: 0, qty: 1, discount: 0
		},
		coupon: null,
		pointAvail: 0,
		pointUse: 0,
		shippingMethod: "택배배송",
		shipPay: "선불",
		expectPointRate: 0.0
	};

	/* 변경/추가: 샘플 쿠폰 데이터 (5개, 전부 사용가능, 금액형만) */
	const coupons = [
		{ id: 101, name: "₩3,000 즉시할인", type: "AMOUNT", value: 3000, min: 20000, issued: "2025-08-01", status: "USABLE", start: "2025-08-01", end: "2025-12-31" },
		{ id: 102, name: "₩5,000 즉시할인", type: "AMOUNT", value: 5000, min: 40000, issued: "2025-08-05", status: "USABLE", start: "2025-08-01", end: "2025-12-31" },
		{ id: 103, name: "₩7,000 즉시할인", type: "AMOUNT", value: 7000, min: 60000, issued: "2025-08-10", status: "USABLE", start: "2025-08-01", end: "2025-12-31" },
		{ id: 104, name: "₩10,000 즉시할인", type: "AMOUNT", value: 10000, min: 90000, issued: "2025-08-15", status: "USABLE", start: "2025-08-01", end: "2025-12-31" },
		{ id: 105, name: "₩2,000 즉시할인", type: "AMOUNT", value: 2000, min: 15000, issued: "2025-08-20", status: "USABLE", start: "2025-08-01", end: "2025-12-31" },
	];

	// ===== 유틸 =====
	const fmt = n => Number(n || 0).toLocaleString();
	const num = s => (typeof s === 'number' ? s : Number(String(s || '').replace(/[^\d]/g, '')) || 0);
	const $ = sel => document.querySelector(sel);
	const $$ = sel => Array.from(document.querySelectorAll(sel));
	function bindText(attr, val) { $$(`[data-bind="${attr}"]`).forEach(el => el.textContent = fmt(val)); }

	// ===== 계산 =====
	function couponDiscount(sumPrice) {
		if (!st.coupon) return 0;
		if (sumPrice < st.coupon.min) return 0;
		// 변경: 퍼센트 쿠폰 없음(방어코드만 유지)
		return st.coupon.type === "PERCENT" ? Math.floor(sumPrice * st.coupon.value / 100) : st.coupon.value;
	}
	function recalc() {
		const qty = st.item.qty;
		const sumPrice = st.item.price * qty;
		const baseDiscount = st.item.discount * qty;
		const couponDc = couponDiscount(sumPrice);
		const pointDc = st.pointUse;
		const ship = st.item.shipFee;
		const grand = Math.max(0, sumPrice - baseDiscount - couponDc - pointDc + ship);

		bindText("price", st.item.price);
		bindText("shipping", st.item.tableShipFee);
		bindText("point", st.item.point);
		bindText("discount", baseDiscount);
		bindText("subtotal", sumPrice - baseDiscount + st.item.tableShipFee);
		bindText("grand", grand);

		bindText("sumPrice", sumPrice);
		bindText("sumShip", ship);
		bindText("sumDiscount", baseDiscount);
		bindText("couponDiscount", couponDc);
		bindText("totalCount", qty);
		bindText("expectPoint", Math.floor(grand * st.expectPointRate));

		const list = $("#paymentSuccess-discountList");
		if (list) {
			list.innerHTML = `
				<li class="d-flex justify-content-between"><span>기본 프로모션</span><span>- ${fmt(baseDiscount)} 원</span></li>
				<li class="d-flex justify-content-between"><span>쿠폰 할인</span><span>- ${fmt(couponDc)} 원</span></li>
				<li class="d-flex justify-content-between"><span>적립금 사용</span><span>- ${fmt(pointDc)} 원</span></li>
			`;
		}
	}

	// ===== 수량 =====
	function attachQty() {
		const input = $(".paymentSuccess-qty-input");
		$(".paymentSuccess-qty-minus").addEventListener("click", () => {
			let v = Math.max(1, num(input.value) - 1); input.value = v; st.item.qty = v; recalc();
		});
		$(".paymentSuccess-qty-plus").addEventListener("click", () => {
			let v = Math.max(1, num(input.value) + 1); input.value = v; st.item.qty = v; recalc();
		});
		input.addEventListener("input", (e) => {
			const v = Math.max(1, num(e.target.value)); e.target.value = v; st.item.qty = v; recalc();
		});
	}

	// ===== 전화번호 자동 이동 =====
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

	// ===== 적립금 제한 =====
	function attachPointLimit() {
		$("#paymentSuccess-pointAvail").textContent = fmt(st.pointAvail);
		const input = $("#paymentSuccess-pointUse");
		input.addEventListener("input", (e) => {
			let v = num(e.target.value); if (v > st.pointAvail) v = st.pointAvail;
			e.target.value = v ? fmt(v) : "";
			st.pointUse = v; recalc();
		});
	}

	// ===== 주소 검색(daum.Postcode) =====
	function attachFindAddress() {
		$("#paymentSuccess-findAddrBtn").addEventListener("click", () => {
			new daum.Postcode({
				oncomplete: function(data) {
					// 기본값: 도로명주소
					const roadAddr = data.roadAddress || "";
					const zonecode = data.zonecode || "";
					$("#paymentSuccess-zip").value = zonecode;
					$("#paymentSuccess-road").value = roadAddr;
					$("#paymentSuccess-detail").focus();
				}
			}).open();
		});
	}

	// ===== 주문자 동일 =====
	function attachSameAsOrderer() {
		$("#paymentSuccess-sameAsOrderer").addEventListener("change", (e) => {
			if (e.target.checked) {
				$("#paymentSuccess-receiverName").value = $("#paymentSuccess-ordererName").value || "";
				const p = $("#paymentSuccess-ordererPhone").value.replace(/[^\d]/g, "");
				if (p.length >= 10) {
					$("#paymentSuccess-hp1").value = p.slice(0, 3);
					$("#paymentSuccess-hp2").value = p.slice(3, 7);
					$("#paymentSuccess-hp3").value = p.slice(7, 11);
				}
			}
		});
	}

	// ===== 약관/결제 =====
	function attachTerms() {
		const btn = $("#paymentSuccess-payBtn");
		$("#paymentSuccess-terms").addEventListener("change", (e) => { btn.disabled = !e.target.checked; });
		btn.addEventListener("click", () => alert("결제 로직 연동 예정입니다."));
		$(".paymentSuccess-termsLink").addEventListener("click", () => alert("약관 상세 페이지 또는 모달로 연결"));
	}

	// ===== 바닐라 모달 =====
	function openModal(el) { el.classList.add("is-open"); el.setAttribute("aria-hidden", "false"); }
	function closeModal(el) { el.classList.remove("is-open"); el.setAttribute("aria-hidden", "true"); }
	function wireModal(el) { el.addEventListener("click", (e) => { if (e.target === el || e.target.hasAttribute("data-close")) closeModal(el); }); }

	// 할인내역 모달
	function attachDiscountDetail() {
		const modal = $("#paymentSuccess-discountModal");
		wireModal(modal);
		$(".paymentSuccess-discountDetailBtn").addEventListener("click", () => openModal(modal));
	}

	// ===== 쿠폰 모달 =====
	function couponTemplate(c) {
		return `
		<li class="list-group-item">
			<input class="form-check-input paymentSuccess-cpnCheck" type="checkbox" value="${c.id}" />
			<div class="paymentSuccess-cpnItem">
				<div class="paymentSuccess-cpnName">${c.name}</div>
				<div class="paymentSuccess-cpnAmt">${Number(c.value).toLocaleString()}원</div>
				<div class="paymentSuccess-cpnPeriod">사용가능 기한: ${c.start} ~ ${c.end}</div>
			</div>
		</li>`;
	}

	function renderCouponList(list) {
		const ul = $("#paymentSuccess-cpnList"); ul.innerHTML = "";
		list.forEach(c => { ul.insertAdjacentHTML("beforeend", couponTemplate(c)); });
		// 체크박스를 라디오처럼 단일 선택으로 동작
		ul.querySelectorAll(".paymentSuccess-cpnCheck").forEach(chk => {
			chk.addEventListener("change", (e) => {
				if (e.target.checked) {
					ul.querySelectorAll(".paymentSuccess-cpnCheck").forEach(other => { if (other !== e.target) other.checked = false; });
					$("#paymentSuccess-cpnApply").disabled = false;
				} else {
					// 모두 해제되면 적용 버튼 비활성화
					const any = ul.querySelector(".paymentSuccess-cpnCheck:checked");
					$("#paymentSuccess-cpnApply").disabled = !any;
				}
			});
		});
	}

	function filterUsableByIssuedRange(list, s, e) {
		const start = s || "0000-01-01";
		const end = e || "9999-12-31";
		// 안내문구대로: 사용가능 쿠폰만
		return list.filter(c => c.status === "USABLE" && c.issued >= start && c.issued <= end);
	}

	function attachCouponModal() {
		const modal = $("#paymentSuccess-couponModal");
		wireModal(modal);
		renderCouponList(filterUsableByIssuedRange(coupons));

		// 검색 버튼: 발급일 범위 기준 + 사용가능만
		$("#paymentSuccess-cpnSearch").addEventListener("click", () => {
			const s = $("#paymentSuccess-cpnStart").value || "";
			const e = $("#paymentSuccess-cpnEnd").value || "";
			renderCouponList(filterUsableByIssuedRange(coupons, s, e));
			$("#paymentSuccess-cpnApply").disabled = true;
		});

		$("#paymentSuccess-openCouponModal").addEventListener("click", () => {
			$("#paymentSuccess-cpnStart").value = "";
			$("#paymentSuccess-cpnEnd").value = "";
			renderCouponList(filterUsableByIssuedRange(coupons));
			$("#paymentSuccess-cpnApply").disabled = true;
			openModal(modal);
		});

		$("#paymentSuccess-cpnApply").addEventListener("click", () => {
			const checked = modal.querySelector(".paymentSuccess-cpnCheck:checked");
			if (!checked) return;
			const c = coupons.find(x => String(x.id) === checked.value);
			st.coupon = c;
			$("#paymentSuccess-couponInput").value = c.name;
			const badge = document.querySelector(".paymentSuccess-selectedCoupon");
			badge.querySelector(".text").textContent = c.name;
			badge.classList.remove("d-none");
			closeModal(modal);
			recalc();
		});

		// 변경/추가: 배지 X 클릭 시 완전 초기화(텍스트도 비움)
		$(".paymentSuccess-removeCoupon").addEventListener("click", () => {
			st.coupon = null;
			$("#paymentSuccess-couponInput").value = "";
			const badge = document.querySelector(".paymentSuccess-selectedCoupon");
			badge.querySelector(".text").textContent = "";
			badge.classList.add("d-none");
			recalc();
		});
	}

	// 기타
	function attachOthers() {
		$(".paymentSuccess-shippingMethod").addEventListener("change", (e) => { st.shippingMethod = e.target.value; recalc(); });
		$$(".paymentSuccess-shipPay").forEach(r => r.addEventListener("change", (e) => { if (e.target.checked) st.shipPay = e.target.value; recalc(); }));
		$$(".paymentSuccess-payMethod").forEach(r => r.addEventListener("change", () => recalc()));
	}

	// init
	function init() {
		$("#paymentSuccess-pointAvail").textContent = fmt(st.pointAvail);
		$("#paymentSuccess-ordererName").value = "한정연"; // 샘플
		$("#paymentSuccess-ordererPhone").value = "010-3112-4624"; // 샘플

		attachQty(); attachPhoneAutoTab(); attachPointLimit();
		attachFindAddress(); attachSameAsOrderer();
		attachTerms(); attachDiscountDetail();
		attachCouponModal(); attachOthers();
		recalc();
	}
	document.addEventListener("DOMContentLoaded", init);
})();
