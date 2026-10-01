package com.econest.notification.dao;

import com.econest.notification.model.EmailNotification;

import java.util.List;
import java.util.Optional;

/** Persistence operations for {@link EmailNotification}. */
public interface NotificationDao {

    List<EmailNotification> findAll();

    Optional<EmailNotification> findById(int id);

    EmailNotification save(EmailNotification entity);

    void delete(int id);
}
