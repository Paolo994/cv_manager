package it.cvmanager.profile.preferences;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@Service
public class JobPreferencesService {

    private final JobPreferencesRepository repository;
    private final JsonMapper jsonMapper;

    JobPreferencesService(JobPreferencesRepository repository, JsonMapper jsonMapper) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional(readOnly = true)
    public JobPreferences get(UUID userId) {
        return repository.findById(userId)
                .map(e -> jsonMapper.readValue(e.getPreferences(), JobPreferences.class))
                .orElseGet(JobPreferences::empty);
    }

    @Transactional
    public JobPreferences save(UUID userId, JobPreferences preferences) {
        JobPreferencesEntity entity = repository.findById(userId).orElseGet(() -> new JobPreferencesEntity(userId));
        entity.update(jsonMapper.writeValueAsString(preferences));
        repository.save(entity);
        return preferences;
    }
}
