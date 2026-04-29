package org.example.doctoratapp.exceptions;

public class ReInscriptionNonAutoriseeException extends RuntimeException {

    public ReInscriptionNonAutoriseeException() {
        super("Réinscription impossible : plus de 3 ans sans dérogation");
    }

    public ReInscriptionNonAutoriseeException(String message) {
        super(message);
    }
}
