package huuthang.config;

import huuthang.entity.Role;
import huuthang.entity.User;
import huuthang.repository.RoleRepository;
import huuthang.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@ConditionalOnProperty(
        name = "app.data-initializer.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class DataInitializer {

    @Bean
    CommandLineRunner initData(
            RoleRepository roles,
            UserRepository users,
            PasswordEncoder encoder,
            @Value("${APP_ADMIN_USERNAME:admin}") String adminUsername,
            @Value("${APP_ADMIN_EMAIL:admin@example.com}") String adminEmail,
            @Value("${APP_ADMIN_PASSWORD:}") String adminPassword) {

        return args -> {
            roles.findByName("ROLE_USER")
                    .orElseGet(() -> roles.save(Role.builder().name("ROLE_USER").build()));

            Role adminRole = roles.findByName("ROLE_ADMIN")
                    .orElseGet(() -> roles.save(Role.builder()
                            .name("ROLE_ADMIN")
                            .build()));

            if (!adminPassword.isBlank()
                    && !"CHANGE_ME".equals(adminPassword)
                    && users.findByUsername(adminUsername).isEmpty()) {
                User admin = User.builder()
                        .username(adminUsername)
                        .email(adminEmail.toLowerCase())
                        .password(encoder.encode(adminPassword))
                        .fullName("System Administrator")
                        .images("/avatar-default.svg")
                        .role(adminRole)
                        .enabled(true)
                        .build();
                users.save(admin);
            }
        };
    }
}
