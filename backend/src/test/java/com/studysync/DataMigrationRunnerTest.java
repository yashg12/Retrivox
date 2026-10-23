package com.studysync;

import com.studysync.model.StudyMaterial;
import com.studysync.model.User;
import com.studysync.repository.StudyMaterialRepository;
import com.studysync.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DataMigrationRunnerTest {

    @Mock
    private StudyMaterialRepository studyMaterialRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DataMigrationRunner migrationRunner;

    @BeforeEach
    void setUp() {
        migrationRunner = new DataMigrationRunner(studyMaterialRepository, userRepository, passwordEncoder);
    }

    @Test
    void run_WhenNoUnassignedMaterials_DoesNothing() {
        when(studyMaterialRepository.findByUserIsNull()).thenReturn(Collections.emptyList());

        migrationRunner.run();

        verify(userRepository, never()).findByEmailIgnoreCase(any());
        verify(studyMaterialRepository, never()).saveAll(any());
    }

    @Test
    void run_WhenUnassignedMaterialsExist_AssignsToLegacyUser() {
        StudyMaterial m1 = new StudyMaterial();
        m1.setId(1L);
        m1.setTitle("Old Material 1");

        StudyMaterial m2 = new StudyMaterial();
        m2.setId(2L);
        m2.setTitle("Old Material 2");

        when(studyMaterialRepository.findByUserIsNull()).thenReturn(List.of(m1, m2));
        when(userRepository.findByEmailIgnoreCase("legacy@studysync.internal")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed-random-pw");

        User legacyUser = new User("legacy@studysync.internal", "hashed-random-pw", "Legacy System User");
        legacyUser.setId(999L);
        when(userRepository.save(any(User.class))).thenReturn(legacyUser);

        migrationRunner.run();

        assertEquals(legacyUser, m1.getUser());
        assertEquals(legacyUser, m2.getUser());
        verify(studyMaterialRepository).saveAll(List.of(m1, m2));
    }
}
