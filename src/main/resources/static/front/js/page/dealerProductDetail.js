/* global jQuery */
(function($) {
    "use strict";

    if (window.__dealerProductDetailPageInitialized === true) {
        return;
    }
    window.__dealerProductDetailPageInitialized = true;

    $(function() {
        // ============================
        // ====== 옵션보기 스크롤 ======
        // ============================
        var $optBtn = $("#btn-go-option");
        var $tabLink = $('.tabsslider .nav.nav-tabs a[href="#tab-option"]');
        var $scrollBlock = $("#tab-option");
        var shouldScroll = false;

        function scrollToOption() {
            if (!$scrollBlock.length) {
                return;
            }

            setTimeout(function() {
                var top = $scrollBlock.offset().top - 200;
                $("html, body").stop().animate({ scrollTop: top }, 450, "swing");
            }, 0);
        }

        if ($tabLink.length) {
            $tabLink.off("shown.bs.tab.optScroll").on("shown.bs.tab.optScroll", function() {
                if (shouldScroll) {
                    shouldScroll = false;
                    scrollToOption();
                }
            });
        }

        if ($optBtn.length) {
            $optBtn.off("click.optScroll").on("click.optScroll", function(e) {
                e.preventDefault();

                var isActive =
                    $tabLink.attr("aria-selected") === "true" ||
                    $tabLink.parent().hasClass("active");

                if (isActive) {
                    scrollToOption();
                } else {
                    shouldScroll = true;
                    $tabLink.tab("show");
                }
            });
        }

        // ==============================
        // ====== 옵션 총합 계산 ======
        // ==============================
        function recalcOptionTotal() {
            var total = 0;

            $(".product-list-option-table tbody tr").each(function() {
                var $row = $(this);
                var $check = $row.find(".product-list-row-check");

                if (!$check.length || !$check.is(":checked")) {
                    return;
                }

                var price = parseInt($check.data("price"), 10);
                if (isNaN(price)) {
                    price = 0;
                }

                var qty = parseInt($row.find(".product-list-qty-input").val(), 10);
                if (isNaN(qty) || qty < 1) {
                    qty = 1;
                }

                total += price * qty;
            });

            $(".product-list-total-price").text(total.toLocaleString("ko-KR"));
        }

        $(document)
            .off("change.dealerOptionTotal", ".product-list-row-check")
            .on("change.dealerOptionTotal", ".product-list-row-check", recalcOptionTotal);

        $(document)
            .off("change.dealerOptionQty keyup.dealerOptionQty", ".product-list-qty-input")
            .on("change.dealerOptionQty keyup.dealerOptionQty", ".product-list-qty-input", recalcOptionTotal);

        // ============================
        // ===== 딜러 리뷰 처리 =====
        // ============================
        var dealerProductId = window.DEALER_PRODUCT_DETAIL_ID || 0;
        var permissionUrl = window.DEALER_PRODUCT_REVIEW_PERMISSION_URL || "";
        var createUrl = window.DEALER_PRODUCT_REVIEW_CREATE_URL || "";

        var $contactsForm = $(".contacts-form");
        var loginMemberId = parseInt($contactsForm.data("login-member-id"), 10);
        if (isNaN(loginMemberId)) {
            loginMemberId = 0;
        }

        var $permissionMessage = $("#dealer-review-permission-message");
        var $reviewTitle = $("#review-title");
        var $reviewContent = $("#review-content");
        var $reviewImages = $("#review-images");
        var $reviewButton = $("#button-review");
        var $reviewCancelButton = $("#button-review-cancel");
        var $ratingInputs = $('input[name="rating"]');

        var $existingImagesWrap = $("#dealer-review-existing-images-wrap");
        var $existingImages = $("#dealer-review-existing-images");
        var $newImagesPreviewWrap = $("#dealer-review-new-images-preview-wrap");
        var $newImagesPreview = $("#dealer-review-new-images-preview");

        var isCreateAllowed = false;
        var lastPermissionMessage = "로그인 후 리뷰 작성 가능 여부를 확인합니다.";
        var isSubmitting = false;
        var isFormEditable = false;
        var currentMode = "create";
        var editingReviewId = null;

        function getCsrfHeader() {
            var headerMeta = document.querySelector('meta[name="_csrf_header"]');
            var tokenMeta = document.querySelector('meta[name="_csrf"]');

            if (!headerMeta || !tokenMeta) {
                return null;
            }

            return {
                headerName: headerMeta.getAttribute("content"),
                token: tokenMeta.getAttribute("content")
            };
        }

        function getUpdateUrl(reviewId) {
            return createUrl + "/" + reviewId;
        }

        function getDeleteUrl(reviewId) {
            return createUrl + "/" + reviewId;
        }

        function setPermissionMessage(type, message) {
            $permissionMessage
                .removeClass("alert-info alert-success alert-warning alert-danger")
                .addClass(type)
                .text(message);
        }

        function refreshButtonState() {
            var disabled = isSubmitting || !isFormEditable;
            var mainText = currentMode === "edit" ? "리뷰수정" : "리뷰작성";

            if (isSubmitting) {
                mainText = "처리중...";
            }

            $reviewButton
                .text(mainText)
                .toggleClass("disabled", disabled)
                .attr("aria-disabled", disabled ? "true" : "false");

            if (currentMode === "edit" && !isSubmitting) {
                $reviewCancelButton.show();
            } else {
                $reviewCancelButton.hide();
            }
        }

        function setFormEditable(editable) {
            isFormEditable = editable;

            $reviewContent.prop("readonly", !editable);
            $reviewImages.prop("disabled", !editable);
            $ratingInputs.prop("disabled", !editable);

            refreshButtonState();
        }

        function setSubmitting(submitting) {
            isSubmitting = submitting;
            refreshButtonState();
        }

        function clearSelectedFiles() {
            $reviewImages.val("");
            renderNewImagesPreview();
        }

        function clearExistingImagesPreview() {
            $existingImages.empty();
            $existingImagesWrap.hide();
        }

        function renderNewImagesPreview() {
            var input = $reviewImages[0];
            var files = input ? input.files : null;

            $newImagesPreview.empty();

            if (!files || files.length === 0) {
                $newImagesPreviewWrap.hide();
                return;
            }

            $newImagesPreviewWrap.show();

            Array.prototype.forEach.call(files, function(file) {
                if (!file || !file.type || file.type.indexOf("image/") !== 0) {
                    return;
                }

                var reader = new FileReader();
                reader.onload = function(e) {
                    var html = ''
                        + '<div class="col-xs-6 col-sm-3" style="margin-bottom:10px;">'
                        + '  <div class="thumbnail" style="margin-bottom:0;">'
                        + '    <img src="' + e.target.result + '" alt="" style="width:100%; height:120px; object-fit:cover;">'
                        + '    <div class="caption" style="padding:8px 5px;">'
                        + '      <p style="margin:0; font-size:12px; word-break:break-all;">' + escapeHtml(file.name) + '</p>'
                        + '    </div>'
                        + '  </div>'
                        + '</div>';
                    $newImagesPreview.append(html);
                };
                reader.readAsDataURL(file);
            });
        }

        function renderExistingImagesPreview(images) {
            $existingImages.empty();

            if (!images || images.length === 0) {
                $existingImagesWrap.hide();
                return;
            }

            $existingImagesWrap.show();

            $.each(images, function(index, image) {
                var html = ''
                    + '<div class="col-xs-6 col-sm-3 dealer-review-existing-image-item" style="margin-bottom:10px;">'
                    + '  <div class="thumbnail" style="margin-bottom:0;">'
                    + '    <img src="' + escapeHtml(image.url) + '" alt="" style="width:100%; height:120px; object-fit:cover;">'
                    + '    <div class="caption" style="padding:8px 5px;">'
                    + '      <p style="margin:0 0 8px 0; font-size:12px; word-break:break-all;">' + escapeHtml(image.name || "") + '</p>'
                    + '      <label style="margin:0; font-weight:normal;">'
                    + '        <input type="checkbox" class="dealer-review-delete-image-check" value="' + image.id + '"> 삭제'
                    + '      </label>'
                    + '    </div>'
                    + '  </div>'
                    + '</div>';

                $existingImages.append(html);
            });
        }

        function getCheckedRating() {
            var value = $('input[name="rating"]:checked').val();
            return value ? parseInt(value, 10) : null;
        }

        function setCheckedRating(rating) {
            $ratingInputs.prop("checked", false);

            if (rating) {
                $('input[name="rating"][value="' + rating + '"]').prop("checked", true);
            }
        }

        function getDeleteImageIds() {
            var ids = [];

            $(".dealer-review-delete-image-check:checked").each(function() {
                var id = parseInt($(this).val(), 10);
                if (!isNaN(id)) {
                    ids.push(id);
                }
            });

            return ids;
        }

        function extractRowImages($row) {
            var images = [];

            $row.find(".dealer-review-row-image-item").each(function() {
                var $item = $(this);
                var id = parseInt($item.attr("data-image-id"), 10);

                if (isNaN(id)) {
                    return;
                }

                images.push({
                    id: id,
                    url: $item.attr("data-image-url") || "",
                    name: $item.attr("data-image-name") || ""
                });
            });

            return images;
        }

        function resetReviewForm() {
            editingReviewId = null;
            currentMode = "create";

            $reviewTitle.text("리뷰작성");
            $reviewContent.val("");
            setCheckedRating(null);
            clearExistingImagesPreview();
            clearSelectedFiles();

            if (isCreateAllowed) {
                setPermissionMessage("alert-success", lastPermissionMessage);
                setFormEditable(true);
            } else {
                setPermissionMessage("alert-info", lastPermissionMessage);
                setFormEditable(false);
            }
        }

        function enterEditMode(reviewId, content, rating, images) {
            editingReviewId = reviewId;
            currentMode = "edit";

            $reviewTitle.text("리뷰수정");
            $reviewContent.val(content || "");
            setCheckedRating(rating || null);
            renderExistingImagesPreview(images || []);
            clearSelectedFiles();

            setPermissionMessage("alert-warning", "수정 모드입니다. 기존 이미지는 삭제 체크할 수 있고, 새 이미지는 추가 업로드할 수 있습니다.");
            setFormEditable(true);
        }

        function escapeHtml(value) {
            if (value == null) {
                return "";
            }

            return String(value)
                .replace(/&/g, "&amp;")
                .replace(/</g, "&lt;")
                .replace(/>/g, "&gt;")
                .replace(/"/g, "&quot;")
                .replace(/'/g, "&#39;");
        }

        function loadReviewPermission() {
            if (!dealerProductId || !permissionUrl) {
                lastPermissionMessage = "리뷰 대상 상품 정보가 올바르지 않습니다.";
                setPermissionMessage("alert-info", lastPermissionMessage);
                setFormEditable(false);
                return;
            }

            $.ajax({
                url: permissionUrl,
                type: "GET",
                success: function(res) {
                    if (res && res.canWrite === true) {
                        isCreateAllowed = true;
                        lastPermissionMessage = res.message || "리뷰를 작성할 수 있습니다.";
                        if (currentMode === "create") {
                            setPermissionMessage("alert-success", lastPermissionMessage);
                            setFormEditable(true);
                        }
                    } else {
                        isCreateAllowed = false;
                        lastPermissionMessage = (res && res.message) ? res.message : "리뷰를 작성할 수 없습니다.";
                        if (currentMode === "create") {
                            setPermissionMessage("alert-info", lastPermissionMessage);
                            setFormEditable(false);
                        }
                    }
                },
                error: function(xhr) {
                    isCreateAllowed = false;
                    lastPermissionMessage = "리뷰 작성 가능 여부를 확인하지 못했습니다.";

                    if (xhr && xhr.responseText) {
                        lastPermissionMessage = xhr.responseText;
                    }

                    if (currentMode === "create") {
                        setPermissionMessage("alert-info", lastPermissionMessage);
                        setFormEditable(false);
                    }
                }
            });
        }

        $(document)
            .off("change.dealerReviewFiles", "#review-images")
            .on("change.dealerReviewFiles", "#review-images", function() {
                renderNewImagesPreview();
            });

        $(document)
            .off("click.dealerReviewSubmit", "#button-review")
            .on("click.dealerReviewSubmit", "#button-review", function(e) {
                e.preventDefault();

                if (!isFormEditable || isSubmitting || $reviewButton.hasClass("disabled")) {
                    return;
                }

                var content = $.trim($reviewContent.val());
                var rating = getCheckedRating();

                if (!content) {
                    alert("리뷰 내용을 입력해 주세요.");
                    $reviewContent.focus();
                    return;
                }

                if (!rating) {
                    alert("별점을 선택해 주세요.");
                    return;
                }

                var formData = new FormData();
                formData.append("content", content);
                formData.append("rating", rating);

                if (currentMode === "edit") {
                    var deleteImageIds = getDeleteImageIds();
                    $.each(deleteImageIds, function(index, id) {
                        formData.append("deleteImageIds", id);
                    });
                }

                var files = $reviewImages[0] ? $reviewImages[0].files : null;
                if (files && files.length > 0) {
                    for (var i = 0; i < files.length; i++) {
                        formData.append("images", files[i]);
                    }
                }

                var csrf = getCsrfHeader();
                var requestUrl = currentMode === "edit" ? getUpdateUrl(editingReviewId) : createUrl;
                var requestType = currentMode === "edit" ? "PUT" : "POST";
                var successMessage = currentMode === "edit" ? "리뷰가 수정되었습니다." : "리뷰가 등록되었습니다.";

                setSubmitting(true);

                $.ajax({
                    url: requestUrl,
                    type: requestType,
                    data: formData,
                    processData: false,
                    contentType: false,
                    beforeSend: function(xhr) {
                        if (csrf) {
                            xhr.setRequestHeader(csrf.headerName, csrf.token);
                        }
                    },
                    success: function() {
                        alert(successMessage);
                        window.location.reload();
                    },
                    error: function(xhr) {
                        var msg = currentMode === "edit"
                            ? "리뷰 수정 중 오류가 발생했습니다."
                            : "리뷰 등록 중 오류가 발생했습니다.";

                        if (xhr && xhr.responseText) {
                            msg = xhr.responseText;
                        }

                        alert(msg);
                    },
                    complete: function() {
                        setSubmitting(false);
                    }
                });
            });

        $(document)
            .off("click.dealerReviewCancel", "#button-review-cancel")
            .on("click.dealerReviewCancel", "#button-review-cancel", function(e) {
                e.preventDefault();
                if (isSubmitting) {
                    return;
                }
                resetReviewForm();
            });

        $(document)
            .off("click.dealerReviewEdit", ".dealer-review-edit-btn")
            .on("click.dealerReviewEdit", ".dealer-review-edit-btn", function(e) {
                e.preventDefault();

                if (isSubmitting) {
                    return;
                }

                var $btn = $(this);
                var $row = $btn.closest("tr");
                var reviewId = parseInt($btn.attr("data-review-id"), 10);
                var rating = parseInt($btn.attr("data-review-rating"), 10);
                var content = $.trim($row.find(".dealer-review-hidden-content").val());
                var images = extractRowImages($row);

                if (isNaN(reviewId)) {
                    alert("수정할 리뷰 정보를 찾을 수 없습니다.");
                    return;
                }

                enterEditMode(reviewId, content, rating, images);

                $("html, body").stop().animate({
                    scrollTop: $("#dealer-review-form").offset().top - 120
                }, 300);
            });

        $(document)
            .off("click.dealerReviewDelete", ".dealer-review-delete-btn")
            .on("click.dealerReviewDelete", ".dealer-review-delete-btn", function(e) {
                e.preventDefault();

                if (isSubmitting) {
                    return;
                }

                var reviewId = parseInt($(this).attr("data-review-id"), 10);
                if (isNaN(reviewId)) {
                    alert("삭제할 리뷰 정보를 찾을 수 없습니다.");
                    return;
                }

                if (!confirm("정말 이 리뷰를 삭제하시겠습니까?")) {
                    return;
                }

                var csrf = getCsrfHeader();
                setSubmitting(true);

                $.ajax({
                    url: getDeleteUrl(reviewId),
                    type: "DELETE",
                    beforeSend: function(xhr) {
                        if (csrf) {
                            xhr.setRequestHeader(csrf.headerName, csrf.token);
                        }
                    },
                    success: function() {
                        alert("리뷰가 삭제되었습니다.");
                        window.location.reload();
                    },
                    error: function(xhr) {
                        var msg = "리뷰 삭제 중 오류가 발생했습니다.";
                        if (xhr && xhr.responseText) {
                            msg = xhr.responseText;
                        }
                        alert(msg);
                    },
                    complete: function() {
                        setSubmitting(false);
                    }
                });
            });

        resetReviewForm();
        loadReviewPermission();
    });
})(jQuery);