// =================================================================
// CREA/AGGIORNA Capstone/src/main/java/com/example/Capstone/repository/MenuRepository.java
// =================================================================

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

    // ===================================================================
    // QUERY PER MENU ATTIVI
    // ===================================================================

    /**
     * Trova tutti i menu attivi ordinati per data di creazione
     */
    List<Menu> findByIsActiveTrueOrderByCreatedAtDesc();

    /**
     * Trova menu per tipo e data specifica (attivi)
     */
    Optional<Menu> findByMenuTypeAndMenuDateAndIsActiveTrue(MenuType menuType, LocalDate menuDate);

    /**
     * Trova il menu stagionale corrente attivo
     */
    @Query("SELECT m FROM Menu m WHERE m.menuType = 'SEASONAL' AND m.isActive = true ORDER BY m.createdAt DESC")
    Optional<Menu> findCurrentSeasonalMenu();

    // ===================================================================
    // QUERY ADMIN
    // ===================================================================

    /**
     * Trova tutti i menu ordinati per data di creazione (più recenti prima)
     */
    List<Menu> findAllByOrderByCreatedAtDesc();

    /**
     * Trova tutti i menu per tipo
     */
    List<Menu> findByMenuTypeOrderByCreatedAtDesc(MenuType menuType);

    /**
     * Trova menu attivi per tipo
     */
    List<Menu> findByMenuTypeAndIsActiveTrueOrderByCreatedAtDesc(MenuType menuType);

    // ===================================================================
    // QUERY STATISTICHE
    // ===================================================================

    /**
     * Conta i menu attivi
     */
    long countByIsActiveTrue();

    /**
     * Conta i menu per tipo
     */
    long countByMenuType(MenuType menuType);

    /**
     * Trova i 5 menu più recenti
     */
    List<Menu> findTop5ByOrderByCreatedAtDesc();

    /**
     * Trova menu per data range
     */
    @Query("SELECT m FROM Menu m WHERE m.menuDate BETWEEN :startDate AND :endDate ORDER BY m.menuDate DESC")
    List<Menu> findByMenuDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Verifica se esiste già un menu del giorno per una data specifica
     */
    boolean existsByMenuTypeAndMenuDate(MenuType menuType, LocalDate menuDate);

    /**
     * Trova menu del giorno per il mese corrente
     */
    @Query("SELECT m FROM Menu m WHERE m.menuType = 'DAILY' " +
            "AND YEAR(m.menuDate) = :year AND MONTH(m.menuDate) = :month " +
            "ORDER BY m.menuDate ASC")
    List<Menu> findDailyMenusByMonth(@Param("year") int year, @Param("month") int month);
}