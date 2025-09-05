// /front/js/page/receipt.js
(function() {
	'use strict';

	var receiptEl = document.getElementById('receiptPage-receipt');
	var btnDownload = document.getElementById('receiptPage-downloadBtn');
	var btnPrint = document.getElementById('receiptPage-printBtn');

	function makeFileName() {
		var orderNoEl = receiptEl.querySelector('.receiptPage-orderNo');
		var orderNo = orderNoEl ? orderNoEl.textContent.trim().replace(/\s+/g, '') : 'receipt';
		var now = new Date();
		var yyyy = now.getFullYear();
		var mm = ('0' + (now.getMonth() + 1)).slice(-2);
		var dd = ('0' + now.getDate()).slice(-2);
		return orderNo + '_' + yyyy + mm + dd;
	}

	// 이미지 다운로드 (영수증 영역만)
	function downloadAsImage() {
		if (!window.html2canvas) {
			alert('다운로드 모듈 로딩 중입니다. 잠시 후 다시 시도해 주세요.');
			return;
		}
		html2canvas(receiptEl, {
			backgroundColor: '#ffffff',
			scale: 2,          // 고해상도 렌더
			useCORS: true,
			scrollY: -window.scrollY // 화면 스크롤 위치 영향 제거
		}).then(function(canvas) {
			var dataUrl = canvas.toDataURL('image/png');
			var a = document.createElement('a');
			a.href = dataUrl;
			a.download = makeFileName() + '.png';
			document.body.appendChild(a);
			a.click();
			a.remove();
		}).catch(function(e) { console.error(e); });
	}

	// 인쇄 (숨김 iframe 이용: 안정적)
	function printReceipt() {
		try {
			var iframe = document.createElement('iframe');
			iframe.style.position = 'fixed';
			iframe.style.right = '0';
			iframe.style.bottom = '0';
			iframe.style.width = '0';
			iframe.style.height = '0';
			iframe.style.border = '0';
			document.body.appendChild(iframe);

			var doc = iframe.contentWindow || iframe.contentDocument;
			if (doc.document) doc = doc.document;

			// 최소 스타일
			var styles = [
				'<style>',
				'html,body{margin:0;padding:16px;}',
				'.receiptPage-paper{width:500px; height:auto; border:0; box-shadow:none; font-family:inherit; color:#111;}',
				'@page { margin: 10mm; }',
				'</style>'
			].join('');

			// 영수증 DOM 복제
			var clone = receiptEl.cloneNode(true);
			// 프린트에 영향 줄 수 있는 그림자/테두리 제거
			clone.style.boxShadow = 'none';
			clone.style.border = '0';

			doc.open();
			doc.write('<!doctype html><html><head><meta charset="utf-8"><title>영수증 인쇄</title>' + styles + '</head><body></body></html>');
			doc.close();

			// body에 붙이고 로드 후 인쇄
			var body = iframe.contentDocument.body;
			body.appendChild(clone);

			setTimeout(function() {
				iframe.contentWindow.focus();
				iframe.contentWindow.print();
				setTimeout(function() {
					document.body.removeChild(iframe);
				}, 200);
			}, 150);
		} catch (e) {
			console.error(e);
			// fallback: 새 창
			var w = window.open('', 'PRINT', 'width=720,height=900,noopener,noreferrer');
			if (!w) return;
			w.document.write('<!doctype html><html><head><meta charset="utf-8"><title>영수증 인쇄</title></head><body>' + receiptEl.outerHTML + '</body></html>');
			w.document.close();
			w.focus();
			w.print();
			w.close();
		}
	}

	if (btnDownload) btnDownload.addEventListener('click', downloadAsImage);
	if (btnPrint) btnPrint.addEventListener('click', printReceipt);
})();
