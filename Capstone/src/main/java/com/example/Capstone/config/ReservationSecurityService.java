package com.example.Capstone.config;

import com.example.Capstone.entity.Role;
import com.example.Capstone.entity.User;
import com.example.Capstone.exception.UnauthorizedOperationException;
import com.example.Capstone.repository.TableReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationSecurityService {

    private final TableReservationRepository reservationRepository;

    public boolean isOwner(Long reservationId, User user) {
        boolean isOwner = reservationRepository.existsByIdAndUser_Id(reservationId, user.getId());
        if (!isOwner) {
            throw new UnauthorizedOperationException("Non sei autorizzato a modificare questa prenotazione");
        }
        return true;
    }

    public boolean canAccessReservation(Long reservationId, User user) {
        if (user.getRuolo() == Role.ADMIN) {
            return true;
        }

        boolean isOwner = reservationRepository.existsByIdAndUser_Id(reservationId, user.getId());
        if (!isOwner) {
            throw new UnauthorizedOperationException("Non sei autorizzato ad accedere a questa prenotazione");
        }
        return true;
    }

    public boolean isOwnerOrAdmin(Long reservationId, User user) {
        if (user.getRuolo() == Role.ADMIN) {
            return true;
        }

        return isOwner(reservationId, user);
    }
}