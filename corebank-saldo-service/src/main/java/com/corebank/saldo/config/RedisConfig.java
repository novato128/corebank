package com.corebank.saldo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

//@Configuration
public class RedisConfig {

//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(
//            RedisConnectionFactory connectionFactory) {
//
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//
//        template.setConnectionFactory(connectionFactory);
//
//        template.setKeySerializer(new StringRedisSerializer());
//        template.setHashKeySerializer(new StringRedisSerializer());
//
//        Jackson2JsonRedisSerializer<Object> serializer =
//                new Jackson2JsonRedisSerializer<>(Object.class);
//
//        template.setValueSerializer(serializer);
//        template.setHashValueSerializer(serializer);
//
//        template.afterPropertiesSet();
//
//        return template;
//    }
}
