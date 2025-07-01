package com.mawai.mrmbplus;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;

public class MyBatisPlusCodeGenerator {
    public static void main(String[] args) {

        FastAutoGenerator.create(
                        "jdbc:mysql://localhost:3306/magic_recipe?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai",
                        "root",
                        "733806")
                .globalConfig(builder -> {
                    builder.author("mawai") // 设置作者
                            .commentDate("yyyy-MM-dd")
                            .enableSpringdoc() // 开启 springDoc 模式
                            .outputDir("mr-mbPlus\\src\\main\\java");// 指定输出目录
                })
                .packageConfig(builder -> {
                    builder.parent("com.mawai") // 设置父包名
                            .moduleName("mrmbplus") // 设置父包模块名
                            .entity("model")
                            .service("service")
                            .serviceImpl("service.impl")
                            .controller("controller")
                            .mapper("dao")
                            .xml("mappers")
                            .pathInfo(Collections.singletonMap(OutputFile.xml, "mr-mbPlus\\src\\main\\resources\\mappers"));
                })
                .strategyConfig(builder -> {
                    builder.addTablePrefix("t_", "c_")
                            .entityBuilder()
                            .enableLombok()
                            .enableChainModel()
                            .enableLombok()
                            .enableFileOverride()
                            .enableTableFieldAnnotation()
                            .idType(IdType.AUTO)
                            .serviceBuilder()
                            .formatServiceFileName("%sService")
                            .formatServiceImplFileName("%sServiceImpl")
                            .enableFileOverride()
                            .controllerBuilder()
                            .enableFileOverride()
                            .enableRestStyle()
                            .formatFileName("%sController")
                            .mapperBuilder()
                            .formatMapperFileName("%sMapper")
                            .formatXmlFileName("%sMapper")
                            .mapperAnnotation(Mapper.class)
                            .enableFileOverride();

                })
                .execute();
    }
}
