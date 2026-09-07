package com.vennhuu.PersonalFinance.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vennhuu.PersonalFinance.Entity.Role;
import com.vennhuu.PersonalFinance.Entity.User;
import com.vennhuu.PersonalFinance.Enum.RoleName;
import com.vennhuu.PersonalFinance.Repository.RoleRepository;
import com.vennhuu.PersonalFinance.Repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInit {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository ;

    @Value("${spring.mail.username}")
    private String defaultUsername;

    @Value("${app.default-admin.password}")
    private String defaultPassword;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeDatabase() {
        log.info("Bắt đầu kiểm tra dữ liệu khởi tạo...");

        Role adminRoleTarget = null;

        if (roleRepository.count() == 0) {
            log.info("Không tìm thấy vai trò nào hết. Tiến hành tạo các vai trò mặc định");

            Role adminRole = new Role();
            adminRole.setName(RoleName.ROLE_ADMIN);
            adminRole.setDescription("Admin thì full quyền");
            adminRoleTarget = roleRepository.save(adminRole);

            Role userRole = new Role();
            userRole.setName(RoleName.ROLE_USER);
            userRole.setDescription("Chỉ có quyền của USER");
            roleRepository.save(userRole);
        } else {
            adminRoleTarget = this.roleRepository.findByName(RoleName.ROLE_ADMIN);
        }

        if (userRepository.count() == 0) {
            log.info("Không tìm thấy tài khoản nào. Tiến hành tạo tài khoản Admin mặc định.");

            if (adminRoleTarget == null) {
                log.error("Không tìm thấy ROLE_ADMIN để gán cho tài khoản mặc định!");
                return;
            }

            User admin = new User();
            admin.setEmail(defaultUsername);
            admin.setFullName("Phan Hữu Phước");
            admin.setPassword(passwordEncoder.encode(defaultPassword));
            admin.setRole(adminRoleTarget);

            userRepository.save(admin);

            log.info("Tài khoản Admin mặc định [{}] đã được tạo thành công!", defaultUsername);
        } else {
            log.info("Hệ thống đã có dữ liệu người dùng. Bỏ qua bước khởi tạo.");
        }
    }
}
