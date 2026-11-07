package it.cvmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CvManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CvManagerApplication.class, args);
    }
}
