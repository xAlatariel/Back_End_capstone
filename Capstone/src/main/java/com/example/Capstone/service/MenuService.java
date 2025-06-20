// =================================================================
// SOSTITUZIONE COMPLETA per Capstone/src/main/java/com/example/Capstone/service/MenuService.java
// =================================================================

package com.example.Capstone.service;

import com.example.Capstone.dto.*;
import com.example.Capstone.entity.*;
import com.example.Capstone.exception.MenuNotFoundException;
import com.example.Capstone.repository.MenuRepository;
import com.example.Capstone.repository.DishRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MenuService {

    private final MenuRepository menuRepository;
    private final DishRepository dishRepository;

    // ===================================================================
    // METODI PUBBLICI
    // ===================================================================

    /**
     * Recupera tutti i menu attivi
     */
    @Transactional(readOnly = true)
    public List<MenuResponseDTO> getAllActiveMenus() {
        log.info("Recupero tutti i menu attivi");
        List<Menu> activeMenus = menuRepository.findByIsActiveTrueOrderByCreatedAtDesc();
        return activeMenus.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Recupera il menu del giorno corrente
     */
    @Transactional(readOnly = true)
    public MenuResponseDTO getTodaysDailyMenu() {
        log.info("Recupero menu del giorno per oggi: {}", LocalDate.now());
        LocalDate today = LocalDate.now();

        Menu menu = menuRepository.findByMenuTypeAndMenuDateAndIsActiveTrue(MenuType.DAILY, today)
                .orElseThrow(() -> new MenuNotFoundException("Nessun menu del giorno disponibile per oggi"));

        return convertToResponseDTO(menu);
    }

    /**
     * Recupera il menu del giorno per una data specifica
     */
    @Transactional(readOnly = true)
    public MenuResponseDTO getDailyMenuByDate(LocalDate date) {
        log.info("Recupero menu del giorno per la data: {}", date);

        Menu menu = menuRepository.findByMenuTypeAndMenuDateAndIsActiveTrue(MenuType.DAILY, date)
                .orElseThrow(() -> new MenuNotFoundException("Nessun menu del giorno disponibile per la data: " + date));

        return convertToResponseDTO(menu);
    }

    /**
     * Recupera il menu stagionale corrente
     */
    @Transactional(readOnly = true)
    public MenuResponseDTO getCurrentSeasonalMenu() {
        log.info("Recupero menu stagionale corrente");

        Menu menu = menuRepository.findCurrentSeasonalMenu()
                .orElseThrow(() -> new MenuNotFoundException("Nessun menu stagionale disponibile"));

        return convertToResponseDTO(menu);
    }

    // ===================================================================
    // METODI ADMIN
    // ===================================================================

    /**
     * Recupera tutti i menu (admin)
     */
    @Transactional(readOnly = true)
    public List<MenuResponseDTO> getAllMenus() {
        log.info("Recupero tutti i menu (admin)");
        List<Menu> menus = menuRepository.findAllByOrderByCreatedAtDesc();
        return menus.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Recupera un menu per ID (admin)
     */
    @Transactional(readOnly = true)
    public MenuResponseDTO getMenuById(Long id) {
        log.info("Recupero menu con ID: {}", id);
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new MenuNotFoundException("Menu non trovato con ID: " + id));

        return convertToResponseDTO(menu);
    }

    /**
     * Crea un nuovo menu
     */
    public MenuResponseDTO createMenu(MenuRequestDTO request) {
        log.info("Creazione nuovo menu: {}", request.name());

        validateMenuRequest(request);

        // Controlla se esiste già un menu del giorno per la data
        if (request.menuType() == MenuType.DAILY && request.menuDate() != null) {
            boolean exists = menuRepository.existsByMenuTypeAndMenuDate(MenuType.DAILY, request.menuDate());
            if (exists) {
                throw new IllegalArgumentException("Esiste già un menu del giorno per la data: " + request.menuDate());
            }
        }

        Menu menu = Menu.builder()
                .name(request.name())
                .description(request.description())
                .menuType(request.menuType())
                .menuDate(request.menuDate())
                .isActive(request.isActive() != null ? request.isActive() : false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Menu savedMenu = menuRepository.save(menu);

        // Crea i piatti associati
        List<Dish> dishes = createDishesFromRequest(request.dishes(), savedMenu);
        savedMenu.setDishes(dishes);

        log.info("Menu creato con successo: {}", savedMenu.getName());
        return convertToResponseDTO(savedMenu);
    }

    /**
     * Aggiorna un menu esistente
     */
    public MenuResponseDTO updateMenu(Long id, MenuRequestDTO request) {
        log.info("Aggiornamento menu ID {}: {}", id, request.name());

        validateMenuRequest(request);

        Menu existingMenu = menuRepository.findById(id)
                .orElseThrow(() -> new MenuNotFoundException("Menu non trovato con ID: " + id));

        // Aggiorna i campi del menu
        existingMenu.setName(request.name());
        existingMenu.setDescription(request.description());
        existingMenu.setMenuDate(request.menuDate());
        existingMenu.setIsActive(request.isActive() != null ? request.isActive() : existingMenu.getIsActive());
        existingMenu.setUpdatedAt(LocalDateTime.now());

        // Rimuovi i piatti esistenti
        dishRepository.deleteByMenuId(id);

        // Crea i nuovi piatti
        List<Dish> dishes = createDishesFromRequest(request.dishes(), existingMenu);
        existingMenu.setDishes(dishes);

        Menu savedMenu = menuRepository.save(existingMenu);

        log.info("Menu aggiornato con successo: {}", savedMenu.getName());
        return convertToResponseDTO(savedMenu);
    }

    /**
     * Elimina un menu
     */
    public void deleteMenu(Long id) {
        log.info("Eliminazione menu ID: {}", id);

        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new MenuNotFoundException("Menu non trovato con ID: " + id));

        // Elimina prima i piatti associati
        dishRepository.deleteByMenuId(id);

        // Poi elimina il menu
        menuRepository.delete(menu);

        log.info("Menu eliminato con successo: {}", menu.getName());
    }

    /**
     * Attiva/Disattiva un menu
     */
    public MenuResponseDTO toggleMenuStatus(Long id, Boolean isActive) {
        log.info("Modifica stato menu ID {}: {}", id, isActive);

        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new MenuNotFoundException("Menu non trovato con ID: " + id));

        menu.setIsActive(isActive);
        menu.setUpdatedAt(LocalDateTime.now());

        Menu savedMenu = menuRepository.save(menu);

        log.info("Stato menu modificato: {} -> {}", menu.getName(), isActive);
        return convertToResponseDTO(savedMenu);
    }

    /**
     * Duplica un menu esistente
     */
    public MenuResponseDTO duplicateMenu(Long id) {
        log.info("Duplicazione menu ID: {}", id);

        Menu originalMenu = menuRepository.findById(id)
                .orElseThrow(() -> new MenuNotFoundException("Menu non trovato con ID: " + id));

        // Crea il nuovo menu duplicato
        Menu duplicatedMenu = Menu.builder()
                .name(originalMenu.getName() + " - Copia")
                .description(originalMenu.getDescription())
                .menuType(originalMenu.getMenuType())
                .menuDate(originalMenu.getMenuDate())
                .isActive(false) // Il duplicato è sempre inattivo inizialmente
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Menu savedMenu = menuRepository.save(duplicatedMenu);

        // Duplica i piatti
        List<Dish> duplicatedDishes = originalMenu.getDishes().stream()
                .map(dish -> Dish.builder()
                        .name(dish.getName())
                        .description(dish.getDescription())
                        .ingredients(dish.getIngredients())
                        .category(dish.getCategory())
                        .price(dish.getPrice())
                        .isAvailable(dish.getIsAvailable())
                        .displayOrder(dish.getDisplayOrder())
                        .menu(savedMenu)
                        .build())
                .map(dishRepository::save)
                .collect(Collectors.toList());

        savedMenu.setDishes(duplicatedDishes);

        log.info("Menu duplicato con successo: {}", savedMenu.getName());
        return convertToResponseDTO(savedMenu);
    }

    /**
     * Statistiche sui menu
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMenuStats() {
        log.info("Recupero statistiche menu");

        Map<String, Object> stats = new HashMap<>();

        // Conteggi generali
        stats.put("totalMenus", menuRepository.count());
        stats.put("activeMenus", menuRepository.countByIsActiveTrue());
        stats.put("dailyMenus", menuRepository.countByMenuType(MenuType.DAILY));
        stats.put("seasonalMenus", menuRepository.countByMenuType(MenuType.SEASONAL));

        // Menu più recenti
        List<Menu> recentMenus = menuRepository.findTop5ByOrderByCreatedAtDesc();
        stats.put("recentMenus", recentMenus.stream()
                .map(menu -> Map.of(
                        "id", menu.getId(),
                        "name", menu.getName(),
                        "type", menu.getMenuType(),
                        "isActive", menu.getIsActive(),
                        "createdAt", menu.getCreatedAt()
                ))
                .collect(Collectors.toList()));

        return stats;
    }

    // ===================================================================
    // METODI DI SUPPORTO E VALIDAZIONE
    // ===================================================================

    private void validateMenuRequest(MenuRequestDTO request) {
        if (request.name() == null || request.name().trim().length() < 3) {
            throw new IllegalArgumentException("Il nome del menu deve essere di almeno 3 caratteri");
        }

        if (request.menuType() == null) {
            throw new IllegalArgumentException("Il tipo di menu è obbligatorio");
        }

        if (request.dishes() == null || request.dishes().isEmpty()) {
            throw new IllegalArgumentException("Il menu deve contenere almeno un piatto");
        }

        // Validazione specifica per menu del giorno
        if (request.menuType() == MenuType.DAILY) {
            validateDailyMenuStructure(request.dishes());
        }
    }

    private void validateDailyMenuStructure(List<DishRequestDTO> dishes) {
        long primiCount = dishes.stream().filter(d -> d.category() == DishCategory.PRIMI).count();
        long secondiCount = dishes.stream().filter(d -> d.category() == DishCategory.SECONDI).count();
        long contorniCount = dishes.stream().filter(d -> d.category() == DishCategory.CONTORNI).count();

        if (primiCount != 3) {
            throw new IllegalArgumentException("Il menu del giorno deve avere esattamente 3 primi piatti");
        }
        if (secondiCount != 3) {
            throw new IllegalArgumentException("Il menu del giorno deve avere esattamente 3 secondi piatti");
        }
        if (contorniCount != 3) {
            throw new IllegalArgumentException("Il menu del giorno deve avere esattamente 3 contorni");
        }
    }

    private List<Dish> createDishesFromRequest(List<DishRequestDTO> dishRequests, Menu menu) {
        return dishRequests.stream()
                .map(dishRequest -> Dish.builder()
                        .name(dishRequest.name())
                        .description(dishRequest.description())
                        .ingredients(dishRequest.ingredients())
                        .category(dishRequest.category())
                        .price(dishRequest.price())
                        .isAvailable(dishRequest.isAvailable() != null ? dishRequest.isAvailable() : true)
                        .displayOrder(dishRequest.displayOrder())
                        .menu(menu)
                        .build())
                .map(dishRepository::save)
                .collect(Collectors.toList());
    }

    private MenuResponseDTO convertToResponseDTO(Menu menu) {
        List<DishResponseDTO> dishDTOs = menu.getDishes().stream()
                .sorted(Comparator.comparing(Dish::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Dish::getName))
                .map(this::convertDishToResponseDTO)
                .collect(Collectors.toList());

        return new MenuResponseDTO(
                menu.getId(),
                menu.getName(),
                menu.getDescription(),
                menu.getMenuType(),
                menu.getMenuDate(),
                menu.getIsActive(),
                menu.getCreatedAt(),
                menu.getUpdatedAt(),
                dishDTOs
        );
    }

    private DishResponseDTO convertDishToResponseDTO(Dish dish) {
        return new DishResponseDTO(
                dish.getId(),
                dish.getName(),
                dish.getDescription(),
                dish.getIngredients(),
                dish.getCategory(),
                dish.getPrice(),
                dish.getFormattedPrice(),
                dish.getIsAvailable(),
                dish.getDisplayOrder()
        );
    }
}