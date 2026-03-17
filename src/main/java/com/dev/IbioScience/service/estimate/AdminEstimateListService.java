package com.dev.IbioScience.service.estimate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.estimate.admin.AdminEstimateDetailResponse;
import com.dev.IbioScience.dto.estimate.admin.AdminEstimateListRowDto;
import com.dev.IbioScience.dto.estimate.admin.AdminEstimateListSearchRequest;
import com.dev.IbioScience.dto.estimate.admin.AdminPageResponse;
import com.dev.IbioScience.dto.estimate.admin.EstimateMailSendRequest;
import com.dev.IbioScience.enums.estimate.EstimateAnswerStatus;
import com.dev.IbioScience.enums.estimate.EstimateCheckStatus;
import com.dev.IbioScience.model.estimate.Estimate;
import com.dev.IbioScience.model.estimate.EstimateAttachment;
import com.dev.IbioScience.model.estimate.EstimateItem;
import com.dev.IbioScience.repository.estimate.EstimateRepository;
import com.dev.IbioScience.service.hiworks.HiworksEstimateMailService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminEstimateListService {

    private final EstimateRepository estimateRepository;
    private final HiworksEstimateMailService hiworksEstimateMailService;

    public AdminPageResponse<AdminEstimateListRowDto> getEstimateList(Long memberId, AdminEstimateListSearchRequest request) {
        Page<AdminEstimateListRowDto> page = estimateRepository.searchAdminEstimateList(memberId, request);

        return AdminPageResponse.of(
                page.getContent(),
                request.getPageValue(),
                request.getSizeValue(),
                page.getTotalElements()
        );
    }

    @Transactional
    public AdminEstimateDetailResponse getEstimateDetail(Long memberId, Long estimateId) {
        Estimate estimate = estimateRepository.findByIdAndMemberId(estimateId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("견적서를 찾을 수 없습니다."));

        if (EstimateCheckStatus.UNCHECKED.equals(estimate.getCheckStatus())) {
            estimate.setCheckStatus(EstimateCheckStatus.CHECKED);
            if (estimate.getCheckedAt() == null) {
                estimate.setCheckedAt(LocalDateTime.now());
            }
        }

        return toDetailResponse(estimate);
    }

    @Transactional
    public void sendEstimateMail(
            Long memberId,
            Long estimateId,
            EstimateMailSendRequest request,
            List<MultipartFile> attachments
    ) {
        Estimate estimate = estimateRepository.findByIdAndMemberId(estimateId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("견적서를 찾을 수 없습니다."));

        String toEmail = estimate.getMember() != null ? estimate.getMember().getEmail() : null;
        if (!StringUtils.hasText(toEmail)) {
            throw new IllegalArgumentException("신청자 이메일이 없어 메일을 발송할 수 없습니다.");
        }

        String subject = request.getNormalizedSubject();
        String bodyHtml = request.getNormalizedBodyHtml();

        if (!StringUtils.hasText(subject)) {
            throw new IllegalArgumentException("이메일 제목을 입력해주세요.");
        }

        if (!StringUtils.hasText(stripHtml(bodyHtml))) {
            throw new IllegalArgumentException("이메일 내용을 입력해주세요.");
        }

        hiworksEstimateMailService.sendHtmlMail(toEmail, subject, bodyHtml, attachments);

        if (EstimateCheckStatus.UNCHECKED.equals(estimate.getCheckStatus())) {
            estimate.setCheckStatus(EstimateCheckStatus.CHECKED);
        }
        if (estimate.getCheckedAt() == null) {
            estimate.setCheckedAt(LocalDateTime.now());
        }

        estimate.setAnswerStatus(EstimateAnswerStatus.ANSWERED);
        estimate.setAnsweredAt(LocalDateTime.now());
    }

    private AdminEstimateDetailResponse toDetailResponse(Estimate estimate) {
        AdminEstimateDetailResponse response = new AdminEstimateDetailResponse();
        response.setEstimateId(estimate.getId());
        response.setMemberId(estimate.getMember().getId());
        response.setMemberUserId(estimate.getMember().getUsername());
        response.setMemberEmail(estimate.getMember().getEmail());
        response.setMemberContactPhone(estimate.getMember().getMobile());
        response.setMemberName(estimate.getMember().getName());

        response.setTitle(estimate.getTitle());
        response.setDetailContent(estimate.getDetailContent());
        response.setRequestedAt(estimate.getRequestedAt());
        response.setCheckedAt(estimate.getCheckedAt());
        response.setAnsweredAt(estimate.getAnsweredAt());
        response.setCheckStatus(estimate.getCheckStatus());
        response.setAnswerStatus(estimate.getAnswerStatus());

        List<AdminEstimateDetailResponse.ItemDto> itemDtos = new ArrayList<>();
        for (EstimateItem item : estimate.getItems()) {
            AdminEstimateDetailResponse.ItemDto itemDto = new AdminEstimateDetailResponse.ItemDto();
            itemDto.setItemId(item.getId());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setLargeCategoryName(item.getLargeCategoryName());
            itemDto.setMediumCategoryName(item.getMediumCategoryName());
            itemDto.setSmallCategoryName(item.getSmallCategoryName());
            itemDto.setBrandName(item.getBrandName());
            itemDto.setProductName(item.getProductName());
            itemDto.setProductCode(item.getProductCode());
            itemDtos.add(itemDto);
        }
        response.setItems(itemDtos);

        List<AdminEstimateDetailResponse.AttachmentDto> attachmentDtos = new ArrayList<>();
        for (EstimateAttachment attachment : estimate.getAttachments()) {
            AdminEstimateDetailResponse.AttachmentDto attachmentDto = new AdminEstimateDetailResponse.AttachmentDto();
            attachmentDto.setAttachmentId(attachment.getId());
            attachmentDto.setOriginalFileName(attachment.getOriginalFileName());
            attachmentDto.setFileUrl(attachment.getFileUrl());
            attachmentDto.setContentType(attachment.getContentType());
            attachmentDto.setFileSize(attachment.getFileSize());
            attachmentDtos.add(attachmentDto);
        }
        response.setAttachments(attachmentDtos);

        return response;
    }

    private String stripHtml(String html) {
        if (!StringUtils.hasText(html)) {
            return "";
        }
        return html.replaceAll("<[^>]*>", "").replace("&nbsp;", " ").trim();
    }
}