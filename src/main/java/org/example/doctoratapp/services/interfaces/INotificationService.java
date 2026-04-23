package org.example.doctoratapp.services.interfaces;

import org.example.doctoratapp.entities.Notification;
import org.example.doctoratapp.entities.User;

import java.util.List;

public interface INotificationService {
    Notification findById(Long id);
    List<Notification> findAll();
    Notification envoyer(Notification notification);
    void supprimer(Long id);
    List<Notification> findByDestinataire(User user);
    List<Notification> findNonLues(User user);
    long countNonLues(User user);
    void marquerCommeLue(Long id);
    void marquerToutesCommeLues(User user);

}
