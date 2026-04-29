package org.example.doctoratapp.exceptions;

// Lancée quand on essaie d'autoriser une soutenance déjà autorisée
public class SoutenanceDejaAutoriseeException extends RuntimeException {

    public SoutenanceDejaAutoriseeException(Long id) {
        super("La soutenance avec l'id " + id + " est déjà autorisée");
    }
}
