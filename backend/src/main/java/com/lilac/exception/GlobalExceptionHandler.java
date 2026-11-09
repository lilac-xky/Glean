package com.lilac.exception;

import com.lilac.domain.result.Result;
import com.lilac.enums.HttpsCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理自定义的业务异常
     *
     * @param e 业务异常
     * @return Result
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleSystemException(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 404：请求路径没有任何 Handler 匹配，Spring 会兜底当作静态资源查找并抛出 NoResourceFoundException。
     * 单独拦截，避免被当成"系统异常"打 ERROR 堆栈刷屏。
     *
     * @param e 资源未找到异常
     * @return Result
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("请求路径不存在: {}", e.getResourcePath());
        return Result.error(HttpsCodeEnum.RESOURCE_NOT_FOUND);
    }

    /**
     * 处理所有未被捕获的未知异常
     *
     * @param e 未知异常
     * @return Result
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("未捕获的系统异常！ ", e);
        // 对外屏蔽内部细节，返回统一的系统错误提示
        return Result.error(HttpsCodeEnum.SYSTEM_ERROR);
    }
}