package com.example.Email.model;

public record VerificationEmailMessage(
		String traceId,
		String hostname,
		String email,
		String name,
		String verificationLink,
		String resendLink) {
}
