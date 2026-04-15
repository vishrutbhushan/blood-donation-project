package com.hemo.backend.repository;

import com.hemo.backend.entity.BloodGroupLookup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BloodGroupLookupRepository extends JpaRepository<BloodGroupLookup, String> {

    @Query("""
            select bg.bloodGroupCode
            from BloodGroupLookup bg
            where bg.active = true
            order by bg.sortOrder, bg.bloodGroupCode
            """)
    List<String> findActiveCodesOrdered();
}