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
        User.Role enumRole = this.role != null ? User.Role.valueOf(this.role) : User.Role.CANDIDAT;
        User user;
        if (enumRole == User.Role.CANDIDAT) {
            org.example.doctoratapp.entities.Doctorant doc = new org.example.doctoratapp.entities.Doctorant();
            doc.setStatutDoctorant(org.example.doctoratapp.entities.Doctorant.Statut.ACTIF);
            doc.setAnneeEnCours(1);
            doc.setDateInscriptionInitiale(java.time.LocalDate.now());
            doc.setNumInscription("DOC-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            user = doc;
        } else if (enumRole == User.Role.DIRECTEUR) {
            org.example.doctoratapp.entities.DirecteurThese dir = new org.example.doctoratapp.entities.DirecteurThese();
            dir.setGrade("PES");
            user = dir;
        } else {
            user = new User();
        }
        user.setNom(this.nom);
        user.setPrenom(this.prenom);
        user.setEmail(this.email);
        // Le mot de passe en clair : sera encodé par le service
        user.setMotDePasse(this.motDePasse);
        user.setRole(enumRole);
        return user;
    }
}
