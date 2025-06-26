//package com.mawai.mrcrawler.service.proxy;
//
//import com.mawai.mrcrawler.model.Proxy;
//import com.mawai.mrcrawler.model.ProxyResponse;
//import com.mawai.mrcrawler.service.cache.CacheService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.client.reactive.ReactorClientHttpConnector;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.core.publisher.Mono;
//import reactor.netty.http.client.HttpClient;
//import reactor.netty.transport.ProxyProvider;
//import reactor.util.retry.Retry;
//import io.netty.handler.ssl.SslContext;
//import io.netty.handler.ssl.SslContextBuilder;
//import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
//import io.netty.channel.ChannelOption;
//
//import java.time.Duration;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Objects;
//import java.util.concurrent.ThreadLocalRandom;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Service
//public class ProxyService {
//
//    private final WebClient webClient;
//    private final CacheService cacheService;
//    private static final String PROXY_CACHE_KEY = "crawler:proxy:list";
//
//    @Value("${proxy.api.url:https://proxy.scdn.io/api/get_proxy.php}")
//    private String proxyApiUrl;
//
//    @Value("${proxy.request.timeout:5}")
//    private int requestTimeout;
//
//    @Value("${proxy.max.retries:3}")
//    private int maxRetries;
//
//    @Value("${proxy.retry.delay:1}")
//    private int retryDelay;
//
//    @Autowired
//    public ProxyService(CacheService cacheService) {
//        this.cacheService = cacheService;
//
//        HttpClient httpClient = HttpClient.create()
//                .responseTimeout(Duration.ofSeconds(requestTimeout));
//
//        this.webClient = WebClient.builder()
//                .clientConnector(new ReactorClientHttpConnector(httpClient))
//                .build();
//    }
//
//    /**
//     * 从API获取新的代理IP列表
//     * @param protocol 代理协议类型：http, https, socks4, socks5, all
//     * @param count 获取代理数量
//     * @return 代理对象列表
//     */
//    public List<Proxy> fetchProxies(String protocol, int count, String country) {
//        try {
//            String url = String.format(
//                    "%s?protocol=%s&count=%d&country=%s",
//                    proxyApiUrl, protocol, count, country);
//
//            List<Proxy> proxies = webClient.get()
//                    .uri(url)
//                    .retrieve()
//                    .bodyToMono(ProxyResponse.class)
//                    // 添加重试策略
//                    .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(retryDelay))
//                            .doBeforeRetry(retrySignal ->
//                                log.info("Retrying proxy fetch attempt: {} due to: {}",
//                                        retrySignal.totalRetries() + 1,
//                                        retrySignal.failure().getMessage()))
//                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
//                                log.error("Failed to fetch proxies after {} attempts", maxRetries);
//                                return retrySignal.failure();
//                            }))
//                    .map(response -> {
//                        if (response != null && response.getCode() == 200 && response.getData() != null) {
//                            List<String> proxyStrings = response.getData().getProxies();
//
//                            if (proxyStrings != null && !proxyStrings.isEmpty()) {
//                                return proxyStrings.stream()
//                                        .map(str -> {
//                                            try {
//                                                return Proxy.fromString(str);
//                                            } catch (Exception e) {
//                                                log.warn("Failed to parse proxy: {}", str);
//                                            }
//                                            return null;
//                                        })
//                                        .filter(Objects::nonNull)
//                                        .collect(Collectors.toList());
//                            }
//                        }
//                        return new ArrayList<Proxy>();
//                    })
//                    .onErrorResume(e -> {
//                        log.error("Failed to fetch proxies: {}", e.getMessage());
//                        return Mono.just(new ArrayList<>());
//                    })
//                    .block();
//
//            // 缓存到Redis
//            if (proxies != null && !proxies.isEmpty()) {
//                try {
//                    // 先清除旧数据
//                    cacheService.delete(PROXY_CACHE_KEY);
//                    // 添加新数据
//                    cacheService.rightPushAll(PROXY_CACHE_KEY, proxies.toArray());
//                    // 设置过期时间，1小时后过期
//                    cacheService.expire(PROXY_CACHE_KEY, Duration.ofHours(1));
//
//                    log.info("Cached {} proxies to Redis", proxies.size());
//                } catch (Exception e) {
//                    log.error("Failed to cache proxies to Redis", e);
//                }
//
//                return proxies;
//            }
//        } catch (Exception e) {
//            log.error("Exception in fetchProxies", e);
//        }
//
//        return new ArrayList<>();
//    }
//
//    /**
//     * 获取随机代理
//     * @return 代理对象
//     */
//    public Proxy getRandomProxy() {
//        try {
//            // 从Redis获取代理
//            Long size = cacheService.listSize(PROXY_CACHE_KEY);
//
//            if (size == null || size == 0) {
//                // Redis中没有代理，重新获取
//                List<Proxy> proxies = fetchProxies("https", 5, "中国");
//                if (proxies != null && !proxies.isEmpty()) {
//                    log.info("Fetched {} proxies from API", proxies);
//                    return proxies.get(ThreadLocalRandom.current().nextInt(proxies.size()));
//                }
//                return null;
//            }
//
//            int index = ThreadLocalRandom.current().nextInt(size.intValue());
//            Object proxy = cacheService.listIndex(PROXY_CACHE_KEY, index);
//            return proxy != null ? (Proxy) proxy : null;
//        } catch (Exception e) {
//            log.error("Error in getRandomProxy", e);
//            return null;
//        }
//    }
//
//    /**
//     * 获取配置了代理的WebClient
//     * @param forceNoProxy 是否强制不使用代理
//     * @return 配置了代理的WebClient
//     */
//    public WebClient getProxyWebClient(boolean forceNoProxy) {
//        if (forceNoProxy) {
//            log.info("强制不使用代理，创建直连WebClient");
//            return createDirectWebClient();
//        }
//
//        Proxy proxy = getRandomProxy();
//        if (proxy == null) {
//            log.warn("无可用代理，返回直连WebClient");
//            return createDirectWebClient();
//        }
//
//        try {
//            log.info("创建带代理的WebClient: {}:{}", proxy.getHost(), proxy.getPort());
//
//            // 创建SSL上下文，信任所有证书
//            SslContext sslContext = SslContextBuilder
//                .forClient()
//                .trustManager(InsecureTrustManagerFactory.INSTANCE)
//                .build();
//
//            // 尝试不同的代理类型，可以根据实际情况调整
//            // 可以使用配置或者轮询方式尝试不同的代理类型
//            ProxyProvider.Proxy[] proxyTypes = {
//                ProxyProvider.Proxy.SOCKS5,
//                ProxyProvider.Proxy.HTTP
//            };
//
//            // 根据Host的哈希值选择代理类型，使得同一Host总是使用相同类型的代理
//            int index = Math.abs(proxy.getHost().hashCode() % proxyTypes.length);
//            ProxyProvider.Proxy proxyType = proxyTypes[index];
//
//            log.info("对主机 {} 使用代理类型: {}", proxy.getHost(), proxyType);
//
//            // 创建带SSL和代理支持的HTTP客户端
//            HttpClient httpClient = HttpClient.create()
//                    .secure(sslSpec -> sslSpec.sslContext(sslContext)
//                        // 设置SSL/TLS超时时间
//                        .handshakeTimeout(Duration.ofSeconds(30))
//                        .closeNotifyFlushTimeout(Duration.ofSeconds(10))
//                        .closeNotifyReadTimeout(Duration.ofSeconds(10))
//                    )
//                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)  // 增加连接超时时间
//                    .option(ChannelOption.SO_KEEPALIVE, true)
//                    .proxy(spec -> spec
//                            .type(proxyType)
//                            .host(proxy.getHost())
//                            .port(proxy.getPort())
//                            .connectTimeoutMillis(30000))
//                    .responseTimeout(Duration.ofSeconds(45));  // 增加响应超时时间
//
//            return WebClient.builder()
//                    .clientConnector(new ReactorClientHttpConnector(httpClient))
//                    .defaultHeader("User-Agent", getRandomUserAgent())
//                    // 添加更多HTTP头以更好地伪装浏览器
//                    .defaultHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
//                    .defaultHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
//                    .defaultHeader("Accept-Encoding", "gzip, deflate, br")
//                    .defaultHeader("Connection", "keep-alive")
//                    .defaultHeader("Upgrade-Insecure-Requests", "1")
//                    .defaultHeader("Cache-Control", "max-age=0")
//                    .build();
//        } catch (Exception e) {
//            log.error("创建代理WebClient时出错", e);
//            return createDirectWebClient();
//        }
//    }
//
//    /**
//     * 获取配置了代理的WebClient（默认使用代理）
//     * @return 配置了代理的WebClient
//     */
//    public WebClient getProxyWebClient() {
//        return getProxyWebClient(false);
//    }
//
//    /**
//     * 创建直接连接的WebClient（不使用代理）
//     * @return 直连的WebClient
//     */
//    private WebClient createDirectWebClient() {
//        try {
//            // 创建SSL上下文，信任所有证书
//            SslContext sslContext = SslContextBuilder
//                .forClient()
//                .trustManager(InsecureTrustManagerFactory.INSTANCE)
//                .build();
//
//            HttpClient httpClient = HttpClient.create()
//                .secure(sslSpec -> sslSpec.sslContext(sslContext)
//                    // 设置SSL/TLS超时时间
//                    .handshakeTimeout(Duration.ofSeconds(30))
//                    .closeNotifyFlushTimeout(Duration.ofSeconds(10))
//                    .closeNotifyReadTimeout(Duration.ofSeconds(10))
//                )
//                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
//                .option(ChannelOption.SO_KEEPALIVE, true)
//                .responseTimeout(Duration.ofSeconds(45));
//
//            return WebClient.builder()
//                .clientConnector(new ReactorClientHttpConnector(httpClient))
//                .defaultHeader("User-Agent", getRandomUserAgent())
//                .defaultHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
//                .defaultHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
//                .defaultHeader("Accept-Encoding", "gzip, deflate, br")
//                .defaultHeader("Connection", "keep-alive")
//                .defaultHeader("Upgrade-Insecure-Requests", "1")
//                .defaultHeader("Cache-Control", "max-age=0")
//                .build();
//        } catch (Exception e) {
//            log.error("创建直连WebClient时出错", e);
//            return WebClient.create();
//        }
//    }
//
//    /**
//     * 获取随机User-Agent
//     * @return 随机User-Agent字符串
//     */
//    public String getRandomUserAgent() {
//        String[] userAgents = {
//            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
//            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.107 Safari/537.36",
//            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Safari/605.1.15",
//            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36",
//            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.107 Safari/537.36",
//            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:90.0) Gecko/20100101 Firefox/90.0",
//            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0",
//            "Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1",
//            "Mozilla/5.0 (iPhone; CPU iPhone OS 14_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1 Mobile/15E148 Safari/604.1",
//            "Mozilla/5.0 (iPad; CPU OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1"
//        };
//        return userAgents[ThreadLocalRandom.current().nextInt(userAgents.length)];
//    }
//}