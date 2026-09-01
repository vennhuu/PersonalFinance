package com.vennhuu.PersonalFinance.Config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vennhuu.PersonalFinance.Entity.Response.Summary.ResCategoryStat;

import com.vennhuu.PersonalFinance.Entity.Response.Summary.ResSummary;
import com.vennhuu.PersonalFinance.Entity.Response.Summary.ResTrendPoint;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(baseConfig())
                .withCacheConfiguration("reportSummary", configFor(ResSummary.class))
                .withCacheConfiguration("reportByCategory", configForList(ResCategoryStat.class))
                .withCacheConfiguration("reportTrend", configForList(ResTrendPoint.class))
                .build();
    }

    // Cấu hình mặc định: TTL 10 phút, không cache giá trị null
    private RedisCacheConfiguration baseConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues();
    }

    // Dùng cho cache trả về 1 object đơn
    private RedisCacheConfiguration configFor(Class<?> type) {
        return baseConfig().serializeValuesWith(serializerFor(mapper.constructType(type)));
    }

    // Dùng cho cache trả về List
    private RedisCacheConfiguration configForList(Class<?> itemType) {
        JavaType listType = mapper.getTypeFactory().constructCollectionType(List.class, itemType);
        return baseConfig().serializeValuesWith(serializerFor(listType));
    }

    private RedisSerializationContext.SerializationPair<Object> serializerFor(JavaType type) {
        return RedisSerializationContext.SerializationPair
                .fromSerializer(new Jackson2JsonRedisSerializer<>(mapper, type));
    }
}