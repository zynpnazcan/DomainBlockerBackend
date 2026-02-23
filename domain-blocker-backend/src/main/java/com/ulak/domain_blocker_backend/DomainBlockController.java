package com.ulak.domain_blocker_backend;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;


@CrossOrigin(origins="http://localhost:4200")
@RestController
@RequestMapping("/api/domain-block")

public class DomainBlockController {
    @Autowired
    private DomainBlockService domainBlockService;

    @PostMapping
    public ResponseEntity<List<String>> blockDomains(@RequestBody DomainRequest request) {
        List<String> domains = request.getDomains();
        List<String> result = domainBlockService.blockDomains(domains);

        return ResponseEntity.ok(result);
    }
    @DeleteMapping("/unblock")
    public String unblockDomain(@RequestParam String domain){
        domainBlockService.unblockDomain(domain);
        return domain + "için engel kaldırıldı";
    }
    @GetMapping("/all")
    public List<BlockedDomain> getAll() {
        return domainBlockService.getAllBlockedDomains();
    }
}
