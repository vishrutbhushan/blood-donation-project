USE redcross_db;

-- BLOOD BANK
CREATE TABLE blood_bank (
    bb_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    contact_number VARCHAR(15),
    email VARCHAR(100),
    full_address TEXT,
    postal_code VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- INVENTORY
CREATE TABLE blood_inventory (
    inventory_id INT AUTO_INCREMENT PRIMARY KEY,
    bb_id INT,
    blood_group VARCHAR(5) NOT NULL,
    component VARCHAR(20) NOT NULL,
    quantity INT DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bb_id) REFERENCES blood_bank(bb_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- DONOR
CREATE TABLE blood_donor (
    donor_id INT AUTO_INCREMENT PRIMARY KEY,
    bb_id INT,
    full_name VARCHAR(100),
    national_id VARCHAR(255),
    contact_number VARCHAR(15),
    address TEXT,
    blood_type VARCHAR(5),
    age INT,
    last_donation_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (bb_id) REFERENCES blood_bank(bb_id) ON DELETE SET NULL
) ENGINE=InnoDB;
