package com.wxq.mybatis.batch.core;

import com.wxq.mybatis.batch.exception.MybatisBatchException;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.Transaction;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;

/**
 * @author weixiaoqiang
 * @date 2024/5/14
 **/
public class DefaultBatchExecutor<M> implements BatchExecutor<M> {

    private Transaction transaction;

    private M mapper;

    private ExecutorContext context;

    public DefaultBatchExecutor(SqlSessionFactory sqlSessionFactory, Transaction transaction, Class<M> mapperInterface) {
        this.transaction = transaction;
        this.context = new ExecutorContext(mapperInterface, sqlSessionFactory.getConfiguration(), transaction);
        this.mapper = (M) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[]{mapperInterface}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                context.executeBatch();
                return -1;
            }
        });
    }

    @Override
    public M getMapper() {
        return mapper;
    }

    @Override
    public void executeBatch() throws MybatisBatchException {
        try {
            context.executeBatch();
        } catch (MybatisBatchException e) {
            doRelease();
            throw e;
        }
    }

    @Override
    public void commit() throws MybatisBatchException {
        try {
            context.executeBatch();
            transaction.commit();
        } catch (SQLException | MybatisBatchException e) {
            throw new MybatisBatchException("Commit failed", e);
        } finally {
            doRelease();
        }
    }

    @Override
    public void rollback() throws MybatisBatchException {
        try {
            transaction.rollback();
        } catch (SQLException e) {
            throw new MybatisBatchException("Rollback failed", e);
        } finally {
            doRelease();
        }
    }

    private void doRelease() {
        try {
            transaction.close();
        } catch (SQLException e) {
            throw new MybatisBatchException("Close transaction failed", e);
        }
    }
}
