package com.example.Capstone.service;

import com.example.Capstone.dto.TableReservationRequestDTO;
import com.example.Capstone.dto.TableReservationResponseDTO;
import com.example.Capstone.entity.ReservationArea;
import com.example.Capstone.entity.TableReservation;
import com.example.Capstone.entity.User;
import com.example.Capstone.exception.*;
import com.example.Capstone.repository.TableReservationRepository;
import com.example.Capstone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TableReservationService {



    @Autowired
    private final TableReservationRepository reservationRepository;

    @Autowired
    private final UserRepository userRepository;

    private static final int MAX_INDOOR_CAPACITY = 60;
    private static final int MAX_OUTDOOR_CAPACITY = 40;

    // CREATE
    public TableReservationResponseDTO createReservation(Long userId, TableReservationRequestDTO request)
            throws UserNotFoundException, CapacityExceededException {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        ReservationArea area = ReservationArea.valueOf(request.getReservationArea().toUpperCase());
        LocalDate date = request.getReservationDate();

        int reservedSeats = reservationRepository.countReservedSeatsByAreaAndDate(area, date, date);
        int maxCapacity = getMaxCapacityForArea(area);

        if (reservedSeats + request.getNumberOfPeople() > maxCapacity) {
            throw new CapacityExceededException(area.name(), date, maxCapacity);
        }

        TableReservation reservation = new TableReservation();
        reservation.setReservationDate(date);
        reservation.setReservationTime(request.getReservationTime());
        reservation.setNumberOfPeople(request.getNumberOfPeople());
        reservation.setReservationArea(area);
        reservation.setUser(user);

        return convertToDTO(reservationRepository.save(reservation));
    }

    // READ
    public TableReservationResponseDTO getReservationById(Long id) throws ReservationNotFoundException {
        return reservationRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new ReservationNotFoundException(id));
    }

    public List<TableReservationResponseDTO> getAllReservations() {
        return reservationRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // UPDATE
    public TableReservationResponseDTO updateReservation(Long id, TableReservationRequestDTO request)
            throws ReservationNotFoundException, CapacityExceededException {

        TableReservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));

        ReservationArea newArea = ReservationArea.valueOf(request.getReservationArea().toUpperCase());
        LocalDate newDate = request.getReservationDate();

        if (!reservation.getReservationDate().equals(newDate) ||
                !reservation.getReservationArea().equals(newArea) ||
                reservation.getNumberOfPeople() != request.getNumberOfPeople()) {

            int currentBookedSeats = reservationRepository.countReservedSeatsByAreaAndDate(
                    reservation.getReservationArea(), reservation.getReservationDate(), reservation.getReservationDate());
            int adjustedBookedSeats = currentBookedSeats - reservation.getNumberOfPeople();

            int maxCapacity = getMaxCapacityForArea(newArea);
            if ((adjustedBookedSeats + request.getNumberOfPeople()) > maxCapacity) {
                throw new CapacityExceededException(newArea.name(), newDate, maxCapacity);
            }
        }

        reservation.setReservationDate(newDate);
        reservation.setReservationTime(request.getReservationTime());
        reservation.setNumberOfPeople(request.getNumberOfPeople());
        reservation.setReservationArea(newArea);

        return convertToDTO(reservationRepository.save(reservation));
    }

    // DELETE
    public void deleteReservation(Long id) throws ReservationNotFoundException {
        if (!reservationRepository.existsById(id)) {
            throw new ReservationNotFoundException(id);
        }
        reservationRepository.deleteById(id);
    }

    // GET BY DATE
    public List<TableReservationResponseDTO> getReservationsByDate(LocalDate date) {
        return reservationRepository.findByReservationDate(date)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<TableReservationResponseDTO> getReservationsByUserId(Long userId) {
        return reservationRepository.findByUserId(userId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    // HELPER METHODS
    private void checkCapacity(LocalDate date, ReservationArea area, int numberOfPeople)
            throws CapacityExceededException {

        int reservedSeats = reservationRepository.countReservedSeatsByAreaAndDate(area, date, date);
        int maxCapacity = getMaxCapacityForArea(area);

        if ((reservedSeats + numberOfPeople) > maxCapacity) {
            throw new CapacityExceededException(area.name(), date, maxCapacity);
        }
    }

    private int getMaxCapacityForArea(ReservationArea area) {
        return area == ReservationArea.INDOOR ? MAX_INDOOR_CAPACITY : MAX_OUTDOOR_CAPACITY;
    }

    private TableReservationResponseDTO convertToDTO(TableReservation reservation) {
        TableReservationResponseDTO dto = new TableReservationResponseDTO();
        dto.setId(reservation.getId());
        dto.setReservationDate(reservation.getReservationDate());
        dto.setReservationTime(reservation.getReservationTime());
        dto.setNumberOfPeople(reservation.getNumberOfPeople());
        dto.setReservationArea(reservation.getReservationArea());
        dto.setUserId(reservation.getUser().getId());
        dto.setUserFullName(
                reservation.getUser().getNome() + " " + reservation.getUser().getCognome()
        );
        return dto;
    }
}