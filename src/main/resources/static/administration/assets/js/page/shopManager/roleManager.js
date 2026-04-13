(function () {
    'use strict';

    const ROLE_KEYS = ['master', 'operator', 'employ'];
    const ACTION_KEYS = ['view', 'create', 'update', 'delete'];
    const ACTION_LABELS = {
        view: '조회',
        create: '등록',
        update: '수정',
        delete: '삭제'
    };

    const state = {
        pages: [],
        dirty: false
    };

    document.addEventListener('DOMContentLoaded', function () {
        bindStaticEvents();
        loadPages();
    });

    function bindStaticEvents() {
        const reloadBtn = document.getElementById('admin-role-manager-reload-btn');
        const saveBtn = document.getElementById('admin-role-manager-save-btn');

        if (reloadBtn) {
            reloadBtn.addEventListener('click', function () {
                loadPages();
            });
        }

        if (saveBtn) {
            saveBtn.addEventListener('click', function () {
                savePages();
            });
        }

        document.addEventListener('click', function (event) {
            const bulkBtn = event.target.closest('.admin-role-manager-bulk-btn');
            if (bulkBtn) {
                const roleKey = bulkBtn.getAttribute('data-role-key');
                const bulkValue = bulkBtn.getAttribute('data-bulk-value') === 'true';
                applyBulkRole(roleKey, bulkValue);
            }
        });

        document.addEventListener('change', function (event) {
            const target = event.target;
            if (!target.classList.contains('admin-role-manager-switch-input')) {
                return;
            }

            const pageId = Number(target.getAttribute('data-page-id'));
            const roleKey = target.getAttribute('data-role-key');
            const actionKey = target.getAttribute('data-action-key');
            const checked = target.checked;

            updateSwitchState(pageId, roleKey, actionKey, checked);
        });
    }

    async function loadPages() {
        toggleLoading(true);
        hideMessage();

        try {
            const response = await fetch('/admin/root/api/role-manager/pages', {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('권한 목록 조회에 실패했습니다.');
            }

            const data = await response.json();

            state.pages = Array.isArray(data.pages) ? data.pages : [];
            state.dirty = false;

            renderGroups();
            renderDirtyBadge();
        } catch (error) {
            console.error(error);
            showMessage(error.message || '권한 목록을 불러오는 중 오류가 발생했습니다.', false);
        } finally {
            toggleLoading(false);
        }
    }

    async function savePages() {
        hideMessage();

        try {
            const response = await fetch('/admin/root/api/role-manager/permissions', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify({
                    pages: state.pages.map(function (page) {
                        return {
                            pageId: page.pageId,
                            master: clonePermissionState(page.master),
                            operator: clonePermissionState(page.operator),
                            employ: clonePermissionState(page.employ)
                        };
                    })
                })
            });

            const result = await response.json().catch(function () {
                return {};
            });

            if (!response.ok) {
                throw new Error(result.message || '권한 저장에 실패했습니다.');
            }

            state.dirty = false;
            renderDirtyBadge();
            showMessage(result.message || '권한이 정상 저장되었습니다.', true);
        } catch (error) {
            console.error(error);
            showMessage(error.message || '권한 저장 중 오류가 발생했습니다.', false);
        }
    }

    function applyBulkRole(roleKey, enabled) {
        if (!ROLE_KEYS.includes(roleKey)) {
            return;
        }

        state.pages.forEach(function (page) {
            page[roleKey] = {
                view: enabled,
                create: enabled,
                update: enabled,
                delete: enabled
            };
        });

        state.dirty = true;
        renderGroups();
        renderDirtyBadge();
    }

    function updateSwitchState(pageId, roleKey, actionKey, checked) {
        const page = state.pages.find(function (item) {
            return Number(item.pageId) === Number(pageId);
        });

        if (!page || !page[roleKey]) {
            return;
        }

        if (actionKey === 'view') {
            page[roleKey].view = checked;

            if (!checked) {
                page[roleKey].create = false;
                page[roleKey].update = false;
                page[roleKey].delete = false;
            }
        } else {
            page[roleKey][actionKey] = checked;

            if (checked) {
                page[roleKey].view = true;
            }
        }

        state.dirty = true;
        renderGroups();
        renderDirtyBadge();
    }

    function renderGroups() {
        const container = document.getElementById('admin-role-manager-group-container');
        if (!container) {
            return;
        }

        const grouped = groupPagesByMenu(state.pages);

        const html = grouped.map(function (group) {
            return `
                <div class="admin-role-manager-group-card">
                    <div class="admin-role-manager-group-header">
                        <h5 class="admin-role-manager-group-title">${escapeHtml(group.menuGroupName)}</h5>
                        <div class="admin-role-manager-group-count">${group.pages.length}개 페이지</div>
                    </div>
                    <div class="admin-role-manager-table-wrap">
                        <table class="table admin-role-manager-table">
                            <thead>
                                <tr>
                                    <th style="width: 320px;">페이지</th>
                                    <th>MASTER</th>
                                    <th>OPERATOR</th>
                                    <th>EMPLOY</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${group.pages.map(renderPageRow).join('')}
                            </tbody>
                        </table>
                    </div>
                </div>
            `;
        }).join('');

        container.innerHTML = html;
        container.classList.remove('d-none');
    }

    function renderPageRow(page) {
        return `
            <tr>
                <td class="admin-role-manager-page-cell">
                    <div class="admin-role-manager-page-name">${escapeHtml(page.pageName || '')}</div>
                    <div class="admin-role-manager-page-url">${escapeHtml(page.pageUrl || '')}</div>
                </td>
                <td class="admin-role-manager-role-cell">
                    ${renderRoleCell(page, 'master')}
                </td>
                <td class="admin-role-manager-role-cell">
                    ${renderRoleCell(page, 'operator')}
                </td>
                <td class="admin-role-manager-role-cell">
                    ${renderRoleCell(page, 'employ')}
                </td>
            </tr>
        `;
    }

    function renderRoleCell(page, roleKey) {
        const roleState = page[roleKey] || {
            view: false,
            create: false,
            update: false,
            delete: false
        };

        return `
            <div class="admin-role-manager-role-grid">
                ${ACTION_KEYS.map(function (actionKey) {
                    const checked = roleState[actionKey] ? 'checked' : '';
                    return `
                        <div class="admin-role-manager-switch-item">
                            <span class="admin-role-manager-switch-label">${ACTION_LABELS[actionKey]}</span>
                            <div class="form-check form-switch">
                                <input
                                    class="form-check-input admin-role-manager-switch-input"
                                    type="checkbox"
                                    data-page-id="${page.pageId}"
                                    data-role-key="${roleKey}"
                                    data-action-key="${actionKey}"
                                    ${checked}>
                            </div>
                        </div>
                    `;
                }).join('')}
            </div>
        `;
    }

    function groupPagesByMenu(pages) {
        const groups = new Map();

        pages.forEach(function (page) {
            const key = page.menuGroupName || '기타';

            if (!groups.has(key)) {
                groups.set(key, {
                    menuGroupName: key,
                    menuOrder: Number(page.menuOrder || 9999),
                    pages: []
                });
            }

            groups.get(key).pages.push(page);
        });

        return Array.from(groups.values())
            .map(function (group) {
                group.pages.sort(function (a, b) {
                    const aOrder = Number(a.pageOrder || 9999);
                    const bOrder = Number(b.pageOrder || 9999);
                    return aOrder - bOrder;
                });
                return group;
            })
            .sort(function (a, b) {
                return a.menuOrder - b.menuOrder;
            });
    }

    function toggleLoading(loading) {
        const loadingBox = document.getElementById('admin-role-manager-loading-box');
        const groupContainer = document.getElementById('admin-role-manager-group-container');

        if (!loadingBox || !groupContainer) {
            return;
        }

        if (loading) {
            loadingBox.classList.remove('d-none');
            groupContainer.classList.add('d-none');
        } else {
            loadingBox.classList.add('d-none');
            groupContainer.classList.remove('d-none');
        }
    }

    function renderDirtyBadge() {
        const dirtyBadge = document.getElementById('admin-role-manager-dirty-badge');
        if (!dirtyBadge) {
            return;
        }

        if (state.dirty) {
            dirtyBadge.classList.remove('d-none');
        } else {
            dirtyBadge.classList.add('d-none');
        }
    }

    function showMessage(message, success) {
        const box = document.getElementById('admin-role-manager-message-box');
        if (!box) {
            return;
        }

        box.classList.remove('d-none', 'admin-role-manager-success', 'admin-role-manager-error');
        box.classList.add(success ? 'admin-role-manager-success' : 'admin-role-manager-error');
        box.textContent = message || '';
    }

    function hideMessage() {
        const box = document.getElementById('admin-role-manager-message-box');
        if (!box) {
            return;
        }

        box.classList.add('d-none');
        box.classList.remove('admin-role-manager-success', 'admin-role-manager-error');
        box.textContent = '';
    }

    function clonePermissionState(stateObj) {
        return {
            view: !!stateObj?.view,
            create: !!stateObj?.create,
            update: !!stateObj?.update,
            delete: !!stateObj?.delete
        };
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }
})();