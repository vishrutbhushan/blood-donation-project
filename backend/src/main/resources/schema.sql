CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(15) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE searches (
    search_id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(user_id) ON DELETE CASCADE,
    hospital_name VARCHAR(150),
    hospital_pincode VARCHAR(10),
    blood_group VARCHAR(5),
    blood_component VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE requests (
    request_id SERIAL PRIMARY KEY,
    search_id INT REFERENCES searches(search_id) ON DELETE CASCADE,
    
    blood_group VARCHAR(5) NOT NULL,
    component VARCHAR(20) NOT NULL,
    
    units_requested INT DEFAULT 1,
    
    number_of_donors_contacted INT DEFAULT 0,
    
    status VARCHAR(20) CHECK (status IN ('ACTIVE', 'INACTIVE')) DEFAULT 'ACTIVE',
    
    parent_request_id INT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (parent_request_id) REFERENCES requests(request_id) ON DELETE SET NULL
);

CREATE TABLE responses (
    response_id SERIAL PRIMARY KEY,
    
    request_id INT REFERENCES requests(request_id) ON DELETE CASCADE,
    
    donor_id VARCHAR(100),  -- from external source (RedCross/WHO)
    
    donor_name VARCHAR(100),
    phone_number VARCHAR(15),
    
    blood_group VARCHAR(5),
    
    location TEXT,
    
    response_status VARCHAR(10) CHECK (response_status IN ('YES', 'NO')),
    
    responded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);