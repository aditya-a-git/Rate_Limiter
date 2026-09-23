package com.adi.ratelimiter.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.core.io.ClassPathResource;
import java.util.List;

@Configuration
public class RedisConfig {
    @Bean
    public DefaultRedisScript<List<Long>> rateLimitScript() {
        DefaultRedisScript<List<Long>> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("script.lua"));
        return script;
    }
}
