package com.dev.IbioScience.controller.management.board;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.dev.IbioScience.service.board.qna.QnaCategoryService;
import com.dev.IbioScience.service.board.qna.QnaService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/manager")
@RequiredArgsConstructor
public class QnaManagerController {

    private final QnaService qnaService;
    private final QnaCategoryService qnaCategoryService;

    @GetMapping("/qnaCategoryManager")
    public String qnaCategoryManager(Model model) {
        model.addAttribute("categoryRows", qnaCategoryService.listWithCounts());
        return "administration/board/qna/qnaCategoryManager";
    }

    @GetMapping("/qnaManager")
    public String qnaManager(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<?> qnaPage = qnaService.search(title, from, to, pageable);

        model.addAttribute("qnaPage", qnaPage);
        model.addAttribute("title", title);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("size", size);

        return "administration/board/qna/qnaManager";
    }

    @GetMapping("/qnaInsertForm")
    public String qnaInsertForm(Model model) {
        model.addAttribute("categories", qnaCategoryService.list());
        return "administration/board/qna/qnaInsertForm";
    }

    @GetMapping("/qnaDetail/{id}")
    public String qnaDetail(@PathVariable Long id, Model model) {
        model.addAttribute("qna", qnaService.getDetail(id));
        model.addAttribute("categories", qnaCategoryService.list());
        return "administration/board/qna/qnaDetail";
    }
}