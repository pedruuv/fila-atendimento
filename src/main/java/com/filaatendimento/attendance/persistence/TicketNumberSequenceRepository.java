package com.filaatendimento.attendance.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/** Persistence of the single ticket number sequence row. */
public interface TicketNumberSequenceRepository extends JpaRepository<TicketNumberSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TicketNumberSequence s where s.id = " + TicketNumberSequence.SINGLE_ROW_ID)
    Optional<TicketNumberSequence> lockSingleRow();
}
