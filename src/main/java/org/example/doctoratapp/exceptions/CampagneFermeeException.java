package org.example.doctoratapp.exceptions;

// Lancée quand un doctorant essaie de s'inscrire hors campagne
public class CampagneFermeeException extends RuntimeException {

    public CampagneFermeeException() {
        super("Aucune campagne d'inscription ouverte actuellement");
    }
}
