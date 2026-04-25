package com.s2886810.jewelagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class JewelAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(JewelAgentApplication.class, args);
    }

}
