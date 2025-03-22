package com.example.Capstone.service;

import com.example.Capstone.dto.UserDTO;
import com.example.Capstone.dto.UserRegistrationDTO;
import com.example.Capstone.entity.Role;
import com.example.Capstone.entity.User;
import com.example.Capstone.exception.UserAlreadyExistsException;
import com.example.Capstone.exception.UserNotFoundException;
import com.example.Capstone.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // User Registration
    public UserDTO registerUser(UserRegistrationDTO registrationDTO) throws UserAlreadyExistsException {
        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered: " + registrationDTO.getEmail());
        }

        User newUser = new User();
        newUser.setNome(registrationDTO.getNome());
        newUser.setCognome(registrationDTO.getCognome());
        newUser.setEmail(registrationDTO.getEmail());
        newUser.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        newUser.setRuolo(Role.USER); // Default role is USER

        newUser = userRepository.save(newUser);
        return convertToDTO(newUser);
    }

    public User findById(Long id) {
        Optional<User> userOptional = userRepository.findById(id);
        return userOptional.orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    // Get User by ID
    public UserDTO getUserById(Long id) throws UserNotFoundException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));
        return convertToDTO(user);
    }

    // Change Password
    public void changePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setNome(user.getNome());
        dto.setCognome(user.getCognome());
        dto.setEmail(user.getEmail());

        return dto;
    }
}