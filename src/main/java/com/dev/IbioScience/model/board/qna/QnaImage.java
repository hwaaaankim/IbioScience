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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "tb_qna_image",
       uniqueConstraints = @UniqueConstraint(name = "uk_qna_image_url", columnNames = "image_url"),
       indexes = @Index(name = "ix_qna_image_qna", columnList = "qna_id"))
public class QnaImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // QNA FK
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "qna_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_qna_image_qna"))
    private Qna qna;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;     // /upload/qna/main/...

    @Column(name = "stored_path", nullable = false, length = 600)
    private String storedPath;   // 실제 파일 위치
}