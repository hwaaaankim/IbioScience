(function () {
    const $ = (s, p = document) => p.querySelector(s);
    const $$ = (s, p = document) => Array.from(p.querySelectorAll(s));

    const API_BASE = '/customer/api/estimate';

    const DELETE_POLICY = Object.freeze({
        ALWAYS: 'ALWAYS',
        BEFORE_CHECK_ONLY: 'BEFORE_CHECK_ONLY',
        CHECKED_BUT_NOT_ANSWERED_ONLY: 'CHECKED_BUT_NOT_ANSWERED_ONLY'
    });

    const state = {
        page: 0,
        size: 10,
        titleKeyword: '',
        from: '',
        to: '',
        sortBy: 'requestedAt',
        sortDir: 'desc',
        deletePolicy: resolveDeletePolicy(window.CUSTOMER_ESTIMATE_DELETE_POLICY)
    };

    const el = {
        pageSize: $('#customer-estimate-list-page-size'),
        dateFrom: $('#customer-estimate-list-date-from'),
        dateTo: $('#customer-estimate-list-date-to'),
        keyword: $('#customer-estimate-list-keyword'),
        searchBtn: $('#customer-estimate-list-search-btn'),
        checkAll: $('#customer-estimate-list-check-all'),
        tbody: $('#customer-estimate-list-tbody'),
        pagination: $('#customer-estimate-list-pagination'),
        deleteBtn: $('#customer-estimate-list-delete-btn')
    };

    function resolveDeletePolicy(value) {
        const normalized = String(value || '').trim().toUpperCase();

        if (normalized === DELETE_POLICY.BEFORE_CHECK_ONLY) {
            return DELETE_POLICY.BEFORE_CHECK_ONLY;
        }

        if (normalized === DELETE_POLICY.CHECKED_BUT_NOT_ANSWERED_ONLY) {
            return DELETE_POLICY.CHECKED_BUT_NOT_ANSWERED_ONLY;
        }

        return DELETE_POLICY.ALWAYS;
    }

    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    async function fetchJson(url, options) {
        const res = await fetch(url, options);

        if (!res.ok) {
            const text = await res.text();
            throw new Error(text || '요청 처리 중 오류가 발생했습니다.');
        }

        return res.json();
    }

    function buildQueryString() {
        const params = new URLSearchParams();

        params.set('page', state.page);
        params.set('size', state.size);
        params.set('sortBy', state.sortBy);
        params.set('sortDir', state.sortDir);

        if (state.titleKeyword) {
            params.set('titleKeyword', state.titleKeyword);
        }

        if (state.from) {
            params.set('from', state.from);
        }

        if (state.to) {
            params.set('to', state.to);
        }

        return params.toString();
    }

    function parseDateLike(value) {
        if (value == null || value === '') {
            return null;
        }

        if (value instanceof Date) {
            return Number.isNaN(value.getTime()) ? null : value;
        }

        if (Array.isArray(value)) {
            const year = Number(value[0] || 0);
            const month = Number(value[1] || 1);
            const day = Number(value[2] || 1);
            const hour = Number(value[3] || 0);
            const minute = Number(value[4] || 0);
            const second = Number(value[5] || 0);
            const nano = Number(value[6] || 0);
            const millisecond = Math.floor(nano / 1000000);

            const date = new Date(year, month - 1, day, hour, minute, second, millisecond);
            return Number.isNaN(date.getTime()) ? null : date;
        }

        if (typeof value === 'number') {
            const date = new Date(value);
            return Number.isNaN(date.getTime()) ? null : date;
        }

        if (typeof value === 'string') {
            const trimmed = value.trim();
            if (!trimmed) {
                return null;
            }

            const isoCandidate = trimmed.includes('T') ? trimmed : trimmed.replace(' ', 'T');
            let date = new Date(isoCandidate);

            if (!Number.isNaN(date.getTime())) {
                return date;
            }

            const match = isoCandidate.match(
                /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?$/
            );

            if (match) {
                date = new Date(
                    Number(match[1]),
                    Number(match[2]) - 1,
                    Number(match[3]),
                    Number(match[4]),
                    Number(match[5]),
                    Number(match[6] || 0)
                );
                return Number.isNaN(date.getTime()) ? null : date;
            }
        }

        return null;
    }

    function formatDateTime(value) {
        const date = parseDateLike(value);
        if (!date) {
            return '-';
        }

        const y = date.getFullYear();
        const m = String(date.getMonth() + 1).padStart(2, '0');
        const d = String(date.getDate()).padStart(2, '0');
        const hh = String(date.getHours()).padStart(2, '0');
        const mm = String(date.getMinutes()).padStart(2, '0');

        return y + '-' + m + '-' + d + ' ' + hh + ':' + mm;
    }

    function buildTitleText(item) {
        const title = String(item.title || '').trim();
        const productSummary = String(item.productSummary || '').trim();

        if (title && productSummary) {
            return title + ' (' + productSummary + ')';
        }

        if (title) {
            return title;
        }

        if (productSummary) {
            return productSummary;
        }

        return '-';
    }

    function isItemDeletable(item) {
        const checkStatus = String(item.checkStatus || '').trim().toUpperCase();
        const answerStatus = String(item.answerStatus || '').trim().toUpperCase();

        switch (state.deletePolicy) {
            case DELETE_POLICY.ALWAYS:
                return true;

            case DELETE_POLICY.BEFORE_CHECK_ONLY:
                return checkStatus === 'UNCHECKED';

            case DELETE_POLICY.CHECKED_BUT_NOT_ANSWERED_ONLY:
                return checkStatus === 'CHECKED' && answerStatus !== 'ANSWERED';

            default:
                return true;
        }
    }

    function getDeleteBlockedMessage() {
        switch (state.deletePolicy) {
            case DELETE_POLICY.BEFORE_CHECK_ONLY:
                return '선택한 문의 중 관리자 확인이 완료된 항목이 포함되어 있어 삭제할 수 없습니다.';

            case DELETE_POLICY.CHECKED_BUT_NOT_ANSWERED_ONLY:
                return '선택한 문의 중 삭제 정책에 맞지 않는 항목이 포함되어 있어 삭제할 수 없습니다.';

            default:
                return '';
        }
    }

    function renderRows(data) {
        const items = data.content || [];

        el.tbody.innerHTML = '';
        el.checkAll.checked = false;
        el.checkAll.indeterminate = false;
        el.checkAll.disabled = items.length === 0;

        if (!items.length) {
            el.tbody.innerHTML = ''
                + '<tr>'
                + '    <td colspan="8" style="text-align:center;padding:30px 10px;">조회된 견적문의가 없습니다.</td>'
                + '</tr>';

            updateDeleteState();
            return;
        }

        items.forEach(function (item, index) {
            const no = data.totalElements - (data.page * data.size) - index;
            const canDelete = isItemDeletable(item);
            const titleText = buildTitleText(item);

            const tr = document.createElement('tr');
            tr.setAttribute('data-estimate-id', String(item.id));
            tr.setAttribute('data-can-delete', canDelete ? 'Y' : 'N');

            tr.innerHTML = ''
                + '<td>'
                + '    <input type="checkbox"'
                + '        class="customer-estimate-list-check"'
                + '        data-id="' + escapeHtml(item.id) + '"'
                + '        data-can-delete="' + (canDelete ? 'Y' : 'N') + '">'
                + '</td>'
                + '<td>' + escapeHtml(no) + '</td>'
                + '<td>' + escapeHtml(item.answerStatusLabel) + '</td>'
                + '<td class="customer-estimate-list-mobile-hidden">' + escapeHtml(item.checkStatusLabel) + '</td>'
                + '<td class="estimateList-td-title">'
                + '    <div class="customer-estimate-list-title-text"'
                + '         title="' + escapeHtml(titleText) + '"'
                + '         style="max-width:100%;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">'
                +          escapeHtml(titleText)
                + '    </div>'
                + '</td>'
                + '<td>' + escapeHtml(formatDateTime(item.requestedAt)) + '</td>'
                + '<td class="customer-estimate-list-mobile-hidden">' + escapeHtml(formatDateTime(item.checkedAt)) + '</td>'
                + '<td class="customer-estimate-list-mobile-hidden">' + escapeHtml(formatDateTime(item.answeredAt)) + '</td>';

            el.tbody.appendChild(tr);
        });

        updateDeleteState();
    }

    function renderPagination(data) {
        el.pagination.innerHTML = '';

        const totalPages = data.totalPages || 0;
        if (totalPages <= 0) {
            return;
        }

        function appendPageItem(label, page, disabled, active) {
            const li = document.createElement('li');
            li.className = 'page-item'
                + (disabled ? ' disabled' : '')
                + (active ? ' active' : '');

            const a = document.createElement('a');
            a.className = 'page-link';
            a.href = '#';
            a.textContent = label;

            a.addEventListener('click', function (e) {
                e.preventDefault();

                if (disabled || active) {
                    return;
                }

                state.page = page;
                loadList();
            });

            li.appendChild(a);
            el.pagination.appendChild(li);
        }

        appendPageItem('First', 0, data.page === 0, false);
        appendPageItem('Previous', Math.max(0, data.page - 1), data.page === 0, false);

        for (let p = 0; p < totalPages; p++) {
            appendPageItem(String(p + 1), p, false, p === data.page);
        }

        appendPageItem('Next', Math.min(totalPages - 1, data.page + 1), data.page >= totalPages - 1, false);
        appendPageItem('Last', totalPages - 1, data.page >= totalPages - 1, false);
    }

    function updateDeleteState() {
        const checks = $$('.customer-estimate-list-check', el.tbody);
        const checked = checks.filter(function (chk) {
            return chk.checked;
        });

        const anyChecked = checked.length > 0;
        const allCheckedDeletable = checked.every(function (chk) {
            return chk.dataset.canDelete === 'Y';
        });

        el.deleteBtn.disabled = !(anyChecked && allCheckedDeletable);

        if (!anyChecked) {
            el.deleteBtn.removeAttribute('title');
            return;
        }

        if (!allCheckedDeletable) {
            const msg = getDeleteBlockedMessage();
            if (msg) {
                el.deleteBtn.setAttribute('title', msg);
            } else {
                el.deleteBtn.removeAttribute('title');
            }
            return;
        }

        el.deleteBtn.removeAttribute('title');
    }

    function updateSortButtonState() {
        $$('.customer-estimate-list-sort-btn').forEach(function (btn) {
            const matched = btn.dataset.sort === state.sortBy && btn.dataset.dir === state.sortDir;
            btn.classList.toggle('active', matched);
        });
    }

    async function loadList() {
        try {
            const data = await fetchJson(API_BASE + '/list?' + buildQueryString());
            renderRows(data);
            renderPagination(data);
            updateSortButtonState();
        } catch (e) {
            console.error(e);
            alert(e.message || '견적 목록 조회 중 오류가 발생했습니다.');
        }
    }

    function syncCheckAllState() {
        const checks = $$('.customer-estimate-list-check', el.tbody);
        const checkedCount = checks.filter(function (chk) {
            return chk.checked;
        }).length;

        el.checkAll.checked = checks.length > 0 && checkedCount === checks.length;
        el.checkAll.indeterminate = checkedCount > 0 && checkedCount < checks.length;
    }

    function bindEvents() {
        el.searchBtn.addEventListener('click', function () {
            state.page = 0;
            state.size = Number(el.pageSize.value || 10);
            state.titleKeyword = el.keyword.value.trim();
            state.from = el.dateFrom.value;
            state.to = el.dateTo.value;
            loadList();
        });

        el.keyword.addEventListener('keydown', function (e) {
            if (e.key !== 'Enter') {
                return;
            }

            e.preventDefault();
            state.page = 0;
            state.size = Number(el.pageSize.value || 10);
            state.titleKeyword = el.keyword.value.trim();
            state.from = el.dateFrom.value;
            state.to = el.dateTo.value;
            loadList();
        });

        el.pageSize.addEventListener('change', function () {
            state.page = 0;
            state.size = Number(el.pageSize.value || 10);
            loadList();
        });

        el.checkAll.addEventListener('change', function () {
            const checked = el.checkAll.checked;

            $$('.customer-estimate-list-check', el.tbody).forEach(function (chk) {
                chk.checked = checked;
            });

            syncCheckAllState();
            updateDeleteState();
        });

        el.tbody.addEventListener('change', function (e) {
            if (!e.target.classList.contains('customer-estimate-list-check')) {
                return;
            }

            syncCheckAllState();
            updateDeleteState();
        });

        document.addEventListener('click', function (e) {
            const btn = e.target.closest('.customer-estimate-list-sort-btn');
            if (!btn) {
                return;
            }

            state.sortBy = btn.dataset.sort;
            state.sortDir = btn.dataset.dir;
            state.page = 0;
            loadList();
        });

        el.deleteBtn.addEventListener('click', async function () {
            const checkedInputs = $$('.customer-estimate-list-check', el.tbody).filter(function (chk) {
                return chk.checked;
            });

            const ids = checkedInputs.map(function (chk) {
                return Number(chk.dataset.id);
            });

            if (!ids.length) {
                return;
            }

            const hasBlocked = checkedInputs.some(function (chk) {
                return chk.dataset.canDelete !== 'Y';
            });

            if (hasBlocked) {
                alert(getDeleteBlockedMessage() || '선택한 문의 중 삭제할 수 없는 항목이 포함되어 있습니다.');
                return;
            }

            if (!confirm(ids.length + '건을 삭제하시겠습니까?')) {
                return;
            }

            try {
                await fetchJson(API_BASE, {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        estimateIds: ids
                    })
                });

                alert('삭제가 완료되었습니다.');
                state.page = 0;
                loadList();
            } catch (e) {
                console.error(e);
                alert(e.message || '삭제 중 오류가 발생했습니다.');
            }
        });
    }

    function init() {
        bindEvents();
        loadList();
    }

    init();
})();