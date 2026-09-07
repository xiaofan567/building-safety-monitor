package com.qst.smartbuildings.config;

import com.qst.smartbuildings.exception.AuthException;
import com.qst.smartbuildings.exception.EquipmentException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 全局异常处理器
@RestControllerAdvice
public class ConfigExceptionHandler {

    // 处理自定义认证异常（如登录失败、账号禁用等）
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException e) {
        if (e.getResultCode() != null) {
            ErrorResponse error = new ErrorResponse(String.valueOf(e.getResultCode().getCode()), e.getResultCode().getMsg());
            return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
        }
        ErrorResponse error = new ErrorResponse("1001", e.getMessage());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // 处理访问被拒绝异常（如无权限访问资源）
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        ErrorResponse error = new ErrorResponse("403", "没有权限访问该资源");
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    // 处理认证失败相关异常（如密码错误）
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException e) {
        ErrorResponse error = new ErrorResponse("1004", "账号或密码错误");
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // 处理用户不存在异常
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFoundException(UsernameNotFoundException e) {
        ErrorResponse error = new ErrorResponse("1004", "用户不存在");
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }
    
    // 处理设备异常
    @ExceptionHandler(EquipmentException.class)
    public ResponseEntity<ErrorResponse> handleEquipmentLocationException(EquipmentException e) {
        ErrorResponse error = new ErrorResponse("500", "设备异常，原因：" + e.getMsg());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // 处理其他未捕获的认证相关异常
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(Exception e) {
        ErrorResponse error = new ErrorResponse("500", "系统错误：" + e.getMessage());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // 错误响应实体类
    public static class ErrorResponse {
        private String code;
        private String message;

        public ErrorResponse(String code, String message) {
            this.code = code;
            this.message = message;
        }

        // getter和setter
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}