(function($) {
	'use strict';

	var $w = $(window);
	var $header, $spacer;

	// ==== 튐 방지 파라미터 ====
	var THRESHOLD = 8;           // 방향 판정 최소 이동(px)
	var DIR_ACC_TARGET = 24;     // 같은 방향 누적 이동이 이 값 이상일 때만 hidden 토글
	var TOP_DEADZONE = 16;       // 최상단 근처에서는 강제 노출
	var BOTTOM_DEADZONE = 16;    // 최하단 근처에서는 강제 노출
	var MIN_SCROLLABLE_BASE = 120; // "짧은 페이지"로 보는 최소 스크롤 여유(px)
	var COMPACT_OFFSET = 0;      // compact 시작 기준

	var lastY = 0;
	var ticking = false;
	var isCompact = false;
	var headerH = 0;

	// 방향 누적(히스테리시스)
	var dirAcc = 0;   // 누적 이동량(+down, -up)
	var lastDir = 0;  // 최근 방향(1:down, -1:up, 0:none)

	function ensureNodes() {
		if (!$header || !$header.length) $header = $('header').first();
		if (!$spacer || !$spacer.length) {
			$spacer = $('<div class="header-spacer" aria-hidden="true"></div>');
			$spacer.insertAfter($header);
		}
	}

	function pageScrollablePx() {
		var docH = document.documentElement.scrollHeight;
		var winH = window.innerHeight;
		return Math.max(0, docH - winH);
	}

	function isShortPage() {
		// 헤더 높이 기준으로 동적 임계치 상향 (헤더가 클수록 여유 필요)
		var dynMin = Math.max(MIN_SCROLLABLE_BASE, headerH + 24);
		return pageScrollablePx() < dynMin;
	}

	function updateSpacerHeight(active) {
		if (active) {
			headerH = $header.outerHeight();
			$spacer.css('height', headerH + 'px');
		} else {
			$spacer.css('height', '0px');
		}
	}

	function setCompact(on) {
		if (isCompact === on) return;
		isCompact = on;
		if (on) {
			$header.addClass('navbar-compact');
			updateSpacerHeight(true);
		} else {
			$header.removeClass('navbar-compact hidden-menu');
			updateSpacerHeight(false);
		}
	}

	function setHiddenMenu(hidden) {
		if (!isCompact) return;
		$header.toggleClass('hidden-menu', !!hidden);
	}

	function hardResetToDefault() {
		setCompact(false);
		setHiddenMenu(false);
		dirAcc = 0;
		lastDir = 0;
	}

	function onScroll() {
		if (ticking) return;
		ticking = true;

		window.requestAnimationFrame(function() {
			var y = $w.scrollTop();
			var maxY = Math.max(0, document.documentElement.scrollHeight - window.innerHeight);

			// 헤더 높이는 리얼타임으로 변할 수 있으니 종종 보정
			headerH = $header.outerHeight();

			// 1) 짧은 페이지면 어떤 토글도 하지 않음 (스페이서 생성 X)
			if (isShortPage()) {
				hardResetToDefault();
				lastY = y;
				ticking = false;
				return;
			}

			// 2) compact 여부 반영
			if (y > COMPACT_OFFSET) setCompact(true);
			else setCompact(false);

			// 3) deadzone 처리 (상단/하단 근처에선 강제 노출)
			if (y <= TOP_DEADZONE || (maxY - y) <= BOTTOM_DEADZONE) {
				setHiddenMenu(false);
				dirAcc = 0; lastDir = 0;
				lastY = y;
				ticking = false;
				return;
			}

			// 4) 방향 판정 + 누적 히스테리시스
			var delta = y - lastY;
			if (Math.abs(delta) >= THRESHOLD) {
				var dir = delta > 0 ? 1 : -1; // 1:down, -1:up
				if (dir !== lastDir) {
					// 방향 바뀌면 누적 리셋
					dirAcc = 0;
					lastDir = dir;
				}
				dirAcc += Math.abs(delta);

				if (dirAcc >= DIR_ACC_TARGET) {
					if (dir === 1) {
						// 충분히 내려갔다 → 숨김
						setHiddenMenu(true);
					} else {
						// 충분히 올라왔다 → 노출
						setHiddenMenu(false);
					}
					dirAcc = 0; // 토글 후 누적 초기화
				}

				lastY = y;
			}

			ticking = false;
		});
	}

	function onResize() {
		// 리사이즈로 문서 높이/헤더 높이가 변할 수 있음
		headerH = $header.outerHeight();

		if (isShortPage()) {
			// 짧은 페이지가 되었다면 즉시 초기화
			hardResetToDefault();
		} else if (isCompact) {
			// compact 유지 중이면 스페이서 리사이즈
			updateSpacerHeight(true);
		}
	}

	function bind() {
		// 중복 방지
		$(window).off('scroll.headerToggle').on('scroll.headerToggle', onScroll);
		$(window).off('resize.headerToggle').on('resize.headerToggle', onResize);

		// 초기화
		headerH = $('header').first().outerHeight();
		lastY = $(window).scrollTop();

		onResize();
		onScroll();

		// 동적 컨텐츠 로드 대비 약한 폴백
		setTimeout(onResize, 0);
		setTimeout(onResize, 250);
		setTimeout(onResize, 1000);
	}

	$(function() {
		ensureNodes();
		bind();
	});

})(jQuery);
