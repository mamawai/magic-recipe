package com.mawai.mrcrawler.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

@Configuration
@ConfigurationProperties(prefix = "crawler")
public class CrawlerConfig {

    // 爬取延迟配置（毫秒）
    @Setter
    @Getter
    private int minDelay = 1000;
    @Setter
    @Getter
    private int maxDelay = 5000;
    
    private final Random random = new Random();
    
    /**
     * 获取随机延迟时间
     * @return 延迟时间（毫秒）
     */
    public int getRandomDelay() {
        return minDelay + random.nextInt(maxDelay - minDelay + 1);
    }

}