package com.example.ai_review;

import com.example.ai_review.cli.CliArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class AiReviewApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(AiReviewApplication.class);
        if (CliArguments.isCliMode(args)) {
            application.setWebApplicationType(WebApplicationType.NONE);
            application.setLogStartupInfo(false);
            application.setDefaultProperties(Map.of(
                    "spring.main.banner-mode", "off",
                    "logging.level.root", "ERROR"
            ));
        }
        application.run(args);
    }

}
