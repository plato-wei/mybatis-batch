package com.wxq.mybatis.batch.core;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransaction;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;

/**
 * @author weixiaoqiang
 * @date 2024/5/14
 **/
public class BatchExecutorFactory {

    private SqlSessionFactory sqlSessionFactory;

    private TransactionFactory transactionFactory;

    private DataSource dataSource;

    public BatchExecutorFactory(SqlSessionFactory sqlSessionFactory, DataSource dataSource) {
        this(sqlSessionFactory, dataSource, null);
    }

    public BatchExecutorFactory(SqlSessionFactory sqlSessionFactory, DataSource dataSource, TransactionFactory transactionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.dataSource = dataSource;
        TransactionFactory defaultTransactionFactory = sqlSessionFactory.getConfiguration().getEnvironment().getTransactionFactory();
        this.transactionFactory = transactionFactory == null ? defaultTransactionFactory : transactionFactory;
    }

    public <M> BatchExecutor<M> doBatch(Class<M> mapperClass) {
        Transaction transaction = transactionFactory.newTransaction(dataSource, null, false);
        return new DefaultBatchExecutor<>(sqlSessionFactory, transaction, mapperClass);
    }
}
