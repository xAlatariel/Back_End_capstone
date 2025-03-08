package com.example.Capstone.service;

import com.example.Capstone.dto.UserDTO;
import com.example.Capstone.dto.UserRegistrationDTO;
import com.example.Capstone.entity.Role;
import com.example.Capstone.entity.User;
import com.example.Capstone.exception.UserAlreadyExistsException;
import com.example.Capstone.exception.UserNotFoundException;
import com.example.Capstone.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;




@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserDTO registerUser(UserRegistrationDTO registrationDTO) throws UserAlreadyExistsException {
        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new UserAlreadyExistsException("Email già registrata: " + registrationDTO.getEmail());
        }

        User newUser = new User();
        newUser.setNome(registrationDTO.getNome());
        newUser.setCognome(registrationDTO.getCognome());
        newUser.setEmail(registrationDTO.getEmail());
        newUser.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        newUser.setRuolo(Role.USER);

        User savedUser = userRepository.save(newUser); // 1. Corretto UserRepository → userRepository
        return convertToDTO(savedUser); // 2. Corretto convertDTO → convertToDTO
    }

    public UserDTO getUserById(Long id) throws UserNotFoundException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato con ID: " + id));

        return convertToDTO(user);
    }

    public UserDTO updateUser(Long id, UserDTO userDTO) throws UserNotFoundException {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato con ID: " + id));

        existingUser.setNome(userDTO.getNome());
        existingUser.setCognome(userDTO.getCognome());
        existingUser.setEmail(userDTO.getEmail());

        if(userDTO.getRuolo() != null && (userDTO.getRuolo() == Role.USER || userDTO.getRuolo() == Role.ADMIN)) {
            existingUser.setRuolo(userDTO.getRuolo());
        }

        if(userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        User updatedUser = userRepository.save(existingUser);
        return convertToDTO(updatedUser);
    }

    public void deleteUser(Long id) throws UserNotFoundException {
        if(!userRepository.existsById(id)) {
            throw new UserNotFoundException("Utente non trovato con ID: " + id);
        }
        userRepository.deleteById(id);
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setNome(user.getNome());
        dto.setCognome(user.getCognome());
        dto.setEmail(user.getEmail());
        dto.setRuolo(user.getRuolo());
        return dto;
    }
}