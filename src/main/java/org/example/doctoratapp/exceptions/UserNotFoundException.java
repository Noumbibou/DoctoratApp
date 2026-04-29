package org.example.doctoratapp.exceptions;

import org.example.doctoratapp.entities.User;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("User introuvable avec l'id : " + id);
    }

    public UserNotFoundException(String email) {
        super("User introuvable avec l'email : " + email);
    }

    public UserNotFoundException(User.Role role) {
        super("User introuvable avec le role : " + role);
    }
}
