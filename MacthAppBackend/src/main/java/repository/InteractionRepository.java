package repository;

import model.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface InteractionRepository extends JpaRepository<Interaction, UUID> {
    boolean existsByUserIdAndTargetUserIdAndType(UUID userId, UUID targetUserId, String type);
}