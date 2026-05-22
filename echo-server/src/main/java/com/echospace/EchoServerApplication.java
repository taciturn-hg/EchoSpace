package com.echospace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// TODO: 等创建第一个 Mapper 接口后，加 @MapperScan("com.echospace.mapper")
@SpringBootApplication
public class EchoServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EchoServerApplication.class, args);
    }

}
