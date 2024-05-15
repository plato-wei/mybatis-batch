package com.wxq.mybatis.batch.core;

import com.wxq.mybatis.batch.exception.MybatisBatchException;

/**
 * @author weixiaoqiang
 * @date 2024/5/14
 **/
public interface BatchExecutor<M> {

    M getMapper();

    void executeBatch() throws MybatisBatchException;

    void commit() throws MybatisBatchException;

    void rollback() throws MybatisBatchException;
}
