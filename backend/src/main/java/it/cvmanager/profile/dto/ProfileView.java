package it.cvmanager.profile.dto;

import it.cvmanager.profile.CandidateProfile;
import it.cvmanager.profile.ProfileStatus;

import java.time.Instant;
import java.util.UUID;

public record ProfileView(ProfileStatus status, UUID sourceCvId, Instant updatedAt, Instant confirmedAt, CandidateProfile profile) {
}
