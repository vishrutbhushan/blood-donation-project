CREATE TABLE who_source.blood_bank (
    bb_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),

    phone VARCHAR(15),
    email VARCHAR(100),

    street TEXT,
    city VARCHAR(50),
    state VARCHAR(50),
    pincode VARCHAR(10),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);



CREATE TABLE who_source.blood_inventory (
    inventory_id SERIAL PRIMARY KEY,

    bb_id INT REFERENCES who_source.blood_bank(bb_id) ON DELETE CASCADE,

    blood_group VARCHAR(5) NOT NULL,
    component_type VARCHAR(20) NOT NULL, -- RBC, PLATELETS, PLASMA

    units_available INT DEFAULT 0,

    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE who_source.blood_donor (
    donor_id SERIAL PRIMARY KEY,

    name VARCHAR(100),
    aadhaar_hash VARCHAR(255),

    phone VARCHAR(15),

    city VARCHAR(50),
    state VARCHAR(50),
    pincode VARCHAR(10),

    blood_group VARCHAR(5),
    age INT,

    last_donated DATE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
