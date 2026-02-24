package com.dev.IbioScience.service.admin.client;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.dev.IbioScience.dto.admin.client.ClientListRowDto;
import com.dev.IbioScience.dto.admin.client.ClientSearchCondition;
import com.dev.IbioScience.repository.auth.MemberRepository;

@Service
public class ClientSearchService {

	private final MemberRepository memberRepository;

	public ClientSearchService(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	public Page<ClientListRowDto> search(ClientSearchCondition cond, String sortKey, String sortDir) {

		int page = Math.max(cond.getPage() == null ? 0 : cond.getPage(), 0);
		int size = normalizeSize(cond.getSize() == null ? 10 : cond.getSize());

		PageRequest pageable = PageRequest.of(page, size);

		Page<ClientListRowDto> result = memberRepository.searchClients(cond, pageable, sortKey, sortDir);

		applyPageRank(result.getContent());

		return result;
	}

	private int normalizeSize(int size) {
		if (size == 30 || size == 50 || size == 100) return size;
		return 10;
	}

	private void applyPageRank(List<ClientListRowDto> rows) {
		if (rows == null || rows.isEmpty()) return;

		// totalOrderAmount 내림차순(금액이 같으면 memberId asc로 안정화)
		List<ClientListRowDto> sorted = rows.stream()
				.sorted(Comparator
						.comparingLong((ClientListRowDto r) -> safeLong(r.getTotalOrderAmount()))
						.reversed()
						.thenComparingLong(r -> r.getMemberId() == null ? Long.MAX_VALUE : r.getMemberId()))
				.toList();

		Map<Long, Integer> rankMap = new HashMap<>();
		int rank = 1;
		for (ClientListRowDto r : sorted) {
			if (r.getMemberId() != null) {
				rankMap.put(r.getMemberId(), rank++);
			}
		}

		for (ClientListRowDto r : rows) {
			r.setPageRank(rankMap.getOrDefault(r.getMemberId(), null));
		}
	}

	private long safeLong(Long v) {
		return v == null ? 0L : v;
	}
}