package com.hemo.backend.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DonorSearchResponseDTO {
    String recipientBloodGroup;
    List<String> compatibleDonorGroups;
    Long totalMatched;
    Integer offset;
    Integer limit;
    List<DonorCandidateDTO> donors;
}
