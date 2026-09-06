package com.group.library_app.config;

import com.group.library_app.repository.user.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class UserConfiguration {
    @Bean
    public UserRepository userRepository(JdbcTemplate jdbcTemplate){
        return new UserRepository(jdbcTemplate);
    }
}
