package com.ihub.notification.email;

/**
 * Abstraction for sending notification emails.
 * Implementations: logging (dev) or SMTP (production).
 */
public interface EmailService {

    void send(String to, String subject, String body);
}
