package com.trade;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 贸易数据管理系统主启动类
 * 
 * @SpringBootApplication: Spring Boot 核心注解，标识这是一个 Spring Boot 应用
 * @MapperScan: 扫描 MyBatis Mapper 接口所在的包
 */
@SpringBootApplication
@MapperScan("com.trade.mapper")
@MapperScan("com.trade.ai.rag")
public class TradeApplication {
    
    /**
     * 应用程序入口方法
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(TradeApplication.class, args);
    }
}
