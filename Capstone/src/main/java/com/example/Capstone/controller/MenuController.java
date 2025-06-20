package com.example.Capstone.controller;

import com.example.Capstone.dto.*;
import com.example.Capstone.dto.APIResponse;
import com.example.Capstone.dto.APIStatus;
import com.example.Capstone.service.MenuService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    // ===================================================================
    // ENDPOINT PUBBLICI (per clienti)
    // ===================================================================
    @GetMapping("/active")
    public ResponseEntity<APIResponse<List<MenuResponseDTO>>> getAllActiveMenus() {
        try {
            List<MenuResponseDTO> menus = menuService.getAllActiveMenus();
            return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS, "Menu recuperati con successo", menus));
        } catch (Exception e) {
            log.error("Errore recupero menu attivi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(APIStatus.ERROR, "Errore nel recupero dei menu"));
        }
    }

    @GetMapping("/daily/today")
    public ResponseEntity<APIResponse<MenuResponseDTO>> getTodaysDailyMenu() {
        try {
            MenuResponseDTO menu = menuService.getTodaysDailyMenu();
            return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS, "Menu del giorno recuperato", menu));
        } catch (Exception e) {
            log.error("Errore recupero menu del giorno", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new APIResponse<>(APIStatus.ERROR, "Nessun menu del giorno disponibile"));
        }
    }

    @GetMapping("/seasonal/current")
    public ResponseEntity<APIResponse<MenuResponseDTO>> getCurrentSeasonalMenu() {
        try {
            MenuResponseDTO menu = menuService.getCurrentSeasonalMenu();
            return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS, "Menu stagionale recuperato", menu));
        } catch (Exception e) {
            log.error("Errore recupero menu stagionale", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new APIResponse<>(APIStatus.ERROR, "Nessun menu stagionale disponibile"));
        }
    }

    // ===================================================================
    // ENDPOINT ADMIN
    // ===================================================================
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<MenuResponseDTO>> createMenu(@Valid @RequestBody MenuRequestDTO request) {
        try {
            MenuResponseDTO menu = menuService.createMenu(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new APIResponse<>(APIStatus.SUCCESS, "Menu creato con successo", menu));
        } catch (IllegalArgumentException e) {
            log.warn("Errore validazione creazione menu: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new APIResponse<>(APIStatus.ERROR, e.getMessage()));
        } catch (Exception e) {
            log.error("Errore creazione menu", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(APIStatus.ERROR, "Errore nella creazione del menu"));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<MenuResponseDTO>> getMenuById(@PathVariable Long id) {
        try {
            MenuResponseDTO menu = menuService.getMenuById(id);
            return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS, "Menu recuperato", menu));
        } catch (Exception e) {
            log.error("Errore recupero menu ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new APIResponse<>(APIStatus.ERROR, "Menu non trovato"));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<MenuResponseDTO>> updateMenu(
            @PathVariable Long id,
            @Valid @RequestBody MenuRequestDTO request) {
        try {
            MenuResponseDTO menu = menuService.updateMenu(id, request);
            return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS, "Menu aggiornato con successo", menu));
        } catch (IllegalArgumentException e) {
            log.warn("Errore validazione aggiornamento menu: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new APIResponse<>(APIStatus.ERROR, e.getMessage()));
        } catch (Exception e) {
            log.error("Errore aggiornamento menu ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(APIStatus.ERROR, "Errore nell'aggiornamento del menu"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<Void>> deleteMenu(@PathVariable Long id) {
        try {
            menuService.deleteMenu(id);
            return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS, "Menu eliminato con successo"));
        } catch (Exception e) {
            log.error("Errore eliminazione menu ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(APIStatus.ERROR, "Errore nell'eliminazione del menu"));
        }
    }
}