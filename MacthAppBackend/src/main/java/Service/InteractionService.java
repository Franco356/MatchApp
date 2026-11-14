package Service;

import model.Interaction;
import model.Match;
import org.aspectj.apache.bcel.classfile.Module;
import repository.InteractionRepository;
import repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InteractionService {

    private final InteractionRepository interactionRepository;
    private final MatchRepository matchRepository;

    public boolean registerInteraction(UUID userId, UUID targetUserId, String type) {
        Interaction interaction = new Interaction();
        interaction.setUserId(userId);
        interaction.setTargetUserId(targetUserId);
        interaction.setType(type);
        interactionRepository.save(interaction);

        if ("like".equalsIgnoreCase(type)) {
            boolean mutualLikeExists = interactionRepository
                    .existsByUserIdAndTargetUserIdAndType(targetUserId, userId, "like");

            if (mutualLikeExists) {
                Match match = new Match();
                match.setUser1Id(userId);
                match.setUser2Id(targetUserId);
                match.setSeen(false);
                matchRepository.save(match);
                return true; // ¡Hay Match!
            }
        }

        return false;
    }
}