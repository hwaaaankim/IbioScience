/* eslint-disable */
(function($) {
    'use strict';

    function formatMoney(n) {
        n = Number(n || 0);
        return n.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
    }

    function recalcTotal($panel) {
        var total = 0;
        $panel.find('tbody tr').each(function() {
            var $tr = $(this);
            if (!$tr.find('.product-list-row-check').is(':checked')) return;
            var price = Number($tr.data('price') || 0);
            var qty = Number($tr.find('.product-list-qty-input').val() || 1);
            if (qty < 1) qty = 1;
            total += price * qty;
        });
        $panel.find('.product-list-total-price').text(formatMoney(total));
    }

    // ✅ 트리거(버튼/아이콘) → 동일 상품의 옵션패널 찾기 (안전하게 범위 한정)
    function getPanelFromTrigger($trigger) {
        var $layout = $trigger.closest('.product-layout'); // 상품 한 덩어리
        if ($layout.length === 0) {
            // 혹시 레이아웃이 없으면 기존 방식으로 (바로 다음 형제)
            var $list = $trigger.closest('.product-item-container.list-container');
            return $list.nextAll('.product-list-option-panel').first();
        }
        // 기본: 같은 레이아웃 안에서, list-container 다음의 panel
        var $listInLayout = $trigger.closest('.product-item-container.list-container');
        var $panel = $listInLayout.nextAll('.product-list-option-panel').first();
        // 예외: 구조가 달라도 같은 레이아웃 안의 첫 panel만 사용
        if ($panel.length === 0) $panel = $layout.find('.product-list-option-panel').first();
        return $panel;
    }

    // ✅ 옵션보기(PC 버튼, 모바일 아이콘) 둘 다 여기로 처리
    $(document).on('click', '.product-list-option-btn, .product-list-grid-option', function(e) {
        e.preventDefault();
        var $trigger = $(this);
        var $panel = getPanelFromTrigger($trigger);

        if ($panel.length === 0) return; // 매칭 실패 방어

        $panel.stop(true, true).slideToggle(180, function() {
            // 패널 표시 상태에 맞춰 아이콘/aria 동기화 (같은 상품 내 트리거 전부)
            var isOpen = $panel.is(':visible');
            var $layout = $trigger.closest('.product-layout');
            $layout.find('.product-list-option-btn, .product-list-grid-option')
                .attr('aria-expanded', isOpen ? 'true' : 'false')
                .each(function() {
                    var $i = $(this).find('i.fa');
                    if ($i.length) {
                        $i.toggleClass('fa-angle-down', !isOpen)
                            .toggleClass('fa-angle-up', isOpen);
                    }
                });

            if (isOpen) recalcTotal($panel);
        });
    });

    // 옵션 체크/수량 변경 시 총액 반영
    $(document).on('change keyup',
        '.product-list-option-panel .product-list-row-check, .product-list-option-panel .product-list-qty-input',
        function() {
            var $panel = $(this).closest('.product-list-option-panel');
            recalcTotal($panel);
        });

})(jQuery);

// ================== GRID / LIST 전환 ==================
function display(view) {
    $('.products-list').removeClass('list grid').addClass(view);
    $('.list-view .btn').removeClass('active');
    if (view == 'list') {
        $('.products-list .product-layout .list-block').removeClass('hidden');
        $('.products-list .product-layout .button-group').addClass('hidden');
        $('.products-list .product-layout .product-item-container.grid-container').addClass('hidden');
        $('.products-list .product-layout .product-item-container.list-container').removeClass('hidden');
        $('.products-list .list-header').removeClass('hidden');
        $('.product-list-option-panel').removeClass('hidden');
        $('.products-list .product-layout .product-item-container .right-block .price').addClass('hidden');
        $('.list-view .' + view).addClass('active');
        $.cookie('display', 'list');
    } else {
        $('.products-list .product-layout .product-item-container.list-container').addClass('hidden');
        $('.products-list .list-header').addClass('hidden');
        $('.products-list .product-layout .product-item-container.grid-container').removeClass('hidden');
        $('.products-list .product-layout .button-group').removeClass('hidden');
        $('.product-list-option-panel').addClass('hidden');
        $('.list-view .' + view).addClass('active');
        $.cookie('display', 'grid');
    }
}

$(document).ready(function() {

    // GRID / LIST 버튼 클릭
    $('.list-view .btn').each(function() {
        var ua = navigator.userAgent,
            event = (ua.match(/iPad/i)) ? 'touchstart' : 'click';
        $(this).bind(event, function() {
            $(this).addClass(function() {
                if ($(this).hasClass('active')) return '';
                return 'active';
            });
            $(this).siblings('.btn').removeClass('active');
            $catalog_mode = $(this).data('view');
            display($catalog_mode);
        });

    });

    // ✅ 정렬/페이지 사이즈 변경 시 폼 전송
    var $form = $('#productListForm');
    if ($form.length) {
        // 정렬
        $form.find('select[name="sort"]').on('change', function() {
            $form.find('input[name="page"]').val(0);
            $form.submit();
        });
        // 페이지 사이즈
        $form.find('select[name="size"]').on('change', function() {
            $form.find('input[name="page"]').val(0);
            $form.submit();
        });

        // ✅ 페이지네이션 클릭 처리
        $(document).on('click', '.product-pagination a[data-page]', function(e) {
            e.preventDefault();
            var page = $(this).data('page');
            if (page == null || $(this).closest('li').hasClass('disabled')) {
                return;
            }
            $form.find('input[name="page"]').val(page);
            $form.submit();
        });
    }
});

// GRID/LIST 초기 상태
if ($.cookie('display')) {
    view = $.cookie('display');
} else {
    view = 'grid';
}
if (view) display(view);
