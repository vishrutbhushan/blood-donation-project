package com.hemo.backend.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestSummaryDTO {
    private Long requestId;
    private Long searchId;
    private String bloodGroup;
    private String component;
    private Integer unitsRequested;
    private Integer numberOfDonorsContacted;
    private String status;
    private Long parentRequestId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime lastNotifiedAt;
    private boolean canReRequest;
    private String reRequestBlockedReason;
}
