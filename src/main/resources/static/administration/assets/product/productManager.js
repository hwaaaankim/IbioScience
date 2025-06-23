document.addEventListener("DOMContentLoaded", function() {
	// === 1. 카테고리 선택 트리 ===
	const largeList = document.getElementById('category-large-list');
	const mediumList = document.getElementById('category-medium-list');
	const smallList = document.getElementById('category-small-list');
	const selectedList = document.getElementById('selected-category-list');
	const applyCategoryBtn = document.getElementById('apply-category-btn');
	let selectedCategories = [];

	fetch('/api/category/list-large')
		.then(res => res.json())
		.then(list => {
			largeList.innerHTML = '';
			list.forEach(large => {
				let li = document.createElement('li');
				li.textContent = large.name;
				li.className = 'list-group-item list-group-item-action category-large-item';
				li.dataset.id = large.id;
				largeList.appendChild(li);
			});
		});

	largeList.addEventListener('click', function(e) {
		if (e.target.classList.contains('category-large-item')) {
			const largeId = e.target.dataset.id;
			fetch(`/api/category/list-medium?largeId=${largeId}`)
				.then(res => res.json())
				.then(list => {
					mediumList.innerHTML = '';
					list.forEach(m => {
						let li = document.createElement('li');
						li.textContent = m.name;
						li.className = 'list-group-item list-group-item-action category-medium-item';
						li.dataset.id = m.id;
						mediumList.appendChild(li);
					});
					smallList.innerHTML = '';
				});
		}
	});

	mediumList.addEventListener('click', function(e) {
		if (e.target.classList.contains('category-medium-item')) {
			const mediumId = e.target.dataset.id;
			fetch(`/api/category/list-small?mediumId=${mediumId}`)
				.then(res => res.json())
				.then(list => {
					smallList.innerHTML = '';
					list.forEach(s => {
						let li = document.createElement('li');
						li.textContent = s.name;
						li.className = 'list-group-item list-group-item-action category-small-item';
						li.dataset.id = s.id;
						smallList.appendChild(li);
					});
				});
		}
	});

	smallList.addEventListener('click', function(e) {
		if (e.target.classList.contains('category-small-item')) {
			const smallId = e.target.dataset.id;
			const text = e.target.textContent;
			if (!selectedCategories.some(sc => sc.id == smallId)) {
				selectedCategories.push({ id: smallId, text: text });
				renderSelectedCategories();
			}
		}
	});

	function renderSelectedCategories() {
		selectedList.innerHTML = '';
		selectedCategories.forEach((c, idx) => {
			let div = document.createElement('div');
			div.className = 'badge bg-primary text-white px-2 py-2 me-2 d-flex align-items-center';
			div.textContent = c.text;
			let x = document.createElement('span');
			x.textContent = '×';
			x.style.cursor = 'pointer';
			x.style.marginLeft = '10px';
			x.onclick = () => { selectedCategories.splice(idx, 1); renderSelectedCategories(); }
			div.appendChild(x);
			selectedList.appendChild(div);
		});
	}

	applyCategoryBtn.addEventListener('click', function() {
		alert('분류 적용: ' + selectedCategories.map(c => c.text).join(', '));
	});

	// ===== 2. 공통 표시옵션(질문) 동적 랜더링 및 CKEditor mount =====
	let ckeInstances = {};

	function makeQuestionInput(option) {
		const requiredAttr = option.required ? 'required' : '';
		const editorId = option.type === 'CKEDITOR' ? `editor-question-${option.id}` : '';
		let inputHtml = '';
		switch (option.type) {
			case 'INPUT':
				inputHtml = `<input type="text" class="form-control form-control-sm" name="question_${option.id}" placeholder="${option.placeholder || ''}" ${requiredAttr}>`;
				break;
			case 'TEXTAREA':
				inputHtml = `<textarea class="form-control form-control-sm" name="question_${option.id}" rows="2" placeholder="${option.placeholder || ''}" ${requiredAttr}></textarea>`;
				break;
			case 'SELECT':
				inputHtml = `<select class="form-select form-select-sm" name="question_${option.id}" ${requiredAttr}>`
					+ (Array.isArray(option.options) && option.options.length > 0
						? option.options.map(opt => {
							if (typeof opt === 'object' && opt !== null) {
								if (opt.value !== undefined && opt.label !== undefined) {
									return `<option value="${opt.value}">${opt.label}</option>`;
								} else if (Object.keys(opt).length === 1) {
									let key = Object.keys(opt)[0];
									return `<option value="${key}">${opt[key]}</option>`;
								} else {
									return `<option disabled>선택지 오류</option>`;
								}
							} else {
								return `<option value="${opt}">${opt}</option>`;
							}
						}).join('')
						: '<option disabled>선택지 없음</option>')
					+ `</select>`;
				break;
			case 'FILE':
				inputHtml = `<input type="file" class="form-control form-control-sm" name="question_${option.id}" ${requiredAttr}>`;
				break;
			case 'CKEDITOR':
				inputHtml = `<textarea class="form-control" name="question_${option.id}" id="${editorId}" rows="3" ${requiredAttr}></textarea>`;
				break;
			default:
				inputHtml = `<input type="text" class="form-control form-control-sm" name="question_${option.id}" placeholder="지원되지 않는 타입" disabled>`;
		}
		return inputHtml;
	}

	fetch('/api/display-questions/list-common')
		.then(res => res.json())
		.then(list => {
			const container = document.getElementById('product-manager-display-options');
			container.innerHTML = '';
			list.forEach(option => {
				const div = document.createElement('div');
				div.className = 'row mb-2 align-items-end';
				div.innerHTML = `
                    <div class="col-md-3">
                        <label class="form-label mb-1">${option.label ?? option.name}${option.required ? ' <span class="text-danger">*</span>' : ''}</label>
                        ${makeQuestionInput(option)}
                    </div>
                `;
				container.appendChild(div);
			});
			setTimeout(() => {
				list.filter(opt => opt.type === 'CKEDITOR').forEach(option => {
					const tId = `editor-question-${option.id}`;
					const textarea = document.getElementById(tId);
					if (textarea && !ckeInstances[tId] && window.ClassicEditor) {
						window.ClassicEditor.create(textarea)
							.then(editor => { ckeInstances[tId] = editor; })
							.catch(err => console.error('CKEditor5 생성 오류:', err));
					}
				});
			}, 100);
		});

	// === 3. 상세설명(이미지/HTML) 에디터 mount (id: editor-desc) ===
	let detailEditor = null;
	(function() {
		const desc = document.getElementById('editor-desc');
		if (desc && window.ClassicEditor) {
			window.ClassicEditor.create(desc)
				.then(editor => { detailEditor = editor; })
				.catch(err => console.error('CKEditor5 생성 오류:', err));
		}
	})();

	// ===== 4. 대표/추가 이미지 미리보기/삭제/순서 =====
	const mainInput = document.getElementById('product-manager-main-image');
	const mainPreview = document.getElementById('product-manager-main-image-preview');
	mainInput.addEventListener('change', function() {
		mainPreview.innerHTML = '';
		if (this.files.length > 0) {
			let file = this.files[0];
			let reader = new FileReader();
			reader.onload = e => {
				let div = document.createElement('div');
				div.className = 'image-preview-thumb position-relative';
				div.innerHTML = `
                    <img src="${e.target.result}" style="width:100%;height:100%;object-fit:cover;">
                    <button class="btn-close btn-sm" style="position:absolute;top:0;right:0;z-index:2;" aria-label="Remove"></button>`;
				div.querySelector('button').onclick = () => {
					mainInput.value = '';
					mainPreview.innerHTML = '';
				}
				mainPreview.appendChild(div);
			}
			reader.readAsDataURL(file);
		}
	});

	// 추가 이미지 (여러장, 미리보기, 삭제, Sortable)
	const subInput = document.getElementById('product-manager-sub-image');
	const subPreview = document.getElementById('product-manager-sub-image-preview');
	let subFiles = [];
	subInput.addEventListener('change', function() {
		subFiles = Array.from(this.files);
		renderSubImagePreview();
	});
	function renderSubImagePreview() {
		subPreview.innerHTML = '';
		subFiles.forEach((file, idx) => {
			let reader = new FileReader();
			reader.onload = e => {
				let div = document.createElement('div');
				div.className = 'image-preview-thumb position-relative';
				div.setAttribute('draggable', 'true');
				div.style.width = '100px'; div.style.height = '100px'; div.style.marginRight = '8px';
				div.innerHTML = `
                    <img src="${e.target.result}" style="width:100%;height:100%;object-fit:cover;">
                    <button class="btn-close btn-sm" style="position:absolute;top:0;right:0;z-index:2;" aria-label="Remove"></button>`;
				div.querySelector('button').onclick = () => {
					subFiles.splice(idx, 1);
					renderSubImagePreview();
				}
				subPreview.appendChild(div);
			}
			reader.readAsDataURL(file);
		});
	}
	new Sortable(subPreview, {
		animation: 150,
		onEnd: function(evt) {
			const oldIndex = evt.oldIndex, newIndex = evt.newIndex;
			if (oldIndex !== newIndex) {
				const moved = subFiles.splice(oldIndex, 1)[0];
				subFiles.splice(newIndex, 0, moved);
				renderSubImagePreview();
			}
		}
	});

	// ===== 5. 추가 입력필드 동적 추가/삭제 =====
	const extraFieldList = document.getElementById('product-manager-extra-field-list');
	const addExtraFieldBtn = document.getElementById('product-manager-add-extra-field');
	let extraFields = [];
	function renderExtraFields() {
		extraFieldList.innerHTML = '';
		extraFields.forEach((field, idx) => {
			let row = document.createElement('div');
			row.className = 'input-group mb-2';
			row.innerHTML = `
                <input type="text" class="form-control form-control-sm" name="extraFields[${idx}].label" placeholder="질문명" value="${field.label || ''}" required>
                <input type="text" class="form-control form-control-sm" name="extraFields[${idx}].value" placeholder="답변값" value="${field.value || ''}" required>
                <button type="button" class="btn btn-outline-danger btn-sm" title="삭제">×</button>
            `;
			row.querySelector('button').onclick = () => { extraFields.splice(idx, 1); renderExtraFields(); }
			extraFieldList.appendChild(row);
		});
	}
	addExtraFieldBtn.addEventListener('click', function() {
		extraFields.push({ label: '', value: '' });
		renderExtraFields();
	});
	// 최초 1개 표시
	renderExtraFields();

	// ===== 6. 옵션그룹/옵션 동적 추가/삭제 =====
	const optionGroupList = document.getElementById('product-manager-option-group-list');
	const addOptionGroupBtn = document.getElementById('product-manager-add-option-group');
	let optionGroups = [];
	function renderOptionGroups() {
		optionGroupList.innerHTML = '';
		optionGroups.forEach((group, groupIdx) => {
			let groupDiv = document.createElement('div');
			groupDiv.className = 'card mb-2';
			groupDiv.innerHTML = `
                <div class="card-body p-2">
                    <div class="input-group mb-2">
                        <input type="text" class="form-control form-control-sm" name="optionGroups[${groupIdx}].name" placeholder="옵션 그룹명" value="${group.name || ''}" required>
                        <button type="button" class="btn btn-outline-danger btn-sm" title="옵션그룹 삭제">×</button>
                    </div>
                    <div id="option-group-options-${groupIdx}"></div>
                    <button type="button" class="btn btn-outline-primary btn-sm mt-1" data-group-idx="${groupIdx}">+ 옵션 추가</button>
                </div>
            `;
			// 옵션그룹 삭제
			groupDiv.querySelector('.btn-outline-danger').onclick = () => { optionGroups.splice(groupIdx, 1); renderOptionGroups(); }
			// 옵션 추가
			groupDiv.querySelector('.btn-outline-primary').onclick = (e) => {
				group.options.push({
					name: '', value: '', extraPrice: '', sign: 'PLUS', sortOrder: group.options.length + 1
				});
				renderOptionGroups();
			}
			// 옵션 리스트
			const optionsContainer = groupDiv.querySelector(`#option-group-options-${groupIdx}`);
			group.options.forEach((opt, optIdx) => {
				let optRow = document.createElement('div');
				optRow.className = 'input-group mb-1';
				optRow.innerHTML = `
                    <input type="text" class="form-control form-control-sm" name="optionGroups[${groupIdx}].options[${optIdx}].name" placeholder="옵션명" value="${opt.name || ''}" required>
                    <input type="text" class="form-control form-control-sm" name="optionGroups[${groupIdx}].options[${optIdx}].value" placeholder="값" value="${opt.value || ''}">
                    <input type="number" class="form-control form-control-sm" name="optionGroups[${groupIdx}].options[${optIdx}].extraPrice" placeholder="추가금액" value="${opt.extraPrice || ''}">
                    <select class="form-select form-select-sm" name="optionGroups[${groupIdx}].options[${optIdx}].sign">
                        <option value="PLUS" ${opt.sign === 'PLUS' ? 'selected' : ''}>+</option>
                        <option value="MINUS" ${opt.sign === 'MINUS' ? 'selected' : ''}>-</option>
                    </select>
                    <input type="number" class="form-control form-control-sm" name="optionGroups[${groupIdx}].options[${optIdx}].sortOrder" placeholder="정렬" value="${opt.sortOrder || optIdx + 1}">
                    <button type="button" class="btn btn-outline-danger btn-sm" title="옵션 삭제">×</button>
                `;
				// 옵션 삭제
				optRow.querySelector('.btn-outline-danger').onclick = () => {
					group.options.splice(optIdx, 1);
					renderOptionGroups();
				};
				optionsContainer.appendChild(optRow);
			});
			optionGroupList.appendChild(groupDiv);
		});
	}
	addOptionGroupBtn.addEventListener('click', function() {
		optionGroups.push({
			name: '',
			options: []
		});
		renderOptionGroups();
	});
	// 최초 1개 표시
	renderOptionGroups();

	// ===== 7. 제품 등록 버튼 - FormData로 수집 및 제출 =====
	document.getElementById('submitProductBtn').addEventListener('click', function(e) {
		e.preventDefault();
		const formData = new FormData();

		// 1) 분류(소분류)  
		selectedCategories.forEach(cat => formData.append('categorySmallIds', cat.id));

		// 2) 공통표시항목
		document.querySelectorAll('#product-manager-display-options [name]').forEach(el => {
			if (el.type === 'file') {
				if (el.files[0]) formData.append(el.name, el.files[0]);
			} else if (el.type === 'textarea' && el.classList.contains('ck-editor__editable')) {
				// CKEditor는 별도 처리
			} else {
				formData.append(el.name, el.value);
			}
		});
		Object.entries(ckeInstances).forEach(([tid, editor]) => {
			formData.append(tid, editor.getData());
		});

		// 3) 대표이미지
		if (mainInput.files[0]) {
			formData.append('mainImage', mainInput.files[0]);
		}

		// 4) 추가이미지
		subFiles.forEach((f, idx) => formData.append('subImages', f));

		// 5) 제품 기본정보
		formData.append('productName', document.getElementById('productName').value);
		formData.append('productCode', document.getElementById('productCode').value);
		formData.append('displayStatus', document.querySelector('input[name="displayStatus"]:checked').value);
		formData.append('saleStatus', document.querySelector('input[name="saleStatus"]:checked').value);

		// 6) 상세설명 에디터
		if (detailEditor) {
			formData.append('detailHtml', detailEditor.getData());
		}

		// 7) 추가입력필드
		extraFields = [];
		extraFieldList.querySelectorAll('.input-group').forEach((row, idx) => {
			const label = row.querySelector(`[name="extraFields[${idx}].label"]`).value;
			const value = row.querySelector(`[name="extraFields[${idx}].value"]`).value;
			extraFields.push({ label, value });
			formData.append(`extraFields[${idx}].label`, label);
			formData.append(`extraFields[${idx}].value`, value);
		});

		// 8) 옵션그룹/옵션
		optionGroups = [];
		optionGroupList.querySelectorAll('.card').forEach((groupDiv, groupIdx) => {
			const groupName = groupDiv.querySelector(`[name="optionGroups[${groupIdx}].name"]`).value;
			const options = [];
			const optionRows = groupDiv.querySelectorAll('.input-group.mb-1');
			optionRows.forEach((row, optIdx) => {
				options.push({
					name: row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].name"]`).value,
					value: row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].value"]`).value,
					extraPrice: row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].extraPrice"]`).value,
					sign: row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].sign"]`).value,
					sortOrder: row.querySelector(`[name="optionGroups[${groupIdx}].options[${optIdx}].sortOrder"]`).value
				});
			});
			optionGroups.push({ name: groupName, options });
		});

		// ===== 여기서부터 콘솔로 모든 데이터 상세 출력 =====

		// 1) 분류(소분류)
		console.log('[카테고리-소분류 선택]');
		if (selectedCategories.length === 0) {
			console.log('- 선택된 소분류 없음');
		} else {
			selectedCategories.forEach((c, i) => {
				console.log(`- ${i + 1}: id=${c.id}, name=${c.text}`);
			});
		}

		// 2) 공통표시항목 (질문)
		console.log('[공통표시항목(질문)]');
		document.querySelectorAll('#product-manager-display-options [name]').forEach(el => {
			if (el.type === 'file') {
				if (el.files.length > 0) {
					Array.from(el.files).forEach((file, idx) => {
						console.log(`- ${el.name} (파일): ${file.name}, size=${file.size}`);
					});
				} else {
					console.log(`- ${el.name} (파일): 없음`);
				}
			} else if (el.type === 'textarea' && el.classList.contains('ck-editor__editable')) {
				// CKEditor는 별도 처리
			} else {
				console.log(`- ${el.name}: ${el.value}`);
			}
		});
		Object.entries(ckeInstances).forEach(([tid, editor]) => {
			console.log(`- CKEditor ${tid}:`, editor.getData());
		});

		// 3) 대표이미지
		console.log('[대표이미지]');
		if (mainInput.files.length > 0) {
			const file = mainInput.files[0];
			console.log(`- 파일명: ${file.name}, 크기: ${file.size}byte`);
		} else {
			console.log('- 대표이미지 없음');
		}

		// 4) 추가이미지
		console.log('[추가이미지]');
		if (subFiles.length === 0) {
			console.log('- 추가이미지 없음');
		} else {
			subFiles.forEach((file, idx) => {
				console.log(`- [${idx}] 파일명: ${file.name}, 크기: ${file.size}byte`);
			});
		}

		// 5) 제품 기본정보
		console.log('[제품 기본정보]');
		console.log('- 상품명:', document.getElementById('productName').value);
		console.log('- 상품코드:', document.getElementById('productCode').value);
		console.log('- 전시여부:', document.querySelector('input[name="displayStatus"]:checked').value);
		console.log('- 판매여부:', document.querySelector('input[name="saleStatus"]:checked').value);

		// 6) 상세설명 에디터
		if (detailEditor) {
			console.log('[상세설명(HTML)]', detailEditor.getData());
		}

		// 7) 추가입력필드
		console.log('[추가입력필드]');
		if (extraFields.length === 0) {
			console.log('- 추가입력필드 없음');
		} else {
			extraFields.forEach((f, idx) => {
				console.log(`- [${idx}] 질문명: ${f.label} / 답변값: ${f.value}`);
			});
		}

		// 8) 옵션그룹/옵션
		console.log('[옵션그룹/옵션]');
		if (optionGroups.length === 0) {
			console.log('- 옵션그룹 없음');
		} else {
			optionGroups.forEach((g, idx) => {
				console.log(`- 옵션그룹[${idx}] 그룹명: ${g.name}`);
				if (!g.options || g.options.length === 0) {
					console.log(`  - 옵션 없음`);
				} else {
					g.options.forEach((opt, i) => {
						console.log(`  - [옵션${i}] 옵션명: ${opt.name} / 값: ${opt.value} / 추가금액: ${opt.extraPrice} / 부호: ${opt.sign} / 정렬: ${opt.sortOrder}`);
					});
				}
			});
		}

		// 실제 전송은 아래처럼 주석 처리해둡니다
		/*
		fetch('/api/product', {
			method: 'POST',
			body: formData
		})
			.then(res => {
				if (res.ok) return res.json();
				throw new Error('등록실패');
			})
			.then(json => {
				alert('제품 등록 성공');
			})
			.catch(err => alert('등록 실패: ' + err.message));
		*/
	});

});
