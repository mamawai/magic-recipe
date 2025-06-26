package com.mawai.mrcrawler.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "xiachufang")
public class SiteConfig {

    private String baseUrl = "https://www.xiachufang.com";

}
