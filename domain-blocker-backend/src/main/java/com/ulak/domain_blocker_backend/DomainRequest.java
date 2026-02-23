package com.ulak.domain_blocker_backend;
import lombok.Data;
import java.util.List;

@Data
public class DomainRequest {
    private List<String> domains;
}