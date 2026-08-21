package com.foliopath360.lms.config;

import com.foliopath360.lms.entity.Role;
import com.foliopath360.lms.entity.User;
import com.foliopath360.lms.entity.UserStatus;
import com.foliopath360.lms.repository.RoleRepository;
import com.foliopath360.lms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log =
            LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${super-admin.email}")
    private String adminEmail;

    @Value("${super-admin.password}")
    private String adminPassword;

    @Value("${super-admin.first-name}")
    private String adminFirstName;

    @Value("${super-admin.last-name}")
    private String adminLastName;

    public DataInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {

        createRoleIfNotExists("SUPER_ADMIN", "Super Administrator with full system access");
        createRoleIfNotExists("STAFF", "Staff member with course and content management access");
        createRoleIfNotExists("STUDENT", "Student with learning and enrollment access");

        createSuperAdminIfNotExists();
    }

    private void createRoleIfNotExists(String roleName, String description) {

        if (roleRepository.findByRoleName(roleName).isEmpty()) {

            Role role = Role.builder()
                    .roleName(roleName)
                    .roleDescription(description)
                    .build();

            roleRepository.save(role);
            log.info("Created {} role", roleName);
        }
    }

    private void createSuperAdminIfNotExists() {

        if (!userRepository.existsByEmail(adminEmail)) {

            Role superAdminRole = roleRepository.findByRoleName("SUPER_ADMIN")
                    .orElseThrow(() -> new RuntimeException("SUPER_ADMIN role not found"));

            User admin = User.builder()
                    .username(adminEmail)
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .firstName(adminFirstName)
                    .lastName(adminLastName)
                    .status(UserStatus.ACTIVE)
                    .enabled(true)
                    .emailVerified(true)
                    .mobileVerified(false)
                    .roles(new HashSet<>(Set.of(superAdminRole)))
                    .build();

            userRepository.save(admin);
            log.info("Created default SUPER_ADMIN account: {}", adminEmail);
        }
    }
}
