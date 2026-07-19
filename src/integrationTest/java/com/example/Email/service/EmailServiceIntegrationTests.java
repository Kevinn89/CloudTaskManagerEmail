package com.example.Email.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
		"spring.rabbitmq.listener.simple.auto-startup=false",
		"email.rabbitmq.queue=email.integration.queue",
		"email.from=integration@example.com"
})
class EmailServiceIntegrationTests {

	@Autowired
	private EmailService emailService;

	@MockitoBean
	private JavaMailSender mailSender;

	@Test
	void applicationWiresQueueMessageProcessingToMailSender() {
		emailService.receiveMessage("""
				{
				  "traceId": "trace-456",
				  "hostname": "example.com",
				  "email": "grace@example.com",
				  "name": "Grace Hopper",
				  "verificationLink": "https://example.com/api/verifications/verify?token=token-456",
				  "resendLink": "https://example.com/api/verifications/resend?email=grace%40example.com"
				}
				""");

		ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(mailCaptor.capture());

		SimpleMailMessage mail = mailCaptor.getValue();
		assertThat(mail.getFrom()).isEqualTo("integration@example.com");
		assertThat(mail.getTo()).containsExactly("grace@example.com");
		assertThat(mail.getText())
				.contains("Hello Grace Hopper,")
				.contains("https://example.com/api/verifications/verify?token=token-456")
				.contains("https://example.com/api/verifications/resend?email=grace%40example.com");
	}
}
