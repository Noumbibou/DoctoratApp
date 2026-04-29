package org.example.doctoratapp.exceptions;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String ressource, Long id) {
        super(ressource + " introuvable avec l'id : " + id);
    }
}
