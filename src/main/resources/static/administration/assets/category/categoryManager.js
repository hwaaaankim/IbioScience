document.addEventListener('DOMContentLoaded', function () {
	// 캐시 변수
	let largeList = [];
	let mediumList = [];
	let smallList = [];
	let mappingListCache = []; // 모든 매핑 캐시

	// ------------------- 대분류 ------------------- //
	const $largeForm = document.getElementById('largeForm');
	const $largeName = document.getElementById('largeName');
	const $largeList = document.getElementById('largeList');
	const $largeSelect = document.getElementById('largeSelect');
	// ------------------- 중분류 ------------------- //
	const $mediumForm = document.getElementById('mediumForm');
	const $mediumName = document.getElementById('mediumName');
	const $mediumList = document.getElementById('mediumList');
	// ------------------- 소분류 ------------------- //
	const $smallForm = document.getElementById('smallForm');
	const $smallName = document.getElementById('smallName');
	const $smallList = document.getElementById('smallList');
	const $smallSelect = document.getElementById('smallSelect');
	// --------- 소분류-중분류 매핑 영역 --------- //
	const $mappingLargeSelect = document.getElementById('mappingLargeSelect');
	const $mappingMediumSelect = document.getElementById('mappingMediumSelect');
	const $mappingBtn = document.getElementById('mappingBtn');
	const $mappingList = document.getElementById('mappingList');

	// ----------- 에러메시지 안전 파싱 API 함수 -----------
	async function api(url, method = 'GET', data) {
		const options = { method, headers: {} };
		if (data) {
			options.headers['Content-Type'] = 'application/json';
			options.body = JSON.stringify(data);
		}
		const res = await fetch(url, options);
		if (!res.ok) {
			let errMsg = '서버 오류';
			try {
				// 우선 text로 전체 받아서 json여부 판별
				const text = await res.text();
				try {
					const err = JSON.parse(text);
					if (err && err.message) errMsg = err.message;
					else errMsg = text;
				} catch {
					if (text) errMsg = text;
				}
			} catch (_) { }
			throw new Error(errMsg);
		}
		return await res.json();
	}

	// =========== 데이터 일괄 초기화 ===============
	async function loadAll() {
		try {
			largeList = await api('/api/category/large');
			mediumList = await api('/api/category/medium');
			smallList = await api('/api/category/small');
			mappingListCache = await api('/api/category/mapping/all');
			renderLargeList();
			renderLargeSelect();
			renderMediumList();
			renderMappingLargeSelect();
			renderSmallList();
			renderSmallSelect();
		} catch (e) {
			alert(e.message);
		}
	}
	loadAll();

	// =============== 대분류 CRUD 및 렌더 =================
	$largeForm.onsubmit = async function (e) {
		e.preventDefault();
		const name = $largeName.value.trim();
		if (!name) return alert('대분류명을 입력하세요.');
		try {
			await api('/api/category/large', 'POST', { name });
			$largeName.value = '';
			await loadAll();
		} catch (e) {
			alert(e.message);
		}
	};

	function renderLargeList() {
		$largeList.innerHTML = '';
		largeList.forEach(l => {
			const mediumCount = mediumList.filter(m => m.largeId == l.id).length;
			const li = document.createElement('li');
			li.className = 'list-group-item d-flex justify-content-between align-items-center';
			li.innerHTML = `
                <span class="name">${l.name} - <span class="badge bg-info text-dark">${mediumCount}개 중분류</span></span>
                <div>
                    <button class="btn btn-sm btn-outline-secondary me-1 edit">수정</button>
                    <button class="btn btn-sm btn-outline-danger delete">삭제</button>
                </div>
            `;
			// 수정
			li.querySelector('.edit').onclick = function () {
				if (li.querySelector('.edit-input')) return;
				const input = document.createElement('input');
				input.type = 'text';
				input.className = 'form-control edit-input me-2';
				input.value = l.name;
				li.querySelector('.name').replaceWith(input);
				this.textContent = '저장';
				this.onclick = async function () {
					const newName = input.value.trim();
					if (!newName) return alert('값을 입력하세요.');
					try {
						await api(`/api/category/large/${l.id}`, 'PUT', { name: newName });
						await loadAll();
					} catch (e) {
						alert(e.message);
					}
				}
			};
			// 삭제
			li.querySelector('.delete').onclick = async function () {
				if (!confirm('정말 삭제하시겠습니까?')) return;
				try {
					await api(`/api/category/large/${l.id}`, 'DELETE');
					await loadAll();
				} catch (e) {
					alert(e.message);
				}
			};
			$largeList.appendChild(li);
		});
	}

	function renderLargeSelect() {
		$largeSelect.innerHTML = '<option value="">대분류 선택</option>';
		largeList.forEach(l => {
			$largeSelect.innerHTML += `<option value="${l.id}">${l.name}</option>`;
		});
	}

	// =============== 중분류 CRUD 및 렌더 ==================
	$mediumForm.onsubmit = async function (e) {
		e.preventDefault();
		const name = $mediumName.value.trim();
		const largeId = $largeSelect.value;
		if (!largeId) return alert('대분류를 선택하세요.');
		if (!name) return alert('중분류명을 입력하세요.');
		try {
			await api('/api/category/medium', 'POST', { name, largeId });
			$mediumName.value = '';
			await loadAll();
		} catch (e) {
			alert(e.message);
		}
	};

	function renderMediumList() {
		$mediumList.innerHTML = '';
		const selLargeId = $largeSelect.value;
		mediumList.filter(m => !selLargeId || m.largeId == selLargeId)
			.forEach(m => {
				const large = largeList.find(l => l.id == m.largeId);

				// [신규] 연결된 소분류 개수 카운트
				const smallCount = mappingListCache.filter(mp => mp.mediumId == m.id).length;

				const li = document.createElement('li');
				li.className = 'list-group-item d-flex justify-content-between align-items-center';
				li.innerHTML = `
                <span class="name">${large ? large.name + " > " : ""}${m.name} - <span class="badge bg-success text-light">${smallCount}개 소분류 연결</span></span>
                <div>
                    <button class="btn btn-sm btn-outline-secondary me-1 edit">수정</button>
                    <button class="btn btn-sm btn-outline-danger delete">삭제</button>
                </div>
            `;
				// 수정
				li.querySelector('.edit').onclick = function () {
					if (li.querySelector('.edit-input')) return;
					const input = document.createElement('input');
					input.type = 'text';
					input.className = 'form-control edit-input me-2';
					input.value = m.name;
					li.querySelector('.name').replaceWith(input);
					this.textContent = '저장';
					this.onclick = async function () {
						const newName = input.value.trim();
						if (!newName) return alert('값을 입력하세요.');
						try {
							await api(`/api/category/medium/${m.id}`, 'PUT', { name: newName });
							await loadAll();
						} catch (e) {
							alert(e.message);
						}
					}
				};
				// 삭제
				li.querySelector('.delete').onclick = async function () {
					if (!confirm('정말 삭제하시겠습니까?')) return;
					try {
						await api(`/api/category/medium/${m.id}`, 'DELETE');
						await loadAll();
					} catch (e) {
						alert(e.message);
					}
				};
				$mediumList.appendChild(li);
			});
	}

	$largeSelect.onchange = renderMediumList;

	// =============== 소분류 CRUD 및 렌더 ===================
	$smallForm.onsubmit = async function (e) {
		e.preventDefault();
		const name = $smallName.value.trim();
		if (!name) return alert('소분류명을 입력하세요.');
		try {
			await api('/api/category/small', 'POST', { name });
			$smallName.value = '';
			await loadAll();
		} catch (e) {
			alert(e.message);
		}
	};

	function renderSmallList() {
		$smallList.innerHTML = '';
		smallList.forEach(s => {
			// 매핑된 중분류
			const mappings = mappingListCache.filter(m => m.smallId == s.id);
			if (mappings.length === 0) {
				// 매핑 없으면 단일 표기
				const li = document.createElement('li');
				li.className = 'list-group-item d-flex justify-content-between align-items-center';
				li.innerHTML = `
                    <span class="name">${s.name}</span>
                    <div>
                        <button class="btn btn-sm btn-outline-secondary me-1 edit">수정</button>
                        <button class="btn btn-sm btn-outline-danger delete">삭제</button>
                    </div>
                `;
				li.querySelector('.edit').onclick = function () {
					if (li.querySelector('.edit-input')) return;
					const input = document.createElement('input');
					input.type = 'text';
					input.className = 'form-control edit-input me-2';
					input.value = s.name;
					li.querySelector('.name').replaceWith(input);
					this.textContent = '저장';
					this.onclick = async function () {
						const newName = input.value.trim();
						if (!newName) return alert('값을 입력하세요.');
						if (!confirm('소분류명 변경 시 모든 중분류 매핑에 적용됩니다.\n계속하시겠습니까?')) return;
						try {
							await api(`/api/category/small/${s.id}`, 'PUT', { name: newName });
							await loadAll();
						} catch (e) {
							alert(e.message);
						}
					}
				};
				li.querySelector('.delete').onclick = async function () {
					if (!confirm('정말 삭제하시겠습니까?')) return;
					try {
						await api(`/api/category/small/${s.id}`, 'DELETE');
						await loadAll();
					} catch (e) {
						alert(e.message);
					}
				};
				$smallList.appendChild(li);
			} else {
				// 매핑마다 계층정보로 표기
				mappings.forEach(m => {
					const medium = mediumList.find(md => md.id == m.mediumId);
					const large = largeList.find(lg => medium && lg.id == medium.largeId);
					const li = document.createElement('li');
					li.className = 'list-group-item d-flex justify-content-between align-items-center';
					li.innerHTML = `
                        <span class="name">${large ? large.name + " > " : ""}${medium ? medium.name + " > " : ""}${s.name}</span>
                        <div>
                            <button class="btn btn-sm btn-outline-secondary me-1 edit">수정</button>
                            <button class="btn btn-sm btn-outline-danger delete">삭제</button>
                        </div>
                    `;
					li.querySelector('.edit').onclick = function () {
						if (li.querySelector('.edit-input')) return;
						const input = document.createElement('input');
						input.type = 'text';
						input.className = 'form-control edit-input me-2';
						input.value = s.name;
						li.querySelector('.name').replaceWith(input);
						this.textContent = '저장';
						this.onclick = async function () {
							const newName = input.value.trim();
							if (!newName) return alert('값을 입력하세요.');
							if (!confirm('소분류명 변경 시 모든 중분류 매핑에 적용됩니다.\n계속하시겠습니까?')) return;
							try {
								await api(`/api/category/small/${s.id}`, 'PUT', { name: newName });
								await loadAll();
							} catch (e) {
								alert(e.message);
							}
						}
					};
					li.querySelector('.delete').onclick = async function () {
						if (!confirm('정말 삭제하시겠습니까?')) return;
						try {
							await api(`/api/category/small/${s.id}`, 'DELETE');
							await loadAll();
						} catch (e) {
							alert(e.message);
						}
					};
					$smallList.appendChild(li);
				});
			}
		});
	}

	function renderSmallSelect() {
		$smallSelect.innerHTML = '<option value="">소분류 선택</option>';
		smallList.forEach(s => {
			$smallSelect.innerHTML += `<option value="${s.id}">${s.name}</option>`;
		});
	}

	// ============ 소분류-중분류 매핑 ===============
	function renderMappingLargeSelect() {
		$mappingLargeSelect.innerHTML = '<option value="">대분류 선택</option>';
		largeList.forEach(l => {
			$mappingLargeSelect.innerHTML += `<option value="${l.id}">${l.name}</option>`;
		});
	}

	$mappingLargeSelect.onchange = function () {
		const largeId = $mappingLargeSelect.value;
		$mappingMediumSelect.innerHTML = '';
		mediumList.filter(m => m.largeId == largeId)
			.forEach(m => {
				$mappingMediumSelect.innerHTML += `<option value="${m.id}">${m.name}</option>`;
			});
	};

	$smallSelect.onchange = renderMappingList;

	async function renderMappingList() {
		$mappingList.innerHTML = '';
		const smallId = $smallSelect.value;
		if (!smallId) return;
		try {
			const mappings = await api(`/api/category/mapping/small/${smallId}`);
			mappings.forEach(m => {
				const medium = mediumList.find(x => x.id == m.mediumId);
				const large = largeList.find(lg => medium && lg.id == medium.largeId);
				const small = smallList.find(s => s.id == m.smallId);
				const li = document.createElement('li');
				li.className = 'list-group-item d-flex justify-content-between align-items-center';
				li.innerHTML = `
                    <span>${large ? large.name + " > " : ""}${medium ? medium.name + " > " : ""}${small ? small.name : ""}</span>
                    <button class="btn btn-sm btn-outline-danger delete">매핑해제</button>
                `;
				li.querySelector('.delete').onclick = async function () {
					if (!confirm('정말 매핑을 해제하시겠습니까?')) return;
					try {
						await api(`/api/category/mapping/${m.id}`, 'DELETE');
						await renderMappingList();
					} catch (e) {
						alert(e.message);
					}
				}
				$mappingList.appendChild(li);
			});
		} catch (e) {
			alert(e.message);
		}
	}

	$mappingBtn.onclick = async function () {
		const smallId = $smallSelect.value;
		const mediumIds = Array.from($mappingMediumSelect.selectedOptions).map(opt => opt.value);
		if (!smallId) return alert('소분류를 선택하세요.');
		if (!mediumIds.length) return alert('중분류를 선택하세요.');
		try {
			await api('/api/category/mapping', 'POST', {
				smallId, mediumIds
			});
			await renderMappingList();
			alert('매핑이 완료되었습니다.');
		} catch (e) {
			alert(e.message);
		}
	};
});
