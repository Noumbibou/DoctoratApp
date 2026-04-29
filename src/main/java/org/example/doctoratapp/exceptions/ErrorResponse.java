package org.example.doctoratapp.exceptions;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private int status;           // code HTTP (400, 404, 500...)
    private String message;       // message lisible
    private LocalDateTime timestamp; // quand l'erreur s'est produite
    private List<String> details; // liste d'erreurs (pour la validation)

    // Constructeur simple sans details
    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.details = new ArrayList<>();
    }
}