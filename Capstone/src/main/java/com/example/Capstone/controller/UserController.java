package com.example.Capstone.controller;

import com.example.Capstone.dto.*;
import com.example.Capstone.entity.User;
import com.example.Capstone.exception.UserAlreadyExistsException;
import com.example.Capstone.exception.UserNotFoundException;
import com.example.Capstone.service.UserService;
import com.example.Capstone.utils.JWTTools;
import jakarta.validation.Valid;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", allowCredentials = "true")
@RestController
@RequestMapping("/api/users")
public class UserController {



    @Autowired
    UserService userService;

    @Autowired
    JWTTools jwt;

    @Autowired
    PasswordEncoder passwordEncoder;




    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUser(
            @Valid @RequestBody UserRegistrationDTO registrationDTO,
            BindingResult validation
    ) throws UserAlreadyExistsException, BadRequestException {
        // Controlla gli errori di validazione
        if (validation.hasErrors()) {
            throw new BadRequestException("Errori di validazione: " +
                    validation.getAllErrors().stream()
                            .map(error -> error.getDefaultMessage())
                            .collect(Collectors.joining(", ")));
        }

        UserDTO createdUser = userService.registerUser(registrationDTO);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }





//    @PostMapping("/login")
//    @ResponseStatus(HttpStatus.OK)
//    public APIResponse<TokenDTO> login(@Valid @RequestBody LoginDTO credentials,
//                                       BindingResult validation) throws BadRequestException {
//        // Verifica gli errori di validazione
//        if (validation.hasErrors()) {
//            throw new BadRequestException("Dati di login non validi");
//        }
//
//        // Trova l'utente tramite email
//        User found = userService.findByEmail(credentials.email());
//
//        // Verifica la password usando PasswordEncoder
//        if (!passwordEncoder.matches(credentials.password(), found.getPassword())) {
//            throw new BadRequestException("Password errata");
//        }
//
//        // Genera il token JWT
//        String token = jwt.createToken(credentials.email());
//
//        // Restituisci la risposta con il token
//        return new APIResponse<>(APIStatus.SUCCESS, new TokenDTO(token));
//    }


    @PostMapping("/login")
    public ResponseEntity<APIResponse<TokenDTO>> login(
            @Valid @RequestBody LoginDTO credentials,
            BindingResult validation
    ) throws BadRequestException {
        // Verifica gli errori di validazione
        if (validation.hasErrors()) {
            throw new BadRequestException("Dati di login non validi");
        }

        // Trova l'utente tramite email
        try {
            User found = userService.findByEmail(credentials.email());

            // Verifica la password usando PasswordEncoder
            if (!passwordEncoder.matches(credentials.password(), found.getPassword())) {
                throw new BadRequestException("Password errata");
            }

            // Genera il token JWT
            String token = jwt.createToken(credentials.email());

            // Restituisci la risposta con il token
            APIResponse<TokenDTO> response = new APIResponse<>(APIStatus.SUCCESS, new TokenDTO(token));
            return ResponseEntity.ok(response);
        } catch (UserNotFoundException e) {
            throw new BadRequestException("Utente non trovato");
        }
    }




    // Recupera Utente per ID (SOLO ADMIN o utente stesso)
    @GetMapping("/{id}")
    @PreAuthorize("#id == principal.id or hasRole('ADMIN')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) throws UserNotFoundException {
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    //



//     Cambio Password (Utente stesso o ADMIN)
    @PostMapping("/{id}/change-password")
    @PreAuthorize("#id == principal.id or hasRole('ADMIN')")
    public ResponseEntity<Void> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody String passwordDTO
    ) {
        String response = userService.changePassword(id, passwordDTO);
        return ResponseEntity.ok().build();
    }

//    // Elimina Utente (SOLO ADMIN)
//    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<Void> deleteUser(@PathVariable Long id) throws UserNotFoundException {
//        userService.deleteUser(id);
//        return ResponseEntity.noContent().build();
//    }
}