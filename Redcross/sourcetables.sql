CREATE TABLE redcross_source.blood_bank (
    bb_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),

    contact_number VARCHAR(15),   -- different column name
    email VARCHAR(100),

    full_address TEXT,            -- single column
    postal_code VARCHAR(10),      -- different naming

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE redcross_source.blood_inventory (
    inventory_id SERIAL PRIMARY KEY,

    bb_id INT REFERENCES redcross_source.blood_bank(bb_id) ON DELETE CASCADE,

    blood_group VARCHAR(5) NOT NULL,
    component VARCHAR(20) NOT NULL,   -- different column name

    quantity INT DEFAULT 0,           -- different naming

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);



CREATE TABLE redcross_source.blood_donor (
    donor_id SERIAL PRIMARY KEY,

    bb_id INT REFERENCES redcross_source.blood_bank(bb_id) ON DELETE SET NULL,

    full_name VARCHAR(100),
    national_id VARCHAR(255),

    contact_number VARCHAR(15),

    address TEXT,

    blood_type VARCHAR(5),
    age INT,

    last_donation_date DATE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
