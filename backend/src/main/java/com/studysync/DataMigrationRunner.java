package com.studysync;

import com.studysync.model.StudyMaterial;
import com.studysync.model.User;
import com.studysync.repository.StudyMaterialRepository;
import com.studysync.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class DataMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataMigrationRunner.class);
    private static final String LEGACY_USER_EMAIL = "legacy@studysync.internal";

    private final StudyMaterialRepository studyMaterialRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataMigrationRunner(
            StudyMaterialRepository studyMaterialRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.studyMaterialRepository = studyMaterialRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<StudyMaterial> unassignedMaterials = studyMaterialRepository.findByUserIsNull();
        if (unassignedMaterials.isEmpty()) {
            return;
        }

        log.info("Found {} existing study material records without an owner. Starting migration...",
                unassignedMaterials.size());

        User legacyUser = userRepository.findByEmailIgnoreCase(LEGACY_USER_EMAIL)
                .orElseGet(() -> {
                    String unguessablePassword = passwordEncoder.encode(UUID.randomUUID().toString());
                    User newUser = new User(LEGACY_USER_EMAIL, unguessablePassword, "Legacy System User");
                    return userRepository.save(newUser);
                });

        for (StudyMaterial material : unassignedMaterials) {
            material.setUser(legacyUser);
        }

        studyMaterialRepository.saveAll(unassignedMaterials);
        log.info("Data migration complete: Assigned {} orphaned study material records to legacy user (ID: {}).",
                unassignedMaterials.size(), legacyUser.getId());
    }
}
