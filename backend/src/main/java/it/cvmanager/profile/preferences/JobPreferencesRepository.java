package it.cvmanager.profile.preferences;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface JobPreferencesRepository extends JpaRepository<JobPreferencesEntity, UUID> {
}
