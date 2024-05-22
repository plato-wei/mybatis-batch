# mybatis-batch
Mybatis-Batch是一个批处理工具，支持手动控制事务和提交批处理。

# 快速开始

1.添加镜像仓库

```xml
<repositories>
  <repository>
    <id>github-repo</id>
    <name>Github Maven Repository</name>
    <url>https://raw.githubusercontent.com/plato-wei/mvn-repo/main/</url>
  </repository>
</repositories>
```

2.引入依赖

```xml
<dependency>
  <groupId>com.wxq.mybaits</groupId>
  <artifactId>mybatis-batch</artifactId>
  <version>1.0.0</version>
</dependency>
```

3.配置BatchExecutorFactory

```
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.Transaction;
import javax.sql.DataSource;
import org.apache.ibatis.transaction.Transaction;

@Configuration
public SystemConfig {

	@Bean
  public BatchExecutorFacotry batchExecutorFactory(SqlSessionFactory sqlSessionFactory, 
  																									DataSource dataSource, 
  																									TransactionFactory transactionFactory) {
  	return new BatchExecutorFacotry(sqlSessionFactory, dataSource, transactionFactory);
  }
  
  @Bean
  public TransactionFactory batchTransactionFactory() {
    return new MyBatisBatchTransactionFactory();
  }
}
```

3.使用BatchExecutorFactory生成代理对象

```java
interface UserMapper{
  
  @Insert("insert into user(username, password) values" +
  "<foreach collection=\"users\" item=\"item\" open=\"(\" close=\")\" seperator=\",\">" +
  "#{item.username},#{item.password}" +
  "</foreach>" +
  ")
	int addUsers(List<User> users);
	
	@Insert("insert into user(username, password) values(#{username}, #{password})")
	int addUser(Uesr user);
          
  int deleteUserByIds(@Param("ids") Collections<Serilizable> ids);
}       
```

1)当前线程已存在spring事务

```java
@Service
public class UserService {

  @Autowired
  private BatchExecutorFacotry batchFactory;
  
  @Transactional
  public void addUsers() {
    // 已存在事务
    // 省略其他数据库操作
    // 实际可能来自其他数据库或前端数据
    List<User> users = new ArrayList(1000);
    BatchExecutor<UserMapper> executor = batchFacotry.doBatch(UserMapper.class);
    int count = 0;
    for(User user: users) {
      executor.getMapper().addUser(user);
      if(count % 1000 == 0) {
        executor.executeBatch();
      }
      count++;
    }
  }
}
```

2)当前线程不存在事务

```java
@Service
public class UserService {

  @Autowired
  private BatchExecutorFacotry batchFactory;
  
  public void addUsers() {
    // 不已存在事务
    // 省略其他数据库操作
    // 实际可能来自其他数据库或前端数据
    List<User> users = new ArrayList(1000);
    BatchExecutor<UserMapper> executor = batchFacotry.doBatch(UserMapper.class);
    int count = 0;
    try {
      for(User user: users) {
        executor.getMapper().addUser(user);
        if(count % 1000 == 0) {
          executor.executeBatch();
        }
        count++;
      }
    } catch(Exception e) {
      executor.rollback();
    } finally {
      executor.commit();
    }
  }
}
```

