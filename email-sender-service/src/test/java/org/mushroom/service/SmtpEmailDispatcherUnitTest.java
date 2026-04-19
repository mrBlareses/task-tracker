package org.mushroom.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.Invocation;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mushroom.entity.OutboxEmail;
import org.mushroom.entity.enums.OutboxEmailType;
import org.mushroom.exception.EmailDispatchException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockingDetails;

@ExtendWith(MockitoExtension.class)
class SmtpEmailDispatcherUnitTest {

    @Mock
    private JavaMailSender mailSender;

    private ObjectMapper objectMapper;
    private SmtpEmailDispatcher smtpEmailDispatcher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        smtpEmailDispatcher = new SmtpEmailDispatcher(mailSender);
        ReflectionTestUtils.setField(
                smtpEmailDispatcher,
                "from",
                "no-reply@task-tracker.local"
        );
    }

    @Test
    @DisplayName("send — отправляет WELCOME письмо с корректными from/to/subject/body")
    void send_ShouldDispatchWelcomeEmail_WhenTypeIsWelcome() throws Exception {
        OutboxEmail email = buildOutboxEmail(
                1L,
                OutboxEmailType.WELCOME,
                "user1@test.local:1",
                Map.of(
                        "id", "1",
                        "email", "user1@test.local",
                        "firstName", "John",
                        "lastName", "Doe"
                )
        );

        smtpEmailDispatcher.send(email);

        Collection<Invocation> invocations = mockingDetails(mailSender).getInvocations();

        Invocation sendInvocation = invocations.stream()
                .filter(invocation -> invocation.getMethod().getName().equals("send"))
                .findFirst()
                .orElse(null);

        assertThat(sendInvocation).isNotNull();

        SimpleMailMessage message = sendInvocation.getArgument(0, SimpleMailMessage.class);

        assertThat(message).isNotNull();
        assertThat(message.getFrom()).isEqualTo("no-reply@task-tracker.local");
        assertThat(message.getTo()).containsExactly("user1@test.local");
        assertThat(message.getSubject()).isEqualTo("Welcome to Task Tracker");
        assertThat(message.getText()).contains("Hello, John Doe!");
        assertThat(message.getText()).contains("Welcome to Task Tracker.");
        assertThat(message.getText()).contains("Email: user1@test.local");
    }

    @Test
    @DisplayName("send — отправляет REPORT письмо с корректными from/to/subject/body")
    void send_ShouldDispatchReportEmail_WhenTypeIsReport() throws Exception {
        OutboxEmail email = buildOutboxEmail(
                2L,
                OutboxEmailType.REPORT,
                "user2@test.local:2",
                Map.of(
                        "userId", "2",
                        "email", "user2@test.local",
                        "pendingCount", 3,
                        "completedCount", 5
                )
        );

        smtpEmailDispatcher.send(email);

        Collection<Invocation> invocations = mockingDetails(mailSender).getInvocations();

        Invocation sendInvocation = invocations.stream()
                .filter(invocation -> invocation.getMethod().getName().equals("send"))
                .findFirst()
                .orElse(null);

        assertThat(sendInvocation).isNotNull();

        SimpleMailMessage message = sendInvocation.getArgument(0, SimpleMailMessage.class);

        assertThat(message).isNotNull();
        assertThat(message.getFrom()).isEqualTo("no-reply@task-tracker.local");
        assertThat(message.getTo()).containsExactly("user2@test.local");
        assertThat(message.getSubject()).isEqualTo("Your daily task report");
        assertThat(message.getText()).contains("Here is your daily Task Tracker report.");
        assertThat(message.getText()).contains("Email: user2@test.local");
        assertThat(message.getText()).contains("Pending tasks: 3");
        assertThat(message.getText()).contains("Completed today: 5");
    }

    @Test
    @DisplayName("send — бросает EmailDispatchException если в payload отсутствует email")
    void send_ShouldThrowException_WhenPayloadEmailIsMissing() {
        OutboxEmail email = buildOutboxEmail(
                3L,
                OutboxEmailType.WELCOME,
                "broken:3",
                Map.of(
                        "id", "3",
                        "firstName", "Broken",
                        "lastName", "User"
                )
        );

        assertThatThrownBy(() -> smtpEmailDispatcher.send(email))
                .isInstanceOf(EmailDispatchException.class)
                .hasMessage("Failed to dispatch email id=3")
                .hasCauseInstanceOf(IllegalStateException.class);

        Collection<Invocation> invocations = mockingDetails(mailSender).getInvocations();

        long sendCalls = invocations.stream()
                .filter(invocation -> invocation.getMethod().getName().equals("send"))
                .count();

        assertThat(sendCalls).isEqualTo(0);
    }

    @Test
    @DisplayName("send — оборачивает исключение mailSender в EmailDispatchException")
    void send_ShouldWrapMailSenderException() {
        OutboxEmail email = buildOutboxEmail(
                4L,
                OutboxEmailType.REPORT,
                "user4@test.local:4",
                Map.of(
                        "userId", "4",
                        "email", "user4@test.local",
                        "pendingCount", 1,
                        "completedCount", 2
                )
        );

        doThrow(new MailSendException("smtp down"))
                .when(mailSender)
                .send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

        assertThatThrownBy(() -> smtpEmailDispatcher.send(email))
                .isInstanceOf(EmailDispatchException.class)
                .hasMessage("Failed to dispatch email id=4")
                .hasCauseInstanceOf(MailSendException.class);
    }

    private OutboxEmail buildOutboxEmail(
            Long id,
            OutboxEmailType type,
            String dedupKey,
            Map<String, Object> payload
    ) {
        return OutboxEmail.builder()
                .id(id)
                .type(type)
                .dedupKey(dedupKey)
                .payload(objectMapper.valueToTree(payload))
                .build();
    }
}