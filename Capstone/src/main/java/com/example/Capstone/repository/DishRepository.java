// =================================================================
// CREA/AGGIORNA Capstone/src/main/java/com/example/Capstone/repository/DishRepository.java
// =================================================================

package com.example.Capstone.repository;

import com.example.Capstone.entity.Dish;
import com.example.Capstone.entity.DishCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DishRepository extends JpaRepository<Dish, Long> {

    /**
     * Trova tutti i piatti di un menu specifico
     */
    List<Dish> findByMenuIdOrderByDisplayOrderAscNameAsc(Long menuId);

    /**
     * Trova piatti per categoria in un menu specifico
     */
    List<Dish> findByMenuIdAndCategoryOrderByDisplayOrderAscNameAsc(Long menuId, DishCategory category);

    /**
     * Trova piatti disponibili di un menu
     */
    List<Dish> findByMenuIdAndIsAvailableTrueOrderByDisplayOrderAscNameAsc(Long menuId);

    /**
     * Elimina tutti i piatti di un menu specifico
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Dish d WHERE d.menu.id = :menuId")
    void deleteByMenuId(@Param("menuId") Long menuId);

    /**
     * Conta i piatti per categoria in un menu
     */
    @Query("SELECT COUNT(d) FROM Dish d WHERE d.menu.id = :menuId AND d.category = :category")
    long countByMenuIdAndCategory(@Param("menuId") Long menuId, @Param("category") DishCategory category);

    /**
     * Trova piatti per nome (ricerca parziale)
     */
    @Query("SELECT d FROM Dish d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Dish> findByNameContainingIgnoreCase(@Param("name") String name);
}