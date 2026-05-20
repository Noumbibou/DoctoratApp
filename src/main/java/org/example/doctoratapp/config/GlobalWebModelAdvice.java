package org.example.doctoratapp.config;

import org.example.doctoratapp.entities.User;
import org.example.doctoratapp.services.interfaces.INotificationService;
import org.example.doctoratapp.services.interfaces.IUserService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

import java.security.Principal;

/**
 * Global advice to add connected user and unread notifications count to all views.
 */
@ControllerAdvice
public class GlobalWebModelAdvice {

    private final IUserService userService;
    private final INotificationService notificationService;

    public GlobalWebModelAdvice(IUserService userService, INotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @ModelAttribute
    public void addGlobalAttributes(Model model, Principal principal) {
        if (principal != null) {
            try {
                User user = userService.findByEmail(principal.getName());
                if (user != null) {
                    model.addAttribute("connectedUser", user);
                    
                    // Add notifications count globally so the sidebar badge is always up to date
                    long count = notificationService.countNonLues(user);
                    model.addAttribute("notificationsCount", count);
                }
            } catch (Exception e) {
                // Ignore exceptions (e.g. user not found or db issues during startup/security filters)
            }
        }
    }
}
