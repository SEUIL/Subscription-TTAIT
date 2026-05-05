package com.ttait.subscription.notification.email.sender;

public interface MessageSender {
    void send(String to, String subject, String htmlBody);
}
