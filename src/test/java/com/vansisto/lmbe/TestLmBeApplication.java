package com.vansisto.lmbe;

import org.springframework.boot.SpringApplication;

public class TestLmBeApplication {

    public static void main(String[] args) {
        SpringApplication.from(LmBeApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
