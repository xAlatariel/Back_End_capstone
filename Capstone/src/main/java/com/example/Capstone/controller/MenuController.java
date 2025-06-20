// =================================================================
// SOSTITUZIONE per MenuController con import corretti
// =================================================================

package com.example.Capstone.controller;

import com.example.Capstone.dto.APIResponse;
import com.example.Capstone.dto.APIStatus;
import com.example.Capstone.dto.MenuRequestDTO;
import com.example.Capstone.dto.MenuResponseDTO;
import com.example.Capstone.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    // ===================================================================
    // ENDPOINT PUBBLICI
    // ===================================================================

    @GetMapping("/menus/active")
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

    @GetMapping("/menus/daily/today")
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

    @GetMapping("/menus/daily/date/{date}")
    public ResponseEntity<APIResponse<MenuResponseDTO>> getDailyMenuByDate(@PathVariable String date) {
        try {
            LocalDate targetDate = LocalDate.parse(date);
            MenuResponseDTO menu = menuService.getDailyMenuByDate(targetDate);
            return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS, "Menu recuperato", menu));
        } catch (Exception e) {
            log.error("Errore recupero menu per data: {}", date, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new APIResponse<>(APIStatus.ERROR, "Nessun menu disponibile per la data: " + date));
        }
    }

    @GetMapping("/menus/seasonal/current")
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

    @GetMapping("/admin/menus")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<List<MenuResponseDTO>>> getAllMenus() {
        try {
            List<MenuResponseDTO> menus = menuService.getAllMenus();
            return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS, "Tutti i menu recuperati", menus));
        } catch (Exception e) {
            log.error("Errore recupero tutti i menu", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(APIStatus.ERROR, "Errore nel recupero dei menu"));
        }
    }

    @GetMapping("/admin/menus/{id}")
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

    @PostMapping("/admin/menus")
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

    @PutMapping("/admin/menus/{id}")
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

    @DeleteMapping("/admin/menus/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<String>> deleteMenu(@PathVariable Long id) {
        try {
            menuService.deleteMenu(id);
            return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS, "Menu eliminato con successo", null));
        } catch (Exception e) {
            log.error("Errore eliminazione menu ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(APIStatus.ERROR, "Errore nell'eliminazione del menu"));
        }
    }

    @PatchMapping("/admin/menus/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<MenuResponseDTO>> toggleMenuStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        try {
            Boolean isActive = request.get("isActive");
            MenuResponseDTO menu = menuService.toggleMenuStatus(id, isActive);
            return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS, "Stato menu modificato", menu));
        } catch (Exception e) {
            log.error("Errore modifica stato menu ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(APIStatus.ERROR, "Errore nella modifica dello stato"));
        }
    }

    @PostMapping("/admin/menus/{id}/duplicate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<MenuResponseDTO>> duplicateMenu(@PathVariable Long id) {
        try {
            MenuResponseDTO menu = menuService.duplicateMenu(id);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new APIResponse<>(APIStatus.SUCCESS, "Menu duplicato con successo", menu));
        } catch (Exception e) {
            log.error("Errore duplicazione menu ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(APIStatus.ERROR, "Errore nella duplicazione del menu"));
        }
    }

    @GetMapping("/admin/menus/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<Map<String, Object>>> getMenuStats() {
        try {
            Map<String, Object> stats = menuService.getMenuStats();
            return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS, "Statistiche recuperate", stats));
        } catch (Exception e) {
            log.error("Errore recupero statistiche menu", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(APIStatus.ERROR, "Errore nel recupero delle statistiche"));
        }
    }
}