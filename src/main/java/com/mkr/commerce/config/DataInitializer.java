package com.mkr.commerce.config;

import com.mkr.commerce.user.entity.User;
import com.mkr.commerce.user.enums.UserRole;
import com.mkr.commerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs once at startup.
 *
 * 1. Creates the employee_id_seq PostgreSQL sequence if it doesn't exist.
 *    All staff employee IDs are drawn from this sequence (1001, 1002, …).
 *
 * 2. Seeds the first SUPER_ADMIN from env/properties if no users exist yet.
 *    This is the only way to bootstrap the system — there is no public
 *    registration endpoint.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate    jdbcTemplate;

    @Value("${app.seed.super-admin.email}")
    private String superAdminEmail;

    @Value("${app.seed.super-admin.password}")
    private String superAdminPassword;

    @Value("${app.seed.super-admin.name}")
    private String superAdminName;       // "Rajesh Kumar" — split into first/last

    @Value("${app.seed.super-admin.mobile:9999999999}")
    private String superAdminMobile;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureEmployeeIdSequence();
        seedSuperAdmin();
    }

    // ── Step 1: PostgreSQL sequence ───────────────────────────────────────────

    private void ensureEmployeeIdSequence() {
        jdbcTemplate.execute(
            "CREATE SEQUENCE IF NOT EXISTS employee_id_seq START WITH 1001 INCREMENT BY 1 NO CYCLE"
        );
        log.info("DataInitializer: employee_id_seq ready.");
    }

    // ── Step 2: First super admin ─────────────────────────────────────────────

    private void seedSuperAdmin() {
        if (userRepository.existsByEmail(superAdminEmail.toLowerCase())) {
            log.info("DataInitializer: super admin already exists, skipping seed.");
            return;
        }

        // Split "Rajesh Kumar" → firstName="Rajesh" lastName="Kumar"
        String[] parts     = superAdminName.trim().split("\\s+", 2);
        String   firstName = parts[0];
        String   lastName  = parts.length > 1 ? parts[1] : parts[0];

        Long empId = jdbcTemplate.queryForObject("SELECT nextval('employee_id_seq')", Long.class);

        User superAdmin = User.builder()
                .employeeId(empId)
                .firstName(firstName)
                .lastName(lastName)
                .email(superAdminEmail.toLowerCase())
                .passwordHash(passwordEncoder.encode(superAdminPassword))
                .role(UserRole.SUPER_ADMIN)
                .department("Management")
                .mobileNumber(superAdminMobile)
                .isActive(true)
                .build();
        superAdmin.composeName();

        userRepository.save(superAdmin);
        log.info("DataInitializer: super admin seeded → {} [EMP-{}]", superAdminEmail, empId);
    }
}
