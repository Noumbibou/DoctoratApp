package org.example.doctoratapp.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.doctoratapp.entities.User;

import java.time.LocalDateTime;

// dto/user/UserDTO.java
// Représente un user (avec champs mot de passe pour le formulaire d'inscription)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format email invalide")
    private String email;

    // Champs utilisés uniquement pour le formulaire d'inscription (register.html)
    // Jamais renvoyés par fromEntity() → sécurité préservée
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String motDePasse;

    private String confirmMotDePasse;

    private String role;
    private LocalDateTime dateDeCreation;

    public static UserDTO fromEntity(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setNom(user.getNom());
        dto.setPrenom(user.getPrenom());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().name());
        dto.setDateDeCreation(user.getDateDeCreation());
        // pas de motDePasse ✅
        return dto;
    }

    // DTO → Entité (pour les requêtes entrantes, notamment l'inscription)
    public User toEntity() {
        User user = new User();
        user.setNom(this.nom);
        user.setPrenom(this.prenom);
        user.setEmail(this.email);
        // Le mot de passe en clair : sera encodé par le service
        user.setMotDePasse(this.motDePasse);
        // Rôle par défaut si non spécifié (cas du formulaire register qui n'a pas de champ rôle)
        user.setRole(this.role != null ? User.Role.valueOf(this.role) : User.Role.CANDIDAT);
        return user;
    }
}
