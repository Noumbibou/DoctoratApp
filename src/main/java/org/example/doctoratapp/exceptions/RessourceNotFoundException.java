package org.example.doctoratapp.exceptions;

public class RessourceNotFoundException extends RuntimeException {
    public RessourceNotFoundException(String message) {
        super(message);
    }
}
