package com.example.dataadmin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 系统管理服务启动入口，负责登录、权限和用户管理。 */
@MapperScan("com.example.dataadmin.mapper")
@EnableDiscoveryClient
@EnableScheduling
@SpringBootApplication
public class DataAdminServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataAdminServiceApplication.class, args);
    }
}
