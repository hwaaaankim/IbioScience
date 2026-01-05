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
			dataType: 'text'
		});
	}

	// ✅ "추가만" 하는 API (이미 있으면 EXISTS)
	function apiAddCheck(productId) {
		return $.ajax({
			url: '/api/customer/wishlist/add-check',
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

		// ✅ 전역은 "추가만" 동작
		add: function(productId) {
			if (!isAuthenticated()) {
				alert('로그인이 필요합니다.');
				return $.Deferred().reject().promise();
			}
			if (!productId) return $.Deferred().reject().promise();

			return apiAddCheck(productId)
				.done(function(res) {
					// res: { count, action }  action = ADDED | EXISTS
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

				self.add(pid)
					.done(function(res) {
						if (!res || !res.action) {
							alert('처리 결과를 확인할 수 없습니다.');
							return;
						}

						// ✅ 전역은 삭제하지 않음
						if (res.action === 'ADDED') {
							$btn.addClass('active');
							alert('관심상품에 추가되었습니다.');
						} else if (res.action === 'EXISTS') {
							// 이미 담김: 안내만
							$btn.addClass('active'); // 이미 담긴 상태를 확실히 표시
							alert('이미 관심상품에 담긴 상품입니다.');
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
