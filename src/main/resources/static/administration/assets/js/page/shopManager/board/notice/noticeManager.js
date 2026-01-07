/* global window, document, fetch, confirm, alert */
(function () {
  'use strict';

  function qs(sel, root) { return (root || document).querySelector(sel); }
  function qsa(sel, root) { return Array.from((root || document).querySelectorAll(sel)); }

  async function delNotice(id) {
    const res = await fetch('/api/manager/notice/' + id, { method: 'DELETE' });
    if (!res.ok) {
      const t = await res.text();
      throw new Error('삭제 실패: ' + t);
    }
  }

  document.addEventListener('DOMContentLoaded', function () {
    qsa('.notice-manager-delete-btn').forEach(btn => {
      btn.addEventListener('click', async function () {
        const id = this.getAttribute('data-id');
        if (!id) return;

        if (!confirm('해당 공지사항을 삭제하시겠습니까?\n(에디터 이미지 파일도 함께 삭제됩니다)')) return;

        try {
          await delNotice(id);
          alert('삭제되었습니다.');
          window.location.reload();
        } catch (e) {
          alert(e.message || '삭제 중 오류가 발생했습니다.');
        }
      });
    });
  });
})();
