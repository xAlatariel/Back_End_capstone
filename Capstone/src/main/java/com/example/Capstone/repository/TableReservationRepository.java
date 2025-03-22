package com.example.Capstone.repository;

import com.example.Capstone.entity.ReservationArea;
import com.example.Capstone.entity.TableReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface TableReservationRepository extends JpaRepository<TableReservation, Long> {

    boolean existsByIdAndUser_Id(Long reservationId, Long userId);

    Optional<TableReservation> findByReservationArea(ReservationArea reservationArea);

    List<TableReservation> findByReservationDate(LocalDate date);


    @Query("SELECT COALESCE(SUM(t.numberOfPeople), 0) " +
            "FROM TableReservation t " +
            "WHERE t.reservationArea = :area " +
            "AND t.reservationDate BETWEEN :startDate AND :endDate")
    int countReservedSeatsByAreaAndDate(
            @Param("area") ReservationArea area,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT t FROM TableReservation t WHERE t.reservationDate BETWEEN :startDate AND :endDate")
    List<TableReservation> findByReservationDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}