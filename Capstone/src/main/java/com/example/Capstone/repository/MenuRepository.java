package com.example.Capstone.repository;

import com.example.Capstone.entity.DishCategory;
import com.example.Capstone.entity.Menu;
import com.example.Capstone.entity.MenuType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {

    // Trova menu attivi per tipo
    List<Menu> findByMenuTypeAndIsActiveTrue(MenuType menuType);

    // Trova menu del giorno per data specifica
    Optional<Menu> findByMenuTypeAndMenuDateAndIsActiveTrue(MenuType menuType, LocalDate date);

    // Trova menu del giorno corrente
    @Query("SELECT m FROM Menu m WHERE m.menuType = 'DAILY' AND m.menuDate = :today AND m.isActive = true")
    Optional<Menu> findTodaysDailyMenu(@Param("today") LocalDate today);

    // Trova menu stagionale attivo (ultimo creato)
    @Query("SELECT m FROM Menu m WHERE m.menuType = 'SEASONAL' AND m.isActive = true ORDER BY m.createdAt DESC LIMIT 1")
    Optional<Menu> findCurrentSeasonalMenu();

    // Trova tutti i menu attivi ordinati per data
    @Query("SELECT m FROM Menu m WHERE m.isActive = true ORDER BY m.menuType, m.menuDate DESC, m.createdAt DESC")
    List<Menu> findAllActiveMenusOrdered();

    // Disattiva vecchi menu giornalieri
    @Query("UPDATE Menu m SET m.isActive = false WHERE m.menuType = 'DAILY' AND m.menuDate < :date")
    int deactivateExpiredDailyMenus(@Param("date") LocalDate date);

    // Controlla se esiste già un menu del giorno per una data
    boolean existsByMenuTypeAndMenuDate(MenuType menuType, LocalDate date);

    // ===================================================================
// QUERY MANCANTI NEL MenuRepository - DA IMPLEMENTARE
// ===================================================================

    // 1. RANGE QUERIES - MANCANTI
    List<Menu> findByMenuDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT m FROM Menu m WHERE m.menuDate >= :startDate AND m.menuDate <= :endDate ORDER BY m.menuDate DESC")
    List<Menu> findByMenuDateBetweenOrderByMenuDateDesc(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // 2. SEARCH QUERIES - MANCANTI
    List<Menu> findByNameContainingIgnoreCase(String name);

    @Query("SELECT m FROM Menu m WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(m.description) LIKE LOWER(CONCAT('%', :description, '%'))")
    List<Menu> findByNameOrDescriptionContaining(@Param("name") String name, @Param("description") String description);

    // 3. FILTERING QUERIES - MANCANTI
    @Query("SELECT m FROM Menu m WHERE m.menuType = :type AND m.isActive = :active ORDER BY m.createdAt DESC")
    List<Menu> findByMenuTypeAndActiveStatus(@Param("type") MenuType type, @Param("active") Boolean active);

    // 4. STATISTICS QUERIES - MANCANTI
    @Query("SELECT COUNT(m) FROM Menu m WHERE m.isActive = true")
    long countActiveMenus();

    @Query("SELECT COUNT(m) FROM Menu m WHERE m.menuType = :type")
    long countByMenuType(@Param("type") MenuType type);

    @Query("SELECT m.menuType, COUNT(m) FROM Menu m GROUP BY m.menuType")
    List<Object[]> countMenusByType();

    @Query("SELECT COUNT(m) FROM Menu m WHERE m.createdAt >= :since")
    long countMenusCreatedSince(@Param("since") LocalDateTime since);

    // 5. CLEANUP QUERIES - MANCANTI
    @Modifying
    @Query("DELETE FROM Menu m WHERE m.isActive = false AND m.createdAt < :cutoffDate")
    int deleteInactiveMenusOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Query("UPDATE Menu m SET m.isActive = false WHERE m.menuType = 'SEASONAL' AND m.createdAt < :cutoffDate")
    int deactivateOldSeasonalMenus(@Param("cutoffDate") LocalDateTime cutoffDate);

    // 6. ADVANCED MENU QUERIES - MANCANTI
    @Query("SELECT DISTINCT m FROM Menu m JOIN FETCH m.dishes d WHERE d.category = :category AND m.isActive = true")
    List<Menu> findActiveMenusWithDishCategory(@Param("category") DishCategory category);

    @Query("SELECT m FROM Menu m WHERE m.isActive = true AND SIZE(m.dishes) >= :minDishes")
    List<Menu> findActiveMenusWithMinimumDishes(@Param("minDishes") int minDishes);

    // 7. PERFORMANCE QUERIES - MANCANTI
    @Query("SELECT m FROM Menu m LEFT JOIN FETCH m.dishes WHERE m.id = :id")
    Optional<Menu> findByIdWithDishes(@Param("id") Long id);

    @Query("SELECT m FROM Menu m LEFT JOIN FETCH m.dishes WHERE m.isActive = true ORDER BY m.menuType, m.menuDate DESC")
    List<Menu> findAllActiveMenusWithDishes();
}