package com.hemo.backend.repository;

import com.hemo.backend.entity.BloodComponentLookup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BloodComponentLookupRepository extends JpaRepository<BloodComponentLookup, String> {

    @Query("""
            select bc.bloodComponentName
            from BloodComponentLookup bc
            where bc.active = true
            order by bc.sortOrder, bc.bloodComponentName
            """)
    List<String> findActiveNamesOrdered();
}