package com.dev.IbioScience.model.estimate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.dev.IbioScience.enums.estimate.EstimateAnswerStatus;
import com.dev.IbioScience.enums.estimate.EstimateCheckStatus;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.embedded.BaseTimeEntity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tb_estimate")
@AttributeOverrides({
    @AttributeOverride(name = "createdAt", column = @Column(name = "created_at", updatable = false)),
    @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class Estimate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 문의 회원 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 제목 */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /** 상세사항 */
    @Column(name = "detail_content", columnDefinition = "TEXT")
    private String detailContent;

    /** 요청일시 */
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    /** 확인일시 */
    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    /** 답변일시 */
    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    /** 확인여부 */
    @Enumerated(EnumType.STRING)
    @Column(name = "check_status", nullable = false, length = 30)
    private EstimateCheckStatus checkStatus = EstimateCheckStatus.UNCHECKED;

    /** 답변여부 */
    @Enumerated(EnumType.STRING)
    @Column(name = "answer_status", nullable = false, length = 30)
    private EstimateAnswerStatus answerStatus = EstimateAnswerStatus.WAITING;

    /** 견적 상품들 */
    @OneToMany(mappedBy = "estimate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EstimateItem> items = new ArrayList<>();

    /** 첨부파일들 */
    @OneToMany(mappedBy = "estimate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EstimateAttachment> attachments = new ArrayList<>();

    public void addItem(EstimateItem item) {
        item.setEstimate(this);
        this.items.add(item);
    }

    public void addAttachment(EstimateAttachment attachment) {
        attachment.setEstimate(this);
        this.attachments.add(attachment);
    }
}