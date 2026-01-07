package com.dev.IbioScience.service.board.notice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.board.notice.NoticeCreateReq;
import com.dev.IbioScience.dto.board.notice.NoticeSearchCond;
import com.dev.IbioScience.dto.board.notice.NoticeUpdateReq;
import com.dev.IbioScience.dto.board.notice.NoticeUploadTempRes;
import com.dev.IbioScience.enums.board.NoticeImageStatus;
import com.dev.IbioScience.model.board.notice.Notice;
import com.dev.IbioScience.model.board.notice.NoticeImage;
import com.dev.IbioScience.repository.board.notice.NoticeImageRepository;
import com.dev.IbioScience.repository.board.notice.NoticeRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class NoticeServiceImpl implements NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeImageRepository noticeImageRepository;

    @Value("${spring.upload.path}")
    private String uploadBasePath;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] RAND = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    @Transactional(readOnly = true)
    @Override
    public Page<Notice> getNoticePage(NoticeSearchCond cond, Pageable pageable) {
        Specification<Notice> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (cond != null) {
                if (StringUtils.hasText(cond.getTitle())) {
                    predicates.add(cb.like(root.get("title"), "%" + cond.getTitle().trim() + "%"));
                }
                if (cond.getFrom() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), cond.getFrom().atStartOfDay()));
                }
                if (cond.getTo() != null) {
                    // to 날짜 포함: 다음날 00:00 미만
                    predicates.add(cb.lessThan(root.get("createdAt"), cond.getTo().plusDays(1).atStartOfDay()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return noticeRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Notice getNoticeDetail(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("공지사항이 존재하지 않습니다. id=" + noticeId));
    }

    @Override
    public Long createNotice(NoticeCreateReq req) {
        if (req == null) throw new IllegalArgumentException("req is null");
        if (!StringUtils.hasText(req.getTitle())) throw new IllegalArgumentException("제목은 필수입니다.");
        if (!StringUtils.hasText(req.getContentHtml())) req.setContentHtml("");

        Notice notice = Notice.builder()
                .title(req.getTitle().trim())
                .contentHtml(req.getContentHtml())
                .writerMemberId(req.getWriterMemberId())
                .writerName(StringUtils.hasText(req.getWriterName()) ? req.getWriterName().trim() : null)
                .viewCount(0)
                .build();

        Notice saved = noticeRepository.save(notice);

        // ✅ temp -> main 이동 + html 치환 + 이미지 엔티티 notice 연결
        String draftKey = StringUtils.hasText(req.getDraftKey()) ? req.getDraftKey().trim() : null;
        if (StringUtils.hasText(draftKey)) {
            String updatedHtml = moveTempImagesToMainAndRewriteHtml(draftKey, saved.getId(), req.getContentHtml());
            saved.setContentHtml(updatedHtml);
        }

        return saved.getId();
    }

    @Override
    public void updateNotice(Long noticeId, NoticeUpdateReq req) {
        if (noticeId == null) throw new IllegalArgumentException("noticeId is null");
        if (req == null) throw new IllegalArgumentException("req is null");
        if (!StringUtils.hasText(req.getTitle())) throw new IllegalArgumentException("제목은 필수입니다.");
        if (!StringUtils.hasText(req.getContentHtml())) req.setContentHtml("");

        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("공지사항이 존재하지 않습니다. id=" + noticeId));

        String oldHtml = safe(notice.getContentHtml());
        String newHtml = safe(req.getContentHtml());

        // 1) 기존 MAIN 이미지 목록
        List<NoticeImage> oldMainImages = noticeImageRepository.findByNotice_IdAndImageStatus(noticeId, NoticeImageStatus.MAIN);

        // 2) newHtml에 포함된 /upload/... src 수집
        Set<String> newHtmlSrcSet = extractImageSrcSet(newHtml);

        // 3) 삭제된 이미지(기존 MAIN인데 새 HTML에는 없는 경우) 실제 파일 삭제 + DB 삭제
        for (NoticeImage img : oldMainImages) {
            if (!newHtmlSrcSet.contains(img.getUrl())) {
                deletePhysicalFileIfExists(img.getStoredRelPath(), img.getStoredName());
                noticeImageRepository.delete(img);
            }
        }

        // 4) 수정 중 업로드된 TEMP(draftKey) -> main 이동 + html 치환
        String draftKey = StringUtils.hasText(req.getDraftKey()) ? req.getDraftKey().trim() : null;
        if (StringUtils.hasText(draftKey)) {
            newHtml = moveTempImagesToMainAndRewriteHtml(draftKey, noticeId, newHtml);
        }

        // 5) 단순히 변경이 없으면 그대로 유지(= 같은 값 세팅)
        notice.setTitle(req.getTitle().trim());
        notice.setContentHtml(newHtml);
    }

    @Override
    public void deleteNotice(Long noticeId) {
        if (noticeId == null) throw new IllegalArgumentException("noticeId is null");

        // 이미지 파일 실제 삭제(ON DELETE CASCADE는 DB만 지워짐)
        List<NoticeImage> imgs = noticeImageRepository.findByNotice_Id(noticeId);
        for (NoticeImage img : imgs) {
            deletePhysicalFileIfExists(img.getStoredRelPath(), img.getStoredName());
        }

        noticeRepository.deleteById(noticeId);
    }

    @Override
    public NoticeUploadTempRes uploadTempImage(String draftKey, MultipartFile file) {
        if (!StringUtils.hasText(draftKey)) throw new IllegalArgumentException("draftKey is required");
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("file is required");

        String day = LocalDate.now().format(DAY_FMT);
        String relDir = "notice/temp/" + day + "/"; // ✅ 요구사항 경로
        String storedName = random16() + safeExt(file.getOriginalFilename());

        Path targetDir = Paths.get(uploadBasePath, relDir);
        Path targetFile = targetDir.resolve(storedName);

        try {
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("임시 이미지 저장 실패: " + e.getMessage(), e);
        }

        String url = "/upload/" + relDir + storedName;

        NoticeImage saved = noticeImageRepository.save(NoticeImage.builder()
                .notice(null)
                .draftKey(draftKey)
                .imageStatus(NoticeImageStatus.TEMP)
                .url(url)
                .storedRelPath(relDir)
                .storedName(storedName)
                .originalName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .build());

        return NoticeUploadTempRes.builder()
                .url(saved.getUrl())
                .originalName(saved.getOriginalName())
                .storedName(saved.getStoredName())
                .size(saved.getFileSize())
                .build();
    }

    // =========================
    // 내부 유틸
    // =========================

    private String moveTempImagesToMainAndRewriteHtml(String draftKey, Long noticeId, String html) {
        String srcHtml = safe(html);

        // HTML에 실제로 들어간 src만 대상으로 이동
        Set<String> htmlSrcSet = extractImageSrcSet(srcHtml);

        List<NoticeImage> tempImgs = noticeImageRepository.findByDraftKeyAndImageStatus(draftKey, NoticeImageStatus.TEMP);
        if (tempImgs.isEmpty()) return srcHtml;

        String day = LocalDate.now().format(DAY_FMT);
        String mainRelDir = "notice/main/" + noticeId + "/" + day + "/";

        Map<String, String> replaceMap = new HashMap<>();

        for (NoticeImage temp : tempImgs) {
            // draftKey의 TEMP 중에서도, HTML에 포함된 것만 이동
            if (!htmlSrcSet.contains(temp.getUrl())) {
                // HTML에 없는 temp는 그냥 삭제(임시 찌꺼기 정리)
                deletePhysicalFileIfExists(temp.getStoredRelPath(), temp.getStoredName());
                noticeImageRepository.delete(temp);
                continue;
            }

            // 물리 이동
            Path from = Paths.get(uploadBasePath, temp.getStoredRelPath(), temp.getStoredName());
            Path toDir = Paths.get(uploadBasePath, mainRelDir);
            Path to = toDir.resolve(temp.getStoredName());

            try {
                Files.createDirectories(toDir);
                if (Files.exists(from)) {
                    Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } else {
                    // 파일이 없으면 DB만 MAIN 처리하면 안 됨
                    throw new IllegalStateException("임시 파일이 존재하지 않습니다: " + from);
                }
            } catch (IOException e) {
                throw new IllegalStateException("임시->본 저장 이동 실패: " + e.getMessage(), e);
            }

            // URL 치환값
            String newUrl = "/upload/" + mainRelDir + temp.getStoredName();
            replaceMap.put(temp.getUrl(), newUrl);

            // 엔티티 업데이트: MAIN으로 변경 + notice 연결 + 경로 변경
            Notice noticeRef = Notice.builder().id(noticeId).build();
            temp.setNotice(noticeRef);
            temp.setImageStatus(NoticeImageStatus.MAIN);
            temp.setStoredRelPath(mainRelDir);
            temp.setUrl(newUrl);
            temp.setDraftKey(null); // 본저장은 draftKey 불필요
        }

        // HTML 치환
        String rewritten = replaceImgSrcByMap(srcHtml, replaceMap);
        return rewritten;
    }

    private Set<String> extractImageSrcSet(String html) {
        if (!StringUtils.hasText(html)) return Collections.emptySet();
        Document doc = Jsoup.parse(html);
        Elements imgs = doc.select("img[src]");
        return imgs.stream()
                .map(e -> e.attr("src"))
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    private String replaceImgSrcByMap(String html, Map<String, String> map) {
        if (!StringUtils.hasText(html) || map == null || map.isEmpty()) return html;
        Document doc = Jsoup.parse(html);
        Elements imgs = doc.select("img[src]");
        imgs.forEach(img -> {
            String src = img.attr("src");
            if (map.containsKey(src)) {
                img.attr("src", map.get(src));
            }
        });
        // body 내부 HTML만 저장
        return doc.body().html();
    }

    private void deletePhysicalFileIfExists(String relPath, String storedName) {
        if (!StringUtils.hasText(relPath) || !StringUtils.hasText(storedName)) return;
        Path p = Paths.get(uploadBasePath, relPath, storedName);
        try {
            Files.deleteIfExists(p);
        } catch (IOException e) {
            // 실제 운영에서는 로그 남기고 계속 진행(삭제 실패로 전체 트랜잭션 실패시키면 곤란)
            // 필요하면 로깅으로 교체하세요.
            System.out.println("[NOTICE] 파일 삭제 실패: " + p + " / " + e.getMessage());
        }
    }

    private String random16() {
        char[] buf = new char[16];
        for (int i = 0; i < buf.length; i++) buf[i] = RAND[SECURE_RANDOM.nextInt(RAND.length)];
        return new String(buf);
    }

    private String safeExt(String originalFilename) {
        String name = (originalFilename == null) ? "" : originalFilename.trim();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "";
        String ext = name.substring(dot).toLowerCase(Locale.ROOT);

        // 간단 화이트리스트(너무 공격적으로 막으면 업로드 불편)
        if (ext.length() > 10) return "";
        if (!ext.matches("\\.[a-z0-9]+")) return "";
        return ext;
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }
}