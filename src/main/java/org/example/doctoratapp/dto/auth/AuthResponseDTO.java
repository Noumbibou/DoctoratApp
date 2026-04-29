package org.example.doctoratapp.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// dto/auth/AuthResponseDTO.java
// Retourné après un login réussi
// Contient le token JWT + les infos de base du user
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {

    private String token;        // token JWT pour les prochaines requêtes
    private String type = "Bearer"; // type du token toujours "Bearer"
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String role;
}