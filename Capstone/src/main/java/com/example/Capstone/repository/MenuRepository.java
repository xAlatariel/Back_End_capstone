package com.example.Capstone.repository;

import com.example.Capstone.entity.Menu;
import com.example.Capstone.entity.MenuType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
}