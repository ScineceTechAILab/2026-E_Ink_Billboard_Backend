package com.stalab.e_ink_billboard_backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan("com.stalab.e_ink_billboard_backend.mapper")
public class EInkBillboardBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EInkBillboardBackendApplication.class, args);
    }

}
