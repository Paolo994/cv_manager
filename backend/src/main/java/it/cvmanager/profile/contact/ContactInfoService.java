package it.cvmanager.profile.contact;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@Service
public class ContactInfoService {

    private final ContactInfoRepository repository;
    private final JsonMapper jsonMapper;

    ContactInfoService(ContactInfoRepository repository, JsonMapper jsonMapper) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
    }

    /** Contatti salvati, oppure solo l'email dell'account se l'utente non li ha ancora inseriti. */
    @Transactional(readOnly = true)
    public ContactInfo get(UUID userId, String accountEmail) {
        return repository.findById(userId)
                .map(e -> jsonMapper.readValue(e.getContact(), ContactInfo.class))
                .orElseGet(() -> ContactInfo.empty(accountEmail));
    }

    @Transactional
    public ContactInfo save(UUID userId, ContactInfo contact) {
        ContactInfoEntity entity = repository.findById(userId).orElseGet(() -> new ContactInfoEntity(userId));
        entity.update(jsonMapper.writeValueAsString(contact));
        repository.save(entity);
        return contact;
    }
}
