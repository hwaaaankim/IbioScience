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

	function normalizeProductType(v) {
		var type = String(v || '').toUpperCase();
		if (type === 'DEALER') {
			return 'DEALER';
		}
		return 'COMPANY';
	}

	function apiGetCount() {
		return $.ajax({
			url: '/api/customer/wishlist/count',
			method: 'GET',
			dataType: 'text'
		});
	}

	function apiAddCheck(productType, targetId) {
		return $.ajax({
			url: '/api/customer/wishlist/add-check',
			method: 'POST',
			dataType: 'json',
			data: {
				productType: productType,
				targetId: targetId
			}
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

		add: function(productType, targetId) {
			if (!isAuthenticated()) {
				alert('로그인이 필요합니다.');
				return $.Deferred().reject().promise();
			}
			if (!targetId) {
				return $.Deferred().reject().promise();
			}

			return apiAddCheck(productType, targetId)
				.done(function(res) {
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
				var targetId = Number($btn.data('product-id'));
				var productType = normalizeProductType($btn.data('product-source-type'));

				if (!targetId) {
					return;
				}

				self.add(productType, targetId)
					.done(function(res) {
						if (!res || !res.action) {
							alert('처리 결과를 확인할 수 없습니다.');
							return;
						}

						if (res.action === 'ADDED') {
							$btn.addClass('active');
							alert('관심상품에 추가되었습니다.');
						} else if (res.action === 'EXISTS') {
							$btn.addClass('active');
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