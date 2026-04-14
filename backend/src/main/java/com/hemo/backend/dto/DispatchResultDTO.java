package com.hemo.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DispatchResultDTO {
    private Long requestId;
    private int notifiedFrom;
    private int notifiedTo;
}
