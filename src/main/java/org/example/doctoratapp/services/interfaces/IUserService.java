package org.example.doctoratapp.services.interfaces;

import org.example.doctoratapp.entities.User;

import java.util.List;

public interface IUserService {
    // READ
    User findById(Long id);
    List<User> findAll();
    User findByEmail(String email);
    List<User> findByRole(User.Role role);

    // CREATE
    User ajouter(User user);

    // UPDATE
    User modifier(Long id, User user);

    // DELETE
    void supprimer(Long id);
}
