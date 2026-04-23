package org.example.doctoratapp.repo;

import org.example.doctoratapp.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findByRole(User.Role role);

    Optional<User> deleteUserByEmail(String email);

    boolean existsByEmail(String email);
}
