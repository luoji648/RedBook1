package com.zhiyan.redbookbackend.exception;

/**
 * 业务可预期失败，由全局异常处理转换为 {@link com.zhiyan.redbookbackend.dto.Result#fail(String)}，
 * 并在事务方法中抛出以触发回滚。
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }
}
