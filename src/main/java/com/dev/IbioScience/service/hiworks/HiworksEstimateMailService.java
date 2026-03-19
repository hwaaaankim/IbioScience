package com.dev.IbioScience.service.hiworks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.config.HiworksMailProperties;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HiworksEstimateMailService {

    private static final Pattern IMG_TAG_WITH_SRC_PATTERN = Pattern.compile(
            "(?is)<img\\b([^>]*?)\\bsrc\\s*=\\s*(['\"])(.*?)\\2([^>]*)>"
    );

    private static final Pattern DATA_URL_IMAGE_PATTERN = Pattern.compile(
            "^data:(image/[a-zA-Z0-9.+-]+);base64,(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    @Qualifier("hiworksEstimateMailSender")
    private final JavaMailSender hiworksEstimateMailSender;

    private final HiworksMailProperties hiworksMailProperties;

    public void sendHtmlMail(String to, String subject, String html, List<MultipartFile> attachments) {
        try {
            MimeMessage mimeMessage = hiworksEstimateMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            InlineImageProcessingResult inlineImageProcessingResult = processInlineImages(html);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(inlineImageProcessingResult.getHtml(), true);
            helper.setFrom(new InternetAddress(
                    hiworksMailProperties.getResolvedFromAddress(),
                    hiworksMailProperties.getFromName(),
                    StandardCharsets.UTF_8.name()
            ));

            for (InlineImage inlineImage : inlineImageProcessingResult.getInlineImages()) {
                helper.addInline(
                        inlineImage.getContentId(),
                        new ByteArrayResource(inlineImage.getBytes()),
                        inlineImage.getContentType()
                );
            }

            if (!CollectionUtils.isEmpty(attachments)) {
                for (MultipartFile file : attachments) {
                    if (file == null || file.isEmpty()) {
                        continue;
                    }

                    String originalFileName = StringUtils.hasText(file.getOriginalFilename())
                            ? StringUtils.cleanPath(file.getOriginalFilename())
                            : "attachment";

                    String contentType = StringUtils.hasText(file.getContentType())
                            ? file.getContentType()
                            : "application/octet-stream";

                    helper.addAttachment(
                            originalFileName,
                            new ByteArrayResource(file.getBytes()),
                            contentType
                    );
                }
            }

            hiworksEstimateMailSender.send(mimeMessage);
        } catch (MailException | MessagingException | IOException | IllegalArgumentException e) {
            log.error("하이웍스 견적 메일 발송 실패. to={}", to, e);
            throw new IllegalStateException("견적서 메일 발송에 실패했습니다.");
        }
    }

    private InlineImageProcessingResult processInlineImages(String html) {
        if (!StringUtils.hasText(html)) {
            return new InlineImageProcessingResult("", new ArrayList<>());
        }

        Matcher matcher = IMG_TAG_WITH_SRC_PATTERN.matcher(html);
        StringBuffer sb = new StringBuffer();
        List<InlineImage> inlineImages = new ArrayList<>();

        while (matcher.find()) {
            String beforeSrcAttributes = matcher.group(1) != null ? matcher.group(1) : "";
            String quote = matcher.group(2);
            String src = matcher.group(3);
            String afterSrcAttributes = matcher.group(4) != null ? matcher.group(4) : "";

            InlineImage inlineImage = tryParseInlineImage(src);
            if (inlineImage == null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }

            String replacementTag = "<img"
                    + beforeSrcAttributes
                    + " src="
                    + quote
                    + "cid:"
                    + inlineImage.getContentId()
                    + quote
                    + afterSrcAttributes
                    + ">";

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacementTag));
            inlineImages.add(inlineImage);
        }

        matcher.appendTail(sb);
        return new InlineImageProcessingResult(sb.toString(), inlineImages);
    }

    private InlineImage tryParseInlineImage(String src) {
        if (!StringUtils.hasText(src)) {
            return null;
        }

        Matcher dataUrlMatcher = DATA_URL_IMAGE_PATTERN.matcher(src);
        if (!dataUrlMatcher.matches()) {
            return null;
        }

        String contentType = dataUrlMatcher.group(1);
        String base64Data = dataUrlMatcher.group(2).replaceAll("\\s+", "");

        byte[] bytes = Base64.getDecoder().decode(base64Data);
        String contentId = "estimate-inline-" + UUID.randomUUID().toString().replace("-", "");

        return new InlineImage(contentId, contentType, bytes);
    }

    private static final class InlineImageProcessingResult {
        private final String html;
        private final List<InlineImage> inlineImages;

        private InlineImageProcessingResult(String html, List<InlineImage> inlineImages) {
            this.html = html;
            this.inlineImages = inlineImages;
        }

        public String getHtml() {
            return html;
        }

        public List<InlineImage> getInlineImages() {
            return inlineImages;
        }
    }

    private static final class InlineImage {
        private final String contentId;
        private final String contentType;
        private final byte[] bytes;

        private InlineImage(String contentId, String contentType, byte[] bytes) {
            this.contentId = contentId;
            this.contentType = contentType;
            this.bytes = bytes;
        }

        public String getContentId() {
            return contentId;
        }

        public String getContentType() {
            return contentType;
        }

        public byte[] getBytes() {
            return bytes;
        }
    }
}