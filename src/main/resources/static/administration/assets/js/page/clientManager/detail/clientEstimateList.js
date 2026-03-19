(function() {
    const MEMBER_ID = window.CRM_ESTIMATE_LIST_MEMBER_ID;
    const API_BASE = '/admin/root/api/client/' + MEMBER_ID + '/estimates';

    const $ = (selector, parent = document) => parent.querySelector(selector);
    const $$ = (selector, parent = document) => Array.from(parent.querySelectorAll(selector));

    const state = {
        page: 1,
        size: 10,
        detailEstimateId: null,
        bootstrapModal: null,
        quill: null,
        editorImageInput: null,
        mailAttachments: []
    };

    const el = {
        size: $('#crm-estimate-list-size'),
        dateType: $('#crm-estimate-list-date-type'),
        fromDate: $('#crm-estimate-list-from-date'),
        toDate: $('#crm-estimate-list-to-date'),
        stateCheckboxes: $$('.crm-estimate-list-state'),
        searchBtn: $('#crm-estimate-list-search-btn'),
        totalCount: $('#crm-estimate-list-total-count'),
        tbody: $('#crm-estimate-list-tbody'),
        pagination: $('#crm-estimate-list-pagination'),
        checkAll: $('#crm-estimate-list-check-all'),

        detailModal: $('#crm-estimate-list-detail-modal'),
        detailTitle: $('#crm-estimate-list-detail-title'),
        detailMemberName: $('#crm-estimate-list-detail-member-name'),
        detailMemberUserId: $('#crm-estimate-list-detail-member-user-id'),
        detailMemberEmail: $('#crm-estimate-list-detail-member-email'),
        detailMemberContactPhone: $('#crm-estimate-list-detail-member-contact-phone'),
        detailRequestedAt: $('#crm-estimate-list-detail-requested-at'),
        detailCheckedAt: $('#crm-estimate-list-detail-checked-at'),
        detailAnsweredAt: $('#crm-estimate-list-detail-answered-at'),
        detailContent: $('#crm-estimate-list-detail-content'),
        detailAttachments: $('#crm-estimate-list-detail-attachments'),
        detailItems: $('#crm-estimate-list-detail-items'),

        mailSubject: $('#crm-estimate-list-mail-subject'),
        mailAttachments: $('#crm-estimate-list-mail-attachments'),
        mailAttachmentPreview: $('#crm-estimate-list-mail-attachment-preview'),
        sendBtn: $('#crm-estimate-list-send-btn')
    };

    document.addEventListener('DOMContentLoaded', function() {
        initializeEditor();
        initializeModal();
        bindEvents();
        loadList(1);
    });

    function initializeEditor() {
        state.quill = new Quill('#crm-estimate-list-mail-editor', {
            theme: 'snow',
            placeholder: '견적서 메일 내용을 입력해주세요.',
            modules: {
                toolbar: [
                    [{ header: [1, 2, 3, false] }],
                    ['bold', 'italic', 'underline', 'strike'],
                    [{ list: 'ordered' }, { list: 'bullet' }],
                    [{ align: [] }],
                    ['link', 'image'],
                    ['clean']
                ]
            }
        });

        const toolbar = state.quill.getModule('toolbar');
        toolbar.addHandler('image', handleEditorImageInsert);

        state.editorImageInput = createEditorImageInput();
        state.quill.on('text-change', updateSendButtonState);
    }

    function initializeModal() {
        state.bootstrapModal = new bootstrap.Modal(el.detailModal);
    }

    function bindEvents() {
        el.searchBtn.addEventListener('click', function() {
            loadList(1);
        });

        el.size.addEventListener('change', function() {
            state.size = parseInt(el.size.value, 10) || 10;
            loadList(1);
        });

        el.checkAll.addEventListener('change', function() {
            const checked = el.checkAll.checked;
            $$('.crm-estimate-list-row-check', el.tbody).forEach(function(checkbox) {
                checkbox.checked = checked;
            });
        });

        el.tbody.addEventListener('change', function(event) {
            if (!event.target.classList.contains('crm-estimate-list-row-check')) {
                return;
            }
            syncCheckAllState();
        });

        el.mailSubject.addEventListener('input', updateSendButtonState);

        el.mailAttachments.addEventListener('change', function() {
            appendSelectedAttachments();
        });

        el.mailAttachmentPreview.addEventListener('click', function(event) {
            const removeButton = event.target.closest('.crm-estimate-list-mail-attachment-remove-btn');
            if (!removeButton) {
                return;
            }

            const index = parseInt(removeButton.getAttribute('data-index'), 10);
            if (Number.isNaN(index)) {
                return;
            }

            removeAttachment(index);
        });

        el.sendBtn.addEventListener('click', sendEstimateMail);

        el.detailModal.addEventListener('hidden.bs.modal', function() {
            resetMailFormState();
        });
    }

    function createEditorImageInput() {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = 'image/*';
        input.style.display = 'none';

        input.addEventListener('change', async function() {
            const file = input.files && input.files[0] ? input.files[0] : null;
            if (!file) {
                return;
            }

            try {
                const dataUrl = await readFileAsDataUrl(file);
                insertImageToEditor(dataUrl);
            } catch (error) {
                alert(error.message || '이미지 업로드 중 오류가 발생했습니다.');
            } finally {
                input.value = '';
                updateSendButtonState();
            }
        });

        document.body.appendChild(input);
        return input;
    }

    function handleEditorImageInsert() {
        if (!state.editorImageInput) {
            return;
        }
        state.editorImageInput.click();
    }

    function readFileAsDataUrl(file) {
        return new Promise(function(resolve, reject) {
            const reader = new FileReader();

            reader.onload = function() {
                resolve(reader.result);
            };

            reader.onerror = function() {
                reject(new Error('이미지 파일을 읽을 수 없습니다.'));
            };

            reader.readAsDataURL(file);
        });
    }

    function insertImageToEditor(dataUrl) {
        const range = state.quill.getSelection(true);
        const index = range ? range.index : state.quill.getLength();

        state.quill.insertEmbed(index, 'image', dataUrl, 'user');
        state.quill.insertText(index + 1, '\n', 'user');
        state.quill.setSelection(index + 2, 0, 'silent');
    }

    function appendSelectedAttachments() {
        const files = Array.from(el.mailAttachments.files || []);
        if (!files.length) {
            return;
        }

        files.forEach(function(file) {
            state.mailAttachments.push(file);
        });

        el.mailAttachments.value = '';
        renderAttachmentPreview();
        updateSendButtonState();
    }

    function removeAttachment(index) {
        if (index < 0 || index >= state.mailAttachments.length) {
            return;
        }

        state.mailAttachments.splice(index, 1);
        renderAttachmentPreview();
        updateSendButtonState();
    }

    function renderAttachmentPreview() {
        if (!state.mailAttachments.length) {
            el.mailAttachmentPreview.innerHTML = '';
            return;
        }

        el.mailAttachmentPreview.innerHTML = state.mailAttachments.map(function(file, index) {
            return `
                <li class="d-flex align-items-center justify-content-between gap-2 border rounded px-2 py-2 mb-2">
                    <span class="text-break">${escapeHtml(file.name)}</span>
                    <button type="button"
                            class="btn btn-sm btn-outline-danger crm-estimate-list-mail-attachment-remove-btn"
                            data-index="${index}"
                            aria-label="첨부파일 삭제">X</button>
                </li>
            `;
        }).join('');
    }

    function resetMailFormState() {
        state.detailEstimateId = null;
        state.mailAttachments = [];
        el.mailSubject.value = '';
        el.mailAttachments.value = '';
        el.mailAttachmentPreview.innerHTML = '';

        if (state.quill) {
            state.quill.setText('');
        }

        updateSendButtonState();
    }

    function syncCheckAllState() {
        const rowChecks = $$('.crm-estimate-list-row-check', el.tbody);
        if (!rowChecks.length) {
            el.checkAll.checked = false;
            return;
        }

        const allChecked = rowChecks.every(function(checkbox) {
            return checkbox.checked;
        });

        el.checkAll.checked = allChecked;
    }

    function getFilters() {
        const params = new URLSearchParams();
        params.append('page', state.page);
        params.append('size', el.size.value || '10');
        params.append('dateType', el.dateType.value || 'REQUESTED_AT');

        if (el.fromDate.value) {
            params.append('fromDate', el.fromDate.value);
        }

        if (el.toDate.value) {
            params.append('toDate', el.toDate.value);
        }

        el.stateCheckboxes
            .filter(function(checkbox) {
                return checkbox.checked;
            })
            .forEach(function(checkbox) {
                params.append('states', checkbox.value);
            });

        return params;
    }

    async function loadList(page) {
        state.page = page;

        renderLoadingRow();

        try {
            const response = await fetch(API_BASE + '?' + getFilters().toString(), {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('견적서 목록 조회에 실패했습니다.');
            }

            const data = await response.json();
            renderList(data);
            renderPagination(data);
        } catch (error) {
            renderErrorRow(error.message || '견적서 목록 조회 중 오류가 발생했습니다.');
        }
    }

    function renderLoadingRow() {
        el.tbody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center py-5 text-muted">데이터를 불러오는 중입니다.</td>
            </tr>
        `;
    }

    function renderErrorRow(message) {
        el.tbody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center py-5 text-danger">${escapeHtml(message)}</td>
            </tr>
        `;
        el.totalCount.textContent = '0';
        el.pagination.innerHTML = '';
    }

    function renderList(data) {
        const content = Array.isArray(data.content) ? data.content : [];
        el.totalCount.textContent = data.totalElements || 0;

        if (content.length === 0) {
            el.tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="text-center py-5 text-muted">조회된 견적서가 없습니다.</td>
                </tr>
            `;
            el.checkAll.checked = false;
            return;
        }

        const rows = content.map(function(row) {
            return `
                <tr data-estimate-id="${row.estimateId}">
                    <td class="text-center">
                        <input type="checkbox" class="crm-estimate-list-row-check" value="${row.estimateId}">
                    </td>
                    <td>
                        <div class="crm-estimate-list-applicant-box">
                            <div class="crm-estimate-list-applicant-main">${escapeHtml(nvl(row.memberName, '-'))}</div>
                            <div class="crm-estimate-list-applicant-sub">${escapeHtml(nvl(row.memberUserId, '-'))}</div>
                            <div class="crm-estimate-list-applicant-sub">${escapeHtml(nvl(row.memberEmail, '-'))}</div>
                            <div class="crm-estimate-list-applicant-sub">${escapeHtml(nvl(row.memberContactPhone, '-'))}</div>
                        </div>
                    </td>
                    <td class="text-center">${row.itemCount || 0}</td>
                    <td class="text-center">
                        ${renderCheckStatusBadge(row.checkStatus)}
                    </td>
                    <td class="text-center">
                        ${renderAnswerStatusBadge(row.answerStatus)}
                    </td>
                    <td class="text-center">
                        <button type="button"
                                class="btn btn-sm btn-outline-primary crm-estimate-list-detail-btn"
                                data-estimate-id="${row.estimateId}">
                            상세보기
                        </button>
                    </td>
                </tr>
            `;
        }).join('');

        el.tbody.innerHTML = rows;

        $$('.crm-estimate-list-detail-btn', el.tbody).forEach(function(button) {
            button.addEventListener('click', function() {
                const estimateId = button.getAttribute('data-estimate-id');
                openDetailModal(estimateId);
            });
        });

        el.checkAll.checked = false;
    }

    function renderPagination(data) {
        const totalPages = data.totalPages || 0;
        const currentPage = data.page || 1;

        if (totalPages <= 1) {
            el.pagination.innerHTML = '';
            return;
        }

        const items = [];

        items.push(renderPageItem('이전', currentPage - 1, currentPage <= 1));

        const startPage = Math.max(1, currentPage - 2);
        const endPage = Math.min(totalPages, currentPage + 2);

        for (let page = startPage; page <= endPage; page++) {
            items.push(renderPageItem(String(page), page, false, page === currentPage));
        }

        items.push(renderPageItem('다음', currentPage + 1, currentPage >= totalPages));

        el.pagination.innerHTML = items.join('');

        $$('.crm-estimate-list-page-link', el.pagination).forEach(function(link) {
            link.addEventListener('click', function(e) {
                e.preventDefault();

                const disabled = link.getAttribute('data-disabled') === 'true';
                if (disabled) {
                    return;
                }

                const page = parseInt(link.getAttribute('data-page'), 10);
                if (!page || page < 1) {
                    return;
                }

                loadList(page);
            });
        });
    }

    function renderPageItem(label, page, disabled, active) {
        return `
            <li class="page-item ${disabled ? 'disabled' : ''} ${active ? 'active' : ''}">
                <a href="#"
                   class="page-link crm-estimate-list-page-link"
                   data-page="${page}"
                   data-disabled="${disabled ? 'true' : 'false'}">${label}</a>
            </li>
        `;
    }

    async function openDetailModal(estimateId) {
        state.detailEstimateId = estimateId;

        try {
            const response = await fetch(API_BASE + '/' + estimateId, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                }
            });

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.message || '견적서 상세 조회에 실패했습니다.');
            }

            fillDetail(data);
            initializeMailForm(data);
            state.bootstrapModal.show();

            loadList(state.page);
        } catch (error) {
            alert(error.message || '견적서 상세 조회 중 오류가 발생했습니다.');
        }
    }

    function fillDetail(data) {
        el.detailTitle.textContent = nvl(data.title, '-');
        el.detailMemberName.textContent = nvl(data.memberName, '-');
        el.detailMemberUserId.textContent = nvl(data.memberUserId, '-');
        el.detailMemberEmail.textContent = nvl(data.memberEmail, '-');
        el.detailMemberContactPhone.textContent = nvl(data.memberContactPhone, '-');
        el.detailRequestedAt.textContent = formatDateTime(data.requestedAt);
        el.detailCheckedAt.textContent = formatDateTime(data.checkedAt);
        el.detailAnsweredAt.textContent = formatDateTime(data.answeredAt);
        el.detailContent.textContent = nvl(data.detailContent, '-');

        renderDetailAttachments(data.attachments || []);
        renderDetailItems(data.items || []);
    }

    function renderDetailAttachments(attachments) {
        if (!attachments.length) {
            el.detailAttachments.innerHTML = '<li class="text-muted">첨부파일이 없습니다.</li>';
            return;
        }

        el.detailAttachments.innerHTML = attachments.map(function(attachment) {
            const url = attachment.fileUrl || '#';
            const fileName = escapeHtml(nvl(attachment.originalFileName, '첨부파일'));
            return `<li><a href="${url}" target="_blank" rel="noopener noreferrer">${fileName}</a></li>`;
        }).join('');
    }

    function renderDetailItems(items) {
        if (!items.length) {
            el.detailItems.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center text-muted py-4">등록된 제품이 없습니다.</td>
                </tr>
            `;
            return;
        }

        el.detailItems.innerHTML = items.map(function(item) {
            return `
                <tr>
                    <td class="text-center">${escapeHtml(nvl(item.largeCategoryName, '-'))}</td>
                    <td class="text-center">${escapeHtml(nvl(item.mediumCategoryName, '-'))}</td>
                    <td class="text-center">${escapeHtml(nvl(item.smallCategoryName, '-'))}</td>
                    <td class="text-center">${escapeHtml(nvl(item.brandName, '-'))}</td>
                    <td>${escapeHtml(nvl(item.productName, '-'))}</td>
                    <td class="text-center">${escapeHtml(nvl(item.productCode, '-'))}</td>
                    <td class="text-center">${item.quantity || 0}</td>
                </tr>
            `;
        }).join('');
    }

    function initializeMailForm(detail) {
        const title = nvl(detail.title, '견적 문의');
        el.mailSubject.value = '[견적서 답변] ' + title;

        const defaultHtml = [
            '<p>안녕하세요. 아이바이오사이언스입니다.</p>',
            '<p>문의해주신 견적 요청에 대한 답변드립니다.</p>',
            '<p>아래 내용을 확인 부탁드립니다.</p>',
            '<p><br></p>',
            '<p>감사합니다.</p>'
        ].join('');

        state.quill.root.innerHTML = defaultHtml;
        state.mailAttachments = [];
        el.mailAttachments.value = '';
        renderAttachmentPreview();
        updateSendButtonState();
    }

    function hasEditorContent() {
        if (!state.quill) {
            return false;
        }

        const text = (state.quill.getText() || '')
            .replace(/\u200B/g, '')
            .trim();

        const hasImage = !!state.quill.root.querySelector('img');

        return !!text || hasImage;
    }

    function updateSendButtonState() {
        const subject = (el.mailSubject.value || '').trim();
        el.sendBtn.disabled = !(subject && hasEditorContent() && state.detailEstimateId);
    }

    async function sendEstimateMail() {
        if (!state.detailEstimateId) {
            alert('선택된 견적서가 없습니다.');
            return;
        }

        const subject = (el.mailSubject.value || '').trim();
        const bodyHtml = state.quill.root.innerHTML;

        if (!subject) {
            alert('이메일 제목을 입력해주세요.');
            el.mailSubject.focus();
            return;
        }

        if (!hasEditorContent()) {
            alert('이메일 내용을 입력해주세요.');
            return;
        }

        const formData = new FormData();
        formData.append('request', new Blob([JSON.stringify({
            subject: subject,
            bodyHtml: bodyHtml
        })], { type: 'application/json' }));

        state.mailAttachments.forEach(function(file) {
            formData.append('attachments', file);
        });

        const csrfToken = $('meta[name="_csrf"]') ? $('meta[name="_csrf"]').getAttribute('content') : '';
        const csrfHeader = $('meta[name="_csrf_header"]') ? $('meta[name="_csrf_header"]').getAttribute('content') : '';

        const headers = {};
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        el.sendBtn.disabled = true;
        el.sendBtn.textContent = '발송 중...';

        try {
            const response = await fetch(API_BASE + '/' + state.detailEstimateId + '/send-email', {
                method: 'POST',
                headers: headers,
                body: formData
            });

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.message || '견적서 메일 발송에 실패했습니다.');
            }

            alert(data.message || '견적서 메일이 발송되었습니다.');
            state.bootstrapModal.hide();
            loadList(state.page);
        } catch (error) {
            alert(error.message || '견적서 메일 발송 중 오류가 발생했습니다.');
            updateSendButtonState();
        } finally {
            el.sendBtn.textContent = '견적서 보내기';
            updateSendButtonState();
        }
    }

    function renderCheckStatusBadge(checkStatus) {
        if (checkStatus === 'CHECKED') {
            return '<span class="crm-estimate-list-badge crm-estimate-list-badge-checked">확인완료</span>';
        }
        return '<span class="crm-estimate-list-badge crm-estimate-list-badge-unchecked">미확인</span>';
    }

    function renderAnswerStatusBadge(answerStatus) {
        if (answerStatus === 'ANSWERED') {
            return '<span class="crm-estimate-list-badge crm-estimate-list-badge-answered">답변완료</span>';
        }
        return '<span class="crm-estimate-list-badge crm-estimate-list-badge-checked">답변대기</span>';
    }

    function formatDateTime(value) {
        if (!value) {
            return '-';
        }

        if (Array.isArray(value)) {
            const year = value[0];
            const month = String(value[1]).padStart(2, '0');
            const day = String(value[2]).padStart(2, '0');
            const hour = String(value[3] || 0).padStart(2, '0');
            const minute = String(value[4] || 0).padStart(2, '0');
            return year + '-' + month + '-' + day + ' ' + hour + ':' + minute;
        }

        const date = new Date(value);
        if (isNaN(date.getTime())) {
            return String(value).replace('T', ' ');
        }

        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hour = String(date.getHours()).padStart(2, '0');
        const minute = String(date.getMinutes()).padStart(2, '0');

        return year + '-' + month + '-' + day + ' ' + hour + ':' + minute;
    }

    function nvl(value, defaultValue) {
        return value === null || value === undefined || value === '' ? defaultValue : value;
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }
})();