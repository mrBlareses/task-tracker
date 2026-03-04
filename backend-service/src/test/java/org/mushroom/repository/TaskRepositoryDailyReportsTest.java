
package org.mushroom.repository;

import org.mushroom.config.PostgresTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mushroom.enums.RoleType;
import org.mushroom.entity.Role;
import org.mushroom.enums.TaskStatus;
import org.mushroom.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.mushroom.entity.Task;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TaskRepositoryDailyReportsTest extends PostgresTestContainer {

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private UserRepository userRepository;

    private UUID userAId;
    private UUID userBId;

    @BeforeEach
    void init() {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        User userA = createAndSaveUser("a@test.com");
        User userB = createAndSaveUser("b@test.com");
        userAId = userA.getId();
        userBId = userB.getId();
    }

    @Test
    @DisplayName("корректно подсчитывает pending и completed за текущую дату")
    void shouldReturnDailyReportsWithCorrectPendingCompleted() {
        ZoneOffset offset = ZoneOffset.UTC;
        OffsetDateTime todayStart = OffsetDateTime.now(offset).toLocalDate().atStartOfDay().atOffset(offset);
        OffsetDateTime yesterday = todayStart.minusDays(1);

        createAndSaveTask("A todo", userAId, TaskStatus.TODO, yesterday.plusHours(1));
        createAndSaveTask("A in progress", userAId, TaskStatus.IN_PROGRESS, yesterday.plusHours(2));
        createAndSaveTask("A done today", userAId, TaskStatus.DONE, todayStart.plusHours(3));
        createAndSaveTask("A done yesterday", userAId, TaskStatus.DONE, yesterday.plusHours(3));

        createAndSaveTask("B done 1", userBId, TaskStatus.DONE, todayStart.plusHours(4));
        createAndSaveTask("B done 2", userBId, TaskStatus.DONE, todayStart.plusHours(5));

        var rows = taskRepository.getDailyReports();

        var a = rows.stream().filter(r -> r.getEmail().equals("a@test.com")).findFirst().orElseThrow();
        var b = rows.stream().filter(r -> r.getEmail().equals("b@test.com")).findFirst().orElseThrow();

        assertEquals(2L, a.getPendingCount());
        assertEquals(1L, a.getCompletedCount());

        assertEquals(0L, b.getPendingCount());
        assertEquals(2L, b.getCompletedCount());
    }

    private User createAndSaveUser(String email) {
        User user = User.builder()
                .email(email)
                .password("123456")
                .firstname("Test")
                .lastname("User")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        Role role = Role.builder()
                .name(RoleType.USER)
                .user(user)
                .build();

        user.getRoles().add(role);

        return userRepository.saveAndFlush(user);
    }

    private void createAndSaveTask(String title, UUID ownerId, TaskStatus status, OffsetDateTime updatedAt) {
        User ownerRef = userRepository.getReferenceById(ownerId);

        Task task = new Task();
        task.setTitle(title);
        task.setDescription("desc");
        task.setStatus(status);
        task.setUser(ownerRef);
        task.setCreatedAt(updatedAt.minusMinutes(10));
        task.setUpdatedAt(updatedAt);

        taskRepository.save(task);
    }
}
