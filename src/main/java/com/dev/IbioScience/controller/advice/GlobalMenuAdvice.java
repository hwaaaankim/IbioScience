package com.dev.IbioScience.controller.advice;

import java.util.List;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.dev.IbioScience.dto.page.index.IdNameDTO;
import com.dev.IbioScience.service.menu.MenuService;

import lombok.RequiredArgsConstructor;

/**
 * 모든 View에서 전역 대분류 접근 가능하도록 주입
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalMenuAdvice {

    private final MenuService menuService;

    @ModelAttribute("globalLargeCategories")
    public List<IdNameDTO> injectLarge() {
        return menuService.listLarge();
    }
}