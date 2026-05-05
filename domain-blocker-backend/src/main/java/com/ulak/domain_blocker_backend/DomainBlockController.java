package com.ulak.domain_blocker_backend;

import com.ulak.domain_blocker_backend.dto.BlockedDomainDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/api/domain-block")
public class DomainBlockController {

    private final DomainBlockService domainBlockService;

    public DomainBlockController(DomainBlockService domainBlockService) {
        this.domainBlockService = domainBlockService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> blockDomains(@RequestBody DomainRequest request) {
        List<String> result = domainBlockService.blockDomains(request.getDomains());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "details", result
        ));
    }

    @DeleteMapping("/unblock")
    public ResponseEntity<Map<String, String>> unblockDomain(@RequestParam String domain) {
        String result = domainBlockService.unblockDomain(domain);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Domain unblocked successfully",
                "detail", result
        ));
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search) {

        // 1. Servisten sayfalanmış veriyi çek
        Page<BlockedDomain> domainPage = domainBlockService.getAllBlockedDomains(page,size,search);

        // 2. Entity'leri güvenli DTO'lara çevir
        List<BlockedDomainDTO> dtoList = domainPage.getContent()
                .stream()
                .map(domain -> new BlockedDomainDTO(
                        domain.getDomainName(),
                        domain.getAppliedBy(),
                        domain.getAppliedAt()))
                .collect(Collectors.toList());

        // 3. Frontend'in okuyabilmesi için kurumsal bir JSON (Map) formatı oluştur
        Map<String, Object> response = new HashMap<>();
        response.put("domains", dtoList);
        response.put("currentPage", domainPage.getNumber());
        response.put("totalItems", domainPage.getTotalElements());
        response.put("totalPages", domainPage.getTotalPages());

        return ResponseEntity.ok(response);
    }
}