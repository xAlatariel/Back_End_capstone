package com.example.Capstone.service;

import com.example.Capstone.dto.*;
import com.example.Capstone.entity.*;
import com.example.Capstone.exception.*;
import com.example.Capstone.repository.MenuRepository;
import com.example.Capstone.repository.DishRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final DishRepository dishRepository;

    // ===================================================================
    // CREAZIONE MENU
    // ===================================================================
    public MenuResponseDTO createMenu(MenuRequestDTO request) {
        validateMenuRequest(request);

        // Controlla se esiste già un menu del giorno per la data
        if (request.menuType() == MenuType.DAILY) {
            LocalDate menuDate = request.menuDate() != null ? request.menuDate() : LocalDate.now();
            if (menuRepository.existsByMenuTypeAndMenuDate(MenuType.DAILY, menuDate)) {
                throw new IllegalArgumentException("Esiste già un menu del giorno per la data: " + menuDate);
            }
        }

        Menu menu = Menu.builder()
                .name(request.name())
                .description(request.description())
                .menuType(request.menuType())
                .menuDate(request.menuDate())
                .isActive(true)
                .build();

        menu = menuRepository.save(menu);

        // Crea i piatti
        List<Dish> dishes = createDishesFromRequest(request.dishes(), menu);
        menu.setDishes(dishes);

        log.info("Menu creato con successo: {} (tipo: {})", menu.getName(), menu.getMenuType());
        return convertToResponseDTO(menu);
    }

    // ===================================================================
    // LETTURA MENU
    // ===================================================================
    public List<MenuResponseDTO> getAllActiveMenus() {
        return menuRepository.findAllActiveMenusOrdered()
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public MenuResponseDTO getTodaysDailyMenu() {
        return menuRepository.findTodaysDailyMenu(LocalDate.now())
                .map(this::convertToResponseDTO)
                .orElseThrow(() -> new MenuNotFoundException("Nessun menu del giorno disponibile per oggi"));
    }

    public MenuResponseDTO getCurrentSeasonalMenu() {
        return menuRepository.findCurrentSeasonalMenu()
                .map(this::convertToResponseDTO)
                .orElseThrow(() -> new MenuNotFoundException("Nessun menu stagionale disponibile"));
    }

    public MenuResponseDTO getMenuById(Long id) {
        return menuRepository.findById(id)
                .map(this::convertToResponseDTO)
                .orElseThrow(() -> new MenuNotFoundException("Menu non trovato con ID: " + id));
    }

    // ===================================================================
    // AGGIORNAMENTO MENU
    // ===================================================================
    public MenuResponseDTO updateMenu(Long id, MenuRequestDTO request) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new MenuNotFoundException("Menu non trovato con ID: " + id));

        validateMenuRequest(request);

        menu.setName(request.name());
        menu.setDescription(request.description());

        // Non permettere cambio di tipo menu
        if (!menu.getMenuType().equals(request.menuType())) {
            throw new IllegalArgumentException("Non è possibile cambiare il tipo di menu");
        }

        // Rimuovi piatti esistenti
        dishRepository.deleteAll(menu.getDishes());
        menu.getDishes().clear();

        // Crea nuovi piatti
        List<Dish> newDishes = createDishesFromRequest(request.dishes(), menu);
        menu.setDishes(newDishes);

        menu = menuRepository.save(menu);
        log.info("Menu aggiornato: {}", menu.getName());

        return convertToResponseDTO(menu);
    }

    // ===================================================================
    // ELIMINAZIONE MENU
    // ===================================================================
    public void deleteMenu(Long id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new MenuNotFoundException("Menu non trovato con ID: " + id));

        menu.setIsActive(false);
        menuRepository.save(menu);

        log.info("Menu disattivato: {}", menu.getName());
    }

    // ===================================================================
    // SCHEDULED TASKS
    // ===================================================================
    @Scheduled(cron = "0 0 6 * * *") // Ogni giorno alle 6:00
    public void cleanupExpiredDailyMenus() {
        try {
            int deactivatedCount = menuRepository.deactivateExpiredDailyMenus(LocalDate.now());
            log.info("Disattivati {} menu giornalieri scaduti", deactivatedCount);
        } catch (Exception e) {
            log.error("Errore durante la pulizia dei menu scaduti", e);
        }
    }

    // ===================================================================
    // METODI HELPER PRIVATI
    // ===================================================================
    private void validateMenuRequest(MenuRequestDTO request) {
        if (request.menuType() == MenuType.DAILY) {
            validateDailyMenuStructure(request.dishes());
        }
        // Validazioni aggiuntive per menu stagionale se necessarie
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