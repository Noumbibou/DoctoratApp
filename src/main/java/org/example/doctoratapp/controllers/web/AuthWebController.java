package org.example.doctoratapp.controllers.web;

import jakarta.validation.Valid;
import org.example.doctoratapp.dto.auth.AuthRequestDTO;
import org.example.doctoratapp.dto.user.UserDTO;
import org.example.doctoratapp.entities.User;
import org.example.doctoratapp.exceptions.EmailDejaUtiliseException;
import org.example.doctoratapp.services.interfaces.IUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller Web pour l'authentification (login, register, logout).
 *
 * <p>Note : Le POST /login est intercepté par Spring Security (UsernamePasswordAuthenticationFilter).
 * Ce controller ne gère que l'affichage de la page de login et le processus d'inscription.</p>
 *
 * <p>Le POST /logout est également géré par Spring Security (LogoutFilter).</p>
 */
@Controller
public class AuthWebController {

    private final IUserService userService;

    // Injection par constructeur (pas de @Autowired)
    public AuthWebController(IUserService userService) {
        this.userService = userService;
    }

    // ══════════════════════════════════════════════
    //  LOGIN
    // ══════════════════════════════════════════════

    /**
     * Affiche la page de login.
     *
     * <p>Spring Security redirige vers /login?error en cas d'échec d'authentification,
     * vers /login?logout après déconnexion. Le POST /register redirige vers
     * /login?registerSuccess après une inscription réussie.</p>
     *
     * <p>La vue utilise th:object="${authRequest}" avec th:field, donc on DOIT
     * fournir un AuthRequestDTO vide dans le model, sinon Thymeleaf lèvera
     * une exception.</p>
     *
     * @param error           présent si Spring Security a échoué l'authentification
     * @param logout          présent si l'utilisateur vient de se déconnecter
     * @param registerSuccess présent si l'utilisateur vient de s'inscrire avec succès
     * @param model           le model Spring MVC
     * @return le nom de la vue "auth/login"
     */
    @GetMapping("/login")
    public String afficherLogin(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "registerSuccess", required = false) String registerSuccess,
            Model model) {

        // Objet nécessaire pour th:object="${authRequest}" et th:field="*{email}", th:field="*{motDePasse}"
        model.addAttribute("authRequest", new AuthRequestDTO());

        // Convertir les query params en booléens pour th:if="${error}", th:if="${logout}", th:if="${registerSuccess}"
        if (error != null) {
            model.addAttribute("error", true);
        }
        if (logout != null) {
            model.addAttribute("logout", true);
        }
        if (registerSuccess != null) {
            model.addAttribute("registerSuccess", true);
        }

        return "auth/login"; // → templates/auth/login.html
    }

    // ══════════════════════════════════════════════
    //  REGISTER
    // ══════════════════════════════════════════════

    /**
     * Affiche la page d'inscription.
     *
     * <p>Fournit un UserDTO vide pour le formulaire d'inscription.</p>
     *
     * @param model le model Spring MVC
     * @return le nom de la vue "auth/register"
     */
    @GetMapping("/register")
    public String afficherRegister(Model model) {
        model.addAttribute("userDTO", new UserDTO());
        return "auth/register"; // → templates/auth/register.html
    }

    /**
     * Traitement du formulaire d'inscription.
     *
     * <p>Validations effectuées :
     * 1. Validation des annotations @Valid sur UserDTO (nom, prenom, email, motDePasse)
     * 2. Vérification côté serveur que motDePasse == confirmMotDePasse
     * 3. Vérification métier que l'email n'est pas déjà utilisé
     *
     * <p>Applique le pattern PRG (Post-Redirect-Get) :
     * en cas de succès, redirige vers /login?registerSuccess.</p>
     *
     * @param dto    le DTO rempli par le formulaire, validé avec @Valid
     * @param result le résultat de la validation (BindingResult)
     * @param model  le model Spring MVC (pour renvoyer les erreurs)
     * @return redirect vers /login?registerSuccess ou retour au formulaire si erreurs
     */
    @PostMapping("/register")
    public String register(@ModelAttribute("userDTO") @Valid UserDTO dto,
                           BindingResult result,
                           Model model) {

        // Vérification côté serveur de la correspondance des mots de passe
        // (la validation JS côté client est contournable)
        if (dto.getMotDePasse() != null
                && !dto.getMotDePasse().equals(dto.getConfirmMotDePasse())) {
            result.rejectValue("confirmMotDePasse", "error.userDTO",
                    "Les mots de passe ne correspondent pas.");
        }

        // Si erreurs de validation → retourner le formulaire
        if (result.hasErrors()) {
            return "auth/register";
        }

        try {
            User user = dto.toEntity();
            userService.ajouter(user);
            // PRG pattern : redirect après POST réussi
            // Le paramètre "registerSuccess" correspond au th:if="${registerSuccess}" dans login.html
            return "redirect:/login?registerSuccess";
        } catch (EmailDejaUtiliseException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }

    // ══════════════════════════════════════════════
    //  LOGOUT
    // ══════════════════════════════════════════════
    // Le POST /logout est entièrement géré par Spring Security (LogoutFilter).
    // Après déconnexion, Spring Security redirige vers /login?logout.
    // Aucune méthode nécessaire ici.
}
