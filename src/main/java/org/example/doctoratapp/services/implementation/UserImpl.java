package org.example.doctoratapp.services.implementation;

import org.example.doctoratapp.entities.User;
import org.example.doctoratapp.repo.UserRepo;
import org.example.doctoratapp.services.interfaces.IUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserImpl implements IUserService {

    private UserRepo userRepo;
    private PasswordEncoder passwordEncoder;

    public UserImpl(UserRepo userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // ─── READ ────────────────────────────────────────────────

    @Override
    public User findById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User introuvable avec l'id : " + id));
    }

    @Override
    public List<User> findAll() {
        return userRepo.findAll();
    }

    @Override
    public User findByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Aucun user avec l'email : " + email));
    }

    @Override
    public List<User> findByRole(User.Role role) {
        List<User> users = userRepo.findByRole(role);
        if (users.isEmpty()) {
            throw new RuntimeException("Aucun user trouvé avec le rôle : " + role);
        }
        return users;
    }

    // ─── CREATE ──────────────────────────────────────────────

    @Override
    public User ajouter(User user) {

        // Vérifier si l'email existe déjà
        if (userRepo.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email déjà utilisé : " + user.getEmail());
        }

        // Hasher le mot de passe avant sauvegarde
        user.setMotDePasse(passwordEncoder.encode(user.getMotDePasse()));

        return userRepo.save(user);
    }

    // ─── UPDATE ──────────────────────────────────────────────

    @Override
    public User modifier(Long id, User userModifie) {

        // Vérifier que le user existe
        User existant = findById(id);

        // Mettre à jour les champs modifiables
        existant.setNom(userModifie.getNom());
        existant.setPrenom(userModifie.getPrenom());
        existant.setRole(userModifie.getRole());

        // Mettre à jour le mot de passe seulement s'il est fourni
        if (userModifie.getMotDePasse() != null &&
                !userModifie.getMotDePasse().isEmpty()) {
            existant.setMotDePasse(
                    passwordEncoder.encode(userModifie.getMotDePasse())
            );
        }

        return userRepo.save(existant);
    }

    // ─── DELETE ──────────────────────────────────────────────

    @Override
    public void supprimer(Long id) {
        if (!userRepo.existsById(id)) {
            throw new RuntimeException("User introuvable avec l'id : " + id);
        }
        userRepo.deleteById(id);
    }
}
