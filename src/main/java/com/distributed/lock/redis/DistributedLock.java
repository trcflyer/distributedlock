package com.distributed.lock.redis;

import java.util.Collections;
import java.util.UUID;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;
/**
 * 
 * @author rencai.tang
 *
 */
public class DistributedLock {
	private static final String LOCK_SUCCESS = "OK";
    private static final Long RELEASE_SUCCESS = 1L;
    private static final String SET_IF_NOT_EXIST = "NX";
    private static final String SET_WITH_EXPIRE_TIME = "PX";
    
    private final JedisPool jedisPool;

    public DistributedLock(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    /**
     * 加锁
     * @param locaName  锁的key
     * @param acquireTimeout  获取超时时间
     * @param timeout   锁的超时时间
     * @return 锁标识
     */
    public String lockWithTimeout(String locaName,
                                  long acquireTimeout, long timeout) {
        Jedis jedis = null;
        String retIdentifier = null;
        try {
            // 获取连接
        	jedis = jedisPool.getResource();
            // 随机生成一个value
            String identifier = UUID.randomUUID().toString();
            // 锁名，即key值
            String lockKey = "lock:" + locaName;
            // 超时时间，上锁后超过此时间则自动释放锁
            int lockExpire = (int)(timeout / 1000);

            // 获取锁的超时时间，超过这个时间则放弃获取锁
            long end = System.currentTimeMillis() + acquireTimeout;
            while (System.currentTimeMillis() < end) {//重试机制
            	/*第一个为key，我们使用key来当锁，因为key是唯一的。

            	第二个为value，我们传的是requestId，很多童鞋可能不明白，有key作为锁不就够了吗，为什么还要用到value？原因就是我们在上面讲到可靠性时，分布式锁要满足加锁和解锁必须是同一个客户端，通过给value赋值为requestId，我们就知道这把锁是哪个请求加的了，在解锁的时候就可以有依据。requestId可以使用UUID.randomUUID().toString()方法生成。

            	第三个为nxxx，这个参数我们填的是NX，意思是SET IF NOT EXIST，即当key不存在时，我们进行set操作；若key已经存在，则不做任何操作；

            	第四个为expx，这个参数我们传的是PX，意思是我们要给这个key加一个过期的设置，具体时间由第五个参数决定。

            	第五个为time，与第四个参数相呼应，代表key的过期时间。*/
            	String result = jedis.set(lockKey, identifier, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, lockExpire);
            	if (LOCK_SUCCESS.equals(result)) {
            		retIdentifier = identifier;
                    return retIdentifier;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (JedisException e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
            	jedis.close();
            }
        }
        return retIdentifier;
    }

    /**
     * 释放锁
     * @param lockName 锁的key
     * @param identifier    释放锁的标识
     * @return
     */
    public boolean releaseLock(String lockName, String identifier) {
        Jedis jedis = null;
        String lockKey = "lock:" + lockName;
        try {
        	jedis = jedisPool.getResource();
        	/*
        	 * 确保释放锁的过程是原子性的，使用lua代码
        	 */
        	 String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
             Object result = jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(identifier));
             if (RELEASE_SUCCESS.equals(result)) {
                 return true;
             }
             return false;
        } catch (JedisException e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
            	jedis.close();
            }
        }
        return false;
    }
}
