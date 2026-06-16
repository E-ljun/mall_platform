package com.mall.content;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.mall.content.mapper")
public class MallContentApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallContentApplication.class, args);
    }
}
