package org.mushroom.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mushroom.entity.JobExecutionEntity;
import org.mushroom.entity.enums.JobExecutionStatus;
import org.mushroom.entity.enums.TriggerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class JobExecutionRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cron_service_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.liquibase.enabled", () -> true);
    }

    @Autowired
    private JobExecutionRepository jobExecutionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("deleteByJobNameAndStatusAndStartedAtBefore — удаляет только stale RUNNING executions")
    void deleteByJobNameAndStatusAndStartedAtBefore_ShouldDeleteOnlyMatchingRows() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime threshold = now.minusMinutes(15);

        JobExecutionEntity staleRunning = persistExecution(
                "DAILY_ANALYTICS",
                JobExecutionStatus.RUNNING,
                TriggerType.CRON,
                now.minusMinutes(30)
        );

        JobExecutionEntity freshRunning = persistExecution(
                "DAILY_ANALYTICS",
                JobExecutionStatus.RUNNING,
                TriggerType.CRON,
                now.minusMinutes(5)
        );

        JobExecutionEntity staleSuccess = persistExecution(
                "DAILY_ANALYTICS",
                JobExecutionStatus.SUCCESS,
                TriggerType.CRON,
                now.minusMinutes(30)
        );

        JobExecutionEntity staleOtherJob = persistExecution(
                "ANOTHER_JOB",
                JobExecutionStatus.RUNNING,
                TriggerType.CRON,
                now.minusMinutes(30)
        );

        entityManager.flush();
        entityManager.clear();

        long deleted = jobExecutionRepository.deleteByJobNameAndStatusAndStartedAtBefore(
                "DAILY_ANALYTICS",
                JobExecutionStatus.RUNNING,
                threshold
        );

        entityManager.flush();
        entityManager.clear();

        assertThat(deleted).isEqualTo(1);

        assertThat(jobExecutionRepository.findById(staleRunning.getId())).isEmpty();
        assertThat(jobExecutionRepository.findById(freshRunning.getId())).isPresent();
        assertThat(jobExecutionRepository.findById(staleSuccess.getId())).isPresent();
        assertThat(jobExecutionRepository.findById(staleOtherJob.getId())).isPresent();
    }

    @Test
    @DisplayName("deleteByJobNameAndStatusAndStartedAtBefore — возвращает 0 если подходящих записей нет")
    void deleteByJobNameAndStatusAndStartedAtBefore_ShouldReturnZero_WhenNothingMatches() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime threshold = now.minusMinutes(15);

        persistExecution(
                "DAILY_ANALYTICS",
                JobExecutionStatus.RUNNING,
                TriggerType.CRON,
                now.minusMinutes(5)
        );

        persistExecution(
                "DAILY_ANALYTICS",
                JobExecutionStatus.SUCCESS,
                TriggerType.CRON,
                now.minusMinutes(30)
        );

        entityManager.flush();
        entityManager.clear();

        long deleted = jobExecutionRepository.deleteByJobNameAndStatusAndStartedAtBefore(
                "DAILY_ANALYTICS",
                JobExecutionStatus.RUNNING,
                threshold
        );

        assertThat(deleted).isZero();
    }

    @Test
    @DisplayName("findTop20ByJobNameOrderByStartedAtDesc — возвращает только нужный jobName, в порядке DESC и не больше 20")
    void findTop20ByJobNameOrderByStartedAtDesc_ShouldReturnOrderedAndLimitedResults() {
        OffsetDateTime base = OffsetDateTime.now().minusHours(1);

        for (int i = 0; i < 25; i++) {
            persistExecution(
                    "DAILY_ANALYTICS",
                    JobExecutionStatus.SUCCESS,
                    TriggerType.CRON,
                    base.plusMinutes(i)
            );
        }

        persistExecution(
                "ANOTHER_JOB",
                JobExecutionStatus.SUCCESS,
                TriggerType.CRON,
                base.plusMinutes(100)
        );

        entityManager.flush();
        entityManager.clear();

        List<JobExecutionEntity> result =
                jobExecutionRepository.findTop20ByJobNameOrderByStartedAtDesc("DAILY_ANALYTICS");

        assertThat(result).hasSize(20);
        assertThat(result).allMatch(entity -> entity.getJobName().equals("DAILY_ANALYTICS"));

        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i).getStartedAt())
                    .isAfterOrEqualTo(result.get(i + 1).getStartedAt());
        }

        assertThat(result.get(0).getStartedAt()).isEqualTo(base.plusMinutes(24));
        assertThat(result.get(19).getStartedAt()).isEqualTo(base.plusMinutes(5));
    }

    @Test
    @DisplayName("findTop20ByJobNameOrderByStartedAtDesc — возвращает пустой список если записей нет")
    void findTop20ByJobNameOrderByStartedAtDesc_ShouldReturnEmptyList_WhenNothingFound() {
        List<JobExecutionEntity> result =
                jobExecutionRepository.findTop20ByJobNameOrderByStartedAtDesc("DAILY_ANALYTICS");

        assertThat(result).isEmpty();
    }

    private JobExecutionEntity persistExecution(
            String jobName,
            JobExecutionStatus status,
            TriggerType triggerType,
            OffsetDateTime startedAt
    ) {
        JobExecutionEntity entity = JobExecutionEntity.builder()
                .id(UUID.randomUUID())
                .jobName(jobName)
                .status(status)
                .triggerType(triggerType)
                .startedAt(startedAt)
                .build();

        entityManager.persist(entity);
        return entity;
    }
}