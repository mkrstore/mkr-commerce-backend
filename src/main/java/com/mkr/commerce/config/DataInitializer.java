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
    private String superAdminName;

    @Value("${app.seed.super-admin.mobile:9999999999}")
    private String superAdminMobile;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureEmployeeIdSequence();
        ensureCustomerNumSequence();
        ensureOrderNumSequence();
        seedSuperAdmin();
    }

    // ── Sequences ─────────────────────────────────────────────────────────────

    private void ensureEmployeeIdSequence() {
        jdbcTemplate.execute(
            "CREATE SEQUENCE IF NOT EXISTS employee_id_seq START WITH 1001 INCREMENT BY 1 NO CYCLE"
        );
        log.info("DataInitializer: employee_id_seq ready.");
    }

    private void ensureCustomerNumSequence() {
        jdbcTemplate.execute(
            "CREATE SEQUENCE IF NOT EXISTS customer_num_seq START WITH 1 INCREMENT BY 1 NO CYCLE"
        );
        log.info("DataInitializer: customer_num_seq ready.");
    }

    private void ensureOrderNumSequence() {
        jdbcTemplate.execute(
            "CREATE SEQUENCE IF NOT EXISTS order_num_seq START WITH 89 INCREMENT BY 1 NO CYCLE"
        );
        log.info("DataInitializer: order_num_seq ready.");
    }

    // ── Super admin ───────────────────────────────────────────────────────────

    private void seedSuperAdmin() {
        if (userRepository.existsByEmail(superAdminEmail.toLowerCase())) {
            log.info("DataInitializer: super admin already exists, skipping seed.");
            return;
        }

        String[] parts     = superAdminName.trim().split("\\s+", 2);
        String   firstName = parts[0];
        String   lastName  = parts.length > 1 ? parts[1] : parts[0];
        Long     empId     = jdbcTemplate.queryForObject("SELECT nextval('employee_id_seq')", Long.class);

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
