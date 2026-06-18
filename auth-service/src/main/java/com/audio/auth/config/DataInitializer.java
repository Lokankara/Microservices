package com.audio.auth.config;

import com.audio.auth.entity.User;
import com.audio.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
@Profile("dev")
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                User alice = new User();
                alice.setUsername("alice");
                alice.setPassword(passwordEncoder.encode("alice"));
                alice.setRoles(Set.of("USER"));
                alice.setEnabled(true);
                userRepository.save(alice);

                User bob = new User();
                bob.setUsername("bob");
                bob.setPassword(passwordEncoder.encode("bob"));
                bob.setRoles(Set.of("ADMIN"));
                bob.setEnabled(true);
                userRepository.save(bob);
            }
        };
    }
}
