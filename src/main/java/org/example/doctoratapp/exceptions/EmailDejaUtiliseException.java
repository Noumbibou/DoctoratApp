package org.example.doctoratapp.exceptions;

// Lancée quand on essaie de créer un user avec un email déjà existant
public class EmailDejaUtiliseException extends RuntimeException {

    public EmailDejaUtiliseException(String email) {
        super("Email déjà utilisé : " + email);
    }
}
