package it.cvmanager.profile.contact;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ContactInfoRepository extends JpaRepository<ContactInfoEntity, UUID> {
}
