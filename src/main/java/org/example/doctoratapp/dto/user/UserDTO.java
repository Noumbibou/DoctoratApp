package org.example.doctoratapp.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.doctoratapp.entities.User;

import java.time.LocalDateTime;

// dto/user/UserDTO.java
// Représente un user sans mot de passe
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

    // Pas de motDePasse ici → sécurité
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

    // DTO → Entité (pour les requêtes entrantes)
    public User toEntity() {
        User user = new User();
        user.setNom(this.nom);
        user.setPrenom(this.prenom);
        user.setEmail(this.email);
        user.setRole(User.Role.valueOf(this.role));
        return user;
    }
}
