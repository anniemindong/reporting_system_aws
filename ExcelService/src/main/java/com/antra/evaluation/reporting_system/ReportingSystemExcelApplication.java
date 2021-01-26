package com.antra.evaluation.reporting_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class ReportingSystemExcelApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportingSystemExcelApplication.class, args);
    }

}
