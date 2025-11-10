/* eslint-disable */
(function() {
	const API = {
		brands: '/api/menu/brands',
		large: '/api/menu/categories/large',
		medium: (largeId) => `/api/menu/categories/medium?largeId=${encodeURIComponent(largeId)}`,
		small: (mediumId) => `/api/menu/categories/small?mediumId=${encodeURIComponent(mediumId)}`,
		products: (params) => {
			const q = [];
			if (params.largeId) q.push(`largeId=${encodeURIComponent(params.largeId)}`);
			if (params.mediumId) q.push(`mediumId=${encodeURIComponent(params.mediumId)}`);
			if (params.smallId) q.push(`smallId=${encodeURIComponent(params.smallId)}`);
			if (params.brandId) q.push(`brandId=${encodeURIComponent(params.brandId)}`);
			return `/api/menu/products${q.length ? `?${q.join('&')}` : ''}`;
		}
	};

	async function jget(url) {
		const r = await fetch(url, { credentials: 'same-origin' });
		if (!r.ok) throw new Error(`HTTP ${r.status}`);
		return r.json();
	}

	const ibioMenu = {
		async bootstrap() {
			const large = Array.isArray(window.__IBIO_LARGE_CATEGORIES__) ? window.__IBIO_LARGE_CATEGORIES__ : [];
			const brands = await jget(API.brands);

			return {
				brands: brands.map(b => ({ id: b.id, name: b.name, imageUrl: b.imageUrl || null })),
				large: large.map(l => ({ id: l.id, name: l.name, medium: [] }))
			};
		},

		async fetchMedium(largeId) {
			const list = await jget(API.medium(largeId));
			return list.map(m => ({ id: m.id, name: m.name, small: [] }));
		},

		async fetchSmall(mediumId) {
			const list = await jget(API.small(mediumId));
			return list.map(s => ({ id: s.id, name: s.name }));
		},

		/** 교집합 제품 조회 */
		async fetchProductsByScope({ largeId = null, mediumId = null, smallId = null, brandId = null }) {
			const list = await jget(API.products({ largeId, mediumId, smallId, brandId }));
			return list.map(p => ({ id: p.id, name: p.name, brandId: p.brandId || null }));
		}
	};

	window.ibioMenu = ibioMenu;
})();