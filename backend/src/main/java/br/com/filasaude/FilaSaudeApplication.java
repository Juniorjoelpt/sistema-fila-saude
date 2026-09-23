package br.com.filasaude;

import br.com.filasaude.security.JwtProperties;
import br.com.filasaude.security.SuperadminProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties({JwtProperties.class, SuperadminProperties.class})
public class FilaSaudeApplication {

    public static void main(String[] args) {
        SpringApplication.run(FilaSaudeApplication.class, args);
    }
}
