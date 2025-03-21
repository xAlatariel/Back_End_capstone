package com.example.Capstone.service;

import com.example.Capstone.dto.UserDTO;

import com.example.Capstone.dto.UserRegistrationDTO;
import com.example.Capstone.entity.Role;
import com.example.Capstone.entity.User;
import com.example.Capstone.exception.UserAlreadyExistsException;
import com.example.Capstone.exception.UserNotFoundException;
import com.example.Capstone.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    //FARE AUTORIWIRED
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
         // corretto diocane
    // Registrazione Utente
    public UserDTO registerUser(UserRegistrationDTO registrationDTO) throws UserAlreadyExistsException {
        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new UserAlreadyExistsException("Email già registrata: " + registrationDTO.getEmail());
        }

        User newUser = new User();
        newUser.setNome(registrationDTO.getNome());
        newUser.setCognome(registrationDTO.getCognome());
        newUser.setEmail(registrationDTO.getEmail());
        newUser.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        newUser.setRuolo(Role.USER); // Usa ROLE_USER come default

        newUser = userRepository.save(newUser);
        return convertToDTO(newUser);


    }



    public User findById(Long id) {
        Optional<User> userOptional = userRepository.findById(id);
        return userOptional.orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    // Recupera Utente per ID solo per admin
    public UserDTO getUserById(Long id) throws UserNotFoundException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato con ID: " + id));
        return convertToDTO(user);
    }

    // Aggiorna Utente (SOLO ADMIN)
//    @PreAuthorize("hasRole('ADMIN')")
//    public UserDTO updateUser(Long id, UserDTO userDTO) throws UserNotFoundException {
//        User existingUser = userRepository.findById(id)
//                .orElseThrow(() -> new UserNotFoundException("Utente non trovato con ID: " + id));
//
//        existingUser.setNome(userDTO.getNome());
//        existingUser.setCognome(userDTO.getCognome());
//        existingUser.setEmail(userDTO.getEmail());
//
//        // Solo ADMIN può modificare il ruolo
//        if (userDTO.getRuolo() != null) {
//            existingUser.setRuolo(userDTO.getRuolo());
//        }
//
//        User updatedUser = userRepository.save(existingUser);
//        return convertToDTO(updatedUser);
//    }

    // Cambio Password (Utente o ADMIN)

    public String changePassword(Long userId,String passwordDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato"));

        // Verifica la vecchia password se richiesto
        // if (!passwordEncoder.matches(passwordDTO.getOldPassword(), user.getPassword())) {
        //     throw new InvalidPasswordException("Password corrente errata");
        // }

        user.setPassword(passwordEncoder.encode( passwordDTO));
        userRepository.save(user);

        return "password cambiata";
    }

    // Elimina Utente (SOLO ADMIN)
//    @PreAuthorize("hasRole('ADMIN')")
//    public void deleteUser(Long id) throws UserNotFoundException {
//        if (!userRepository.existsById(id)) {
//            throw new UserNotFoundException("Utente non trovato con ID: " + id);
//        }
//        userRepository.deleteById(id);
//    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setNome(user.getNome());
        dto.setCognome(user.getCognome());
        dto.setEmail(user.getEmail());

        return dto;
    }


//    private UserDTO convertFromRegisterToDTO(UserRegistrationDTO userRegistrationDTO){
//
//        UserDTO userDTO = new UserDTO();
//        userDTO.setNome(userRegistrationDTO.getNome());
//        userDTO.setCognome(userRegistrationDTO.getCognome());
//        userDTO.setEmail(userRegistrationDTO.getEmail());
//
//        return userDTO;
//    }
}