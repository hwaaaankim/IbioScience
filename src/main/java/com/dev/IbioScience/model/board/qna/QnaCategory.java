package com.dev.IbioScience.model.board.qna;

import com.dev.IbioScience.model.auth.embedded.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(name = "tb_qna_category",
       uniqueConstraints = @UniqueConstraint(name = "uk_qna_category_name", columnNames = "name"),
       indexes = {
           @Index(name = "ix_qna_category_active_sort", columnList = "is_active, sort_order")
       })
public class QnaCategory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}