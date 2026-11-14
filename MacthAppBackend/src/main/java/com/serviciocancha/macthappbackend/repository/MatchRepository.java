package com.serviciocancha.macthappbackend.repository;

import com.serviciocancha.macthappbackend.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface MatchRepository extends JpaRepository<Match, UUID> {
    boolean existsByUser1IdAndUser2Id(UUID user1Id, UUID user2Id);
}
