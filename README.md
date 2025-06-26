# Magic Recipe（魔法食谱）--- 开发中

## 项目介绍

Magic Recipe是一个食谱采集和管理系统，主要功能是从网络上自动抓取食谱数据，并提供数据管理和查询功能。该系统采用Java开发，基于Spring Boot框架构建，是一个多模块Maven项目。

## 项目架构

项目由三个主要模块组成：

### 1. mr-admin（管理模块）
- 提供系统管理功能
- 包含数据展示和操作界面

### 2. mr-common（公共模块）
- 包含通用工具类和配置
- 提供Swagger文档配置
- 包含异常处理和日志切面

### 3. mr-crawler（爬虫模块）
- 核心爬虫功能实现
- 负责抓取各类食谱网站数据
- 包含分类爬取、搜索结果爬取和食谱内容爬取
- 提供数据解析和缓存服务

## 技术栈

- 后端框架：Spring Boot
- 构建工具：Maven
- 爬虫技术：WebClient
- 缓存：Redis
- API文档：Swagger
- 代理IP池支持（待完善）

## 核心功能

1. **分类爬取**：抓取食谱分类信息
2. **食谱列表爬取**：根据分类抓取食谱列表
3. **食谱内容爬取**：抓取完整食谱内容，包括材料和步骤
4. **搜索爬取**：根据关键词搜索并抓取食谱
5. **代理IP支持**：支持使用代理IP进行爬取，避免被屏蔽
6. **数据缓存**：使用Redis缓存爬取结果

## 快速开始

### 环境要求

- JDK 11或以上
- Maven 3.6或以上
- Redis服务器

### 构建项目

```bash
# 克隆项目
git clone https://github.com/mamawai/magic-recipe.git

# 进入项目目录
cd magic-recipe

# 编译项目
mvn clean install
```

### 运行项目

```bash
# 运行爬虫模块
cd mr-crawler
mvn spring-boot:run

# 运行管理模块
cd ../mr-admin
mvn spring-boot:run
```

### 配置说明

各模块的配置文件位于`src/main/resources/application.yml`，可根据需要进行修改。

## API文档

启动项目后，可通过以下地址访问Swagger API文档：

- 爬虫模块API：http://localhost:8082/swagger-ui.html
## 开发指南

### 添加新的爬虫

1. 在`service/crawler/impl`下创建新的爬虫实现类
2. 在`service/parser/impl`下创建对应的解析器
3. 在`SpiderServiceImpl`中注册新的爬虫

### 自定义配置

可在`config`包下修改或添加配置类，如爬虫配置、Redis配置等。

## 联系方式

如有任何问题或建议，请联系项目维护者。

## 许可证

[待定] 
