package com.mawai.mrcrawler;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = {"com.mawai.mrcrawler", "com.mawai.mrcommon", "com.mawai.mrmbplus"})
@MapperScan(basePackages = {"com.mawai.mrmbplus.dao"})
public class MrCrawlerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MrCrawlerApplication.class, args);
	}

}
