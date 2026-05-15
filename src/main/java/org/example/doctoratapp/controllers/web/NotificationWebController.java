package org.example.doctoratapp.controllers.web;

import org.example.doctoratapp.entities.Notification;
import org.example.doctoratapp.entities.User;
import org.example.doctoratapp.services.interfaces.INotificationService;
import org.example.doctoratapp.services.interfaces.IUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

/**
 * Controller Web pour la gestion des notifications utilisateur.
 *
 * <p>Pages gérées :
 * <ul>
 *   <li>GET /notifications → Liste de toutes les notifications du user</li>
 *   <li>POST /notifications/{id}/lue → Marquer une notification comme lue</li>
 *   <li>POST /notifications/touteslues → Tout marquer comme lu</li>
 * </ul>
 * </p>
 */
@Controller
@RequestMapping("/notifications")
public class NotificationWebController {

    private final INotificationService notificationService;
    private final IUserService userService;

    public NotificationWebController(INotificationService notificationService, IUserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    // ══════════════════════════════════════════════
    //  GET /notifications
    // ══════════════════════════════════════════════

    @GetMapping
    public String listeNotifications(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        User user = userService.findByEmail(principal.getName());
        List<Notification> notifications = notificationService.findByDestinataire(user);

        model.addAttribute("notifications", notifications);
        model.addAttribute("connectedUser", user);

        return "notifications/liste";
    }

    // ══════════════════════════════════════════════
    //  POST /notifications/{id}/lue
    // ══════════════════════════════════════════════

    @PostMapping("/{id}/lue")
    public String marquerLue(@PathVariable Long id, Principal principal) {
        if (principal == null) return "redirect:/login";

        // Vérifier que la notification appartient bien à l'utilisateur
        Notification n = notificationService.findById(id);
        if (n.getDestinataire().getEmail().equals(principal.getName())) {
            notificationService.marquerCommeLue(id);
        }

        return "redirect:/notifications";
    }

    // ══════════════════════════════════════════════
    //  POST /notifications/touteslues
    // ══════════════════════════════════════════════

    @PostMapping("/touteslues")
    public String marquerToutesLues(Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        User user = userService.findByEmail(principal.getName());
        notificationService.marquerToutesCommeLues(user);

        redirectAttributes.addFlashAttribute("successMessage", "Toutes les notifications ont été marquées comme lues.");
        return "redirect:/notifications";
    }
}
