package com.hemo.backend.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DonorSearchSummaryDTO {
    String recipientBloodGroup;
    List<String> compatibleDonorGroups;
    Long totalMatched;
    Long below10KmCount;
    Long below50KmCount;
    Long above50KmCount;
    Integer offset;
    Integer limit;
}
