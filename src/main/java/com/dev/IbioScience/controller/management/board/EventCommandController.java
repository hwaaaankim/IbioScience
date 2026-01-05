package com.dev.IbioScience.controller.management.board;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.dev.IbioScience.dto.board.event.EventCreateReq;
import com.dev.IbioScience.dto.board.event.EventUpdateReq;
import com.dev.IbioScience.model.board.event.Event;
import com.dev.IbioScience.service.board.event.EventService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/manager/event")
public class EventCommandController {

    private final EventService eventService;

    @PostMapping("/create")
    public String create(
            @AuthenticationPrincipal(expression = "member.id") Long loginMemberId,
            @RequestParam("title") String title,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam(value = "contentHtml", required = false) String contentHtml,
            @RequestParam("repImage") MultipartFile repImage,
            RedirectAttributes ra
    ) {
        EventCreateReq req = EventCreateReq.builder()
                .title(title)
                .startDate(java.time.LocalDate.parse(startDate))
                .endDate(java.time.LocalDate.parse(endDate))
                .contentHtml(contentHtml)
                .build();

        Event saved = eventService.create(loginMemberId, req, repImage);

        ra.addFlashAttribute("msg", "이벤트가 등록되었습니다.");
        return "redirect:/admin/manager/eventDetail/" + saved.getId();
    }

    @PostMapping("/update/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam(value = "contentHtml", required = false) String contentHtml,
            @RequestParam(value = "repImage", required = false) MultipartFile repImage,
            RedirectAttributes ra
    ) {
        EventUpdateReq req = EventUpdateReq.builder()
                .title(title)
                .startDate(java.time.LocalDate.parse(startDate))
                .endDate(java.time.LocalDate.parse(endDate))
                .contentHtml(contentHtml)
                .build();

        eventService.update(id, req, repImage);

        ra.addFlashAttribute("msg", "이벤트가 수정되었습니다.");
        return "redirect:/admin/manager/eventDetail/" + id;
    }
}