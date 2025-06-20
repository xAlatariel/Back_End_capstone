package com.example.Capstone.repository;

import com.example.Capstone.entity.Dish;
import com.example.Capstone.entity.DishCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DishRepository extends JpaRepository<Dish, Long> {

    // Trova piatti per menu
    List<Dish> findByMenuIdOrderByDisplayOrderAscNameAsc(Long menuId);

    // Trova piatti per categoria e menu
    List<Dish> findByMenuIdAndCategoryOrderByDisplayOrderAscNameAsc(Long menuId, DishCategory category);

    // Trova piatti disponibili per menu
    @Query("SELECT d FROM Dish d WHERE d.menu.id = :menuId AND d.isAvailable = true ORDER BY d.displayOrder ASC, d.name ASC")
    List<Dish> findAvailableDishesByMenu(@Param("menuId") Long menuId);

    // Conta piatti per categoria in un menu
    long countByMenuIdAndCategory(Long menuId, DishCategory category);
}