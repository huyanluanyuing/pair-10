package ca.en.solution.common;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import tools.jackson.databind.json.JsonMapper;

@Configuration
public class SeedDataConfig {

    @Bean
    SeedData seedData(JsonMapper jsonMapper) throws IOException {
        try (InputStream seed = new ClassPathResource("seed.json").getInputStream()) {
            return jsonMapper.readValue(seed, SeedData.class);
        }
    }

}
