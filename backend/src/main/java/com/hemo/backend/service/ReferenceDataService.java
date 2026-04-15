package com.hemo.backend.service;

import com.hemo.backend.dto.ReferenceDataDTO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReferenceDataService {
    private final JdbcTemplate jdbcTemplate;

    public ReferenceDataDTO getReferenceData() {
        return ReferenceDataDTO.builder()
                .bloodGroups(loadBloodGroups())
                .bloodComponents(loadBloodComponents())
                .build();
    }

    private List<String> loadBloodGroups() {
        return jdbcTemplate.queryForList(
                "select blood_group_code from blood_group_lookup where is_active = true order by sort_order, blood_group_code",
                String.class
        );
    }

    private List<String> loadBloodComponents() {
        return jdbcTemplate.queryForList(
                "select blood_component_name from blood_component_lookup where is_active = true order by sort_order, blood_component_name",
                String.class
        );
    }
}