package com.serviciocancha.macthappbackend.repository;

import com.serviciocancha.macthappbackend.model.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface InteractionRepository extends JpaRepository<Interaction, UUID> {
    boolean existsByUserIdAndTargetUserId(UUID userId, UUID targetUserId);
    boolean existsByUserIdAndTargetUserIdAndType(UUID userId, UUID targetUserId, String type);
}