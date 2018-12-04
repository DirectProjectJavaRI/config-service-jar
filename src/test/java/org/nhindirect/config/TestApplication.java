package org.nhindirect.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"org.nhindirect.config"})
public class TestApplication
{	
    public static void main(String[] args) 
    {
        SpringApplication.run(TestApplication.class, args);
    }  
}
