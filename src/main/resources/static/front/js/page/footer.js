// footer.js — PC 공지 리스트 + 모바일 무한 스크롤(속도 50% 느리게)
(function() {
	"use strict";

	const NOTICES = [
		{ title: "이곳은 공지 사항이 안내됩니다. 이곳은 공지사항이 안내됩니다.", date: "2025-08-20", author: "관리자" },
		{ title: "이곳은 공지사항이 안내됩니다.", date: "2025-08-12", author: "관리자" },
		{ title: "공지사항 예시 문구가 노출됩니다.", date: "2025-08-05", author: "관리자" },
		{ title: "점검 안내: 시스템 점검이 예정되어 있습니다.", date: "2025-07-28", author: "관리자" },
		{ title: "배송 지연 안내 드립니다.", date: "2025-07-18", author: "관리자" }
	].slice(0, 5);

	/* PC 공지 리스트 */
	const pcList = document.getElementById("front-footer-notice-list-pc");
	if (pcList) {
		pcList.innerHTML = NOTICES.map(n => `
      <li class="front-footer-notice-item">
        <a href="#" class="front-footer-notice-title" title="${esc(n.title)}">${esc(n.title)}</a>
        <span class="front-footer-notice-meta">${n.date} · ${n.author}</span>
      </li>
    `).join("");
	}

	/* 모바일 티커 (Tablet은 없음) */
	buildMobileTicker("front-footer-ticker-track-mo");

	function buildMobileTicker(id) {
		const el = document.getElementById(id);
		if (!el) return;

		const row = NOTICES.map(n => `
      <span class="front-footer-ticker-item">
        <i class="front-footer-ticker-bullet" aria-hidden="true"></i>
        <span class="front-footer-ticker-title">${esc(n.title)}</span>
        <span class="front-footer-ticker-meta">(${n.date} · ${n.author})</span>
      </span>
    `).join("");

		// 무한루프 자연스럽게: 내용 2회 반복
		el.innerHTML = row + row;

		// 실제 폭 기반으로 속도 산정(기본 약 60px/s) → ×2 = 50% 느리게
		requestAnimationFrame(() => {
			const width = el.scrollWidth / 2;
			const sec = Math.max(20, Math.round(width / 60) * 2); // 느리게
			el.style.animationDuration = `${sec}s`;
		});
	}

	/* utils */
	function esc(s) {
		return String(s)
			.replace(/&/g, "&amp;")
			.replace(/</g, "&lt;")
			.replace(/>/g, "&gt;")
			.replace(/"/g, "&quot;");
	}
})();
