package org.example.doctoratapp.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// dto/auth/AuthRequestDTO.java
// Reçoit les données du formulaire de login
// On a besoin uniquement de l'email et du mot de passe
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequestDTO {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String motDePasse;
}
