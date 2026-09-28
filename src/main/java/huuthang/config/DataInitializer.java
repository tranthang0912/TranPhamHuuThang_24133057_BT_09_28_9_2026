package huuthang.config;

import huuthang.entity.Role;
import huuthang.entity.User;
import huuthang.repository.RoleRepository;
import huuthang.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(
            RoleRepository roles,
            UserRepository users,
            PasswordEncoder encoder) {

        return args -> {
            Role userRole = roles.findByName("ROLE_USER")
                    .orElseGet(() -> roles.save(Role.builder()
                            .name("ROLE_USER")
                            .build()));

            if (users.findByUsername("user01").isEmpty()) {
                User user = User.builder()
                        .username("user01")
                        .email("user01@gmail.com")
                        .password(encoder.encode("123456"))
                        .fullName("Hữu Thắng")
                        .images("/user.svg")
                        .role(userRole)
                        .enabled(true)
                        .build();
                users.save(user);
            }
        };
    }
}
