package com.mawai.mrcrawler.config;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class WebClientConfig {

    // 浏览器标识和版本
    private static final String[] BROWSERS = {
            "Chrome/91.0.4472.124",
            "Chrome/92.0.4515.159",
            "Chrome/93.0.4577.82",
            "Chrome/94.0.4606.81",
            "Firefox/89.0",
            "Firefox/90.0",
            "Firefox/91.0",
            "Firefox/92.0",
            "Safari/605.1.15",
            "Safari/604.1",
            "Edge/91.0.864.59",
            "Edge/92.0.902.78"
    };
    
    // 操作系统信息
    private static final String[] OS_INFO = {
            "Windows NT 10.0; Win64; x64",
            "Windows NT 6.1; Win64; x64",
            "Windows NT 6.3; Win64; x64",
            "Macintosh; Intel Mac OS X 10_15_7",
            "Macintosh; Intel Mac OS X 11_4_0",
            "X11; Linux x86_64",
            "X11; Ubuntu; Linux x86_64",
            "X11; Fedora; Linux x86_64",
            "iPhone; CPU iPhone OS 14_6 like Mac OS X",
            "iPad; CPU OS 14_6 like Mac OS X",
            "Linux; Android 11; SM-G991B",
            "Linux; Android 10; Mi 9T Pro"
    };
    
    // 渲染引擎版本
    private static final String[] WEBKIT_VERSIONS = {
            "537.36",
            "537.43",
            "605.1.15",
            "602.1.50"
    };
    
    private static final String[] GECKO_VERSIONS = {
            "20100101",
            "20100101"
    };
    
    // 保留几个完整的UA作为后备
    private static final String[] COMPLETE_UA = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Safari/605.1.15",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1"
    };
    
    @Autowired
    private CrawlerConfig crawlerConfig;
    
    private final Random random = new Random();

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new UserAgentInterceptor())
                .addInterceptor(new DelayInterceptor())
                .build();
    }
    
    /**
     * 用户代理拦截器 - 随机切换用户代理
     */
    private class UserAgentInterceptor implements Interceptor {
        @NotNull
        @Override
        public Response intercept(Chain chain) throws IOException {
            String userAgent = getRandomUserAgent();
            log.info("User-Agent: {}", userAgent);
            Request request = chain.request().newBuilder()
                    .header("User-Agent", userAgent)
                    .build();
            return chain.proceed(request);
        }
    }
    
    /**
     * 延迟拦截器 - 添加随机延迟，控制爬取频率
     */
    private class DelayInterceptor implements Interceptor {
        @NotNull
        @Override
        public Response intercept(Chain chain) throws IOException {
            try {
                // 随机延迟
                int delay = crawlerConfig.getRandomDelay();
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return chain.proceed(chain.request());
        }
    }

    /**
     * 生成随机UserAgent
     * 有5%的概率使用完整的UA，95%的概率组合生成
     */
    private String getRandomUserAgent() {
        // 有5%的概率使用完整的UA
        if (random.nextInt(100) < 5) {
            return COMPLETE_UA[random.nextInt(COMPLETE_UA.length)];
        }
        
        // 组合生成UserAgent
        String browser = BROWSERS[random.nextInt(BROWSERS.length)];
        String osInfo = OS_INFO[random.nextInt(OS_INFO.length)];
        
        // 针对不同的浏览器和操作系统，选择合适的附加信息和格式
        StringBuilder userAgent = new StringBuilder("Mozilla/5.0 (");
        userAgent.append(osInfo);
        userAgent.append(") ");
        
        // 根据浏览器类型添加不同的额外信息和引擎
        if (browser.startsWith("Chrome") || browser.startsWith("Edge") || browser.startsWith("Safari")) {
            // 为Chrome、Edge和Safari选择一个WebKit版本
            String webkitVersion = WEBKIT_VERSIONS[random.nextInt(WEBKIT_VERSIONS.length)];
            userAgent.append("AppleWebKit/").append(webkitVersion).append(" (KHTML, like Gecko) ");
            
            if (browser.startsWith("Safari")) {
                // Safari浏览器特殊处理
                if (osInfo.contains("iPhone") || osInfo.contains("iPad")) {
                    userAgent.append("Version/14.0 Mobile/15E148 ");
                } else {
                    userAgent.append("Version/14.1.1 ");
                }
            }
            
            userAgent.append(browser);
            
            // 对移动设备添加Mobile标识
            if (browser.startsWith("Chrome") || browser.startsWith("Edge")) {
                if (osInfo.contains("Android") || osInfo.contains("iPhone") || osInfo.contains("iPad")) {
                    userAgent.append(" Mobile Safari/").append(webkitVersion);
                } else {
                    userAgent.append(" Safari/").append(webkitVersion);
                }
            }
        } else if (browser.startsWith("Firefox")) {
            // Firefox的UA结构特殊，使用rv:版本号和Gecko引擎
            String version = browser.split("/")[1];
            String geckoVersion = GECKO_VERSIONS[random.nextInt(GECKO_VERSIONS.length)];
            
            userAgent.append("rv:");
            userAgent.append(version);
            userAgent.append(") Gecko/").append(geckoVersion).append(" ");
            userAgent.append(browser);
        }
        
        return userAgent.toString();
    }
}
