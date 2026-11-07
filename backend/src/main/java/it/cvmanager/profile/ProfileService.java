package it.cvmanager.profile;

import it.cvmanager.profile.dto.ProfileView;
import it.cvmanager.shared.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.UUID;

@Service
public class ProfileService {

    private final CandidateProfileRepository profiles;
    private final JsonMapper jsonMapper;

    ProfileService(CandidateProfileRepository profiles, JsonMapper jsonMapper) {
        this.profiles = profiles;
        this.jsonMapper = jsonMapper;
    }

    /** Il CV Parser sostituisce il profilo corrente con una bozza da rivedere. */
    @Transactional
    public ProfileView saveDraftFromParser(UUID userId, UUID sourceCvId, CandidateProfile profile) {
        CandidateProfileEntity entity = profiles.findById(userId).orElseGet(() -> new CandidateProfileEntity(userId));
        entity.replaceWithDraft(sourceCvId, jsonMapper.writeValueAsString(profile));
        return toView(profiles.save(entity));
    }

    @Transactional
    public ProfileView update(UUID userId, CandidateProfile profile, boolean confirm) {
        CandidateProfileEntity entity = profiles.findById(userId).orElseThrow(() -> new NotFoundException("Profilo"));
        entity.edit(jsonMapper.writeValueAsString(profile), confirm);
        return toView(entity);
    }

    @Transactional(readOnly = true)
    public Optional<ProfileView> find(UUID userId) {
        return profiles.findById(userId).map(this::toView);
    }

    private ProfileView toView(CandidateProfileEntity entity) {
        return new ProfileView(entity.getStatus(), entity.getSourceCvId(), entity.getUpdatedAt(), entity.getConfirmedAt(),
                jsonMapper.readValue(entity.getProfile(), CandidateProfile.class));
    }
}
