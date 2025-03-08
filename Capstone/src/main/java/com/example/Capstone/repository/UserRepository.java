package com.example.Capstone.repository;

import com.example.Capstone.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {

     Optional<User> findByEmail(String email);

    List<User> findByNomeAndCognome(String nome, String cognome);

    boolean existsByEmail(String email);
}
