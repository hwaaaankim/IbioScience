/* global jQuery */
(function($) {
	"use strict";

	// ==================== 상단 카드형 슬라이더 ====================
	const CONF_TOP = {
		margin: 20,
		autoplayTimeout: 3000,
		smartSpeed: 450,
		pc: { items: 4, edgeVisible: 0.80 }, // PC: 양쪽 20% 잘림(=80% 보임)
		tab: { items: 4, edgeVisible: 0.20 }, // Tablet: 양쪽 80% 잘림(=20% 보임)
		mob: { items: 3, edgeVisible: 0.20 }  // Mobile: 양쪽 80% 잘림(=20% 보임)
	};

	const WRAP = '.dealer-productList-wrap';
	const SLIDER = '.dealer-productList-slider';
	const PREV = '.dealer-productList-prev';
	const NEXT = '.dealer-productList-next';
	const TOGGLE = '.dealer-productList-toggle';
	const BAR = '.dealer-productList-progress-bar';

	let $owlTop = null;
	let autoplayOn = true;
	let totalCount = 0;
	let currentIndex = 0;

	function specByViewportTop() {
		const vw = window.innerWidth || document.documentElement.clientWidth || document.body.clientWidth;
		if (vw >= 1200) return CONF_TOP.pc;
		if (vw >= 768) return CONF_TOP.tab;
		return CONF_TOP.mob;
	}
	function wrapWidth($wrap) { return $wrap.width(); }
	function calcItemWidth($wrap, items, margin) {
		const W = wrapWidth($wrap);
		return (W - margin * (items - 1)) / items;
	}
	function calcCropPx($wrap, items, margin, edgeVisible) {
		const w = calcItemWidth($wrap, items, margin);
		return Math.max(0, Math.round(w * (1 - edgeVisible)));
	}
	function useOwl($el, opts) {
		if (typeof $.fn.owlCarousel2 === 'function') return $el.owlCarousel2(opts);
		if (typeof $.fn.owlCarousel === 'function') return $el.owlCarousel(opts);
		console.error('Owl 플러그인을 찾을 수 없습니다.');
		return $el;
	}
	function triggerPrevTop() { if ($owlTop) { $owlTop.trigger('prev.owl.carousel'); $owlTop.trigger('prev.owl.carousel2'); } }
	function triggerNextTop() { if ($owlTop) { $owlTop.trigger('next.owl.carousel'); $owlTop.trigger('next.owl.carousel2'); } }
	function triggerPlayTop() { if ($owlTop) { $owlTop.trigger('play.owl.autoplay', [CONF_TOP.autoplayTimeout]); $owlTop.trigger('play.owl.autoplay2', [CONF_TOP.autoplayTimeout]); } }
	function triggerStopTop() { if ($owlTop) { $owlTop.trigger('stop.owl.autoplay'); $owlTop.trigger('stop.owl.autoplay2'); } }

	function updateProgress(i) {
		const $bar = $(BAR);
		if (totalCount <= 0) { $bar.css({ width: '0%', left: '0%' }); return; }
		const seg = 100 / totalCount;
		$bar.css({ width: seg + '%', left: (i * seg) + '%' });
	}
	function applyEdgeCrop($wrap, items, margin, edgeVisible) {
		const crop = calcCropPx($wrap, items, margin, edgeVisible);
		const $outer = $wrap.find('.owl-stage-outer');
		$outer.css({ overflow: 'hidden' });
		$outer.css('clip-path', `inset(0px ${crop}px 0px ${crop}px)`);
		$outer.css('-webkit-clip-path', `inset(0px ${crop}px 0px ${crop}px)`);
	}

	function initTop() {
		const $wrap = $(WRAP);
		const $slider = $(SLIDER);
		if (!$slider.length) return;

		const originalCount = $slider.children('.dealer-productList-item').length;
		const s = specByViewportTop();

		if ($slider.hasClass('owl-loaded')) {
			$slider.trigger('destroy.owl.carousel'); $slider.trigger('destroy.owl.carousel2');
			$slider.removeClass('owl-loaded');
			$slider.find('.owl-stage-outer').children().unwrap();
		}

		useOwl($slider, {
			margin: CONF_TOP.margin,
			loop: true,
			nav: false,
			dots: false,
			autoplay: autoplayOn,
			autoplayTimeout: CONF_TOP.autoplayTimeout,
			autoplayHoverPause: false,
			smartSpeed: CONF_TOP.smartSpeed,
			stagePadding: 0,
			responsiveRefreshRate: 150,
			responsive: {
				0: { items: CONF_TOP.mob.items },
				768: { items: CONF_TOP.tab.items },
				1200: { items: CONF_TOP.pc.items }
			}
		});

		$owlTop = $slider;
		totalCount = originalCount;
		currentIndex = 0;
		updateProgress(0);
		applyEdgeCrop($wrap, s.items, CONF_TOP.margin, s.edgeVisible);

		$slider
			.off('changed.owl.carousel changed.owl.carousel2 resized.owl.carousel resized.owl.carousel2')
			.on('changed.owl.carousel changed.owl.carousel2', function(e) {
				const clones = (e.relatedTarget && e.relatedTarget._clones) ? e.relatedTarget._clones.length : 0;
				const idx = (typeof e.item?.index === 'number') ? (e.item.index - clones / 2) : 0;
				const real = (idx % totalCount + totalCount) % totalCount;
				currentIndex = real;
				updateProgress(real);
			})
			.on('resized.owl.carousel resized.owl.carousel2', function() {
				const s2 = specByViewportTop();
				applyEdgeCrop($wrap, s2.items, CONF_TOP.margin, s2.edgeVisible);
				updateProgress(currentIndex);
			});
	}

	function bindTop() {
		$(PREV).off('click').on('click', triggerPrevTop);
		$(NEXT).off('click').on('click', triggerNextTop);
		$(TOGGLE).off('click').on('click', function() {
			autoplayOn = !autoplayOn;
			if (autoplayOn) { triggerPlayTop(); $(this).text('⏸').attr('aria-label', '일시정지'); }
			else { triggerStopTop(); $(this).text('▶').attr('aria-label', '재생'); }
		});

		let t = null;
		$(window).off('resize.dealerProductList').on('resize.dealerProductList', function() {
			clearTimeout(t);
			t = setTimeout(function() {
				initTop();
				if (!autoplayOn) triggerStopTop();
			}, 120);
		});
	}

	// ==================== 하단 브랜드 슬라이더 ====================
	const BRAND = {
		WRAP: '.dealer-productList-brandWrap',
		SLIDER: '.dealer-productList-brandSlider'
	};

	function useOwlBrand($el, opts) {
		if (typeof $.fn.owlCarousel2 === 'function') return $el.owlCarousel2(opts);
		if (typeof $.fn.owlCarousel === 'function') return $el.owlCarousel(opts);
		console.error('Owl 플러그인을 찾을 수 없습니다(brand).');
		return $el;
	}

	function initBrand() {
		const $wrap = $(BRAND.WRAP);
		const $slider = $(BRAND.SLIDER);
		if (!$slider.length) return;

		if ($slider.hasClass('owl-loaded')) {
			$slider.trigger('destroy.owl.carousel'); $slider.trigger('destroy.owl.carousel2');
			$slider.removeClass('owl-loaded');
			$slider.find('.owl-stage-outer').children().unwrap();
		}

		useOwlBrand($slider, {
			loop: true,           // 요청: loop 켬
			nav: true,            // 요청: arrow 사용
			dots: false,          // 요청: 도트/프로그레스바 없음
			margin: 12,
			autoplay: false,
			smartSpeed: 350,
			mouseDrag: true, touchDrag: true, pullDrag: true,
			navText: ['‹', '›'],
			responsiveRefreshRate: 120,
			responsive: {
				0: { items: 5 },
				360: { items: 6 },
				480: { items: 7 },
				576: { items: 8 },
				700: { items: 9 },
				768: { items: 10 },
				860: { items: 11 },
				992: { items: 12 },
				1100: { items: 13 },
				1200: { items: 14 },
				1400: { items: 15 }
			}
		});

		// 혹시 테마가 .owl-nav를 slider 밖으로 뽑아둘 경우를 대비, wrap로 이동시켜 절대배치 고정
		const $nav = $slider.find('.owl-nav').length ? $slider.find('.owl-nav') : $wrap.find('> .owl-nav');
		if ($nav.length) { $wrap.append($nav); } // wrap의 자식으로 강제

		// 안전: nav가 클릭되도록 pointer-events 보정(스타일에서 처리했지만 한 번 더 보정)
		$wrap.find('.owl-nav').css({ 'pointer-events': 'none' });
		$wrap.find('.owl-prev, .owl-next').css({ 'pointer-events': 'auto' });
	}

	// ==================== 시작 ====================
	function startAll() {
		let tries = 0;
		(function wait() {
			const ok = (typeof $.fn.owlCarousel2 === 'function') || (typeof $.fn.owlCarousel === 'function');
			if (ok) {
				initTop(); bindTop();
				initBrand();
			} else if (tries++ < 100) {
				setTimeout(wait, 50);
			} else {
				console.error('Owl 플러그인이 로드되지 않았습니다.');
			}
		})();

		// 리사이즈
		let t2 = null;
		$(window).off('resize.dealerProductListBrand').on('resize.dealerProductListBrand', function() {
			clearTimeout(t2);
			t2 = setTimeout(function() { initBrand(); }, 120);
		});
	}

	$(startAll);
})(jQuery);
