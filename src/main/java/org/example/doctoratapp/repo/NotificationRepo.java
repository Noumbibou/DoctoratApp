package org.example.doctoratapp.repo;

import org.example.doctoratapp.entities.Notification;
import org.example.doctoratapp.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepo extends JpaRepository<Notification, Long> {

    // Notifications d'un user
    List<Notification> findByDestinataire(User user);

    // Notifications non lues d'un user
    List<Notification> findByDestinataireAndLu(User user, Boolean lu);

    // Compter les notifications non lues
    long countByDestinataireAndLu(User user, Boolean lu);

    // Notifications par type
    List<Notification> findByDestinataireAndType(
            User user,
            Notification.TypeNotification type
    );
}
