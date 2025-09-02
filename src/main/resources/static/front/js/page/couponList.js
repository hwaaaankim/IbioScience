/* /front/js/page/couponList.js
 * 쿠폰 리스트 (샘플 데이터 + 기간 프리셋 + 검색 필터 + 반응형 렌더)
 * - 탭: 사용가능 / 사용불가능
 * - 기간 프리셋: 1개월/3개월/6개월/1년 → #startDate/#endDate 입력칸 즉시 반영
 * - 검색: 발급일(issuedAt) 기준으로 기간 필터 후 렌더
 * - 반응형: ≥992px(데스크톱 5열) / ≤991px(모바일 2열)
 * - [시작일] ~ [종료일] 텍스트 자동 반영
 */

/* ===== 유틸 ===== */
function pad2(n) { return n < 10 ? '0' + n : '' + n; }

function formatYYYYMMDD(d) {
	const y = d.getFullYear();
	const m = pad2(d.getMonth() + 1);
	const day = pad2(d.getDate());
	return `${y}-${m}-${day}`;
}

function parseYYYYMMDD(str) {
	// '2025-09-01' -> Date(로컬)
	const [y, m, d] = (str || '').split('-').map(Number);
	if (!y || !m || !d) return null;
	const dt = new Date(y, m - 1, d, 0, 0, 0, 0);
	return isNaN(dt.getTime()) ? null : dt;
}

function isMobileView() {
	return window.innerWidth <= 991;
}

function debounce(fn, wait) {
	let t;
	return function(...args) {
		clearTimeout(t);
		t = setTimeout(() => fn.apply(this, args), wait);
	};
}

function formatMoneyKRW(num) {
	if (typeof num !== 'number') return num;
	return num.toLocaleString('ko-KR') + '원';
}

/* ===== 샘플 데이터 ===== */
const sampleAvailable = [
	{ no: 1, name: '신규회원 5% 할인쿠폰', minAmount: 30000, validUntil: '2025-12-31', available: true, issuedAt: '2025-08-01' },
	{ no: 2, name: '브랜드A 10,000원 할인', minAmount: 100000, validUntil: '2025-11-15', available: true, issuedAt: '2025-08-10' },
	{ no: 3, name: '주말 특가 7% 쿠폰', minAmount: 50000, validUntil: '2025-10-30', available: true, issuedAt: '2025-08-18' },
	{ no: 4, name: '장바구니 3천원 할인', minAmount: 20000, validUntil: '2025-09-25', available: true, issuedAt: '2025-08-20' },
];

const sampleUnavailable = [
	{ no: 1, name: '여름맞이 10% 쿠폰', minAmount: 40000, validUntil: '2025-08-20', available: false, issuedAt: '2025-07-15' },
	{ no: 2, name: '브랜드B 5천원 할인', minAmount: 60000, validUntil: '2025-08-10', available: false, issuedAt: '2025-07-28' },
	{ no: 3, name: '배송비 무료 쿠폰', minAmount: 0, validUntil: '2025-08-05', available: false, issuedAt: '2025-07-30' },
];

/* ===== 상태 ===== */
const st = {
	tab: 'available',        // 'available' | 'unavailable'
	baseData: sampleAvailable,
	filtered: sampleAvailable,
};

/* ===== 현재 탭에 맞는 원본 데이터 반환 ===== */
function getTabData() {
	return st.tab === 'available' ? sampleAvailable : sampleUnavailable;
}

/* ===== 상단 문구에 날짜 반영 ===== */
function updateRangeText() {
	const el = document.querySelector('.couponList-table-desc');
	if (!el) return;
	const sVal = document.getElementById('startDate')?.value?.trim();
	const eVal = document.getElementById('endDate')?.value?.trim();
	if (sVal && eVal) {
		el.textContent = `[${sVal}] ~ [${eVal}] 까지 쿠폰 리스트 입니다.`;
	} else {
		// 기간 미선택 시 기본 문구 유지(원하시면 커스터마이즈 가능)
		el.textContent = `기간을 선택해 주세요.`;
	}
}

/* ===== 기간 프리셋 → 시작/종료 날짜 인풋 반영 ===== */
function applyRangePreset(presetVal) {
	// 기준: 오늘(로컬, Asia/Seoul 가정)
	const today = new Date();
	const end = new Date(today.getFullYear(), today.getMonth(), today.getDate()); // 00:00
	let months = 1;
	if (presetVal === '3m') months = 3;
	else if (presetVal === '6m') months = 6;
	else if (presetVal === '1y') months = 12;

	const start = new Date(end.getFullYear(), end.getMonth() - months, end.getDate());

	const $start = document.getElementById('startDate');
	const $end = document.getElementById('endDate');
	if ($start) $start.value = formatYYYYMMDD(start);
	if ($end) $end.value = formatYYYYMMDD(end);

	// 프리셋 선택 즉시 상단 문구 갱신
	updateRangeText();
}

/* ===== 기간 필터 (발급일 issuedAt 기준, 양끝 포함) ===== */
function filterByDateRange(rows) {
	const $start = document.getElementById('startDate');
	const $end = document.getElementById('endDate');
	const startStr = $start?.value?.trim();
	const endStr = $end?.value?.trim();

	const start = parseYYYYMMDD(startStr);
	const end = parseYYYYMMDD(endStr);

	if (!start || !end) return rows;  // 기간 미설정 시 필터 없이 반환

	// 유효성: 시작 > 종료면 경고
	if (start.getTime() > end.getTime()) {
		alert('시작일이 종료일보다 늦습니다. 기간을 확인해 주세요.');
		return rows;
	}

	// issuedAt 기준 필터
	return rows.filter(r => {
		const issued = parseYYYYMMDD(r.issuedAt);
		if (!issued) return false;
		return issued.getTime() >= start.getTime() && issued.getTime() <= end.getTime();
	});
}

/* ===== 렌더러: 데스크톱(5열) ===== */
function renderDesktop(tableEl, rows) {
	tableEl.innerHTML = `
    <colgroup>
      <col style="width:100px" />
      <col />
      <col style="width:160px" />
      <col style="width:200px" />
      <col style="width:160px" />
    </colgroup>
    <thead>
      <tr>
        <th>번호</th>
        <th>쿠폰명</th>
        <th>최소구매금액</th>
        <th>사용기한</th>
        <th>발급일</th>
      </tr>
    </thead>
    <tbody>
      ${rows.map(r => {
		const badgeClass = r.available ? 'ok' : 'expired';
		const badgeText = r.available ? '사용가능' : '기간만료';
		return `
          <tr>
            <td class="couponList-table-td-no">${r.no}</td>
            <td class="couponList-table-td-title">${r.name}</td>
            <td class="couponList-table-td-money">${formatMoneyKRW(r.minAmount)}</td>
            <td class="couponList-table-td-valid">
              ${r.validUntil}
              &nbsp;<span class="couponList-table-badge ${badgeClass}">${badgeText}</span>
            </td>
            <td class="couponList-table-td-date">${r.issuedAt}</td>
          </tr>
        `;
	}).join('')}
    </tbody>
  `;
}

/* ===== 렌더러: 모바일(2열: 내용/사용기한) ===== */
function renderMobile(tableEl, rows) {
	tableEl.innerHTML = `
    <colgroup>
      <col />
      <col style="width:180px" />
    </colgroup>
    <thead>
      <tr>
        <th>내용</th>
        <th>사용기한</th>
      </tr>
    </thead>
    <tbody>
      ${rows.map(r => {
		const badgeClass = r.available ? 'ok' : 'expired';
		const badgeText = r.available ? '사용가능' : '기간만료';
		return `
          <tr>
            <td>
              <span class="couponList-table-title">${r.name}</span>
              <span class="couponList-table-meta">최소구매금액: ${formatMoneyKRW(r.minAmount)}</span>
              <span class="couponList-table-meta">발급일: ${r.issuedAt}</span>
            </td>
            <td class="couponList-table-td-valid">
              ${r.validUntil}
              <br/>
              <span class="couponList-table-badge ${badgeClass}">${badgeText}</span>
            </td>
          </tr>
        `;
	}).join('')}
    </tbody>
  `;
}

/* ===== 메인 렌더 ===== */
function renderTable() {
	const table = document.getElementById('couponListTable');
	if (!table) return;
	const rows = st.filtered;

	if (isMobileView()) {
		renderMobile(table, rows);
	} else {
		renderDesktop(table, rows);
	}
}

/* ===== 현재 탭 + 기간 조건으로 필터링 갱신 ===== */
function applyFiltersAndRender() {
	st.baseData = getTabData();
	st.filtered = filterByDateRange(st.baseData);
	renderTable();
	// 필터 적용 후 상단 문구도 반영
	updateRangeText();
}

/* ===== 탭 이벤트 ===== */
function bindTabs() {
	const tabAvailable = document.getElementById('couponTab-available');
	const tabUnavailable = document.getElementById('couponTab-unavailable');

	function activate(tab) {
		if (tab === 'available') {
			st.tab = 'available';
			tabAvailable.classList.add('active');
			tabUnavailable.classList.remove('active');
			tabAvailable.setAttribute('aria-selected', 'true');
			tabUnavailable.setAttribute('aria-selected', 'false');
		} else {
			st.tab = 'unavailable';
			tabUnavailable.classList.add('active');
			tabAvailable.classList.remove('active');
			tabUnavailable.setAttribute('aria-selected', 'true');
			tabAvailable.setAttribute('aria-selected', 'false');
		}
		applyFiltersAndRender();
	}

	tabAvailable?.addEventListener('click', () => activate('available'));
	tabUnavailable?.addEventListener('click', () => activate('unavailable'));
}

/* ===== 기간 프리셋 이벤트 ===== */
function bindRangePreset() {
	const radios = document.querySelectorAll('input[name="rangePreset"]');
	radios.forEach(r => {
		r.addEventListener('change', (e) => {
			const val = e.target.value; // '1m' | '3m' | '6m' | '1y'
			applyRangePreset(val);
			// 프리셋 바꾸면 자동 검색까지 원하시면 아래 주석 해제
			// applyFiltersAndRender();
		});
	});
}

/* ===== 검색 버튼 ===== */
function bindSearch() {
	const btn = document.getElementById('orderFilterSearchBtn');
	btn?.addEventListener('click', () => {
		applyFiltersAndRender();
	});
}

/* ===== 리사이즈 대응(디바운스) ===== */
const onResize = debounce(() => {
	renderTable();
}, 120);

/* ===== 초기화 ===== */
document.addEventListener('DOMContentLoaded', () => {
	// 1) 탭
	bindTabs();

	// 2) 기간 프리셋 → 기본(checked) 값으로 날짜 인풋 세팅 + 문구 반영
	const checked = document.querySelector('input[name="rangePreset"]:checked');
	if (checked) applyRangePreset(checked.value);
	else updateRangeText(); // 프리셋이 없으면 일단 문구만 갱신

	bindRangePreset();

	// 3) 검색 버튼
	bindSearch();

	// 4) 초기 렌더 (기본 탭 + 기본 기간)
	applyFiltersAndRender();

	// 5) 반응형 전환 대응
	window.addEventListener('resize', onResize);
});
