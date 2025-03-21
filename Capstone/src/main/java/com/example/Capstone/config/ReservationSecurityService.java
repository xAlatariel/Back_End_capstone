package com.example.Capstone.config;

import com.example.Capstone.entity.Role;
import com.example.Capstone.entity.User;
import com.example.Capstone.repository.TableReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationSecurityService {

    private final TableReservationRepository reservationRepository;

    public boolean isOwner(Long reservationId, User user) {
        return reservationRepository.existsByIdAndUser_Id (reservationId, user.getId());
    }

    public boolean canAccessReservation(Long reservationId, User user) {
        return user.getRuolo() == Role.ADMIN || isOwner(reservationId, user);
    }

    public boolean isOwnerOrAdmin(Long reservationId, User user) {
        return user.getRuolo() == Role.ADMIN || isOwner(reservationId, user);
    }
}