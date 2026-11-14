package com.serviciocancha.macthappbackend.service;

import com.serviciocancha.macthappbackend.exception.ApiException;
import com.serviciocancha.macthappbackend.model.Interaction;
import com.serviciocancha.macthappbackend.model.Match;
import com.serviciocancha.macthappbackend.repository.InteractionRepository;
import com.serviciocancha.macthappbackend.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InteractionService {

    private final InteractionRepository interactionRepository;
    private final MatchRepository matchRepository;

    @Transactional
    public boolean registerInteraction(UUID userId, UUID targetUserId, String type) {
        if (userId == null || targetUserId == null || userId.equals(targetUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Interacción inválida.");
        }

        String normalizedType = type == null ? "" : type.toLowerCase(Locale.ROOT);

        if (!normalizedType.equals("like") && !normalizedType.equals("reject")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El tipo debe ser like o reject.");
        }

        if (interactionRepository.existsByUserIdAndTargetUserId(userId, targetUserId)) {
            return false;
        }

        Interaction interaction = new Interaction();
        interaction.setUserId(userId);
        interaction.setTargetUserId(targetUserId);
        interaction.setType(normalizedType);
        interactionRepository.save(interaction);

        if (normalizedType.equals("reject")) {
            return false;
        }

        boolean mutualLikeExists = interactionRepository
                .existsByUserIdAndTargetUserIdAndType(targetUserId, userId, "like");

        if (!mutualLikeExists) {
            return false;
        }

        UUID firstId = userId.compareTo(targetUserId) < 0 ? userId : targetUserId;
        UUID secondId = userId.compareTo(targetUserId) < 0 ? targetUserId : userId;

        if (!matchRepository.existsByUser1IdAndUser2Id(firstId, secondId)) {
            Match match = new Match();
            match.setUser1Id(firstId);
            match.setUser2Id(secondId);
            match.setSeen(false);
            matchRepository.save(match);
        }

        return true;
    }
}
