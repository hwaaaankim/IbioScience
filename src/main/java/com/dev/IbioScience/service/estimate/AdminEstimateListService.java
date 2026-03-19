package com.dev.IbioScience.service.estimate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.HtmlUtils;

import com.dev.IbioScience.dto.estimate.admin.AdminEstimateDetailResponse;
import com.dev.IbioScience.dto.estimate.admin.AdminEstimateListRowDto;
import com.dev.IbioScience.dto.estimate.admin.AdminEstimateListSearchRequest;
import com.dev.IbioScience.dto.estimate.admin.AdminPageResponse;
import com.dev.IbioScience.dto.estimate.admin.EstimateMailSendRequest;
import com.dev.IbioScience.enums.estimate.EstimateAnswerStatus;
import com.dev.IbioScience.enums.estimate.EstimateCheckStatus;
import com.dev.IbioScience.model.auth.CompanyProfile;
import com.dev.IbioScience.model.auth.Member;
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

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("(?is)<[^>]+>");
    private static final Pattern IMG_TAG_PATTERN = Pattern.compile("(?is)<img\\b[^>]*>");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 내부 수신용 고정 메일 주소 */
    private static final String INTERNAL_ESTIMATE_RECEIVER = "info@ibioscience.co.kr";

    private final EstimateRepository estimateRepository;
    private final HiworksEstimateMailService hiworksEstimateMailService;

    public AdminPageResponse<AdminEstimateListRowDto> getEstimateList(Long memberId, AdminEstimateListSearchRequest request) {
        org.springframework.data.domain.Page<AdminEstimateListRowDto> page = estimateRepository.searchAdminEstimateList(memberId, request);

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

        if (!hasMeaningfulHtmlContent(bodyHtml)) {
            throw new IllegalArgumentException("이메일 내용을 입력해주세요.");
        }

        String internalSubject = buildInternalMailSubject(estimate, subject);
        String internalBodyHtml = buildInternalMailBodyHtml(estimate, subject, bodyHtml);

        /**
         * 1) 고객 발송
         * 2) 내부 확인용 사본 발송
         *
         * 주의:
         * SMTP 발송은 DB 트랜잭션처럼 완전 원자적으로 롤백되지 않으므로,
         * 고객 발송 성공 후 내부 발송 실패가 발생할 수 있습니다.
         */
        hiworksEstimateMailService.sendHtmlMail(toEmail, subject, bodyHtml, attachments);
        hiworksEstimateMailService.sendHtmlMail(INTERNAL_ESTIMATE_RECEIVER, internalSubject, internalBodyHtml, attachments);

        if (EstimateCheckStatus.UNCHECKED.equals(estimate.getCheckStatus())) {
            estimate.setCheckStatus(EstimateCheckStatus.CHECKED);
        }
        if (estimate.getCheckedAt() == null) {
            estimate.setCheckedAt(LocalDateTime.now());
        }

        estimate.setAnswerStatus(EstimateAnswerStatus.ANSWERED);
        estimate.setAnsweredAt(LocalDateTime.now());
    }

    private String buildInternalMailSubject(Estimate estimate, String customerSubject) {
        Member member = estimate.getMember();
        String username = member != null && StringUtils.hasText(member.getUsername())
                ? member.getUsername().trim()
                : "unknown";

        return "[보낸견적] " + username + " 님의 견적에 대한 답신. - " + customerSubject;
    }

    private String buildInternalMailBodyHtml(Estimate estimate, String customerSubject, String customerBodyHtml) {
        Member member = estimate.getMember();
        CompanyProfile companyProfile = member != null ? member.getCompanyProfile() : null;
        List<EstimateItem> items = estimate.getItems() != null ? estimate.getItems() : new ArrayList<>();

        int productTypeCount = items.size();
        int totalQuantity = 0;
        for (EstimateItem item : items) {
            totalQuantity += item.getQuantity() != null ? item.getQuantity() : 0;
        }

        StringBuilder sb = new StringBuilder();

        sb.append("<div style=\"font-family:'Malgun Gothic',Arial,sans-serif;font-size:14px;line-height:1.7;color:#222;\">");

        sb.append("<div style=\"margin-bottom:24px;\">")
          .append("<h2 style=\"margin:0 0 10px 0;font-size:22px;\">[내부 수신용] 견적 답신 발송 사본</h2>")
          .append("<p style=\"margin:0;\">이 메일은 고객에게 실제 발송된 견적 답신과 동일한 첨부파일/본문을 포함하며, 내부 확인용 추가 정보가 함께 포함되어 있습니다.</p>")
          .append("</div>");

        sb.append("<div style=\"margin-bottom:24px;\">")
          .append("<h3 style=\"margin:0 0 10px 0;font-size:18px;\">기본 정보</h3>")
          .append("<table style=\"width:100%;border-collapse:collapse;border:1px solid #dcdcdc;\">")
          .append(buildInfoRow("고객 발송 제목", customerSubject))
          .append(buildInfoRow("견적 ID", estimate.getId()))
          .append(buildInfoRow("문의 제목", estimate.getTitle()))
          .append(buildInfoRow("신청일", formatDateTime(estimate.getRequestedAt())))
          .append(buildInfoRow("확인일", formatDateTime(estimate.getCheckedAt())))
          .append(buildInfoRow("답변일", formatDateTime(estimate.getAnsweredAt())))
          .append("</table>")
          .append("</div>");

        sb.append("<div style=\"margin-bottom:24px;\">")
          .append("<h3 style=\"margin:0 0 10px 0;font-size:18px;\">회원 정보</h3>")
          .append("<table style=\"width:100%;border-collapse:collapse;border:1px solid #dcdcdc;\">")
          .append(buildInfoRow("회원 ID", member != null ? member.getId() : null))
          .append(buildInfoRow("아이디(username)", member != null ? member.getUsername() : null))
          .append(buildInfoRow("이름", member != null ? member.getName() : null))
          .append(buildInfoRow("이메일", member != null ? member.getEmail() : null))
          .append(buildInfoRow("휴대폰", member != null ? member.getMobile() : null))
          .append(buildInfoRow("유선전화", member != null ? member.getTel() : null))
          .append(buildInfoRow("도메인", member != null ? member.getDomain() : null))
          .append(buildInfoRow("고객유형", member != null ? member.getCustomerType() : null))
          .append(buildInfoRow("딜러유형", member != null ? member.getDealerType() : null))
          .append("</table>")
          .append("</div>");

        if (companyProfile != null) {
            sb.append("<div style=\"margin-bottom:24px;\">")
              .append("<h3 style=\"margin:0 0 10px 0;font-size:18px;\">회사 정보</h3>")
              .append("<table style=\"width:100%;border-collapse:collapse;border:1px solid #dcdcdc;\">")
              .append(buildInfoRow("회사명", companyProfile.getCompanyName()))
              .append(buildInfoRow("대표자명", companyProfile.getCeoName()))
              .append(buildInfoRow("사업자등록번호", companyProfile.getBusinessRegistrationNumber()))
              .append("</table>")
              .append("</div>");
        }

        sb.append("<div style=\"margin-bottom:24px;\">")
          .append("<h3 style=\"margin:0 0 10px 0;font-size:18px;\">문의 제품 요약</h3>")
          .append("<p style=\"margin:0 0 10px 0;\">")
          .append("총 <strong>").append(productTypeCount).append("</strong>개 제품 종류, ")
          .append("총 요청 수량 <strong>").append(totalQuantity).append("</strong>개")
          .append("</p>");

        if (items.isEmpty()) {
            sb.append("<div style=\"padding:12px;border:1px solid #dcdcdc;background:#fafafa;\">문의 제품이 없습니다.</div>");
        } else {
            sb.append("<table style=\"width:100%;border-collapse:collapse;border:1px solid #dcdcdc;\">")
              .append("<thead>")
              .append("<tr>")
              .append("<th style=\"padding:10px;border:1px solid #dcdcdc;background:#f7f7f7;\">No</th>")
              .append("<th style=\"padding:10px;border:1px solid #dcdcdc;background:#f7f7f7;\">대분류</th>")
              .append("<th style=\"padding:10px;border:1px solid #dcdcdc;background:#f7f7f7;\">중분류</th>")
              .append("<th style=\"padding:10px;border:1px solid #dcdcdc;background:#f7f7f7;\">소분류</th>")
              .append("<th style=\"padding:10px;border:1px solid #dcdcdc;background:#f7f7f7;\">브랜드</th>")
              .append("<th style=\"padding:10px;border:1px solid #dcdcdc;background:#f7f7f7;\">제품명</th>")
              .append("<th style=\"padding:10px;border:1px solid #dcdcdc;background:#f7f7f7;\">제품코드</th>")
              .append("<th style=\"padding:10px;border:1px solid #dcdcdc;background:#f7f7f7;\">수량</th>")
              .append("</tr>")
              .append("</thead>")
              .append("<tbody>");

            for (int i = 0; i < items.size(); i++) {
                EstimateItem item = items.get(i);

                sb.append("<tr>")
                  .append("<td style=\"padding:10px;border:1px solid #dcdcdc;text-align:center;\">").append(i + 1).append("</td>")
                  .append("<td style=\"padding:10px;border:1px solid #dcdcdc;\">").append(escape(item.getLargeCategoryName())).append("</td>")
                  .append("<td style=\"padding:10px;border:1px solid #dcdcdc;\">").append(escape(item.getMediumCategoryName())).append("</td>")
                  .append("<td style=\"padding:10px;border:1px solid #dcdcdc;\">").append(escape(item.getSmallCategoryName())).append("</td>")
                  .append("<td style=\"padding:10px;border:1px solid #dcdcdc;\">").append(escape(item.getBrandName())).append("</td>")
                  .append("<td style=\"padding:10px;border:1px solid #dcdcdc;\">").append(escape(item.getProductName())).append("</td>")
                  .append("<td style=\"padding:10px;border:1px solid #dcdcdc;\">").append(escape(item.getProductCode())).append("</td>")
                  .append("<td style=\"padding:10px;border:1px solid #dcdcdc;text-align:center;\">").append(item.getQuantity() != null ? item.getQuantity() : 0).append("</td>")
                  .append("</tr>");
            }

            sb.append("</tbody>")
              .append("</table>");
        }

        sb.append("</div>");

        sb.append("<div style=\"margin-top:32px;\">")
          .append("<h3 style=\"margin:0 0 10px 0;font-size:18px;\">고객에게 발송된 본문</h3>")
          .append("<div style=\"padding:18px;border:1px solid #dcdcdc;border-radius:8px;background:#fff;\">")
          .append(customerBodyHtml)
          .append("</div>")
          .append("</div>");

        sb.append("</div>");

        return sb.toString();
    }

    private String buildInfoRow(String label, Object value) {
        return new StringBuilder()
                .append("<tr>")
                .append("<th style=\"width:220px;padding:10px;border:1px solid #dcdcdc;background:#f7f7f7;text-align:left;font-weight:600;\">")
                .append(escape(label))
                .append("</th>")
                .append("<td style=\"padding:10px;border:1px solid #dcdcdc;\">")
                .append(escape(value))
                .append("</td>")
                .append("</tr>")
                .toString();
    }

    private String escape(Object value) {
        if (value == null) {
            return "-";
        }

        String text = String.valueOf(value);
        if (!StringUtils.hasText(text)) {
            return "-";
        }

        return HtmlUtils.htmlEscape(text);
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "-";
        }
        return value.format(DATE_TIME_FORMATTER);
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

    private boolean hasMeaningfulHtmlContent(String html) {
        if (!StringUtils.hasText(html)) {
            return false;
        }

        String normalized = html
                .replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                .replaceAll("(?is)<br\\s*/?>", " ")
                .replace("&nbsp;", " ");

        String textOnly = HTML_TAG_PATTERN.matcher(normalized).replaceAll(" ").trim();
        if (StringUtils.hasText(textOnly)) {
            return true;
        }

        return IMG_TAG_PATTERN.matcher(normalized).find();
    }
}