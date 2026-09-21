package br.com.filasaude;

import br.com.filasaude.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(JwtProperties.class)
public class FilaSaudeApplication {

    public static void main(String[] args) {
        SpringApplication.run(FilaSaudeApplication.class, args);
    }
}
