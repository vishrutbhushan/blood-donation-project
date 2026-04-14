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
public class SearchResponseDTO {
    private Long searchId;
    private Long userId;
    private String hospitalName;
    private String hospitalPincode;
    private String bloodGroup;
    private String bloodComponent;
    private LocalDateTime createdAt;
}
