/* global jQuery */
(function(window, $) {
	'use strict';

	if (!$) {
		console.error('[wish] jQuery not found');
		return;
	}

	function isAuthenticated() {
		return (window.__isAuthenticated === true);
	}

	function toNumber(v) {
		var n = Number(v);
		return isNaN(n) ? 0 : n;
	}

	function apiGetCount() {
		return $.ajax({
			url: '/api/customer/wishlist/count',
			method: 'GET',
			dataType: 'text' // count는 숫자 텍스트로 와도 안전
		});
	}

	// ✅ toggle은 JSON(action+count)로 받음
	function apiToggle(productId) {
		return $.ajax({
			url: '/api/customer/wishlist/toggle',
			method: 'POST',
			dataType: 'json',
			data: { productId: productId }
		});
	}

	function setHeaderCount(n) {
		$('.front-header-wish-count').text(toNumber(n));
	}

	var IbioWish = {
		refreshCount: function() {
			if (!isAuthenticated()) {
				setHeaderCount(0);
				return;
			}

			apiGetCount()
				.done(function(res) {
					setHeaderCount(res);
				})
				.fail(function() { });
		},

		toggle: function(productId) {
			if (!isAuthenticated()) {
				alert('로그인이 필요합니다.');
				return $.Deferred().reject().promise();
			}
			if (!productId) return $.Deferred().reject().promise();

			return apiToggle(productId)
				.done(function(res) {
					// res: { count, action }
					if (res && typeof res.count !== 'undefined') {
						setHeaderCount(res.count);
					}
				});
		},

		bindGlobalButtons: function() {
			var self = this;

			$(document).on('click', '.ibio-wish-toggle', function(e) {
				e.preventDefault();

				var $btn = $(this);
				var pid = Number($btn.data('product-id'));
				if (!pid) return;

				self.toggle(pid)
					.done(function(res) {
						if (!res || !res.action) {
							alert('처리 결과를 확인할 수 없습니다.');
							return;
						}

						// ✅ 서버 action 기반으로 UI 확정
						if (res.action === 'ADDED') {
							$btn.addClass('active');
							alert('관심상품에 추가되었습니다.');
						} else if (res.action === 'REMOVED') {
							$btn.removeClass('active');
							alert('관심상품에서 삭제되었습니다.');
						} else {
							alert('처리 중 오류가 발생했습니다.');
						}
					})
					.fail(function(xhr) {
						if (xhr && xhr.status === 401) {
							alert('로그인이 필요합니다.');
						} else {
							alert('처리 중 오류가 발생했습니다.');
						}
					});
			});
		},

		init: function() {
			this.bindGlobalButtons();
			this.refreshCount();
		}
	};

	window.IbioWish = IbioWish;

	$(function() {
		IbioWish.init();
	});

})(window, jQuery);
