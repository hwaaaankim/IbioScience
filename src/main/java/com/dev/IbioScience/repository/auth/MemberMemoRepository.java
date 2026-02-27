package com.dev.IbioScience.repository.auth;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.model.auth.MemberMemo;

public interface MemberMemoRepository extends JpaRepository<MemberMemo, Long> {

    List<MemberMemo> findTop5ByTargetMember_IdOrderByCreatedAtDesc(Long memberId);

    @Query("""
        select m
        from MemberMemo m
        where m.targetMember.id = :memberId
          and (:from is null or m.createdAt >= :from)
          and (:toExclusive is null or m.createdAt < :toExclusive)
        """)
    Page<MemberMemo> searchMemos(
        @Param("memberId") Long memberId,
        @Param("from") LocalDateTime from,
        @Param("toExclusive") LocalDateTime toExclusive,
        Pageable pageable
    );

    void deleteByIdAndTargetMember_Id(Long memoId, Long memberId);
}