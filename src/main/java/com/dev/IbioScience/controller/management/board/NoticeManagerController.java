package com.dev.IbioScience.controller.management.board;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.dev.IbioScience.dto.board.notice.NoticeSearchCond;
import com.dev.IbioScience.service.board.notice.NoticeService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/manager")
public class NoticeManagerController {

    private final NoticeService noticeService;

    @GetMapping("/noticeManager")
    public String noticeManager(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        // size 허용값 보정
        int fixedSize = switch (size) {
            case 10, 30, 50, 70, 100 -> size;
            default -> 10;
        };

        Pageable pageable = PageRequest.of(page, fixedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        NoticeSearchCond cond = NoticeSearchCond.builder()
                .title(title)
                .from(from)
                .to(to)
                .build();

        Page<?> noticePage = noticeService.getNoticePage(cond, pageable);

        model.addAttribute("noticePage", noticePage);
        model.addAttribute("cond", cond);
        model.addAttribute("size", fixedSize);

        return "administration/board/notice/noticeManager";
    }

    @GetMapping("/noticeInsertForm")
    public String noticeInsertForm() {
        return "administration/board/notice/noticeInsertForm";
    }

    @GetMapping("/noticeDetail/{id}")
    public String noticeDetail(@PathVariable("id") Long id, Model model) {
        Object notice = noticeService.getNoticeDetail(id);
        model.addAttribute("notice", notice);
        return "administration/board/notice/noticeDetail";
    }
}