package com.example.Email.service;

import com.example.Email.model.VerificationEmailMessage;

public interface EmailService {

	void receiveMessage(String message);

	// void receiveMessage(VerificationEmailMessage message);

}
