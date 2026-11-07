package it.cvmanager.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** La chiave è lo userId: un profilo per utente. */
interface CandidateProfileRepository extends JpaRepository<CandidateProfileEntity, UUID> {
}
