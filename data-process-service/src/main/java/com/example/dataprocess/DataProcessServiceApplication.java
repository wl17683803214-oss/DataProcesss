package com.example.dataprocess;
import com.example.common.appstore.AppStoreReportConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableScheduling;
/** 具体数据处理业务服务启动入口。 */
@EnableDiscoveryClient
@EnableScheduling
@MapperScan("com.example.dataprocess.mapper")
@SpringBootApplication
@Import(AppStoreReportConfiguration.class)
public class DataProcessServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataProcessServiceApplication.class, args);
    }
}
