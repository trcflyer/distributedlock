# distributedlock
## 分布式锁
### 1.使用redis实现分布式锁
> 获取锁使用jedis.set(key,value,"NX","EX",timeOut); 避免使用jedis.setnx(lockKey, identifier)和jedis.expire(lockKey, lockExpire)导致的非原子操作，释放锁使用的是lua代码实现原子性
### 2.使用zookeeper实现分布式锁
