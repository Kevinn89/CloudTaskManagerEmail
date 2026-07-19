package com.example.Email.service;

import com.example.Email.model.VerificationEmailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@Service
public class EmailServiceImpl implements EmailService {

	private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);
	private static final String SUBJECT = "Verify your email address";
	private final ObjectMapper objectMapper;
	private final JavaMailSender mailSender;
	private final String fromAddress;

	public EmailServiceImpl(ObjectMapper objectMapper, JavaMailSender mailSender,
			@Value("${email.from}") String fromAddress) {
		this.objectMapper = objectMapper;
		this.mailSender = mailSender;
		this.fromAddress = fromAddress;
	}

	// public EmailServiceImpl(JavaMailSender mailSender,
	// @Value("${email.from}") String fromAddress) {
	// this.mailSender = mailSender;
	// this.fromAddress = fromAddress;
	// }

	@RabbitListener(queues = "${email.rabbitmq.queue}")
	public void consumeMessage(Message message) {
		receiveMessage(new String(message.getBody(), StandardCharsets.UTF_8));
	}

	// @RabbitListener(queues = "${email.rabbitmq.queue}")
	// public void consumeMessage(VerificationEmailMessage emailMessage) {
	// receiveMessage(emailMessage);
	// }

	// @Override
	// public void receiveMessage(VerificationEmailMessage emailMessage) {

	// validate(emailMessage);

	// SimpleMailMessage mail = new SimpleMailMessage();
	// mail.setFrom(fromAddress);
	// mail.setTo(emailMessage.email());
	// mail.setSubject(SUBJECT);
	// mail.setText(buildEmailBody(emailMessage));

	// mailSender.send(mail);
	// log.info("Verification email sent to {} for traceId {}",
	// emailMessage.email(), emailMessage.traceId());
	// }

	@Override
	public void receiveMessage(String message) {
		VerificationEmailMessage emailMessage = parse(message);
		validate(emailMessage);

		SimpleMailMessage mail = new SimpleMailMessage();
		mail.setFrom(fromAddress);
		mail.setTo(emailMessage.email());
		mail.setSubject(SUBJECT);
		mail.setText(buildEmailBody(emailMessage));

		mailSender.send(mail);
		log.info("Verification email sent to {} for traceId {}",
				emailMessage.email(), emailMessage.traceId());
	}

	private VerificationEmailMessage parse(String message) {
		try {
			return objectMapper.readValue(message, VerificationEmailMessage.class);
		} catch (JacksonException exception) {
			throw new IllegalArgumentException("Email queue message must be valid JSON",
					exception);
		}
	}

	private void validate(VerificationEmailMessage message) {
		if (message.email() == null || message.email().isBlank()) {
			throw new IllegalArgumentException("Email queue message must contain an email address");
		}
		if (message.verificationLink() == null || message.verificationLink().isBlank()) {
			throw new IllegalArgumentException("Email queue message must contain a verification link");
		}
		if (message.resendLink() == null || message.resendLink().isBlank()) {
			throw new IllegalArgumentException("Email queue message must contain a resend link");
		}

		validateHttpLink(message.verificationLink(), "verification");
		validateHttpLink(message.resendLink(), "resend");
	}

	private void validateHttpLink(String value, String linkType) {
		try {
			URI link = URI.create(value);
			if (!("http".equalsIgnoreCase(link.getScheme()) || "https".equalsIgnoreCase(link.getScheme()))
					|| link.getHost() == null) {
				throw new IllegalArgumentException();
			}
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException(
					"Email queue message must contain a valid HTTP " + linkType + " link");
		}
	}

	private String buildEmailBody(VerificationEmailMessage message) {
		String greeting = message.name() == null || message.name().isBlank()
				? "Hello,"
				: "Hello " + message.name() + ",";

		return greeting + "\n\n"
				+ "Please verify your email address by visiting the following link:\n"
				+ message.verificationLink() + "\n\n"
				+ "If the verification link has expired, request a new one here:\n"
				+ message.resendLink();
	}

}
