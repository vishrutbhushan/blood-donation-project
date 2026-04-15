package com.hemo.backend.service;

import com.hemo.backend.dto.ReferenceDataDTO;
import com.hemo.backend.repository.BloodComponentLookupRepository;
import com.hemo.backend.repository.BloodGroupLookupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReferenceDataService {
    private final BloodGroupLookupRepository bloodGroupLookupRepository;
    private final BloodComponentLookupRepository bloodComponentLookupRepository;

    public ReferenceDataDTO getReferenceData() {
        return ReferenceDataDTO.builder()
                .bloodGroups(bloodGroupLookupRepository.findActiveCodesOrdered())
                .bloodComponents(bloodComponentLookupRepository.findActiveNamesOrdered())
                .build();
    }
}