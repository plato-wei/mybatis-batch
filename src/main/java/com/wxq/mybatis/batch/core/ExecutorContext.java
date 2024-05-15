package com.wxq.mybatis.batch.core;

import com.wxq.mybatis.batch.exception.MybatisBatchException;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.transaction.Transaction;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author weixiaoqiang
 * @date 2024/5/14
 **/
public class ExecutorContext {

    private final Connection connection;

    private final Configuration configuration;

    private final Class mapperInterface;

    private PreparedStatement preparedStatement;

    private String sql;

    public ExecutorContext(Class mapperInterface, Configuration config, Transaction transaction) {
        this.mapperInterface = mapperInterface;
        this.configuration = config;
        this.connection = getConnection(transaction);
    }

    public void execute(Method method, Object[] args) throws MybatisBatchException {
        String methodName = method.getName();
        MappedStatement mappedStatement = resolveMappedStatement(mapperInterface, method.getName(), method.getDeclaringClass(), configuration);
        if(mappedStatement == null) {
            throw new MybatisBatchException("Invalid bound statement (not found): " + mapperInterface.getName() + "." + methodName);
        }
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
        switch (sqlCommandType) {
            case INSERT:
            case UPDATE:
            case DELETE:
                try {
                    ParamNameResolver paramNameResolver = new ParamNameResolver(configuration, method);
                    Object objectParameter = paramNameResolver.getNamedParams(args);
                    doUpdate(mappedStatement, objectParameter);
                }catch (SQLException e) {
                    throw new MybatisBatchException("Execution failed", e);
                }
                break;
            default: throw new MybatisBatchException("Unknown sql command type: " + sqlCommandType.name());
        }
    }

    public void executeBatch() throws MybatisBatchException {
        try {
            preparedStatement.executeBatch();
            preparedStatement.clearBatch();
        } catch (SQLException e) {
            throw new MybatisBatchException("Execution failed", e);
        }
    }

    private void doUpdate(MappedStatement ms, Object parameterObject) throws SQLException {
        BoundSql boundSql = ms.getBoundSql(parameterObject);
        initializeStatement(boundSql);
        DefaultParameterHandler handler = new DefaultParameterHandler(ms, parameterObject, boundSql);
        preparedStatement.clearParameters();
        handler.setParameters(preparedStatement);
        preparedStatement.addBatch();
    }

    private void initializeStatement(BoundSql boundSql) throws SQLException {
        String sql = boundSql.getSql();
        if(preparedStatement == null || !this.sql.equals(sql)) {
            this.sql = sql;
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            if(this.preparedStatement != null) {
                executeBatch();
            }
            this.preparedStatement = preparedStatement;
        }
    }

    private MappedStatement resolveMappedStatement(Class<?> mapperInterface, String methodName, Class<?> declaringClass, Configuration configuration) {
        String statementId = mapperInterface.getName() + "." + methodName;
        if (configuration.hasStatement(statementId)) {
            return configuration.getMappedStatement(statementId);
        } else if (mapperInterface.equals(declaringClass)) {
            return null;
        } else {
            for (Class<?> supperInterface : mapperInterface.getInterfaces()) {
                if (declaringClass.isAssignableFrom(supperInterface)) {
                    MappedStatement ms = this.resolveMappedStatement(supperInterface, methodName, declaringClass, configuration);
                    if (ms != null) {
                        return ms;
                    }
                }
            }
            return null;
        }
    }

    private Connection getConnection(Transaction transaction) {
        try {
            return transaction.getConnection();
        } catch (SQLException e) {
            throw new MybatisBatchException("Get connection failed", e);
        }
    }
}
