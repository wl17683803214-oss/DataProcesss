package com.example.simulator;

import com.example.simulator.config.SimulatorProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 独立模拟源服务入口。 */
@SpringBootApplication
@MapperScan("com.example.simulator.mapper")
@EnableScheduling
@EnableConfigurationProperties(SimulatorProperties.class)
public class SimulatorApplication {
    public static void main(String[] args) {
        // 启动独立的管理接口和发送任务管理器。
        SpringApplication.run(SimulatorApplication.class, args);
    }
}
