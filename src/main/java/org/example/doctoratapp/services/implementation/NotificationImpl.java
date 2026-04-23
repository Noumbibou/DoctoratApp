package org.example.doctoratapp.services.implementation;

import org.example.doctoratapp.entities.Notification;
import org.example.doctoratapp.entities.User;
import org.example.doctoratapp.repo.NotificationRepo;
import org.example.doctoratapp.services.interfaces.INotificationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationImpl implements INotificationService {

    private NotificationRepo notificationRepo;

    public NotificationImpl(NotificationRepo notificationRepo) {
        this.notificationRepo = notificationRepo;
    }

    @Override
    public Notification findById(Long id) {
        return notificationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification introuvable avec l'id : " + id));
    }

    @Override
    public List<Notification> findAll() {
        return notificationRepo.findAll();
    }

    @Override
    public Notification envoyer(Notification notification) {
        return notificationRepo.save(notification);
    }

    @Override
    public void supprimer(Long id) {
        if (!notificationRepo.existsById(id)) {
            throw new RuntimeException("Notification introuvable avec l'id : " + id);
        }
        notificationRepo.deleteById(id);
    }

    @Override
    public List<Notification> findByDestinataire(User user) {
        return notificationRepo.findByDestinataire(user);
    }

    @Override
    public List<Notification> findNonLues(User user) {
        return notificationRepo.findByDestinataireAndLu(user, false);
    }

    @Override
    public long countNonLues(User user) {
        return notificationRepo.countByDestinataireAndLu(user, false);
    }

    @Override
    public void marquerCommeLue(Long id) {
        Notification notification = findById(id);
        notification.setLu(true);
        notificationRepo.save(notification);
    }

    @Override
    public void marquerToutesCommeLues(User user) {
        List<Notification> nonLues = findNonLues(user);
        nonLues.forEach(n -> n.setLu(true));
        notificationRepo.saveAll(nonLues);
    }
}
