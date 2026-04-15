package com.hemo.backend.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReferenceDataDTO {
    private List<String> bloodGroups;
    private List<String> bloodComponents;
}