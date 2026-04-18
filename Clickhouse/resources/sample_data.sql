-- Insert sources
INSERT INTO blood_ops.dim_source VALUES
(1, 'redcross', 'Red Cross', 1, now()),
(2, 'who', 'World Health Organization', 1, now());

-- Insert blood groups
INSERT INTO blood_ops.dim_blood_group VALUES
(1, 'A+', now()),
(2, 'A-', now()),
(3, 'B+', now()),
(4, 'B-', now()),
(5, 'O+', now()),
(6, 'O-', now()),
(7, 'AB+', now()),
(8, 'AB-', now());

-- Insert components
INSERT INTO blood_ops.dim_component VALUES
(1, 'Whole Blood', now()),
(2, 'Plasma', now()),
(3, 'Platelets', now()),
(4, 'Red Cells', now());

-- Insert locations with coordinates (Mumbai area)
-- Using format: location_id, pincode, city, state, street, lat, lon
INSERT INTO blood_ops.dim_location VALUES
(1, '400001', 'Mumbai', 'Maharashtra', 'Fort Area, South Mumbai', now()),
(2, '400012', 'Mumbai', 'Maharashtra', 'Parel, Central Mumbai', now()),
(3, '400051', 'Mumbai', 'Maharashtra', 'Bandra West, Suburban Mumbai', now()),
(4, '400054', 'Mumbai', 'Maharashtra', 'Vile Parle East, Suburban Mumbai', now()),
(5, '400101', 'Thane', 'Maharashtra', 'Thane West', now()),
(6, '411001', 'Pune', 'Maharashtra', 'Pune Camp', now()),
(7, '560001', 'Bangalore', 'Karnataka', 'MG Road', now()),
(8, '110001', 'New Delhi', 'Delhi', 'Connaught Place', now());

-- Insert blood banks with locations
INSERT INTO blood_ops.dim_blood_bank VALUES
(1, 1, 'RC001', 'Tata Memorial Hospital Blood Bank', 'Government', '+91-22-24177000', 'bloodbank@tmc.gov.in', 2, now(), now(), 0, 1),
(2, 1, 'RC002', 'KEM Hospital Blood Bank', 'Government', '+91-22-24107000', 'bloodbank@kem.in', 2, now(), now(), 0, 1),
(3, 2, 'WHO001', 'Lilavati Hospital Blood Bank', 'Private', '+91-22-26567891', 'bloodbank@lilavatihospital.com', 3, now(), now(), 0, 1),
(4, 1, 'RC003', 'Holy Family Hospital Blood Bank', 'Private', '+91-22-26426666', 'bloodbank@holyfamily.com', 3, now(), now(), 0, 1),
(5, 2, 'WHO002', 'Nanavati Hospital Blood Bank', 'Private', '+91-22-26878900', 'bloodbank@nanavati.com', 4, now(), now(), 0, 1),
(6, 1, 'RC004', 'Thane Civil Hospital Blood Bank', 'Government', '+91-22-25372527', 'bloodbank@thanecivil.gov.in', 5, now(), now(), 0, 1),
(7, 2, 'WHO003', 'Ruby Hall Clinic Blood Bank', 'Private', '+91-20-26631700', 'bloodbank@rubyhall.com', 6, now(), now(), 0, 1),
(8, 1, 'RC005', 'Bangalore Medical College Blood Bank', 'Government', '+91-80-26702444', 'bloodbank@bmcri.edu.in', 7, now(), now(), 0, 1);

-- Insert time dimension (sample dates)
INSERT INTO blood_ops.dim_date VALUES
(20260405, '2026-04-05', 2026, 2, 4, 5, 14),
(20260404, '2026-04-04', 2026, 2, 4, 4, 14),
(20260403, '2026-04-03', 2026, 2, 4, 3, 14);

INSERT INTO blood_ops.dim_time VALUES
(1743879600, '2026-04-05 12:00:00', 20260405, 12, 0),
(1743883200, '2026-04-05 13:00:00', 20260405, 13, 0),
(1743886800, '2026-04-05 14:00:00', 20260405, 14, 0);

-- Insert inventory snapshots
INSERT INTO blood_ops.fact_inventory_snapshot VALUES
(1, 1, 1, 2, 1, 1, 1743879600, 20260405, 45, 1, 0, '2026-04-05 12:00:00', now(), 1),
(2, 1, 1, 2, 5, 1, 1743879600, 20260405, 120, 1, 0, '2026-04-05 12:00:00', now(), 1),
(3, 1, 1, 2, 3, 2, 1743879600, 20260405, 15, 1, 0, '2026-04-05 12:00:00', now(), 1),
(4, 1, 2, 2, 1, 1, 1743879600, 20260405, 67, 1, 0, '2026-04-05 12:00:00', now(), 1),
(5, 1, 2, 2, 5, 1, 1743879600, 20260405, 200, 1, 0, '2026-04-05 12:00:00', now(), 1),
(6, 1, 2, 2, 6, 1, 1743879600, 20260405, 34, 1, 0, '2026-04-05 12:00:00', now(), 1),
(7, 2, 3, 3, 1, 1, 1743879600, 20260405, 89, 1, 0, '2026-04-05 12:00:00', now(), 1),
(8, 2, 3, 3, 7, 1, 1743879600, 20260405, 45, 1, 0, '2026-04-05 12:00:00', now(), 1),
(9, 2, 3, 3, 5, 4, 1743879600, 20260405, 156, 1, 0, '2026-04-05 12:00:00', now(), 1),
(10, 1, 4, 3, 3, 1, 1743879600, 20260405, 78, 1, 0, '2026-04-05 12:00:00', now(), 1),
(11, 1, 4, 3, 4, 1, 1743879600, 20260405, 23, 1, 0, '2026-04-05 12:00:00', now(), 1),
(12, 2, 5, 4, 1, 1, 1743879600, 20260405, 134, 1, 0, '2026-04-05 12:00:00', now(), 1),
(13, 2, 5, 4, 5, 1, 1743879600, 20260405, 290, 1, 0, '2026-04-05 12:00:00', now(), 1),
(14, 2, 5, 4, 6, 2, 1743879600, 20260405, 67, 1, 0, '2026-04-05 12:00:00', now(), 1),
(15, 1, 6, 5, 5, 1, 1743879600, 20260405, 189, 1, 0, '2026-04-05 12:00:00', now(), 1),
(16, 1, 6, 5, 1, 1, 1743879600, 20260405, 56, 1, 0, '2026-04-05 12:00:00', now(), 1),
(17, 2, 7, 6, 1, 1, 1743879600, 20260405, 112, 1, 0, '2026-04-05 12:00:00', now(), 1),
(18, 2, 7, 6, 5, 1, 1743879600, 20260405, 267, 1, 0, '2026-04-05 12:00:00', now(), 1),
(19, 1, 8, 7, 5, 1, 1743879600, 20260405, 345, 1, 0, '2026-04-05 12:00:00', now(), 1),
(20, 1, 8, 7, 1, 1, 1743879600, 20260405, 123, 1, 0, '2026-04-05 12:00:00', now(), 1);
