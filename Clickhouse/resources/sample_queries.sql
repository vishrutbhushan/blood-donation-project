-- Query 1: Blood Bank Inventory by Location (City-wise)
-- Shows available blood units grouped by city
SELECT
    l.city,
    l.state,
    bb.bank_name,
    bg.blood_group,
    c.component_name,
    sum(inv.units_available) as total_units,
    count(distinct bb.bank_id) as num_banks
FROM blood_ops.fact_inventory_snapshot inv
JOIN blood_ops.dim_blood_bank bb ON inv.bank_id = bb.bank_id
JOIN blood_ops.dim_location l ON inv.location_id = l.location_id
JOIN blood_ops.dim_blood_group bg ON inv.blood_group_id = bg.blood_group_id
JOIN blood_ops.dim_component c ON inv.component_id = c.component_id
WHERE inv.is_deleted = 0
GROUP BY l.city, l.state, bb.bank_name, bg.blood_group, c.component_name
ORDER BY l.city, total_units DESC;

-- Query 2: Mumbai Blood Banks with Inventory
-- Filter for Mumbai region only
SELECT
    bb.bank_name,
    l.street_or_address,
    l.pincode,
    bg.blood_group,
    sum(inv.units_available) as available_units
FROM blood_ops.fact_inventory_snapshot inv
JOIN blood_ops.dim_blood_bank bb ON inv.bank_id = bb.bank_id
JOIN blood_ops.dim_location l ON bb.location_id = l.location_id
JOIN blood_ops.dim_blood_group bg ON inv.blood_group_id = bg.blood_group_id
WHERE inv.is_deleted = 0
  AND l.city = 'Mumbai'
GROUP BY bb.bank_name, l.street_or_address, l.pincode, bg.blood_group
ORDER BY available_units DESC;

-- Query 3: Distance calculation (Simulated using pincode difference)
-- This shows "nearby" blood banks based on pincode proximity
-- For real distance, you'd need lat/lon coordinates
SELECT
    bb.bank_name,
    l.city,
    l.pincode,
    bg.blood_group,
    sum(inv.units_available) as units_available,
    abs(toInt32(l.pincode) - 400051) as pincode_diff,
    -- Approximate distance in km (rough estimate: 1 pincode unit ≈ 0.5km)
    round(abs(toInt32(l.pincode) - 400051) * 0.5, 2) as approx_distance_km
FROM blood_ops.fact_inventory_snapshot inv
JOIN blood_ops.dim_blood_bank bb ON inv.bank_id = bb.bank_id
JOIN blood_ops.dim_location l ON bb.location_id = l.location_id
JOIN blood_ops.dim_blood_group bg ON inv.blood_group_id = bg.blood_group_id
WHERE inv.is_deleted = 0
  AND l.city IN ('Mumbai', 'Thane')  -- Mumbai metro area
GROUP BY bb.bank_name, l.city, l.pincode, bg.blood_group
ORDER BY approx_distance_km, units_available DESC;

-- Query 4: O+ Blood availability across all cities (Most commonly needed)
SELECT
    l.city,
    l.state,
    bb.bank_name,
    bb.category,
    c.component_name,
    sum(inv.units_available) as o_positive_units
FROM blood_ops.fact_inventory_snapshot inv
JOIN blood_ops.dim_blood_bank bb ON inv.bank_id = bb.bank_id
JOIN blood_ops.dim_location l ON bb.location_id = l.location_id
JOIN blood_ops.dim_blood_group bg ON inv.blood_group_id = bg.blood_group_id
JOIN blood_ops.dim_component c ON inv.component_id = c.component_id
WHERE inv.is_deleted = 0
  AND bg.blood_group = 'O+'
GROUP BY l.city, l.state, bb.bank_name, bb.category, c.component_name
ORDER BY o_positive_units DESC;

-- Query 5: Total inventory by source (Redcross vs WHO)
SELECT
    s.source_name,
    l.city,
    bg.blood_group,
    sum(inv.units_available) as total_units,
    count(distinct bb.bank_id) as num_banks
FROM blood_ops.fact_inventory_snapshot inv
JOIN blood_ops.dim_source s ON inv.source_id = s.source_id
JOIN blood_ops.dim_blood_bank bb ON inv.bank_id = bb.bank_id
JOIN blood_ops.dim_location l ON bb.location_id = l.location_id
JOIN blood_ops.dim_blood_group bg ON inv.blood_group_id = bg.blood_group_id
WHERE inv.is_deleted = 0
GROUP BY s.source_name, l.city, bg.blood_group
ORDER BY l.city, total_units DESC;

-- Query 6: Blood banks ranked by total inventory (for Grafana table)
SELECT
    bb.bank_name,
    l.city,
    l.state,
    l.pincode,
    bb.phone,
    sum(inv.units_available) as total_blood_units,
    count(distinct bg.blood_group_id) as blood_types_available
FROM blood_ops.fact_inventory_snapshot inv
JOIN blood_ops.dim_blood_bank bb ON inv.bank_id = bb.bank_id
JOIN blood_ops.dim_location l ON bb.location_id = l.location_id
JOIN blood_ops.dim_blood_group bg ON inv.blood_group_id = bg.blood_group_id
WHERE inv.is_deleted = 0
GROUP BY bb.bank_name, l.city, l.state, l.pincode, bb.phone
ORDER BY total_blood_units DESC;
