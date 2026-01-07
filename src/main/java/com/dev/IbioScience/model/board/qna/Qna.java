package com.dev.IbioScience.model.board.qna;

import com.dev.IbioScience.model.auth.embedded.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tb_qna",
       indexes = {
           @Index(name = "ix_qna_category", columnList = "category_id"),
           @Index(name = "ix_qna_created_at", columnList = "created_at"),
           @Index(name = "ix_qna_title", columnList = "title")
       })
public class Qna extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 카테고리 FK
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_qna_category"))
    private QnaCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(name = "content_html", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String contentHtml;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    // 프로젝트 멤버 테이블/엔티티 확정 전까지 안전하게 ID만 저장
    @Column(name = "writer_member_id")
    private Long writerMemberId;

    public void increaseViewCount() {
        this.viewCount++;
    }
}