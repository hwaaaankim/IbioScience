package com.dev.IbioScience.controller.management.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.dev.IbioScience.dto.board.event.EventSearchCond;
import com.dev.IbioScience.model.board.event.Event;
import com.dev.IbioScience.service.board.event.EventService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/manager")
public class EventManagerController {

    private final EventService eventService;

    @GetMapping("/eventManager")
    public String eventManager(
            @ModelAttribute("cond") EventSearchCond cond,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model
    ) {
        int pageSize = (cond.getSize() != null ? cond.getSize() : size);
        // 허용 사이즈 제한
        if (!(pageSize == 10 || pageSize == 30 || pageSize == 50 || pageSize == 70 || pageSize == 100)) {
            pageSize = 10;
        }
        cond.setSize(pageSize);

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<Event> result = eventService.search(cond, pageable);

        model.addAttribute("page", result);
        return "administration/board/event/eventManager";
    }

    @GetMapping("/eventInsertForm")
    public String eventInsertForm() {
        return "administration/board/event/eventInsertForm";
    }

    @GetMapping("/eventDetail/{id}")
    public String eventDetail(@PathVariable Long id, Model model) {
        Event event = eventService.getOrThrow(id);
        model.addAttribute("event", event);
        return "administration/board/event/eventDetail";
    }
}