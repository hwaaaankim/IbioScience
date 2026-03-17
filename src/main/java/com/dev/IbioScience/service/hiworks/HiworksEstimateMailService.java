package com.dev.IbioScience.service.hiworks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

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

    @Qualifier("hiworksEstimateMailSender")
    private final JavaMailSender hiworksEstimateMailSender;

    private final HiworksMailProperties hiworksMailProperties;

    public void sendHtmlMail(String to, String subject, String html, List<MultipartFile> attachments) {
        try {
            MimeMessage mimeMessage = hiworksEstimateMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    true,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            helper.setFrom(new InternetAddress(
                    hiworksMailProperties.getResolvedFromAddress(),
                    hiworksMailProperties.getFromName(),
                    StandardCharsets.UTF_8.name()
            ));

            if (!CollectionUtils.isEmpty(attachments)) {
                for (MultipartFile file : attachments) {
                    if (file == null || file.isEmpty()) {
                        continue;
                    }

                    String originalFileName = StringUtils.hasText(file.getOriginalFilename())
                            ? file.getOriginalFilename()
                            : "attachment";

                    helper.addAttachment(
                            originalFileName,
                            new ByteArrayResource(file.getBytes()),
                            file.getContentType()
                    );
                }
            }

            hiworksEstimateMailSender.send(mimeMessage);
        } catch (MailException | MessagingException | IOException e) {
            log.error("하이웍스 견적 메일 발송 실패. to={}", to, e);
            throw new IllegalStateException("견적서 메일 발송에 실패했습니다.");
        }
    }
}