package com.dev.IbioScience.service.board.event;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.board.event.EventCreateReq;
import com.dev.IbioScience.dto.board.event.EventSearchCond;
import com.dev.IbioScience.dto.board.event.EventUpdateReq;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.board.event.Event;
import com.dev.IbioScience.model.board.event.EventImage;
import com.dev.IbioScience.model.board.event.EventImage.Kind;
import com.dev.IbioScience.repository.auth.MemberRepository;
import com.dev.IbioScience.repository.board.event.EventImageRepository;
import com.dev.IbioScience.repository.board.event.EventRepository;
import com.dev.IbioScience.service.board.event.EventFileStorageUtil.MovedFile;
import com.dev.IbioScience.service.board.event.EventFileStorageUtil.StoredFile;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventImageRepository eventImageRepository;
    private final MemberRepository memberRepository;

    @Value("${spring.upload.path}")
    private String uploadBasePath;

    /** HTML에서 /upload/event/... 이미지 src 추출 */
    private static final Pattern IMG_SRC_PATTERN =
            Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    @Override
    @Transactional(readOnly = true)
    public Page<Event> search(EventSearchCond cond, Pageable pageable) {
        Specification<Event> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            if (StringUtils.hasText(cond.getTitle())) {
                ps.add(cb.like(root.get("title"), "%" + cond.getTitle().trim() + "%"));
            }

            // 작성일 범위 (BaseTimeEntity의 createdAt 필드명을 createdAt으로 가정)
            if (cond.getFromDate() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), cond.getFromDate().atStartOfDay()));
            }
            if (cond.getToDate() != null) {
                ps.add(cb.lessThan(root.get("createdAt"), cond.getToDate().plusDays(1).atStartOfDay()));
            }

            // 진행여부: start<=today<=end => ONGOING, end<today => ENDED
            String status = StringUtils.hasText(cond.getStatus()) ? cond.getStatus() : "ALL";
            LocalDate today = LocalDate.now();

            if ("ONGOING".equalsIgnoreCase(status)) {
                ps.add(cb.lessThanOrEqualTo(root.get("startDate"), today));
                ps.add(cb.greaterThanOrEqualTo(root.get("endDate"), today));
            } else if ("ENDED".equalsIgnoreCase(status)) {
                ps.add(cb.lessThan(root.get("endDate"), today));
            }

            return cb.and(ps.toArray(new Predicate[0]));
        };

        Pageable p = pageable;
        if (pageable.getSort().isUnsorted()) {
            p = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        }
        return eventRepository.findAll(spec, p);
    }

    @Override
    @Transactional(readOnly = true)
    public Event getOrThrow(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("event not found: " + id));
    }

    @Override
    public Event create(Long writerMemberId, EventCreateReq req, MultipartFile repImage) {
        if (writerMemberId == null) throw new IllegalArgumentException("writerMemberId required");
        if (!StringUtils.hasText(req.getTitle())) throw new IllegalArgumentException("title required");
        if (req.getStartDate() == null || req.getEndDate() == null) throw new IllegalArgumentException("start/end required");
        if (req.getEndDate().isBefore(req.getStartDate())) throw new IllegalArgumentException("endDate must be >= startDate");
        if (repImage == null || repImage.isEmpty()) throw new IllegalArgumentException("repImage required");

        Member writer = memberRepository.findById(writerMemberId)
                .orElseThrow(() -> new NoSuchElementException("member not found: " + writerMemberId));

        Event event = Event.builder()
                .title(req.getTitle().trim())
                .writer(writer)
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .contentHtml(StringUtils.hasText(req.getContentHtml()) ? req.getContentHtml() : "")
                .viewCount(0L)
                .build();

        // 1) 먼저 저장해서 eventId 확보
        event = eventRepository.save(event);

        // 2) 대표이미지 저장(메인 폴더)
        saveOrReplaceRepresentative(event, repImage);

        // 3) 본문 temp 이미지 -> main 이동 + html 치환 + 이미지 테이블 동기화
        String finalHtml = moveTempImagesAndRewriteHtml(event.getId(), event.getContentHtml());
        event.setContentHtml(finalHtml);

        // 4) 최종 저장
        return eventRepository.save(event);
    }

    @Override
    public Event update(Long id, EventUpdateReq req, MultipartFile repImage) {
        Event event = getOrThrow(id);

        if (!StringUtils.hasText(req.getTitle())) throw new IllegalArgumentException("title required");
        if (req.getStartDate() == null || req.getEndDate() == null) throw new IllegalArgumentException("start/end required");
        if (req.getEndDate().isBefore(req.getStartDate())) throw new IllegalArgumentException("endDate must be >= startDate");

        event.setTitle(req.getTitle().trim());
        event.setStartDate(req.getStartDate());
        event.setEndDate(req.getEndDate());
        event.setContentHtml(StringUtils.hasText(req.getContentHtml()) ? req.getContentHtml() : "");

        // 대표이미지 교체(선택)
        if (repImage != null && !repImage.isEmpty()) {
            saveOrReplaceRepresentative(event, repImage);
        }

        // 본문 temp 이동/치환 + 이미지 동기화(불필요 이미지 삭제)
        String finalHtml = moveTempImagesAndRewriteHtml(event.getId(), event.getContentHtml());
        event.setContentHtml(finalHtml);

        return eventRepository.save(event);
    }

    @Override
    public void delete(Long id) {
        Event event = getOrThrow(id);

        // 1) 대표 이미지 파일 삭제
        if (StringUtils.hasText(event.getRepImageRelPath())) {
            EventFileStorageUtil.deleteIfExists(uploadBasePath, event.getRepImageRelPath());
        }

        // 2) 본문 이미지 파일들 삭제
        List<EventImage> imgs = eventImageRepository.findAllByEventIdAndKind(id, Kind.CONTENT);
        for (EventImage img : imgs) {
            EventFileStorageUtil.deleteIfExists(uploadBasePath, img.getRelPath());
        }

        // 3) DB 삭제 (image는 FK cascade)
        eventRepository.delete(event);
    }

    @Override
    public EventImage uploadTempImage(MultipartFile file) {
        try {
            String day = EventFileStorageUtil.todayFolder();
            String relDir = "event/temp/" + day;

            StoredFile stored = EventFileStorageUtil.saveToDir(uploadBasePath, relDir, file);

            String url = "/upload/" + stored.getRelPath().replace("\\", "/");

            // 이벤트 생성 전이라 event_id는 NULL 불가 → 임시 업로드는 EventImage로 저장하지 않고,
            // "최종 저장 시 HTML 파싱+이동+동기화"로 관리하는 게 깔끔합니다.
            // 하지만 사용자는 “관리 엔티티 필요”라고 하셨으니, 여기서는 event 없이 저장하지 말고
            // 최종 저장 시에만 tb_event_image에 기록합니다.
            // => 따라서 여기서는 메타만 돌려줍니다(컨트롤러에서 응답).

            return EventImage.builder()
                    .kind(Kind.CONTENT)
                    .originalName(stored.getOriginalName())
                    .storedName(stored.getStoredName())
                    .relPath(stored.getRelPath())
                    .url(url)
                    .size(stored.getSize())
                    .build();

        } catch (IOException e) {
            throw new IllegalStateException("temp upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void increaseViewCount(Long id) {
        Event event = getOrThrow(id);
        event.setViewCount((event.getViewCount() == null ? 0 : event.getViewCount()) + 1);
        eventRepository.save(event);
    }

    /* =========================
       내부 메서드
    ========================= */

    private void saveOrReplaceRepresentative(Event event, MultipartFile repImage) {
        // 기존 대표 이미지 파일 삭제
        if (StringUtils.hasText(event.getRepImageRelPath())) {
            EventFileStorageUtil.deleteIfExists(uploadBasePath, event.getRepImageRelPath());
        }

        try {
            String day = EventFileStorageUtil.todayFolder();
            String relDir = "event/main/" + event.getId() + "/" + day;

            StoredFile stored = EventFileStorageUtil.saveToDir(uploadBasePath, relDir, repImage);
            String url = "/upload/" + stored.getRelPath().replace("\\", "/");

            event.setRepImageOriginalName(stored.getOriginalName());
            event.setRepImageStoredName(stored.getStoredName());
            event.setRepImageRelPath(stored.getRelPath());
            event.setRepImageUrl(url);
            event.setRepImageSize(stored.getSize());

            // 대표이미지 엔티티도 저장(선택: 관리 목적)
            // 기존 representative row 제거 후 재등록
            eventImageRepository.deleteAllByEventIdAndKind(event.getId(), Kind.REPRESENTATIVE);

            EventImage repRow = EventImage.builder()
                    .event(event)
                    .kind(Kind.REPRESENTATIVE)
                    .originalName(stored.getOriginalName())
                    .storedName(stored.getStoredName())
                    .relPath(stored.getRelPath())
                    .url(url)
                    .size(stored.getSize())
                    .build();
            eventImageRepository.save(repRow);

        } catch (IOException e) {
            throw new IllegalStateException("rep image save failed: " + e.getMessage(), e);
        }
    }

    /**
     * HTML 내 /upload/event/temp/{day}/{file} 을 찾아
     * /upload/event/main/{eventId}/{day}/{file} 로 이동시키고 HTML 치환.
     * 또한 CONTENT 이미지 테이블을 "현재 HTML에 포함된 이미지들"로 동기화(삭제 포함).
     */
    private String moveTempImagesAndRewriteHtml(Long eventId, String html) {
        String srcHtml = StringUtils.hasText(html) ? html : "";

        // 1) 현재 HTML의 img src 목록 추출
        Set<String> allSrc = extractImgSrc(srcHtml);

        // 2) temp src만 선별
        List<String> tempSrcList = new ArrayList<>();
        for (String src : allSrc) {
            if (src != null && src.contains("/upload/event/temp/")) {
                tempSrcList.add(src);
            }
        }

        String day = EventFileStorageUtil.todayFolder();
        String mainRelDir = "event/main/" + eventId + "/" + day;

        // 3) temp -> main 이동 + html 치환
        String rewritten = srcHtml;
        Map<String, String> replacedMap = new HashMap<>();

        for (String tempSrc : tempSrcList) {
            // tempSrc 예: /upload/event/temp/2026-01-05/AbCd....png
            String tempRelPath = toRelPathFromUploadUrl(tempSrc); // event/temp/...

            try {
                MovedFile moved = EventFileStorageUtil.move(uploadBasePath, tempRelPath, mainRelDir);
                String newUrl = "/upload/" + moved.getToRelPath();
                replacedMap.put(tempSrc, newUrl);
            } catch (IOException e) {
                // 이동 실패 시: HTML 치환 안 하고 그대로 둠 (하지만 그럼 나중에 깨질 수 있음)
                // 운영에서는 로깅/알림 권장
            }
        }

        for (Map.Entry<String, String> en : replacedMap.entrySet()) {
            rewritten = rewritten.replace(en.getKey(), en.getValue());
        }

        // 4) 최종 HTML 기준 이미지 동기화
        syncContentImages(eventId, rewritten);

        return rewritten;
    }

    private void syncContentImages(Long eventId, String finalHtml) {
        Set<String> finalSrc = extractImgSrc(finalHtml);

        // finalSrc 중 event/main 경로만 관리 대상으로 잡음 (외부 URL은 제외)
        Set<String> managed = new HashSet<>();
        for (String src : finalSrc) {
            if (src == null) continue;
            if (src.startsWith("/upload/event/main/")) {
                managed.add(src);
            }
        }

        // 기존 DB
        List<EventImage> existing = eventImageRepository.findAllByEventIdAndKind(eventId, Kind.CONTENT);
        Map<String, EventImage> byUrl = new HashMap<>();
        for (EventImage e : existing) byUrl.put(e.getUrl(), e);

        // 삭제 대상: DB에는 있는데 HTML에는 없다
        for (EventImage old : existing) {
            if (!managed.contains(old.getUrl())) {
                EventFileStorageUtil.deleteIfExists(uploadBasePath, old.getRelPath());
                eventImageRepository.delete(old);
            }
        }

        // 추가 대상: HTML에는 있는데 DB에 없다
        Event eventRef = getOrThrow(eventId);

        for (String url : managed) {
            if (byUrl.containsKey(url)) continue;

            String rel = toRelPathFromUploadUrl(url); // event/main/...
            String storedName = rel.substring(rel.lastIndexOf('/') + 1);

            EventImage row = EventImage.builder()
                    .event(eventRef)
                    .kind(Kind.CONTENT)
                    .originalName(null) // HTML에서 원본명은 알기 어려움
                    .storedName(storedName)
                    .relPath(rel)
                    .url(url)
                    .size(null)
                    .build();
            eventImageRepository.save(row);
        }
    }

    private Set<String> extractImgSrc(String html) {
        Set<String> out = new HashSet<>();
        if (!StringUtils.hasText(html)) return out;
        Matcher m = IMG_SRC_PATTERN.matcher(html);
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    private String toRelPathFromUploadUrl(String uploadUrl) {
        // "/upload/" 제거 후 나머지
        if (!StringUtils.hasText(uploadUrl)) return "";
        String u = uploadUrl.trim();
        int idx = u.indexOf("/upload/");
        if (idx < 0) return u;
        return u.substring(idx + "/upload/".length());
    }
}