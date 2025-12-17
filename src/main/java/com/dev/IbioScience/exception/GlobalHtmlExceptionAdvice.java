package com.dev.IbioScience.exception;

import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
public class GlobalHtmlExceptionAdvice {

	@ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(Model model, HttpServletRequest req) {
        model.addAttribute("status", 403);
        model.addAttribute("error", "Forbidden");
        model.addAttribute("message", "접근 권한이 없습니다.");
        model.addAttribute("path", req.getRequestURI());
        return "error/403";
    }

    @ExceptionHandler({ NoHandlerFoundException.class })
    public String handleNoHandler(Model model, HttpServletRequest req) {
        model.addAttribute("status", 404);
        model.addAttribute("error", "Not Found");
        model.addAttribute("message", "요청하신 페이지를 찾을 수 없습니다.");
        model.addAttribute("path", req.getRequestURI());
        return "error/404";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public String handleNoResource(Model model, HttpServletRequest req) {
        model.addAttribute("status", 404);
        model.addAttribute("error", "Not Found");
        model.addAttribute("message", "리소스를 찾을 수 없습니다.");
        model.addAttribute("path", req.getRequestURI());
        return "error/404";
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public String handleMethodNotSupported(Model model, HttpServletRequest req) {
        model.addAttribute("status", 405);
        model.addAttribute("error", "Method Not Allowed");
        model.addAttribute("message", "허용되지 않은 요청 방식입니다.");
        model.addAttribute("path", req.getRequestURI());
        return "error/405";
    }

    @ExceptionHandler(Exception.class)
    public String handleOther(Model model, HttpServletRequest req, Exception ex) {
        model.addAttribute("status", 500);
        model.addAttribute("error", "Internal Server Error");
        model.addAttribute("message", Optional.ofNullable(ex.getMessage()).orElse("서버 내부 오류가 발생했습니다."));
        model.addAttribute("path", req.getRequestURI());
        return "error/500";
    }
}
