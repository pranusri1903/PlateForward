package com.plateforward.auth;

import com.plateforward.user.User;
import com.plateforward.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first admin account on startup (credentials come from ADMIN_EMAIL / ADMIN_PASSWORD). */
@Component
@RequiredArgsConstructor
class AdminSeeder implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    @Value("${app.admin.email}")
    private String email;
    @Value("${app.admin.password}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (users.existsByRole(User.Role.ADMIN)) return;
        var admin = new User();
        admin.setEmail(email);
        admin.setPasswordHash(encoder.encode(password));
        admin.setName("PlateForward Admin");
        admin.setRole(User.Role.ADMIN);
        users.save(admin);
    }
}
