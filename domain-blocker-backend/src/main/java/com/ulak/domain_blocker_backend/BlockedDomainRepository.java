package com.ulak.domain_blocker_backend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List; // List kullanabilmek için bu import şart!
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface BlockedDomainRepository extends JpaRepository<BlockedDomain, UUID> {

    @Transactional
    void deleteByDomainName(String domainName);

    boolean existsByDomainName(String domainName);

    List<BlockedDomain> findAllByOrderByAppliedAtAsc();
}