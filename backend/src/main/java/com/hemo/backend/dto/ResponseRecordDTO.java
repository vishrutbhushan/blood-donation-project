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
public class ResponseRecordDTO {
    private Long responseId;
    private Long requestId;
    private String donorId;
    private String donorName;
    private String abhaId;
    private String phoneNumber;
    private String bloodGroup;
    private String location;
    private String responseStatus;
    private LocalDateTime respondedAt;
}
