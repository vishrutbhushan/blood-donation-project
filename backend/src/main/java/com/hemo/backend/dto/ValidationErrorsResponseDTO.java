package com.hemo.backend.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ValidationErrorsResponseDTO {
    private List<ValidationErrorDTO> errors;
}
