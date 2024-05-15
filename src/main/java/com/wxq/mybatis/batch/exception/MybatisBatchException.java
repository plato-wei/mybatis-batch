package com.wxq.mybatis.batch.exception;

/**
 * @author weixiaoqiang
 * @date 2024/5/14
 **/
public class MybatisBatchException extends RuntimeException{

    public MybatisBatchException(String message) {
        super(message);
    }

    public MybatisBatchException(String message, Throwable e) {
        super(message, e);
    }
}
