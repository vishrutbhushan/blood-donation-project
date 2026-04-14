package com.hemo.backend.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BloodCompatibilityService {
    private static final Map<String, List<String>> RECIPIENT_TO_DONORS = Map.ofEntries(
        Map.entry("O-", List.of("O-")),
        Map.entry("O+", List.of("O+", "O-")),
        Map.entry("A-", List.of("A-", "O-")),
        Map.entry("A+", List.of("A+", "A-", "O+", "O-")),
        Map.entry("B-", List.of("B-", "O-")),
        Map.entry("B+", List.of("B+", "B-", "O+", "O-")),
        Map.entry("AB-", List.of("AB-", "A-", "B-", "O-")),
        Map.entry("AB+", List.of("AB+", "AB-", "A+", "A-", "B+", "B-", "O+", "O-")),
        Map.entry("BOMBAY", List.of("BOMBAY")),
        Map.entry("RH NULL", List.of("RH NULL"))
    );

    public String normalize(String bloodGroup) {
        if (bloodGroup == null) {
            return "";
        }
        String normalized = bloodGroup.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
        if ("HH".equals(normalized) || "OH".equals(normalized)) {
            return "BOMBAY";
        }
        if ("RHNULL".equals(normalized) || "RH-NULL".equals(normalized)) {
            return "RH NULL";
        }
        return normalized;
    }

    public List<String> compatibleDonorGroups(String recipientBloodGroup) {
        String normalized = normalize(recipientBloodGroup);
        return RECIPIENT_TO_DONORS.getOrDefault(normalized, List.of(normalized));
    }
}
