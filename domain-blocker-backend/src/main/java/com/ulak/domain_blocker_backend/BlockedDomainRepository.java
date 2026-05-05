package com.ulak.domain_blocker_backend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface BlockedDomainRepository extends JpaRepository<BlockedDomain, UUID> {

    @Transactional
    void deleteByDomainName(String domainName);

    boolean existsByDomainName(String domainName);

    List<BlockedDomain> findAllByOrderByAppliedAtAsc();
    Page<BlockedDomain> findByDomainNameContainingIgnoreCase(String keyword, Pageable pageable);
}