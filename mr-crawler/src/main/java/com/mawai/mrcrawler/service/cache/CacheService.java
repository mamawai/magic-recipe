package com.mawai.mrcrawler.service.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 缓存服务 - 封装Redis操作
 */
@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 存储值到缓存
     * @param key 键
     * @param value 值
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 存储值到缓存并设置过期时间
     * @param key 键
     * @param value 值
     * @param timeout 过期时间
     * @param unit 时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 存储值到缓存并设置过期时间
     * @param key 键
     * @param value 值
     * @param duration 过期时间
     */
    public void set(String key, Object value, Duration duration) {
        redisTemplate.opsForValue().set(key, value, duration);
    }

    /**
     * 从缓存获取值
     * @param key 键
     * @return 值
     */
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    /**
     * 判断键是否存在
     * @param key 键
     * @return 是否存在
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 删除键
     * @param key 键
     */
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /**
     * 获取键过期时间
     * @param key 键
     * @return 过期时间（秒）
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * 设置键过期时间
     * @param key 键
     * @param timeout 过期时间
     * @param unit 时间单位
     * @return 是否成功
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }

    /**
     * 设置键过期时间
     * @param key 键
     * @param duration 过期时间
     * @return 是否成功
     */
    public boolean expire(String key, Duration duration) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, duration));
    }
    
    /**
     * 向List右侧添加元素
     * @param key 键
     * @param values 值
     * @return 添加后List长度
     */
    public Long rightPushAll(String key, Object... values) {
        return redisTemplate.opsForList().rightPushAll(key, values);
    }
    
    /**
     * 获取List大小
     * @param key 键
     * @return List大小
     */
    public Long listSize(String key) {
        return redisTemplate.opsForList().size(key);
    }
    
    /**
     * 获取List指定索引元素
     * @param key 键
     * @param index 索引
     * @return 元素
     */
    public <T> T listIndex(String key, long index) {
        return (T) redisTemplate.opsForList().index(key, index);
    }
    
    /**
     * 设置状态字符串
     * @param key 键
     * @param status 状态
     * @param timeout 过期时间
     * @param unit 时间单位
     */
    public void setStatus(String key, String status, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, status, timeout, unit);
    }
    
    /**
     * 仅当键不存在时设置值（实现分布式锁）
     * @param key 键
     * @param value 值
     * @param timeout 过期时间
     * @param unit 时间单位
     * @return 是否设置成功
     */
    public boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit));
    }

    /**
     * 获取List指定范围元素
     * @param key 键
     * @param start 开始索引
     * @param end 结束索引
     * @return 元素列表
     */
    public <T> List<T> listRange(String key, long start, long end) {
        return (List<T>) redisTemplate.opsForList().range(key, start, end);
    }
} 