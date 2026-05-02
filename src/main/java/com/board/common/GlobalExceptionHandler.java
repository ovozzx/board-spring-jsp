package com.board.common;

import com.board.exception.BusinessException;
import com.board.exception.NotFoundException;
import com.board.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 컨트롤러 실행 중 예외가 발생했을 때
 * 또는 컨트롤러 호출 과정에서 예외가 발생했을 때
 * = DispatcherServlet 안에서 예외를 처리하는 메커니즘
 */
// TODO : 대용량 다운로드 시 테스트
@ControllerAdvice // 모든 Controller에서 발생하는 예외를 전역적으로 처리하는 클래스
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DataAccessException.class) // 인프라(DB) 예외 (원본 메세지 노출 안 함)
    public String handleDatabase(DataAccessException e, Model model){
        log.error("DB 오류 발생", e);
        model.addAttribute("alertMsg", "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        model.addAttribute("redirectUrl", "/board/list");
        return "board/alert";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class) // 이 예외가 발생하면, 아래 메서드를 실행!
    public String handleMaxSizeException(MaxUploadSizeExceededException e, Model model) {
        log.error("MaxUploadSizeExceededException : ", e);
        model.addAttribute("alertMsg"," 파일 용량이 초과되었습니다. (최대 10MB)");
        model.addAttribute("redirectUrl", "/board/write");
        return "board/alert";
    }

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException e, Model model){ //  NotFoundException, ValidationException 도 다 잡힘
        log.warn("BusinessException : {}", e.getMessage());
        model.addAttribute("alertMsg", e.getMessage()); // 생성자에 넣은 문자열이 출력
        model.addAttribute("redirectUrl", "");
        return "board/alert";
    }

    @ExceptionHandler(Exception.class) // fallback
    public String handleAll(Exception e, Model model){
        log.error("Unhandled exception : ", e);
        model.addAttribute("alertMsg", "오류가 발생했습니다.");
        model.addAttribute("redirectUrl", "/board/list");
        return "board/alert";
    }
}
