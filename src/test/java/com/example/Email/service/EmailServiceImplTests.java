package com.example.Email.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class EmailServiceImplTests {

	private JavaMailSender mailSender;
	private EmailServiceImpl service;

	@BeforeEach
	void setUp() {
		mailSender = mock(JavaMailSender.class);
		service = new EmailServiceImpl(
				JsonMapper.builder().findAndAddModules().build(), mailSender, "no-reply@example.com");
	}

	@Test
	void receiveMessageExtractsValuesAndSendsVerificationEmail() {
		service.receiveMessage("""
				{
				  "traceId":"trace-123",
				  "hostname":"example.com",
				  "email":"ada@example.com",
				  "name":"Ada Lovelace",
				  "verificationLink":"https://example.com/verify?token=abc123",
				  "resendLink":"https://example.com/verifications/resend?email=ada%40example.com"
				}
				""");

		ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(mailCaptor.capture());

		SimpleMailMessage mail = mailCaptor.getValue();
		assertThat(mail.getFrom()).isEqualTo("no-reply@example.com");
		assertThat(mail.getTo()).containsExactly("ada@example.com");
		assertThat(mail.getSubject()).isEqualTo("Verify your email address");
		assertThat(mail.getText())
				.contains("Hello Ada Lovelace,")
				.contains("https://example.com/verify?token=abc123")
				.contains("https://example.com/verifications/resend?email=ada%40example.com");
	}

	@Test
	void consumeMessageReadsJsonFromRawRabbitMessage() {
		String json = """
				{
				  "traceId":"trace-rabbit-123",
				  "hostname":"localhost",
				  "email":"ada@example.com",
				  "name":"Ada Lovelace",
				  "verificationLink":"https://example.com/verify?token=rabbit",
				  "resendLink":"https://example.com/verifications/resend"
				}
				""";

		service.consumeMessage(new Message(
				json.getBytes(StandardCharsets.UTF_8), new MessageProperties()));

		ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(mailCaptor.capture());
		assertThat(mailCaptor.getValue().getTo()).containsExactly("ada@example.com");
		assertThat(mailCaptor.getValue().getText())
				.contains("https://example.com/verify?token=rabbit")
				.contains("https://example.com/verifications/resend");
	}

	@Test
	void receiveMessageRejectsMissingEmail() {
		assertThatThrownBy(() -> service.receiveMessage("""
				{
				  "verificationLink":"https://example.com/verify?token=abc123",
				  "resendLink":"https://example.com/verifications/resend"
				}
				"""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Email queue message must contain an email address");

		verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
	}

	@Test
	void receiveMessageRejectsInvalidVerificationLink() {
		assertThatThrownBy(() -> service.receiveMessage("""
				{
				  "email":"ada@example.com",
				  "verificationLink":"not-a-link",
				  "resendLink":"https://example.com/verifications/resend"
				}
				"""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Email queue message must contain a valid HTTP verification link");

		verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
	}

	@Test
	void receiveMessageRejectsMissingResendLink() {
		assertThatThrownBy(() -> service.receiveMessage("""
				{
				  "email":"ada@example.com",
				  "verificationLink":"https://example.com/verify?token=abc123"
				}
				"""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Email queue message must contain a resend link");

		verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
	}

	@Test
	void receiveMessageRejectsMalformedJson() {
		assertThatThrownBy(() -> service.receiveMessage("not-json"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Email queue message must be valid JSON");

		verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
	}
}
